// src/main/java/com/taskmanager/role/repository/UserRoleRepository.java
package com.taskmanager.role.repository;

import com.taskmanager.role.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, String> {
}