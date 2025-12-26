package com.taskmanager.workflow;

import com.taskmanager.workflow.service.WorkflowService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class WorkflowServiceTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testStartTodoProcess() {
        // 模擬 ProcessInstance，回傳 ID
        ProcessInstance mockInstance = mock(ProcessInstance.class);
        when(mockInstance.getId()).thenReturn("testProcess123");

        // 模擬啟動流程
        when(runtimeService.startProcessInstanceByKey(eq("todoProcess"), anyMap()))
                .thenReturn(mockInstance);

        // 測試 startProcess 方法
        // ★★★ 修正：傳入 4 個參數 ★★★
        String title = "Test Task";
        String description = "Description";
        String priority = "high";
        String processId = workflowService.startProcess("john", title, description, priority);

        // 驗證 runtimeService 被正確呼叫
        ArgumentCaptor<String> processKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(runtimeService).startProcessInstanceByKey(processKeyCaptor.capture(), variablesCaptor.capture());

        // 驗證流程 ID 與變數內容
        assertEquals("testProcess123", processId);
        assertEquals("todoProcess", processKeyCaptor.getValue());

        Map<String, Object> capturedVariables = variablesCaptor.getValue();
        assertEquals("john", capturedVariables.get("assignee"));
        // ★★★ 新增：驗證新加入的變數 ★★★
        assertEquals(title, capturedVariables.get("todoTitle"));
        assertEquals(description, capturedVariables.get("description"));
        assertEquals(priority, capturedVariables.get("priority"));
    }

    @Test
    void testGetUserTasks() {
        // 模擬 Task
        Task mockTask1 = mock(Task.class);
        Task mockTask2 = mock(Task.class);
        when(mockTask1.getName()).thenReturn("Review Task");
        when(mockTask2.getName()).thenReturn("Approval Task");

        // Mock TaskQuery，確保 createTaskQuery() 不是 null
        TaskQuery mockTaskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(mockTaskQuery);
        when(mockTaskQuery.taskAssignee("john")).thenReturn(mockTaskQuery);
        when(mockTaskQuery.list()).thenReturn(List.of(mockTask1, mockTask2));

        // 測試 getUserTasks 方法
        List<String> taskNames = workflowService.getUserTasks("john");

        // 驗證回傳的 Task 名稱
        assertEquals(2, taskNames.size());
        assertEquals("Review Task", taskNames.get(0));
        assertEquals("Approval Task", taskNames.get(1));

        // 驗證是否有正確呼叫 taskService.createTaskQuery()
        verify(taskService).createTaskQuery();
        verify(mockTaskQuery).taskAssignee("john");
        verify(mockTaskQuery).list();
    }

    @Test
    void testCompleteTask() {
        // 測試 completeTask 方法
        workflowService.completeTask("task123", "approve", "high");

        // 驗證 taskService.complete 是否正確呼叫
        ArgumentCaptor<String> taskIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(taskService).complete(taskIdCaptor.capture(), variablesCaptor.capture());

        assertEquals("task123", taskIdCaptor.getValue());
        assertEquals("approve", variablesCaptor.getValue().get("action"));
        assertEquals("high", variablesCaptor.getValue().get("priority"));
    }
}