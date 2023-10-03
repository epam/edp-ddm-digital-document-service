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

package com.epam.digital.data.platform.dgtldcmnt;

import com.epam.digital.data.platform.dgtldcmnt.config.DigitalDocumentsConfigurationProperties;
import com.epam.digital.data.platform.dgtldcmnt.config.OpenApiConfig;
import com.epam.digital.data.platform.dgtldcmnt.config.TestBeansConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * The class represents a spring boot application runner that is used for openapi-doc generation.
 */
@SpringBootApplication
@ActiveProfiles("test")
@ComponentScan(
    basePackages = {"com.epam.digital.data.platform.dgtldcmnt.controller",
        "com.epam.digital.data.platform.dgtldcmnt.util.convert"})
@Import({TestBeansConfig.class, OpenApiConfig.class})
@EnableConfigurationProperties(DigitalDocumentsConfigurationProperties.class)
public class TestApp {

  public static void main(String[] args) {
    SpringApplication.run(TestApp.class, args);
  }

}
