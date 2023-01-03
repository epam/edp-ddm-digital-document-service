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

package com.epam.digital.data.platform.dgtldcmnt.dto;

import lombok.Builder;
import lombok.Data;

/**
 * The dto that contains document id and a context info for getting document.
 */
@Data
@Builder
public class GetDocumentDto {

  /**
   * The document id.
   */
  private String id;

  /**
   * The process instance id to whom the document belongs to. It is used for authorization.
   */
  private String processInstanceId;

  /**
   * The task id that has ui form and filedName under which document is loaded. It is used for
   * authorization.
   */
  private String taskId;

  /**
   * The field name on ui form under which document is loaded. It is used for authorization and
   * validation.
   */
  private String fieldName;
}
