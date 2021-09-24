package com.epam.digital.data.platform.dgtldcmnt.service;

import com.epam.digital.data.platform.starter.validation.dto.ComponentsDto;
import com.epam.digital.data.platform.starter.validation.dto.FormDto;
import com.epam.digital.data.platform.starter.validation.dto.NestedComponentDto;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.rest.dto.task.TaskDto;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
  public static final String FIELD_NAMES_NOT_FOUND_MSG = "Task form does not have fields with names %s";

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
   * @param processInstanceId specified process instance id in which the documents are loaded
   * @param fieldNames        the field names of the ui form in which documents are loaded.
   * @param taskDto           the task in which the documents are loaded.
   * @param formDto           the ui form in which the documents are loaded.
   * @throws AccessDeniedException when user does not have permission or access to the documents.
   */
  public void authorize(String processInstanceId, List<String> fieldNames, TaskDto taskDto,
      FormDto formDto) {
    log.debug("Starting authorization for files {} for task {} in process {}", fieldNames,
        taskDto.getId(), processInstanceId);
    checkTaskExistenceInProcessInstance(processInstanceId, taskDto);
    checkTaskStatus(taskDto);
    checkTaskAssignee(taskDto);
    checkFiledNamesExistence(fieldNames, formDto);
    log.debug("Files {} for task {} have been authorized for user", fieldNames, taskDto.getId());
  }

  private void checkTaskExistenceInProcessInstance(String processInstanceId, TaskDto taskDto) {
    if (!taskDto.getProcessInstanceId().equals(processInstanceId)) {
      throw new AccessDeniedException(String.format(TASK_NOT_FOUND_IN_PROCESS_INSTANCE_MSG,
          taskDto.getId(), processInstanceId));
    }
    log.trace("Task's {} process instance was verified ({})", taskDto.getId(), processInstanceId);
  }

  private void checkTaskStatus(TaskDto taskDto) {
    if (taskDto.isSuspended()) {
      throw new AccessDeniedException(String.format(TASK_IS_SUSPENDED_MSG, taskDto.getId()));
    }
    log.trace("Verified that task {} isn't suspended", taskDto.getId());
  }

  private void checkTaskAssignee(TaskDto taskDto) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (Objects.isNull(authentication) || !authentication.getName().equals(taskDto.getAssignee())) {
      throw new AccessDeniedException(
          String.format(CURRENT_USER_IS_NOT_ASSIGNED_MSG, taskDto.getId()));
    }
    log.trace("Verified that current user {} has assigned to task {}", authentication.getName(),
        taskDto.getId());
  }

  private void checkFiledNamesExistence(List<String> fieldNames, FormDto formDto) {
    var componentsKeys = formDto.getComponents().stream().map(ComponentsDto::getKey)
        .collect(Collectors.toList());
    var nestedComponentKeys = formDto.getComponents().stream()
        .filter(c -> !CollectionUtils.isEmpty(c.getComponents()))
        .flatMap(c -> c.getComponents().stream()).map(NestedComponentDto::getKey)
        .collect(Collectors.toList());
    var formKeys = Stream.of(componentsKeys, nestedComponentKeys)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    if (!formKeys.containsAll(fieldNames)) {
      var notFoundFiledNames = fieldNames.stream().filter(fn -> !formKeys.contains(fn))
          .collect(Collectors.toList());
      throw new AccessDeniedException(String.format(FIELD_NAMES_NOT_FOUND_MSG, notFoundFiledNames));
    }
    log.trace("Verified that all field names {} are present on form {}", fieldNames, formDto);
  }
}
