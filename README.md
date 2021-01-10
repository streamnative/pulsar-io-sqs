## Pulsar IO :: AWS SQS Connector

pulsar-io-sqs is a [Pulsar IO Connector](http://pulsar.apache.org/docs/en/io-overview/) for copying data between Amazon AWS SQS and Pulsar.

### Get started

This section provides a step-by-step example how to use this AWS SQS connector, include using SQS source connector to copy queue data from a SQS queue to a Pulsar topic, and using SQS sink connector to copy topic data from a Pulsar topic to a SQS queue.

#### Build pulsar-io-sqs connector

1. Git clone `pulsar-io-sqs`. Assume *PULSAR_IO_SQS_HOME* is the home directory for your
   cloned `pulsar-io-sqs` repo for the remaining steps.
   ```
   $ git clone https://github.com/streamnative/pulsar-io-sqs
   ```

2. Build the connector in `${PULSAR_IO_SQS_HOME}` directory.
   ```
   $ mvn clean install -DskipTests
   ```
   After successfully built the connector, a *NAR* package is generated under *target* directory. The *NAR* package is the one you can applied to Pulsar.
   ```
   $ ls target/pulsar-io-sqs-2.7.0-SNAPSHOT.nar
   target/pulsar-io-sqs-2.7.0-SNAPSHOT.nar
   ```
    
#### Prepare a config for using pulsar-io-sqs connector

An example yaml config is available [here](https://github.com/streamnative/pulsar-io-sqs/blob/master/conf/pulsar-io-sqs.yaml)

This example yaml config is used for both source connector and sink connector.

#### Run pulsar-io-sqs source connector
[TBD]

#### Run pulsar-io-sqs sink connector
[TBD]

