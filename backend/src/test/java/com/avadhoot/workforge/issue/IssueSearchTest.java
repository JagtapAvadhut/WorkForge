package com.avadhoot.workforge.issue;

import com.avadhoot.workforge.common.dto.PageResponse;
import com.avadhoot.workforge.exception.BusinessException;
import com.avadhoot.workforge.issue.domain.Status;
import com.avadhoot.workforge.issue.dto.IssueDtos.CreateIssueRequest;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.issue.repository.StatusRepository;
import com.avadhoot.workforge.project.ProjectService;
import com.avadhoot.workforge.project.dto.ProjectDtos.CreateProjectRequest;
import com.avadhoot.workforge.project.dto.ProjectDtos.ProjectResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class IssueSearchTest {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private IssueService issueService;
    @Autowired
    private StatusRepository statusRepository;

    private CreateIssueRequest issue(String projectKey, String summary) {
        return new CreateIssueRequest(
                projectKey, summary, null, "TASK", "MEDIUM", null,
                null, null, null, null, null, null);
    }

    @Test
    void search_filtersByProjectAndStatus() {
        String key = "SEA" + (System.nanoTime() % 100000);
        ProjectResponse project = projectService.create(
                new CreateProjectRequest(key, "Search Project", null, "1", null));

        issueService.create(issue(project.key(), "First todo issue"));
        issueService.create(issue(project.key(), "Second todo issue"));
        IssueResponse third = issueService.create(issue(project.key(), "Will be in progress"));

        Long inProgress = statusRepository.findByNameIgnoreCase("In Progress").map(Status::getId).orElseThrow();
        issueService.transition(third.key(), inProgress);

        PageResponse<IssueResponse> todo = issueService.search(
                "project = " + key + " AND status = \"To Do\"", PageRequest.of(0, 20));
        assertThat(todo.totalElements()).isEqualTo(2);

        PageResponse<IssueResponse> byPriority = issueService.search(
                "project = " + key + " AND priority = Medium", PageRequest.of(0, 20));
        assertThat(byPriority.totalElements()).isEqualTo(3);
    }

    @Test
    void search_withUnknownField_isRejected() {
        assertThatThrownBy(() -> issueService.search("bogus = 5", PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class);
    }
}
