// src/main/java/com/taskmanager/task/dto/TaskJumpRequest.java
package com.taskmanager.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request object for jumping to a specific task")
public class TaskJumpRequest {

    @Schema(description = "ID of the target task", example = "ManagerTask", required = true)
    private String targetTaskId;

    public String getTargetTaskId() { return targetTaskId; }
    public void setTargetTaskId(String targetTaskId) { this.targetTaskId = targetTaskId; }
}