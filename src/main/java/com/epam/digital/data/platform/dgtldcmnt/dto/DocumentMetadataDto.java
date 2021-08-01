package com.epam.digital.data.platform.dgtldcmnt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
public class DocumentMetadataDto {

  private String id;
  private String url;
  private String name;
  private String type;
  private String checksum;
  private Long size;
}
