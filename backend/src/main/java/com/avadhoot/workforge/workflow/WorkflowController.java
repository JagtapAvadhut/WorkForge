package com.avadhoot.workforge.workflow;

import com.avadhoot.workforge.common.dto.ApiResponse;
import com.avadhoot.workforge.workflow.domain.WorkflowStatus;
import com.avadhoot.workforge.workflow.domain.WorkflowTransition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/{workflowId}/statuses")
    public ApiResponse<List<WorkflowStatus>> statuses(@PathVariable Long workflowId) {
        return ApiResponse.success(workflowService.statuses(workflowId));
    }

    @GetMapping("/{workflowId}/transitions")
    public ApiResponse<List<WorkflowTransition>> transitions(@PathVariable Long workflowId,
                                                             @RequestParam(required = false) Long fromStatusId) {
        return ApiResponse.success(workflowService.availableTransitions(workflowId, fromStatusId));
    }
}
