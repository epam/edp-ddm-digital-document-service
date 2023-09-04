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

package com.epam.digital.data.platform.dgtldcmnt.listener.kafka;

import com.epam.digital.data.platform.bpms.api.dto.FileStorageCleanupDto;
import com.epam.digital.data.platform.dgtldcmnt.facade.DocumentFacade;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class FileCleanerListenerTest {

  @InjectMocks
  FileCleanerListener listener;
  @Mock
  DocumentFacade facade;

  @Test
  void processFileCleanupMessage() {
    var processInstanceId = RandomString.make();
    var dto = new FileStorageCleanupDto(processInstanceId);

    listener.processFileCleanupMessages(dto);

    Mockito.verify(facade).delete(processInstanceId);
  }
}
