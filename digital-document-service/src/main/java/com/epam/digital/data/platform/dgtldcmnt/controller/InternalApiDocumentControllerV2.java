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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal-api/v2/documents")
public class InternalApiDocumentControllerV2 {

  private final DocumentFacade documentFacade;

  @PostMapping("/{rootProcessInstanceId}")
  @Operation(summary = "Upload MultiPart document", description = "Returns uploaded document metadata")
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
