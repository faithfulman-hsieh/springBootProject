// src/main/java/com/taskmanager/task/dto/TaskReassignRequest.java
package com.taskmanager.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request object for reassigning a task")
public class TaskReassignRequest {

    @Schema(description = "Username of the new assignee", example = "john_doe", required = true)
    private String assignee;

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
}