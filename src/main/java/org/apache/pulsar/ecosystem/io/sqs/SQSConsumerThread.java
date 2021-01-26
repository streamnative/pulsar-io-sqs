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

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.util.StringUtils;
import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.functions.api.Record;

/**
 * The sqs consumer thread class for {@link SQSSource}.
 */
@Slf4j
public class SQSConsumerThread extends Thread {

    private final SQSSource source;

    public SQSConsumerThread(SQSSource source) {
        this.source = source;
    }

    public void run() {
        while (true) {
            try {
                source.receive().forEachOrdered(this::process);
            } catch (Exception ex) {
                log.error("receive message from sqs error", ex);
            }
        }
    }

    private void process(Message message) {
        log.debug("process message [{}]", message.getBody());
        if (!StringUtils.isNullOrEmpty(message.getBody())) {
            try {
                source.consume(new SQSRecord(Optional.empty(), message.getBody().getBytes(UTF_8)));
                source.ack(message);
                log.debug("message received and deleted [{}]", message.getBody());
            } catch (Exception ex) {
                source.fail(message);
                log.warn("process message with exception, uacked message.", ex);
            }
        } else {
            source.ack(message);
        }
    }

    @Data
    private static class SQSRecord implements Record<byte[]> {
        private final Optional<String> key;
        private final byte[] value;
    }

}
