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

package com.epam.digital.data.platform.dgtldcmnt.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.epam.digital.data.platform.dgtldcmnt.config.DigitalDocumentsConfigurationProperties;
import com.epam.digital.data.platform.dgtldcmnt.config.DigitalDocumentsConfigurationProperties.ContentConfigurationProperties;
import com.epam.digital.data.platform.dgtldcmnt.util.unit.FractionalDataSize;
import com.epam.digital.data.platform.starter.errorhandling.exception.ValidationException;
import org.junit.jupiter.api.Test;

class RemoteFileSizeValidatorTest {

  private final RemoteFileSizeValidator instance = new RemoteFileSizeValidator(
      new DigitalDocumentsConfigurationProperties(
          FractionalDataSize.parse("1MB"),
          FractionalDataSize.parse("1MB"),
          new ContentConfigurationProperties("")));

  @Test
  void shouldNotThrowException() {
    assertDoesNotThrow(() -> instance.validate(1024 * 1024));
  }

  @Test
  void shouldThrowException() {
    assertThrows(ValidationException.class, () -> instance.validate(1024 * 1024 + 1));
  }
}