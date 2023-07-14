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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.bpms.api.dto.DdmSignableTaskDto;
import com.epam.digital.data.platform.dgtldcmnt.BaseIT;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentIdDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentMetadataSearchRequestDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentFromUserFormDto;
import com.epam.digital.data.platform.starter.security.jwt.JwtAuthenticationFilter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriComponentsBuilder;

class DocumentControllerIT extends BaseIT {

  private final String filename = "test.pdf";
  private final String contentType = "application/pdf";
  private final String taskId = "testTaskId";
  private final String rootProcessInstanceId = "testProcessInstanceId";
  private final String fieldName = "testUpload1";
  private final String formKey = "upload-test";
  private final byte[] data = new byte[]{1};

  @LocalServerPort
  private int port;

  @BeforeEach
  public void init() {
    String assignee = tokenParser.parseClaims(accessToken).getPreferredUsername();
    mockBpmsGetTaskById(taskId, assignee, rootProcessInstanceId, formKey);
    mockCheckFieldNames(formKey);
    mockFormProviderGetFormMetadata(formKey, fieldName,
        "/json/testFormMetadata.json");
    mockBpmsGetProcessInstanceById(rootProcessInstanceId);
  }

  @Test
  void shouldUploadDocument() {
    var response = uploadFile(filename, contentType, data, createDocumentContextDto());

    assertThat(response).isNotNull();
    assertThat(response.getName()).isEqualTo(filename);
    assertThat(response.getType()).isEqualTo(contentType);
    assertThat(response.getSize()).isEqualTo(1L);
    assertThat(response.getId()).isNotNull();
    assertThat(response.getChecksum()).isEqualTo(DigestUtils.sha256Hex(data));
    var expectedUrl = UriComponentsBuilder.newInstance().scheme("https").host(host)
        .pathSegment("documents")
        .pathSegment(rootProcessInstanceId)
        .pathSegment(taskId)
        .pathSegment(fieldName)
        .pathSegment(response.getId())
        .toUriString();
    assertThat(response.getUrl()).isEqualTo(expectedUrl);
  }

  @Test
  @SneakyThrows
  void shouldThrowIfFileIsGreaterThenMaxFileSizeUploadDocument() {
    var contextDto = createDocumentContextDto();
    var url = UriComponentsBuilder.newInstance().scheme("http")
        .host("localhost")
        .port(port)
        .pathSegment("documents")
        .pathSegment(contextDto.getRootProcessInstanceId())
        .pathSegment(contextDto.getTaskId())
        .pathSegment(contextDto.getFieldName())
        .build()
        .toUri();

    var fileData = Strings.repeat("file", 65535); // ~256KB
    var data =
        "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"file.csv\"\r\nContent-Type: text/csv\r\n\r\n"
            + fileData + "\r\n--$boundary--";

    var request = HttpRequest.newBuilder()
        .uri(url)
        .header("Content-Type", "multipart/form-data;boundary=$boundary")
        .header("Content-Disposition", "form-data; name=file; filename=file.csv")
        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken)
        .header(DocumentController.X_FORWARDED_HOST_HEADER, host)
        .POST(HttpRequest.BodyPublishers
            .ofInputStream(() -> new ByteArrayInputStream(data.getBytes())))
        .build();

    var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(413);

    DocumentContext ctx = JsonPath.parse(response.body());
    assertThat(ctx.read("$.traceId", String.class)).isNotNull();
    assertThat(ctx.read("$.code", String.class)).isEqualTo("FILE_SIZE_IS_TOO_LARGE");
    assertThat(ctx.read("$.message", String.class)).isEqualTo(
        "The size of the uploaded file exceeds 0.2MB");
    assertThat(ctx.read("$.localizedMessage", String.class)).isNull();
  }

  @Test
  void shouldUploadDocumentWithEditGrid() {
    var hygienistCertificateFile = "hygienistCertificateFile";
    mockFormProviderGetFormMetadata(formKey, hygienistCertificateFile,
        "/json/formWithEditGridMetadata.json");
    var documentContextDto = createDocumentContextDto();
    documentContextDto.setFieldName(hygienistCertificateFile);
    var response = uploadFile(filename, contentType, data, documentContextDto);

    assertThat(response).isNotNull();
    assertThat(response.getName()).isEqualTo(filename);
    assertThat(response.getType()).isEqualTo(contentType);
    assertThat(response.getSize()).isEqualTo(1L);
    assertThat(response.getId()).isNotNull();
    assertThat(response.getChecksum()).isEqualTo(DigestUtils.sha256Hex(data));
    var expectedUrl = UriComponentsBuilder.newInstance().scheme("https").host(host)
        .pathSegment("documents")
        .pathSegment(rootProcessInstanceId)
        .pathSegment(taskId)
        .pathSegment(hygienistCertificateFile)
        .pathSegment(response.getId())
        .toUriString();
    assertThat(response.getUrl()).isEqualTo(expectedUrl);
  }

  @Test
  void shouldDownloadDocument() throws Exception {
    DocumentMetadataDto documentMetadataDto = uploadFile(filename, contentType, data,
        createDocumentContextDto());
    var id = documentMetadataDto.getId();

    var uriBuilder = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(rootProcessInstanceId)
        .pathSegment(taskId)
        .pathSegment(fieldName)
        .pathSegment(id);
    var request = get(uriBuilder.toUriString()).accept(MediaType.APPLICATION_JSON_VALUE);
    request.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken);
    var response = mockMvc.perform(request)
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    assertThat(response).isNotNull();
    assertThat(response.getContentType()).isEqualTo(contentType);
    assertThat(response.getContentAsByteArray()).isEqualTo(data);
    assertThat(response.getHeader("Content-Disposition")).contains(filename);
  }

  @Test
  void shouldSearchMetadata() {
    var documentMetadataDto = uploadFile(filename, contentType, data, createDocumentContextDto());
    var id = documentMetadataDto.getId();

    var urlBuilder = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(rootProcessInstanceId)
        .pathSegment(taskId)
        .pathSegment("search");
    var response = performPost(urlBuilder.toUriString(),
        DocumentMetadataSearchRequestDto.builder()
            .documents(List.of(DocumentIdDto.builder().id(id).fieldName(fieldName).build()))
            .build(),
        new TypeReference<List<DocumentMetadataDto>>() {
        });

    assertThat(response).isNotNull();
    assertThat(response.size()).isOne();
    assertThat(response.get(0).getName()).isEqualTo(filename);
    assertThat(response.get(0).getType()).isEqualTo(contentType);
    assertThat(response.get(0).getChecksum()).isEqualTo(DigestUtils.sha256Hex(data));
    var expectedUrl = UriComponentsBuilder.newInstance().scheme("https").host(host)
        .pathSegment("documents")
        .pathSegment(rootProcessInstanceId)
        .pathSegment(taskId)
        .pathSegment(fieldName)
        .pathSegment(id)
        .toUriString();
    assertThat(response.get(0).getUrl()).isEqualTo(expectedUrl);
    assertThat(response.get(0).getId()).isEqualTo(id);
  }

  @Test
  void shouldUploadAndGetFileMetadataWithCyrillicName() {
    var filename = "кириллица.pdf";
    DocumentMetadataDto documentMetadataDto = uploadFile(filename, contentType, data,
        createDocumentContextDto());

    var id = documentMetadataDto.getId();

    var urlBuilder = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(rootProcessInstanceId)
        .pathSegment(taskId)
        .pathSegment("search");
    var response = performPost(urlBuilder.toUriString(),
        DocumentMetadataSearchRequestDto.builder()
            .documents(List.of(DocumentIdDto.builder().id(id).fieldName(fieldName).build()))
            .build(),
        new TypeReference<List<DocumentMetadataDto>>() {
        });

    assertThat(response).isNotNull();
    assertThat(response.size()).isOne();
    assertThat(response.get(0).getName()).isEqualTo(filename);
  }

  @Test
  void shouldDeleteFiles() throws Exception {
    var documentMetadataDto = uploadFile(filename, contentType, data, createDocumentContextDto());
    var id = documentMetadataDto.getId();

    var deleteUrl = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(rootProcessInstanceId).toUriString();
    var deleteReq = delete(deleteUrl);
    deleteReq.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken);
    mockMvc.perform(deleteReq).andExpect(status().isOk());

    var uriBuilder = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(rootProcessInstanceId)
        .pathSegment(taskId)
        .pathSegment(fieldName)
        .pathSegment(id);
    var request = get(uriBuilder.toUriString()).accept(MediaType.APPLICATION_JSON_VALUE);
    request.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken);

    mockMvc.perform(request).andExpect(status().is4xxClientError());
  }

  @Test
  void shouldDeleteFileById() throws Exception {
    var documentMetadataDto = uploadFile(filename, contentType, data, createDocumentContextDto());
    var id = documentMetadataDto.getId();

    var deleteUrl = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(rootProcessInstanceId)
        .pathSegment(taskId)
        .pathSegment(fieldName)
        .pathSegment(id)
        .toUriString();
    var deleteReq = delete(deleteUrl);
    deleteReq.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken);
    mockMvc.perform(deleteReq).andExpect(status().isOk());

    var uriBuilder = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(rootProcessInstanceId)
        .pathSegment(taskId)
        .pathSegment(fieldName)
        .pathSegment(id);
    var request = get(uriBuilder.toUriString()).accept(MediaType.APPLICATION_JSON_VALUE);
    request.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken);

    mockMvc.perform(request).andExpect(status().is4xxClientError());
  }

  @SneakyThrows
  private DocumentMetadataDto uploadFile(String filename, String contentType, byte[] data,
      UploadDocumentFromUserFormDto contextDto) {

    var url = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(contextDto.getRootProcessInstanceId())
        .pathSegment(contextDto.getTaskId())
        .pathSegment(contextDto.getFieldName())
        .toUriString();

    var responseAsStr = mockMvc.perform(MockMvcRequestBuilders.multipart(url)
            .file(new MockMultipartFile("file", filename, contentType, data))
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken)
            .header(DocumentController.X_FORWARDED_HOST_HEADER, host))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return getPayloadFromJSON(responseAsStr, DocumentMetadataDto.class);
  }

  private UploadDocumentFromUserFormDto createDocumentContextDto() {
    return UploadDocumentFromUserFormDto.builder()
        .rootProcessInstanceId(rootProcessInstanceId)
        .fieldName(fieldName)
        .taskId(taskId)
        .build();
  }

  @SneakyThrows
  private void mockBpmsGetTaskById(String taskId, String assignee, String rootProcessInstanceId,
      String formKey) {
    var taskById = new DdmSignableTaskDto();
    taskById.setFormKey(formKey);
    taskById.setId(taskId);
    taskById.setAssignee(assignee);
    taskById.setRootProcessInstanceId(rootProcessInstanceId);
    bpmServer.addStubMapping(
        stubFor(WireMock.get(urlPathEqualTo("/api/extended/task/" + taskId))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody(objectMapper.writeValueAsString(taskById)))
        )
    );
  }

  private void mockBpmsGetProcessInstanceById(String rootProcessInstanceId) {
    bpmServer.addStubMapping(
        stubFor(WireMock.get(urlPathEqualTo("/api/history/process-instance/" + rootProcessInstanceId))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody("{\"id\": \"" + rootProcessInstanceId + "\", \"state\": \"ACTIVE\"}"))));
  }

  @SneakyThrows
  private void mockFormProviderGetFormMetadata(String formKey, String fieldName,
      String formMetadataPath) {
    var formMetadata = new String(ByteStreams.toByteArray(
        Objects.requireNonNull(BaseIT.class.getResourceAsStream(formMetadataPath))));
    formValidationServer.addStubMapping(
        stubFor(WireMock.post(urlPathEqualTo(
                String.format("/api/form-submissions/%s/fields/%s/validate", formKey, fieldName)))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody(formMetadata))));
  }

  @SneakyThrows
  private void mockCheckFieldNames(String formKey) {
    formValidationServer.addStubMapping(
        stubFor(WireMock.post(
                urlPathEqualTo(String.format("/api/form-submissions/%s/fields/check", formKey)))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200))));
  }
}