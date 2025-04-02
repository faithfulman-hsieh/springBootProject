package com.taskmanager.todo.service;

import com.taskmanager.todo.dto.TodoRequest;
import com.taskmanager.todo.model.Todo;
import com.taskmanager.todo.repository.TodoRepository;
import com.taskmanager.workflow.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private TodoService todoService;

    private TodoRequest todoRequest;
    private Todo todo;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 初始化測試資料
        todoRequest = new TodoRequest();
        todoRequest.setTitle("Test Todo");
        todoRequest.setDescription("This is a test todo.");
        todoRequest.setAssignee("john.doe");

        todo = new Todo("Test Todo", "This is a test todo.", "john.doe");
        todo.setId(1L);
        todo.setProcessInstanceId("process-12345");
    }

    @Test
    void testCreateTodo() {
        // 設定模擬返回
        when(workflowService.startProcess(todoRequest.getAssignee())).thenReturn("process-12345");
        when(todoRepository.save(any(Todo.class))).thenReturn(todo);

        // 執行方法
        Todo createdTodo = todoService.createTodo(todoRequest);

        // 驗證結果
        assertNotNull(createdTodo);
        assertEquals("Test Todo", createdTodo.getTitle());
        assertEquals("This is a test todo.", createdTodo.getDescription());
        assertEquals("john.doe", createdTodo.getAssignee());
        assertEquals("PENDING", createdTodo.getStatus());
        assertEquals("process-12345", createdTodo.getProcessInstanceId());

        // 驗證方法是否被呼叫
        verify(workflowService, times(1)).startProcess(todoRequest.getAssignee());
        verify(todoRepository, times(1)).save(any(Todo.class));
    }

    @Test
    void testGetTodoStatusInProgress() {
        // 設定模擬返回
        when(todoRepository.findById(1L)).thenReturn(java.util.Optional.of(todo));
        when(workflowService.getUserTasks(todo.getAssignee())).thenReturn(List.of("task1", "task2"));

        // 執行方法
        String status = todoService.getTodoStatus(1L);

        // 驗證結果
        assertEquals("IN_PROGRESS", status);

        // 驗證方法是否被呼叫
        verify(todoRepository, times(1)).findById(1L);
        verify(workflowService, times(1)).getUserTasks(todo.getAssignee());
    }

    @Test
    void testGetTodoStatusCompleted() {
        // 設定模擬返回
        when(todoRepository.findById(1L)).thenReturn(java.util.Optional.of(todo));
        when(workflowService.getUserTasks(todo.getAssignee())).thenReturn(List.of());

        // 執行方法
        String status = todoService.getTodoStatus(1L);

        // 驗證結果
        assertEquals("COMPLETED", status);

        // 驗證方法是否被呼叫
        verify(todoRepository, times(1)).findById(1L);
        verify(workflowService, times(1)).getUserTasks(todo.getAssignee());
    }

    @Test
    void testCompleteTodo() {
        // 設定模擬返回
        when(todoRepository.findById(1L)).thenReturn(java.util.Optional.of(todo));

        // 執行方法
        todoService.completeTodo(1L, "approve", "high");

        // 驗證方法是否被呼叫
        verify(workflowService, times(1)).completeTask(todo.getProcessInstanceId(), "approve", "high");
        verify(todoRepository, times(1)).save(todo);

        // 驗證 Todo 狀態是否已更新
        assertEquals("COMPLETED", todo.getStatus());
    }
}