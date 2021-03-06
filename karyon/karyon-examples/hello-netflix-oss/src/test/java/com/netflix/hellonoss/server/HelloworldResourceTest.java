/*
 * Copyright 2013 Netflix, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */
package com.netflix.hellonoss.server;

import com.google.inject.Inject;
import com.netflix.hellonoss.utils.Deployments;
import com.netflix.kayron.server.test.RunInKaryon;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.Response;

import static junit.framework.Assert.assertEquals;

/**
 * An example test case that demonstrates a injection of REST endpoint.
 *
 * @author Jakub Narloch (jmnarloch@gmail.com)
 */
@RunWith(Arquillian.class)
@RunInKaryon(applicationId = "hello-netflix-oss")
public class HelloworldResourceTest {

    /**
     * Creates the test deployment.
     *
     * @return the test deployment
     */
    @Deployment
    public static Archive createTestArchive() {

        return Deployments.createDeployment();
    }

    /**
     * The instance of the REST service.
     */
    @Inject
    private HelloworldResource instance;

    /**
     * Test the {@link HelloworldResource#hello()} method.
     */
    @Test
    public void shouldRetrieveHelloMessage() {

        // when
        Response response = instance.hello();

        // then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("{\"Message\":\"Hello Netflix OSS component!\"}", response.getEntity());
    }
}
