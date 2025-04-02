package com.taskmanager.todo.controller;

import com.taskmanager.todo.model.Todo;
import com.taskmanager.todo.service.TodoService;
import com.taskmanager.todo.dto.TodoRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    // 建立 Todo
    @PostMapping
    public ResponseEntity<Todo> createTodo(@RequestBody TodoRequest request) {
        Todo createdTodo = todoService.createTodo(request);
        return ResponseEntity.ok(createdTodo);
    }

    // 取得 Todo 狀態
    @GetMapping("/{id}/status")
    public ResponseEntity<String> getTodoStatus(@PathVariable Long id, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        System.out.println("Accessed by IP: " + clientIp);
        String status = todoService.getTodoStatus(id);
        return ResponseEntity.ok(status);
    }

    // 完成 Todo
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeTodo(
            @PathVariable Long id,
            @RequestParam String action,
            @RequestParam String priority) {
        todoService.completeTodo(id, action, priority);
        return ResponseEntity.ok().build();
    }
}