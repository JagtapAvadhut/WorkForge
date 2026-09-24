package com.avadhoot.workforge.workflow.repository;

import com.avadhoot.workforge.workflow.domain.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

    List<WorkflowTransition> findByWorkflowId(Long workflowId);

    List<WorkflowTransition> findByWorkflowIdAndFromStatusId(Long workflowId, Long fromStatusId);

    boolean existsByWorkflowIdAndFromStatusIdAndToStatusId(Long workflowId, Long fromStatusId, Long toStatusId);
}
