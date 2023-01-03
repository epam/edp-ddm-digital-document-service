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

import com.epam.digital.data.platform.integration.ceph.factory.CephS3Factory;
import com.epam.digital.data.platform.storage.file.config.FileDataCephStorageConfiguration;
import com.epam.digital.data.platform.storage.file.factory.FileStorageServiceFactory;
import com.epam.digital.data.platform.storage.file.repository.FileRepository;
import com.epam.digital.data.platform.storage.file.service.FileStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The configurations that contain and configures beans for form data file storage service.
 */
@Configuration
@Import(FormDataFileServiceConfig.class)
public class FileServiceConfig {

  @Bean
  public FileStorageServiceFactory fileStorageServiceFactory(CephS3Factory cephS3Factory) {
    return new FileStorageServiceFactory(cephS3Factory);
  }

  @Bean
  public FileStorageService fileStorageService(
      FileStorageServiceFactory factory,
      FileDataCephStorageConfiguration config) {
    return factory.fileStorageService(config);
  }

  @Bean
  @ConditionalOnMissingBean
  public FileRepository fileRepository(FileStorageServiceFactory factory,
      FileDataCephStorageConfiguration config) {
    return factory.newFileRepository(config);
  }
}
