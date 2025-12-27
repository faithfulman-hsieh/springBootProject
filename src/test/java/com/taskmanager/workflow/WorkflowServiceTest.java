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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void testStartProcess() {
        // 模擬 ProcessInstance
        ProcessInstance mockInstance = mock(ProcessInstance.class);
        when(mockInstance.getId()).thenReturn("proc-123");
        when(mockInstance.getProcessInstanceId()).thenReturn("proc-123");

        // 模擬 runtimeService
        when(runtimeService.startProcessInstanceByKey(eq("todoProcess"), anyMap()))
                .thenReturn(mockInstance);

        // ★★★ 修正：呼叫時傳入 4 個參數 ★★★
        String processId = workflowService.startProcess("user", "Title", "Desc", "High");

        assertEquals("proc-123", processId);

        // 驗證參數傳遞
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(runtimeService).startProcessInstanceByKey(eq("todoProcess"), captor.capture());

        Map<String, Object> vars = captor.getValue();
        assertEquals("user", vars.get("assignee"));
        assertEquals("Title", vars.get("todoTitle"));
        assertEquals("Desc", vars.get("description"));
        assertEquals("High", vars.get("priority"));
    }

    @Test
    void testGetCurrentTask() {
        // 模擬 TaskQuery 鏈式呼叫
        TaskQuery mockQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(mockQuery);
        when(mockQuery.processInstanceId(anyString())).thenReturn(mockQuery);

        Task mockTask = mock(Task.class);
        when(mockTask.getName()).thenReturn("TestTask");

        // ★★★ 關鍵修正：模擬 list() 而不是 singleResult() ★★★
        // 因為我們在 Service 裡改用了 .list() 來支援並行任務
        when(mockQuery.list()).thenReturn(List.of(mockTask));

        Task result = workflowService.getCurrentTask("proc-123");

        assertNotNull(result);
        assertEquals("TestTask", result.getName());

        // 驗證呼叫了 list()
        verify(mockQuery).list();
    }

    @Test
    void testCompleteTask() {
        workflowService.completeTask("task-1", "approve", "high");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(taskService).complete(eq("task-1"), captor.capture());

        assertEquals("approve", captor.getValue().get("action"));
        assertEquals("high", captor.getValue().get("priority"));
    }
}