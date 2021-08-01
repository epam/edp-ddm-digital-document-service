package com.epam.digital.data.platform.dgtldcmnt.service;

import static com.epam.digital.data.platform.dgtldcmnt.service.AuthorizationService.CURRENT_USER_IS_NOT_ASSIGNED_MSG;
import static com.epam.digital.data.platform.dgtldcmnt.service.AuthorizationService.FIELD_NAMES_NOT_FOUND_MSG;
import static com.epam.digital.data.platform.dgtldcmnt.service.AuthorizationService.PROCESS_INSTANCE_IS_NOT_ACTIVE_MSG;
import static com.epam.digital.data.platform.dgtldcmnt.service.AuthorizationService.TASK_NOT_FOUND_IN_PROCESS_INSTANCE_MSG;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.bpms.client.ProcessInstanceHistoryRestClient;
import com.epam.digital.data.platform.dgtldcmnt.dto.TestTaskDto;
import com.epam.digital.data.platform.starter.validation.dto.ComponentsDto;
import com.epam.digital.data.platform.starter.validation.dto.FormDto;
import java.util.ArrayList;
import java.util.List;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.camunda.bpm.engine.rest.dto.history.HistoricProcessInstanceDto;
import org.camunda.bpm.engine.rest.dto.task.TaskDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationServiceTest {

  @Mock
  private ProcessInstanceHistoryRestClient processInstanceHistoryRestClient;

  private AuthorizationService authorizationService;

  private final String taskId = "testTaskId";
  private final String processInstanceId = "testProcessInstanceId";
  private final String fieldName = "testUpload1";
  private final String formKey = "upload-test";
  private final String assignee = "testAssignee";

  private TaskDto taskDto;
  private FormDto formDto;

  @Before
  public void init() {
    authorizationService = new AuthorizationService(processInstanceHistoryRestClient);
    var task = new TestTaskDto();
    task.setFormKey(formKey);
    task.setId(taskId);
    task.setAssignee(assignee);
    task.setProcessInstanceId(processInstanceId);
    taskDto = TaskDto.fromEntity(task);

    var componentsDtos = List
        .of(new ComponentsDto("testUpload1", "file", null, "application/pdf", "50MB"));
    formDto = new FormDto(componentsDtos);

    var historicProcessInstance = new HistoricProcessInstanceEntity();
    historicProcessInstance.setId(processInstanceId);
    historicProcessInstance.setState(HistoricProcessInstance.STATE_ACTIVE);
    var processInstance = HistoricProcessInstanceDto
        .fromHistoricProcessInstance(historicProcessInstance);
    when(processInstanceHistoryRestClient.getProcessInstanceById(processInstanceId))
        .thenReturn(processInstance);

    var principal = new User(assignee, "", new ArrayList<>());
    var authentication = new UsernamePasswordAuthenticationToken(principal, null, null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @Test
  public void shouldAuthorize() {
    authorizationService.authorize(processInstanceId, List.of(fieldName), taskDto, formDto);
  }

  @Test
  public void shouldNotAuthorizeTaskDoesNotBelongToProcessInstance() {
    var invalidProcessInstance = "invalidProcessInstance";
    var exception = assertThrows(AccessDeniedException.class,
        () -> authorizationService
            .authorize(invalidProcessInstance, List.of(fieldName), taskDto, formDto));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(TASK_NOT_FOUND_IN_PROCESS_INSTANCE_MSG, taskId, invalidProcessInstance));
  }

  @Test
  public void shouldNotAuthorizeProcessInstanceIsNotActive() {
    var notActiveProcessInstanceId = "notActiveProcessInstanceId";
    var task = new TestTaskDto();
    task.setFormKey(formKey);
    task.setId(taskId);
    task.setAssignee(assignee);
    task.setProcessInstanceId(notActiveProcessInstanceId);
    var historicProcessInstance = new HistoricProcessInstanceEntity();
    historicProcessInstance.setId(notActiveProcessInstanceId);
    historicProcessInstance.setState(HistoricProcessInstance.STATE_SUSPENDED);
    var processInstance = HistoricProcessInstanceDto
        .fromHistoricProcessInstance(historicProcessInstance);
    when(processInstanceHistoryRestClient.getProcessInstanceById(notActiveProcessInstanceId))
        .thenReturn(processInstance);

    var exception = assertThrows(AccessDeniedException.class,
        () -> authorizationService
            .authorize(notActiveProcessInstanceId, List.of(fieldName), TaskDto.fromEntity(task),
                formDto));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(PROCESS_INSTANCE_IS_NOT_ACTIVE_MSG, notActiveProcessInstanceId));
  }

  @Test
  public void shouldNotAuthorizeCurrenUserIsNotAssigned() {
    var task = new TestTaskDto();
    task.setFormKey(formKey);
    task.setId(taskId);
    task.setAssignee("invalidAssignee");
    task.setProcessInstanceId(processInstanceId);

    var exception = assertThrows(AccessDeniedException.class,
        () -> authorizationService
            .authorize(processInstanceId, List.of(fieldName), TaskDto.fromEntity(task), formDto));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(CURRENT_USER_IS_NOT_ASSIGNED_MSG, taskId));
  }

  @Test
  public void shouldNotAuthorizeFileNameNotFound() {
    var componentsDtos = List
        .of(new ComponentsDto("invalidFiledName", "file", null, "application/pdf", "50MB"));
    var formDto = new FormDto(componentsDtos);

    var exception = assertThrows(AccessDeniedException.class,
        () -> authorizationService
            .authorize(processInstanceId, List.of(fieldName), taskDto, formDto));

    assertThat(exception.getMessage()).isEqualTo(
        String.format(FIELD_NAMES_NOT_FOUND_MSG, List.of(fieldName)));
  }
}