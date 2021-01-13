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
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.ecosystem.io.sqs.SQSConnectorConfig;
import org.apache.pulsar.ecosystem.io.sqs.SQSSink;
import org.apache.pulsar.ecosystem.io.sqs.SQSUtils;
import org.apache.pulsar.io.aws.AbstractAwsConnector;
import org.apache.pulsar.io.aws.AwsCredentialProviderPlugin;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration test {@link SQSSink}.
 */
@Slf4j
@SuppressWarnings("unchecked")
public class SQSSinkIntegrationTest extends AbstractAwsConnector {

    private static final String PULSAR_TOPIC = "test-sqs-sink-topic";
    private static final String PULSAR_PRODUCER_NAME = "test-sqs-sink-producer";
    private static final String MSG = "hello-message-";
    private static final String SQS_QUEUE_NAME = "test-queue-sink";

    private AmazonSQSBufferedAsyncClient client;

    private String queueUrl;

    @Test
    public void testSQSSinkPushMsgToSQSQueue() {

        // prepare sqs client
        try {
            prepareSQSClient();
        } catch (Exception e) {
            Assert.assertNull("prepare sqs client should not throw exception", e);
        }

        // send test messages to Pulsar
        try {
            produceMessagesToPulsar();
        } catch (Exception e) {
            Assert.assertNull("produce test messages to pulsar should not throw exception", e);
        }

        // test if sink pushed message to sqs successfully
        validateSinkResult();

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

    public void produceMessagesToPulsar() throws Exception {
        @Cleanup
        PulsarClient pulsarClient = PulsarClient.builder()
                .serviceUrl("http://localhost:8080")
                .build();

        @Cleanup
        Producer<String> pulsarProducer = pulsarClient.newProducer(Schema.STRING)
                .topic(PULSAR_TOPIC)
                .producerName(PULSAR_PRODUCER_NAME)
                .create();

        for (int i = 0; i < 100; i++) {
            pulsarProducer.newMessage().value(MSG + i).sendAsync();
        }

        pulsarProducer.close();
        pulsarClient.close();
    }

    public void validateSinkResult() {
        Callable<Boolean> callableTask = () -> {
            final ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl)
                    .withWaitTimeSeconds(2);

            int recordsNumber = 0;
            while (recordsNumber < 100) {
                List<Message> msgs = client.receiveMessage(request).getMessages();

                msgs.forEach(msg -> {
                    log.info("Received message: id = {}, body = {}.", msg.getMessageId(), msg.getBody());
                    Assert.assertTrue(msg.getBody().contains(MSG));
                    final DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest()
                            .withQueueUrl(queueUrl)
                            .withReceiptHandle(msg.getReceiptHandle());

                    client.deleteMessageAsync(deleteMessageRequest);
                });

                recordsNumber += msgs.size();
            }

            Assert.assertEquals(100, recordsNumber);
            client.flush();

            return true;
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<Future<Boolean>> result = executor.invokeAll(
                    Collections.singletonList(callableTask), 10, TimeUnit.SECONDS);
            Assert.assertTrue("validate sink result task should finished", result.stream().allMatch(Future::isDone));
        } catch (Exception e) {
            Assert.assertNull("validate sink result should not throw exception", e);
        }
        executor.shutdown();
    }
}
