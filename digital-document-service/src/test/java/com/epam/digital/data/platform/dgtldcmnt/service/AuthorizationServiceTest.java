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

package com.epam.digital.data.platform.dgtldcmnt.service;

import static com.epam.digital.data.platform.dgtldcmnt.service.AuthorizationService.CURRENT_USER_IS_NOT_ASSIGNED_MSG;
import static com.epam.digital.data.platform.dgtldcmnt.service.AuthorizationService.TASK_IS_SUSPENDED_MSG;
import static com.epam.digital.data.platform.dgtldcmnt.service.AuthorizationService.TASK_NOT_FOUND_IN_PROCESS_INSTANCE_MSG;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.epam.digital.data.platform.bpms.api.dto.DdmSignableTaskDto;
import com.epam.digital.data.platform.integration.formprovider.dto.ComponentsDto;
import com.epam.digital.data.platform.integration.formprovider.dto.FormDto;
import com.epam.digital.data.platform.integration.formprovider.dto.NestedComponentDto;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

  private AuthorizationService authorizationService;

  private final String taskId = "testTaskId";
  private final String rootProcessInstanceId = "testProcessInstanceId";
  private final List<String> fieldNames = List.of("testUpload1");
  private final String formKey = "upload-test";
  private final String assignee = "testAssignee";

  private DdmSignableTaskDto taskDto;
  private Authentication authentication;

  @BeforeEach
  public void init() {
    authorizationService = new AuthorizationService();
    taskDto = new DdmSignableTaskDto();
    taskDto.setFormKey(formKey);
    taskDto.setId(taskId);
    taskDto.setAssignee(assignee);
    taskDto.setRootProcessInstanceId(rootProcessInstanceId);

    var principal = new User(assignee, "", new ArrayList<>());
    authentication = new UsernamePasswordAuthenticationToken(principal, null, null);
  }

  @Test
  void shouldAuthorize() {
    assertDoesNotThrow(
        () -> authorizationService
            .authorize(rootProcessInstanceId, fieldNames, taskDto, authentication));
  }

  @Test
  void shouldNotAuthorizeTaskDoesNotBelongToProcessInstance() {
    var invalidProcessInstance = "invalidProcessInstance";
    var exception = assertThrows(AccessDeniedException.class,
        () -> authorizationService
            .authorize(invalidProcessInstance, fieldNames, taskDto, authentication));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(TASK_NOT_FOUND_IN_PROCESS_INSTANCE_MSG, taskId, invalidProcessInstance));
  }

  @Test
  void shouldNotAuthorizeProcessInstanceIsNotActive() {
    var notActiveProcessInstanceId = "notActiveProcessInstanceId";
    var taskDto = new DdmSignableTaskDto();
    taskDto.setFormKey(formKey);
    taskDto.setId(taskId);
    taskDto.setAssignee(assignee);
    taskDto.setRootProcessInstanceId(notActiveProcessInstanceId);
    taskDto.setSuspended(true);

    var exception = assertThrows(AccessDeniedException.class,
        () -> authorizationService.authorize(notActiveProcessInstanceId, fieldNames, taskDto,
            authentication));

    assertThat(exception.getMessage()).isEqualTo(String.format(TASK_IS_SUSPENDED_MSG, taskId));
  }

  @Test
  void shouldNotAuthorizeCurrentUserIsNotAssigned() {
    var taskDto = new DdmSignableTaskDto();
    taskDto.setFormKey(formKey);
    taskDto.setId(taskId);
    taskDto.setAssignee("invalidAssignee");
    taskDto.setRootProcessInstanceId(rootProcessInstanceId);

    var exception = assertThrows(AccessDeniedException.class,
        () -> authorizationService
            .authorize(rootProcessInstanceId, fieldNames, taskDto, authentication));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(CURRENT_USER_IS_NOT_ASSIGNED_MSG, taskId));
  }

  @Test
  void shouldAuthorizeWithNestedComponentKeys() {
    var nestedComponentDto = new NestedComponentDto("testUpload1", "file", null, null, null, null);
    var componentsDtos = List
        .of(new ComponentsDto(null, null, null, List.of(nestedComponentDto), null, null, null));
    var formDto = new FormDto(componentsDtos);

    assertDoesNotThrow(
        () -> authorizationService
            .authorize(rootProcessInstanceId, fieldNames, taskDto, authentication));
  }
}