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

package com.netflix.exhibitor.core.state;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.activity.QueueGroups;
import com.netflix.exhibitor.core.activity.RepeatingActivity;
import com.netflix.exhibitor.core.activity.RepeatingActivityImpl;
import com.netflix.exhibitor.core.config.ConfigListener;
import com.netflix.exhibitor.core.config.EncodedConfigParser;
import com.netflix.exhibitor.core.config.InstanceConfig;
import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.config.StringConfigs;
import com.netflix.exhibitor.core.controlpanel.ControlPanelTypes;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MonitorRunningInstance implements Closeable
{
    private final Exhibitor                         exhibitor;
    private final AtomicReference<InstanceState>    currentInstanceState = new AtomicReference<InstanceState>();
    private final AtomicBoolean                     currentIsLeader = new AtomicBoolean(false);
    private final RepeatingActivity                 repeatingActivity;

    private static final int    DOWN_RECHECK_FACTOR = 10;

    public MonitorRunningInstance(Exhibitor exhibitor)
    {
        this.exhibitor = exhibitor;
        Activity activity = new Activity()
        {
            @Override
            public void completed(boolean wasSuccessful)
            {
                // NOP
            }

            @Override
            public Boolean call() throws Exception
            {
                doWork();
                return true;
            }
        };

        repeatingActivity = new RepeatingActivityImpl(exhibitor.getLog(), exhibitor.getActivityQueue(), QueueGroups.MAIN, activity, exhibitor.getConfigManager().getConfig().getInt(IntConfigs.CHECK_MS));
    }

    public void start()
    {
        repeatingActivity.start();
        exhibitor.getConfigManager().addConfigListener
        (
            new ConfigListener()
            {
                @Override
                public void configUpdated()
                {
                    repeatingActivity.setTimePeriodMs(exhibitor.getConfigManager().getConfig().getInt(IntConfigs.CHECK_MS));
                }
            }
        );
    }

    @Override
    public void close() throws IOException
    {
        repeatingActivity.close();
    }

    public InstanceStateTypes   getCurrentInstanceState()
    {
        InstanceState   state = currentInstanceState.get();
        return (state != null) ? state.getState() : InstanceStateTypes.LATENT;
    }

    public boolean              isCurrentlyLeader()
    {
        return currentIsLeader.get();
    }

    @VisibleForTesting
    void doWork() throws Exception
    {
        InstanceConfig  config = exhibitor.getConfigManager().getConfig();
        StateAndLeader  stateAndLeader = new Checker(exhibitor).calculateState();
        InstanceState   instanceState = new InstanceState(new ServerList(config.getString(StringConfigs.SERVERS_SPEC)), stateAndLeader.getState(), new RestartSignificantConfig(config));

        currentIsLeader.set(stateAndLeader.isLeader());

        exhibitor.getConfigManager().checkRollingConfig(instanceState);

        InstanceState   localCurrentInstanceState = currentInstanceState.get();
        if ( instanceState.equals(localCurrentInstanceState) )
        {
            checkForRestart(config, localCurrentInstanceState);
        }
        else
        {
            handleServerListChange(instanceState, localCurrentInstanceState);
        }
    }

    private void handleServerListChange(InstanceState instanceState, InstanceState localCurrentInstanceState) throws Exception
    {
        boolean         serverListChange = (localCurrentInstanceState != null) && !localCurrentInstanceState.getServerList().equals(instanceState.getServerList());
        boolean         configChange = (localCurrentInstanceState != null) && !localCurrentInstanceState.getCurrentConfig().equals(instanceState.getCurrentConfig());
        currentInstanceState.set(instanceState);

        exhibitor.getLog().add(ActivityLog.Type.INFO, "State: " + instanceState.getState().getDescription());

        if ( serverListChange )
        {
            exhibitor.getLog().add(ActivityLog.Type.INFO, "Server list has changed");
            restartZooKeeper(localCurrentInstanceState);
        }
        else if ( configChange )
        {
            exhibitor.getLog().add(ActivityLog.Type.INFO, "ZooKeeper related configuration has changed");
            restartZooKeeper(localCurrentInstanceState);
        }
        else
        {
            switch ( instanceState.getState() )
            {
                case DOWN:
                {
                    restartZooKeeper(localCurrentInstanceState);
                    break;
                }

                default:
                {
                    // nop
                    break;
                }
            }
        }
    }

    private void checkForRestart(InstanceConfig config, InstanceState localCurrentInstanceState) throws Exception
    {
        if ( exhibitor.getConfigManager().isRolling() )
        {
            return;
        }

        UsState     state = new UsState(exhibitor);
        boolean     serverListIsDefined = (state.getServerList().getSpecs().size() > 0);
        if ( serverListIsDefined && (state.getUs() == null) )
        {
            return; // there is a server list and we're not in it. Therefore, there's no point in restarting, it will always fail
        }

        if ( (localCurrentInstanceState.getState() == InstanceStateTypes.DOWN) || (localCurrentInstanceState.getState() == InstanceStateTypes.NOT_SERVING) )
        {
            long        elapsedMs = System.currentTimeMillis() - localCurrentInstanceState.getTimestampMs();
            int         downInstanceRestartMs = getDownInstanceRestartMs(config);
            if ( elapsedMs > downInstanceRestartMs )
            {
                exhibitor.getLog().add(ActivityLog.Type.INFO, "Restarting down/not-serving ZooKeeper after " + elapsedMs + " ms pause");
                restartZooKeeper(localCurrentInstanceState);
            }
            else
            {
                exhibitor.getLog().add(ActivityLog.Type.INFO, "ZooKeeper down/not-serving waiting " + elapsedMs + " of " + downInstanceRestartMs + " ms before restarting");
            }
        }
    }

    @VisibleForTesting
    protected void restartZooKeeper(InstanceState currentInstanceState) throws Exception
    {
        if ( currentInstanceState != null )
        {
            currentInstanceState.updateTimestampMs();
        }
        if ( !exhibitor.getControlPanelValues().isSet(ControlPanelTypes.RESTARTS) )
        {
            exhibitor.getLog().add(ActivityLog.Type.INFO, "Restart of ZooKeeper skipped due to control panel setting");
            return;
        }

        exhibitor.getActivityQueue().add(QueueGroups.MAIN, new KillRunningInstance(exhibitor, true));
    }

    private int getDownInstanceRestartMs(InstanceConfig config)
    {
        EncodedConfigParser     parser = new EncodedConfigParser(exhibitor.getConfigManager().getConfig().getString(StringConfigs.ZOO_CFG_EXTRA));
        int                     tickTime = parseInt(parser.getValue("tickTime"));
        int                     initLimit = parseInt(parser.getValue("initLimit"));
        int                     syncLimit = parseInt(parser.getValue("syncLimit"));

        if ( (tickTime > 0) && ((initLimit > 0) || (syncLimit > 0)) )
        {
            return 2 * tickTime * Math.max(initLimit, syncLimit);  // ZK should sync or fail within the initLimit/syncLimit
        }

        return (config.getInt(IntConfigs.CHECK_MS) * DOWN_RECHECK_FACTOR);
    }

    private int parseInt(String str)
    {
        try
        {
            return (str != null) ? Integer.parseInt(str) : 0;
        }
        catch ( NumberFormatException e )
        {
            // ignore
        }
        return 0;
    }
}
