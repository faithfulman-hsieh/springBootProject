package com.taskmanager.todo.repository;

import com.taskmanager.todo.model.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // 根據標題查詢 Todo
    Optional<Todo> findByTitle(String title);

    // 根據狀態查詢 Todo
    List<Todo> findByStatus(String status);

    // 根據指派人查詢 Todo
    List<Todo> findByAssignee(String assignee);
}