package com.epam.digital.data.platform.dgtldcmnt.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentIdDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentsMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.exception.DocumentNotFoundException;
import com.epam.digital.data.platform.dgtldcmnt.util.CephKeyProvider;
import com.epam.digital.data.platform.integration.ceph.UserMetadataHeaders;
import com.epam.digital.data.platform.integration.ceph.service.S3ObjectCephService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.util.UriComponentsBuilder;

@RunWith(MockitoJUnitRunner.class)
public class CephDocumentServiceTest {

  @Mock
  private S3ObjectCephService s3ObjectCephService;

  private DocumentService service;

  private final String key = "testKey";
  private final String filename = "testFilename";
  private final String contentType = "application/pdf";
  private final String taskId = "testTaskId";
  private final String processInstanceId = "testProcessInstanceId";
  private final String fieldName = "testFieldName";
  private final String originRequestUrl = "test.com";
  private final Long contentLength = 1000L;
  private final byte[] data = new byte[]{1};

  private CephKeyProvider keyProvider = new CephKeyProvider();

  @Before
  public void init() {
    service = new CephDocumentService(s3ObjectCephService, keyProvider);
  }

  @Test
  public void testPutDocument() {
    var is = new ByteArrayInputStream(data);
    var testObjectMetaData = new ObjectMetadata();
    testObjectMetaData.setContentType(contentType);
    testObjectMetaData.setContentLength(contentLength);
    var uploadDto = UploadDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .originRequestUrl(originRequestUrl)
        .contentType(contentType)
        .fieldName(fieldName)
        .fileInputStream(is)
        .filename(filename)
        .taskId(taskId)
        .build();

    ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
    when(s3ObjectCephService
        .put(any(String.class), eq(contentType), captor.capture(), any(InputStream.class)))
        .thenReturn(testObjectMetaData);

    var savedDocMetadata = service.put(uploadDto);

    assertThat(savedDocMetadata).isNotNull();
    assertThat(savedDocMetadata.getType()).isEqualTo(contentType);
    assertThat(savedDocMetadata.getSize()).isEqualTo(contentLength);
    assertThat(savedDocMetadata.getName()).isEqualTo(filename);
    Map<String, String> userMetadata = captor.getValue();
    assertThat(userMetadata.get(UserMetadataHeaders.ID)).isNotEmpty();
    assertThat(userMetadata.get(UserMetadataHeaders.CHECKSUM)).isNotEmpty();
    assertThat(userMetadata.get(UserMetadataHeaders.FILENAME)).isEqualTo(filename);
    assertThat(savedDocMetadata.getChecksum())
        .isEqualTo(userMetadata.get(UserMetadataHeaders.CHECKSUM));
    assertThat(savedDocMetadata.getId()).isEqualTo(userMetadata.get(UserMetadataHeaders.ID));
    assertThat(savedDocMetadata.getUrl()).contains(userMetadata.get(UserMetadataHeaders.ID));
    var expectedUrl = UriComponentsBuilder.newInstance().scheme("https").host(originRequestUrl)
        .pathSegment("documents")
        .pathSegment(processInstanceId)
        .pathSegment(taskId)
        .pathSegment(fieldName)
        .pathSegment(userMetadata.get(UserMetadataHeaders.ID))
        .toUriString();
    assertThat(savedDocMetadata.getUrl()).isEqualTo(expectedUrl);
  }

  @Test
  public void testGetDocument() throws IOException {
    var id = keyProvider.generateKey(key, processInstanceId);
    var s3Object = new S3Object();
    s3Object.setKey(id);
    s3Object.setObjectContent(new ByteArrayInputStream(data));
    var objectMetadata = new ObjectMetadata();
    objectMetadata.setContentLength(contentLength);
    objectMetadata.setContentType(contentType);
    objectMetadata.setUserMetadata(Map.of(UserMetadataHeaders.FILENAME, filename));
    s3Object.setObjectMetadata(objectMetadata);
    var getDocumentDto = GetDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .id(key)
        .build();

    when(s3ObjectCephService.get(id)).thenReturn(Optional.of(s3Object));

    DocumentDto documentDto = service.get(getDocumentDto);

    assertThat(documentDto).isNotNull();
    assertThat(documentDto.getName()).isEqualTo(filename);
    assertThat(documentDto.getContentType()).isEqualTo(contentType);
    assertThat(documentDto.getSize()).isEqualTo(contentLength);
    assertThat(documentDto.getContent().readAllBytes()).isEqualTo(data);
  }

  @Test
  public void testGetDocumentThatNotFound() {
    when(s3ObjectCephService.get(keyProvider.generateKey(key, processInstanceId)))
        .thenReturn(Optional.empty());

    var exception = assertThrows(DocumentNotFoundException.class,
        () -> service
            .get(GetDocumentDto.builder().processInstanceId(processInstanceId).id(key).build()));

    assertThat(exception.getIds().iterator().next()).isEqualTo(key);
  }

  @Test
  public void testGetMetadata() {
    var objectMetadata = new ObjectMetadata();
    objectMetadata.setContentLength(contentLength);
    objectMetadata.setContentType(contentType);
    objectMetadata.setUserMetadata(Map.of(UserMetadataHeaders.FILENAME, "test.pdf"));
    var getMetadataDto = GetDocumentsMetadataDto.builder()
        .documents(List.of(DocumentIdDto.builder().id(key).fieldName(fieldName).build()))
        .processInstanceId(processInstanceId)
        .originRequestUrl(originRequestUrl)
        .taskId(taskId)
        .build();

    when(s3ObjectCephService.getMetadata(List.of(keyProvider.generateKey(key, processInstanceId))))
        .thenReturn(Optional.of(List.of(objectMetadata)));

    var metadata = service.getMetadata(getMetadataDto);
    assertThat(metadata.size()).isOne();
    assertThat(metadata.get(0).getType()).isEqualTo(contentType);
    assertThat(metadata.get(0).getSize()).isEqualTo(contentLength);
  }

  @Test
  public void testGetMetadataDocumentNotFound() {
    var getMetadataDto = GetDocumentsMetadataDto.builder()
        .documents(List.of(DocumentIdDto.builder().id(key).fieldName(fieldName).build()))
        .processInstanceId(processInstanceId)
        .originRequestUrl(originRequestUrl)
        .taskId(taskId)
        .build();
    when(s3ObjectCephService.getMetadata(List.of(keyProvider.generateKey(key, processInstanceId))))
        .thenReturn(Optional.empty());

    var exception = assertThrows(DocumentNotFoundException.class,
        () -> service.getMetadata(getMetadataDto));

    assertThat(exception.getIds().iterator().next()).isEqualTo(key);
  }
}