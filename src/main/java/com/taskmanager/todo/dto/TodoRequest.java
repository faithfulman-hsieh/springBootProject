package com.taskmanager.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request object for creating a todo task")
public class TodoRequest {

    @Schema(description = "Title of the todo task", example = "Complete project report", required = true)
    private String title;

    @Schema(description = "Description of the todo task", example = "Finish the final report for the project", required = false)
    private String description;

    @Schema(description = "Username of the assignee", example = "john_doe", required = false)
    private String assignee;

    // getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
}