package com.taskmanager.process.repository;

import com.taskmanager.process.model.ProcessDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessDefinitionRepository extends JpaRepository<ProcessDefinition, String> {
}