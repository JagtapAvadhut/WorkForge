package com.avadhoot.workforge.workflow;

import com.avadhoot.workforge.exception.InvalidTransitionException;
import com.avadhoot.workforge.issue.IssueService;
import com.avadhoot.workforge.issue.domain.Status;
import com.avadhoot.workforge.issue.dto.IssueDtos.CreateIssueRequest;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.issue.repository.StatusRepository;
import com.avadhoot.workforge.project.ProjectService;
import com.avadhoot.workforge.project.dto.ProjectDtos.CreateProjectRequest;
import com.avadhoot.workforge.workflow.domain.Workflow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@ActiveProfiles("test")
class WorkflowTransitionTest {

    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private IssueService issueService;

    private Long workflowId;
    private Long todo;
    private Long inProgress;
    private Long inReview;
    private Long done;

    @BeforeEach
    void setUp() {
        Workflow workflow = workflowService.defaultWorkflow();
        workflowId = workflow.getId();
        todo = status("To Do");
        inProgress = status("In Progress");
        inReview = status("In Review");
        done = status("Done");
    }

    private Long status(String name) {
        return statusRepository.findByNameIgnoreCase(name).map(Status::getId).orElseThrow();
    }

    @Test
    void allowedTransition_isPermitted() {
        assertThat(workflowService.canTransition(workflowId, todo, inProgress)).isTrue();
        assertThat(workflowService.canTransition(workflowId, inProgress, inReview)).isTrue();
        assertThat(workflowService.canTransition(workflowId, inReview, done)).isTrue();
    }

    @Test
    void disallowedTransition_isRejected() {
        assertThat(workflowService.canTransition(workflowId, todo, done)).isFalse();
        assertThatExceptionOfType(InvalidTransitionException.class)
                .isThrownBy(() -> workflowService.validateTransition(workflowId, todo, done));
    }

    @Test
    void issueTransition_followsWorkflow() {
        String key = "WF" + (System.nanoTime() % 100000);
        projectService.create(new CreateProjectRequest(key, "Workflow Project", null, "1", null));

        IssueResponse issue = issueService.create(new CreateIssueRequest(
                key, "Test issue", null, "TASK", "MEDIUM", null,
                null, null, null, null, null, null));
        assertThat(issue.status().name()).isEqualTo("To Do");

        IssueResponse moved = issueService.transition(issue.key(), inProgress);
        assertThat(moved.status().name()).isEqualTo("In Progress");

        assertThatExceptionOfType(InvalidTransitionException.class)
                .isThrownBy(() -> issueService.transition(issue.key(), done));

        issueService.transition(issue.key(), inReview);
        IssueResponse finished = issueService.transition(issue.key(), done);
        assertThat(finished.status().name()).isEqualTo("Done");
    }
}
