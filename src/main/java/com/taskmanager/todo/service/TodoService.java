package com.taskmanager.todo.service;

import com.taskmanager.todo.dto.TodoRequest;
import com.taskmanager.todo.model.Todo;
import com.taskmanager.todo.repository.TodoRepository;
import com.taskmanager.workflow.service.WorkflowService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Service
public class TodoService {

    private final TodoRepository todoRepository;
    private final WorkflowService workflowService;
    private final RepositoryService repositoryService;

    public TodoService(TodoRepository todoRepository, WorkflowService workflowService, RepositoryService repositoryService) {
        this.todoRepository = todoRepository;
        this.workflowService = workflowService;
        this.repositoryService = repositoryService;
    }

    public List<Todo> getAllTodos() {
        return todoRepository.findAll();
    }

    public Todo createTodo(TodoRequest request) {
        Todo todo = new Todo(request.getTitle(), request.getDescription(), request.getAssignee());
        String processInstanceId = workflowService.startProcess(request.getAssignee());
        todo.setProcessInstanceId(processInstanceId);

        // 獲取當前任務並儲存 processDefinitionId
        Task currentTask = workflowService.getCurrentTask(processInstanceId);
        if (currentTask != null) {
            todo.setProcessDefinitionId(currentTask.getProcessDefinitionId());
            todo.setStatus(currentTask.getName());
        } else {
            // 如果沒有當前任務（流程可能立即結束），需要從流程實例中獲取 processDefinitionId
            // 假設 WorkflowService 提供一個方法來獲取 processDefinitionId
            // BUG String processDefinitionId = workflowService.getProcessDefinitionId(processInstanceId);
            // if (processDefinitionId != null) {
                todo.setProcessDefinitionId(null);
            // }
            todo.setStatus("待辦結束");
        }

        return todoRepository.save(todo);
    }

    public String getTodoStatus(Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        return todo.getStatus();
    }

    public Map<String, String> getProcessDiagram(Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        String processInstanceId = todo.getProcessInstanceId();
        if (processInstanceId == null) {
            throw new RuntimeException("該 Todo 沒有對應的流程");
        }

        // 獲取當前任務（可能為 null）
        Task currentTask = workflowService.getCurrentTask(processInstanceId);

        // 使用儲存的 processDefinitionId 查詢流程定義
        String processDefinitionId = todo.getProcessDefinitionId();
        if (processDefinitionId == null) {
            throw new RuntimeException("該 Todo 沒有對應的流程定義 ID");
        }

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        if (processDefinition == null) {
            throw new RuntimeException("找不到對應的流程定義");
        }

        // 從 InputStream 讀取 BPMN XML 內容
        String bpmnXml;
        try (InputStream inputStream = repositoryService.getProcessModel(processDefinition.getId());
             Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            bpmnXml = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        } catch (Exception e) {
            throw new RuntimeException("無法讀取 BPMN XML: " + e.getMessage());
        }

        Map<String, String> result = new HashMap<>();
        result.put("bpmnXml", bpmnXml);
        result.put("currentTask", currentTask != null ? currentTask.getTaskDefinitionKey() : null);
        return result;
    }

    public void completeTodo(Long todoId, String action, String priority) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("Todo not found"));

        String processInstanceId = todo.getProcessInstanceId();
        if (processInstanceId == null) {
            throw new RuntimeException("該 Todo 沒有對應的流程");
        }

        Task currentTask = workflowService.getCurrentTask(processInstanceId);
        if (currentTask == null) {
            throw new RuntimeException("找不到對應的 Task，可能流程已結束");
        }

        workflowService.completeTask(currentTask.getId(), action, priority);

        currentTask = workflowService.getCurrentTask(processInstanceId);
        if (currentTask == null) {
            todo.setStatus("待辦結束");
        } else {
            todo.setStatus(currentTask.getName());
        }
        todoRepository.save(todo);
    }
}