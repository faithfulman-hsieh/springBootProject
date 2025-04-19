package com.taskmanager.process.service;

import com.taskmanager.process.model.ProcessDef;
import com.taskmanager.process.model.ProcessIns;
import com.taskmanager.process.repository.ProcessDefRepository;
import com.taskmanager.process.repository.ProcessInsRepository;
import org.activiti.bpmn.model.*;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.Execution;
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
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ProcessService(ProcessDefRepository definitionRepository, ProcessInsRepository instanceRepository,
                          RepositoryService repositoryService, RuntimeService runtimeService, TaskService taskService) {
        this.definitionRepository = definitionRepository;
        this.instanceRepository = instanceRepository;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    // ... (其他方法保持不變：getAllDefinitions, deployProcess, toggleProcessStatus, startProcess, getAllInstances,
    // getProcessInstanceDiagram, getProcessDefinitionDiagram, getProcessFormFields, getUsers, getFlowNodes,
    // reassignTask, extractFormFields, mapFormPropertyType)

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
            ProcessInstance instance = runtimeService.startProcessInstanceById(processDefinitionId, variables);
            Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

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
                throw new IllegalArgumentException("無運行中的流程實例");
            }
            return processInsList;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("獲取流程實例失敗：" + e.getMessage(), e);
        }
    }

    public Map<String, Object> getProcessInstanceDiagram(String instanceId) {
        try {
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(instanceId)
                    .singleResult();
            if (instance == null) {
                throw new IllegalArgumentException("流程實例不存在：" + instanceId);
            }

            Task task = taskService.createTaskQuery().processInstanceId(instanceId).singleResult();
            String bpmnXml;
            try (InputStream inputStream = repositoryService.getProcessModel(instance.getProcessDefinitionId());
                 Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                bpmnXml = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            }

            Map<String, Object> response = new HashMap<>();
            response.put("bpmnXml", bpmnXml);
            response.put("currentTask", task != null ? task.getTaskDefinitionKey() : null);
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

            return extractFormFields(startEvent.getExtensionElements());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("獲取表單字段失敗：" + e.getMessage(), e);
        }
    }

    public List<Map<String, String>> getUsers() {
        try {
            // 這裡簡化為硬編碼，實際應從用戶管理系統獲取
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
            FlowElement targetElement = process.getFlowElements().stream()
                    .filter(element -> element.getId().equals(targetNode))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("目標節點不存在：" + targetNode));

            // 根據目標節點設置必要的變量以滿足閘道條件
            Map<String, Object> variables = new HashMap<>();
            variables.put("targetActivityId", targetNode);

            // 動態設置 action 和 priority 變量以引導流程
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
                    // AutoAssignTask 是服務任務，可能需要特殊處理
                    variables.put("action", "reassign"); // 假設回退到初始分配
                    break;
                default:
                    throw new IllegalArgumentException("不支援跳轉到節點：" + targetNode);
            }

            // 完成當前任務
            taskService.complete(currentTask.getId(), variables);

            // 更新流程實例狀態
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

    private List<Map<String, Object>> extractFormFields(Map<String, List<ExtensionElement>> extensionElements) {
        List<Map<String, Object>> formFields = new ArrayList<>();

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

        return formFields;
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