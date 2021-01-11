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

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import lombok.Data;
import org.apache.pulsar.io.aws.AwsCredentialProviderPlugin;
import org.apache.pulsar.io.core.annotations.FieldDoc;

/**
 * The configuration class for {@link SQSSink} and {@link SQSSource}.
 */
@Data
public class SQSConnectorConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @FieldDoc(
            required = false,
            defaultValue = "",
            help = "AWS SQS end-point url. It can be found at https://docs.aws.amazon.com/general/latest/gr/rande.html"
    )
    private String awsEndpoint = "";

    @FieldDoc(
            required = true,
            defaultValue = "",
            help = "Appropriate aws region. E.g. us-west-1, us-west-2"
    )
    private String awsRegion;

    @FieldDoc(
            required = true,
            defaultValue = "",
            help = "The SQS queue name that messages should be read from or written to."
    )
    private String queueName;

    @FieldDoc(
            required = false,
            defaultValue = "",
            help = "Fully-Qualified class name of implementation of AwsCredentialProviderPlugin."
                    + " It is a factory class which creates an AWSCredentialsProvider that will be used by sqs client."
                    + " If it is empty then sqs client will create a default AWSCredentialsProvider which accepts json"
                    + " of credentials in `awsCredentialPluginParam`")
    private String awsCredentialPluginName = "";

    @FieldDoc(
            required = false,
            defaultValue = "",
            help = "json-parameters to initialize `AwsCredentialsProviderPlugin`")
    private String awsCredentialPluginParam = "";

    public static SQSConnectorConfig load(Map<String, Object> map) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new ObjectMapper().writeValueAsString(map), SQSConnectorConfig.class);
    }

    public AmazonSQS buildAmazonSQSClient(AwsCredentialProviderPlugin credPlugin) {
        AmazonSQSClientBuilder builder =  AmazonSQSClientBuilder.standard();

        if (!this.getAwsEndpoint().isEmpty()) {
            builder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                    this.getAwsEndpoint(),
                    this.getAwsRegion()));
        } else if (!this.getAwsRegion().isEmpty()) {
            builder.setRegion(this.getAwsRegion());
        }
        builder.setCredentials(credPlugin.getCredentialProvider());
        return builder.build();
    }

}
