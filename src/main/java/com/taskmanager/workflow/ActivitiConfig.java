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
        return repositoryService.createDeployment()
                .addClasspathResource("processes/taskProcess.bpmn20.xml")
                .deploy();
    }

    @Bean
    public ProcessEngineConfigurationImpl processEngineConfiguration() {
        StandaloneProcessEngineConfiguration config = new StandaloneProcessEngineConfiguration();
        config.setJdbcUrl("jdbc:h2:mem:activiti-db;DB_CLOSE_DELAY=-1");
        config.setJdbcDriver("org.h2.Driver");
        config.setJdbcUsername("sa");
        config.setJdbcPassword("");
        config.setDatabaseSchemaUpdate("true"); // 自動更新 DB Schema
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
}