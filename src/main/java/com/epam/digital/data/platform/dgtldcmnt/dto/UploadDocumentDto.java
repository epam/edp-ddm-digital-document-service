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
