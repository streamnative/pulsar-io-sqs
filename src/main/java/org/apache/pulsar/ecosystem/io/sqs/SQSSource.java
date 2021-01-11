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
import static java.nio.charset.StandardCharsets.UTF_8;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.functions.api.Record;
import org.apache.pulsar.io.aws.AbstractAwsConnector;
import org.apache.pulsar.io.aws.AwsCredentialProviderPlugin;
import org.apache.pulsar.io.core.Source;
import org.apache.pulsar.io.core.SourceContext;


/**
 * A source connector for AWS SQS.
 */
@Slf4j
public class SQSSource extends AbstractAwsConnector implements Source<byte[]> {

    private LinkedBlockingQueue<Record<byte[]>> queue;
    private static final int DEFAULT_QUEUE_LENGTH = 1000;

    @Getter
    private SQSConnectorConfig config;

    private SQSConnection connection;
    private Session session;
    private MessageConsumer consumer;

    @Override
    public void open(Map<String, Object> map, SourceContext sourceContext) throws Exception {
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

        queue = new LinkedBlockingQueue<> (this.getQueueLength());

        connection = connectionFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        SQSUtils.ensureQueueExists(connection, config.getQueueName());
        Destination destination;

        if (!StringUtils.isNullOrEmpty(config.getQueueName())) {
            SQSUtils.ensureQueueExists(connection, config.getQueueName());
            destination = session.createQueue(config.getQueueName());
        } else {
            throw new Exception("destination is null.");
        }
        consumer = session.createConsumer(destination);
        consumer.setMessageListener(new MessageListenerImpl(this));
    }

    @Override
    public Record<byte[]> read() throws Exception {
        return this.queue.take();
    }

    @Override
    public void close() throws Exception {
        if (consumer != null) {
            consumer.close();
        }
        if (session != null) {
            session.close();
        }
        if (connection != null) {
            connection.close();
        }
    }

    public void consume(Record<byte[]> record) {
        try {
            this.queue.put(record);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int getQueueLength() {
        return DEFAULT_QUEUE_LENGTH;
    }

    @Data
    private static class SQSRecord implements Record<byte[]> {
        private final Optional<String> key;
        private final byte[] value;
    }

    private static class MessageListenerImpl implements MessageListener {

        private final SQSSource source;

        public MessageListenerImpl(SQSSource source) {
            this.source = source;
        }

        @Override
        public void onMessage(Message message) {
            try {
                if (message instanceof TextMessage) {
                    TextMessage txtMessage = (TextMessage) message;
                    byte[] bytes = txtMessage.getText().getBytes(UTF_8);
                    source.consume(new SQSRecord(Optional.empty(), bytes));
                } else if (message instanceof BytesMessage) {
                    BytesMessage byteMessage = (BytesMessage) message;
                    byte[] bytes = new byte[(int) byteMessage.getBodyLength()];
                    byteMessage.readBytes(bytes);
                    source.consume(new SQSRecord(Optional.empty(), bytes));
                }
            } catch (Exception ex) {
                log.error("failed to read AWS SQS message.");
            }
        }
    }
}

