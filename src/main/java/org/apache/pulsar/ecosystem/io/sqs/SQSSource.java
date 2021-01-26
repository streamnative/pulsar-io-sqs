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
/*
 * Classes for implementing a pulsar IO connector for AWS SQS.
 */
package org.apache.pulsar.ecosystem.io.sqs;

import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.functions.api.Record;
import org.apache.pulsar.io.core.Source;
import org.apache.pulsar.io.core.SourceContext;

/**
 * A source connector for AWS SQS.
 */
@Slf4j
public class SQSSource extends SQSAbstractConnector implements Source<byte[]> {

    private static final int DEFAULT_QUEUE_LENGTH = 1000;
    private static final Integer MAX_WAIT_TIME = 20;
    private static final String METRICS_TOTAL_SUCCESS = "_sqs_source_total_success_";
    private static final String METRICS_TOTAL_FAILURE = "_sqs_source_total_failure_";
    private SourceContext sourceContext;
    private ExecutorService executor;
    private LinkedBlockingQueue<Record<byte[]>> queue;

    @Override
    public void open(Map<String, Object> map, SourceContext sourceContext) throws Exception {
        this.sourceContext = sourceContext;
        setConfig(SQSConnectorConfig.load(map));
        prepareSqsClient();

        queue = new LinkedBlockingQueue<>(this.getQueueLength());

        executor = Executors.newFixedThreadPool(1);
        executor.execute(new SQSConsumerThread(this));
    }

    public Stream<Message> receive() {
        final ReceiveMessageRequest request = new ReceiveMessageRequest(getQueueUrl())
                .withWaitTimeSeconds(MAX_WAIT_TIME);
        return getClient().receiveMessage(request).getMessages().stream();
    }

    public void fail(Message message) {
        final ChangeMessageVisibilityRequest request = new ChangeMessageVisibilityRequest()
                .withQueueUrl(getQueueUrl())
                .withReceiptHandle(message.getReceiptHandle())
                .withVisibilityTimeout(MAX_WAIT_TIME);

        getClient().changeMessageVisibilityAsync(request);
        if (sourceContext != null) {
            sourceContext.recordMetric(METRICS_TOTAL_FAILURE, 1);
        }
    }

    public void ack(Message message) {
        final DeleteMessageRequest request = new DeleteMessageRequest()
                .withQueueUrl(getQueueUrl())
                .withReceiptHandle(message.getReceiptHandle());

        getClient().deleteMessageAsync(request);
        if (sourceContext != null) {
            sourceContext.recordMetric(METRICS_TOTAL_SUCCESS, 1);
        }
    }

    @Override
    public Record<byte[]> read() throws Exception {
        return this.queue.take();
    }

    public void consume(Record<byte[]> record) throws InterruptedException {
        this.queue.put(record);
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        if (getClient() != null) {
            getClient().shutdown();
        }
    }

    public int getQueueLength() {
        return DEFAULT_QUEUE_LENGTH;
    }

}

