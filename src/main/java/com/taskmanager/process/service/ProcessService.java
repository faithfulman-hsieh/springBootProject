package com.taskmanager.process.service;

import com.taskmanager.process.model.ProcessDef;
import com.taskmanager.process.model.ProcessIns;
import com.taskmanager.process.repository.ProcessDefRepository;
import com.taskmanager.process.repository.ProcessInsRepository;
// ★★★ 確保引用 BPMN Model 的類別 ★★★
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FormProperty;
import org.activiti.bpmn.model.FormValue;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
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
            processDef.setDeploymentTime(LocalDateTime.now().format(FORMATTER));
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
            String currentUserId = "user";

            // 自動注入預設變數
            if (variables == null) {
                variables = new HashMap<>();
            }
            variables.putIfAbsent("managerAssignee", "admin");
            variables.putIfAbsent("financeAssignee", "admin");
            variables.putIfAbsent("itemName", "POC測試項目");
            variables.putIfAbsent("amount", 10000L);

            ProcessInstance instance = runtimeService.startProcessInstanceById(processDefinitionId, variables);

            List<Task> tasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();

            // 若任務無指派人，嘗試自動指派
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
            List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().list();
            List<ProcessIns> processInsList = new ArrayList<>();
            for (ProcessInstance instance : instances) {
                List<Task> tasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();

                ProcessIns processIns = new ProcessIns();
                processIns.setId(instance.getId());
                processIns.setName(instance.getProcessDefinitionName());
                processIns.setStatus("running");
                processIns.setProcessDefinitionId(instance.getProcessDefinitionId());
                processIns.setStartTime(LocalDateTime.ofInstant(instance.getStartTime().toInstant(),
                        java.time.ZoneId.systemDefault()).format(FORMATTER));

                if (tasks.isEmpty()) {
                    processIns.setCurrentTask("Completed");
                    processIns.setAssignee(null);
                } else {
                    String taskNames = tasks.stream().map(Task::getName).collect(Collectors.joining(", "));
                    String assignees = tasks.stream().map(t -> t.getAssignee() == null ? "待認領" : t.getAssignee()).collect(Collectors.joining(", "));
                    processIns.setCurrentTask(taskNames);
                    processIns.setAssignee(assignees);
                }

                processInsList.add(processIns);
                instanceRepository.save(processIns);
            }
            return processInsList;
        } catch (Exception e) {
            throw new IllegalStateException("獲取流程實例失敗：" + e.getMessage(), e);
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

            StartEvent startEvent = (StartEvent) process.getFlowElements().stream()
                    .filter(element -> element instanceof StartEvent)
                    .findFirst()
                    .orElse(null);

            if (startEvent != null) {
                return convertFormProperties(startEvent.getFormProperties());
            }
            return new ArrayList<>();
        } catch (Exception e) {
            throw new IllegalStateException("獲取表單字段失敗：" + e.getMessage(), e);
        }
    }

    // 參數是 org.activiti.bpmn.model.FormProperty
    private List<Map<String, Object>> convertFormProperties(List<FormProperty> formProperties) {
        List<Map<String, Object>> formFields = new ArrayList<>();

        for (FormProperty prop : formProperties) {
            Map<String, Object> field = new HashMap<>();
            field.put("key", prop.getId());
            field.put("label", prop.getName() != null ? prop.getName() : prop.getId());

            // ★★★ 修正：prop.getType() 回傳的就是 String，不需要再 .getName() ★★★
            String type = prop.getType() != null ? prop.getType() : "string";
            field.put("type", mapFormPropertyType(type));

            field.put("required", prop.isRequired());
            field.put("disabled", !prop.isWriteable());

            // ★★★ 修正：使用 getDefaultExpression() 取得預設值 ★★★
            if (prop.getDefaultExpression() != null) {
                field.put("value", prop.getDefaultExpression());
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
            }
            formFields.add(field);
        }
        return formFields;
    }

    // ... (getUsers, getFlowNodes, reassignTask, jumpToNode, getProcessTemplate, mapFormPropertyType 保持不變) ...
    public List<Map<String, String>> getUsers() {
        try {
            List<Map<String, String>> users = new ArrayList<>();
            Map<String, String> user1 = new HashMap<>(); user1.put("label", "張三"); user1.put("value", "zhangsan");
            Map<String, String> user2 = new HashMap<>(); user2.put("label", "李四"); user2.put("value", "lisi");
            users.add(user1); users.add(user2);
            return users;
        } catch (Exception e) { throw new IllegalStateException(e.getMessage()); }
    }

    public List<Map<String, String>> getFlowNodes(String processInstanceId) {
        try {
            ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            if (instance == null) throw new IllegalArgumentException("流程實例不存在");
            BpmnModel bpmnModel = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
            org.activiti.bpmn.model.Process process = bpmnModel.getProcesses().get(0);
            return process.getFlowElements().stream()
                    .filter(e -> e instanceof UserTask || e instanceof ServiceTask)
                    .map(e -> { Map<String, String> n = new HashMap<>(); n.put("id", e.getId()); n.put("name", e.getName()); return n; })
                    .collect(Collectors.toList());
        } catch (Exception e) { throw new IllegalStateException(e.getMessage()); }
    }

    public void reassignTask(String processInstanceId, String newAssignee) {
        try {
            Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
            if (task == null) throw new IllegalArgumentException("無活動任務");
            taskService.setAssignee(task.getId(), newAssignee);
            ProcessIns ins = instanceRepository.findById(processInstanceId).orElseThrow();
            ins.setAssignee(newAssignee);
            instanceRepository.save(ins);
        } catch (Exception e) { throw new IllegalStateException(e.getMessage()); }
    }

    public void jumpToNode(String processInstanceId, String targetNode) {
        try {
            ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            if (instance == null) throw new IllegalArgumentException("流程實例不存在");
            Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
            Map<String, Object> vars = new HashMap<>();
            vars.put("targetActivityId", targetNode);
            taskService.complete(task.getId(), vars);
            Task newTask = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
            ProcessIns ins = instanceRepository.findById(processInstanceId).orElseThrow();
            ins.setCurrentTask(newTask != null ? newTask.getName() : "Completed");
            instanceRepository.save(ins);
        } catch (Exception e) { throw new IllegalStateException(e.getMessage()); }
    }

    public Resource getProcessTemplate(String filename) {
        try {
            Resource resource = new ClassPathResource("processes/" + filename);
            if (!resource.exists()) throw new FileNotFoundException("檔案不存在");
            return resource;
        } catch (Exception e) { throw new IllegalStateException(e.getMessage()); }
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