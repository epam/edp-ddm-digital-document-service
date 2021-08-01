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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIT {

  protected final String host = "test.com";

  @Autowired
  @Qualifier("bpms")
  protected WireMockServer bpmServer;
  @Autowired
  @Qualifier("form-provider")
  protected WireMockServer formProviderServer;
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
