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

package com.epam.digital.data.platform.dgtldcmnt.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.dgtldcmnt.BaseIT;
import com.epam.digital.data.platform.dgtldcmnt.dto.InternalApiDocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.RemoteDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.RemoteDocumentMetadataDto;
import com.epam.digital.data.platform.starter.security.jwt.JwtAuthenticationFilter;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URL;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

class InternalApiDocumentControllerIT extends BaseIT {

  private static final String EXPECTED_CHECKSUM =
      "039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81";

  @Autowired
  @Qualifier("remote-file-repository")
  private WireMockServer remoteFileRepository;

  private static final String BASE_URL = "/internal-api/documents";

  private final String filename = "testImage.png";
  private final String contentType = "image/png";
  private final String rootProcessInstanceId = "testProcessInstanceId";
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

  @Test
  @SneakyThrows
  void shouldThrow415ErrorIfFileExtensionNotSupported() {
    remoteFileRepository.addStubMapping(
        stubFor(WireMock.get(urlPathEqualTo("/file.txt"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "plain/text")
                .withStatus(200)
                .withBody(data))));

    var payload = RemoteDocumentDto.builder()
        .remoteFileLocation(new URL(remoteFileRepository.url("/file.txt"))).filename(filename)
        .build();

    mockMvc.perform(post(BASE_URL + "/" + rootProcessInstanceId)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken)
            .content(objectMapper.writeValueAsString(payload))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  @SneakyThrows
  void shouldReturnMetadata() {
    var uploadResponse = mockMvc.perform(
            multipart("/internal-api/v2/documents/{rootProcessInstanceId}", rootProcessInstanceId)
                .file(new MockMultipartFile("file", filename, contentType, data))
                .param("filename", filename)
                .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    var saveMetadata = getPayloadFromJSON(uploadResponse, InternalApiDocumentMetadataDto.class);
    var id = saveMetadata.getId();

    var metadataResponse = mockMvc.perform(
            get("/internal-api/documents/{rootProcessInstanceId}/{id}/metadata", rootProcessInstanceId, id)
                .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    var metadata = getPayloadFromJSON(metadataResponse, InternalApiDocumentMetadataDto.class);
    Assertions.assertThat(metadata)
        .hasFieldOrPropertyWithValue("id", id)
        .hasFieldOrPropertyWithValue("name", filename)
        .hasFieldOrPropertyWithValue("size", 3L)
        .hasFieldOrPropertyWithValue("type", contentType);
  }

  @Test
  @SneakyThrows
  void shouldUploadAndDownloadDocumentWithMultiPartBody() {
    var url = UriComponentsBuilder.newInstance().pathSegment("internal-api")
        .pathSegment("v2")
        .pathSegment("documents")
        .pathSegment(rootProcessInstanceId)
        .toUriString();

    var uploadResponse = mockMvc.perform(multipart(url)
            .file(new MockMultipartFile("file", filename, contentType, data))
            .param("filename", filename)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    var metadata = getPayloadFromJSON(uploadResponse, RemoteDocumentMetadataDto.class);

    var uriBuilder = UriComponentsBuilder.newInstance().pathSegment("internal-api")
        .pathSegment("documents")
        .pathSegment(rootProcessInstanceId)
        .pathSegment(metadata.getId());
    var request = get(uriBuilder.toUriString()).accept(MediaType.APPLICATION_JSON_VALUE);
    request.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken);
    var downloadResponse = mockMvc.perform(request)
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    assertThat(downloadResponse).isNotNull();
    assertThat(downloadResponse.getContentType()).isEqualTo(contentType);
    assertThat(downloadResponse.getContentAsByteArray()).isEqualTo(data);
  }

  @SneakyThrows
  private RemoteDocumentMetadataDto uploadFile() {
    var payload = RemoteDocumentDto.builder()
        .remoteFileLocation(new URL(remoteFileRepository.url("/image.png"))).filename(filename)
        .build();

    var responseAsStr = mockMvc.perform(post(BASE_URL + "/" + rootProcessInstanceId)
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
