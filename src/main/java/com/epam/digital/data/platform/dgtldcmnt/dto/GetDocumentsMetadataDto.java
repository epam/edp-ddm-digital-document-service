/*
 * Copyright 2021 EPAM Systems.
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

package com.epam.digital.data.platform.dgtldcmnt.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * It contains required parameters for getting documents metadata.
 */
@Data
@Builder
public class GetDocumentsMetadataDto {

  private List<DocumentIdDto> documents;

  /**
   * The task id that has ui form and filedNames under which documents are loaded.
   */
  private String taskId;

  /**
   * The process instance id to whom documents belongs to.
   */
  private String processInstanceId;

  /**
   * The origin forwarded request url.
   */
  private String originRequestUrl;

}
