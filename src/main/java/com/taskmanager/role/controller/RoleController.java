// src/main/java/com/taskmanager/role/controller/RoleController.java
package com.taskmanager.role.controller;

import com.taskmanager.role.dto.RoleRequest;
import com.taskmanager.role.model.UserRole;
import com.taskmanager.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role")
@Tag(name = "Role API", description = "API for managing user roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/users")
    @Operation(summary = "Get all user roles", description = "Retrieves all users and their roles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserRole.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<UserRole>> getAllUserRoles() {
        return ResponseEntity.ok(roleService.getAllUserRoles());
    }

    @PutMapping("/users/{username}")
    @Operation(summary = "Update user roles", description = "Updates roles for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserRole.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid role data", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<UserRole> updateUserRoles(@PathVariable String username, @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.updateUserRoles(username, request.getRoles()));
    }
}