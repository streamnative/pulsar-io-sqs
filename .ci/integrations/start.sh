#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

set -e

SRC_DIR=$(git rev-parse --show-toplevel)
cd $SRC_DIR

IMAGE_NAME=pulsar-io-sqs-test:latest
MVN_VERSION=`${SRC_DIR}/.ci/versions/get-project-version.py`

docker build -t ${IMAGE_NAME} .

docker network create sqs-test

docker kill pulsar-io-sqs-test || true
docker run  --network sqs-test -d --rm --name pulsar-io-sqs-test \
            -p 8080:8080 \
            -p 6650:6650 \
            -p 8443:8843 \
            -p 6651:6651 \
            ${IMAGE_NAME}

PULSAR_ADMIN="docker exec -d pulsar-io-sqs-test /pulsar/bin/pulsar-admin"

echo "-- Wait for Pulsar service to be ready"
until curl http://localhost:8080/metrics > /dev/null 2>&1 ; do sleep 1; done

echo "-- Pulsar service ready to test"

docker kill localstack || true
docker run  --network sqs-test -d --rm --name localstack \
            -p 4566:4566 \
            -e SERVICES=sqs \
            -e DEFAULT_REGION=us-east-1 \
            -e HOSTNAME_EXTERNAL=localstack \
            localstack/localstack:latest

sudo echo "127.0.0.1 localstack" | sudo tee -a /etc/hosts
echo "-- Wait for localstack service to be ready"
until $(curl --silent --fail http://localstack:4566/health | grep "\"sqs\": \"running\"" > /dev/null);  do sleep 1; done
echo "-- localstack service ready to test"

docker ps

# run connector
echo "-- run pulsar-io-sqs source connector"
$PULSAR_ADMIN sources localrun -a /pulsar-io-sqs/target/pulsar-io-sqs-${MVN_VERSION}.nar \
        --tenant public --namespace default --name test-sqs-source \
        --source-config-file /pulsar-io-sqs/.ci/test-pulsar-io-sqs-source.yaml \
        --destination-topic-name test-sqs-source-topic

echo "-- run pulsar-io-sqs sink connector"
$PULSAR_ADMIN sinks localrun -a /pulsar-io-sqs/target/pulsar-io-sqs-${MVN_VERSION}.nar \
        --tenant public --namespace default --name test-sqs-sink \
        --sink-config-file /pulsar-io-sqs/.ci/test-pulsar-io-sqs-sink.yaml \
        -i test-sqs-sink-topic

echo "-- ready to do integration tests"