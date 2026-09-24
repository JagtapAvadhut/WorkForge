package com.avadhoot.workforge.workflow.repository;

import com.avadhoot.workforge.workflow.domain.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    Optional<Workflow> findByIsDefaultTrue();

    Optional<Workflow> findByNameIgnoreCase(String name);
}
