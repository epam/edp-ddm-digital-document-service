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

import static com.epam.digital.data.platform.dgtldcmnt.service.ValidationService.TOTAL_FILES_SIZE_EXCEEDS_MAX_BATCH_FILES_SIZE_MSG;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.dgtldcmnt.config.DigitalDocumentsConfigurationProperties;
import com.epam.digital.data.platform.dgtldcmnt.config.DigitalDocumentsConfigurationProperties.ContentConfigurationProperties;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentFromUserFormDto;
import com.epam.digital.data.platform.dgtldcmnt.exception.BatchFileMaxSizeException;
import com.epam.digital.data.platform.dgtldcmnt.util.unit.FractionalDataSize;
import com.epam.digital.data.platform.integration.formprovider.client.FormValidationClient;
import com.epam.digital.data.platform.integration.formprovider.dto.FileDataValidationDto;
import com.epam.digital.data.platform.integration.formprovider.exception.SubmissionValidationException;
import com.epam.digital.data.platform.starter.errorhandling.dto.ErrorDetailDto;
import com.epam.digital.data.platform.starter.errorhandling.dto.ErrorsListDto;
import com.epam.digital.data.platform.starter.errorhandling.dto.ValidationErrorDto;
import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import com.epam.digital.data.platform.storage.file.dto.FileMetadataDto;
import com.epam.digital.data.platform.storage.file.service.FormDataFileStorageService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

  private final String taskId = "testTaskId";
  private final String rootProcessInstanceId = "testProcessInstanceId";
  private final String fieldName = "testUpload1";
  private final String filePattern = "application/pdf";
  private final String formKey = "formKey";
  private final Long size = 1000L;

  private final DigitalDocumentsConfigurationProperties properties =
      new DigitalDocumentsConfigurationProperties(
          FractionalDataSize.parse("100MB"),
          FractionalDataSize.parse("100MB"),
          new ContentConfigurationProperties(""));
  @Mock
  private FormValidationClient formValidationClient;
  @Mock
  private FormDataFileStorageService formDataFileStorageService;

  private ValidationService validationService;

  @BeforeEach
  public void init() {
    validationService = new ValidationService(properties, formValidationClient,
        formDataFileStorageService);
  }

  @Test
  void shouldValidate() {
    var uploadDto = UploadDocumentFromUserFormDto.builder()
        .rootProcessInstanceId(rootProcessInstanceId)
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
    when(formDataFileStorageService.getMetadata(rootProcessInstanceId)).thenReturn(List.of(metadata));

    validationService.validate(uploadDto);
  }

  @Test
  void shouldNotValidateBatchFileSizeExceedsMaxBatchFileSize() {
    var uploadDto = UploadDocumentFromUserFormDto.builder()
        .rootProcessInstanceId(rootProcessInstanceId)
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
    when(formDataFileStorageService.getMetadata(rootProcessInstanceId)).thenReturn(List.of(metadata));

    var exception = assertThrows(BatchFileMaxSizeException.class,
        () -> validationService.validate(uploadDto));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(TOTAL_FILES_SIZE_EXCEEDS_MAX_BATCH_FILES_SIZE_MSG,
            properties.getMaxTotalFileSize().toMegabytes()));
  }

  @Test
  void shouldNotValidateDocumentTypeNotSupported() {
    var unsupportedContentType = "image/png";
    var errorMessage = "The type of the downloaded file is not supported.";
    var uploadDto = UploadDocumentFromUserFormDto.builder()
        .rootProcessInstanceId(rootProcessInstanceId)
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

    doThrow(new SubmissionValidationException(
        ValidationErrorDto.builder().code("VALIDATION_ERROR").message(errorMessage)
            .traceId("traceId")
            .details(new ErrorsListDto(List.of(new ErrorDetailDto()))).build()))
        .when(formValidationClient)
        .validateFileField("formKey", fieldName, formValidationDto);
    var exception = assertThrows(ValidationException.class,
        () -> validationService.validate(uploadDto));

    assertThat(exception.getMessage()).isEqualTo(errorMessage);
  }
}
