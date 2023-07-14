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

import com.epam.digital.data.platform.dgtldcmnt.dto.RemoteDocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.validator.RemoteFileSizeValidator;
import com.epam.digital.data.platform.dgtldcmnt.wrapper.Sha256DigestCalculatingInputStream;
import com.epam.digital.data.platform.dgtldcmnt.wrapper.ValidateLengthInputStream;
import com.epam.digital.data.platform.integration.ceph.exception.CephCommunicationException;
import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import com.epam.digital.data.platform.storage.file.dto.BaseFileMetadataDto;
import com.epam.digital.data.platform.storage.file.dto.BaseFileMetadataDto.BaseUserMetadataHeaders;
import com.epam.digital.data.platform.storage.file.dto.FileObjectDto;
import com.epam.digital.data.platform.storage.file.service.FileStorageService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

/**
 * The service for management of the documents based on ceph storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CephInternalApiDocumentService implements InternalApiDocumentService {

  private final FileStorageService storage;
  private final RemoteFileSizeValidator validator;

  public RemoteDocumentMetadataDto put(UploadDocumentDto documentDto) {
    validator.validate(documentDto.getSize());
    
    var fileId = UUID.randomUUID().toString();
    var validateLengthIS = new ValidateLengthInputStream(documentDto.getFileInputStream(), validator);
    var sha256DigestCalculatingIS = new Sha256DigestCalculatingInputStream(validateLengthIS);
    log.info("Downloading file {} from remote URI and uploading it to the storage in process {}",
        documentDto.getFilename(), documentDto.getRootProcessInstanceId());

    var userMetadata = buildUserMetadata(fileId, documentDto.getFilename());
    var fileMetadata = new BaseFileMetadataDto(
        documentDto.getSize(), documentDto.getContentType(), userMetadata);
    var fileObjectDto = FileObjectDto.builder()
        .content(sha256DigestCalculatingIS)
        .metadata(fileMetadata).build();

    BaseFileMetadataDto resultMetadata;
    try {
      resultMetadata = storage.save(documentDto.getRootProcessInstanceId(), fileId, fileObjectDto);
    } catch (CephCommunicationException e) {
      if(e.getCause() instanceof ValidationException) {
        throw (ValidationException)e.getCause();
      }
      throw e;
    }
    String checksum = Hex.encodeHexString(sha256DigestCalculatingIS.getDigest());
    userMetadata.put(BaseUserMetadataHeaders.CHECKSUM, checksum);
    storage.setUserMetadata(documentDto.getRootProcessInstanceId(), fileId, userMetadata);
    log.debug("File {} uploaded. Id {}", documentDto.getFilename(), fileId);
    return toRemoteDocumentMetadataDto(resultMetadata, checksum);
  }

  private RemoteDocumentMetadataDto toRemoteDocumentMetadataDto(BaseFileMetadataDto metadataDto,
      String sha256Digest) {
    return RemoteDocumentMetadataDto.builder()
        .id(metadataDto.getId())
        .name(metadataDto.getFilename())
        .type(metadataDto.getContentType())
        .checksum(sha256Digest)
        .size(metadataDto.getContentLength())
        .build();
  }
  
  private Map<String, String> buildUserMetadata(String fileId, String fileName) {
    Map<String, String> userMetadata = new LinkedHashMap<>();
    userMetadata.put(BaseUserMetadataHeaders.ID, fileId);
    userMetadata.put(BaseUserMetadataHeaders.FILENAME, fileName);
    return userMetadata;
  }
}
