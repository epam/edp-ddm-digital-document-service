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

import com.epam.digital.data.platform.dgtldcmnt.config.DigitalDocumentsConfigurationProperties;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentFromUserFormDto;
import com.epam.digital.data.platform.dgtldcmnt.exception.BatchFileMaxSizeException;
import com.epam.digital.data.platform.integration.formprovider.client.FormValidationClient;
import com.epam.digital.data.platform.integration.formprovider.dto.FileDataValidationDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FormFieldListValidationDto;
import com.epam.digital.data.platform.integration.formprovider.exception.SubmissionValidationException;
import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import com.epam.digital.data.platform.storage.file.dto.FileMetadataDto;
import com.epam.digital.data.platform.storage.file.service.FormDataFileStorageService;
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

  public static final String TOTAL_FILES_SIZE_EXCEEDS_MAX_BATCH_FILES_SIZE_MSG = "The total size of the downloaded files exceeds %sMB";

  private final DigitalDocumentsConfigurationProperties digitalDocumentsProperties;
  private final FormValidationClient formValidationClient;
  private final FormDataFileStorageService formDataFileStorageService;

  /**
   * Validate the uploaded document metadata based on the ui form metadata.
   *
   * @param uploadDto contains uploaded document metadata.
   */
  public void validate(UploadDocumentFromUserFormDto uploadDto) {
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

  public void verifyTotalFilesSize(UploadDocumentFromUserFormDto uploadDto) {
    var metadata = formDataFileStorageService.getMetadata(uploadDto.getRootProcessInstanceId());
    var otherFilesSize = metadata.stream()
        .filter(md -> uploadDto.getFormKey().equals(md.getFormKey()) &&
            uploadDto.getFieldName().equals(md.getFieldName()))
        .mapToDouble(FileMetadataDto::getContentLength)
        .sum();
    var currentFileSize = uploadDto.getSize();
    if (currentFileSize + otherFilesSize > digitalDocumentsProperties.getMaxTotalFileSize()
        .toBytes()) {
      throw new BatchFileMaxSizeException(
          String.format(TOTAL_FILES_SIZE_EXCEEDS_MAX_BATCH_FILES_SIZE_MSG,
              digitalDocumentsProperties.getMaxTotalFileSize().toMegabytes()));
    }
  }
}
