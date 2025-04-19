package com.taskmanager.process.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ProcessDef {
    @Id
    private String id;
    private String name;
    private String version;
    private String status;
    private String deploymentTime;
    private String processDefinitionId;

    public ProcessDef() {
    }

    public ProcessDef(String id, String name, String version, String status, String deploymentTime, String processDefinitionId) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.status = status;
        this.deploymentTime = deploymentTime;
        this.processDefinitionId = processDefinitionId;
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
}