---
description: The AWS SQS source connector receives messages from Amazon AWS SQS and writes messages to Pulsar topics.
author: ["StreamNative"]
contributors: ["StreamNative"]
language: Java
document: 
source: "https://github.com/streamnative/pulsar-io-sqs/tree/v2.7.0"
license: Apache License 2.0
tags: ["Pulsar IO", "AWS", "SQS", "Source"]
alias: AWS SQS Source
features: ["Use SQS source connector to sync data to Pulsar"]
license_link: "https://www.apache.org/licenses/LICENSE-2.0"
icon: "/images/connectors/aws_sqs.png"
download: "https://github.com/streamnative/pulsar-io-sqs/releases/download/v2.7.0/pulsar-io-sqs-2.7.0.nar"
support: StreamNative
support_link: https://streamnative.io
support_img: "/images/connectors/streamnative.png"
dockerfile:
owner_name: "StreamNative"
owner_img: "/images/streamnative.png"
id: "sqs-source"
---

# AWS SQS source connector

The [AWS Simple Queue Service (SQS)](https://aws.amazon.com/sqs/?nc1=h_ls) source connector receives messages from Amazon AWS SQS and writes messages to Pulsar topics.

## Installation

To install the SQS source connector, follow these steps.

1. [Download](https://github.com/streamnative/pulsar-io-sqs/releases/download/v2.7.0/pulsar-io-sqs-2.7.0.nar) the NAR package of the SQS source connector.

2. Copy the NAR package to the Pulsar connectors directory.

    ```
    cp pulsar-io-sqs-2.7.0.nar $PULSAR_HOME/connectors/pulsar-io-sqs-2.7.0.nar
    ```

3. Start Pulsar in standalone mode.

    ```
    PULSAR_HOME/bin/pulsar standalone
    ```

4. Run the SQS source connector locally.

    ```
    PULSAR_HOME/bin/pulsar-admin source localrun \
    --source-config-file sqs-source-config.yaml \
    --destination-topic-name test-queue-pulsar
    ```

## Configuration

Follow the guidelines below to configure the SQS source connector.

### SQS source connector configuration

The configuration of the SQS source connector has the following properties.

| Name | Type|Required | Default | Description
|------|----------|----------|---------|-------------|
| `awsEndpoint` |String| false | "tcp" | AWS SQS end-point URL. It can be found at [here](https://docs.aws.amazon.com/general/latest/gr/rande.html). |
| `awsRegion` | String| true | " " (empty string) | Appropriate AWS region. For example, us-west-1, us-west-2. |
| `awsCredentialPluginName` | String|false | " " (empty string) | Fully-qualified class name of implementation of `AwsCredentialProviderPlugin`. |
| `awsCredentialPluginParam` | String|true | " " (empty string) | JSON parameter to initialize `AwsCredentialsProviderPlugin`. |
| `queueName` | String|true | " " (empty string) | SQS queue name that messages should be read from or written to. |

### Configure SQS source connector

Before using the SQS source connector, you need to create a configuration file through one of the following methods.

* JSON 

    ```json
    {
        "tenant": "public",
        "namespace": "default",
        "name": "sqs-source",
        "topicName": "test-queue-pulsar",
        "archive": "connectors/pulsar-io-sqs-2.7.0.nar",
        "parallelism": 1,
        "configs":
        {
            "awsEndpoint": "https://dynamodb.us-west-2.amazonaws.com",
            "awsRegion": "us-east-1",
            "queueName": "test-queue",
            "awsCredentialPluginName": "",
            "awsCredentialPluginParam": '{"accessKey":"myKey","secretKey":"my-Secret"}',
        }
    }
    ```

* YAML

    ```yaml
   tenant: "public"
   namespace: "default"
   name: "sqs-source"
   topicName: "test-queue-pulsar"
   archive: "connectors/pulsar-io-sqs-2.7.0.nar"
   parallelism: 1

   configs:
      awsEndpoint: "https://dynamodb.us-west-2.amazonaws.com"
      awsRegion: "us-east-1"
      queueName: "test-queue"
      awsCredentialPluginName: ""
      awsCredentialPluginParam: '{"accessKey":"myKey","secretKey":"my-Secret"}'
    ```

## Usage

This section describes how to use the SQS source connector to receive messages from AWS SQS and write messages to Pulsar topics.

1. [Prepare SQS service](https://aws.amazon.com/sqs/getting-started/).

2. Copy the NAR package to the Pulsar connectors directory.

    ```
    cp pulsar-io-sqs-2.7.0.nar $PULSAR_HOME/connectors/pulsar-io-sqs-2.7.0.nar
    ```

3. Start Pulsar in standalone mode.

    ```
    PULSAR_HOME/bin/pulsar standalone
    ```

4. Run the SQS source connector locally.

    ```
    PULSAR_HOME/bin/pulsar-admin source localrun \
    --source-config-file sqs-source-config.yaml
    ```

5. Consume the message from the Pulsar topic.

    ```
    PULSAR_HOME/bin/pulsar-client consume -s "sub-products" public/default/test-queue-pulsar -n 0
    ```

6. Send a message to the SQS queue using the [AWS SQS CLI tool](https://aws.amazon.com/cli/). 

   ```
   aws sqs send-message --queue-url ${QUEUE_URL} --message-body "Hello From SQS"
   ```
 
    Now you can see the message "Hello From SQS" from the Pulsar consumer.


