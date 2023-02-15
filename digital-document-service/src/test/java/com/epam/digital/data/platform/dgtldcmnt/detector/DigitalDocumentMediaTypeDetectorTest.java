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
import java.io.InputStream;
import lombok.SneakyThrows;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class DigitalDocumentMediaTypeDetectorTest {

  @InjectMocks
  DigitalDocumentMediaTypeDetector detector;

  @Mock
  Detector defaultDetector;

  @Mock
  InputStream inputStream;
  @Mock
  Metadata metadata;

  @Test
  @SneakyThrows
  void detectNotSignedFile() {
    Mockito.doReturn(MediaType.OCTET_STREAM).when(defaultDetector).detect(inputStream, metadata);

    Assertions.assertThat(detector.detect(inputStream, metadata))
        .isEqualTo(MediaType.OCTET_STREAM);

    Mockito.verify(defaultDetector).detect(inputStream, metadata);
  }

  @Test
  @SneakyThrows
  void detectSignedFileButExtensionIsP7s() {
    Mockito.doReturn(MediaType.parse(DocumentConstants.SIGNATURE_TYPE)).when(defaultDetector)
        .detect(inputStream, metadata);
    Mockito.doReturn("file.docx.p7s").when(metadata).get(TikaCoreProperties.RESOURCE_NAME_KEY);

    Assertions.assertThat(detector.detect(inputStream, metadata))
        .isEqualTo(MediaType.parse(DocumentConstants.SIGNATURE_TYPE));

    Mockito.verify(defaultDetector).detect(inputStream, metadata);
    Mockito.verify(metadata).get(TikaCoreProperties.RESOURCE_NAME_KEY);
  }

  @Test
  @SneakyThrows
  void detectSignedFileButExtensionIsNotP7s() {
    Mockito.doReturn(MediaType.parse(DocumentConstants.SIGNATURE_TYPE))
        .when(defaultDetector).detect(inputStream, metadata);
    Mockito.doReturn(MediaType.parse("application/pdf"))
        .when(defaultDetector)
        .detect(Mockito.refEq(new BufferedInputStream(inputStream)), Mockito.eq(metadata));
    Mockito.doReturn("file.pdf").when(metadata).get(TikaCoreProperties.RESOURCE_NAME_KEY);

    Assertions.assertThat(detector.detect(inputStream, metadata))
        .isEqualTo(MediaType.parse("application/pdf"));

    Mockito.verify(defaultDetector).detect(inputStream, metadata);
    Mockito.verify(defaultDetector)
        .detect(Mockito.refEq(new BufferedInputStream(inputStream)), Mockito.eq(metadata));
    Mockito.verify(metadata).get(TikaCoreProperties.RESOURCE_NAME_KEY);
    Mockito.verify(inputStream).mark(65);
    Mockito.verify(inputStream).readNBytes(65);
    Mockito.verify(inputStream).reset();
  }

}
