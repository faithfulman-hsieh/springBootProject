package com.taskmanager.process.controller;

import com.taskmanager.process.dto.ProcessRequest;
import com.taskmanager.process.dto.HistoryLog; // ★★★ 新增 Import
import com.taskmanager.process.model.ProcessDef;
import com.taskmanager.process.model.ProcessIns;
import com.taskmanager.process.service.ProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/process")
@Tag(name = "Process API", description = "API for managing processes")
public class ProcessController {

    private final ProcessService processService;

    public ProcessController(ProcessService processService) {
        this.processService = processService;
    }

    @GetMapping("/definitions")
    @Operation(summary = "Get all process definitions", description = "Retrieves all process definitions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcessDef.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<ProcessDef>> getAllDefinitions() {
        return ResponseEntity.ok(processService.getAllDefinitions());
    }

    @PostMapping("/deploy")
    @Operation(summary = "Deploy a process", description = "Deploys a new process with BPMN file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deployed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcessDef.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<ProcessDef> deployProcess(@ModelAttribute ProcessRequest request) {
        return ResponseEntity.ok(processService.deployProcess(request.getName(), request.getFile()));
    }

    @PostMapping("/definitions/{id}/toggle")
    @Operation(summary = "Toggle process status", description = "Enables or disables a process definition")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status toggled",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcessDef.class))),
            @ApiResponse(responseCode = "404", description = "Process not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<ProcessDef> toggleProcessStatus(@PathVariable String id) {
        return ResponseEntity.ok(processService.toggleProcessStatus(id));
    }

    @PostMapping("/start")
    @Operation(summary = "Start a process", description = "Starts a process instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Started successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcessIns.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Process not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<ProcessIns> startProcess(@RequestBody ProcessRequest request) {
        return ResponseEntity.ok(processService.startProcess(request.getProcessDefinitionId(), request.getVariables()));
    }

    @GetMapping("/instances")
    @Operation(summary = "Get all instances", description = "Retrieves all running process instances")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcessIns.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<ProcessIns>> getAllInstances() {
        return ResponseEntity.ok(processService.getAllInstances());
    }

    @GetMapping("/instances/{id}/diagram")
    @Operation(summary = "Get process instance diagram", description = "Retrieves process instance diagram and current task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Instance not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> getProcessInstanceDiagram(@PathVariable String id) {
        return ResponseEntity.ok(processService.getProcessInstanceDiagram(id));
    }

    @GetMapping("/definitions/{id}/form")
    @Operation(summary = "Get process form fields", description = "Retrieves form fields for a process definition")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "404", description = "Process definition not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<Map<String, Object>>> getProcessFormFields(@PathVariable String id) {
        return ResponseEntity.ok(processService.getProcessFormFields(id));
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Retrieves all available users for task assignment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<Map<String, String>>> getUsers() {
        return ResponseEntity.ok(processService.getUsers());
    }

    @GetMapping("/instances/{processInstanceId}/nodes")
    @Operation(summary = "Get flow nodes", description = "Retrieves all flow nodes for a process instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "404", description = "Instance not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<Map<String, String>>> getFlowNodes(@PathVariable String processInstanceId) {
        return ResponseEntity.ok(processService.getFlowNodes(processInstanceId));
    }

    @PostMapping("/instances/{processInstanceId}/reassign")
    @Operation(summary = "Reassign task", description = "Reassigns the current task of a process instance to a new assignee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task reassigned successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Instance or task not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Void> reassignTask(@PathVariable String processInstanceId, @RequestBody Map<String, String> request) {
        String newAssignee = request.get("newAssignee");
        if (newAssignee == null || newAssignee.isEmpty()) {
            throw new IllegalArgumentException("新執行者不能為空");
        }
        processService.reassignTask(processInstanceId, newAssignee);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/instances/{processInstanceId}/jump")
    @Operation(summary = "Jump to node", description = "Jumps the process instance to a specified node")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Node jumped successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Instance or node not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Void> jumpToNode(@PathVariable String processInstanceId, @RequestBody Map<String, String> request) {
        String targetNode = request.get("targetNode");
        if (targetNode == null || targetNode.isEmpty()) {
            throw new IllegalArgumentException("目標節點不能為空");
        }
        processService.jumpToNode(processInstanceId, targetNode);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/definitions/{id}/diagram")
    @Operation(summary = "Get process definition diagram", description = "Retrieves process definition diagram")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Definition not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> getProcessDefinitionDiagram(@PathVariable String id) {
        return ResponseEntity.ok(processService.getProcessDefinitionDiagram(id));
    }

    @GetMapping("/template/{filename}")
    @Operation(summary = "Download process template", description = "Downloads a BPMN process template file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "404", description = "Template not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Resource> downloadTemplate(@PathVariable String filename) {
        Resource resource = processService.getProcessTemplate(filename);
        String encodedFilename = URLEncoder.encode(resource.getFilename(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                .body(resource);
    }

    // ★★★ 新增：歷史紀錄查詢 API ★★★
    @GetMapping("/instances/{id}/history")
    @Operation(summary = "Get process history", description = "Retrieves execution history for a process instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "404", description = "Instance not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<HistoryLog>> getProcessHistory(@PathVariable String id) {
        return ResponseEntity.ok(processService.getProcessHistory(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}