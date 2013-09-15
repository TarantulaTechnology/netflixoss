/*
 *
 *  Copyright 2013 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.netflix.genie.server.services;

import org.junit.Assert;
import org.junit.Test;

import org.apache.commons.configuration.AbstractConfiguration;
import com.netflix.config.ConfigurationManager;

/**
 * Test for factories that instanstiate services.
 *
 * @author skrishnan
 */
public class TestFactory {

    /**
     * Test instantiation from a good/existing property.
     *
     * @throws Exception if anything went wrong with the test
     */
    @Test
    public void testGoodInstantiation() throws Exception {
        AbstractConfiguration conf = ConfigurationManager.getConfigInstance();
        conf.setProperty("unit.test", "java.lang.String");
        String s = (String) BaseServiceFactory
                .instantiateFromProperty("unit.test");
        Assert.assertNotNull(s);
    }

    /**
     * Test error throwing/handling if a bad/non-existing property is provided.
     */
    @Test
    public void testBadInstantiation() {
        AbstractConfiguration conf = ConfigurationManager.getConfigInstance();
        conf.setProperty("unit.test", "java.lang.NotAString");
        try {
            BaseServiceFactory.instantiateFromProperty("unit.test");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }
}
