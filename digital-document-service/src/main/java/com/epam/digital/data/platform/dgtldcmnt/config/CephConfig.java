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

import com.amazonaws.metrics.RequestMetricCollector;
import com.epam.digital.data.platform.integration.ceph.config.S3ConfigProperties;
import com.epam.digital.data.platform.integration.ceph.factory.CephS3Factory;
import com.epam.digital.data.platform.integration.ceph.metric.MicrometerMetricsCollector;
import com.epam.digital.data.platform.storage.file.config.FileDataCephStorageConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The configurations that contain and configures beans for form data file storage service.
 */
@Configuration
public class CephConfig {

  @Bean
  @ConfigurationProperties(prefix = "s3.config")
  public S3ConfigProperties s3ConfigProperties() {
    return new S3ConfigProperties();
  }

  @Bean
  public CephS3Factory cephS3Factory(
      @Autowired(required = false) RequestMetricCollector collector) {
    return new CephS3Factory(s3ConfigProperties(), collector);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "s3.config.client.metrics",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = false
  )
  public RequestMetricCollector micrometerMetricsCollector(MeterRegistry registry) {
    return new MicrometerMetricsCollector(registry);
  }

  @Bean
  @ConfigurationProperties(prefix = "ceph")
  public FileDataCephStorageConfiguration cephStorageConfiguration() {
    return new FileDataCephStorageConfiguration();
  }
}
