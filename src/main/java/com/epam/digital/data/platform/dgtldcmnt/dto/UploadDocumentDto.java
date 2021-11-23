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

import java.io.InputStream;
import lombok.Builder;
import lombok.Data;

/**
 * It contains document input stream and describes a file metadata and a context with which the
 * document should be uploaded
 */
@Data
@Builder
public class UploadDocumentDto {

  private Long size;
  private String filename;
  private String contentType;
  private InputStream fileInputStream;

  /**
   * The task id that has ui form and filedName under which document is loaded. It is used for
   * authorization.
   */
  private String taskId;

  /**
   * The field name on ui form under which document is loaded. It is used for validation.
   */
  private String fieldName;

  /**
   * The process instance id to whom the document belongs to. It is used for authorization.
   */
  private String processInstanceId;

  /**
   * The origin forwarded request url.
   */
  private String originRequestUrl;

}
