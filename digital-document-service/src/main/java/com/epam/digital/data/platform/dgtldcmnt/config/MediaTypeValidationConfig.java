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

import com.epam.digital.data.platform.dgtldcmnt.detector.DigitalDocumentMediaTypeDetector;
import org.apache.tika.Tika;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MediaTypeValidationConfig {

  @Bean
  @Qualifier("default-detector")
  public Detector defaultDetector() {
    return new DefaultDetector();
  }

  @Bean
  @Primary
  @ConditionalOnProperty(
      value = "media-type-validation.signed-file-detection.enabled",
      havingValue = "true",
      matchIfMissing = true
  )
  @Qualifier("digital-document-media-type-detector")
  public Detector digitalDocumentMediaTypeDetector(
      @Qualifier("default-detector") Detector defaultDetector) {
    return new DigitalDocumentMediaTypeDetector(defaultDetector);
  }

  @Bean
  public Tika tika(Detector detector) {
    return new Tika(detector);
  }
}
