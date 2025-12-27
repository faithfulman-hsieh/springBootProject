package com.taskmanager.todo.service;

import com.taskmanager.todo.dto.TodoRequest;
import com.taskmanager.todo.model.Todo;
import com.taskmanager.todo.repository.TodoRepository;
import com.taskmanager.workflow.service.WorkflowService;
import org.activiti.engine.task.Task; // 記得 import Task
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

        todoRequest = new TodoRequest();
        todoRequest.setTitle("Test Todo");
        todoRequest.setDescription("This is a test todo.");
        todoRequest.setAssignee("john.doe");
        todoRequest.setPriority("medium"); // 設定優先級

        todo = new Todo("Test Todo", "This is a test todo.", "john.doe");
        todo.setId(1L);
        todo.setProcessInstanceId("process-12345");
        todo.setStatus("TaskName");
    }

    @Test
    void testCreateTodo() {
        // 設定模擬行為
        // ★★★ 修正 1：startProcess 參數匹配 (使用 anyString 或具體值) ★★★
        when(workflowService.startProcess(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("process-12345");

        // 模擬 getCurrentTask 回傳一個任務，避免 NullPointerException
        Task mockTask = mock(Task.class);
        when(mockTask.getProcessDefinitionId()).thenReturn("def-123");
        when(mockTask.getName()).thenReturn("TaskName");
        when(mockTask.getAssignee()).thenReturn("john.doe");
        when(workflowService.getCurrentTask("process-12345")).thenReturn(mockTask);

        when(todoRepository.save(any(Todo.class))).thenReturn(todo);

        // 執行測試
        Todo createdTodo = todoService.createTodo(todoRequest);

        // 驗證結果
        assertNotNull(createdTodo);
        assertEquals("Test Todo", createdTodo.getTitle());
        assertEquals("process-12345", createdTodo.getProcessInstanceId());

        // ★★★ 修正 2：驗證呼叫時傳入 4 個參數 ★★★
        verify(workflowService, times(1)).startProcess(
                eq(todoRequest.getAssignee()),
                eq(todoRequest.getTitle()),
                eq(todoRequest.getDescription()),
                eq(todoRequest.getPriority())
        );
        verify(todoRepository, times(1)).save(any(Todo.class));
    }

    @Test
    void testGetTodoStatus() {
        when(todoRepository.findById(1L)).thenReturn(java.util.Optional.of(todo));

        String status = todoService.getTodoStatus(1L);

        assertEquals("TaskName", status);
        verify(todoRepository, times(1)).findById(1L);
    }

    @Test
    void testCompleteTodo() {
        when(todoRepository.findById(1L)).thenReturn(java.util.Optional.of(todo));

        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn("task-123");
        when(mockTask.getName()).thenReturn("NextTask");
        when(mockTask.getAssignee()).thenReturn("john.doe");

        // 模擬 complete 前後的 task 狀態
        when(workflowService.getCurrentTask(todo.getProcessInstanceId()))
                .thenReturn(mockTask); // 第一次呼叫 (complete 前)
        //.thenReturn(null);   // 第二次呼叫 (complete 後，這裡簡化處理)

        // 執行
        todoService.completeTodo(1L, "approve", "high");

        // 驗證
        verify(workflowService, times(1)).completeTask("task-123", "approve", "high");
        verify(todoRepository, times(1)).save(todo);
    }
}