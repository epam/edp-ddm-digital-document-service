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
import java.io.InputStream;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import lombok.SneakyThrows;
import org.apache.tika.Tika;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

@ExtendWith(SpringExtension.class)
class AllowedUploadedDocumentValidatorTest {

  @Mock
  Tika tika;
  @Mock
  InputStream inputStream;
  @Mock
  ConstraintValidatorContext context;
  @Mock
  ConstraintViolationBuilder builder;
  @Mock
  NodeBuilderCustomizableContext customizableContext;

  @BeforeEach
  void setUp() {
    Mockito.doReturn(builder).when(context)
        .buildConstraintViolationWithTemplate(Mockito.anyString());
    Mockito.doReturn(customizableContext).when(builder).addPropertyNode(Mockito.anyString());
    Mockito.doReturn(context).when(customizableContext).addConstraintViolation();
  }

  @ParameterizedTest(name = "{0} equals to {2} and {1} corresponds to {2}")
  @DisplayName("should return true if input contentType equals to detected contentType and file extension corresponds to input contentType")
  @CsvSource({
      "application/pdf,file.pdf,application/pdf",
      "image/jpeg,file.jpeg,image/jpeg",
      "image/jpeg,file.jpg,image/jpeg",
      "image/png,file.png,image/png",
      "text/csv,file.csv,text/csv",
      "application/pkcs7-signature,file.docx.p7s,application/pkcs7-signature",
      "application/octet-stream,file.pdf.asics,application/octet-stream",
      "application/vnd.etsi.asic-s+zip,file.pdf.asics,application/octet-stream"
  })
  @SneakyThrows
  void validate_success(String detectedContentType, String filename, String inputContentType) {
    final var uploadDocumentDto = UploadDocumentDto.builder()
        .fileInputStream(inputStream)
        .filename(filename)
        .contentType(inputContentType)
        .build();
    Mockito.doReturn(detectedContentType).when(tika).detect(inputStream, filename);

    final var validator = new AllowedUploadedDocumentValidator(tika, true, false);

    Assertions.assertThat(validator.isValid(uploadDocumentDto, context)).isTrue();
    Mockito.verify(tika).detect(inputStream, filename);
  }

  @ParameterizedTest(name = "{1} is not supported")
  @DisplayName("should throw an UnsupportedMediaTypeException if input contentType is not supported")
  @CsvSource({
      "file.gif,image/gif",
      "file.xml,application/xml",
      "file.json,application/json",
      "file.exe,application/vnd.microsoft.portable-executable",
      "file.bat,application/bat"
  })
  @SneakyThrows
  void validate_unsupportedMediaType(String filename, String inputContentType) {
    final var uploadDocumentDto = UploadDocumentDto.builder()
        .fileInputStream(inputStream)
        .filename(filename)
        .contentType(inputContentType)
        .build();

    final var validator = new AllowedUploadedDocumentValidator(tika, true, false);

    Assertions.assertThatThrownBy(() -> validator.isValid(uploadDocumentDto, context))
        .isInstanceOf(UnsupportedMediaTypeStatusException.class)
        .hasMessage("415 UNSUPPORTED_MEDIA_TYPE \"Content type '%s' not supported\"",
            inputContentType);

    Mockito.verifyNoInteractions(tika);
  }

  @ParameterizedTest(name = "{1} differs from detected application/pkcs7-signature")
  @DisplayName("should always pass on signed files if input contentType is supported and file extension is valid and signedFileDetectionEnabled == false")
  @CsvSource({
      "file.pdf,application/pdf",
      "file.jpeg,image/jpeg",
      "file.jpg,image/jpeg",
      "file.png,image/png",
      "file.csv,text/csv"
  })
  @SneakyThrows
  void validate_skipSignedFilesIfDetectionDisabled(String filename, String inputContentType) {
    final var uploadDocumentDto = UploadDocumentDto.builder()
        .fileInputStream(inputStream)
        .filename(filename)
        .contentType(inputContentType)
        .build();

    Mockito.doReturn(DocumentConstants.SIGNATURE_TYPE).when(tika)
        .detect(inputStream, filename);

    final var validator = new AllowedUploadedDocumentValidator(tika, true, false);

    Assertions.assertThat(validator.isValid(uploadDocumentDto, context)).isTrue();
    Mockito.verify(tika).detect(inputStream, filename);
  }

  @ParameterizedTest(name = "{0} has no extensions")
  @DisplayName("should return false if filename hasn't any extensions")
  @CsvSource({
      "file,application/pdf",
      "file.,application/pdf",
      "file?jpeg,image/jpeg",
      "file/jpg,image/jpeg",
      "file;png,image/png",
      "file\"csv,text/csv"
  })
  @SneakyThrows
  void validate_filenameWithoutExtensions(String filename, String inputContentType) {
    final var uploadDocumentDto = UploadDocumentDto.builder()
        .fileInputStream(inputStream)
        .filename(filename)
        .contentType(inputContentType)
        .build();

    final var validator = new AllowedUploadedDocumentValidator(tika, true, false);

    Assertions.assertThat(validator.isValid(uploadDocumentDto, context)).isFalse();

    Mockito.verifyNoInteractions(tika);
    Mockito.verify(context)
        .buildConstraintViolationWithTemplate("Filename doesn't have any extensions");
    Mockito.verify(builder).addPropertyNode("filename");
    Mockito.verify(customizableContext).addConstraintViolation();
    Mockito.verify(context).disableDefaultConstraintViolation();
  }

  @ParameterizedTest(name = "{1} is supported and {0} doesn't correspond to {1}")
  @DisplayName("should return false if filename has an extension that doesn't correspond to input media type")
  @CsvSource({
      "file.dat,application/pdf",
      "file.jpeg.pdf,image/jpeg",
      "file.gif,image/jpeg",
      "file.exe,image/png",
      "file.txt,text/csv"
  })
  @SneakyThrows
  void validate_invalidExtension(String filename, String inputContentType) {
    final var uploadDocumentDto = UploadDocumentDto.builder()
        .fileInputStream(inputStream)
        .filename(filename)
        .contentType(inputContentType)
        .build();

    final var validator = new AllowedUploadedDocumentValidator(tika, true, false);

    Assertions.assertThat(validator.isValid(uploadDocumentDto, context)).isFalse();

    Mockito.verifyNoInteractions(tika);
    Mockito.verify(context).buildConstraintViolationWithTemplate(
        "File extension doesn't correspond to input media type");
    Mockito.verify(builder).addPropertyNode("filename");
    Mockito.verify(customizableContext).addConstraintViolation();
    Mockito.verify(context).disableDefaultConstraintViolation();
  }

  @ParameterizedTest(name = "{2} differs from detected {0}")
  @DisplayName("should always return false if detected media type doesn't equal to input media type and signedFileDetectionEnabled == true")
  @CsvSource({
      "image/jpeg,file.pdf,application/pdf",
      "application/vnd.microsoft.portable-executable,file.jpeg,image/jpeg",
      "application/bat,file.jpg,image/jpeg",
      "application/pkcs7-signature,file.png,image/png",
      "text/plain,file.csv,text/csv"
  })
  @SneakyThrows
  void validate_invalidDetectedContentType(String detectedContentType, String filename,
      String inputContentType) {
    final var uploadDocumentDto = UploadDocumentDto.builder()
        .fileInputStream(inputStream)
        .filename(filename)
        .contentType(inputContentType)
        .build();

    Mockito.doReturn(detectedContentType).when(tika).detect(inputStream, filename);

    final var validator = new AllowedUploadedDocumentValidator(tika, true, true);

    Assertions.assertThat(validator.isValid(uploadDocumentDto, context)).isFalse();

    Mockito.verify(tika).detect(inputStream, filename);
    Mockito.verify(context).buildConstraintViolationWithTemplate(
        "Detected file content type doesn't match input content type");
    Mockito.verify(builder).addPropertyNode("fileInputStream");
    Mockito.verify(customizableContext).addConstraintViolation();
    Mockito.verify(context).disableDefaultConstraintViolation();
  }

  @ParameterizedTest(name = "{2} differs from detected {0}")
  @DisplayName("should always return false on unsigned files if detected media type doesn't equal to input media type and signedFileDetectionEnabled == false")
  @CsvSource({
      "image/jpeg,file.pdf,application/pdf",
      "application/vnd.microsoft.portable-executable,file.jpeg,image/jpeg",
      "application/bat,file.jpg,image/jpeg",
      "application/pdf,file.png,image/png",
      "text/plain,file.csv,text/csv"
  })
  @SneakyThrows
  void validate_invalidDetectedContentTypeOnUnsignedFiles(String detectedContentType,
      String filename, String inputContentType) {
    final var uploadDocumentDto = UploadDocumentDto.builder()
        .fileInputStream(inputStream)
        .filename(filename)
        .contentType(inputContentType)
        .build();

    Mockito.doReturn(detectedContentType).when(tika).detect(inputStream, filename);

    final var validator = new AllowedUploadedDocumentValidator(tika, true, false);

    Assertions.assertThat(validator.isValid(uploadDocumentDto, context)).isFalse();

    Mockito.verify(tika).detect(inputStream, filename);
    Mockito.verify(context).buildConstraintViolationWithTemplate(
        "Detected file content type doesn't match input content type");
    Mockito.verify(builder).addPropertyNode("fileInputStream");
    Mockito.verify(customizableContext).addConstraintViolation();
    Mockito.verify(context).disableDefaultConstraintViolation();
  }

  @ParameterizedTest(name = "{2} differs from detected {0}")
  @DisplayName("should always return true if media type validation is disabled")
  @CsvSource({
      "image/jpeg,file.pdf,application/pdf",
      "application/vnd.microsoft.portable-executable,file.jpeg,image/jpeg",
      "application/bat,file.jpg,image/jpeg",
      "application/pdf,file.png,image/png",
      "text/plain,file.csv,text/csv"
  })
  @SneakyThrows
  void validate_validationDisabled(String detectedContentType, String filename,
      String inputContentType) {
    final var uploadDocumentDto = UploadDocumentDto.builder()
        .fileInputStream(inputStream)
        .filename(filename)
        .contentType(inputContentType)
        .build();

    Mockito.doReturn(detectedContentType).when(tika).detect(inputStream, filename);

    final var validator = new AllowedUploadedDocumentValidator(tika, false, true);

    Assertions.assertThat(validator.isValid(uploadDocumentDto, context)).isTrue();

    Mockito.verifyNoInteractions(tika);
    Mockito.verifyNoInteractions(context);
  }

  @Test
  @DisplayName("should return false if faced IOException on file content detection")
  @SneakyThrows
  void validate_ioExceptionOccurred() {
    final var uploadDocumentDto = UploadDocumentDto.builder()
        .fileInputStream(inputStream)
        .filename("file.pdf")
        .contentType("application/pdf")
        .build();

    Mockito.doThrow(IOException.class).when(tika).detect(inputStream, "file.pdf");

    final var validator = new AllowedUploadedDocumentValidator(tika, true, false);

    Assertions.assertThat(validator.isValid(uploadDocumentDto, context)).isFalse();

    Mockito.verify(tika).detect(inputStream, "file.pdf");
    Mockito.verify(context).buildConstraintViolationWithTemplate(
        "Couldn't read the file to detect file content type");
    Mockito.verify(builder).addPropertyNode("fileInputStream");
    Mockito.verify(customizableContext).addConstraintViolation();
    Mockito.verify(context).disableDefaultConstraintViolation();
  }
}
