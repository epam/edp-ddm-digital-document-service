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

package com.epam.digital.data.platform.dgtldcmnt.controller;

import com.epam.digital.data.platform.dgtldcmnt.config.DigitalDocumentsConfigurationProperties;
import com.epam.digital.data.platform.dgtldcmnt.dto.DeleteDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentMetadataSearchRequestDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentsMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentFromUserFormDto;
import com.epam.digital.data.platform.dgtldcmnt.facade.DocumentFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javax.servlet.annotation.MultipartConfig;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@MultipartConfig
public class DocumentController {

  public static final String X_FORWARDED_HOST_HEADER = "x-forwarded-host";

  private final DigitalDocumentsConfigurationProperties digitalDocumentsProperties;
  private final DocumentFacade documentFacade;

  /**
   * Endpoint for uploading document.
   *
   * @param rootProcessInstanceId specified id of root process instance.
   * @param taskId                specified task id.
   * @param fieldName             specified filed name.
   * @param file                  {@link MultipartFile} representation of a document.
   * @param filename              specified filename(optional).
   * @param authentication        object with authentication data.
   * @return {@link DocumentMetadataDto} with metadata of the stored document.
   */
  @PostMapping("/{rootProcessInstanceId}/{taskId}/{fieldName}")
  @Operation(summary = "Upload document", description = "Returns uploaded document metadata")
  @ApiResponse(
      description = "Returns uploaded document metadata",
      responseCode = "200",
      content = @Content(schema = @Schema(implementation = DocumentMetadataDto.class)))
  public DocumentMetadataDto upload(
      @RequestHeader(X_FORWARDED_HOST_HEADER) String originRequestUrl,
      @PathVariable("rootProcessInstanceId") String rootProcessInstanceId,
      @PathVariable("taskId") String taskId,
      @PathVariable("fieldName") String fieldName,
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false, name = "filename") String filename,
      Authentication authentication) throws IOException {
    var uploadDocumentDto = UploadDocumentFromUserFormDto.builder()
        .filename(Objects.isNull(filename) ? file.getOriginalFilename() : filename)
        .fileInputStream(new BufferedInputStream(file.getInputStream()))
        .contentType(file.getContentType())
        .rootProcessInstanceId(rootProcessInstanceId)
        .originRequestUrl(originRequestUrl)
        .fieldName(fieldName)
        .size(file.getSize())
        .taskId(taskId)
        .build();
    return documentFacade.validateAndPut(uploadDocumentDto, authentication);
  }

  /**
   * Endpoint that handles downloading document by id.
   *
   * @param rootProcessInstanceId specified process instance id.
   * @param taskId                specified task id.
   * @param fieldName             specified filed name.
   * @param id                    specified document id.
   * @param authentication        object with authentication data.
   * @return document as {@link InputStreamResource}.
   */
  @GetMapping("/{rootProcessInstanceId}/{taskId}/{fieldName}/{id}")
  @Operation(summary = "Download document by id", description = "Returns document by id")
  public ResponseEntity<Resource> download(
      @PathVariable("rootProcessInstanceId") String rootProcessInstanceId,
      @PathVariable("taskId") String taskId,
      @PathVariable("fieldName") String fieldName,
      @PathVariable("id") String id,
      Authentication authentication) {
    var getDocumentDto = GetDocumentDto.builder()
        .rootProcessInstanceId(rootProcessInstanceId)
        .fieldName(fieldName)
        .taskId(taskId)
        .id(id)
        .build();
    var documentDto = documentFacade.validateAndGet(getDocumentDto, authentication);
    var contentDisposition = ContentDisposition.builder(
            digitalDocumentsProperties.getContent().getDispositionType())
        .filename(documentDto.getName()).build();
    var headers = new HttpHeaders();
    headers.setContentDisposition(contentDisposition);
    var resource = new InputStreamResource(documentDto.getContent());
    return ResponseEntity.ok()
        .contentType(MediaType.valueOf(documentDto.getContentType()))
        .contentLength(documentDto.getSize())
        .headers(headers)
        .body(resource);
  }

  @PostMapping("/{rootProcessInstanceId}/{taskId}/search")
  @Operation(summary = "Search documents metadata", description = "Returns list of documents metadata")
  public List<DocumentMetadataDto> searchMetadata(
      @RequestHeader(X_FORWARDED_HOST_HEADER) String originRequestUrl,
      @PathVariable("rootProcessInstanceId") String rootProcessInstanceId,
      @PathVariable("taskId") String taskId,
      @Valid @RequestBody DocumentMetadataSearchRequestDto requestDto,
      Authentication authentication) {
    var getDocumentsMetadataDto = GetDocumentsMetadataDto.builder()
        .documents(requestDto.getDocuments())
        .rootProcessInstanceId(rootProcessInstanceId)
        .originRequestUrl(originRequestUrl)
        .taskId(taskId)
        .build();
    return documentFacade.getMetadata(getDocumentsMetadataDto, authentication);
  }

  /**
   * Endpoint that deletes all documents associated with specified process instance id. The endpoint
   * should be allowed only in internal network for system needs only as cleaning temporary data.
   *
   * @param rootProcessInstanceId specified process instance id
   */
  @DeleteMapping("/{rootProcessInstanceId}")
  public void delete(@PathVariable("rootProcessInstanceId") String rootProcessInstanceId) {
    documentFacade.delete(rootProcessInstanceId);
  }

  /**
   * Endpoint that deletes document associated with specified process instance id and file id.
   *
   * @param rootProcessInstanceId specified process instance id
   * @param fileId                specified file id
   */
  @DeleteMapping("/{rootProcessInstanceId}/{taskId}/{fieldName}/{fileId}")
  @Operation(summary = "Delete document by id")
  public void deleteByFileId(@PathVariable("rootProcessInstanceId") String rootProcessInstanceId,
      @PathVariable("taskId") String taskId,
      @PathVariable("fieldName") String fieldName,
      @PathVariable("fileId") String fileId,
      Authentication authentication) {
    var deleteDocumentDto = DeleteDocumentDto.builder()
        .rootProcessInstanceId(rootProcessInstanceId)
        .fieldName(fieldName)
        .taskId(taskId)
        .id(fileId)
        .build();
    documentFacade.delete(deleteDocumentDto, authentication);
  }
}