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

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.util.StringUtils;
import java.util.Map;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.functions.api.Record;
import org.apache.pulsar.io.aws.AbstractAwsConnector;
import org.apache.pulsar.io.aws.AwsCredentialProviderPlugin;
import org.apache.pulsar.io.core.Sink;
import org.apache.pulsar.io.core.SinkContext;


/**
 * A source connector for AWS SQS.
 */
@Slf4j
public class SQSSink extends AbstractAwsConnector implements Sink<byte[]> {
    @Getter
    private SQSConnectorConfig config;

    private SQSConnection connection;
    private Session session;
    private MessageProducer producer;

    @Override
    public void open(Map<String, Object> map, SinkContext sinkContext) throws Exception {
        if (config != null || connection != null) {
            throw new IllegalStateException("Connector is already open");
        }

        config = SQSConnectorConfig.load(map);

        AwsCredentialProviderPlugin credentialsProvider = createCredentialProvider(
                config.getAwsCredentialPluginName(),
                config.getAwsCredentialPluginParam());

        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
                new ProviderConfiguration(),
                config.buildAmazonSQSClient(credentialsProvider)
        );

        connection = connectionFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination;

        if (!StringUtils.isNullOrEmpty(config.getQueueName())) {
            SQSUtils.ensureQueueExists(connection, config.getQueueName());
            destination = session.createQueue(config.getQueueName());
        } else {
            throw new Exception("destination is null.");
        }

        producer = session.createProducer(destination);
    }

    @Override
    public void write(Record<byte[]> record) {
        try {
            TextMessage message = session.createTextMessage(new String(record.getValue(), UTF_8));
            producer.send(message);
            record.ack();
        } catch (Exception e) {
            log.error("failed send message to AWS SQS.");
            record.fail();
        }
    }

    @Override
    public void close() throws Exception {
        if (producer != null) {
            producer.close();
        }
        if (session != null) {
            session.close();
        }
        if (connection != null) {
            connection.close();
        }
    }
}
