package com.taskmanager.todo.repository;

import com.taskmanager.todo.model.Todo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class TodoRepositoryTest {

    @Autowired
    private TodoRepository todoRepository;

    private Todo todo1;
    private Todo todo2;

    @BeforeEach
    void setUp() {
        // 在每個測試之前插入資料
        todo1 = new Todo("Todo 1", "Description of Todo 1", "john.doe");
        todo1.setStatus("PENDING");
        todoRepository.save(todo1);

        todo2 = new Todo("Todo 2", "Description of Todo 2", "jane.doe");
        todo2.setStatus("COMPLETED");
        todoRepository.save(todo2);
    }

    @Test
    void testFindByTitle() {
        // 測試根據標題查詢 Todo
        Optional<Todo> result = todoRepository.findByTitle("Todo 1");
        assertTrue(result.isPresent());
        assertEquals("Todo 1", result.get().getTitle());
    }

    @Test
    void testFindByStatus() {
        // 測試根據狀態查詢 Todo
        List<Todo> pendingTodos = todoRepository.findByStatus("PENDING");
        assertEquals(1, pendingTodos.size());
        assertEquals("Todo 1", pendingTodos.get(0).getTitle());

        List<Todo> completedTodos = todoRepository.findByStatus("COMPLETED");
        assertEquals(1, completedTodos.size());
        assertEquals("Todo 2", completedTodos.get(0).getTitle());
    }

    @Test
    void testFindByAssignee() {
        // 測試根據指派人查詢 Todo
        List<Todo> johnsTodos = todoRepository.findByAssignee("john.doe");
        assertEquals(1, johnsTodos.size());
        assertEquals("Todo 1", johnsTodos.get(0).getTitle());

        List<Todo> janesTodos = todoRepository.findByAssignee("jane.doe");
        assertEquals(1, janesTodos.size());
        assertEquals("Todo 2", janesTodos.get(0).getTitle());
    }

    @Test
    void testFindByTitleNotFound() {
        // 測試查詢不存在的標題
        Optional<Todo> result = todoRepository.findByTitle("Nonexistent Todo");
        assertFalse(result.isPresent());
    }
}