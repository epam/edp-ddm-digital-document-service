package com.epam.digital.data.platform.dgtldcmnt.dto;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadataSearchRequestDto {

  @NotNull
  private List<DocumentIdDto> documents;
}
