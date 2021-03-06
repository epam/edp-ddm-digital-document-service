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

import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentIdDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentsMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.starter.logger.annotation.Confidential;
import com.epam.digital.data.platform.storage.file.dto.FileDataDto;
import com.epam.digital.data.platform.storage.file.dto.FileMetadataDto;
import com.epam.digital.data.platform.storage.file.service.FormDataFileStorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The service for management of the documents based on ceph storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CephDocumentService implements DocumentService {

  private final FormDataFileStorageService storage;

  @Override
  public DocumentMetadataDto put(UploadDocumentDto uploadDocumentDto) {
    var id = UUID.randomUUID().toString();
    log.debug("Uploading file {}, id {}, processInstanceId {}, taskId {}",
        uploadDocumentDto.getFilename(), id, uploadDocumentDto.getProcessInstanceId(),
        uploadDocumentDto.getTaskId());
    byte[] data = readBytes(uploadDocumentDto.getFileInputStream());
    var sha256hex = DigestUtils.sha256Hex(data);
    var fileMetadata = buildFileMetadata(id, sha256hex, uploadDocumentDto);
    var fileDataDto = FileDataDto.builder().content(new ByteArrayInputStream(data))
        .metadata(fileMetadata).build();
    var savedFileMetadata = storage.save(uploadDocumentDto.getProcessInstanceId(), id, fileDataDto);
    var url = generateGetDocumentUrl(id, uploadDocumentDto);
    log.debug("File {} uploaded. Id {}", uploadDocumentDto.getFilename(), id);
    return DocumentMetadataDto.builder()
        .size(savedFileMetadata.getContentLength())
        .name(uploadDocumentDto.getFilename())
        .type(savedFileMetadata.getContentType())
        .checksum(sha256hex)
        .url(url)
        .id(id)
        .build();
  }

  @Override
  @Confidential
  public DocumentDto get(GetDocumentDto getDocumentDto) {
    log.debug("Getting document with id {}", getDocumentDto.getId());
    var fileData = storage.loadByProcessInstanceIdAndId(getDocumentDto.getProcessInstanceId(),
        getDocumentDto.getId());
    log.debug("File downloaded. Id {}", getDocumentDto.getId());
    return DocumentDto.builder()
        .name(decodeUtf8(fileData.getMetadata().getFilename()))
        .contentType(fileData.getMetadata().getContentType())
        .size(fileData.getMetadata().getContentLength())
        .content(fileData.getContent())
        .build();
  }

  @Override
  public List<DocumentMetadataDto> getMetadata(GetDocumentsMetadataDto getMetadataDto) {
    log.debug("Getting documents metadata by ids {}", getMetadataDto.getDocuments());
    var documentIdAndFiledNameMap = getMetadataDto.getDocuments().stream()
        .collect(Collectors.toMap(DocumentIdDto::getId, DocumentIdDto::getFieldName));
    var result = storage.getMetadata(getMetadataDto.getProcessInstanceId(),
            documentIdAndFiledNameMap.keySet()).stream()
        .map(objectMetadata -> map(objectMetadata, getMetadataDto, documentIdAndFiledNameMap))
        .collect(Collectors.toList());
    log.debug("Documents metadata by ids {} received", getMetadataDto.getDocuments());
    return result;
  }

  @Override
  public void delete(String processInstanceId) {
    log.debug("Deleting all documents associated with process instance id {}", processInstanceId);
    storage.deleteByProcessInstanceId(processInstanceId);
    log.debug("All documents associated with process instance id {} were deleted successfully",
        processInstanceId);
  }

  private DocumentMetadataDto map(FileMetadataDto fileMetadataDto,
      GetDocumentsMetadataDto getMetadataDto, Map<String, String> documentIdAndFiledNameMap) {
    var id = fileMetadataDto.getId();
    var url = generateGetDocumentUrl(getMetadataDto.getOriginRequestUrl(),
        getMetadataDto.getProcessInstanceId(), getMetadataDto.getTaskId(),
        documentIdAndFiledNameMap.get(id), id);
    return DocumentMetadataDto.builder()
        .name(decodeUtf8(fileMetadataDto.getFilename()))
        .size(fileMetadataDto.getContentLength())
        .checksum(fileMetadataDto.getChecksum())
        .type(fileMetadataDto.getContentType())
        .url(url)
        .id(id)
        .build();
  }

  private String generateGetDocumentUrl(String host, String processInstanceId,
      String taskId, String fieldName, String id) {
    return UriComponentsBuilder.newInstance().scheme("https").host(host).pathSegment("documents")
        .pathSegment(processInstanceId)
        .pathSegment(taskId)
        .pathSegment(fieldName)
        .pathSegment(id)
        .toUriString();
  }

  private String generateGetDocumentUrl(String fileId, UploadDocumentDto uploadDocumentDto) {
    return this.generateGetDocumentUrl(uploadDocumentDto.getOriginRequestUrl(),
        uploadDocumentDto.getProcessInstanceId(), uploadDocumentDto.getTaskId(),
        uploadDocumentDto.getFieldName(), fileId);
  }

  private byte[] readBytes(InputStream inputStream) {
    try {
      return inputStream.readAllBytes();
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to read bytes", e);
    }
  }

  private String encodeUtf8(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Unable to encode value", e);
    }
  }

  private String decodeUtf8(String value) {
    try {
      return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Unable to decode value", e);
    }
  }

  private FileMetadataDto buildFileMetadata(String id, String checksum,
      UploadDocumentDto uploadDocumentDto) {
    return FileMetadataDto.builder()
        .filename(encodeUtf8(uploadDocumentDto.getFilename()))
        .contentType(uploadDocumentDto.getContentType())
        .checksum(checksum)
        .id(id)
        .build();
  }
}
