/*
 * Copyright 2025 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.dgtldcmnt.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Parameters for file compression operations.
 * This class contains configuration parameters that can be applied to various file types,
 * including but not limited to images, documents, and other file formats.
 * Specific parameters may apply only to certain file types (e.g., imageMaxWidth is only
 * applicable to image files).
 */
@Builder
@Getter
public class ImageCompressorParameters {

  /**
   * Maximum width for image compression in pixels.
   * Only applicable when compressing image files.
   */
  private Integer imageMaxWidth;

  /**
   * Maximum height for image compression in pixels.
   * Only applicable when compressing image files.
   */
  private Integer imageMaxHeight;

  /**
   * Compression quality factor between 0 (maximum compression) and 100 (highest quality).
   * The interpretation may vary depending on the file type being compressed.
   */
  private Integer compressionQuality;
}