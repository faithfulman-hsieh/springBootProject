package com.taskmanager.todo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "todos")
@Getter
@Setter
@NoArgsConstructor
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String status;

    private String processInstanceId;
    private String processDefinitionId; // 新增欄位：儲存流程定義 ID
    private String assignee;

    // 預設建構子
    public Todo(String title, String description, String assignee) {
        this.title = title;
        this.description = description;
        this.status = "PENDING"; // 預設狀態為 PENDING
        this.assignee = assignee;
    }
}