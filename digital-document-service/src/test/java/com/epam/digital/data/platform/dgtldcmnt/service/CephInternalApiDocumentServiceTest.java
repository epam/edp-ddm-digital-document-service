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

import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.validator.RemoteFileSizeValidator;
import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import com.epam.digital.data.platform.storage.file.dto.BaseFileMetadataDto;
import com.epam.digital.data.platform.storage.file.dto.BaseFileMetadataDto.BaseUserMetadataHeaders;
import com.epam.digital.data.platform.storage.file.dto.FileObjectDto;
import com.epam.digital.data.platform.storage.file.service.FileStorageService;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class CephInternalApiDocumentServiceTest {

  @Mock
  private FileStorageService storageService;
  private InternalApiDocumentService documentService;

  private static final String FILE_ID = "fileId";
  private static final String FILENAME = "testFilename";
  private static final String CONTENT_TYPE = "image/png";
  private static final String PROCESS_INSTANCE_ID = "testProcessInstanceId";
  private static final long CONTENT_LENGTH = 1000L;
  // expected checksum is checksum of empty inputStream because storageService is a mock and
  // it doesn't read the stream
  private static final String EXPECTED_CHECKSUM =
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
  private static final byte[] DATA = new byte[]{1, 2, 3};

  @BeforeEach
  public void init() {
    documentService = new CephInternalApiDocumentService(storageService,
        new RemoteFileSizeValidator(DataSize.ofMegabytes(1)));
  }

  @Test
  void shouldThrowExceptionWhenContentLengthMoreThenAllowed() {
    var uploadDto = UploadDocumentDto.builder()
        .size(1024 * 1024 + 1)
        .build();

    assertThrows(ValidationException.class, () -> documentService.put(uploadDto));
  }

  @Test
  void testPutDocument() {
    var is = new BufferedInputStream(new ByteArrayInputStream(DATA));
    var baseFileMetadataDto = new BaseFileMetadataDto(
        CONTENT_LENGTH, CONTENT_TYPE, buildUserMetadata());

    var uploadDto = UploadDocumentDto.builder()
        .filename(FILENAME)
        .contentType(CONTENT_TYPE)
        .size(0)
        .fileInputStream(is)
        .processInstanceId(PROCESS_INSTANCE_ID)
        .build();

    ArgumentCaptor<FileObjectDto> captor = ArgumentCaptor.forClass(FileObjectDto.class);
    when(storageService.save(eq(PROCESS_INSTANCE_ID), anyString(),
        captor.capture())).thenReturn(baseFileMetadataDto);

    var savedDocMetadata = documentService.put(uploadDto);

    assertThat(savedDocMetadata).isNotNull();
    assertThat(savedDocMetadata.getId()).isEqualTo(FILE_ID);
    assertThat(savedDocMetadata.getName()).isEqualTo(FILENAME);
    assertThat(savedDocMetadata.getType()).isEqualTo(CONTENT_TYPE);
    assertThat(savedDocMetadata.getChecksum()).isEqualTo(EXPECTED_CHECKSUM);
    assertThat(savedDocMetadata.getSize()).isEqualTo(CONTENT_LENGTH);

    var fileMetadataDto = captor.getValue().getMetadata();
    assertThat(fileMetadataDto.getContentLength()).isZero();
    assertThat(fileMetadataDto.getContentType()).isEqualTo(CONTENT_TYPE);
    assertThat(fileMetadataDto.getId()).isNotEqualTo(FILE_ID); // should be generated
    assertThat(fileMetadataDto.getChecksum()).isEqualTo(EXPECTED_CHECKSUM);
    assertThat(fileMetadataDto.getFilename()).isEqualTo(FILENAME);
  }

  private Map<String, String> buildUserMetadata() {
    Map<String, String> userMetadata = new LinkedHashMap<>();
    userMetadata.put(BaseUserMetadataHeaders.ID, CephInternalApiDocumentServiceTest.FILE_ID);
    userMetadata.put(BaseUserMetadataHeaders.FILENAME, CephInternalApiDocumentServiceTest.FILENAME);
    return userMetadata;
  }
}
