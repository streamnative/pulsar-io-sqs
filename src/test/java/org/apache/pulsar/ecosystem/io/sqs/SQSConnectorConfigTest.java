/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.ecosystem.io.sqs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Unit test {@link SQSConnectorConfig}.
 */
public class SQSConnectorConfigTest {

    /*
     * Test Case: load the configuration from an empty property map.
     *
     * @throws IOException when failed to load the property map
     */
    @Test
    public void testLoadEmptyPropertyMap() throws IOException {
        Map<String, Object> emptyMap = Collections.emptyMap();
        SQSConnectorConfig config = SQSConnectorConfig.load(emptyMap);
        assertNull("Region should not be set", config.getAwsRegion());
        assertEquals("Endpoint should not be set", "", config.getAwsEndpoint());
        assertNull("QueueName should not be set", config.getQueueName());
        assertEquals("AwsCredentialPluginName should not be set", "", config.getAwsCredentialPluginName());
        assertEquals("AwsCredentialPluginParam should not be set", "", config.getAwsCredentialPluginParam());
    }

    /*
     * Test Case: load the configuration from a property map.
     *
     * @throws IOException when failed to load the property map
     */
    @Test
    public void testLoadPropertyMap() throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("awsRegion", "us-east-1");
        properties.put("queueName", "test-queue");
        properties.put("awsEndpoint", "https://some.endpoint.aws");
        properties.put("awsCredentialPluginParam", "{\"accessKey\":\"myKey\",\"secretKey\":\"my-Secret\"}");

        SQSConnectorConfig config = SQSConnectorConfig.load(properties);
        assertEquals("Mismatched Region : " + config.getAwsRegion(),
                "us-east-1", config.getAwsRegion());
        assertEquals("Mismatched queueName : " + config.getQueueName(),
                "test-queue", config.getQueueName());
        assertEquals("Mismatched awsEndpoint : " + config.getAwsEndpoint(),
                "https://some.endpoint.aws", config.getAwsEndpoint());
        assertEquals("Mismatched awsCredentialPluginParam : " + config.getAwsCredentialPluginParam(),
                "{\"accessKey\":\"myKey\",\"secretKey\":\"my-Secret\"}", config.getAwsCredentialPluginParam());
    }

    /*
     * Test Case: init source connector without required params.
     */
    @Test
    public final void testMissingCredentialParam() {
        Map<String, Object> properties = new HashMap<String, Object> ();
        properties.put("awsEndpoint", "https://some.endpoint.aws");
        properties.put("awsRegion", "us-east-1");
        properties.put("queueName", "test-queue");

        try {
            SQSSource source = new SQSSource();
            source.open(properties, null);
        } catch (Exception ex) {
            assertNotNull("Missing param should lead to exception", ex);
        }
    }

}
