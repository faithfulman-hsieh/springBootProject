package com.taskmanager.task.service;

import com.taskmanager.task.dto.TaskDto;
import org.activiti.bpmn.model.*;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaskManagerService {

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TaskManagerService(TaskService taskService,
                              RuntimeService runtimeService,
                              RepositoryService repositoryService,
                              HistoryService historyService) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.historyService = historyService;
    }

    public List<TaskDto> getMyTasks() {
        String assignee = "user";
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                assignee = auth.getName();
            }
        } catch (Exception e) {
            // ignore
        }

        // 回歸單純的 Task 查詢，不撈變數
        List<Task> activitiTasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime().desc()
                .list();

        List<TaskDto> tasks = new ArrayList<>();
        for (Task t : activitiTasks) {
            String processName = "Unknown Process";
            try {
                processName = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(t.getProcessDefinitionId())
                        .singleResult()
                        .getName();
            } catch (Exception e) {
                // ignore
            }

            TaskDto taskDto = new TaskDto(
                    t.getId(),
                    t.getName(),
                    processName,
                    t.getAssignee(),
                    t.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).format(DATE_FORMATTER),
                    t.getProcessInstanceId(),
                    null
            );
            tasks.add(taskDto);
        }
        return tasks;
    }

    public List<TaskDto> getHistoryTasks() {
        String assignee = "user";
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                assignee = auth.getName();
            }
        } catch (Exception e) {
            // ignore
        }

        // 回歸單純的歷史查詢
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(assignee)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();

        List<TaskDto> tasks = new ArrayList<>();
        for (HistoricTaskInstance ht : historicTasks) {
            String processName = "Unknown Process";
            try {
                processName = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(ht.getProcessDefinitionId())
                        .singleResult()
                        .getName();
            } catch (Exception e) {
                // ignore
            }

            String currentAssignee = "流程已結束";
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(ht.getProcessInstanceId())
                    .singleResult();

            if (pi != null) {
                List<Task> activeTasks = taskService.createTaskQuery()
                        .processInstanceId(ht.getProcessInstanceId())
                        .list();

                if (!activeTasks.isEmpty()) {
                    currentAssignee = activeTasks.stream()
                            .map(t -> t.getAssignee() == null ? "待認領" : t.getAssignee())
                            .distinct()
                            .collect(Collectors.joining(", "));
                }
            }

            TaskDto taskDto = new TaskDto(
                    ht.getId(),
                    ht.getName(),
                    processName,
                    ht.getAssignee(),
                    ht.getEndTime().toInstant().atZone(ZoneId.systemDefault()).format(DATE_FORMATTER),
                    ht.getProcessInstanceId(),
                    currentAssignee
            );
            tasks.add(taskDto);
        }
        return tasks;
    }

    public List<Map<String, Object>> getTaskForm(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任務不存在：" + taskId);
        }

        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        FlowElement flowElement = bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());

        if (!(flowElement instanceof UserTask)) {
            return new ArrayList<>();
        }

        // ★★★ 關鍵：取得變數，這是對話框能顯示資料的來源 ★★★
        Map<String, Object> variables = taskService.getVariables(taskId);

        UserTask userTask = (UserTask) flowElement;
        List<Map<String, Object>> formFields = new ArrayList<>();

        for (FormProperty prop : userTask.getFormProperties()) {
            Map<String, Object> field = new HashMap<>();
            field.put("key", prop.getId());
            field.put("label", prop.getName() != null ? prop.getName() : prop.getId());

            String type = prop.getType() != null ? prop.getType() : "string";
            field.put("type", mapFormPropertyType(type));
            field.put("required", prop.isRequired());

            // 是否唯讀
            field.put("disabled", !prop.isWriteable());

            // ★★★ 關鍵：如果有對應的變數值，填入 value 屬性 ★★★
            if (variables.containsKey(prop.getId())) {
                field.put("value", variables.get(prop.getId()));
            }

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
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任務不存在：" + taskId);
        }
        taskService.complete(taskId, formData);
    }

    public void reassignTask(String taskId, String assignee) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任務不存在：" + taskId);
        }
        taskService.setAssignee(taskId, assignee);
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