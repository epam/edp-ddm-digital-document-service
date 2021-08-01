package com.epam.digital.data.platform.dgtldcmnt.dto;

import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;

/**
 * Data transfer object that is used to set formKey field.
 */
public class TestTaskDto extends TaskEntity {

  public void setFormKey(String formKey) {
    isFormKeyInitialized = true;
    this.formKey = formKey;
  }

  @Override
  public void setTaskDefinitionKey(String taskDefinitionKey) {
    this.taskDefinitionKey = taskDefinitionKey;
  }
}
