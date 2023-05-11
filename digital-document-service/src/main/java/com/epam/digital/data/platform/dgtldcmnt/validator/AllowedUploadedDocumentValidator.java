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

package com.epam.digital.data.platform.dgtldcmnt.validator;

import com.epam.digital.data.platform.dgtldcmnt.constant.DocumentConstants;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * Class that validates input file Content-Type and file extension.
 * <ol>
 * <li>Checks if input Content-Type is in list of supported types</li>
 * <li>Checks if file extension corresponds to input Content-Type</li>
 * <li>Checks if real file Content-Type is equal to input Content-Type
 * <p>
 * Note: If signedFileDetectionEnabled == false and real Content-Type is
 * application/pkcs7-signature then it will be passed
 * </li>
 * </ol>
 */
@Slf4j
@RequiredArgsConstructor
public class AllowedUploadedDocumentValidator implements
    ConstraintValidator<AllowedUploadedDocument, UploadDocumentDto> {

  private final Tika tika;
  @Value("${media-type-validation.enabled:true}")
  private final boolean mediaTypeValidationEnabled;
  @Value("${media-type-validation.signed-file-detection.enabled:true}")
  private final boolean signedFileDetectionEnabled;

  private boolean isFilenameExtensionValid(UploadDocumentDto uploadDocumentDto,
      ConstraintValidatorContext context) {
    log.debug("Validating filename extension for process-instance '{}'",
        uploadDocumentDto.getProcessInstanceId());
    final var contentType = uploadDocumentDto.getContentType();
    var acceptedExtensions = getAcceptedExtensions(contentType);

    final var fileExtension = FilenameUtils.getExtension(uploadDocumentDto.getFilename());
    if (StringUtils.isBlank(fileExtension)) {
      context.buildConstraintViolationWithTemplate("Filename doesn't have any extensions")
          .addPropertyNode("filename")
          .addConstraintViolation()
          .disableDefaultConstraintViolation();
      return false;
    }
    log.trace("Filename extension - '{}'", fileExtension);

    var isExtensionValid = acceptedExtensions.contains(fileExtension);
    if (!isExtensionValid) {
      context.buildConstraintViolationWithTemplate(
              "File extension doesn't correspond to input media type")
          .addPropertyNode("filename")
          .addConstraintViolation()
          .disableDefaultConstraintViolation();
    }
    log.debug("Filename extension '{}' with content-type '{}'  is valid - '{}'",
        fileExtension, contentType, isExtensionValid);
    return isExtensionValid;
  }

  private Set<String> getAcceptedExtensions(String contentType) {
    log.trace("Founding accepted extension for input content-type - '{}'", contentType);
    var acceptedExtensions = DocumentConstants.MEDIA_TYPE_TO_EXTENSIONS_MAP.get(contentType);
    if (Objects.nonNull(acceptedExtensions)) {
      log.trace("Found extension for input content-type '{}' - '{}'", contentType,
          acceptedExtensions);
      return acceptedExtensions;
    }
    throw new UnsupportedMediaTypeStatusException(MediaType.parseMediaType(contentType),
        DocumentConstants.SUPPORTED_MEDIA_TYPES);
  }

  private boolean isDetectedFileContentTypeEqualsToInputContentType(
      UploadDocumentDto uploadDocumentDto, ConstraintValidatorContext context) {
    log.debug("Validating file content type for process-instance '{}'",
        uploadDocumentDto.getProcessInstanceId());
    final var filename = uploadDocumentDto.getFilename();
    final var inputFile = uploadDocumentDto.getFileInputStream();
    final String fileContentType;
    try {
      fileContentType = tika.detect(inputFile, filename);
    } catch (IOException e) {
      context.buildConstraintViolationWithTemplate(
              "Couldn't read the file to detect file content type")
          .addPropertyNode("fileInputStream")
          .addConstraintViolation()
          .disableDefaultConstraintViolation();
      return false;
    }

    final var inputContentType = uploadDocumentDto.getContentType();
    final var isDetectedContentTypeEqualsToInputContentType = fileContentType.equals(
        inputContentType);
    final var isDetectedContentTypeCorrespondsToInputContentType =
        DocumentConstants.CORRESPONDED_MEDIA_TYPES
            .getOrDefault(inputContentType, Set.of())
            .contains(fileContentType);
    final var shouldPassSignatureType = !signedFileDetectionEnabled && fileContentType.equals(
        DocumentConstants.SIGNATURE_TYPE);

    final var isValid = isDetectedContentTypeEqualsToInputContentType
        || isDetectedContentTypeCorrespondsToInputContentType
        || shouldPassSignatureType;
    if (!isValid) {
      context.buildConstraintViolationWithTemplate(
              "Detected file content type doesn't match input content type")
          .addPropertyNode("fileInputStream")
          .addConstraintViolation()
          .disableDefaultConstraintViolation();
    }
    log.debug("Detected file content type '{}' corresponds to input content-type '{}' - '{}'."
            + "Content type detection for singed files enabled - '{}'",
        fileContentType, inputContentType, isValid, signedFileDetectionEnabled);
    return isValid;
  }

  @Override
  public boolean isValid(UploadDocumentDto value, ConstraintValidatorContext context) {
    log.debug("Validating input document. Process-instance - '{}'. Validation enabled - '{}'",
        value.getProcessInstanceId(), mediaTypeValidationEnabled);
    var isValid = !mediaTypeValidationEnabled || (isFilenameExtensionValid(value, context)
        && isDetectedFileContentTypeEqualsToInputContentType(value, context));
    log.debug("Input document for process '{}' is valid - '{}'.", value.getProcessInstanceId(),
        isValid);
    return isValid;
  }
}
