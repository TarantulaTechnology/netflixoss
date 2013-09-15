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

public enum IntConfigs
{
    /**
     * The port to connect to the ZK server - default: 2181
     */
    CLIENT_PORT()
    {
        @Override
        public boolean isRestartSignificant()
        {
            return true;
        }
    },

    /**
     * The port ZK instances use to connect to each other - default: 2888
     */
    CONNECT_PORT()
    {
        @Override
        public boolean isRestartSignificant()
        {
            return true;
        }
    },

    /**
     * The 2nd port ZK instances use to connect to each other - default: 3888
     */
    ELECTION_PORT()
    {
        @Override
        public boolean isRestartSignificant()
        {
            return true;
        }
    },

    /**
     * Period in ms to check that ZK is running - default: 30000
     */
    CHECK_MS()
    {
        @Override
        public boolean isRestartSignificant()
        {
            return false;
        }
    },

    /**
     * Period in ms to perform log cleanup - default: 12 hours
     */
    CLEANUP_PERIOD_MS()
    {
        @Override
        public boolean isRestartSignificant()
        {
            return false;
        }
    },

    /**
     * Value to pass to PurgeTxnLog as max - default: 3
     */
    CLEANUP_MAX_FILES()
    {
        @Override
        public boolean isRestartSignificant()
        {
            return false;
        }
    },

    /**
     * Max backup session to retain - default: 5
     */
    BACKUP_MAX_STORE_MS()
    {
        @Override
        public boolean isRestartSignificant()
        {
            return false;
        }
    },

    /**
     * Period in ms to perform backups - default: 60000
     */
    BACKUP_PERIOD_MS()
    {
        @Override
        public boolean isRestartSignificant()
        {
            return false;
        }
    },

    /**
     * true/false (0 or 1) - determine if automatic instance management is on/off - default is false
     */
    AUTO_MANAGE_INSTANCES()
    {
        @Override
        public boolean isRestartSignificant()
        {
            return false;
        }
    },

    /**
     * Period in ms to wait for instances to settle (i.e. no change in state) before processing
     * automatic instance management
     */
    AUTO_MANAGE_INSTANCES_SETTLING_PERIOD_MS()
    {
        @Override
        public boolean isRestartSignificant()
        {
            return false;
        }
    },

    /**
     * Marks which instances are made Observers by automatic instance management. Instances below
     * this number are normal instances. Instances from this number and up are Observers.
     */
    OBSERVER_THRESHOLD()
    {
        @Override
        public boolean isRestartSignificant()
        {
            return false;
        }
    },

    /**
     * If non-zero, automatic instance management will attempt to keep the ensemble at this fixed size. Further, rolling
     * config changes will _not_ be used. Experience has shown that rolling config changes can lead to runtime problems.
     */
    AUTO_MANAGE_INSTANCES_FIXED_ENSEMBLE_SIZE()
    {
        @Override
        public boolean isRestartSignificant()
        {
            return false;
        }
    },

    /**
     * boolean - if true (non zero) automatic instance management will make config changes all at once (instead
     * of via rolling change).
     */
    AUTO_MANAGE_INSTANCES_APPLY_ALL_AT_ONCE()
    {
        @Override
        public boolean isRestartSignificant()
        {
            return false;
        }
    }
    ;

    /**
     * Return true if a change to this config requires that the ZK instance be restarted
     *
     * @return true/false
     */
    public abstract boolean     isRestartSignificant();
}
