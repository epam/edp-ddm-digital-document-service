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

import com.epam.digital.data.platform.bpms.client.config.FeignConfig;
import javax.servlet.MultipartConfigElement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@Import({FeignConfig.class})
public class GeneralConfig implements WebMvcConfigurer {

  private final MultipartProperties multipartProperties;
  private final DigitalDocumentsConfigurationProperties digitalDocumentsProperties;

  @Bean
  @Primary
  public MultipartConfigElement multipartConfigElement() {
    multipartProperties.setMaxFileSize(
        DataSize.ofBytes(digitalDocumentsProperties.getMaxFileSize().toBytes()));
    multipartProperties.setMaxRequestSize(
        DataSize.ofBytes(digitalDocumentsProperties.getMaxTotalFileSize().toBytes()));
    return this.multipartProperties.createMultipartConfig();
  }
}
