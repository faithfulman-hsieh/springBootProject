package com.taskmanager.process.controller;

import com.taskmanager.process.dto.ProcessRequest;
import com.taskmanager.process.model.ProcessDefinition;
import com.taskmanager.process.model.ProcessInstance;
import com.taskmanager.process.service.ProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcessDefinition.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<ProcessDefinition>> getAllDefinitions() {
        return ResponseEntity.ok(processService.getAllDefinitions());
    }

    @PostMapping("/deploy")
    @Operation(summary = "Deploy a process", description = "Deploys a new process with BPMN file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deployed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcessDefinition.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<ProcessDefinition> deployProcess(@ModelAttribute ProcessRequest request) {
        return ResponseEntity.ok(processService.deployProcess(request.getName(), request.getFile()));
    }

    @PostMapping("/definitions/{id}/toggle")
    @Operation(summary = "Toggle process status", description = "Enables or disables a process definition")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status toggled",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcessDefinition.class))),
            @ApiResponse(responseCode = "404", description = "Process not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<ProcessDefinition> toggleProcessStatus(@PathVariable String id) {
        return ResponseEntity.ok(processService.toggleProcessStatus(id));
    }

    @PostMapping("/start")
    @Operation(summary = "Start a process", description = "Starts a process instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Started successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcessInstance.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Process not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<ProcessInstance> startProcess(@RequestBody ProcessRequest request) {
        return ResponseEntity.ok(processService.startProcess(request.getProcessDefinitionId(), request.getVariables()));
    }

    @GetMapping("/instances")
    @Operation(summary = "Get all instances", description = "Retrieves all running process instances")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcessInstance.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<ProcessInstance>> getAllInstances() {
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