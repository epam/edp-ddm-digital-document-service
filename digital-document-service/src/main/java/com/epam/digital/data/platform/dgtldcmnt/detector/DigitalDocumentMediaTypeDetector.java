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

package com.epam.digital.data.platform.dgtldcmnt.detector;

import com.epam.digital.data.platform.dgtldcmnt.constant.DocumentConstants;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;

/**
 * Implementation of media type {@link Detector} that detects original file content type even if
 * this file is signed (has type application/pkcs7-signature)
 */
@Slf4j
@RequiredArgsConstructor
public class DigitalDocumentMediaTypeDetector implements Detector {

  private static final int SIGNATURE_BYTES_LENGTH = 65;

  private final Detector defaultDetector;

  @Override
  public MediaType detect(InputStream input, Metadata metadata) throws IOException {
    var mediaType = defaultDetector.detect(input, metadata);
    log.trace("Detected media type - '{}'", mediaType);

    if (mediaType.equals(MediaType.parse(DocumentConstants.SIGNATURE_TYPE))) {
      log.trace("As media type is  - '{}'. Checking filename extension",
          DocumentConstants.SIGNATURE_TYPE);
      // if file extension is p7s then return found media type
      var name = metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY);
      var extension = FilenameUtils.getExtension(name);
      if (DocumentConstants.P7S_EXTENSION.equals(extension)) {
        log.trace("As filename extension is '{}' then return found media type - '{}'",
            DocumentConstants.P7S_EXTENSION, mediaType);
        return mediaType;
      }

      log.trace("Filename extension is not '{}'. "
              + "Skip first '{}' bytes in input stream and detect again",
          DocumentConstants.P7S_EXTENSION, SIGNATURE_BYTES_LENGTH);
      input.mark(SIGNATURE_BYTES_LENGTH);
      // file is signed so skip first 65 bytes and detect again
      try {
        input.readNBytes(SIGNATURE_BYTES_LENGTH);
        var newMediaType = defaultDetector.detect(new BufferedInputStream(input), metadata);
        log.trace("Detected media type for signed file - '{}'", newMediaType);
        return newMediaType;
      } finally {
        input.reset();
      }
    }

    return mediaType;
  }
}
