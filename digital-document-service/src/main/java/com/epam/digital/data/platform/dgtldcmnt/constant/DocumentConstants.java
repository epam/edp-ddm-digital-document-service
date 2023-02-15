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

package com.epam.digital.data.platform.dgtldcmnt.constant;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;

public final class DocumentConstants {

  public static final String SIGNATURE_TYPE = "application/pkcs7-signature";
  public static final String P7S_EXTENSION = "p7s";

  public static final Map<String, Set<String>> MEDIA_TYPE_TO_EXTENSIONS_MAP = Map.of(
      "application/pdf", Set.of("pdf"),
      "image/png", Set.of("png"),
      "image/jpeg", Set.of("jpg", "jpeg"),
      "text/csv", Set.of("csv"),
      "application/octet-stream", Set.of("asics"),
      SIGNATURE_TYPE, Set.of(P7S_EXTENSION)
  );

  public static final List<MediaType> SUPPORTED_MEDIA_TYPES = MEDIA_TYPE_TO_EXTENSIONS_MAP.keySet()
      .stream()
      .map(MediaType::parseMediaType)
      .collect(Collectors.toList());

  public static final Map<String, Set<String>> CORRESPONDED_MEDIA_TYPES = Map.of(
      "application/octet-stream", Set.of("application/vnd.etsi.asic-s+zip")
  );

  private DocumentConstants() {
  }
}
