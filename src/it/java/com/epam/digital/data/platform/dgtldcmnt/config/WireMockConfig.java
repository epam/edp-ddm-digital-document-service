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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WireMockConfig {

  @Qualifier("bpms")
  @Bean(destroyMethod = "stop")
  public WireMockServer restClientWireMock(@Value("${bpms.url}") String urlStr)
      throws MalformedURLException {
    return createAndStartMockServerForUrl(urlStr);
  }

  @Qualifier("form-provider")
  @Bean(destroyMethod = "stop")
  public WireMockServer restFormProviderWireMock(@Value("${form-management-provider.url}") String urlStr)
      throws MalformedURLException {
    return createAndStartMockServerForUrl(urlStr);
  }

  private WireMockServer createAndStartMockServerForUrl(String urlStr) throws MalformedURLException {
    var url = new URL(urlStr);
    var wireMockServer = new WireMockServer(wireMockConfig().port(url.getPort()));
    WireMock.configureFor(url.getHost(), url.getPort());
    wireMockServer.start();
    return wireMockServer;
  }
}
