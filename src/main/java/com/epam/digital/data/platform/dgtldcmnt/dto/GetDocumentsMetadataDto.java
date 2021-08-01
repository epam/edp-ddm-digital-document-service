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
