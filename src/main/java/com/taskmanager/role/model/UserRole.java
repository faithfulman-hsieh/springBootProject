// src/main/java/com/taskmanager/role/model/UserRole.java
package com.taskmanager.role.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_roles")
@Getter
@Setter
@NoArgsConstructor
public class UserRole {

    @Id
    private String username;

    private String roles;

    public UserRole(String username, String roles) {
        this.username = username;
        this.roles = roles;
    }
}