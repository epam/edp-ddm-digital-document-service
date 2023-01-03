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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.dgtldcmnt.controller.DocumentController;
import com.epam.digital.data.platform.starter.security.jwt.JwtAuthenticationFilter;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIT {

  protected final String host = "test.com";

  @Autowired
  @Qualifier("bpms")
  protected WireMockServer bpmServer;
  @Autowired
  @Qualifier("form-validation")
  protected WireMockServer formValidationServer;
  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  protected ObjectMapper objectMapper;
  @Autowired
  protected String accessToken;
  @Autowired
  protected TokenParser tokenParser;

  @SneakyThrows
  protected <T> T performPost(String url, Object body, TypeReference<T> valueTypeRef) {
    var request = post(url)
        .content(objectMapper.writeValueAsString(body))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE);
    return getPayloadFromJSON(performAndConvertToString(request), valueTypeRef);
  }

  @SneakyThrows
  protected String performAndConvertToString(MockHttpServletRequestBuilder request) {
    request.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken);
    request.header(DocumentController.X_FORWARDED_HOST_HEADER, host);
    return mockMvc.perform(request)
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString(StandardCharsets.UTF_8);
  }

  @SneakyThrows
  protected <T> T getPayloadFromJSON(String resultAsString, Class<T> type) {
    return objectMapper.convertValue(objectMapper.readValue(resultAsString, Map.class), type);
  }

  @SneakyThrows
  protected <T> T getPayloadFromJSON(String resultAsString, TypeReference<T> valueTypeRef) {
    return objectMapper.readValue(resultAsString, valueTypeRef);
  }
}
