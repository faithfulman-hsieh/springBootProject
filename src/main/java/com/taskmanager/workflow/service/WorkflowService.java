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
     * ★★★ 修正：增加參數 title, description, priority ★★★
     */
    public String startProcess(String assignee, String title, String description, String priority) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("assignee", assignee);

        // ★★★ 關鍵：必須把這些資料存入流程變數，getTaskForm 才能讀取到並回傳給前端 ★★★
        variables.put("todoTitle", title);
        variables.put("description", description);
        variables.put("priority", priority != null ? priority : "medium");

        return runtimeService.startProcessInstanceByKey("todoProcess", variables).getProcessInstanceId();
    }

    public List<String> getUserTasks(String assignee) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        return tasks.stream().map(Task::getName).collect(Collectors.toList());
    }

    public Task getCurrentTask(String processInstanceId) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
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