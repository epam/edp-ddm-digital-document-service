package com.epam.digital.data.platform.dgtldcmnt.service;

import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.starter.errorhandling.BaseRestExceptionHandler;
import com.epam.digital.data.platform.starter.errorhandling.dto.ValidationErrorDto;
import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import com.epam.digital.data.platform.starter.validation.dto.ComponentsDto;
import com.epam.digital.data.platform.starter.validation.dto.FormDto;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeTypeUtils;

/**
 * Validation service that validates the document metadata based on the form metadata.
 */
@Component
public class ValidationService {

  public static final String FIELD_NOT_FOUND_MSG = "The field (%s) is not found on the ui form";
  public static final String UNSUPPORTED_FILE_SIZE_DEFINITION_MSG = "The file size definition specified in the ui form is not supported (%s). Supported file size definitions %s.";
  public static final String FILE_SIZE_EXCEEDS_MAX_FILE_SIZE_MSG = "The size of the downloaded file exceeds %s";
  public static final String FILE_TYPE_IS_NOT_SUPPORTED_MSG = "The type of the downloaded file is not supported. Supported types %s.";

  public static final String MEGA_BYTES = "MB";
  public static final String DIGIT_REGEX = "\\d";
  public static final String NON_DIGIT_REGEX = "\\D+";

  public static final Set<String> ALLOWED_FILE_SIZE_DEFINITIONS = Set.of(MEGA_BYTES);

  /**
   * Validate the uploaded document metadata based on the ui form metadata.
   *
   * @param uploadDto contains uploaded document metadata.
   * @param formDto   contains metadata of the specified ui form.
   */
  public void validate(UploadDocumentDto uploadDto, FormDto formDto) {
    var fileComponent = formDto.getComponents().stream()
        .filter(c -> "file".equals(c.getType()) && uploadDto.getFieldName().equals(c.getKey()))
        .findFirst().orElseThrow(() -> createValidationException(
            String.format(FIELD_NOT_FOUND_MSG, uploadDto.getFieldName())));

    validateFileMaxSize(uploadDto.getSize(), fileComponent);
    validateFileType(uploadDto.getContentType(), fileComponent);
  }

  private void validateFileMaxSize(Long size, ComponentsDto componentsDto) {
    var fileMaxSizePattern = componentsDto.getFileMaxSize();
    var fileSizeDefinition = fileMaxSizePattern.replaceAll(DIGIT_REGEX, StringUtils.EMPTY);
    if (!ALLOWED_FILE_SIZE_DEFINITIONS.contains(fileSizeDefinition)) {
      throw createValidationException(
          String.format(UNSUPPORTED_FILE_SIZE_DEFINITION_MSG, fileSizeDefinition,
              ALLOWED_FILE_SIZE_DEFINITIONS));
    }
    var fileSize = getFileSizeBySizeDefinition(size, fileSizeDefinition);
    var fileMaxSize =
        Double.valueOf(fileMaxSizePattern.replaceAll(NON_DIGIT_REGEX, StringUtils.EMPTY));
    if (fileSize > fileMaxSize) {
      throw createValidationException(
          String.format(FILE_SIZE_EXCEEDS_MAX_FILE_SIZE_MSG, fileMaxSizePattern));
    }
  }

  private void validateFileType(String contentType, ComponentsDto componentsDto) {
    var supportedMimeTypes = wrapInvalidMimeType(
        () -> MimeTypeUtils.parseMimeTypes(componentsDto.getFilePattern()));
    var allTypes = supportedMimeTypes.stream().anyMatch(MimeTypeUtils.ALL::equals);
    var fileMimeType = wrapInvalidMimeType(() -> MimeTypeUtils.parseMimeType(contentType));
    if (!allTypes && !supportedMimeTypes.contains(fileMimeType)) {
      throw createValidationException(
          String.format(FILE_TYPE_IS_NOT_SUPPORTED_MSG, supportedMimeTypes));
    }
  }

  private ValidationException createValidationException(String msg) {
    var error = ValidationErrorDto.builder()
        .traceId(MDC.get(BaseRestExceptionHandler.TRACE_ID_KEY))
        .code(String.valueOf(HttpStatus.UNPROCESSABLE_ENTITY.value()))
        .message(msg)
        .build();
    return new ValidationException(error);
  }

  protected <T> T wrapInvalidMimeType(Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (InvalidMimeTypeException exception) {
      throw createValidationException(exception.getMessage());
    }
  }

  private Double getFileSizeBySizeDefinition(Long size, String fileSizeDefinition) {
    if (MEGA_BYTES.equals(fileSizeDefinition)) {
      return getSizeInMegaBytes(size);
    }
    throw createValidationException(
        String.format(UNSUPPORTED_FILE_SIZE_DEFINITION_MSG, fileSizeDefinition,
            ALLOWED_FILE_SIZE_DEFINITIONS));
  }

  private Double getSizeInMegaBytes(Long size) {
    return (double) size / (1024 * 1024);
  }
}