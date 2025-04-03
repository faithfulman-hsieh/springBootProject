package com.taskmanager.workflow.service;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkflowService {
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public WorkflowService(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    /**
     * 啟動 todoProcess 流程
     */
    public String startProcess(String assignee) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("assignee", assignee);
        return runtimeService.startProcessInstanceByKey("todoProcess", variables).getProcessInstanceId();
    }

    /**
     * 取得某使用者的待處理任務
     */
    public List<String> getUserTasks(String assignee) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        return tasks.stream().map(Task::getName).collect(Collectors.toList());
    }

    /**
     * 查詢目前 ProcessInstance 的 Task
     */
    public Task getCurrentTask(String processInstanceId) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
    }

    /**
     * 完成任務
     */
    public void completeTask(String taskId, String action, String priority) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("action", action);
        variables.put("priority", priority);
        taskService.complete(taskId, variables);
    }
}