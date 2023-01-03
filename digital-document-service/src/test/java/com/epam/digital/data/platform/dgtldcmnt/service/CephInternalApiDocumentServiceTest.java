package com.epam.digital.data.platform.dgtldcmnt.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.dgtldcmnt.dto.UploadRemoteDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.validator.RemoteFileSizeValidator;
import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import com.epam.digital.data.platform.storage.file.dto.BaseFileMetadataDto;
import com.epam.digital.data.platform.storage.file.dto.BaseFileMetadataDto.BaseUserMetadataHeaders;
import com.epam.digital.data.platform.storage.file.dto.FileObjectDto;
import com.epam.digital.data.platform.storage.file.service.FileStorageService;
import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CephInternalApiDocumentServiceTest {

  @Mock
  private FileStorageService storageService;
  private InternalApiDocumentService documentService;

  private static final String FILE_ID = "fileId";
  private static final String FILENAME = "testFilename";
  private static final String CONTENT_TYPE = "image/png";
  private static final String PROCESS_INSTANCE_ID = "testProcessInstanceId";
  private static final long CONTENT_LENGTH = 1000L;
  // expected checksum is checksum of empty inputStream because storageService is a mock and 
  // it doesn't read the stream
  private static final String EXPECTED_CHECKSUM =
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
  private static final byte[] DATA = new byte[]{1, 2, 3};

  @BeforeEach
  public void init() {
    documentService = new CephInternalApiDocumentService(storageService,
        new RemoteFileSizeValidator(1));
  }

  @Test
  void shouldThrowExceptionWhenContentLengthMoreThenAllowed() {
    var uploadDto = UploadRemoteDocumentDto.builder()
        .contentLength(1024 * 1024 + 1)
        .build();

    assertThrows(ValidationException.class, () -> documentService.put(uploadDto));
  }

  @Test
  void testPutDocument() {
    var is = new ByteArrayInputStream(DATA);
    var baseFileMetadataDto = new BaseFileMetadataDto(
        CONTENT_LENGTH, CONTENT_TYPE, buildUserMetadata(FILE_ID, FILENAME));

    var uploadDto = UploadRemoteDocumentDto.builder()
        .filename(FILENAME)
        .contentType(CONTENT_TYPE)
        .contentLength(0)
        .inputStream(is)
        .processInstanceId(PROCESS_INSTANCE_ID)
        .build();

    ArgumentCaptor<FileObjectDto> captor = ArgumentCaptor.forClass(FileObjectDto.class);
    when(storageService.save(eq(PROCESS_INSTANCE_ID), anyString(),
        captor.capture())).thenReturn(baseFileMetadataDto);

    var savedDocMetadata = documentService.put(uploadDto);

    assertThat(savedDocMetadata).isNotNull();
    assertThat(savedDocMetadata.getId()).isEqualTo(FILE_ID);
    assertThat(savedDocMetadata.getName()).isEqualTo(FILENAME);
    assertThat(savedDocMetadata.getType()).isEqualTo(CONTENT_TYPE);
    assertThat(savedDocMetadata.getChecksum()).isEqualTo(EXPECTED_CHECKSUM);
    assertThat(savedDocMetadata.getSize()).isEqualTo(CONTENT_LENGTH);

    var fileMetadataDto = captor.getValue().getMetadata();
    assertThat(fileMetadataDto.getContentLength()).isZero();
    assertThat(fileMetadataDto.getContentType()).isEqualTo(CONTENT_TYPE);
    assertThat(fileMetadataDto.getId()).isNotEqualTo(FILE_ID); // should be generated
    assertThat(fileMetadataDto.getChecksum()).isEqualTo(EXPECTED_CHECKSUM);
    assertThat(fileMetadataDto.getFilename()).isEqualTo(FILENAME);
  }

  private Map<String, String> buildUserMetadata(String fileId, String fileName) {
    Map<String, String> userMetadata = new LinkedHashMap<>();
    userMetadata.put(BaseUserMetadataHeaders.ID, fileId);
    userMetadata.put(BaseUserMetadataHeaders.FILENAME, fileName);
    return userMetadata;
  }
}
