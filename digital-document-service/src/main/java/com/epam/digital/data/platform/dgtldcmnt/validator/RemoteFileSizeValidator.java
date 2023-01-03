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

import com.epam.digital.data.platform.starter.errorhandling.BaseRestExceptionHandler;
import com.epam.digital.data.platform.starter.errorhandling.dto.ValidationErrorDto;
import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Validates the size of a file that is downloaded from a remote source. It must be no larger than
 * specified in the digital-document-service.max-remote-file-size-mb parameter
 */
@Component
public class RemoteFileSizeValidator implements FileSizeValidator {

  public static final String FILE_SIZE_EXCEEDS_LIMIT = "File size exceeded %s MB";

  private final double maxFileSizeMegabytes;
  private final long maxFileSizeBytes;

  public RemoteFileSizeValidator(
      @Value("${digital-document-service.max-remote-file-size-mb}") double maxFileSizeMegabytes) {
    this.maxFileSizeMegabytes = maxFileSizeMegabytes;
    this.maxFileSizeBytes = (long) (maxFileSizeMegabytes * 1024 * 1024);
  }

  /**
   * Validates current amount of bytes.
   *
   * @param numberOfBytes contains uploaded document metadata.
   */
  @Override
  public void validate(long numberOfBytes) {
    if (numberOfBytes > maxFileSizeBytes) {
      throw createValidationException(String.format(FILE_SIZE_EXCEEDS_LIMIT, maxFileSizeMegabytes));
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
}
