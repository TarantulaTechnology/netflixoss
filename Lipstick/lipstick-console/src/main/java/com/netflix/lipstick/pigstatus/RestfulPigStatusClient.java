/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.lipstick.pigstatus;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import com.netflix.lipstick.model.P2jPlanPackage;
import com.netflix.lipstick.model.P2jPlanStatus;
import com.netflix.lipstick.model.P2jSampleOutputList;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * RESTful client implementation of PigStatusClient.
 *
 * @author nbates
 *
 */
public class RestfulPigStatusClient implements PigStatusClient {
    protected enum RequestVerb {
        POST, PUT
    };

    private static final Log LOG = LogFactory.getLog(RestfulPigStatusClient.class);

    protected ObjectMapper om = new ObjectMapper();
    protected String serviceUrl = null;

    /**
     * Constructs a default RestfulPigStatusClient.
     */
    public RestfulPigStatusClient() {
    }

    /**
     * Constructs a RestfulPigStatusClient with the given serviceUrl.
     *
     * @param serviceUrl
     */
    public RestfulPigStatusClient(String serviceUrl) {
        LOG.info("Initializing " + this.getClass() + " with serviceUrl: " + serviceUrl);
        this.serviceUrl = serviceUrl;
    }

    @Override
    public String savePlan(P2jPlanPackage plans) {
        plans.getStatus().setHeartbeatTime();
        String resourceUrl = serviceUrl + "/job/";
        ClientResponse response = sendRequest(resourceUrl, plans, RequestVerb.POST);
        if (response == null) {
	        return null;
        }

        try {
            String output = (String) om.readValue(response.getEntity(String.class), Map.class).get("uuid");
            if (!plans.getUuid().equals(output)) {
                LOG.error("Incorrect uuid returned from server");
            }
            LOG.info("This script has been assigned uuid: " + output);
            LOG.info("Navigate to " + serviceUrl + "#job/" + output + " to view progress.");
            return plans.getUuid();

        } catch (Exception e) {
            LOG.error("Error getting uuid from server response.", e);
        }
        return null;
    }

    @Override
    public void saveStatus(String uuid, P2jPlanStatus status) {
        status.setHeartbeatTime();
        String resourceUrl = serviceUrl + "/job/" + uuid;
        sendRequest(resourceUrl, status, RequestVerb.PUT);
        LOG.info("Navigate to " + serviceUrl + "#job/" + uuid + " to view progress.");
    }

    @Override
    public void saveSampleOutput(String uuid, String jobId, P2jSampleOutputList sampleOutputList) {
        String resourceUrl = String.format("%s/job/%s/sampleOutput/%s", serviceUrl, uuid, jobId);
        sendRequest(resourceUrl, sampleOutputList, RequestVerb.PUT);
    }

    protected ClientResponse sendRequest(String resourceUrl, Object requestObj, RequestVerb verb) {
        try {
            Client client = Client.create();
            WebResource webResource = client.resource(resourceUrl);
            LOG.debug("Sending " + verb + " request to " + resourceUrl);
            LOG.debug(om.writeValueAsString(requestObj));

            ClientResponse response = null;

            switch (verb) {
            case POST:
                response = webResource.type("application/json").post(ClientResponse.class,
                                                                     om.writeValueAsString(requestObj));
                break;
            case PUT:
                response = webResource.type("application/json").put(ClientResponse.class,
                                                                    om.writeValueAsString(requestObj));
                break;
            default:
                throw new RuntimeException("Invalid verb: " + verb + " for resourceUrl: " + resourceUrl);
            }

            if (response != null && response.getStatus() != 200) {
                LOG.error("Error contacting Lipstick server.  Received status code " + response.getStatus());
                LOG.debug(response.getEntity(String.class));
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
            }

            return response;

        } catch (Exception e) {
            LOG.error("Error contacting Lipstick server.");
            LOG.debug("Stacktrace", e);
        }

        return null;
    }
}
