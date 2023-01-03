package com.epam.digital.data.platform.dgtldcmnt.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.dgtldcmnt.BaseIT;
import com.epam.digital.data.platform.dgtldcmnt.dto.RemoteDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.RemoteDocumentMetadataDto;
import com.epam.digital.data.platform.starter.security.jwt.JwtAuthenticationFilter;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URL;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;

class InternalApiDocumentControllerIT extends BaseIT {

  private static final String EXPECTED_CHECKSUM =
      "039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81";

  @Autowired
  @Qualifier("remote-file-repository")
  private WireMockServer remoteFileRepository;

  private static final String BASE_URL = "/internal-api/documents";

  private final String filename = "testImage.png";
  private final String contentType = "image/png";
  private final String processInstanceId = "testProcessInstanceId";
  private final String url = "http://localhost:8091/image.png";
  private final byte[] data = new byte[]{1, 2, 3};


  @Test
  void shouldUploadDocument() {
    mockRemoteFileStorage();
    var response = uploadFile();

    assertThat(response).isNotNull();
    assertThat(response.getId()).isNotNull();
    assertThat(response.getName()).isEqualTo(filename);
    assertThat(response.getType()).isEqualTo(contentType);
    assertThat(response.getChecksum()).isEqualTo(EXPECTED_CHECKSUM);
    assertThat(response.getSize()).isEqualTo(3L);
  }

  @SneakyThrows
  private RemoteDocumentMetadataDto uploadFile() {
    var payload = RemoteDocumentDto.builder()
        .remoteFileLocation(new URL(url)).filename(filename)
        .build();

    var responseAsStr = mockMvc.perform(post(BASE_URL + "/" + processInstanceId)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken)
            .content(objectMapper.writeValueAsString(payload))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return getPayloadFromJSON(responseAsStr, RemoteDocumentMetadataDto.class);
  }

  @SneakyThrows
  private void mockRemoteFileStorage() {
    remoteFileRepository.addStubMapping(
        stubFor(WireMock.get(urlPathEqualTo("/image.png"))
            .willReturn(aResponse()
                .withHeader("Content-Type", contentType)
                .withStatus(200)
                .withBody(data))));
  }
}
