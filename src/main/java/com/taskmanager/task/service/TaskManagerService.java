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
import org.springframework.security.core.GrantedAuthority; // ★★★ 新增 Import ★★★
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*; // 確保有 import java.util.*
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

    private String getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                return auth.getName();
            }
        } catch (Exception e) {
            // ignore
        }
        return "user";
    }

    // ★★★ 新增：取得目前使用者的角色清單 (即 Activiti 的 Groups) ★★★
    private List<String> getUserRoles() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                return auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // ignore
        }
        return Collections.emptyList();
    }

    public List<TaskDto> getMyTasks() {
        String assignee = getCurrentUserId();

        List<Task> activitiTasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime().desc()
                .list();

        return convertTasks(activitiTasks, null);
    }

    // ★★★ 修改：手動處理 User 和 Group 的聯集查詢 ★★★
    public List<TaskDto> getGroupTasks() {
        String userId = getCurrentUserId();
        List<String> groups = getUserRoles();

        // 使用 Set 來避免重複 (雖然 Activiti 任務 ID 是唯一的，但分開查詢可能會重疊)
        Map<String, Task> taskMap = new HashMap<>();

        // 1. 查詢「我是候選人」的任務 (Direct Candidate User)
        List<Task> userTasks = taskService.createTaskQuery()
                .taskCandidateUser(userId)
                .taskUnassigned()
                .list();
        for (Task t : userTasks) {
            taskMap.put(t.getId(), t);
        }

        // 2. 查詢「我的群組是候選群組」的任務 (Candidate Group)
        // 這是解決 "No UserGroupManager" 警告的關鍵：我們明確告訴引擎要查哪些群組
        if (!groups.isEmpty()) {
            List<Task> groupTasks = taskService.createTaskQuery()
                    .taskCandidateGroupIn(groups)
                    .taskUnassigned()
                    .list();
            for (Task t : groupTasks) {
                taskMap.put(t.getId(), t);
            }
        }

        // 3. 轉為 List 並排序
        List<Task> finalTasks = new ArrayList<>(taskMap.values());
        finalTasks.sort((t1, t2) -> t2.getCreateTime().compareTo(t1.getCreateTime())); // 降序

        return convertTasks(finalTasks, "待認領");
    }

    public void claimTask(String taskId) {
        String userId = getCurrentUserId();
        taskService.claim(taskId, userId);
    }

    public void unclaimTask(String taskId) {
        taskService.unclaim(taskId);
    }

    public List<TaskDto> getHistoryTasks() {
        String assignee = getCurrentUserId();

        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(assignee)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();

        List<TaskDto> tasks = new ArrayList<>();
        for (HistoricTaskInstance ht : historicTasks) {
            String processName = getProcessName(ht.getProcessDefinitionId());
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

            tasks.add(new TaskDto(
                    ht.getId(),
                    ht.getName(),
                    processName,
                    ht.getAssignee(),
                    ht.getEndTime().toInstant().atZone(ZoneId.systemDefault()).format(DATE_FORMATTER),
                    ht.getProcessInstanceId(),
                    currentAssignee
            ));
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
            field.put("disabled", !prop.isWriteable());

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

    private List<TaskDto> convertTasks(List<Task> tasks, String defaultAssignee) {
        List<TaskDto> result = new ArrayList<>();
        for (Task t : tasks) {
            String processName = getProcessName(t.getProcessDefinitionId());
            String assignee = t.getAssignee() != null ? t.getAssignee() : defaultAssignee;
            result.add(new TaskDto(
                    t.getId(),
                    t.getName(),
                    processName,
                    assignee,
                    t.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).format(DATE_FORMATTER),
                    t.getProcessInstanceId(),
                    null
            ));
        }
        return result;
    }

    private String getProcessName(String processDefinitionId) {
        try {
            return repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult()
                    .getName();
        } catch (Exception e) {
            return "Unknown Process";
        }
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