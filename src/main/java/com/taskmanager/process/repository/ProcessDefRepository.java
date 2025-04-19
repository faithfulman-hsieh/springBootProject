package com.taskmanager.process.repository;

import com.taskmanager.process.model.ProcessDef;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessDefRepository extends JpaRepository<ProcessDef, String> {
}