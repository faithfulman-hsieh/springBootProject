package com.taskmanager.process.dto;

import java.util.Map;

public class HistoryLog {
    private String activityName; // 節點名稱
    private String activityType; // 類型 (startEvent, userTask, endEvent)
    private String assignee;     // 處理人
    private String startTime;    // 開始時間
    private String endTime;      // 結束時間
    private String duration;     // 耗時
    private String status;       // 狀態 (Completed, Running)
    private Map<String, Object> variables; // 該節點相關變數 (如審核意見)

    public HistoryLog() {}

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }
}