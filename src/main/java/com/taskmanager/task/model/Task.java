// src/main/java/com/taskmanager/task/model/Task.java
package com.taskmanager.task.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task {

    @Id
    private String id;

    private String name;
    private String processName;
    private String assignee;
    private String createTime;

    public Task(String id, String name, String processName, String assignee, String createTime) {
        this.id = id;
        this.name = name;
        this.processName = processName;
        this.assignee = assignee;
        this.createTime = createTime;
    }
}