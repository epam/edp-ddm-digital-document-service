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
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.starter.security.jwt.JwtAuthenticationFilter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.io.ByteStreams;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriComponentsBuilder;

public class DocumentControllerIT extends BaseIT {

  private final String filename = "test.pdf";
  private final String contentType = "application/pdf";
  private final String taskId = "testTaskId";
  private final String processInstanceId = "testProcessInstanceId";
  private final String fieldName = "testUpload1";
  private final String formKey = "upload-test";
  private final byte[] data = new byte[]{1};
  private String assignee;

  @BeforeEach
  public void init() {
    assignee = tokenParser.parseClaims(accessToken).getPreferredUsername();
    mockBpmsGetTaskById(taskId, assignee, processInstanceId, formKey);
    mockFormProviderGetFormMetadata(formKey, "/json/testFormMetadata.json");
    mockBpmsGetProcessInstanceById(processInstanceId);
  }

  @Test
  public void shouldUploadDocument() {
    var response = uploadFile(filename, contentType, data, createDocumentContextDto());

    assertThat(response).isNotNull();
    assertThat(response.getName()).isEqualTo(filename);
    assertThat(response.getType()).isEqualTo(contentType);
    assertThat(response.getSize()).isEqualTo(1000L);
    assertThat(response.getId()).isNotNull();
    assertThat(response.getChecksum()).isEqualTo(DigestUtils.sha256Hex(data));
    var expectedUrl = UriComponentsBuilder.newInstance().scheme("https").host(host)
        .pathSegment("documents")
        .pathSegment(processInstanceId)
        .pathSegment(taskId)
        .pathSegment(fieldName)
        .pathSegment(response.getId())
        .toUriString();
    assertThat(response.getUrl()).isEqualTo(expectedUrl);
  }

  @Test
  public void shouldUploadDocumentWithEditGrid() {
    mockFormProviderGetFormMetadata(formKey, "/json/formWithEditGridMetadata.json");
    var hygienistCertificateFile = "hygienistCertificateFile";
    var documentContextDto = createDocumentContextDto();
    documentContextDto.setFieldName(hygienistCertificateFile);
    var response = uploadFile(filename, contentType, data, documentContextDto);

    assertThat(response).isNotNull();
    assertThat(response.getName()).isEqualTo(filename);
    assertThat(response.getType()).isEqualTo(contentType);
    assertThat(response.getSize()).isEqualTo(1000L);
    assertThat(response.getId()).isNotNull();
    assertThat(response.getChecksum()).isEqualTo(DigestUtils.sha256Hex(data));
    var expectedUrl = UriComponentsBuilder.newInstance().scheme("https").host(host)
        .pathSegment("documents")
        .pathSegment(processInstanceId)
        .pathSegment(taskId)
        .pathSegment(hygienistCertificateFile)
        .pathSegment(response.getId())
        .toUriString();
    assertThat(response.getUrl()).isEqualTo(expectedUrl);
  }

  @Test
  public void shouldDownloadDocument() throws Exception {
    DocumentMetadataDto documentMetadataDto = uploadFile(filename, contentType, data,
        createDocumentContextDto());
    var id = documentMetadataDto.getId();

    var uriBuilder = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(processInstanceId)
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
  public void shouldSearchMetadata() {
    var documentMetadataDto = uploadFile(filename, contentType, data, createDocumentContextDto());
    var id = documentMetadataDto.getId();

    var urlBuilder = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(processInstanceId)
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
        .pathSegment(processInstanceId)
        .pathSegment(taskId)
        .pathSegment(fieldName)
        .pathSegment(id)
        .toUriString();
    assertThat(response.get(0).getUrl()).isEqualTo(expectedUrl);
    assertThat(response.get(0).getId()).isEqualTo(id);
  }

  @Test
  public void shouldUploadAndGetFileMetadataWithCyrillicName() {
    var filename = "??????????????????.pdf";
    DocumentMetadataDto documentMetadataDto = uploadFile(filename, contentType, data,
        createDocumentContextDto());
    var id = documentMetadataDto.getId();

    var urlBuilder = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(processInstanceId)
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
  public void shouldDeleteFiles() throws Exception {
    var documentMetadataDto = uploadFile(filename, contentType, data, createDocumentContextDto());
    var id = documentMetadataDto.getId();

    var deleteUrl = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(processInstanceId).toUriString();
    var deleteReq = delete(deleteUrl);
    deleteReq.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken);
    mockMvc.perform(deleteReq).andExpect(status().isOk());

    var uriBuilder = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(processInstanceId)
        .pathSegment(taskId)
        .pathSegment(fieldName)
        .pathSegment(id);
    var request = get(uriBuilder.toUriString()).accept(MediaType.APPLICATION_JSON_VALUE);
    request.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, accessToken);

    mockMvc.perform(request).andExpect(status().is4xxClientError());
  }

  @SneakyThrows
  private DocumentMetadataDto uploadFile(String filename, String contentType, byte[] data,
      UploadDocumentDto contextDto) {

    var url = UriComponentsBuilder.newInstance().pathSegment("documents")
        .pathSegment(contextDto.getProcessInstanceId())
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

  private UploadDocumentDto createDocumentContextDto() {
    return UploadDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .fieldName(fieldName)
        .taskId(taskId)
        .build();
  }

  @SneakyThrows
  private void mockBpmsGetTaskById(String taskId, String assignee, String processInstanceId,
      String formKey) {
    var taskById = new DdmSignableTaskDto();
    taskById.setFormKey(formKey);
    taskById.setId(taskId);
    taskById.setAssignee(assignee);
    taskById.setProcessInstanceId(processInstanceId);
    bpmServer.addStubMapping(
        stubFor(WireMock.get(urlPathEqualTo("/api/extended/task/" + taskId))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody(objectMapper.writeValueAsString(taskById)))
        )
    );
  }

  private void mockBpmsGetProcessInstanceById(String processInstanceId) {
    bpmServer.addStubMapping(
        stubFor(WireMock.get(urlPathEqualTo("/api/history/process-instance/" + processInstanceId))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody("{\"id\": \"" + processInstanceId + "\", \"state\": \"ACTIVE\"}"))));
  }

  @SneakyThrows
  private void mockFormProviderGetFormMetadata(String formKey, String formMetadataPath) {
    var formMetadata = new String(ByteStreams.toByteArray(
        Objects.requireNonNull(BaseIT.class.getResourceAsStream(formMetadataPath))));
    formProviderServer.addStubMapping(
        stubFor(WireMock.get(urlPathEqualTo("/" + formKey))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody(formMetadata))));
  }
}