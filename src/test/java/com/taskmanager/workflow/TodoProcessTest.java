package com.taskmanager.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TodoProcessTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    /**
     * 測試完整流程：高優先級任務被審閱後批准
     */
    @Test
    public void testHighPriorityApprovedFlow() {
        // 啟動流程
        Map<String, Object> vars = new HashMap<>();
        vars.put("priority", "high");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("todoProcess", vars);

        // 1. 自動分派 -> 處理待辦
        Task processTask = getTask(processInstance.getId(), "ProcessTask");
        assertThat(processTask.getName()).isEqualTo("處理待辦");

        // 2. 處理待辦 -> 完成 -> 確認待辦
        completeTask(processTask.getId(), "action", "complete");
        Task confirmTask = getTask(processInstance.getId(), "ConfirmTask");
        assertThat(confirmTask.getName()).isEqualTo("確認待辦");

        // 3. 確認待辦 -> 確認 -> 審閱待辦 (因為是高優先級)
        completeTask(confirmTask.getId(), "action", "confirm");
        Task reviewTask = getTask(processInstance.getId(), "ReviewTask");
        assertThat(reviewTask.getName()).isEqualTo("審閱待辦");

        // 4. 審閱待辦 -> 批准 -> 流程結束
        completeTask(reviewTask.getId(), "action", "approve");
        assertProcessEnded(processInstance);
    }

    /**
     * 測試完整流程：高優先級任務被審閱後拒絕
     */
    @Test
    public void testHighPriorityRejectedFlow() {
        // 啟動流程
        Map<String, Object> vars = new HashMap<>();
        vars.put("priority", "high");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("todoProcess", vars);

        // 處理待辦 -> 完成
        Task processTask = getTask(processInstance.getId(), "ProcessTask");
        completeTask(processTask.getId(), "action", "complete");

        // 確認待辦 -> 確認
        Task confirmTask = getTask(processInstance.getId(), "ConfirmTask");
        completeTask(confirmTask.getId(), "action", "confirm");

        // 審閱待辦 -> 拒絕 -> 回到確認待辦
        Task reviewTask = getTask(processInstance.getId(), "ReviewTask");
        completeTask(reviewTask.getId(), "action", "reject");

        // 應該回到確認待辦
        confirmTask = getTask(processInstance.getId(), "ConfirmTask");
        assertThat(confirmTask).isNotNull();
        assertThat(confirmTask.getName()).isEqualTo("確認待辦");

        // 完成流程
        completeTask(confirmTask.getId(), "action", "confirm");
        reviewTask = getTask(processInstance.getId(), "ReviewTask");
        completeTask(reviewTask.getId(), "action", "approve");
        assertProcessEnded(processInstance);
    }

    /**
     * 測試完整流程：低優先級任務直接結束
     */
    @Test
    public void testLowPriorityFlow() {
        // 啟動流程
        Map<String, Object> vars = new HashMap<>();
        vars.put("priority", "low");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("todoProcess", vars);

        // 處理待辦 -> 完成
        Task processTask = getTask(processInstance.getId(), "ProcessTask");
        completeTask(processTask.getId(), "action", "complete");

        // 確認待辦 -> 確認 -> 直接結束 (因為是低優先級)
        Task confirmTask = getTask(processInstance.getId(), "ConfirmTask");
        completeTask(confirmTask.getId(), "action", "confirm");

        assertProcessEnded(processInstance);
    }

    /**
     * 測試重新指派流程
     */
    @Test
    public void testReassignFlow() {
        // 啟動流程
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("todoProcess");

        // 第一次處理 -> 重新指派
        Task processTask = getTask(processInstance.getId(), "ProcessTask");
        completeTask(processTask.getId(), "action", "reassign");

        // 應該回到處理待辦
        processTask = getTask(processInstance.getId(), "ProcessTask");
        assertThat(processTask).isNotNull();
        assertThat(processTask.getName()).isEqualTo("處理待辦");

        // 這次選擇完成
        completeTask(processTask.getId(), "action", "complete");

        // 確認已進入確認待辦
        Task confirmTask = getTask(processInstance.getId(), "ConfirmTask");
        assertThat(confirmTask).isNotNull();
        assertThat(confirmTask.getName()).isEqualTo("確認待辦");
    }

    /**
     * 測試確認待辦拒絕流程
     */
    @Test
    public void testConfirmRejectFlow() {
        // 啟動流程
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("todoProcess");

        // 處理待辦 -> 完成
        Task processTask = getTask(processInstance.getId(), "ProcessTask");
        completeTask(processTask.getId(), "action", "complete");

        // 確認待辦 -> 拒絕 -> 回到處理待辦
        Task confirmTask = getTask(processInstance.getId(), "ConfirmTask");
        completeTask(confirmTask.getId(), "action", "reject");

        // 應該回到處理待辦
        Task newProcessTask = getTask(processInstance.getId(), "ProcessTask");
        assertThat(newProcessTask).isNotNull();
        assertThat(newProcessTask.getName()).isEqualTo("處理待辦");
    }

    /**
     * 測試多種優先級和決策組合
     */
    @Test
    public void testMultiplePriorityAndDecisionCombinations() {
        // 測試1: 高優先級 -> 確認 -> 審閱 -> 批准
        Map<String, Object> vars1 = new HashMap<>();
        vars1.put("priority", "high");
        ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("todoProcess", vars1);
        testPath(pi1, "complete", "confirm", "approve");
        assertProcessEnded(pi1);

        // 測試2: 高優先級 -> 確認 -> 審閱 -> 拒絕 -> 確認 -> 審閱 -> 批准
        Map<String, Object> vars2 = new HashMap<>();
        vars2.put("priority", "high");
        ProcessInstance pi2 = runtimeService.startProcessInstanceByKey("todoProcess", vars2);
        testPath(pi2, "complete", "confirm", "reject", "confirm", "approve");
        assertProcessEnded(pi2);

        // 測試3: 低優先級 -> 完成 -> 確認 (直接結束)
        Map<String, Object> vars3 = new HashMap<>();
        vars3.put("priority", "low");
        ProcessInstance pi3 = runtimeService.startProcessInstanceByKey("todoProcess", vars3);
        testPath(pi3, "complete", "confirm");
        assertProcessEnded(pi3);

        // 測試4: 處理 -> 重新指派 -> 處理 -> 完成 -> 確認 -> 審閱 -> 批准
        Map<String, Object> vars4 = new HashMap<>();
        vars4.put("priority", "high");
        ProcessInstance pi4 = runtimeService.startProcessInstanceByKey("todoProcess", vars4);
        testPath(pi4, "reassign", "complete", "confirm", "approve");
        assertProcessEnded(pi4);
    }

    /**
     * 測試任務歷史記錄
     */
    @Test
    public void testTaskHistory() {
        // 啟動流程
        Map<String, Object> vars = new HashMap<>();
        vars.put("priority", "high");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("todoProcess", vars);

        // 執行完整流程
        testPath(processInstance, "complete", "confirm", "approve");

        // 驗證流程已結束
        assertProcessEnded(processInstance);

        // 驗證所有任務都已執行 (通過任務數量驗證)
        List<Task> allTasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .list();
        assertThat(allTasks).isEmpty(); // 流程結束後應該沒有任務
    }

    /**
     * 輔助方法：按照指定路徑完成任務
     */
    private void testPath(ProcessInstance processInstance, String... actions) {
        for (String action : actions) {
            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .singleResult();
            assertThat(task).isNotNull();
            completeTask(task.getId(), "action", action);
        }
    }

    /**
     * 輔助方法：獲取指定任務
     */
    private Task getTask(String processInstanceId, String taskDefinitionKey) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskDefinitionKey)
                .singleResult();
    }

    /**
     * 輔助方法：完成任務並設置變數
     */
    private void completeTask(String taskId, String varName, String varValue) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(varName, varValue);
        taskService.complete(taskId, variables);
    }

    /**
     * 輔助方法：驗證流程已結束
     */
    private void assertProcessEnded(ProcessInstance processInstance) {
        ProcessInstance endedProcess = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(endedProcess).isNull();

        // 替代歷史記錄檢查的方式
        long activeTaskCount = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .count();
        assertThat(activeTaskCount).isZero();
    }
}