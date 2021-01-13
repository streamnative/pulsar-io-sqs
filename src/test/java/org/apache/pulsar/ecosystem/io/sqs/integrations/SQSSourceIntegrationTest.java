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
package org.apache.pulsar.ecosystem.io.sqs.integrations;

import static org.apache.pulsar.ecosystem.io.sqs.SQSTestUtils.getTestConfig;
import static org.apache.pulsar.ecosystem.io.sqs.SQSTestUtils.purgeSQSQueue;

import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.ecosystem.io.sqs.SQSConnectorConfig;
import org.apache.pulsar.ecosystem.io.sqs.SQSSource;
import org.apache.pulsar.ecosystem.io.sqs.SQSUtils;
import org.apache.pulsar.io.aws.AbstractAwsConnector;
import org.apache.pulsar.io.aws.AwsCredentialProviderPlugin;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration test {@link SQSSource}.
 */
@Slf4j
@SuppressWarnings("unchecked")
public class SQSSourceIntegrationTest extends AbstractAwsConnector {

    private static final String PULSAR_TOPIC = "test-sqs-source-topic";
    private static final String PULSAR_SUB_NAME = "test-sqs-source-validator";
    private static final String MSG = "hello-message-";
    private static final String SQS_QUEUE_NAME = "test-queue-source";

    private AmazonSQSBufferedAsyncClient client;

    private String queueUrl;

    @Test
    public void testSQSSourcePushMsgToPulsar() throws IOException {

        // prepare sqs client
        try {
            prepareSQSClient();
        } catch (Exception e) {
            Assert.assertNull("prepare sqs client should not throw exception", e);
        }

        // send test messages to SQS
        produceMessagesToSQS();

        // test if source pushed message to pulsar successfully
        try {
            validateSourceResult();
        } catch (Exception e) {
            Assert.assertNull("validate source result should not throw exception", e);
        }

        // clean up
        cleanupSQSClient();
    }

    public void prepareSQSClient() throws Exception {
        SQSConnectorConfig config = getTestConfig();
        config.setQueueName(SQS_QUEUE_NAME);
        AwsCredentialProviderPlugin credentialsProvider = createCredentialProvider(
                config.getAwsCredentialPluginName(),
                config.getAwsCredentialPluginParam());

        client = config.buildAmazonSQSClient(credentialsProvider);
        queueUrl = SQSUtils.ensureQueueExists(client, config.getQueueName());

        purgeSQSQueue(client, queueUrl);
    }

    public void cleanupSQSClient() {
        queueUrl = null;
        client.flush();
        client.shutdown();
        client = null;
    }

    public void produceMessagesToSQS() {
        for (int i = 0; i < 100; i++) {
            final SendMessageRequest request = new SendMessageRequest();
            request.withMessageBody(MSG + i).withQueueUrl(queueUrl);
            client.sendMessageAsync(request);
        }
    }

    public void validateSourceResult() throws Exception {
        @Cleanup
        PulsarClient pulsarClient = PulsarClient.builder()
                .serviceUrl("http://localhost:8080")
                .build();

        @Cleanup
        Consumer<byte[]> pulsarConsumer = pulsarClient.newConsumer()
                .topic(PULSAR_TOPIC)
                .subscriptionName(PULSAR_SUB_NAME)
                .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                .subscribe();

        int recordsNumber = 0;
        Message<byte[]> msg = pulsarConsumer.receive(2, TimeUnit.SECONDS);
        while (msg != null) {
            final String key = msg.getKey();
            final String value = new String(msg.getValue());
            log.info("Received message: key = {}, value = {}.", key, value);
            Assert.assertTrue(value.contains(MSG));
            pulsarConsumer.acknowledge(msg);
            recordsNumber++;
            msg = pulsarConsumer.receive(2, TimeUnit.SECONDS);
        }
        Assert.assertEquals(100, recordsNumber);
        pulsarConsumer.close();
        pulsarClient.close();
    }
}
