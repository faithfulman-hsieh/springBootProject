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
            // ★★★ 修正點：先檢查變數中是否已經有 assignee ★★★
            String existingAssignee = (String) execution.getVariable("assignee");
            String assignee;

            if (existingAssignee != null && !existingAssignee.isEmpty()) {
                assignee = existingAssignee;
                logger.info("ℹ️ 使用已存在的 Assignee: {}", assignee);
            } else {
                assignee = determineAssignee(execution);
                execution.setVariable("assignee", assignee);
                logger.info("✅ 任務自動分派完成 - 分派給: {}, 流程實例ID: {}",
                        assignee, execution.getProcessInstanceId());
            }

        } catch (Exception e) {
            logger.error("❌ 自動分派任務失敗 - 流程實例ID: {}",
                    execution.getProcessInstanceId(), e);
            throw new RuntimeException("自動分派任務失敗", e);
        }
    }

    private String determineAssignee(DelegateExecution execution) {
        // 這裡可以實作更複雜的分派邏輯
        return defaultAssignee;
    }
}