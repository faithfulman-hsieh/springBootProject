package com.taskmanager.todo.service;

import com.taskmanager.todo.dto.TodoRequest;
import com.taskmanager.todo.model.Todo;
import com.taskmanager.todo.repository.TodoRepository;
import com.taskmanager.workflow.service.WorkflowService;
import org.activiti.engine.task.Task;
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

    // 取得所有 Todo
    public List<Todo> getAllTodos() {
        return todoRepository.findAll();
    }

    // 建立 Todo 並觸發 Workflow
    public Todo createTodo(TodoRequest request) {
        Todo todo = new Todo(request.getTitle(), request.getDescription(), request.getAssignee());
        // 觸發 Workflow
        String processInstanceId = workflowService.startProcess(request.getAssignee());
        todo.setProcessInstanceId(processInstanceId);
        Task currentTask = workflowService.getCurrentTask(todo.getProcessInstanceId());
        if (currentTask == null) {
            todo.setStatus("待辦結束");// 流程結束
        }else{
            todo.setStatus(currentTask.getName());
        }
        return todoRepository.save(todo);
    }

    // 取得某個 Todo 的狀態
    public String getTodoStatus(Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("Todo not found"));
        return todo.getStatus();
    }

    // 完成 Todo 時，同時完成對應的 Workflow
    public void completeTodo(Long todoId, String action, String priority) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("Todo not found"));

        String processInstanceId = todo.getProcessInstanceId();
        if (processInstanceId == null) {
            throw new RuntimeException("該 Todo 沒有對應的流程");
        }

        // 取得當前 Task
        Task currentTask = workflowService.getCurrentTask(processInstanceId);
        if (currentTask == null) {
            throw new RuntimeException("找不到對應的 Task，可能流程已結束");
        }

        // 使用 Task ID 完成對應的 Workflow 任務
        workflowService.completeTask(currentTask.getId(), action, priority);

        // RE更新 Todo 狀態
        currentTask = workflowService.getCurrentTask(processInstanceId);
        if (currentTask == null) {
           //"找不到對應的 Task，可能流程已結束
            todo.setStatus("待辦結束");
        }else{
            todo.setStatus(currentTask.getName());
        }
        todoRepository.save(todo);
    }
}