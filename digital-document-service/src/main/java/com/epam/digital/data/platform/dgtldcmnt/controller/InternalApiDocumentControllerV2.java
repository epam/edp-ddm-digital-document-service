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

import com.epam.digital.data.platform.dgtldcmnt.dto.InternalApiDocumentMetadataDto;
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
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal-api/v2/documents")
@Tag(description = "Digital document service internal Rest API", name = "digital-document-service-internal-api-v2")
public class InternalApiDocumentControllerV2 {

  private final DocumentFacade documentFacade;

  @PostMapping("/{rootProcessInstanceId}")
  @Operation(summary = "Upload MultiPart document",
      description = "### Endpoint purpose:\n This endpoint allows to upload a document as part of a specified process instance. It accepts a multi-part file and an optional file name. The uploaded document's metadata is returned upon successful storage.\n"
          + "### Validation:\n The file size should not exceed the system limit; otherwise, a _413 Payload Too Large_ status code is returned. For batch file uploads, the total file size should not exceed the expected limit. Media type validation accepts the following formats: PDF, PNG, JPG/JPEG, CSV, ASICs, P7S. If a different format is used, a _422 Unprocessable Entity_ status code is returned.",
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
              content = @Content(schema = @Schema(implementation = InternalApiDocumentMetadataDto.class),
                  examples = {
                      @ExampleObject(value = "{\n"
                          + "  \"id\": \"my-file-id\",\n"
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
              responseCode = "415",
              description = "Unsupported Media Type",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              description = "Unprocessable Entity",
              responseCode = "422",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          )
      })
  public InternalApiDocumentMetadataDto upload(
      @PathVariable("rootProcessInstanceId") String rootProcessInstanceId,
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false, value = "filename") String filename) throws IOException {
    var documentDto = UploadDocumentFromUserFormDto.builder()
        .contentType(file.getContentType())
        .size(file.getSize())
        .filename(Objects.isNull(filename) ? file.getOriginalFilename() : filename)
        .fileInputStream(new BufferedInputStream(file.getInputStream()))
        .rootProcessInstanceId(rootProcessInstanceId)
        .build();
    return documentFacade.put(documentDto);
  }
}
