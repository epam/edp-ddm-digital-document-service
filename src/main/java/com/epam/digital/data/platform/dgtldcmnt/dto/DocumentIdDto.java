package com.epam.digital.data.platform.dgtldcmnt.dto;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentIdDto {

  @NotNull
  private String id;

  /**
   * The field name of the ui form under which document is loaded. It is used for validation.
   */
  @NotNull
  private String fieldName;
}
