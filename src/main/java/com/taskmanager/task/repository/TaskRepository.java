// src/main/java/com/taskmanager/task/repository/TaskRepository.java
package com.taskmanager.task.repository;

import com.taskmanager.task.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByAssignee(String assignee);
}