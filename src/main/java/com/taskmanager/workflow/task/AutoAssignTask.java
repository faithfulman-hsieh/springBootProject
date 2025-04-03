package com.taskmanager.workflow.task;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AutoAssignTask implements JavaDelegate {
    private static final Logger logger = LoggerFactory.getLogger(AutoAssignTask.class);

    @Value("${workflow.default-assignee:user}")
    private final String defaultAssignee = "user";

    @Override
    public void execute(DelegateExecution execution) {
        try {
            String assignee = determineAssignee(execution);
            execution.setVariable("assignee", assignee);
            logger.info("✅ 任務自動分派完成 - 分派給: {}, 流程實例ID: {},execution.getId(): {},execution.getProcessDefinitionId(): {}",
                    assignee, execution.getProcessInstanceId(),execution.getId(),execution.getProcessDefinitionId());
        } catch (Exception e) {
            logger.error("❌ 自動分派任務失敗 - 流程實例ID: {}",
                    execution.getProcessInstanceId(), e);
            throw new RuntimeException("自動分派任務失敗", e);
        }
    }

    private String determineAssignee(DelegateExecution execution) {
        // 這裡可以實作更複雜的分派邏輯，例如:
        // 1. 從流程變數獲取候選人
        // 2. 查詢資料庫獲取合適人選
        // 3. 調用外部API獲得分派資訊
        return defaultAssignee;
    }
}