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

package com.epam.digital.data.platform.dgtldcmnt.mapper;

import com.epam.digital.data.platform.dgtldcmnt.dto.DocumentMetadataDto;
import com.epam.digital.data.platform.dgtldcmnt.dto.InternalApiDocumentMetadataDto;
import com.epam.digital.data.platform.storage.file.dto.FileMetadataDto;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * Used for mapping {@link FileMetadataDto} to {@link InternalApiDocumentMetadataDto} and
 * {@link DocumentMetadataDto}
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface DocumentMetadataDtoMapper {

  @Mapping(source = "id", target = "id")
  @Mapping(source = "checksum", target = "checksum")
  @Mapping(source = "filename", target = "name", qualifiedByName = "mapFilename")
  @Mapping(source = "contentType", target = "type")
  @Mapping(source = "contentLength", target = "size")
  InternalApiDocumentMetadataDto toInternalApiDocumentMetadataDto(FileMetadataDto fileMetadataDto);

  @Mapping(source = "fileMetadataDto.id", target = "id")
  @Mapping(source = "fileMetadataDto.checksum", target = "checksum")
  @Mapping(source = "fileMetadataDto.filename", target = "name", qualifiedByName = "mapFilename")
  @Mapping(source = "fileMetadataDto.contentType", target = "type")
  @Mapping(source = "fileMetadataDto.contentLength", target = "size")
  @Mapping(source = "url", target = "url")
  DocumentMetadataDto toDocumentMetadataDto(FileMetadataDto fileMetadataDto, String url);

  @Named("mapFilename")
  default String mapFilename(String fileName) {
    if (Objects.isNull(fileName)) {
      return null;
    }
    return URLDecoder.decode(fileName, StandardCharsets.UTF_8);
  }
}
