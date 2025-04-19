package com.taskmanager.process.repository;

import com.taskmanager.process.model.ProcessIns;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessInsRepository extends JpaRepository<ProcessIns, String> {
}