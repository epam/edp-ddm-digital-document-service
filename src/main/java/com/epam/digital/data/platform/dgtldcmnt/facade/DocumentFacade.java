package com.epam.digital.data.platform.dgtldcmnt.facade;

import com.epam.digital.data.platform.bpms.client.CamundaTaskRestClient;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentIdDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentsMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.service.AuthorizationService;
import com.epam.digital.data.platform.dgtldcmnt.service.DocumentService;
import com.epam.digital.data.platform.dgtldcmnt.service.ValidationService;
import com.epam.digital.data.platform.starter.validation.client.FormManagementProviderClient;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final CamundaTaskRestClient taskRestClient;
  private final FormManagementProviderClient formProviderClient;

  /**
   * Put document to storage. Before uploading the method does authorization and validation.
   *
   * @param uploadDocumentDto contains file input stream, metadata, and document context info.
   * @return {@link DocumentMetadataDto} of the saved document.
   */
  public DocumentMetadataDto put(UploadDocumentDto uploadDocumentDto) {
    var taskId = uploadDocumentDto.getTaskId();
    var processInstanceId = uploadDocumentDto.getProcessInstanceId();
    var fieldName = uploadDocumentDto.getFieldName();
    log.info("Uploading file {} to storage for task {} in process {}", fieldName, taskId,
        processInstanceId);

    var task = taskRestClient.getTaskById(taskId);
    var form = formProviderClient.getForm(task.getFormKey());

    authorizationService.authorize(processInstanceId, List.of(fieldName), task, form);
    validationService.validate(uploadDocumentDto, form);

    var result = documentService.put(uploadDocumentDto);
    log.info("File {} for task {} has been uploaded", fieldName, taskId);
    return result;
  }

  /**
   * Get document from storage by id. Before downloading the method does authorization and
   * validation.
   *
   * @param getDocumentDto contains document id and context of the document.
   * @return document representation.
   */
  public DocumentDto get(GetDocumentDto getDocumentDto) {
    var taskId = getDocumentDto.getTaskId();
    var processInstanceId = getDocumentDto.getProcessInstanceId();
    var fieldName = getDocumentDto.getFieldName();
    log.info("Downloading file {} for task {} in process {}", fieldName, taskId, processInstanceId);

    authorize(processInstanceId, taskId, List.of(fieldName));

    var result = documentService.get(getDocumentDto);
    log.info("File {} for task {} has been downloaded", fieldName, taskId);
    return result;
  }

  /**
   * Get documents metadata by ids. Before retrieving metadata the method does authorization and
   * validation.
   *
   * @param getMetadataDto contains document ids and a context of the documents.
   * @return list of documents metadata.
   */
  public List<DocumentMetadataDto> getMetadata(GetDocumentsMetadataDto getMetadataDto) {
    var fieldNames = getMetadataDto.getDocuments().stream()
        .map(DocumentIdDto::getFieldName).collect(Collectors.toList());
    var taskId = getMetadataDto.getTaskId();
    var processInstanceId = getMetadataDto.getProcessInstanceId();
    log.info("Getting files metadata {} for task {} in process {}", fieldNames, taskId,
        processInstanceId);

    authorize(getMetadataDto.getProcessInstanceId(), getMetadataDto.getTaskId(), fieldNames);

    var result = documentService.getMetadata(getMetadataDto);
    log.info("Files metadata {} for task {} has been downloaded", fieldNames, taskId);
    return result;
  }

  private void authorize(String processInstance, String taskId, List<String> filedNames) {
    var task = taskRestClient.getTaskById(taskId);
    var form = formProviderClient.getForm(task.getFormKey());

    authorizationService.authorize(processInstance, filedNames, task, form);
  }
}
