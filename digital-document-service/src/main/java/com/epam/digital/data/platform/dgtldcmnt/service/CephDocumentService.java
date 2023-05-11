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

import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentIdDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentsMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.InternalApiDocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentFromUserFormDto;
import com.epam.digital.data.platform.dgtldcmnt.mapper.DocumentMetadataDtoMapper;
import com.epam.digital.data.platform.storage.file.dto.FileDataDto;
import com.epam.digital.data.platform.storage.file.dto.FileMetadataDto;
import com.epam.digital.data.platform.storage.file.service.FormDataFileStorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The service for management of the documents based on ceph storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CephDocumentService implements DocumentService {

  private final FormDataFileStorageService storage;
  private final DocumentMetadataDtoMapper mapper;

  @Override
  public DocumentMetadataDto put(UploadDocumentFromUserFormDto uploadDocumentDto) {
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
  public InternalApiDocumentMetadataDto getMetadata(String processInstanceId, String documentId) {
    log.debug("Getting document metadata by id {}", documentId);
    var result = storage.getMetadata(processInstanceId, Set.of(documentId))
        .stream()
        .map(mapper::toInternalApiDocumentMetadataDto)
        .collect(Collectors.toList());
    log.debug("Document metadata by id {} received", documentId);
    return CollectionUtils.firstElement(result);
  }

  @Override
  public void delete(String processInstanceId) {
    log.debug("Deleting all documents associated with process instance id {}", processInstanceId);
    storage.deleteByProcessInstanceId(processInstanceId);
    log.debug("All documents associated with process instance id {} were deleted successfully",
        processInstanceId);
  }

  @Override
  public void delete(String processInstanceId, String fileId) {
    log.debug("Deleting document associated with process instance id {} and id {}",
        processInstanceId, fileId);
    storage.deleteByProcessInstanceIdAndId(processInstanceId, fileId);
    log.debug("Document associated with process instance id {} and id {} was deleted successfully",
        processInstanceId, fileId);
  }

  private DocumentMetadataDto map(FileMetadataDto fileMetadataDto,
      GetDocumentsMetadataDto getMetadataDto, Map<String, String> documentIdAndFiledNameMap) {
    var id = fileMetadataDto.getId();
    var url = generateGetDocumentUrl(getMetadataDto.getOriginRequestUrl(),
        getMetadataDto.getProcessInstanceId(), getMetadataDto.getTaskId(),
        documentIdAndFiledNameMap.get(id), id);
    return mapper.toDocumentMetadataDto(fileMetadataDto, url);
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

  private String generateGetDocumentUrl(String fileId,
      UploadDocumentFromUserFormDto uploadDocumentDto) {
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
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String decodeUtf8(String value) {
    return Optional.ofNullable(value)
        .map(v -> URLDecoder.decode(v, StandardCharsets.UTF_8))
        .orElse(null);
  }

  private FileMetadataDto buildFileMetadata(String id, String checksum,
      UploadDocumentFromUserFormDto uploadDocumentDto) {
    return FileMetadataDto.builder()
        .filename(encodeUtf8(uploadDocumentDto.getFilename()))
        .contentType(uploadDocumentDto.getContentType())
        .contentLength(uploadDocumentDto.getSize())
        .fieldName(uploadDocumentDto.getFieldName())
        .formKey(uploadDocumentDto.getFormKey())
        .checksum(checksum)
        .id(id)
        .build();
  }
}
