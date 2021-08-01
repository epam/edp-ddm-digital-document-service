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
