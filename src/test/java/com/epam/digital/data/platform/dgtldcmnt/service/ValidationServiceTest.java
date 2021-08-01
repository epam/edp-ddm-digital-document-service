package com.epam.digital.data.platform.dgtldcmnt.service;

import static com.epam.digital.data.platform.dgtldcmnt.service.ValidationService.ALLOWED_FILE_SIZE_DEFINITIONS;
import static com.epam.digital.data.platform.dgtldcmnt.service.ValidationService.FIELD_NOT_FOUND_MSG;
import static com.epam.digital.data.platform.dgtldcmnt.service.ValidationService.FILE_SIZE_EXCEEDS_MAX_FILE_SIZE_MSG;
import static com.epam.digital.data.platform.dgtldcmnt.service.ValidationService.FILE_TYPE_IS_NOT_SUPPORTED_MSG;
import static com.epam.digital.data.platform.dgtldcmnt.service.ValidationService.UNSUPPORTED_FILE_SIZE_DEFINITION_MSG;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import com.epam.digital.data.platform.starter.validation.dto.ComponentsDto;
import com.epam.digital.data.platform.starter.validation.dto.FormDto;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ValidationServiceTest {

  private final String taskId = "testTaskId";
  private final String processInstanceId = "testProcessInstanceId";
  private final String fieldName = "testUpload1";
  private final String filePattern = "application/pdf";
  private final String fileMaxSize = "50MB";
  private final Long size = 1000L;

  private final ValidationService validationService = new ValidationService();

  private FormDto formDto;

  @Before
  public void init() {
    var componentsDtos = List
        .of(new ComponentsDto(fieldName, "file", null, filePattern, fileMaxSize));
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
        .of(new ComponentsDto(fieldName, "file", null, "*", fileMaxSize));
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
        .of(new ComponentsDto("stingFiled", "string", null, null, null));
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
        .of(new ComponentsDto(fieldName, "file", null, filePattern, invalidFilePattern));
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
        .of(new ComponentsDto(fieldName, "file", null, "*.zip", fileMaxSize));
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
