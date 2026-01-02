package com.taskmanager.task.controller;

import com.taskmanager.task.dto.TaskDto;
import com.taskmanager.task.dto.TaskFormRequest;
import com.taskmanager.task.dto.TaskReassignRequest;
import com.taskmanager.task.service.TaskManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/task")
@Tag(name = "Task API", description = "API for managing tasks")
public class TaskController {

    private final TaskManagerService taskManagerService;

    public TaskController(TaskManagerService taskManagerService) {
        this.taskManagerService = taskManagerService;
    }

    @GetMapping("/my-tasks")
    @Operation(summary = "Get user's tasks", description = "Retrieves tasks assigned to the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDto.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<TaskDto>> getMyTasks() {
        return ResponseEntity.ok(taskManagerService.getMyTasks());
    }

    // ★★★ 新增：取得群組可認領任務 ★★★
    @GetMapping("/group-tasks")
    @Operation(summary = "Get group tasks", description = "Retrieves unassigned tasks for candidate groups")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDto.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<TaskDto>> getGroupTasks() {
        return ResponseEntity.ok(taskManagerService.getGroupTasks());
    }

    // ★★★ 新增：簽收任務 ★★★
    @PostMapping("/{id}/claim")
    @Operation(summary = "Claim a task", description = "Assign the task to the current user")
    public ResponseEntity<Void> claimTask(@PathVariable String id) {
        taskManagerService.claimTask(id);
        return ResponseEntity.ok().build();
    }

    // ★★★ 新增：反簽收任務 ★★★
    @PostMapping("/{id}/unclaim")
    @Operation(summary = "Unclaim a task", description = "Release the task back to group")
    public ResponseEntity<Void> unclaimTask(@PathVariable String id) {
        taskManagerService.unclaimTask(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history-tasks")
    @Operation(summary = "Get user's history tasks", description = "Retrieves completed tasks assigned to the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDto.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<TaskDto>> getHistoryTasks() {
        return ResponseEntity.ok(taskManagerService.getHistoryTasks());
    }

    @GetMapping("/{id}/form")
    @Operation(summary = "Get task form", description = "Retrieves the form structure for a task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<Map<String, Object>>> getTaskForm(@PathVariable String id) {
        return ResponseEntity.ok(taskManagerService.getTaskForm(id));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit task form", description = "Submits form data for a task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Submitted successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid form data", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Void> submitTaskForm(@PathVariable String id, @RequestBody TaskFormRequest request) {
        taskManagerService.submitTaskForm(id, request.getFormData());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reassign")
    @Operation(summary = "Reassign task", description = "Reassigns a task to a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reassigned successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid assignee", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Void> reassignTask(@PathVariable String id, @RequestBody TaskReassignRequest request) {
        taskManagerService.reassignTask(id, request.getAssignee());
        return ResponseEntity.ok().build();
    }
}