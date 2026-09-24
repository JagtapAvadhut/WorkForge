package com.avadhoot.workforge.workflow.repository;

import com.avadhoot.workforge.workflow.domain.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, Long> {

    List<WorkflowStatus> findByWorkflowIdOrderByPosition(Long workflowId);

    Optional<WorkflowStatus> findByWorkflowIdAndInitialTrue(Long workflowId);
}
