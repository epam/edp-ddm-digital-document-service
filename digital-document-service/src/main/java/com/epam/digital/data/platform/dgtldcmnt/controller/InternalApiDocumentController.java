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
import com.epam.digital.data.platform.dgtldcmnt.dto.GetDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.InternalApiDocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.RemoteDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.RemoteDocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.facade.DocumentFacade;
import com.epam.digital.data.platform.dgtldcmnt.service.InternalApiDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal-api/documents")
public class InternalApiDocumentController {

  private final DigitalDocumentsConfigurationProperties digitalDocumentsProperties;
  private final InternalApiDocumentService internalApiDocumentService;
  private final DocumentFacade documentFacade;

  /**
   * Endpoint for uploading document.
   *
   * @param processInstanceId specified process instance id.
   * @return {@link RemoteDocumentMetadataDto} with metadata of the stored document.
   */
  @PostMapping("/{processInstanceId}")
  @Operation(summary = "Upload document", description = "Returns uploaded document metadata")
  @ApiResponse(
      description = "Returns uploaded document metadata",
      responseCode = "201",
      content = @Content(schema = @Schema(implementation = RemoteDocumentMetadataDto.class)))
  public RemoteDocumentMetadataDto upload(
      @PathVariable("processInstanceId") String processInstanceId,
      @Valid @RequestBody RemoteDocumentDto requestDto) throws IOException {
    RemoteDocumentMetadataDto metadataDto;
    var connection = requestDto.getRemoteFileLocation().openConnection();
    try (InputStream inputStream = connection.getInputStream()) {
      var documentDto = UploadDocumentDto.builder()
          .contentType(connection.getContentType())
          .size(connection.getContentLength())
          .filename(requestDto.getFilename())
          .fileInputStream(new BufferedInputStream(inputStream))
          .processInstanceId(processInstanceId)
          .build();
      metadataDto = internalApiDocumentService.put(documentDto);
    }
    return metadataDto;
  }

  @GetMapping("/{processInstanceId}/{id}")
  @Operation(summary = "Download document by id", description = "Returns document by id")
  public ResponseEntity<Resource> download(
      @PathVariable("processInstanceId") String processInstanceId,
      @PathVariable("id") String id) {
    var getDocumentDto = GetDocumentDto.builder()
        .processInstanceId(processInstanceId)
        .id(id)
        .build();
    var documentDto = documentFacade.get(getDocumentDto);
    var resource = new InputStreamResource(documentDto.getContent());
    var contentDisposition = ContentDisposition.builder(
            digitalDocumentsProperties.getContent().getDispositionType())
        .filename(documentDto.getName()).build();
    var headers = new HttpHeaders();
    headers.setContentDisposition(contentDisposition);
    return ResponseEntity.ok()
        .contentType(MediaType.valueOf(documentDto.getContentType()))
        .contentLength(documentDto.getSize())
        .headers(headers)
        .body(resource);
  }

  @GetMapping("/{processInstanceId}/{id}/metadata")
  @Operation(summary = "Get document metadata by id", description = "Returns document metadata by document id")
  public InternalApiDocumentMetadataDto getMetadata(
      @PathVariable("processInstanceId") String processInstanceId,
      @PathVariable("id") String id) {
    return documentFacade.getMetadata(processInstanceId, id);
  }
}