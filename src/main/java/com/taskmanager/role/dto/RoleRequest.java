// src/main/java/com/taskmanager/role/dto/RoleRequest.java
package com.taskmanager.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Request object for updating user roles")
public class RoleRequest {

    @Schema(description = "List of role names", example = "[\"employee\", \"manager\"]", required = true)
    private List<String> roles;

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}