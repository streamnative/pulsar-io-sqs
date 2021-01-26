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

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;

/**
 * SQSUtils defines utils for AWS SQS connector.
*/
public class SQSUtils {
    public static String ensureQueueExists(AmazonSQS client, String queueName) throws AmazonClientException {
        String queueUrl = queueExists(client, queueName);
        if (queueUrl == null) {
            queueUrl = client.createQueue(queueName).getQueueUrl();
        }
        return queueUrl;
    }

    public static String queueExists(AmazonSQS client, String queueName) throws AmazonClientException {
        try {
            return client.getQueueUrl(new GetQueueUrlRequest(queueName)).getQueueUrl();
        } catch (QueueDoesNotExistException e) {
            return null;
        }
    }
}
