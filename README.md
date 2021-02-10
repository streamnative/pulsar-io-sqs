# AWS SQS connector

The [AWS Simple Queue Service (SQS)](https://aws.amazon.com/sqs/?nc1=h_ls) connector is a [Pulsar IO connector](http://pulsar.apache.org/docs/en/next/io-overview/) for copying data between Amazon AWS SQS and Pulsar.

Currently, SQS connector versions (`x.y.z`) are based on Pulsar versions (`x.y.z`).

| SQS connector version | Pulsar version | Doc |
| :---------- | :------------------- | :------------- |
[2.7.0](https://github.com/streamnative/pulsar-io-sqs/releases/tag/v2.7.0)| [2.7.0](http://pulsar.apache.org/en/download/) | - [SQS source connector](https://github.com/streamnative/pulsar-io-sqs/blob/master/docs/sqs-source.md)<br>- [SQS sink connector](https://github.com/streamnative/pulsar-io-sqs/blob/master/docs/sqs-sink.md)

## Project layout

Below are the sub folders and files of this project and their corresponding descriptions.

```bash

├── conf // examples of configuration files of this connector
├── docs // user guides of this connector
├── script // scripts of this connector
├── src // source code of this connector
│   ├── checkstyle // checkstyle configuration files of this connector
│   ├── license // license header for this project. `mvn license:format` can
    be used for formatting the project with the stored license header in this directory
│   │   └── ALv2
│   ├── main // main source files of this connector
│   │   └── java
│   ├── spotbugs // spotbugs configuration files of this connector
│   └── test // test related files of this connector
│       └── java

```

## License
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Fstreamnative%2Fpulsar-io-sqs.svg?type=large)](https://app.fossa.io/projects/git%2Bgithub.com%2Fstreamnative%2Fpulsar-io-sqs?ref=badge_large)
