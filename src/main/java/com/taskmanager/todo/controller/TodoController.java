package com.taskmanager.todo.controller;

import com.taskmanager.todo.model.Todo;
import com.taskmanager.todo.service.TodoService;
import com.taskmanager.todo.dto.TodoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todos")
@Tag(name = "Todo API", description = "API for managing todo tasks")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    @Operation(summary = "Get all todos", description = "Retrieves a list of all todo tasks")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved todo list",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Todo.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<Todo>> getAllTodos() {
        List<Todo> todos = todoService.getAllTodos();
        return ResponseEntity.ok(todos);
    }

    @PostMapping("/addTodo")
    @Operation(summary = "Create a new todo", description = "Creates a new todo task with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Todo.class))),
            @ApiResponse(responseCode = "400", description = "Invalid todo data", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Todo> createTodo(@RequestBody TodoRequest request) {
        Todo createdTodo = todoService.createTodo(request);
        return ResponseEntity.ok(createdTodo);
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get todo status", description = "Retrieves the status of a specific todo task by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved todo status",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Todo not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<String> getTodoStatus(
            @Parameter(description = "ID of the todo task") @PathVariable Long id,
            HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        System.out.println("Accessed by IP: " + clientIp);
        String status = todoService.getTodoStatus(id);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/{id}/diagram")
    @Operation(summary = "Get process diagram", description = "Retrieves the process diagram and current task for a specific todo by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved process diagram",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Todo not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Map<String, String>> getProcessDiagram(
            @Parameter(description = "ID of the todo task") @PathVariable Long id) {
        Map<String, String> diagramData = todoService.getProcessDiagram(id);
        return ResponseEntity.ok(diagramData);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a todo", description = "Marks a todo task as completed with the specified action and priority")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo completed successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Todo not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid action or priority", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Void> completeTodo(
            @Parameter(description = "ID of the todo task") @PathVariable Long id,
            @Parameter(description = "Action to complete the todo") @RequestParam String action,
            @Parameter(description = "Priority of the todo") @RequestParam String priority) {
        todoService.completeTodo(id, action, priority);
        return ResponseEntity.ok().build();
    }
}