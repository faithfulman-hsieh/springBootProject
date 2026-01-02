package com.taskmanager.process.service;

import com.taskmanager.process.dto.HistoryLog;
import com.taskmanager.process.model.ProcessDef;
import com.taskmanager.process.model.ProcessIns;
import com.taskmanager.process.repository.ProcessDefRepository;
import com.taskmanager.process.repository.ProcessInsRepository;
import com.taskmanager.task.dto.TaskFormRequest; // 補回 import (上次可能漏了)
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FormProperty;
import org.activiti.bpmn.model.FormValue;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.util.ProcessDefinitionUtil;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder; // ★★★ 新增 Import ★★★
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProcessService {

    private final ProcessDefRepository definitionRepository;
    private final ProcessInsRepository instanceRepository;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final ManagementService managementService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ProcessService(ProcessDefRepository definitionRepository, ProcessInsRepository instanceRepository,
                          RepositoryService repositoryService, RuntimeService runtimeService, TaskService taskService,
                          HistoryService historyService, ManagementService managementService) {
        this.definitionRepository = definitionRepository;
        this.instanceRepository = instanceRepository;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.managementService = managementService;
    }

    public List<ProcessDef> getAllDefinitions() {
        return definitionRepository.findAll();
    }

    public ProcessDef deployProcess(String name, MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("BPMN 文件不能為空");
            }
            if (!file.getOriginalFilename().endsWith(".bpmn") && !file.getOriginalFilename().endsWith(".xml")) {
                throw new IllegalArgumentException("檔案格式必須為 .bpmn 或 .xml");
            }

            String bpmnContent;
            try (InputStream inputStream = file.getInputStream();
                 Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                bpmnContent = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            }

            if (bpmnContent.contains("<?xml") && !bpmnContent.startsWith("<?xml")) {
                throw new IllegalArgumentException("BPMN 文件格式錯誤：XML 處理指令必須位於文件開頭");
            }

            Deployment deployment = repositoryService.createDeployment()
                    .name(name)
                    .addInputStream(file.getOriginalFilename(), file.getInputStream())
                    .deploy();

            org.activiti.engine.repository.ProcessDefinition activitiDef = repositoryService
                    .createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();

            if (activitiDef == null) {
                throw new IllegalStateException("無法獲取流程定義，可能部署失敗");
            }

            ProcessDef processDef = new ProcessDef();
            processDef.setId(activitiDef.getId());
            processDef.setName(name);
            processDef.setVersion(activitiDef.getVersion() + ".0");
            processDef.setStatus("active");
            processDef.setDeploymentTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            processDef.setProcessDefinitionId(activitiDef.getId());

            return definitionRepository.save(processDef);
        } catch (Exception e) {
            throw new IllegalStateException("流程部署失敗：" + e.getMessage(), e);
        }
    }

    public ProcessDef toggleProcessStatus(String id) {
        try {
            ProcessDef processDef = definitionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("流程不存在：" + id));

            if ("active".equals(processDef.getStatus())) {
                repositoryService.suspendProcessDefinitionById(id);
                processDef.setStatus("suspended");
            } else {
                repositoryService.activateProcessDefinitionById(id);
                processDef.setStatus("active");
            }
            return definitionRepository.save(processDef);
        } catch (Exception e) {
            throw new IllegalStateException("狀態切換失敗：" + e.getMessage(), e);
        }
    }

    public ProcessIns startProcess(String processDefinitionId, Map<String, Object> variables) {
        try {
            // ★★★ 修正：動態獲取當前登入的使用者 ID ★★★
            String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

            if (variables == null) {
                variables = new HashMap<>();
            }

            // 預設 Demo 變數 (可保留或移除)
            variables.putIfAbsent("managerAssignee", "admin");
            variables.putIfAbsent("financeAssignee", "admin");
            variables.putIfAbsent("itemName", "POC測試項目");
            variables.putIfAbsent("amount", 10000L);

            if (!variables.containsKey("nextAssignee")) {
                variables.put("nextAssignee", "admin");
            }

            // 設定 Activiti 的認證使用者，這樣 historyService 才能記錄 startedBy
            org.activiti.engine.impl.identity.Authentication.setAuthenticatedUserId(currentUserId);

            ProcessInstance instance = runtimeService.startProcessInstanceById(processDefinitionId, variables);

            // 清除，避免影響執行緒池中的其他請求
            org.activiti.engine.impl.identity.Authentication.setAuthenticatedUserId(null);

            List<Task> tasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();

            // 如果第一個任務沒有 Assignee，暫時指派給發起人 (視業務需求而定，這裡保留原邏輯但用動態 ID)
            for (Task t : tasks) {
                if (t.getAssignee() == null) {
                    taskService.setAssignee(t.getId(), currentUserId);
                }
            }
            tasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();

            ProcessIns processIns = new ProcessIns();
            processIns.setId(instance.getId());
            processIns.setName(instance.getProcessDefinitionName());
            processIns.setStatus("running");
            processIns.setProcessDefinitionId(processDefinitionId);
            processIns.setStartTime(LocalDateTime.now().format(FORMATTER));

            // ★★★ 建議：將發起人存入我們自己的表，方便後續查詢 (如果 ProcessIns 有這個欄位的話) ★★★
            // processIns.setStartUserId(currentUserId);

            if (tasks.isEmpty()) {
                processIns.setCurrentTask("Completed");
                processIns.setAssignee(null);
            } else {
                String taskNames = tasks.stream().map(Task::getName).collect(Collectors.joining(", "));
                String assignees = tasks.stream().map(t -> t.getAssignee() == null ? "待認領" : t.getAssignee()).collect(Collectors.joining(", "));
                processIns.setCurrentTask(taskNames);
                processIns.setAssignee(assignees);
            }

            return instanceRepository.save(processIns);
        } catch (Exception e) {
            throw new IllegalStateException("流程啟動失敗：" + e.getMessage(), e);
        }
    }

    public List<ProcessIns> getAllInstances() {
        try {
            List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                    .orderByProcessInstanceStartTime().desc()
                    .list();

            List<ProcessIns> processInsList = new ArrayList<>();
            for (HistoricProcessInstance hPi : instances) {
                ProcessIns ins = new ProcessIns();
                ins.setId(hPi.getId());
                ins.setName(hPi.getProcessDefinitionName());
                ins.setProcessDefinitionId(hPi.getProcessDefinitionId());

                if (hPi.getStartTime() != null) {
                    ins.setStartTime(LocalDateTime.ofInstant(hPi.getStartTime().toInstant(),
                            java.time.ZoneId.systemDefault()).format(FORMATTER));
                }

                if (hPi.getEndTime() != null) {
                    ins.setStatus("Completed");
                    ins.setCurrentTask("Completed");
                    ins.setAssignee("-");
                } else {
                    ins.setStatus("Running");
                    List<Task> tasks = taskService.createTaskQuery().processInstanceId(hPi.getId()).list();
                    if (!tasks.isEmpty()) {
                        String taskNames = tasks.stream().map(Task::getName).collect(Collectors.joining(", "));
                        String assignees = tasks.stream().map(t -> t.getAssignee() == null ? "待認領" : t.getAssignee())
                                .collect(Collectors.joining(", "));
                        ins.setCurrentTask(taskNames);
                        ins.setAssignee(assignees);
                    } else {
                        ins.setCurrentTask("系統處理中");
                    }
                }
                processInsList.add(ins);
            }
            return processInsList;
        } catch (Exception e) {
            throw new IllegalStateException("獲取流程實例失敗：" + e.getMessage(), e);
        }
    }

    public List<ProcessIns> getMyProcessInstances() {
        try {
            // ★★★ 修正：動態獲取當前登入的使用者 ID ★★★
            String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

            // 使用 startedBy(currentUserId) 查詢該使用者發起的流程
            List<HistoricProcessInstance> historicInstances = historyService.createHistoricProcessInstanceQuery()
                    .startedBy(currentUserId)
                    .orderByProcessInstanceStartTime().desc()
                    .list();

            List<ProcessIns> resultList = new ArrayList<>();

            for (HistoricProcessInstance hPi : historicInstances) {
                ProcessIns ins = new ProcessIns();
                ins.setId(hPi.getId());
                ins.setName(hPi.getProcessDefinitionName());
                ins.setProcessDefinitionId(hPi.getProcessDefinitionId());

                if (hPi.getStartTime() != null) {
                    ins.setStartTime(LocalDateTime.ofInstant(hPi.getStartTime().toInstant(),
                            java.time.ZoneId.systemDefault()).format(FORMATTER));
                }

                if (hPi.getEndTime() != null) {
                    ins.setStatus("Completed");
                    ins.setCurrentTask("Completed");
                    ins.setAssignee("-");
                } else {
                    ins.setStatus("Running");
                    List<Task> tasks = taskService.createTaskQuery().processInstanceId(hPi.getId()).list();
                    if (!tasks.isEmpty()) {
                        String taskNames = tasks.stream().map(Task::getName).collect(Collectors.joining(", "));
                        String assignees = tasks.stream().map(t -> t.getAssignee() == null ? "待認領" : t.getAssignee())
                                .collect(Collectors.joining(", "));
                        ins.setCurrentTask(taskNames);
                        ins.setAssignee(assignees);
                    } else {
                        ins.setCurrentTask("系統處理中");
                    }
                }
                resultList.add(ins);
            }
            return resultList;
        } catch (Exception e) {
            throw new IllegalStateException("獲取我的申請紀錄失敗：" + e.getMessage(), e);
        }
    }

    public Map<String, Object> getProcessInstanceDiagram(String instanceId) {
        try {
            String processDefinitionId;
            List<String> activeActivityIds = new ArrayList<>();

            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(instanceId)
                    .singleResult();

            if (instance != null) {
                processDefinitionId = instance.getProcessDefinitionId();
                List<Task> tasks = taskService.createTaskQuery().processInstanceId(instanceId).list();
                activeActivityIds = tasks.stream()
                        .map(Task::getTaskDefinitionKey)
                        .collect(Collectors.toList());
            } else {
                HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(instanceId)
                        .singleResult();

                if (historicInstance == null) {
                    throw new IllegalArgumentException("流程實例不存在：" + instanceId);
                }
                processDefinitionId = historicInstance.getProcessDefinitionId();

                List<HistoricActivityInstance> endActivities = historyService.createHistoricActivityInstanceQuery()
                        .processInstanceId(instanceId)
                        .activityType("endEvent")
                        .finished()
                        .orderByHistoricActivityInstanceEndTime().desc()
                        .list();

                if (!endActivities.isEmpty()) {
                    activeActivityIds.add(endActivities.get(0).getActivityId());
                }
            }

            String bpmnXml;
            try (InputStream inputStream = repositoryService.getProcessModel(processDefinitionId);
                 Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                bpmnXml = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            }

            Map<String, Object> response = new HashMap<>();
            response.put("bpmnXml", bpmnXml);
            response.put("currentTask", activeActivityIds);
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("獲取流程實例圖失敗：" + e.getMessage(), e);
        }
    }

    public Map<String, Object> getProcessDefinitionDiagram(String processDefinitionId) {
        try {
            String bpmnXml;
            try (InputStream inputStream = repositoryService.getProcessModel(processDefinitionId);
                 Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                bpmnXml = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            }
            Map<String, Object> response = new HashMap<>();
            response.put("bpmnXml", bpmnXml);
            response.put("currentTask", null);
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("獲取流程定義圖失敗：" + e.getMessage(), e);
        }
    }

    // ★★★ 核心功能：獲取指定節點的表單欄位 (用於跳關時預填) ★★★
    public List<Map<String, Object>> getNodeFormFields(String processInstanceId, String nodeId) {
        try {
            // 1. 取得流程實例
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (instance == null) {
                throw new IllegalArgumentException("流程實例不存在或已結束");
            }

            // 2. 讀取 BPMN 模型
            BpmnModel bpmnModel = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
            org.activiti.bpmn.model.Process process = bpmnModel.getProcesses().get(0);

            // 3. 找到目標節點
            org.activiti.bpmn.model.FlowElement flowElement = process.getFlowElement(nodeId);

            // 4. 解析表單屬性 (只針對 UserTask)
            if (flowElement instanceof UserTask) {
                UserTask userTask = (UserTask) flowElement;
                return convertFormProperties(userTask.getFormProperties(), Collections.emptySet());
            }

            return new ArrayList<>();

        } catch (Exception e) {
            throw new IllegalStateException("獲取節點表單失敗：" + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getProcessFormFields(String processDefinitionId) {
        try {
            org.activiti.engine.repository.ProcessDefinition processDefinition = repositoryService
                    .createProcessDefinitionQuery()
                    .deploymentId(processDefinitionId)
                    .singleResult();

            if (processDefinition == null) {
                processDefinition = repositoryService
                        .createProcessDefinitionQuery()
                        .processDefinitionId(processDefinitionId)
                        .singleResult();
            }

            if (processDefinition == null) {
                throw new IllegalArgumentException("流程定義不存在：" + processDefinitionId);
            }

            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            org.activiti.bpmn.model.Process process = bpmnModel.getProcesses().get(0);

            Set<String> multiInstanceCollections = new HashSet<>();
            process.getFlowElements().stream()
                    .filter(e -> e instanceof UserTask)
                    .map(e -> (UserTask) e)
                    .forEach(task -> {
                        if (task.getLoopCharacteristics() != null && task.getLoopCharacteristics().getInputDataItem() != null) {
                            multiInstanceCollections.add(task.getLoopCharacteristics().getInputDataItem());
                        }
                    });

            StartEvent startEvent = (StartEvent) process.getFlowElements().stream()
                    .filter(element -> element instanceof StartEvent)
                    .findFirst()
                    .orElse(null);

            if (startEvent != null) {
                return convertFormProperties(startEvent.getFormProperties(), multiInstanceCollections);
            }
            return new ArrayList<>();
        } catch (Exception e) {
            throw new IllegalStateException("獲取表單字段失敗：" + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> convertFormProperties(List<FormProperty> formProperties, Set<String> multiInstanceCollections) {
        List<Map<String, Object>> formFields = new ArrayList<>();

        for (FormProperty prop : formProperties) {
            Map<String, Object> field = new HashMap<>();
            field.put("key", prop.getId());
            field.put("label", prop.getName() != null ? prop.getName() : prop.getId());

            String type = prop.getType() != null ? prop.getType() : "string";

            if ("enum".equals(type) && multiInstanceCollections.contains(prop.getId())) {
                type = "checkbox-group";
            }

            field.put("type", "checkbox-group".equals(type) ? "checkbox-group" : mapFormPropertyType(type));

            field.put("required", prop.isRequired());
            field.put("disabled", !prop.isWriteable());

            if (prop.getDefaultExpression() != null) {
                field.put("value", prop.getDefaultExpression());
            }

            if ("enum".equals(prop.getType()) || "checkbox-group".equals(type)) {
                List<Map<String, String>> options = new ArrayList<>();
                for (FormValue val : prop.getFormValues()) {
                    Map<String, String> option = new HashMap<>();
                    option.put("label", val.getName());
                    option.put("value", val.getId());
                    options.add(option);
                }
                field.put("options", options);
            }
            formFields.add(field);
        }
        return formFields;
    }

    public List<Map<String, String>> getUsers() {
        try {
            List<Map<String, String>> users = new ArrayList<>();
            Map<String, String> user1 = new HashMap<>();
            user1.put("label", "張三");
            user1.put("value", "zhangsan");
            Map<String, String> user2 = new HashMap<>();
            user2.put("label", "李四");
            user2.put("value", "lisi");
            users.add(user1);
            users.add(user2);
            return users;
        } catch (Exception e) {
            throw new IllegalStateException("獲取用戶列表失敗：" + e.getMessage(), e);
        }
    }

    public List<Map<String, String>> getFlowNodes(String processInstanceId) {
        try {
            ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            if (instance == null) {
                throw new IllegalArgumentException("流程實例不存在或已結束：" + processInstanceId);
            }

            BpmnModel bpmnModel = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
            org.activiti.bpmn.model.Process process = bpmnModel.getProcesses().get(0);

            List<Map<String, String>> nodes = process.getFlowElements().stream()
                    .filter(element -> element instanceof UserTask || element instanceof ServiceTask)
                    .map(element -> {
                        Map<String, String> node = new HashMap<>();
                        node.put("id", element.getId());
                        node.put("name", element.getName() != null ? element.getName() : element.getId());
                        return node;
                    })
                    .collect(Collectors.toList());

            if (nodes.isEmpty()) {
                throw new IllegalArgumentException("流程中無可用節點：" + processInstanceId);
            }
            return nodes;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("獲取流程節點失敗：" + e.getMessage(), e);
        }
    }

    public void reassignTask(String processInstanceId, String newAssignee) {
        try {
            Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
            if (task == null) {
                throw new IllegalArgumentException("當前流程實例無活動任務：" + processInstanceId);
            }

            taskService.setAssignee(task.getId(), newAssignee);

            ProcessIns processIns = instanceRepository.findById(processInstanceId)
                    .orElseThrow(() -> new IllegalArgumentException("流程實例不存在：" + processInstanceId));
            processIns.setAssignee(newAssignee);
            instanceRepository.save(processIns);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("任務重新分配失敗：" + e.getMessage(), e);
        }
    }

    public void jumpToNode(String processInstanceId, String targetNodeId, Map<String, Object> variables) {
        try {
            if (variables != null && !variables.isEmpty()) {
                runtimeService.setVariables(processInstanceId, variables);
            }

            managementService.executeCommand(new JumpCmd(processInstanceId, targetNodeId));

            ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            List<Task> newTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
            ProcessIns processIns = instanceRepository.findById(processInstanceId).orElse(new ProcessIns());
            processIns.setId(processInstanceId);
            if (instance != null) {
                processIns.setName(instance.getProcessDefinitionName());
                processIns.setProcessDefinitionId(instance.getProcessDefinitionId());
                processIns.setStatus("Running");
            }

            if (!newTasks.isEmpty()) {
                String taskNames = newTasks.stream().map(Task::getName).collect(Collectors.joining(", "));
                String assignees = newTasks.stream().map(t -> t.getAssignee() == null ? "待認領" : t.getAssignee()).collect(Collectors.joining(", "));
                processIns.setCurrentTask(taskNames);
                processIns.setAssignee(assignees);
            }
            instanceRepository.save(processIns);

        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private static class JumpCmd implements Command<Void> {
        private final String processInstanceId;
        private final String targetNodeId;

        public JumpCmd(String processInstanceId, String targetNodeId) {
            this.processInstanceId = processInstanceId;
            this.targetNodeId = targetNodeId;
        }

        @Override
        public Void execute(CommandContext commandContext) {
            List<ExecutionEntity> executions = commandContext.getExecutionEntityManager().findChildExecutionsByProcessInstanceId(processInstanceId);
            List<ExecutionEntity> activeExecutions = new ArrayList<>();
            for (ExecutionEntity exec : executions) {
                if (exec.getCurrentActivityId() != null && exec.isActive()) {
                    activeExecutions.add(exec);
                }
            }

            if (activeExecutions.size() > 1) {
                throw new IllegalStateException("為防止流程死鎖，系統禁止在並行分支(Parallel Gateway)執行中進行跳關。");
            }

            ExecutionEntity execution = activeExecutions.isEmpty()
                    ? commandContext.getExecutionEntityManager().findByRootProcessInstanceId(processInstanceId)
                    : activeExecutions.get(0);

            if (execution == null || execution.isEnded()) {
                throw new IllegalStateException("找不到可用的執行實例，流程可能已結束或無效: " + processInstanceId);
            }

            List<TaskEntity> tasks = commandContext.getTaskEntityManager().findTasksByExecutionId(execution.getId());
            for (TaskEntity task : tasks) {
                commandContext.getTaskEntityManager().deleteTask(task, "Jump", false, true);
            }

            org.activiti.bpmn.model.Process process = ProcessDefinitionUtil.getProcess(execution.getProcessDefinitionId());
            org.activiti.bpmn.model.FlowElement targetElement = process.getFlowElement(targetNodeId);

            if (targetElement == null) {
                throw new IllegalArgumentException("目標節點不存在: " + targetNodeId);
            }

            execution.setCurrentFlowElement(targetElement);
            commandContext.getAgenda().planContinueProcessOperation(execution);
            return null;
        }
    }

    public Resource getProcessTemplate(String filename) {
        try {
            Resource resource = new ClassPathResource("processes/" + filename);
            if (!resource.exists()) {
                throw new FileNotFoundException("範本檔案不存在：" + filename);
            }
            return resource;
        } catch (Exception e) {
            throw new IllegalStateException("讀取範本失敗：" + e.getMessage(), e);
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

    public List<HistoryLog> getProcessHistory(String processInstanceId) {
        try {
            List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricActivityInstanceStartTime().asc()
                    .list();

            List<HistoricVariableInstance> historyVariables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list();

            Map<String, Object> globalVars = historyVariables.stream()
                    .collect(Collectors.toMap(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue, (v1, v2) -> v2));

            List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
            List<String> activeTaskIds = activeTasks.stream().map(Task::getId).collect(Collectors.toList());

            List<HistoryLog> historyLogs = new ArrayList<>();

            for (HistoricActivityInstance activity : activities) {
                if (!"startEvent".equals(activity.getActivityType()) &&
                        !"endEvent".equals(activity.getActivityType()) &&
                        !"userTask".equals(activity.getActivityType())) {
                    continue;
                }

                HistoryLog log = new HistoryLog();
                String name = activity.getActivityName();
                if (name == null || name.isEmpty()) {
                    if ("startEvent".equals(activity.getActivityType())) name = "流程發起";
                    else if ("endEvent".equals(activity.getActivityType())) name = "流程結束";
                    else name = "系統節點";
                }
                log.setActivityName(name);
                log.setActivityType(activity.getActivityType());
                log.setAssignee(activity.getAssignee());

                if (activity.getEndTime() != null) {
                    log.setEndTime(LocalDateTime.ofInstant(activity.getEndTime().toInstant(), java.time.ZoneId.systemDefault()).format(FORMATTER));

                    if (activity.getDeleteReason() != null) {
                        log.setStatus("Skipped");
                    } else {
                        log.setStatus("Completed");
                    }
                    log.setDuration(formatDuration(activity.getDurationInMillis()));
                } else {
                    if ("userTask".equals(activity.getActivityType())) {
                        if (activeTaskIds.contains(activity.getTaskId())) {
                            log.setStatus("Running");
                        } else {
                            log.setStatus("Skipped");
                            log.setEndTime(LocalDateTime.now().format(FORMATTER));
                        }
                    } else {
                        log.setStatus("Running");
                    }
                    log.setDuration("-");
                }

                if ("userTask".equals(activity.getActivityType())) {
                    log.setVariables(globalVars);
                }

                historyLogs.add(log);
            }
            return historyLogs;
        } catch (Exception e) {
            throw new IllegalStateException("獲取歷史紀錄失敗：" + e.getMessage(), e);
        }
    }

    private String formatDuration(Long durationInMillis) {
        if (durationInMillis == null) return "";
        Duration duration = Duration.ofMillis(durationInMillis);
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天 ");
        if (hours > 0) sb.append(hours).append("時 ");
        sb.append(minutes).append("分");
        return sb.toString().isEmpty() ? "1分內" : sb.toString();
    }
}