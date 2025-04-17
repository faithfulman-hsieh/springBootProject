package com.taskmanager.process.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ProcessInstance {
    @Id
    private String id;
    private String name;
    private String status;
    private String processDefinitionId;
    private String startTime;
    private String currentTask;
    private String assignee;

    public ProcessInstance() {
    }

    public ProcessInstance(String id, String name, String status, String processDefinitionId, String startTime, String currentTask, String assignee) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.processDefinitionId = processDefinitionId;
        this.startTime = startTime;
        this.currentTask = currentTask;
        this.assignee = assignee;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(String currentTask) {
        this.currentTask = currentTask;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
}