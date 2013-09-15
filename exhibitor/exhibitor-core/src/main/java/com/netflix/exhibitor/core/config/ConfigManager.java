/*
 * Copyright 2012 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.exhibitor.core.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.activity.QueueGroups;
import com.netflix.exhibitor.core.activity.RepeatingActivity;
import com.netflix.exhibitor.core.activity.RepeatingActivityImpl;
import com.netflix.exhibitor.core.config.none.NoneConfigProvider;
import com.netflix.exhibitor.core.state.InstanceState;
import com.netflix.exhibitor.core.state.InstanceStateTypes;
import com.netflix.exhibitor.core.automanage.RemoteInstanceRequest;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ConfigManager implements Closeable
{
    private final Exhibitor exhibitor;
    private final ConfigProvider provider;
    private final int maxAttempts;
    private final RepeatingActivity repeatingActivity;
    private final AtomicReference<LoadedInstanceConfig> config = new AtomicReference<LoadedInstanceConfig>();
    private final Set<ConfigListener> configListeners = Sets.newSetFromMap(Maps.<ConfigListener, Boolean>newConcurrentMap());
    private final AtomicReference<RollingConfigAdvanceAttempt> rollingConfigAdvanceAttempt = new AtomicReference<RollingConfigAdvanceAttempt>(null);
    private final AtomicInteger waitingForQuorumAttempts = new AtomicInteger(0);

    @VisibleForTesting
    final static int DEFAULT_MAX_ATTEMPTS = 4;

    public ConfigManager(Exhibitor exhibitor, ConfigProvider provider, int checkMs) throws Exception
    {
        this(exhibitor, provider, checkMs, DEFAULT_MAX_ATTEMPTS);
    }

    @VisibleForTesting
    ConfigManager(Exhibitor exhibitor, ConfigProvider provider, int checkMs, int maxAttempts) throws Exception
    {
        this.exhibitor = exhibitor;
        this.provider = provider;
        this.maxAttempts = maxAttempts;

        Activity    activity = new Activity()
        {
            @Override
            public void completed(boolean wasSuccessful)
            {
            }

            @Override
            public Boolean call() throws Exception
            {
                doWork();
                return true;
            }
        };
        repeatingActivity = new RepeatingActivityImpl(exhibitor.getLog(), exhibitor.getActivityQueue(), QueueGroups.MAIN, activity, checkMs);

        config.set(provider.loadConfig());
    }

    public void   start() throws Exception
    {
        provider.start();
        repeatingActivity.start();
    }

    @Override
    public void close() throws IOException
    {
        repeatingActivity.close();
        Closeables.closeQuietly(provider);
    }

    public InstanceConfig getConfig()
    {
        return getCollection().getConfigForThisInstance(exhibitor.getThisJVMHostname());
    }

    public boolean              isRolling()
    {
        return getCollection().isRolling();
    }

    public RollingConfigState   getRollingConfigState()
    {
        return getCollection().getRollingConfigState();
    }

    /**
     * Add a listener for config changes
     *
     * @param listener listener
     */
    public void addConfigListener(ConfigListener listener)
    {
        configListeners.add(listener);
    }

    public boolean isStandaloneMode()
    {
        return provider instanceof NoneConfigProvider;
    }

    public enum CancelMode
    {
        ROLLBACK,
        FORCE_COMMIT
    }

    public PseudoLock       newConfigBasedLock() throws Exception
    {
        return provider.newPseudoLock();
    }

    public synchronized void     cancelRollingConfig(CancelMode mode) throws Exception
    {
        ConfigCollection localConfig = getCollection();
        if ( localConfig.isRolling() )
        {
            clearAttempts();

            InstanceConfig          newConfig = (mode == CancelMode.ROLLBACK) ? localConfig.getRootConfig() : localConfig.getRollingConfig();
            ConfigCollection        newCollection = new ConfigCollectionImpl(newConfig, null);
            internalUpdateConfig(newCollection);
        }
    }

    public synchronized void     checkRollingConfig(InstanceState instanceState) throws Exception
    {
        ConfigCollection localConfig = getCollection();
        if ( localConfig.isRolling() )
        {
            RollingReleaseState     state = new RollingReleaseState(instanceState, localConfig);
            if ( state.getCurrentRollingHostname().equals(exhibitor.getThisJVMHostname()) )
            {
                if ( state.serverListHasSynced() )
                {
                    if ( instanceState.getState() == InstanceStateTypes.SERVING )
                    {
                        advanceRollingConfig(localConfig);
                    }
                    else if ( instanceState.getState() == InstanceStateTypes.NOT_SERVING )
                    {
                        if ( waitingForQuorumAttempts.incrementAndGet() >= maxAttempts )
                        {
                            exhibitor.getLog().add(ActivityLog.Type.INFO, "Ensemble is not achieving quorum. Now falling back to a force commit of the configuration change.");
                            cancelRollingConfig(CancelMode.FORCE_COMMIT);
                        }
                        else
                        {
                            exhibitor.getLog().add(ActivityLog.Type.INFO, "Waiting for instance sync before advancing rolling config. Attempt " + waitingForQuorumAttempts.get() + " of " + maxAttempts);
                        }
                    }
                }
            }
        }
    }

    public synchronized boolean startRollingConfig(final InstanceConfig newConfig, String leaderHostname) throws Exception
    {
        ConfigCollection        localConfig = getCollection();
        if ( localConfig.isRolling() )
        {
            return false;
        }

        InstanceConfig          currentConfig = getCollection().getRootConfig();
        RollingHostNamesBuilder builder = new RollingHostNamesBuilder(currentConfig, newConfig, leaderHostname);

        clearAttempts();

        ConfigCollection        newCollection = new ConfigCollectionImpl(currentConfig, newConfig, builder.getRollingHostNames(), 0);
        return advanceOrStartRollingConfig(newCollection, -1);
    }

    public synchronized boolean updateConfig(final InstanceConfig newConfig) throws Exception
    {
        ConfigCollection        localConfig = getCollection();
        if ( localConfig.isRolling() )
        {
            return false;
        }

        clearAttempts();

        ConfigCollection        newCollection = new ConfigCollectionImpl(newConfig, null);
        return internalUpdateConfig(newCollection);
    }

    @VisibleForTesting
    ConfigCollection getCollection()
    {
        return config.get().getConfig();
    }

    @VisibleForTesting
    public LoadedInstanceConfig getLoadedInstanceConfig()
    {
        return config.get();
    }

    /**
     * FOR TESTING ONLY
     *
     * @param testConfig override config
     */
    @VisibleForTesting
    public void testingSetLoadedInstanceConfig(LoadedInstanceConfig testConfig)
    {
        config.set(testConfig);
    }

    @VisibleForTesting
    RollingConfigAdvanceAttempt getRollingConfigAdvanceAttempt()
    {
        return rollingConfigAdvanceAttempt.get();
    }

    private void advanceRollingConfig(ConfigCollection config) throws Exception
    {
        int             rollingHostNamesIndex = config.getRollingConfigState().getRollingHostNamesIndex();
        advanceOrStartRollingConfig(config, rollingHostNamesIndex);
    }

    private boolean advanceOrStartRollingConfig(ConfigCollection config, int rollingHostNamesIndex) throws Exception
    {
        waitingForQuorumAttempts.set(0);

        List<String>        rollingHostNames = config.getRollingConfigState().getRollingHostNames();
        boolean             updateConfigResult;
        ConfigCollection    newCollection = checkNextInstanceState(config, rollingHostNames, rollingHostNamesIndex);
        if ( newCollection != null )
        {
            clearAttempts();
            updateConfigResult = internalUpdateConfig(newCollection);
        }
        else
        {
            if ( rollingHostNamesIndex < 0 )
            {
                // this is the start phase - park the bad instance in the back for now
                List<String>        newRollingHostNames = Lists.newArrayList(rollingHostNames);
                Collections.rotate(newRollingHostNames, -1);
                ConfigCollection        collection = new ConfigCollectionImpl(config.getRootConfig(), config.getRollingConfig(), newRollingHostNames, rollingHostNamesIndex + 1);

                clearAttempts();
                updateConfigResult = internalUpdateConfig(collection);
            }
            else
            {
                updateConfigResult = true;
            }
        }
        return updateConfigResult;
    }

    private void clearAttempts()
    {
        rollingConfigAdvanceAttempt.set(null);
        waitingForQuorumAttempts.set(0);
    }

    private ConfigCollection checkNextInstanceState(ConfigCollection config, List<String> rollingHostNames, int rollingHostNamesIndex)
    {
        if ( (rollingHostNamesIndex + 1) >= rollingHostNames.size() )
        {
            // we're done - switch back to single config
            return new ConfigCollectionImpl(config.getRollingConfig(), null);
        }

        ConfigCollection                newCollection = new ConfigCollectionImpl(config.getRootConfig(), config.getRollingConfig(), rollingHostNames, rollingHostNamesIndex + 1);
        RollingReleaseState             state = new RollingReleaseState(new InstanceState(), newCollection);
        if ( state.getCurrentRollingHostname().equals(exhibitor.getThisJVMHostname()) )
        {
            return newCollection;
        }

        RollingConfigAdvanceAttempt         activeAttempt = rollingConfigAdvanceAttempt.get();

        RemoteInstanceRequest.Result        result;
        if ( (activeAttempt == null) || !activeAttempt.getHostname().equals(state.getCurrentRollingHostname()) || (activeAttempt.getAttemptCount() < maxAttempts) )
        {
            RemoteInstanceRequest           remoteInstanceRequest = new RemoteInstanceRequest(exhibitor, state.getCurrentRollingHostname());
            result = remoteInstanceRequest.makeRequest(exhibitor.getRemoteInstanceRequestClient(), "getStatus");

            if ( activeAttempt == null )
            {
                activeAttempt = new RollingConfigAdvanceAttempt(state.getCurrentRollingHostname());
                rollingConfigAdvanceAttempt.set(activeAttempt);
            }
            activeAttempt.incrementAttemptCount();

            if ( result.errorMessage.length() != 0 )
            {
                if ( activeAttempt.getAttemptCount() >= maxAttempts )
                {
                    exhibitor.getLog().add(ActivityLog.Type.INFO, "Exhausted attempts to connect to " + remoteInstanceRequest.getHostname() + " - skipping and moving on to next instance");
                    newCollection = checkNextInstanceState(config, rollingHostNames, rollingHostNamesIndex + 1);  // it must be down. Skip it.
                }
                else
                {
                    exhibitor.getLog().add(ActivityLog.Type.INFO, "Could not connect to " + remoteInstanceRequest.getHostname() + " - attempt #" + activeAttempt.getAttemptCount());
                    newCollection = null;
                }
            }
        }
        else
        {
            newCollection = null;
        }
        return newCollection;
    }

    private boolean internalUpdateConfig(ConfigCollection newCollection) throws Exception
    {
        LoadedInstanceConfig updated = provider.storeConfig(newCollection, config.get().getVersion());
        if ( updated != null )
        {
            setNewConfig(updated);
            return true;
        }

        return false;
    }

    private void setNewConfig(LoadedInstanceConfig newConfig) throws Exception
    {
        config.getAndSet(newConfig);
        notifyListeners();
    }

    private synchronized void notifyListeners()
    {
        for ( ConfigListener listener : configListeners )
        {
            listener.configUpdated();
        }
    }

    private synchronized void doWork() throws Exception
    {
        LoadedInstanceConfig    newConfig = provider.loadConfig();
        if ( newConfig.getVersion() != config.get().getVersion() )
        {
            setNewConfig(newConfig);
        }
    }
}
