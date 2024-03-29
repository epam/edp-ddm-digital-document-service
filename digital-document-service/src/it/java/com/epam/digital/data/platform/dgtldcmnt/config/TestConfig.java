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

package com.epam.digital.data.platform.dgtldcmnt.config;

import com.epam.digital.data.platform.dgtldcmnt.BaseIT;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.storage.file.repository.CephFormDataFileRepository;
import com.epam.digital.data.platform.storage.file.repository.FileRepository;
import com.epam.digital.data.platform.storage.file.repository.FileRepositoryImpl;
import com.epam.digital.data.platform.storage.file.repository.FormDataFileRepository;
import com.epam.digital.data.platform.storage.file.service.FileStorageService;
import com.epam.digital.data.platform.storage.file.service.FormDataFileKeyProviderImpl;
import com.epam.digital.data.platform.storage.file.service.FormDataFileStorageService;
import com.google.common.io.ByteStreams;
import java.util.Objects;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TestConfig {

  @Bean
  public CephService s3ObjectCephService() {
    return new TestS3ObjectCephService();
  }

  @Bean
  public FormDataFileRepository formDataFileRepository(
      @Value("${ceph.bucket}") String bucket) {
    return CephFormDataFileRepository.builder()
        .cephService(s3ObjectCephService())
        .cephBucketName(bucket)
        .build();
  }

  @Bean
  @Primary
  public FormDataFileStorageService fromDataFileStorageService(
      FormDataFileRepository formDataFileRepository) {
    return FormDataFileStorageService.builder()
        .keyProvider(new FormDataFileKeyProviderImpl())
        .repository(formDataFileRepository)
        .build();
  }

  @Bean
  public FileRepository FileRepository(@Value("${ceph.bucket}") String bucket) {
    return FileRepositoryImpl.builder()
        .cephService(s3ObjectCephService())
        .cephBucketName(bucket)
        .build();
  }

  @Bean
  @Primary
  public FileStorageService FileStorageService(FileRepository fileRepository) {
    return FileStorageService.builder()
        .keyProvider(new FormDataFileKeyProviderImpl())
        .repository(fileRepository)
        .build();
  }

  @Bean
  @SneakyThrows
  public String accessToken() {
    return new String(ByteStreams.toByteArray(
        Objects.requireNonNull(BaseIT.class.getResourceAsStream("/json/accessToken.json"))));
  }
}
