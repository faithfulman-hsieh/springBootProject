package com.taskmanager.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "任務列表顯示用的資料傳輸物件")
public class TaskDto {

    private String id;
    private String name;
    private String processName;
    private String assignee;
    private String createTime;

    // 用於前端查看流程圖
    private String processInstanceId;

    // ★★★ 新增：當前處理人員 (用於經手任務) ★★★
    private String currentAssignee;

    public TaskDto() {
    }

    public TaskDto(String id, String name, String processName, String assignee, String createTime, String processInstanceId, String currentAssignee) {
        this.id = id;
        this.name = name;
        this.processName = processName;
        this.assignee = assignee;
        this.createTime = createTime;
        this.processInstanceId = processInstanceId;
        this.currentAssignee = currentAssignee;
    }

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

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getCurrentAssignee() {
        return currentAssignee;
    }

    public void setCurrentAssignee(String currentAssignee) {
        this.currentAssignee = currentAssignee;
    }
}