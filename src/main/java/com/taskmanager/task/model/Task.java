package com.taskmanager.task.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    private String id;
    private String name;
    private String processName;
    private String assignee;
    private String createTime;

    // 1. 無參數建構子 (JPA 需要)
    public Task() {
    }

    // 2. 全參數建構子 (Service 需要)
    public Task(String id, String name, String processName, String assignee, String createTime) {
        this.id = id;
        this.name = name;
        this.processName = processName;
        this.assignee = assignee;
        this.createTime = createTime;
    }

    // 3. 標準 Getter/Setter (確保 JSON 序列化一定成功)
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

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}