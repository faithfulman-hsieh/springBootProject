package com.taskmanager.process.repository;

import com.taskmanager.process.model.ProcessInstance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessInstanceRepository extends JpaRepository<ProcessInstance, String> {
}