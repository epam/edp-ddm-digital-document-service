package com.epam.digital.data.platform.dgtldcmnt.dto;

import java.io.InputStream;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentDto {

  private Long size;
  private String name;
  private String contentType;
  private InputStream content;
}
