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

import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.io.aws.AbstractAwsConnector;
import org.apache.pulsar.io.aws.AwsCredentialProviderPlugin;

/**
 * Abstract Class for pulsar connector to AWS SQS.
 */
@Slf4j
public abstract class SQSAbstractConnector extends AbstractAwsConnector {
    @Getter
    @Setter
    private SQSConnectorConfig config;

    @Getter
    private AmazonSQSBufferedAsyncClient client;

    @Getter
    private String queueUrl;

    public void prepareSqsClient() throws Exception {
        if (config == null) {
            throw new IllegalStateException("Configuration not set");
        }
        if (client != null) {
            throw new IllegalStateException("Connector is already open");
        }

        AwsCredentialProviderPlugin credentialsProvider = createCredentialProvider(
                config.getAwsCredentialPluginName(),
                config.getAwsCredentialPluginParam());

        client = config.buildAmazonSQSClient(credentialsProvider);

        queueUrl = SQSUtils.ensureQueueExists(client, config.getQueueName());
    }
}
