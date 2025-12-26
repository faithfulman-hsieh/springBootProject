package com.taskmanager.process.service;

import com.taskmanager.process.model.ProcessDef;
import com.taskmanager.process.model.ProcessIns;
import com.taskmanager.process.repository.ProcessDefRepository;
import com.taskmanager.process.repository.ProcessInsRepository;
import org.activiti.bpmn.model.*;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance; // ★★★ 記得引入這個
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

@Service
public class ProcessService {

    private final ProcessDefRepository definitionRepository;
    private final ProcessInsRepository instanceRepository;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ProcessService(ProcessDefRepository definitionRepository, ProcessInsRepository instanceRepository,
                          RepositoryService repositoryService, RuntimeService runtimeService, TaskService taskService,
                          HistoryService historyService) {
        this.definitionRepository = definitionRepository;
        this.instanceRepository = instanceRepository;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
    }

    public List<ProcessDef> getAllDefinitions() {
        try {
            return definitionRepository.findAll();
        } catch (Exception e) {
            throw new IllegalStateException("獲取流程定義失敗：" + e.getMessage(), e);
        }
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
            if (bpmnContent.matches("(?i).*<?\\s*[xX][mM][lL].*")) {
                throw new IllegalArgumentException("BPMN 文件格式錯誤：不允許符合 '[xX][mM][lL]' 的處理指示目標");
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
            processDef.setDeploymentTime(LocalDateTime.now().format(FORMATTER));
            processDef.setProcessDefinitionId(activitiDef.getId());

            return definitionRepository.save(processDef);
        } catch (IllegalArgumentException e) {
            throw e;
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
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("狀態切換失敗：" + e.getMessage(), e);
        }
    }

    public ProcessIns startProcess(String processDefinitionId, Map<String, Object> variables) {
        try {
            String currentUserId = "user";
            try {
                org.springframework.security.core.Authentication auth =
                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && !"anonymousUser".equals(auth.getName())) {
                    currentUserId = auth.getName();
                }
            } catch (Exception e) {
                // 忽略 Context 錯誤，使用預設值
            }

            ProcessInstance instance = runtimeService.startProcessInstanceById(processDefinitionId, variables);

            Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

            if (task != null && task.getAssignee() == null) {
                taskService.setAssignee(task.getId(), currentUserId);
                task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
            }

            ProcessIns processIns = new ProcessIns();
            processIns.setId(instance.getId());
            processIns.setName(instance.getProcessDefinitionName());
            processIns.setStatus("running");
            processIns.setProcessDefinitionId(processDefinitionId);
            processIns.setStartTime(LocalDateTime.now().format(FORMATTER));
            processIns.setCurrentTask(task != null ? task.getName() : null);
            processIns.setAssignee(task != null ? task.getAssignee() : null);

            return instanceRepository.save(processIns);
        } catch (Exception e) {
            throw new IllegalStateException("流程啟動失敗：" + e.getMessage(), e);
        }
    }

    public List<ProcessIns> getAllInstances() {
        try {
            List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().list();
            List<ProcessIns> processInsList = new ArrayList<>();
            for (ProcessInstance instance : instances) {
                Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

                ProcessIns processIns = new ProcessIns();
                processIns.setId(instance.getId());
                processIns.setName(instance.getProcessDefinitionName());
                processIns.setStatus("running");
                processIns.setProcessDefinitionId(instance.getProcessDefinitionId());
                processIns.setStartTime(LocalDateTime.ofInstant(instance.getStartTime().toInstant(),
                        java.time.ZoneId.systemDefault()).format(FORMATTER));
                processIns.setCurrentTask(task != null ? task.getName() : "Completed");
                processIns.setAssignee(task != null ? task.getAssignee() : null);

                processInsList.add(processIns);
                instanceRepository.save(processIns);
            }

            if (processInsList.isEmpty()) {
                return new ArrayList<>();
            }
            return processInsList;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("獲取流程實例失敗：" + e.getMessage(), e);
        }
    }

    // ★★★ 修正方法：支援查詢已結束的流程圖，並停在結束節點 ★★★
    public Map<String, Object> getProcessInstanceDiagram(String instanceId) {
        try {
            String processDefinitionId;
            String currentTaskKey = null;

            // 1. 先嘗試從 RuntimeService 查詢 (正在運行中)
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(instanceId)
                    .singleResult();

            if (instance != null) {
                processDefinitionId = instance.getProcessDefinitionId();
                Task task = taskService.createTaskQuery().processInstanceId(instanceId).singleResult();
                currentTaskKey = (task != null) ? task.getTaskDefinitionKey() : null;
            } else {
                // 2. Runtime 查不到，嘗試從 HistoryService 查詢 (已結束)
                HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(instanceId)
                        .singleResult();

                if (historicInstance == null) {
                    throw new IllegalArgumentException("流程實例不存在(包含歷史紀錄)：" + instanceId);
                }
                processDefinitionId = historicInstance.getProcessDefinitionId();

                // ★★★ 關鍵：找出這個流程最後走到的「結束節點 (End Event)」 ★★★
                List<HistoricActivityInstance> endActivities = historyService.createHistoricActivityInstanceQuery()
                        .processInstanceId(instanceId)
                        .activityType("endEvent") // 只抓取結束節點
                        .finished()
                        .orderByHistoricActivityInstanceEndTime().desc()
                        .list();

                if (!endActivities.isEmpty()) {
                    // 取最後一個觸發的結束節點 (處理流程中可能有多個結束點的情況)
                    currentTaskKey = endActivities.get(0).getActivityId();
                }
            }

            // 3. 讀取 BPMN XML
            String bpmnXml;
            try (InputStream inputStream = repositoryService.getProcessModel(processDefinitionId);
                 Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                bpmnXml = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            }

            Map<String, Object> response = new HashMap<>();
            response.put("bpmnXml", bpmnXml);
            response.put("currentTask", currentTaskKey);
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
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
            if (bpmnXml.isEmpty()) {
                throw new IllegalArgumentException("流程定義不存在或無法獲取 BPMN 圖：" + processDefinitionId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("bpmnXml", bpmnXml);
            response.put("currentTask", null);
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("獲取流程定義圖失敗：" + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getProcessFormFields(String processDefinitionId) {
        try {
            org.activiti.engine.repository.ProcessDefinition processDefinition = repositoryService
                    .createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();

            if (processDefinition == null) {
                throw new IllegalArgumentException("流程定義不存在：" + processDefinitionId);
            }

            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            org.activiti.bpmn.model.Process process = bpmnModel.getProcesses().get(0);

            StartEvent startEvent = process.getFlowElements().stream()
                    .filter(element -> element instanceof StartEvent)
                    .map(element -> (StartEvent) element)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("流程中未找到開始事件"));

            return convertFormProperties(startEvent.getFormProperties());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("獲取表單字段失敗：" + e.getMessage(), e);
        }
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
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (instance == null) {
                throw new IllegalArgumentException("流程實例不存在：" + processInstanceId);
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
            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
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

    public void jumpToNode(String processInstanceId, String targetNode) {
        try {
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (instance == null) {
                throw new IllegalArgumentException("流程實例不存在：" + processInstanceId);
            }

            Task currentTask = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (currentTask == null) {
                throw new IllegalArgumentException("當前流程實例無活動任務：" + processInstanceId);
            }

            BpmnModel bpmnModel = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
            org.activiti.bpmn.model.Process process = bpmnModel.getProcesses().get(0);
            process.getFlowElements().stream()
                    .filter(element -> element.getId().equals(targetNode))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("目標節點不存在：" + targetNode));

            Map<String, Object> variables = new HashMap<>();
            variables.put("targetActivityId", targetNode);

            switch (targetNode) {
                case "ProcessTask":
                    variables.put("action", "reassign");
                    break;
                case "ConfirmTask":
                    variables.put("action", "complete");
                    break;
                case "ReviewTask":
                    variables.put("action", "confirm");
                    variables.put("priority", "high");
                    break;
                case "AutoAssignTask":
                    variables.put("action", "reassign");
                    break;
                default:
                    break;
            }

            taskService.complete(currentTask.getId(), variables);

            Task newTask = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            ProcessIns processIns = instanceRepository.findById(processInstanceId)
                    .orElseThrow(() -> new IllegalArgumentException("流程實例不存在：" + processInstanceId));
            processIns.setCurrentTask(newTask != null ? newTask.getName() : "Completed");
            processIns.setAssignee(newTask != null ? newTask.getAssignee() : null);
            instanceRepository.save(processIns);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("節點跳轉失敗：" + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> convertFormProperties(List<FormProperty> formProperties) {
        List<Map<String, Object>> formFields = new ArrayList<>();

        for (FormProperty prop : formProperties) {
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
            }
            formFields.add(field);
        }
        return formFields;
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