package com.taskmanager.task.service;

import com.taskmanager.task.model.Task;
import com.taskmanager.task.repository.TaskRepository;
import org.activiti.bpmn.model.*; // 這裡包含了 FormProperty 和 FormValue
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
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
    private final RepositoryService repositoryService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TaskManagerService(TaskRepository repository, TaskService taskService,
                              RuntimeService runtimeService, RepositoryService repositoryService) {
        this.repository = repository;
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
    }

    public List<Task> getMyTasks() {
        String assignee = "user";

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                assignee = auth.getName();
            }
        } catch (Exception e) {
            // ignore
        }

        List<org.activiti.engine.task.Task> activitiTasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime().desc()
                .list();

        List<Task> tasks = new ArrayList<>();
        for (org.activiti.engine.task.Task t : activitiTasks) {
            String processName = "Unknown Process";
            try {
                processName = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(t.getProcessDefinitionId())
                        .singleResult()
                        .getName();
            } catch (Exception e) {
                // ignore
            }

            Task task = new Task(
                    t.getId(),
                    t.getName(),
                    processName,
                    t.getAssignee(),
                    t.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).format(DATE_FORMATTER)
            );
            tasks.add(task);
        }
        return tasks;
    }

    public List<Map<String, Object>> getTaskForm(String taskId) {
        org.activiti.engine.task.Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任務不存在：" + taskId);
        }

        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        FlowElement flowElement = bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());

        if (!(flowElement instanceof UserTask)) {
            return new ArrayList<>();
        }

        UserTask userTask = (UserTask) flowElement;
        List<Map<String, Object>> formFields = new ArrayList<>();

        // 這裡直接使用 FormProperty，因為上方 import org.activiti.bpmn.model.* 已經包含了它
        for (FormProperty prop : userTask.getFormProperties()) {
            Map<String, Object> field = new HashMap<>();
            field.put("key", prop.getId());
            field.put("label", prop.getName() != null ? prop.getName() : prop.getId());

            String type = prop.getType() != null ? prop.getType() : "string";
            field.put("type", mapFormPropertyType(type));
            field.put("required", prop.isRequired());
            field.put("disabled", !prop.isWriteable());

            if ("enum".equals(type)) {
                List<Map<String, String>> options = new ArrayList<>();
                for (FormValue val : prop.getFormValues()) {
                    Map<String, String> option = new HashMap<>();
                    option.put("label", val.getName());
                    option.put("value", val.getId());
                    options.add(option);
                }
                field.put("options", options);

                if ("action".equalsIgnoreCase(prop.getId())) {
                    field.put("uiComponent", "buttons");
                }
            }
            formFields.add(field);
        }

        return formFields;
    }

    public void submitTaskForm(String taskId, Map<String, Object> formData) {
        org.activiti.engine.task.Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任務不存在：" + taskId);
        }

        taskService.complete(taskId, formData);

        repository.findById(taskId).ifPresent(taskEntity -> {
            repository.save(taskEntity);
        });
    }

    public void reassignTask(String taskId, String assignee) {
        org.activiti.engine.task.Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任務不存在：" + taskId);
        }

        taskService.setAssignee(taskId, assignee);

        repository.findById(taskId).ifPresent(taskEntity -> {
            taskEntity.setAssignee(assignee);
            repository.save(taskEntity);
        });
    }

    private String mapFormPropertyType(String activitiType) {
        switch (activitiType) {
            case "string": return "text";
            case "long": return "number";
            case "date": return "date";
            case "enum": return "select";
            case "boolean": return "switch";
            default: return "text";
        }
    }
}