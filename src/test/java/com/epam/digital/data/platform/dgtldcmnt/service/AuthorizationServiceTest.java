/*
 * Copyright 2021 EPAM Systems.
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
import static com.epam.digital.data.platform.dgtldcmnt.service.AuthorizationService.FIELD_NAMES_NOT_FOUND_MSG;
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
public class AuthorizationServiceTest {

  private AuthorizationService authorizationService;

  private final String taskId = "testTaskId";
  private final String processInstanceId = "testProcessInstanceId";
  private final List<String> fieldNames = List.of("testUpload1");
  private final String formKey = "upload-test";
  private final String assignee = "testAssignee";

  private DdmSignableTaskDto taskDto;
  private FormDto formDto;
  private Authentication authentication;

  @BeforeEach
  public void init() {
    authorizationService = new AuthorizationService();
    taskDto = new DdmSignableTaskDto();
    taskDto.setFormKey(formKey);
    taskDto.setId(taskId);
    taskDto.setAssignee(assignee);
    taskDto.setProcessInstanceId(processInstanceId);

    var componentsDtos = List
        .of(new ComponentsDto("testUpload1", "file", null, null, "application/pdf", "50MB", null));
    formDto = new FormDto(componentsDtos);

    var principal = new User(assignee, "", new ArrayList<>());
    authentication = new UsernamePasswordAuthenticationToken(principal, null, null);
  }

  @Test
  public void shouldAuthorize() {
    assertDoesNotThrow(
        () -> authorizationService
            .authorize(processInstanceId, fieldNames, taskDto, formDto, authentication));
  }

  @Test
  public void shouldNotAuthorizeTaskDoesNotBelongToProcessInstance() {
    var invalidProcessInstance = "invalidProcessInstance";
    var exception = assertThrows(AccessDeniedException.class,
        () -> authorizationService
            .authorize(invalidProcessInstance, fieldNames, taskDto, formDto, authentication));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(TASK_NOT_FOUND_IN_PROCESS_INSTANCE_MSG, taskId, invalidProcessInstance));
  }

  @Test
  public void shouldNotAuthorizeProcessInstanceIsNotActive() {
    var notActiveProcessInstanceId = "notActiveProcessInstanceId";
    var taskDto = new DdmSignableTaskDto();
    taskDto.setFormKey(formKey);
    taskDto.setId(taskId);
    taskDto.setAssignee(assignee);
    taskDto.setProcessInstanceId(notActiveProcessInstanceId);
    taskDto.setSuspended(true);

    var exception = assertThrows(AccessDeniedException.class,
        () -> authorizationService.authorize(notActiveProcessInstanceId, fieldNames, taskDto,
            formDto, authentication));

    assertThat(exception.getMessage()).isEqualTo(String.format(TASK_IS_SUSPENDED_MSG, taskId));
  }

  @Test
  public void shouldNotAuthorizeCurrentUserIsNotAssigned() {
    var taskDto = new DdmSignableTaskDto();
    taskDto.setFormKey(formKey);
    taskDto.setId(taskId);
    taskDto.setAssignee("invalidAssignee");
    taskDto.setProcessInstanceId(processInstanceId);

    var exception = assertThrows(AccessDeniedException.class,
        () -> authorizationService
            .authorize(processInstanceId, fieldNames, taskDto, formDto, authentication));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(CURRENT_USER_IS_NOT_ASSIGNED_MSG, taskId));
  }

  @Test
  public void shouldNotAuthorizeFileNameNotFound() {
    var componentsDtos = List
        .of(new ComponentsDto("invalidFiledName", "file", null, null, "application/pdf", "50MB",
            null));
    var formDto = new FormDto(componentsDtos);

    var exception = assertThrows(AccessDeniedException.class,
        () -> authorizationService
            .authorize(processInstanceId, fieldNames, taskDto, formDto, authentication));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(FIELD_NAMES_NOT_FOUND_MSG, fieldNames));
  }

  @Test
  public void shouldAuthorizeWithNestedComponentKeys() {
    var nestedComponentDto = new NestedComponentDto("testUpload1", "file", null, null, null, null);
    var componentsDtos = List
        .of(new ComponentsDto(null, null, null, List.of(nestedComponentDto), null, null, null));
    var formDto = new FormDto(componentsDtos);

    assertDoesNotThrow(
        () -> authorizationService
            .authorize(processInstanceId, fieldNames, taskDto, formDto, authentication));
  }
}