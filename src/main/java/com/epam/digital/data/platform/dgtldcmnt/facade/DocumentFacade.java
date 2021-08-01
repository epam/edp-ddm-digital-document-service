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
import org.springframework.stereotype.Component;

/**
 * The document facade for management of the documents. It contains authorization and validation.
 */
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
    var task = taskRestClient.getTaskById(uploadDocumentDto.getTaskId());
    var form = formProviderClient.getForm(task.getFormKey());

    authorizationService.authorize(uploadDocumentDto.getProcessInstanceId(),
        List.of(uploadDocumentDto.getFieldName()), task, form);
    validationService.validate(uploadDocumentDto, form);

    return documentService.put(uploadDocumentDto);
  }

  /**
   * Get document from storage by id. Before downloading the method does authorization and
   * validation.
   *
   * @param getDocumentDto contains document id and context of the document.
   * @return document representation.
   */
  public DocumentDto get(GetDocumentDto getDocumentDto) {
    authorize(getDocumentDto.getProcessInstanceId(), getDocumentDto.getTaskId(),
        List.of(getDocumentDto.getFieldName()));

    return documentService.get(getDocumentDto);
  }

  /**
   * Get documents metadata by ids. Before retrieving metadata the method does authorization and
   * validation.
   *
   * @param getMetadataDto contains document ids and a context of the documents.
   * @return list of documents metadata.
   */
  public List<DocumentMetadataDto> getMetadata(GetDocumentsMetadataDto getMetadataDto) {
    var filedNames = getMetadataDto.getDocuments().stream()
        .map(DocumentIdDto::getFieldName).collect(Collectors.toList());
    authorize(getMetadataDto.getProcessInstanceId(), getMetadataDto.getTaskId(), filedNames);

    return documentService.getMetadata(getMetadataDto);
  }

  private void authorize(String processInstance, String taskId, List<String> filedNames) {
    var task = taskRestClient.getTaskById(taskId);
    var form = formProviderClient.getForm(task.getFormKey());

    authorizationService.authorize(processInstance, filedNames, task, form);
  }
}
