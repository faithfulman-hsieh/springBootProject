package com.taskmanager.todo.service;

import com.taskmanager.todo.dto.TodoRequest;
import com.taskmanager.todo.model.Todo;
import com.taskmanager.todo.repository.TodoRepository;
import com.taskmanager.workflow.service.WorkflowService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TodoService {

    private final TodoRepository todoRepository;
    private final WorkflowService workflowService;

    public TodoService(TodoRepository todoRepository, WorkflowService workflowService) {
        this.todoRepository = todoRepository;
        this.workflowService = workflowService;
    }

    // 建立 Todo 並觸發 Workflow
    public Todo createTodo(TodoRequest request) {
        Todo todo = new Todo(request.getTitle(), request.getDescription(), request.getAssignee());

        // 觸發 Workflow
        String processInstanceId = workflowService.startProcess(request.getAssignee());
        todo.setProcessInstanceId(processInstanceId);

        return todoRepository.save(todo);
    }

    // 取得某個 Todo 的狀態
    public String getTodoStatus(Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("Todo not found"));

        // 查詢該使用者的待辦工作流
        List<String> tasks = workflowService.getUserTasks(todo.getAssignee());
        return tasks.isEmpty() ? "COMPLETED" : "IN_PROGRESS";
    }

    // 完成 Todo 時，同時完成對應的 Workflow
    public void completeTodo(Long todoId, String action, String priority) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("Todo not found"));

        // 透過 workflowService 完成對應的流程任務
        workflowService.completeTask(todo.getProcessInstanceId(), action, priority);

        // 更新 Todo 狀態
        todo.setStatus("COMPLETED");
        todoRepository.save(todo);
    }
}