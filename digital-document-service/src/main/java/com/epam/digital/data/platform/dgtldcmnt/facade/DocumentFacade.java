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
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.service.AuthorizationService;
import com.epam.digital.data.platform.dgtldcmnt.service.DocumentService;
import com.epam.digital.data.platform.dgtldcmnt.service.ValidationService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * The document facade for management of the documents. It contains authorization and validation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
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
  public DocumentMetadataDto put(UploadDocumentDto uploadDocumentDto,
      Authentication authentication) {
    var taskId = uploadDocumentDto.getTaskId();
    var processInstanceId = uploadDocumentDto.getProcessInstanceId();
    var fieldName = uploadDocumentDto.getFieldName();
    log.info("Uploading file {} to storage for task {} in process {}", fieldName, taskId,
        processInstanceId);

    var task = taskRestClient.getTaskById(taskId);
    var formKey = task.getFormKey();
    uploadDocumentDto.setFormKey(formKey);

    authorizationService.authorize(processInstanceId, List.of(fieldName), task,
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
  public DocumentDto get(GetDocumentDto getDocumentDto, Authentication authentication) {
    var taskId = getDocumentDto.getTaskId();
    var processInstanceId = getDocumentDto.getProcessInstanceId();
    var fieldName = getDocumentDto.getFieldName();
    log.info("Downloading file {} for task {} in process {}", fieldName, taskId, processInstanceId);

    authorize(processInstanceId, taskId, List.of(fieldName), authentication);

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
    var processInstanceId = getMetadataDto.getProcessInstanceId();
    log.info("Getting files metadata {} for task {} in process {}", fieldNames, taskId,
        processInstanceId);

    authorize(getMetadataDto.getProcessInstanceId(), getMetadataDto.getTaskId(), fieldNames,
        authentication);

    var result = documentService.getMetadata(getMetadataDto);
    log.info("Files metadata {} for task {} has been downloaded", fieldNames, taskId);
    return result;
  }

  /**
   * Delete all documents associated with provided process instance id
   *
   * @param processInstanceId specified process instance id
   */
  public void delete(String processInstanceId) {
    documentService.delete(processInstanceId);
  }

  /**
   * Delete document associated with provided process instance id and file id
   *
   * @param deleteDocumentDto contains document ids and a context of the documents.
   * @param authentication object with authentication data.
   */
  public void delete(DeleteDocumentDto deleteDocumentDto, Authentication authentication) {
    var taskId = deleteDocumentDto.getTaskId();
    var processInstanceId = deleteDocumentDto.getProcessInstanceId();
    var fieldName = deleteDocumentDto.getFieldName();

    log.info("Deleting file {} for task {} in process {}", fieldName, taskId, processInstanceId);
    authorize(processInstanceId, taskId, List.of(fieldName), authentication);
    documentService.delete(processInstanceId, deleteDocumentDto.getId());
    log.info("File {} for task {} has been deleted", fieldName, taskId);
  }

  private void authorize(String processInstance, String taskId, List<String> filedNames,
      Authentication authentication) {
    var task = taskRestClient.getTaskById(taskId);

    validationService.checkFieldNamesExistence(filedNames, task.getFormKey());
    authorizationService.authorize(processInstance, filedNames, task, authentication);
  }
}