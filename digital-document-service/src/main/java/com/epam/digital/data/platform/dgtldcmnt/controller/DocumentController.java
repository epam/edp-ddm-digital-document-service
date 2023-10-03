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
import com.epam.digital.data.platform.dgtldcmnt.dto.RemoteDocumentDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.UploadDocumentFromUserFormDto;
import com.epam.digital.data.platform.dgtldcmnt.facade.DocumentFacade;
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
@Tag(description = "Digital document service Rest API", name = "digital-document-service-api")
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
  @Operation(summary = "Upload document in business process",
      description = "### Endpoint purpose:\n This endpoint allows to upload a document as part of a specified process instance and task. It accepts a multi-part file and associated parameters, such as the task ID, form field name, and an optional file name. The uploaded document's metadata is returned upon successful storage.\n"
          + "### Authorization:\n This endpoint requires valid user authentication. To access this endpoint, the request must include a valid access token in the _X-Access-Token_ header, otherwise, the API will return a _401 Unauthorized_ status code. Also if _rootProcessInstanceId_ not in task, which retrieved by _taskId_, or task is suspended, or assignee of task is not the same as provided in _X-Access-Token_ then _403_ status code returned.\n"
          + "### Validation:\n This endpoint requires a valid _fieldName_. If the provided field name is not found in the form related to the user task retrieved by _taskId_, a _422_ status code is returned. The file size should not exceed the system limit; otherwise, a _413 Payload Too Large_ status code is returned. For batch file uploads, the total file size should not exceed the expected limit. Media type validation accepts the following formats: PDF, PNG, JPG/JPEG, CSV, ASICs, P7S. If a different format is used, a _422 Unprocessable Entity_ status code is returned.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)),
      responses = {
          @ApiResponse(
              description = "Document uploaded, returns uploaded document metadata",
              responseCode = "200",
              content = @Content(schema = @Schema(implementation = DocumentMetadataDto.class),
                  examples = {
                      @ExampleObject(value = "{\n"
                          + "  \"id\": \"my-file-id\",\n"
                          + "  \"url\": \"https://my-file-url\",\n"
                          + "  \"name\": \"my-file-name.pdf\",\n"
                          + "  \"type\": \"application/pdf\",\n"
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
              description = "Forbidden. Validation of rootProcessInstanceId or taskId not passed.",
              responseCode = "403",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              description = "Payload Too Large. Uploaded document size more than allowed.",
              responseCode = "413",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "415",
              description = "Unsupported Media Type",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          )
      })
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
  @Operation(summary = "Download document",
      description = "### Endpoint purpose:\n This endpoint allows users to download a document associated with a specified process instance, task, field, and document ID. The document is returned as a downloadable resource.\n"
          + "### Authorization:\n This endpoint requires valid user authentication. To access this endpoint, the request must include a valid access token in the _X-Access-Token_ header, otherwise, the API will return a _401 Unauthorized_ status code. Also if _rootProcessInstanceId_ not in task, which retrieved by _taskId_, or task is suspended, or assignee of task is not the same as provided in _X-Access-Token_ then _403_ status code returned. This endpoint requires a valid _fieldName_. If the provided field name is not found in the form related to the user task retrieved by _taskId_, a _403_ status code is returned.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              description = "Document is returned",
              responseCode = "200",
              content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
          ,
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              description = "Forbidden. Validation of rootProcessInstanceId or taskId not passed.",
              responseCode = "403",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              description = "Document not found",
              responseCode = "404",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          )
      })
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
  @Operation(summary = "Search documents metadata",
      description = "### Endpoint purpose:\n This endpoint allows to search for document metadata associated with a specified process instance and task. Document IDs and field names are provided in the request body, and a list of matching document metadata is returned. Server returns every metadata that found and missing files are ignored.\n"
          + "### Authorization:\n This endpoint requires valid user authentication. To access this endpoint, the request must include a valid access token in the _X-Access-Token_ header, otherwise, the API will return a _401 Unauthorized_ status code. Also if _rootProcessInstanceId_ not in task, which retrieved by _taskId_, or task is suspended, or assignee of task is not the same as provided in _X-Access-Token_ then _403_ status code returned. This endpoint requires a valid _fieldName_. If the provided field name is not found in the form related to the user task retrieved by _taskId_, a _403_ status code is returned.",
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
                  @ExampleObject(value = "[{\n"
                      + "  \"id\": \"file-id-1\",\n"
                      + "  \"fieldName\": \"form-field-name-1\"\n"
                      + "},"
                      + "{\n"
                      + "  \"id\": \"file-id-2\",\n"
                      + "  \"fieldName\": \"form-field-name-2\"\n"
                      + "}]")
              }
          )
      ),
      responses = {
          @ApiResponse(
              description = "Returns list of document metadata",
              responseCode = "200",
              content = @Content(schema = @Schema(implementation = DocumentMetadataDto.class),
                  examples = {
                      @ExampleObject(value = "[{\n"
                          + "  \"id\": \"file-id-1\",\n"
                          + "  \"url\": \"https://my-file-url\",\n"
                          + "  \"name\": \"my-file-name.pdf\",\n"
                          + "  \"type\": \"application/pdf\",\n"
                          + "  \"checksum\": \"039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81\",\n"
                          + "  \"size\": 3\n"
                          + "},"
                          + "{\n"
                          + "  \"id\": \"file-id-2\",\n"
                          + "  \"url\": \"https://my-file-url2\",\n"
                          + "  \"name\": \"my-file-name2.pdf\",\n"
                          + "  \"type\": \"application/pdf\",\n"
                          + "  \"checksum\": \"039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81\",\n"
                          + "  \"size\": 5\n"
                          + "}]")
                  })),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              description = "Forbidden. Validation of rootProcessInstanceId or taskId not passed.",
              responseCode = "403",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          )
      }
  )
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
  @Operation(summary = "Delete all documents by process instance ID",
      description = "### Endpoint purpose:\n This endpoint is intended for internal system use only and should be restricted to the internal network. It allows the deletion of all documents associated with the specified business process, typically for cleaning temporary data.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              description = "Documents deleted successfully.",
              responseCode = "200"
              ),
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
      }
  )
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
  @Operation(
      summary = "Delete document by id",
      description = "### Endpoint purpose:\n This endpoint allows the deletion of a specific document associated with the specified process instance ID, task ID, field name, and file ID.\n"
          + "### Authorization:\n This endpoint requires valid user authentication. To access this endpoint, the request must include a valid access token in the _X-Access-Token_ header, otherwise, the API will return a _401 Unauthorized_ status code. Also if _rootProcessInstanceId_ not in task, which retrieved by _taskId_, or task is suspended, or assignee of task is not the same as provided in _X-Access-Token_ then _403_ status code returned. This endpoint requires a valid _fieldName_. If the provided field name is not found in the form related to the user task retrieved by _taskId_, a _403_ status code is returned.",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      responses = {
          @ApiResponse(
              description = "Document deleted successfully",
              responseCode = "200"
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              description = "Forbidden. Validation of rootProcessInstanceId or taskId not passed.",
              responseCode = "403",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          )
      }
  )
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