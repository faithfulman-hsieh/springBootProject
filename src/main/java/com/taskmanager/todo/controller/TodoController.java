package com.taskmanager.todo.controller;

import com.taskmanager.todo.model.Todo;
import com.taskmanager.todo.service.TodoService;
import com.taskmanager.todo.dto.TodoRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public ResponseEntity<List<Todo>> getAllTodos() {
        List<Todo> todos = todoService.getAllTodos();
        return ResponseEntity.ok(todos);
    }

    @PostMapping("/addTodo")
    public ResponseEntity<Todo> createTodo(@RequestBody TodoRequest request) {
        Todo createdTodo = todoService.createTodo(request);
        return ResponseEntity.ok(createdTodo);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<String> getTodoStatus(@PathVariable Long id, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        System.out.println("Accessed by IP: " + clientIp);
        String status = todoService.getTodoStatus(id);
        return ResponseEntity.ok(status);
    }

    // 新增：獲取流程圖和當前任務
    @GetMapping("/{id}/diagram")
    public ResponseEntity<Map<String, String>> getProcessDiagram(@PathVariable Long id) {
        Map<String, String> diagramData = todoService.getProcessDiagram(id);
        return ResponseEntity.ok(diagramData);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeTodo(
            @PathVariable Long id,
            @RequestParam String action,
            @RequestParam String priority) {
        todoService.completeTodo(id, action, priority);
        return ResponseEntity.ok().build();
    }
}