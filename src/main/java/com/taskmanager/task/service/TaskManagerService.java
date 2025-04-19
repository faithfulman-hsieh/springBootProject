package com.taskmanager.task.service;

import com.taskmanager.task.model.Task;
import com.taskmanager.task.repository.TaskRepository;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ExtensionElement;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final RepositoryService repositoryService;

    public TaskManagerService(TaskRepository repository, TaskService taskService, RuntimeService runtimeService, RepositoryService repositoryService) {
        this.repository = repository;
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
    }

    public List<Task> getMyTasks() {
        String assignee = SecurityContextHolder.getContext().getAuthentication().getName();
        List<org.activiti.engine.task.Task> activitiTasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        List<Task> tasks = new ArrayList<>();
        for (org.activiti.engine.task.Task t : activitiTasks) {
            String processName = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(t.getProcessDefinitionId())
                    .singleResult()
                    .getName();
            Task task = new Task(
                    t.getId(),
                    t.getName(),
                    processName,
                    t.getAssignee(),
                    t.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            );
            tasks.add(task);
            repository.save(task);
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
            throw new IllegalStateException("任務非用戶任務：" + taskId);
        }

        UserTask userTask = (UserTask) flowElement;
        List<Map<String, Object>> formFields = new ArrayList<>();
        Map<String, List<ExtensionElement>> extensionElements = userTask.getExtensionElements();
        List<ExtensionElement> formProperties = extensionElements.getOrDefault("formProperty", new ArrayList<>());
        for (ExtensionElement formProperty : formProperties) {
            Map<String, Object> field = new HashMap<>();
            field.put("key", formProperty.getAttributeValue(null, "id"));
            field.put("label", formProperty.getAttributeValue(null, "name"));
            String type = formProperty.getAttributeValue(null, "type");
            field.put("type", mapFormPropertyType(type != null ? type : "string"));

            if ("enum".equals(type)) {
                List<Map<String, String>> options = new ArrayList<>();
                List<ExtensionElement> values = formProperty.getChildElements().getOrDefault("value", new ArrayList<>());
                for (ExtensionElement value : values) {
                    Map<String, String> option = new HashMap<>();
                    option.put("label", value.getAttributeValue(null, "name"));
                    option.put("value", value.getAttributeValue(null, "id"));
                    options.add(option);
                }
                field.put("options", options);
            }

            formFields.add(field);
        }

        // 根據任務類型添加特定字段
        if (task.getTaskDefinitionKey().equals("ProcessTask")) {
            formFields.add(createActionField(List.of(
                    Map.of("label", "重新分配", "value", "reassign"),
                    Map.of("label", "完成", "value", "complete")
            )));
        } else if (task.getTaskDefinitionKey().equals("ConfirmTask")) {
            formFields.add(createActionField(List.of(
                    Map.of("label", "拒絕", "value", "reject"),
                    Map.of("label", "確認", "value", "confirm")
            )));
            formFields.add(createPriorityField());
        } else if (task.getTaskDefinitionKey().equals("ReviewTask")) {
            formFields.add(createActionField(List.of(
                    Map.of("label", "拒絕", "value", "reject"),
                    Map.of("label", "通過", "value", "approve")
            )));
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
            taskEntity.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
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

    private Map<String, Object> createActionField(List<Map<String, String>> options) {
        Map<String, Object> field = new HashMap<>();
        field.put("key", "action");
        field.put("label", "操作");
        field.put("type", "select");
        field.put("options", options);
        return field;
    }

    private Map<String, Object> createPriorityField() {
        Map<String, Object> field = new HashMap<>();
        field.put("key", "priority");
        field.put("label", "優先級");
        field.put("type", "select");
        field.put("options", List.of(
                Map.of("label", "高", "value", "high"),
                Map.of("label", "一般", "value", "normal")
        ));
        return field;
    }

    private String mapFormPropertyType(String activitiType) {
        switch (activitiType) {
            case "string":
                return "text";
            case "enum":
                return "select";
            default:
                return "text";
        }
    }
}