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

import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.integration.formprovider.client.FormValidationClient;
import com.epam.digital.data.platform.integration.formprovider.dto.FileDataValidationDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FormFieldListValidationDto;
import com.epam.digital.data.platform.integration.formprovider.exception.FileFieldValidationException;
import com.epam.digital.data.platform.starter.errorhandling.dto.ValidationErrorDto;
import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Validation service that validates the document metadata based on the form metadata.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationService {

  private final FormValidationClient formValidationClient;

  /**
   * Validate the uploaded document metadata based on the ui form metadata.
   *
   * @param uploadDto contains uploaded document metadata.
   * @param formKey   form identifier
   */
  public void validate(UploadDocumentDto uploadDto, String formKey) {
    log.debug("Validating file {} in task {} in form {}", uploadDto.getFieldName(),
        uploadDto.getTaskId(), formKey);
    var fileData = FileDataValidationDto.builder()
        .contentType(uploadDto.getContentType())
        .documentKey(uploadDto.getFieldName())
        .fileName(uploadDto.getFilename())
        .size(uploadDto.getSize()).build();
    try {
      formValidationClient.validateFileField(formKey, uploadDto.getFieldName(), fileData);
    } catch (FileFieldValidationException exception) {
      throw createValidationException(exception);
    }
    log.debug("File {} type and size are valid. Task {}", uploadDto.getFieldName(),
        uploadDto.getTaskId());
  }

  public void checkFieldNamesExistence(List<String> fieldNames, String formKey) {
    try {
      formValidationClient.checkFieldNames(formKey,
          FormFieldListValidationDto.builder().fields(fieldNames).build());
    } catch (FileFieldValidationException exception) {
      throw new AccessDeniedException(exception.getFileFieldError().getMessage());
    }
  }

  private ValidationException createValidationException(FileFieldValidationException exception) {
    var fileFieldError = exception.getFileFieldError();
    var error = ValidationErrorDto.builder()
        .traceId(fileFieldError.getTraceId())
        .code(fileFieldError.getCode())
        .message(fileFieldError.getMessage())
        .build();
    return new ValidationException(error);
  }
}
