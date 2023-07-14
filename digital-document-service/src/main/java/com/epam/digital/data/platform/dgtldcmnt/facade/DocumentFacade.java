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

package com.epam.digital.data.platform.dgtldcmnt.facade;

import com.epam.digital.data.platform.bpms.client.TaskRestClient;
import com.epam.digital.data.platform.dgtldcmnt.dto.DeleteDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentIdDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentsMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.InternalApiDocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentFromUserFormDto;
import com.epam.digital.data.platform.dgtldcmnt.service.AuthorizationService;
import com.epam.digital.data.platform.dgtldcmnt.service.DocumentService;
import com.epam.digital.data.platform.dgtldcmnt.service.ValidationService;
import com.epam.digital.data.platform.dgtldcmnt.validator.AllowedUploadedDocument;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * The document facade for management of the documents. It contains authorization and validation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Validated
public class DocumentFacade {

  private final DocumentService documentService;
  private final AuthorizationService authorizationService;
  private final ValidationService validationService;
  private final TaskRestClient taskRestClient;

  /**
   * Put document to storage. Before uploading the method does authorization and validation.
   *
   * @param uploadDocumentDto contains file input stream, metadata, and document context info.
   * @param authentication    object with authentication data.
   * @return {@link DocumentMetadataDto} of the saved document.
   */
  public DocumentMetadataDto validateAndPut(
      @AllowedUploadedDocument UploadDocumentFromUserFormDto uploadDocumentDto,
      Authentication authentication) {
    var taskId = uploadDocumentDto.getTaskId();
    var rootProcessInstanceId = uploadDocumentDto.getRootProcessInstanceId();
    var fieldName = uploadDocumentDto.getFieldName();
    log.info("Uploading file {} to storage for task {} in process {}", fieldName, taskId,
        rootProcessInstanceId);

    var task = taskRestClient.getTaskById(taskId);
    var formKey = task.getFormKey();
    uploadDocumentDto.setFormKey(formKey);

    authorizationService.authorize(rootProcessInstanceId, List.of(fieldName), task,
        authentication);
    validationService.validate(uploadDocumentDto);

    var result = documentService.put(uploadDocumentDto);
    log.info("File {} for task {} has been uploaded", fieldName, taskId);
    return result;
  }

  /**
   * Get document from storage by id. Before downloading the method does authorization and
   * validation.
   *
   * @param getDocumentDto contains document id and context of the document.
   * @param authentication object with authentication data.
   * @return document representation.
   */
  public DocumentDto validateAndGet(GetDocumentDto getDocumentDto, Authentication authentication) {
    var taskId = getDocumentDto.getTaskId();
    var rootProcessInstanceId = getDocumentDto.getRootProcessInstanceId();
    var fieldName = getDocumentDto.getFieldName();
    log.info("Downloading file {} for task {} in process {}", fieldName, taskId,
        rootProcessInstanceId);

    authorize(rootProcessInstanceId, taskId, List.of(fieldName), authentication);

    var result = documentService.get(getDocumentDto);
    log.info("File {} for task {} has been downloaded", fieldName, taskId);
    return result;
  }

  /**
   * Get documents metadata by ids. Before retrieving metadata the method does authorization and
   * validation.
   *
   * @param getMetadataDto contains document ids and a context of the documents.
   * @param authentication object with authentication data.
   * @return list of documents metadata.
   */
  public List<DocumentMetadataDto> getMetadata(GetDocumentsMetadataDto getMetadataDto,
      Authentication authentication) {
    var fieldNames = getMetadataDto.getDocuments().stream()
        .map(DocumentIdDto::getFieldName).collect(Collectors.toList());
    var taskId = getMetadataDto.getTaskId();
    var rootProcessInstanceId = getMetadataDto.getRootProcessInstanceId();
    log.info("Getting files metadata {} for task {} in process {}", fieldNames, taskId,
        rootProcessInstanceId);

    authorize(getMetadataDto.getRootProcessInstanceId(), getMetadataDto.getTaskId(), fieldNames,
        authentication);

    var result = documentService.getMetadata(getMetadataDto);
    log.info("Files metadata {} for task {} has been downloaded", fieldNames, taskId);
    return result;
  }

  /**
   * Get document metadata by id.
   *
   * @param rootProcessInstanceId id of a process-instance document has been stored in
   * @param documentId            id of a document to get metadata
   * @return list of documents metadata.
   */
  public InternalApiDocumentMetadataDto getMetadata(String rootProcessInstanceId,
      String documentId) {
    log.info("Getting file {} metadata in process {}", documentId, rootProcessInstanceId);

    var result = documentService.getMetadata(rootProcessInstanceId, documentId);
    log.info("File {} metadata has been downloaded", documentId);
    return result;
  }

  /**
   * Delete all documents associated with provided process instance id
   *
   * @param rootProcessInstanceId specified process instance id
   */
  public void delete(String rootProcessInstanceId) {
    documentService.delete(rootProcessInstanceId);
  }

  /**
   * Delete document associated with provided process instance id and file id
   *
   * @param deleteDocumentDto contains document ids and a context of the documents.
   * @param authentication    object with authentication data.
   */
  public void delete(DeleteDocumentDto deleteDocumentDto, Authentication authentication) {
    var taskId = deleteDocumentDto.getTaskId();
    var rootProcessInstanceId = deleteDocumentDto.getRootProcessInstanceId();
    var fieldName = deleteDocumentDto.getFieldName();

    log.info("Deleting file {} for task {} in process {}", fieldName, taskId,
        rootProcessInstanceId);
    authorize(rootProcessInstanceId, taskId, List.of(fieldName), authentication);
    documentService.delete(rootProcessInstanceId, deleteDocumentDto.getId());
    log.info("File {} for task {} has been deleted", fieldName, taskId);
  }

  /**
   * Put document to storage.
   *
   * @param uploadDocumentDto contains file input stream, metadata, and document context info.
   * @return {@link InternalApiDocumentMetadataDto} of the saved document.
   */
  public InternalApiDocumentMetadataDto put(
      @AllowedUploadedDocument UploadDocumentFromUserFormDto uploadDocumentDto) {
    var rootProcessInstanceId = uploadDocumentDto.getRootProcessInstanceId();
    log.info("Uploading file by rootProcessInstanceId: {}", rootProcessInstanceId);
    var documentMetadata = documentService.put(uploadDocumentDto);
    log.info("File has been uploaded by rootProcessInstanceId: {}", rootProcessInstanceId);
    return InternalApiDocumentMetadataDto.builder()
        .id(documentMetadata.getId())
        .size(documentMetadata.getSize())
        .type(documentMetadata.getType())
        .name(documentMetadata.getName())
        .checksum(documentMetadata.getChecksum())
        .build();
  }

  /**
   * Get document from storage by id.
   *
   * @param getDocumentDto contains document id and context of the document.
   * @return document representation.
   */
  public DocumentDto get(GetDocumentDto getDocumentDto) {
    log.info("Downloading file with rootProcessInstanceId: {}",
        getDocumentDto.getRootProcessInstanceId());
    return documentService.get(getDocumentDto);
  }

  private void authorize(String rootProcessInstanceId, String taskId, List<String> filedNames,
      Authentication authentication) {
    var task = taskRestClient.getTaskById(taskId);

    validationService.checkFieldNamesExistence(filedNames, task.getFormKey());
    authorizationService.authorize(rootProcessInstanceId, filedNames, task, authentication);
  }
}