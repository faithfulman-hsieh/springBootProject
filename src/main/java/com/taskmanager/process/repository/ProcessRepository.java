// src/main/java/com/taskmanager/process/repository/ProcessRepository.java
package com.taskmanager.process.repository;

import com.taskmanager.process.model.Process;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessRepository extends JpaRepository<Process, String> {
}