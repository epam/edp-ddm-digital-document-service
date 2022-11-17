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

package com.epam.digital.data.platform.dgtldcmnt.service;

import static com.epam.digital.data.platform.dgtldcmnt.service.ValidationService.TOTAL_FILES_SIZE_EXCEEDS_MAX_BATCH_FILES_SIZE_MSG;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.integration.formprovider.client.FormValidationClient;
import com.epam.digital.data.platform.integration.formprovider.dto.FileDataValidationDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FileFieldErrorDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FormErrorDetailDto;
import com.epam.digital.data.platform.integration.formprovider.exception.FileFieldValidationException;
import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import com.epam.digital.data.platform.storage.file.dto.FileMetadataDto;
import com.epam.digital.data.platform.storage.file.repository.FormDataFileRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ValidationServiceTest {

  private final String taskId = "testTaskId";
  private final String processInstanceId = "testProcessInstanceId";
  private final String fieldName = "testUpload1";
  private final String filePattern = "application/pdf";
  private final String formKey = "formKey";
  private final Long size = 1000L;
  private Double maxBatchFilesSize = 100.0;

  @Mock
  private FormValidationClient formValidationClient;
  @Mock
  private FormDataFileRepository formDataFileRepository;

  private ValidationService validationService;

  @BeforeEach
  public void init() {
    validationService = new ValidationService(formValidationClient, formDataFileRepository);
    validationService.setMaxBatchFileSize(maxBatchFilesSize);
  }

  @Test
  public void shouldValidate() {
    var uploadDto = UploadDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .contentType(filePattern)
        .fieldName(fieldName)
        .formKey(formKey)
        .taskId(taskId)
        .size(size)
        .build();
    var metadata = new FileMetadataDto(size, null, Map.of(
        "formKey", formKey,
        "fieldName", fieldName
    ));
    when(formDataFileRepository.getMetadata(processInstanceId)).thenReturn(List.of(metadata));

    validationService.validate(uploadDto);
  }

  @Test
  public void shouldNotValidateBatchFileSizeExceedsMaxBatchFileSize() {
    var uploadDto = UploadDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .contentType(filePattern)
        .fieldName(fieldName)
        .formKey(formKey)
        .taskId(taskId)
        .size(size)
        .build();
    var metadata = new FileMetadataDto(114857700L, null, Map.of(
        "formKey", formKey,
        "fieldName", fieldName
    ));
    when(formDataFileRepository.getMetadata(processInstanceId)).thenReturn(List.of(metadata));

    var exception = assertThrows(ValidationException.class,
        () -> validationService.validate(uploadDto));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(TOTAL_FILES_SIZE_EXCEEDS_MAX_BATCH_FILES_SIZE_MSG, maxBatchFilesSize));
  }

  @Test
  public void shouldNotValidateDocumentTypeNotSupported() {
    var unsupportedContentType = "image/png";
    var errorMessage = "The type of the downloaded file is not supported.";
    var uploadDto = UploadDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .contentType(unsupportedContentType)
        .fieldName(fieldName)
        .formKey(formKey)
        .taskId(taskId)
        .size(size)
        .filename("fileName")
        .build();
    var formValidationDto = FileDataValidationDto.builder()
        .contentType(unsupportedContentType)
        .size(size)
        .fileName("fileName")
        .documentKey(fieldName)
        .build();

    doThrow(new FileFieldValidationException(
        FileFieldErrorDto.builder().code("422").message(errorMessage).traceId("traceId")
            .errors(List.of(
                new FormErrorDetailDto())).build())).when(formValidationClient)
        .validateFileField("formKey", fieldName, formValidationDto);
    var exception = assertThrows(ValidationException.class,
        () -> validationService.validate(uploadDto));

    assertThat(exception.getMessage()).isEqualTo(errorMessage);
  }
}
