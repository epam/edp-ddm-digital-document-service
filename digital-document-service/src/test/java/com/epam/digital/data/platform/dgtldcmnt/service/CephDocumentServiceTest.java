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

package com.epam.digital.data.platform.dgtldcmnt.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentIdDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentsMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentFromUserFormDto;
import com.epam.digital.data.platform.dgtldcmnt.mapper.DocumentMetadataDtoMapper;
import com.epam.digital.data.platform.storage.file.dto.FileDataDto;
import com.epam.digital.data.platform.storage.file.dto.FileMetadataDto;
import com.epam.digital.data.platform.storage.file.exception.FileNotFoundException;
import com.epam.digital.data.platform.storage.file.service.FormDataFileStorageService;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class CephDocumentServiceTest {

  @Mock
  private FormDataFileStorageService fromDataFileStorageService;
  @Spy
  private DocumentMetadataDtoMapper mapper = Mappers.getMapper(DocumentMetadataDtoMapper.class);

  private DocumentService service;

  private final String key = "testKey";
  private final String filename = "testFilename";
  private final String contentType = "application/pdf";
  private final String taskId = "testTaskId";
  private final String processInstanceId = "testProcessInstanceId";
  private final String fieldName = "testFieldName";
  private final String originRequestUrl = "test.com";
  private final Long contentLength = 1000L;
  private final byte[] data = new byte[]{1};

  @BeforeEach
  public void init() {
    service = new CephDocumentService(fromDataFileStorageService, mapper);
  }

  @Test
  void testPutDocument() {
    var is = new BufferedInputStream(new ByteArrayInputStream(data));
    var testObjectMetaData = FileMetadataDto.builder()
        .contentLength(contentLength)
        .contentType(contentType)
        .build();
    var uploadDto = UploadDocumentFromUserFormDto.builder()
        .processInstanceId(processInstanceId)
        .originRequestUrl(originRequestUrl)
        .contentType(contentType)
        .fieldName(fieldName)
        .fileInputStream(is)
        .filename(filename)
        .taskId(taskId)
        .build();

    ArgumentCaptor<FileDataDto> captor = ArgumentCaptor.forClass(FileDataDto.class);
    when(fromDataFileStorageService.save(eq(processInstanceId), anyString(),
        captor.capture())).thenReturn(testObjectMetaData);

    var savedDocMetadata = service.put(uploadDto);

    assertThat(savedDocMetadata).isNotNull();
    assertThat(savedDocMetadata.getType()).isEqualTo(contentType);
    assertThat(savedDocMetadata.getSize()).isEqualTo(contentLength);
    assertThat(savedDocMetadata.getName()).isEqualTo(filename);
    FileMetadataDto userMetadata = captor.getValue().getMetadata();
    assertThat(userMetadata.getId()).isNotEmpty();
    assertThat(userMetadata.getChecksum()).isNotEmpty();
    assertThat(userMetadata.getFilename()).isEqualTo(filename);
    assertThat(savedDocMetadata.getChecksum()).isEqualTo(userMetadata.getChecksum());
    assertThat(savedDocMetadata.getId()).isEqualTo(userMetadata.getId());
    assertThat(savedDocMetadata.getUrl()).contains(userMetadata.getId());
    var expectedUrl = UriComponentsBuilder.newInstance().scheme("https").host(originRequestUrl)
        .pathSegment("documents")
        .pathSegment(processInstanceId)
        .pathSegment(taskId)
        .pathSegment(fieldName)
        .pathSegment(userMetadata.getId())
        .toUriString();
    assertThat(savedDocMetadata.getUrl()).isEqualTo(expectedUrl);
  }

  @Test
  void testGetDocument() throws IOException {
    var getDocumentDto = GetDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .id(key)
        .build();
    var metadataDto = FileMetadataDto.builder()
        .contentLength(contentLength)
        .contentType(contentType)
        .filename(filename)
        .build();
    var fileDataDto = FileDataDto.builder()
        .content(new ByteArrayInputStream(data))
        .metadata(metadataDto)
        .build();

    when(
        fromDataFileStorageService.loadByProcessInstanceIdAndId(processInstanceId, key)).thenReturn(
        fileDataDto);

    DocumentDto documentDto = service.get(getDocumentDto);

    assertThat(documentDto).isNotNull();
    assertThat(documentDto.getName()).isEqualTo(filename);
    assertThat(documentDto.getContentType()).isEqualTo(contentType);
    assertThat(documentDto.getSize()).isEqualTo(contentLength);
    assertThat(documentDto.getContent().readAllBytes()).isEqualTo(data);
  }

  @Test
  void testGetDocumentThatNotFound() {
    when(fromDataFileStorageService.loadByProcessInstanceIdAndId(processInstanceId, key))
        .thenThrow(FileNotFoundException.class);

    assertThrows(FileNotFoundException.class,
        () -> service
            .get(GetDocumentDto.builder().processInstanceId(processInstanceId).id(key).build()));
  }

  @Test
  void testGetMetadata() {
    var fileMetadata = FileMetadataDto.builder()
        .contentLength(contentLength)
        .contentType(contentType)
        .filename("test.pdf")
        .build();
    var getMetadataDto = GetDocumentsMetadataDto.builder()
        .documents(List.of(DocumentIdDto.builder().id(key).fieldName(fieldName).build()))
        .processInstanceId(processInstanceId)
        .originRequestUrl(originRequestUrl)
        .taskId(taskId)
        .build();

    when(fromDataFileStorageService.getMetadata(processInstanceId, Set.of(key))).thenReturn(
        List.of(fileMetadata));

    var metadata = service.getMetadata(getMetadataDto);
    assertThat(metadata.size()).isOne();
    assertThat(metadata.get(0).getType()).isEqualTo(contentType);
    assertThat(metadata.get(0).getSize()).isEqualTo(contentLength);
  }

  @Test
  void testGetMetadataById() {
    var fileMetadata = FileMetadataDto.builder()
        .contentLength(contentLength)
        .contentType(contentType)
        .filename("test.pdf")
        .build();

    when(fromDataFileStorageService.getMetadata(processInstanceId, Set.of(key))).thenReturn(
        List.of(fileMetadata));

    var metadata = service.getMetadata(processInstanceId, key);
    assertThat(metadata.getType()).isEqualTo(contentType);
    assertThat(metadata.getSize()).isEqualTo(contentLength);
  }

  @Test
  void testGetMetadataDocumentNotFound() {
    var getMetadataDto = GetDocumentsMetadataDto.builder()
        .documents(List.of(DocumentIdDto.builder().id(key).fieldName(fieldName).build()))
        .processInstanceId(processInstanceId)
        .originRequestUrl(originRequestUrl)
        .taskId(taskId)
        .build();
    when(fromDataFileStorageService.getMetadata(processInstanceId, Set.of(key))).thenThrow(
        FileNotFoundException.class);

    assertThrows(FileNotFoundException.class, () -> service.getMetadata(getMetadataDto));
  }

  @Test
  void shouldGetDocumentWhenFileNameNull() throws IOException {
    var getDocumentDto = GetDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .id(key)
        .build();
    var metadataDto = FileMetadataDto.builder()
        .contentLength(contentLength)
        .contentType(contentType)
        .build();
    var fileDataDto = FileDataDto.builder()
        .content(new ByteArrayInputStream(data))
        .metadata(metadataDto)
        .build();

    when(
        fromDataFileStorageService.loadByProcessInstanceIdAndId(processInstanceId, key)).thenReturn(
        fileDataDto);

    DocumentDto documentDto = service.get(getDocumentDto);

    assertThat(documentDto).isNotNull();
    assertThat(documentDto.getName()).isNull();
    assertThat(documentDto.getContentType()).isEqualTo(contentType);
    assertThat(documentDto.getSize()).isEqualTo(contentLength);
    assertThat(documentDto.getContent().readAllBytes()).isEqualTo(data);
  }
}