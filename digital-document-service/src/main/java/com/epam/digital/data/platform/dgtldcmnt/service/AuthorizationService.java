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

import com.epam.digital.data.platform.bpms.api.dto.DdmSignableTaskDto;
import com.epam.digital.data.platform.integration.formprovider.client.FormValidationClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Authorization service that determines if the client has permission or access to manage the
 * documents based on a context(process instance, form metadata, user task info etc.).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationService {

  public static final String TASK_NOT_FOUND_IN_PROCESS_INSTANCE_MSG = "The task (%s) does not belong to the process instance (%s)";
  public static final String TASK_IS_SUSPENDED_MSG = "The task (%s) is suspended";
  public static final String CURRENT_USER_IS_NOT_ASSIGNED_MSG = "Current user is not assigned for the task (%s)";

  /**
   * The method determines if the client has permission or access to manage the documents based on
   * the document's context(process instance, form metadata, user task info etc.) in which the
   * documents are loaded. Verification includes:
   *
   * <li> Checking task existence in the provided process instance(process instance id).
   * <li> Check process instance status (it should be active).
   * <li> Checking task assignee (current user should be assigned).
   * <li> Checking filed names existence in the form metadata
   *
   * @param rootProcessInstanceId specified id of root process instance in which the documents are
   *                              loaded
   * @param fieldNames            the field names of the ui form in which documents are loaded.
   * @param taskDto               the task in which the documents are loaded.
   * @param authentication        object with authentication data.
   * @throws AccessDeniedException when user does not have permission or access to the documents.
   */
  public void authorize(String rootProcessInstanceId, List<String> fieldNames,
      DdmSignableTaskDto taskDto, Authentication authentication) {
    log.debug("Starting authorization for files {} for task {} in process {}", fieldNames,
        taskDto.getId(), rootProcessInstanceId);
    checkTaskExistenceInProcessInstance(rootProcessInstanceId, taskDto);
    checkTaskStatus(taskDto);
    checkTaskAssignee(taskDto, authentication);
    log.debug("Files {} for task {} have been authorized for user", fieldNames, taskDto.getId());
  }

  private void checkTaskExistenceInProcessInstance(String rootProcessInstanceId,
      DdmSignableTaskDto taskDto) {
    if (!taskDto.getRootProcessInstanceId().equals(rootProcessInstanceId)) {
      throw new AccessDeniedException(String.format(TASK_NOT_FOUND_IN_PROCESS_INSTANCE_MSG,
          taskDto.getId(), rootProcessInstanceId));
    }
    log.trace("Task's {} root process instance was verified ({})", taskDto.getId(),
        rootProcessInstanceId);
  }

  private void checkTaskStatus(DdmSignableTaskDto taskDto) {
    if (taskDto.isSuspended()) {
      throw new AccessDeniedException(String.format(TASK_IS_SUSPENDED_MSG, taskDto.getId()));
    }
    log.trace("Verified that task {} isn't suspended", taskDto.getId());
  }

  private void checkTaskAssignee(DdmSignableTaskDto taskDto, Authentication authentication) {
    if (!authentication.getName().equals(taskDto.getAssignee())) {
      throw new AccessDeniedException(
          String.format(CURRENT_USER_IS_NOT_ASSIGNED_MSG, taskDto.getId()));
    }
    log.trace("Verified that current user {} has assigned to task {}", authentication.getName(),
        taskDto.getId());
  }
}