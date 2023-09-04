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

import com.epam.digital.data.platform.bpms.api.dto.FileStorageCleanupDto;
import com.epam.digital.data.platform.dgtldcmnt.facade.DocumentFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "data-platform", name = "kafka.consumer.enabled", havingValue = "true")
@RequiredArgsConstructor
public class FileCleanerListener {

  private final DocumentFacade documentFacade;

  @KafkaListener(topics = "#{kafkaProperties.topics.get('lowcode-file-storage-cleanup-topic')}",
      groupId = "#{kafkaProperties.consumer.groupId}")
  public void processFileCleanupMessages(@Payload FileStorageCleanupDto fileStorageCleanupDto) {
    var processInstanceId = fileStorageCleanupDto.getProcessInstanceId();
    documentFacade.delete(processInstanceId);
  }
}
