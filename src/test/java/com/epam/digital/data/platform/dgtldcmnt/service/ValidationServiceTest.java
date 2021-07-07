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

import static com.epam.digital.data.platform.dgtldcmnt.service.ValidationService.ALLOWED_FILE_SIZE_DEFINITIONS;
import static com.epam.digital.data.platform.dgtldcmnt.service.ValidationService.FIELD_NOT_FOUND_MSG;
import static com.epam.digital.data.platform.dgtldcmnt.service.ValidationService.FILE_SIZE_EXCEEDS_MAX_FILE_SIZE_MSG;
import static com.epam.digital.data.platform.dgtldcmnt.service.ValidationService.FILE_TYPE_IS_NOT_SUPPORTED_MSG;
import static com.epam.digital.data.platform.dgtldcmnt.service.ValidationService.UNSUPPORTED_FILE_SIZE_DEFINITION_MSG;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.integration.formprovider.dto.ComponentsDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FormDto;
import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ValidationServiceTest {

  private final String taskId = "testTaskId";
  private final String processInstanceId = "testProcessInstanceId";
  private final String fieldName = "testUpload1";
  private final String filePattern = "application/pdf";
  private final String fileMaxSize = "50MB";
  private final Long size = 1000L;

  private final ValidationService validationService = new ValidationService();

  private FormDto formDto;

  @BeforeEach
  public void init() {
    var componentsDtos = List
        .of(new ComponentsDto(fieldName, "file", null, null, filePattern, fileMaxSize, null));
    formDto = new FormDto(componentsDtos);
  }

  @Test
  public void shouldValidate() {
    var uploadDto = UploadDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .contentType(filePattern)
        .fieldName(fieldName)
        .taskId(taskId)
        .size(size)
        .build();

    validationService.validate(uploadDto, formDto);
  }

  @Test
  public void shouldValidateWhenAllDocumentTypesAllowed() {
    var componentsDtos = List
        .of(new ComponentsDto(fieldName, "file", null, null, "*", fileMaxSize, null));
    var formDto = new FormDto(componentsDtos);
    var uploadDto = UploadDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .contentType(filePattern)
        .fieldName(fieldName)
        .taskId(taskId)
        .size(size)
        .build();

    validationService.validate(uploadDto, formDto);
  }

  @Test
  public void shouldNotValidateFileSizeExceedsMaxSize() {
    var uploadDto = UploadDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .contentType(filePattern)
        .fieldName(fieldName)
        .taskId(taskId)
        .size(55000000L)
        .build();

    var exception = assertThrows(ValidationException.class,
        () -> validationService.validate(uploadDto, formDto));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(FILE_SIZE_EXCEEDS_MAX_FILE_SIZE_MSG, fileMaxSize));
  }

  @Test
  public void shouldNotValidateDocumentTypeNotSupported() {
    var unsupportedContentType = "image/png";
    var uploadDto = UploadDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .contentType(unsupportedContentType)
        .fieldName(fieldName)
        .taskId(taskId)
        .size(size)
        .build();

    var exception = assertThrows(ValidationException.class,
        () -> validationService.validate(uploadDto, formDto));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(FILE_TYPE_IS_NOT_SUPPORTED_MSG, List.of(filePattern)));
  }

  @Test
  public void shouldNotValidateFieldNotFound() {
    var componentsDtos = List
        .of(new ComponentsDto("stingFiled", "string", null, null, null, null, null));
    var formDto = new FormDto(componentsDtos);
    var uploadDto = UploadDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .contentType(filePattern)
        .fieldName(fieldName)
        .taskId(taskId)
        .size(size)
        .build();

    var exception = assertThrows(ValidationException.class,
        () -> validationService.validate(uploadDto, formDto));

    assertThat(exception.getMessage()).isEqualTo(String.format(FIELD_NOT_FOUND_MSG, fieldName));
  }

  @Test
  public void shouldNotValidateUnsupportedFileSizeDefinition() {
    var invalidFilePattern = "50KB";
    var componentsDtos = List
        .of(new ComponentsDto(fieldName, "file", null, null, filePattern, invalidFilePattern, null));
    var formDto = new FormDto(componentsDtos);
    var uploadDto = UploadDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .contentType(filePattern)
        .fieldName(fieldName)
        .taskId(taskId)
        .size(size)
        .build();

    var exception = assertThrows(ValidationException.class,
        () -> validationService.validate(uploadDto, formDto));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(UNSUPPORTED_FILE_SIZE_DEFINITION_MSG, "KB", ALLOWED_FILE_SIZE_DEFINITIONS));
  }

  @Test
  public void shouldNotValidateDocumentTypeNotValid() {
    var componentsDtos = List
        .of(new ComponentsDto(fieldName, "file", null, null, "*.zip", fileMaxSize, null));
    var formDto = new FormDto(componentsDtos);
    var uploadDto = UploadDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .contentType(filePattern)
        .fieldName(fieldName)
        .taskId(taskId)
        .size(size)
        .build();

    var exception = assertThrows(ValidationException.class,
        () -> validationService.validate(uploadDto, formDto));

    assertThat(exception.getMessage())
        .isEqualTo("Invalid mime type \"*.zip\": does not contain '/'");
  }
}
