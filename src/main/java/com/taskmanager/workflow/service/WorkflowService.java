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

    public String startProcess(String assignee, String title, String description, String priority) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("assignee", assignee);
        variables.put("todoTitle", title);
        variables.put("description", description);
        variables.put("priority", priority != null ? priority : "medium");

        return runtimeService.startProcessInstanceByKey("todoProcess", variables).getProcessInstanceId();
    }

    // ★★★ 新增：支援任意流程啟動的通用方法 ★★★
    public String startProcessByKey(String processDefinitionKey, Map<String, Object> variables) {
        return runtimeService.startProcessInstanceByKey(processDefinitionKey, variables).getId();
    }

    public List<String> getUserTasks(String assignee) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        return tasks.stream().map(Task::getName).collect(Collectors.toList());
    }

    // ★★★ 修正：使用 list() 以避免並行流程導致 singleResult 報錯 ★★★
    public Task getCurrentTask(String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();

        // 如果有多個任務，回傳第一個以保持相容性，避免系統崩潰
        if (tasks != null && !tasks.isEmpty()) {
            return tasks.get(0);
        }
        return null;
    }

    public void completeTask(String taskId, String action, String priority) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("action", action);
        if (priority != null) {
            variables.put("priority", priority);
        }
        taskService.complete(taskId, variables);
    }
}