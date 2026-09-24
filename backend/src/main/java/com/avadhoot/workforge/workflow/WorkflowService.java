package com.avadhoot.workforge.workflow;

import com.avadhoot.workforge.exception.InvalidTransitionException;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.workflow.domain.Workflow;
import com.avadhoot.workforge.workflow.domain.WorkflowStatus;
import com.avadhoot.workforge.workflow.domain.WorkflowTransition;
import com.avadhoot.workforge.workflow.repository.WorkflowRepository;
import com.avadhoot.workforge.workflow.repository.WorkflowStatusRepository;
import com.avadhoot.workforge.workflow.repository.WorkflowTransitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Encapsulates the workflow transition rules. Transitions are only permitted when a
 * matching {@link WorkflowTransition} row exists for the workflow.
 */
@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowTransitionRepository transitionRepository;

    public WorkflowService(WorkflowRepository workflowRepository,
                           WorkflowStatusRepository workflowStatusRepository,
                           WorkflowTransitionRepository transitionRepository) {
        this.workflowRepository = workflowRepository;
        this.workflowStatusRepository = workflowStatusRepository;
        this.transitionRepository = transitionRepository;
    }

    @Transactional(readOnly = true)
    public Workflow resolveWorkflow(Long workflowId) {
        if (workflowId != null) {
            return workflowRepository.findById(workflowId)
                    .orElseThrow(() -> new ResourceNotFoundException("Workflow", workflowId));
        }
        return defaultWorkflow();
    }

    @Transactional(readOnly = true)
    public Workflow defaultWorkflow() {
        return workflowRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No default workflow configured"));
    }

    @Transactional(readOnly = true)
    public Long initialStatusId(Long workflowId) {
        return workflowStatusRepository.findByWorkflowIdAndInitialTrue(workflowId)
                .map(WorkflowStatus::getStatusId)
                .orElseThrow(() -> new InvalidTransitionException(
                        "Workflow " + workflowId + " has no initial status"));
    }

    @Transactional(readOnly = true)
    public boolean canTransition(Long workflowId, Long fromStatusId, Long toStatusId) {
        if (fromStatusId != null && fromStatusId.equals(toStatusId)) {
            return true;
        }
        return transitionRepository.existsByWorkflowIdAndFromStatusIdAndToStatusId(
                workflowId, fromStatusId, toStatusId);
    }

    /**
     * @throws InvalidTransitionException when the requested status change is not allowed.
     */
    @Transactional(readOnly = true)
    public void validateTransition(Long workflowId, Long fromStatusId, Long toStatusId) {
        if (!canTransition(workflowId, fromStatusId, toStatusId)) {
            throw new InvalidTransitionException(
                    "Transition from status %s to %s is not permitted in workflow %s"
                            .formatted(fromStatusId, toStatusId, workflowId));
        }
    }

    @Transactional(readOnly = true)
    public List<WorkflowTransition> availableTransitions(Long workflowId, Long fromStatusId) {
        return transitionRepository.findByWorkflowIdAndFromStatusId(workflowId, fromStatusId);
    }

    @Transactional(readOnly = true)
    public List<WorkflowStatus> statuses(Long workflowId) {
        return workflowStatusRepository.findByWorkflowIdOrderByPosition(workflowId);
    }
}
