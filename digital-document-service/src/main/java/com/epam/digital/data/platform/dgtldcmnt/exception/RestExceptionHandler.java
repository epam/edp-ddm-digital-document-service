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

package com.epam.digital.data.platform.dgtldcmnt.exception;

import com.epam.digital.data.platform.dgtldcmnt.validator.AllowedUploadedDocument;
import com.epam.digital.data.platform.starter.errorhandling.BaseRestExceptionHandler;
import com.epam.digital.data.platform.starter.errorhandling.dto.SystemErrorDto;
import com.epam.digital.data.platform.storage.file.exception.FileNotFoundException;
import java.lang.annotation.Annotation;
import javax.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class RestExceptionHandler {

  @Value("${spring.servlet.multipart.max-request-size}")
  private DataSize maxRequestSize;

  private static final String FILE_SIZE_IS_TOO_LARGE_CODE = "FILE_SIZE_IS_TOO_LARGE";
  private static final String BATCH_FILE_SIZE_IS_TOO_LARGE_CODE = "BATCH_FILE_SIZE_IS_TOO_LARGE";


  /**
   * It handles base spring-validation/servlet 'bad request' exceptions and wraps it into
   * {@link SystemErrorDto}.
   *
   * @param ex an exception to handle.
   * @return response entity with {@link SystemErrorDto}.
   */
  @ExceptionHandler({
      MethodArgumentNotValidException.class,
      MissingServletRequestPartException.class
  })
  public ResponseEntity<SystemErrorDto> handleBadRequest(Exception ex) {
    var error = SystemErrorDto.builder()
        .traceId(MDC.get(BaseRestExceptionHandler.TRACE_ID_KEY))
        .code(String.valueOf(HttpStatus.BAD_REQUEST.value()))
        .message(ex.getMessage())
        .build();
    log.error("Bad request error", ex);
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(FileNotFoundException.class)
  public ResponseEntity<SystemErrorDto> handleDocumentNotFound(FileNotFoundException ex) {
    var error = SystemErrorDto.builder()
        .traceId(MDC.get(BaseRestExceptionHandler.TRACE_ID_KEY))
        .code(String.valueOf(HttpStatus.NOT_FOUND.value()))
        .message(ex.getMessage())
        .build();
    log.error("Document not found error", ex);
    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<SystemErrorDto> handleAccessDeniedException(AccessDeniedException ex) {
    var error = SystemErrorDto.builder()
        .traceId(MDC.get(BaseRestExceptionHandler.TRACE_ID_KEY))
        .code(String.valueOf(HttpStatus.FORBIDDEN.value()))
        .message(ex.getMessage())
        .build();
    log.warn(BaseRestExceptionHandler.ACCESS_IS_DENIED, ex);
    return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
  public ResponseEntity<SystemErrorDto> handleUnsupportedMediaTypeStatusException(
      UnsupportedMediaTypeStatusException ex) {
    var error = SystemErrorDto.builder()
        .traceId(MDC.get(BaseRestExceptionHandler.TRACE_ID_KEY))
        .code(String.valueOf(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()))
        .message(ex.getMessage())
        .build();
    log.warn("Unsupported media-type - {}", ex.getContentType(), ex);
    return new ResponseEntity<>(error, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler
  public ResponseEntity<SystemErrorDto> handleConstraintViolationException(
      ConstraintViolationException exception) {
    var error = SystemErrorDto.builder()
        .traceId(MDC.get(BaseRestExceptionHandler.TRACE_ID_KEY))
        .message(exception.getMessage())
        .build();
    var annotation = getAnnotationFromConstraintViolationException(exception);
    if (annotation instanceof AllowedUploadedDocument) {
      log.warn(exception.getMessage());
      error.setCode(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase());
      return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
    }
    log.error("Constraint violation exception");
    error.setCode(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<SystemErrorDto> handleMaxFileSizeExceeded(
      MaxUploadSizeExceededException ex) {
    var error = SystemErrorDto.builder()
        .traceId(MDC.get(BaseRestExceptionHandler.TRACE_ID_KEY))
        .code(FILE_SIZE_IS_TOO_LARGE_CODE)
        .message(String.format("The size of the uploaded file exceeds %sMB", maxRequestSize.toMegabytes()))
        .build();
    log.error("File size is too large", ex);
    return new ResponseEntity<>(error, HttpStatus.PAYLOAD_TOO_LARGE);
  }

  @ExceptionHandler(BatchFileMaxSizeException.class)
  public ResponseEntity<SystemErrorDto> handleBatchFileMaxSizeException(
      BatchFileMaxSizeException ex) {
    var error = SystemErrorDto.builder()
        .traceId(MDC.get(BaseRestExceptionHandler.TRACE_ID_KEY))
        .code(BATCH_FILE_SIZE_IS_TOO_LARGE_CODE)
        .message(ex.getMessage())
        .build();
    log.error("Total file size is too large", ex);
    return new ResponseEntity<>(error, HttpStatus.PAYLOAD_TOO_LARGE);
  }

  private Annotation getAnnotationFromConstraintViolationException(
      ConstraintViolationException exception) {
    var constraintViolations = exception.getConstraintViolations();
    if (constraintViolations != null && !constraintViolations.isEmpty()) {
      var next = constraintViolations.iterator().next();
      if (next != null && next.getConstraintDescriptor() != null) {
        return next.getConstraintDescriptor().getAnnotation();
      }
    }
    return null;
  }
}
