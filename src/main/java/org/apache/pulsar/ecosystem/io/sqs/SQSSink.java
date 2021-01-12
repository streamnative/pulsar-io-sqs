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
import static java.nio.charset.StandardCharsets.UTF_8;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.functions.api.Record;
import org.apache.pulsar.io.core.Sink;
import org.apache.pulsar.io.core.SinkContext;

/**
 * A source connector for AWS SQS.
 */
@Slf4j
public class SQSSink extends SQSAbstractConnector implements Sink<byte[]> {
    private SinkContext sinkContext;

    private static final String METRICS_TOTAL_SUCCESS = "_sqs_sink_total_success_";
    private static final String METRICS_TOTAL_FAILURE = "_sqs_sink_total_failure_";

    @Override
    public void open(Map<String, Object> map, SinkContext sinkContext) throws Exception {
        this.sinkContext = sinkContext;
        setConfig(SQSConnectorConfig.load(map));
        prepareSqsClient();
    }

    @Override
    public void write(Record<byte[]> record) {
        try {
            send(new String(record.getValue(), UTF_8));
            record.ack();
            if (sinkContext != null) {
                sinkContext.recordMetric(METRICS_TOTAL_SUCCESS, 1);
            }
        } catch (Exception e) {
            log.error("failed send message to AWS SQS.", e);
            record.fail();
            if (sinkContext != null) {
                sinkContext.recordMetric(METRICS_TOTAL_FAILURE, 1);
            }
        }
    }

    public void send(String msgBody) {
        final SendMessageRequest request = new SendMessageRequest();
        request.withMessageBody(msgBody).withQueueUrl(getQueueUrl());
        getClient().sendMessageAsync(request);
    }

    @Override
    public void close() {
        if (getClient() != null) {
            getClient().shutdown();
        }
    }
}
