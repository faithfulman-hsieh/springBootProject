package com.taskmanager.workflow;

import org.activiti.engine.*;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.repository.Deployment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActivitiConfig {

    @Bean
    public Deployment deployProcess(RepositoryService repositoryService) {
        // 確保路徑正確，若無此檔案可先註解掉或確保檔案存在
        return repositoryService.createDeployment()
                .addClasspathResource("processes/todoProcess.bpmn20.xml")
                .deploy();
    }

    @Bean
    public ProcessEngineConfigurationImpl processEngineConfiguration() {
        StandaloneProcessEngineConfiguration config = new StandaloneProcessEngineConfiguration();
        // 請注意：這裡使用 H2 記憶體資料庫，每次重啟資料都會消失
        config.setJdbcUrl("jdbc:h2:mem:activiti-db;DB_CLOSE_DELAY=-1");
        config.setJdbcDriver("org.h2.Driver");
        config.setJdbcUsername("sa");
        config.setJdbcPassword("");
        config.setDatabaseSchemaUpdate("true"); // 自動更新 DB Schema

        // ★★★ 關鍵：確保歷史層級被設定 ★★★
        config.setHistory("full");

        return config;
    }

    @Bean
    public ProcessEngine processEngine(ProcessEngineConfigurationImpl config) {
        return config.buildProcessEngine();
    }

    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    // ★★★ 新增：註冊 ManagementService Bean (解決 Parameter 6 error) ★★★
    @Bean
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }
}