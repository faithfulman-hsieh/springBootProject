// src/main/java/com/taskmanager/role/service/RoleService.java
package com.taskmanager.role.service;

import com.taskmanager.role.model.UserRole;
import com.taskmanager.role.repository.UserRoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {

    private final UserRoleRepository repository;

    public RoleService(UserRoleRepository repository) {
        this.repository = repository;
    }

    public List<UserRole> getAllUserRoles() {
        return repository.findAll();
    }

    public UserRole updateUserRoles(String username, List<String> roles) {
        String rolesStr = String.join(",", roles);
        UserRole userRole = repository.findById(username)
                .orElse(new UserRole(username, rolesStr));
        userRole.setRoles(rolesStr);
        return repository.save(userRole);
    }
}