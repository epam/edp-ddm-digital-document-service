package com.epam.digital.data.platform.dgtldcmnt.config;

import com.epam.digital.data.platform.integration.ceph.config.S3ConfigProperties;
import com.epam.digital.data.platform.integration.ceph.factory.CephS3Factory;
import com.epam.digital.data.platform.storage.base.config.CephStorageConfiguration;
import com.epam.digital.data.platform.storage.base.factory.StorageServiceFactory;
import com.epam.digital.data.platform.storage.file.service.FormDataFileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The configurations that contains and configures beans for form data file storage service.
 */
@Configuration
public class FormDataFileServiceConfig {

  @Bean
  @ConfigurationProperties(prefix = "s3.config")
  public S3ConfigProperties s3ConfigProperties() {
    return new S3ConfigProperties();
  }

  @Bean
  public CephS3Factory cephS3Factory() {
    return new CephS3Factory(s3ConfigProperties());
  }

  @Bean
  public StorageServiceFactory storageServiceFactory(ObjectMapper objectMapper, CephS3Factory cephS3Factory) {
    return new StorageServiceFactory(objectMapper, cephS3Factory);
  }

  @Bean
  @ConfigurationProperties(prefix = "ceph")
  public CephStorageConfiguration cephStorageConfiguration() {
    return new CephStorageConfiguration();
  }

  @Bean
  public FormDataFileStorageService formDataStorageService(StorageServiceFactory factory,
      CephStorageConfiguration config) {
    return factory.fromDataFileStorageService(config);
  }
}
