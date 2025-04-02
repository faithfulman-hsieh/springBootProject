package com.taskmanager.todo.controller;

import com.taskmanager.todo.dto.TodoRequest;
import com.taskmanager.todo.model.Todo;
import com.taskmanager.todo.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TodoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TodoService todoService;

    @InjectMocks
    private TodoController todoController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(todoController).build();
    }

    @Test
    void testCreateTodo() throws Exception {
        TodoRequest request = new TodoRequest();
        request.setTitle("測試標題");
        request.setDescription("測試描述");
        request.setAssignee("user1");

        Todo createdTodo = new Todo();
        createdTodo.setId(1L);
        createdTodo.setTitle("測試標題");
        createdTodo.setStatus("PENDING");
        createdTodo.setDescription("測試描述");
        createdTodo.setAssignee("user1");
        createdTodo.setProcessInstanceId("process123");

        when(todoService.createTodo(any(TodoRequest.class))).thenReturn(createdTodo);

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("測試標題"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(todoService, times(1)).createTodo(any(TodoRequest.class));
    }

    @Test
    void testGetTodoStatus() throws Exception {
        Long todoId = 1L;
        String expectedStatus = "IN_PROGRESS";

        when(todoService.getTodoStatus(todoId)).thenReturn(expectedStatus);

        mockMvc.perform(get("/api/todos/{id}/status", todoId))
                .andExpect(status().isOk())
                .andExpect(content().string("IN_PROGRESS"));

        verify(todoService, times(1)).getTodoStatus(todoId);
    }

    @Test
    void testCompleteTodo() throws Exception {
        Long todoId = 1L;
        String action = "approve";
        String priority = "high";

        doNothing().when(todoService).completeTodo(todoId, action, priority);

        mockMvc.perform(post("/api/todos/{id}/complete", todoId)
                        .param("action", action)
                        .param("priority", priority))
                .andExpect(status().isOk());

        verify(todoService, times(1)).completeTodo(todoId, action, priority);
    }
}