// Role.java
package com.taskmanager.account.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @ManyToMany(mappedBy = "roles")
    @JsonIgnore  // 忽略 users 屬性，防止 JSON 遞迴
    private Set<User> users = new HashSet<>();

    public Role() {}
    public Role(String name) {
        this.name = name;
    }
    public Role(Long id,String name) {
        this.id = id;
        this.name = name;
    }
}