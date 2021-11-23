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

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentIdDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentsMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.exception.DocumentNotFoundException;
import com.epam.digital.data.platform.dgtldcmnt.util.CephKeyProvider;
import com.epam.digital.data.platform.integration.ceph.UserMetadataHeaders;
import com.epam.digital.data.platform.integration.ceph.service.S3ObjectCephService;
import com.epam.digital.data.platform.starter.logger.annotation.Confidential;
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

  private final S3ObjectCephService s3ObjectCephService;
  private final CephKeyProvider keyProvider;

  @Override
  public DocumentMetadataDto put(UploadDocumentDto uploadDocumentDto) {
    var id = UUID.randomUUID().toString();
    log.debug("Uploading file {}, id {}, processInstanceId {}, taskId {}",
        uploadDocumentDto.getFilename(), id, uploadDocumentDto.getProcessInstanceId(),
        uploadDocumentDto.getTaskId());
    byte[] data = readBytes(uploadDocumentDto.getFileInputStream());
    var sha256hex = DigestUtils.sha256Hex(data);
    var userMetadata = Map.of(
        UserMetadataHeaders.ID, id,
        UserMetadataHeaders.CHECKSUM, sha256hex,
        UserMetadataHeaders.FILENAME, encodeUtf8(uploadDocumentDto.getFilename())
    );
    var key = keyProvider.generateKey(id, uploadDocumentDto.getProcessInstanceId());
    var objectMetadata = s3ObjectCephService
        .put(key, uploadDocumentDto.getContentType(), userMetadata, new ByteArrayInputStream(data));
    var url = generateGetDocumentUrl(uploadDocumentDto.getOriginRequestUrl(),
        uploadDocumentDto.getProcessInstanceId(), uploadDocumentDto.getTaskId(),
        uploadDocumentDto.getFieldName(), id);
    log.debug("File {} uploaded. Id {}", uploadDocumentDto.getFilename(), id);
    return DocumentMetadataDto.builder()
        .size(objectMetadata.getContentLength())
        .name(uploadDocumentDto.getFilename())
        .type(objectMetadata.getContentType())
        .checksum(sha256hex)
        .url(url)
        .id(id)
        .build();
  }

  @Override
  @Confidential
  public DocumentDto get(GetDocumentDto getDocumentDto) {
    log.debug("Getting document with id {}", getDocumentDto.getId());
    var key = keyProvider
        .generateKey(getDocumentDto.getId(), getDocumentDto.getProcessInstanceId());
    var s3Object = s3ObjectCephService.get(key)
        .orElseThrow(() -> new DocumentNotFoundException(List.of(getDocumentDto.getId())));
    log.debug("File downloaded. Id {}", getDocumentDto.getId());
    return DocumentDto.builder()
        .name(decodeUtf8(
            s3Object.getObjectMetadata().getUserMetaDataOf(UserMetadataHeaders.FILENAME)))
        .contentType(s3Object.getObjectMetadata().getContentType())
        .size(s3Object.getObjectMetadata().getContentLength())
        .content(s3Object.getObjectContent())
        .build();
  }

  @Override
  public List<DocumentMetadataDto> getMetadata(GetDocumentsMetadataDto getMetadataDto) {
    log.debug("Getting documents metadata by ids {}", getMetadataDto.getDocuments());
    var documentIdAndFiledNameMap = getMetadataDto.getDocuments().stream()
        .collect(Collectors.toMap(DocumentIdDto::getId, DocumentIdDto::getFieldName));
    var ids = documentIdAndFiledNameMap.keySet().stream()
        .map(id -> keyProvider.generateKey(id, getMetadataDto.getProcessInstanceId()))
        .collect(Collectors.toList());
    var result = s3ObjectCephService.getMetadata(ids)
        .map(metadataList -> metadataList.stream()
            .map(objectMetadata -> map(objectMetadata, getMetadataDto, documentIdAndFiledNameMap))
            .collect(Collectors.toList())
        ).orElseThrow(() -> new DocumentNotFoundException(documentIdAndFiledNameMap.keySet()));
    log.debug("Documents metadata by ids {} received", getMetadataDto.getDocuments());
    return result;
  }

  private DocumentMetadataDto map(ObjectMetadata objectMetadata,
      GetDocumentsMetadataDto getMetadataDto, Map<String, String> documentIdAndFiledNameMap) {
    var id = objectMetadata.getUserMetaDataOf(UserMetadataHeaders.ID);
    var url = generateGetDocumentUrl(getMetadataDto.getOriginRequestUrl(),
        getMetadataDto.getProcessInstanceId(), getMetadataDto.getTaskId(),
        documentIdAndFiledNameMap.get(id),
        objectMetadata.getUserMetaDataOf(UserMetadataHeaders.ID));
    return DocumentMetadataDto.builder()
        .url(url)
        .name(decodeUtf8(objectMetadata.getUserMetaDataOf(UserMetadataHeaders.FILENAME)))
        .checksum(objectMetadata.getUserMetaDataOf(UserMetadataHeaders.CHECKSUM))
        .id(objectMetadata.getUserMetaDataOf(UserMetadataHeaders.ID))
        .size(objectMetadata.getContentLength())
        .type(objectMetadata.getContentType())
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
}
