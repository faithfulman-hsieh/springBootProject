package com.taskmanager.task.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

// ★★★ 關鍵修正：忽略前端多傳的欄位 (如 processInstanceId)，避免 400 錯誤 ★★★
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskJumpRequest {
    private String targetNode;
    private Map<String, Object> variables;

    public String getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(String targetNode) {
        this.targetNode = targetNode;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
}