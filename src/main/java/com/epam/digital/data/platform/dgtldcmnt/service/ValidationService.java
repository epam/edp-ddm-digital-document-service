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
import com.epam.digital.data.platform.integration.formprovider.exception.SubmissionValidationException;
import com.epam.digital.data.platform.starter.errorhandling.BaseRestExceptionHandler;
import com.epam.digital.data.platform.starter.errorhandling.dto.ValidationErrorDto;
import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import com.epam.digital.data.platform.storage.file.repository.FormDataFileRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Validation service that validates the document metadata based on the form metadata.
 */
@Slf4j
@Setter
@Component
@RequiredArgsConstructor
public class ValidationService {

  public static final String TOTAL_FILES_SIZE_EXCEEDS_MAX_BATCH_FILES_SIZE_MSG = "The total size of the downloaded files exceeds %sMB";

  @Value("${digital-document-service.max-batch-file-size-mb}")
  private Double maxBatchFileSize;
  private final FormValidationClient formValidationClient;
  private final FormDataFileRepository formDataFileRepository;

  /**
   * Validate the uploaded document metadata based on the ui form metadata.
   *
   * @param uploadDto contains uploaded document metadata.
   */
  public void validate(UploadDocumentDto uploadDto) {
    log.debug("Validating file {} in task {} in form {}", uploadDto.getFieldName(),
        uploadDto.getTaskId(), uploadDto.getFormKey());
    var fileData = FileDataValidationDto.builder()
        .contentType(uploadDto.getContentType())
        .documentKey(uploadDto.getFieldName())
        .fileName(uploadDto.getFilename())
        .size(uploadDto.getSize()).build();
    try {
      formValidationClient
          .validateFileField(uploadDto.getFormKey(), uploadDto.getFieldName(), fileData);
    } catch (SubmissionValidationException exception) {
      throw new ValidationException(exception.getErrors());
    }
    verifyTotalFilesSize(uploadDto);
    log.debug("File {} type and size are valid. Task {}", uploadDto.getFieldName(),
        uploadDto.getTaskId());
  }

  public void checkFieldNamesExistence(List<String> fieldNames, String formKey) {
    try {
      formValidationClient.checkFieldNames(formKey,
          FormFieldListValidationDto.builder().fields(fieldNames).build());
    } catch (SubmissionValidationException exception) {
      throw new AccessDeniedException(exception.getErrors().getMessage());
    }
  }

  public void verifyTotalFilesSize(UploadDocumentDto uploadDto) {
    var metadata = formDataFileRepository.getMetadata(uploadDto.getProcessInstanceId());
    var otherFilesSize = metadata.stream()
        .filter(md -> uploadDto.getFormKey().equals(md.getFormKey()) &&
            uploadDto.getFieldName().equals(md.getFieldName()))
        .mapToDouble(md -> getMBSize(md.getContentLength())).sum();
    var currentFileSize = getMBSize(uploadDto.getSize());
    if (currentFileSize + otherFilesSize > maxBatchFileSize) {
      throw createValidationException(
          String.format(TOTAL_FILES_SIZE_EXCEEDS_MAX_BATCH_FILES_SIZE_MSG, maxBatchFileSize));
    }
  }

  private double getMBSize(Long size) {
    return (double) size / (1024 * 1024);
  }

  private ValidationException createValidationException(String msg) {
    var error = ValidationErrorDto.builder()
        .traceId(MDC.get(BaseRestExceptionHandler.TRACE_ID_KEY))
        .code(String.valueOf(HttpStatus.UNPROCESSABLE_ENTITY.value()))
        .message(msg)
        .build();
    return new ValidationException(error);
  }
}
