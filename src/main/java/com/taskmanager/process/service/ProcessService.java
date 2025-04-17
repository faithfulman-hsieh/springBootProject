package com.taskmanager.process.service;

import com.taskmanager.process.model.ProcessDefinition;
import com.taskmanager.process.model.ProcessInstance;
import com.taskmanager.process.repository.ProcessDefinitionRepository;
import com.taskmanager.process.repository.ProcessInstanceRepository;
import org.activiti.bpmn.model.*;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstanceQuery;
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

@Service
public class ProcessService {

    private final ProcessDefinitionRepository definitionRepository;
    private final ProcessInstanceRepository instanceRepository;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ProcessService(ProcessDefinitionRepository definitionRepository, ProcessInstanceRepository instanceRepository,
                          RepositoryService repositoryService, RuntimeService runtimeService, TaskService taskService) {
        this.definitionRepository = definitionRepository;
        this.instanceRepository = instanceRepository;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    public List<ProcessDefinition> getAllDefinitions() {
        try {
            return definitionRepository.findAll();
        } catch (Exception e) {
            throw new IllegalStateException("獲取流程定義失敗：" + e.getMessage(), e);
        }
    }

    public ProcessDefinition deployProcess(String name, MultipartFile file) {
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

            ProcessDefinition processDefinition = new ProcessDefinition();
            processDefinition.setId(activitiDef.getId());
            processDefinition.setName(name);
            processDefinition.setVersion(activitiDef.getVersion() + ".0");
            processDefinition.setStatus("active");
            processDefinition.setDeploymentTime(LocalDateTime.now().format(FORMATTER));
            processDefinition.setProcessDefinitionId(activitiDef.getId());

            return definitionRepository.save(processDefinition);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("流程部署失敗：" + e.getMessage(), e);
        }
    }

    public ProcessDefinition toggleProcessStatus(String id) {
        try {
            ProcessDefinition processDefinition = definitionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("流程不存在：" + id));

            if ("active".equals(processDefinition.getStatus())) {
                repositoryService.suspendProcessDefinitionById(id);
                processDefinition.setStatus("suspended");
            } else {
                repositoryService.activateProcessDefinitionById(id);
                processDefinition.setStatus("active");
            }
            return definitionRepository.save(processDefinition);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("狀態切換失敗：" + e.getMessage(), e);
        }
    }

    public ProcessInstance startProcess(String processDefinitionId, Map<String, Object> variables) {
        try {
            org.activiti.engine.runtime.ProcessInstance instance = runtimeService.startProcessInstanceById(processDefinitionId, variables);
            org.activiti.engine.task.Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

            ProcessInstance processInstance = new ProcessInstance();
            processInstance.setId(instance.getId());
            processInstance.setName(instance.getProcessDefinitionName());
            processInstance.setStatus("running");
            processInstance.setProcessDefinitionId(processDefinitionId);
            processInstance.setStartTime(LocalDateTime.now().format(FORMATTER));
            processInstance.setCurrentTask(task != null ? task.getName() : null);
            processInstance.setAssignee(task != null ? task.getAssignee() : null);

            return instanceRepository.save(processInstance);
        } catch (Exception e) {
            throw new IllegalStateException("流程啟動失敗：" + e.getMessage(), e);
        }
    }

    public List<ProcessInstance> getAllInstances() {
        try {
            List<org.activiti.engine.runtime.ProcessInstance> instances = runtimeService.createProcessInstanceQuery().list();
            List<ProcessInstance> processInstances = new ArrayList<>();
            for (org.activiti.engine.runtime.ProcessInstance instance : instances) {
                org.activiti.engine.task.Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

                ProcessInstance processInstance = new ProcessInstance();
                processInstance.setId(instance.getId());
                processInstance.setName(instance.getProcessDefinitionName());
                processInstance.setStatus("running");
                processInstance.setProcessDefinitionId(instance.getProcessDefinitionId());
                processInstance.setStartTime(LocalDateTime.ofInstant(instance.getStartTime().toInstant(),
                        java.time.ZoneId.systemDefault()).format(FORMATTER));
                processInstance.setCurrentTask(task != null ? task.getName() : "Completed");
                processInstance.setAssignee(task != null ? task.getAssignee() : null);

                processInstances.add(processInstance);
                instanceRepository.save(processInstance);
            }

            if (processInstances.isEmpty()) {
                throw new IllegalArgumentException("無運行中的流程實例");
            }
            return processInstances;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("獲取流程實例失敗：" + e.getMessage(), e);
        }
    }

    public Map<String, Object> getProcessInstanceDiagram(String instanceId) {
        try {
            org.activiti.engine.runtime.ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(instanceId)
                    .singleResult();
            if (instance == null) {
                throw new IllegalArgumentException("流程實例不存在：" + instanceId);
            }

            org.activiti.engine.task.Task task = taskService.createTaskQuery().processInstanceId(instanceId).singleResult();
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
            org.activiti.bpmn.model.Process process = bpmnModel.getProcesses().get(0); // 假設只有一個流程

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