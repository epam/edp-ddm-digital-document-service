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

package com.epam.digital.data.platform.dgtldcmnt.config;

import com.epam.digital.data.platform.dgtldcmnt.facade.DocumentFacade;
import com.epam.digital.data.platform.dgtldcmnt.service.InternalApiDocumentService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestBeansConfig {

  @Bean
  public DocumentFacade testDocumentFacade() {
    return Mockito.mock(DocumentFacade.class);
  }

  @Bean
  public InternalApiDocumentService testInternalApiDocumentService() {
    return Mockito.mock(InternalApiDocumentService.class);
  }
}
