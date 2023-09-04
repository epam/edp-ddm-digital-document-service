/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.dgtldcmnt.listener.kafka;

import static org.awaitility.Awaitility.await;

import com.amazonaws.util.StringInputStream;
import com.epam.digital.data.platform.bpms.api.dto.FileStorageCleanupDto;
import com.epam.digital.data.platform.dgtldcmnt.BaseIT;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

class FileCleanerListenerIT extends BaseIT {

  @Autowired
  private KafkaTemplate<String, Object> kafkaTemplate;
  @Autowired
  private CephService cephService;


  @Value("#{kafkaProperties.topics.get('lowcode-file-storage-cleanup-topic')}")
  private String topic;
  @Value("${ceph.bucket}")
  private String bucket;

  @Test
  @SneakyThrows
  void processFileCleanupMessage() {
    var processInstanceId = UUID.randomUUID().toString();
    var fileId = UUID.randomUUID().toString();
    var fileCephKey = String.format("process/%s/%s", processInstanceId, fileId);
    var fileInputStream = new StringInputStream("some file content");
    cephService.put(bucket, fileCephKey, "text/plain", Map.of(), fileInputStream);

    var dto = new FileStorageCleanupDto(processInstanceId);
    kafkaTemplate.send(topic, dto);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .pollDelay(100, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> Assertions.assertThat(cephService.get(bucket, fileCephKey)).isEmpty());
  }
}
