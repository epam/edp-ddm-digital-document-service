/*
 * Copyright 2021 EPAM Systems.
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
import com.epam.digital.data.platform.integration.ceph.service.S3ObjectCephService;
import com.google.common.io.ByteStreams;
import java.util.Objects;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TestConfig {

  @Bean
  @Primary
  public S3ObjectCephService s3ObjectCephService() {
    return new TestS3ObjectCephService();
  }

  @Bean
  @SneakyThrows
  public String accessToken() {
    return new String(ByteStreams.toByteArray(
        Objects.requireNonNull(BaseIT.class.getResourceAsStream("/json/accessToken.json"))));
  }
}
