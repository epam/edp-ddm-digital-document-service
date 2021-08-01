package com.epam.digital.data.platform.dgtldcmnt.service;

import com.epam.digital.data.platform.bpms.client.ProcessInstanceHistoryRestClient;
import com.epam.digital.data.platform.starter.validation.dto.ComponentsDto;
import com.epam.digital.data.platform.starter.validation.dto.FormDto;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.rest.dto.task.TaskDto;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Authorization service that determines if the client has permission or access to manage the
 * documents based on a context(process instance, form metadata, user task info etc).
 */
@Component
@RequiredArgsConstructor
public class AuthorizationService {

  public static final String TASK_NOT_FOUND_IN_PROCESS_INSTANCE_MSG = "The task (%s) does not belong to the process instance (%s)";
  public static final String PROCESS_INSTANCE_IS_NOT_ACTIVE_MSG = "The process instance (%s) is not active";
  public static final String CURRENT_USER_IS_NOT_ASSIGNED_MSG = "Current user is not assigned for the task (%s)";
  public static final String FIELD_NAMES_NOT_FOUND_MSG = "Task form does not have fields with names %s";

  private final ProcessInstanceHistoryRestClient processInstanceHistoryRestClient;

  /**
   * The method determines if the client has permission or access to manage the documents based on
   * the documents context(process instance, form metadata, user task info etc) in which the
   * documents are loaded. Verification includes:
   *
   * <li> Checking task existence in the provided process instance(process instance id).
   * <li> Check process instance status (it should be active).
   * <li> Checking task assignee (current user should be assigned).
   * <li> Checking filed names existence in the form metadata
   *
   * @param processInstanceId specified process instance id in which the documents are loaded
   * @param fieldNames        the field names of the ui form in which documents are loaded.
   * @param taskDto           the task in which the documents are loaded.
   * @param formDto           the ui form in which the documents are loaded.
   * @throws AccessDeniedException when user does not have permission or access to the documents.
   */
  public void authorize(String processInstanceId, List<String> fieldNames, TaskDto taskDto,
      FormDto formDto) {
    checkTaskExistenceInProcessInstance(processInstanceId, taskDto);
    checkProcessInstanceStatus(processInstanceId);
    checkTaskAssignee(taskDto);
    checkFiledNamesExistence(fieldNames, formDto);
  }

  private void checkTaskExistenceInProcessInstance(String processInstanceId, TaskDto taskDto) {
    if (!taskDto.getProcessInstanceId().equals(processInstanceId)) {
      throw new AccessDeniedException(String.format(TASK_NOT_FOUND_IN_PROCESS_INSTANCE_MSG,
          taskDto.getId(), processInstanceId));
    }
  }

  private void checkProcessInstanceStatus(String processInstanceId) {
    var processInstances = processInstanceHistoryRestClient
        .getProcessInstanceById(processInstanceId);
    if (!HistoricProcessInstance.STATE_ACTIVE.equals(processInstances.getState())) {
      throw new AccessDeniedException(
          String.format(PROCESS_INSTANCE_IS_NOT_ACTIVE_MSG, processInstanceId));
    }
  }

  private void checkTaskAssignee(TaskDto taskDto) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (Objects.isNull(authentication) || !authentication.getName().equals(taskDto.getAssignee())) {
      throw new AccessDeniedException(
          String.format(CURRENT_USER_IS_NOT_ASSIGNED_MSG, taskDto.getId()));
    }
  }

  private void checkFiledNamesExistence(List<String> fieldNames, FormDto formDto) {
    var formKeys = formDto.getComponents().stream().map(ComponentsDto::getKey)
        .collect(Collectors.toList());
    if (!formKeys.containsAll(fieldNames)) {
      var notFoundFiledNames = fieldNames.stream().filter(fn -> !formKeys.contains(fn))
          .collect(Collectors.toList());
      throw new AccessDeniedException(String.format(FIELD_NAMES_NOT_FOUND_MSG, notFoundFiledNames));
    }
  }
}