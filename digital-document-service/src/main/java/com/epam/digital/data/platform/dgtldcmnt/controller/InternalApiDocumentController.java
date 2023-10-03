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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(description = "Digital document service internal Rest API", name = "digital-document-service-internal-api")
public class InternalApiDocumentController {

  private final DigitalDocumentsConfigurationProperties digitalDocumentsProperties;
  private final InternalApiDocumentService internalApiDocumentService;
  private final DocumentFacade documentFacade;

  /**
   * Endpoint for uploading document.
   *
   * @param rootProcessInstanceId specified process instance id.
   * @return {@link RemoteDocumentMetadataDto} with metadata of the stored document.
   */
  @PostMapping("/{rootProcessInstanceId}")
  @Operation(summary = "Upload document",
      description = "### Endpoint purpose:\n This endpoint downloads document from remote URL passed in request body and using root process instance ID to save document. It returns the uploaded document's metadata.\n"
          + "### Validation:\n The file size should not exceed the system limit; otherwise, a _413 Payload Too Large_ status code is returned. Media type validation accepts the following formats: PDF, PNG, JPG/JPEG, CSV, ASICs, P7S. If a different format is used, a _422 Unprocessable Entity_ status code is returned.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RemoteDocumentDto.class),
              examples = {
                  @ExampleObject(value = "{\n"
                      + "  \"remoteFileLocation\": \"https://somefilelocation.com\",\n"
                      + "  \"filename\": \"my-file-name.png\",\n"
                      + "}")
              }
          )
      ),
      responses = {
          @ApiResponse(
              description = "Returns uploaded document metadata",
              responseCode = "200",
              content = @Content(schema = @Schema(implementation = RemoteDocumentMetadataDto.class),
                  examples = {
                      @ExampleObject(value = "{\n"
                          + "  \"id\": \"my-file-id\",\n"
                          + "  \"name\": \"my-file-name.png\",\n"
                          + "  \"type\": \"image/png\",\n"
                          + "  \"checksum\": \"039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81\",\n"
                          + "  \"size\": 3,\n"
                          + "}")
                  })),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "415",
              description = "Unsupported Media Type",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              description = "Unprocessable Entity. Can happen when remote file size more than allowed.",
              responseCode = "422",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          )
      }
  )
  public RemoteDocumentMetadataDto upload(
      @PathVariable("rootProcessInstanceId") String rootProcessInstanceId,
      @Valid @RequestBody RemoteDocumentDto requestDto) throws IOException {
    RemoteDocumentMetadataDto metadataDto;
    var connection = requestDto.getRemoteFileLocation().openConnection();
    try (InputStream inputStream = connection.getInputStream()) {
      var documentDto = UploadDocumentDto.builder()
          .contentType(connection.getContentType())
          .size(connection.getContentLength())
          .filename(requestDto.getFilename())
          .fileInputStream(new BufferedInputStream(inputStream))
          .rootProcessInstanceId(rootProcessInstanceId)
          .build();
      metadataDto = internalApiDocumentService.put(documentDto);
    }
    return metadataDto;
  }

  @GetMapping("/{rootProcessInstanceId}/{id}")
  @Operation(summary = "Download document by id",
      description = "### Endpoint purpose:\n This endpoint allows to download a document associated with a specified process instance and document ID. The document is returned as a downloadable resource.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              description = "Returns uploaded document metadata",
              responseCode = "200",
              content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Not Found",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          )
      }
  )
  public ResponseEntity<Resource> download(
      @PathVariable("rootProcessInstanceId") String rootProcessInstanceId,
      @PathVariable("id") String id) {
    var getDocumentDto = GetDocumentDto.builder()
        .rootProcessInstanceId(rootProcessInstanceId)
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

  @GetMapping("/{rootProcessInstanceId}/{id}/metadata")
  @Operation(summary = "Get document metadata by id",
      description = "### Endpoint purpose\n This endpoint allows users to retrieve document metadata based on a specific document ID associated with a given root process instance. Document metadata includes information such as the document's name, content type, size, and other relevant details.",
      responses = {
          @ApiResponse(
              description = "Returns uploaded document metadata",
              responseCode = "200",
              content = @Content(schema = @Schema(implementation = InternalApiDocumentMetadataDto.class),
                  examples = {
                      @ExampleObject(value = "{\n"
                          + "  \"id\": \"my-file-id\",\n"
                          + "  \"name\": \"my-file-name.png\",\n"
                          + "  \"type\": \"image/png\",\n"
                          + "  \"checksum\": \"039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81\",\n"
                          + "  \"size\": 3,\n"
                          + "}")
                  })),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          )
      })
  public InternalApiDocumentMetadataDto getMetadata(
      @PathVariable("rootProcessInstanceId") String rootProcessInstanceId,
      @PathVariable("id") String id) {
    return documentFacade.getMetadata(rootProcessInstanceId, id);
  }
}