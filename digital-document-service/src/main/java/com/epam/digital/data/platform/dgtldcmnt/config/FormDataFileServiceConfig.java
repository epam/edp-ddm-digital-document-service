package com.epam.digital.data.platform.dgtldcmnt.config;

import com.epam.digital.data.platform.integration.ceph.factory.CephS3Factory;
import com.epam.digital.data.platform.storage.file.config.FileDataCephStorageConfiguration;
import com.epam.digital.data.platform.storage.file.factory.FormDataFileStorageServiceFactory;
import com.epam.digital.data.platform.storage.file.repository.FormDataFileRepository;
import com.epam.digital.data.platform.storage.file.service.FormDataFileStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The configurations that contain and configures beans for form data file storage service.
 */
@Configuration
@Import(CephConfig.class)
public class FormDataFileServiceConfig {

  @Bean
  public FormDataFileStorageServiceFactory formDataFileStorageServiceFactory(CephS3Factory cephS3Factory) {
    return new FormDataFileStorageServiceFactory(cephS3Factory);
  }

  @Bean
  public FormDataFileStorageService formDataFileStorageService(
      FormDataFileStorageServiceFactory factory,
      FileDataCephStorageConfiguration config) {
    return factory.fromDataFileStorageService(config);
  }

  @Bean
  @ConditionalOnMissingBean
  public FormDataFileRepository formDataFileRepository(FormDataFileStorageServiceFactory factory,
      FileDataCephStorageConfiguration config) {
    return factory.newCephFormDataFileRepository(config);
  }
}
