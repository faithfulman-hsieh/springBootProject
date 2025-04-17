// src/main/java/com/taskmanager/task/service/TaskManagerService.java
package com.taskmanager.task.service;

import com.taskmanager.task.model.Task;
import com.taskmanager.task.repository.TaskRepository;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.TaskQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskManagerService {

    private final TaskRepository repository;
    private final TaskService taskService;
    private final RuntimeService runtimeService;

    public TaskManagerService(TaskRepository repository, TaskService taskService, RuntimeService runtimeService) {
        this.repository = repository;
        this.taskService = taskService;
        this.runtimeService = runtimeService;
    }

    public List<Task> getMyTasks() {
        String assignee = "user"; // TODO: 從 SecurityContext 獲取
        TaskQuery query = taskService.createTaskQuery().taskAssignee(assignee);
        List<org.activiti.engine.task.Task> activitiTasks = query.list();
        List<Task> tasks = new ArrayList<>();
        for (org.activiti.engine.task.Task t : activitiTasks) {
            Task task = new Task(
                    t.getId(), t.getName(), t.getProcessDefinitionId().split(":")[0],
                    t.getAssignee(), t.getCreateTime().toInstant().toString()
            );
            tasks.add(task);
            repository.save(task);
        }
        return tasks;
    }

    public List<Map<String, Object>> getTaskForm(String taskId) {
        List<Map<String, Object>> form = new ArrayList<>();
        Map<String, Object> field1 = new HashMap<>();
        field1.put("key", "reason");
        field1.put("label", "原因");
        field1.put("type", "text");
        form.add(field1);
        Map<String, Object> field2 = new HashMap<>();
        field2.put("key", "status");
        field2.put("label", "狀態");
        field2.put("type", "select");
        field2.put("options", List.of(
                Map.of("label", "通過", "value", "approve"),
                Map.of("label", "拒絕", "value", "reject")
        ));
        form.add(field2);
        return form;
    }

    public void submitTaskForm(String taskId, Map<String, Object> formData) {
        taskService.complete(taskId, formData);
        repository.findById(taskId).ifPresent(task -> {
            task.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            repository.save(task);
        });
    }

    public void reassignTask(String taskId, String assignee) {
        taskService.setAssignee(taskId, assignee);
        repository.findById(taskId).ifPresent(task -> {
            task.setAssignee(assignee);
            repository.save(task);
        });
    }

    public void jumpToTask(String instanceId, String targetTaskId) {
        org.activiti.engine.task.Task currentTask = taskService.createTaskQuery()
                .processInstanceId(instanceId).singleResult();
        if (currentTask == null) {
            throw new RuntimeException("No active task found for process instance");
        }

        // 設置流程變數以控制跳轉
        Map<String, Object> variables = new HashMap<>();
        variables.put("targetTask", targetTaskId);
        taskService.complete(currentTask.getId(), variables);

        // 更新本地任務記錄
        repository.findById(currentTask.getId()).ifPresent(task -> {
            task.setName(targetTaskId);
            task.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            repository.save(task);
        });
    }
}