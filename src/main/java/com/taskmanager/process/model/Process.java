package com.taskmanager.process.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Process {
    @Id
    private String id;
    private String name;
    private String version;
    private String status;
    private String deploymentTime;
    private String processDefinitionId;
    private String startTime;
    private String currentTask;
    private String assignee;

    public Process() {
    }

    public Process(String id, String name, String version, String status, String deploymentTime,
                   String processDefinitionId, String startTime, String currentTask, String assignee) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.status = status;
        this.deploymentTime = deploymentTime;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeploymentTime() {
        return deploymentTime;
    }

    public void setDeploymentTime(String deploymentTime) {
        this.deploymentTime = deploymentTime;
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