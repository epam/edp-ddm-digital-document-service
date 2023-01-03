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

import com.epam.digital.data.platform.dgtldcmnt.dto.RemoteDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.RemoteDocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadRemoteDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.service.InternalApiDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.IOException;
import java.io.InputStream;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal-api/documents")
public class InternalApiDocumentController {

  private final InternalApiDocumentService internalApiDocumentService;

  public InternalApiDocumentController(InternalApiDocumentService internalApiDocumentService) {
    this.internalApiDocumentService = internalApiDocumentService;
  }

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
      var documentDto = UploadRemoteDocumentDto.builder()
          .contentType(connection.getContentType())
          .contentLength(connection.getContentLength())
          .filename(requestDto.getFilename())
          .inputStream(inputStream)
          .processInstanceId(processInstanceId)
          .build();
      metadataDto = internalApiDocumentService.put(documentDto);
    }
    return metadataDto;
  }
}