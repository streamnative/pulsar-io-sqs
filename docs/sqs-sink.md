---
description: The AWS SQS sink connector pulls messages from Pulsar topics and persists messages to Amazon AWS SQS.
author: ["StreamNative"]
contributors: ["StreamNative"]
language: Java
document: 
source: "https://github.com/streamnative/pulsar-io-sqs/tree/v2.7.0"
license: Apache License 2.0
tags: ["Pulsar IO", "AWS", "SQS", "Sink"]
alias: AWS SQS Sink
features: ["Use SQS sink connector to sync data from Pulsar"]
license_link: "https://www.apache.org/licenses/LICENSE-2.0"
icon: "/images/connectors/aws_sqs.png"
download: "https://github.com/streamnative/pulsar-io-sqs/releases/download/v2.7.0/pulsar-io-sqs-2.7.0.nar"
support: StreamNative
support_link: https://streamnative.io
support_img: "/images/connectors/streamnative.png"
dockerfile:
owner_name: "StreamNative"
owner_img: "/images/streamnative.png"
id: "sqs-sink"
---

# AWS SQS sink connector

The [AWS Simple Queue Service (SQS)](https://aws.amazon.com/sqs/?nc1=h_ls) sink connector pulls messages from Pulsar topics and writes messages to AWS SQS.

## Installation

To install the SQS sink connector, follow these steps.

1. [Download](https://github.com/streamnative/pulsar-io-sqs/releases/download/v2.7.0/pulsar-io-sqs-2.7.0.nar) the NAR package of the SQS sink connector.

2. Copy the NAR package to the Pulsar connectors directory.

    ```
    cp pulsar-io-sqs-2.7.0.nar $PULSAR_HOME/connectors/pulsar-io-sqs-2.7.0.nar
    ```

3. Start Pulsar in standalone mode.

    ```
    PULSAR_HOME/bin/pulsar standalone
    ```

4. Run the SQS sink connector locally.

    ```
    PULSAR_HOME/bin/pulsar-admin sink localrun \
    --sink-config-file sqs-sink-config.yaml
    ```

## Configuration

Follow the guidelines below to configure the SQS sink connector.

### SQS source connector configuration

The configuration of the SQS source connector has the following properties.

| Name | Type|Required | Default | Description
|------|----------|----------|---------|-------------|
| `awsEndpoint` |String| false | "tcp" | AWS SQS end-point URL. It can be found at [here](https://docs.aws.amazon.com/general/latest/gr/rande.html). |
| `awsRegion` | String| true | " " (empty string) | Appropriate AWS region. For example, us-west-1, us-west-2. |
| `awsCredentialPluginName` | String|false | " " (empty string) | Fully-qualified class name of implementation of `AwsCredentialProviderPlugin`. |
| `awsCredentialPluginParam` | String|true | " " (empty string) | JSON parameter to initialize `AwsCredentialsProviderPlugin`. |
| `queueName` | String|true | " " (empty string) | SQS queue name that messages should be read from or written to. |

### Configure SQS sink connector

Before using the SQS sink connector, you need to create a configuration file through one of the following methods.

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

This section describes how to use the SQS sink connector to pull messages from Pulsar topics to AWS SQS.

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

5. Send a message to a Pulsar topic.

    ```
    PULSAR_HOME/bin/pulsar-client produce public/default/test-queue-pulsar --messages hello -n 10
    ```

6. Receive the message from the SQS queue using the [AWS SQS CLI tool](https://aws.amazon.com/cli/).

   ```
   aws sqs receive-message --queue-url ${QUEUE_URL} --max-number-of-messages 10
   ```

    Now you can see 10 messages from the AWS SQS CLI terminal window.