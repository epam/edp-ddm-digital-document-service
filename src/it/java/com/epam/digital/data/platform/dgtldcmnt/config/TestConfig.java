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
