package com.avadhoot.workforgeai.ai.tools;

import com.avadhoot.workforgeai.ai.tools.model.IssueRecord;
import com.avadhoot.workforgeai.ai.tools.model.ProjectRecord;
import com.avadhoot.workforgeai.ai.tools.repository.WorkforgeSampleRepository;
import com.avadhoot.workforgeai.ai.tools.service.IssueToolService;
import com.avadhoot.workforgeai.ai.tools.service.ProjectToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkforgeToolsTest {

    private WorkforgeSampleRepository repository;
    private WorkforgeTools tools;

    @BeforeEach
    void setUp() {
        repository = mock(WorkforgeSampleRepository.class);
        IssueToolService issueService = new IssueToolService(repository, 50, 10);
        ProjectToolService projectService = new ProjectToolService(repository);
        tools = new WorkforgeTools(issueService, projectService);
    }

    @Test
    void getIssue_returnsIssueFields() {
        when(repository.findIssueByKey("MWS-1")).thenReturn(Optional.of(
                new IssueRecord("MWS-1", "Fix checkout", "OPEN", "HIGH", "avadhoot", "MWS", "Bug", "Sprint 12")));

        Map<String, Object> result = tools.getIssue("MWS-1");

        assertThat(result.get("found")).isEqualTo(true);
        assertThat(result.get("issueKey")).isEqualTo("MWS-1");
        assertThat(result.get("status")).isEqualTo("OPEN");
        assertThat(result.get("assignee")).isEqualTo("avadhoot");
    }

    @Test
    void getIssue_unknown_returnsNotFound() {
        when(repository.findIssueByKey("MWS-999")).thenReturn(Optional.empty());

        Map<String, Object> result = tools.getIssue("MWS-999");

        assertThat(result.get("found")).isEqualTo(false);
        assertThat(result.get("message").toString()).contains("not found");
    }

    @Test
    void searchIssues_enforcesLimitCap() {
        IssueToolService issueService = new IssueToolService(repository, 50, 10);
        assertThatThrownBy(() -> issueService.searchIssues("MWS", "OPEN", null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
        assertThatThrownBy(() -> issueService.searchIssues("MWS", "OPEN", null, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most");
    }

    @Test
    void searchIssues_returnsSummaries() {
        when(repository.searchIssues(eq("MWS"), eq("OPEN"), eq("avadhoot"), eq(10)))
                .thenReturn(List.of(
                        new IssueRecord("MWS-1", "Fix checkout", "OPEN", "HIGH", "avadhoot", "MWS", "Bug", "Sprint 12")));

        Map<String, Object> result = tools.searchIssues("MWS", "OPEN", "avadhoot", 10);

        assertThat(result.get("count")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        assertThat(issues.getFirst().get("issueKey")).isEqualTo("MWS-1");
        assertThat(issues.getFirst()).doesNotContainKey("issueType");
    }

    @Test
    void getProject_returnsProjectWithIssueCount() {
        when(repository.findProjectByKey("MWS")).thenReturn(Optional.of(
                new ProjectRecord("MWS", "Mobile Web Store", "demo", "avadhoot", 5)));

        Map<String, Object> result = tools.getProject("MWS");

        assertThat(result.get("found")).isEqualTo(true);
        assertThat(result.get("key")).isEqualTo("MWS");
        assertThat(result.get("issueCount")).isEqualTo(5L);
    }

    @Test
    void searchIssues_invalidLimit_returnsErrorPayload() {
        Map<String, Object> result = tools.searchIssues("MWS", "OPEN", null, -1);
        assertThat(result.get("error").toString()).contains("limit");
        assertThat(result.get("count")).isEqualTo(0);
    }

    @Test
    void searchIssues_nullFilters_allowed() {
        when(repository.searchIssues(isNull(), isNull(), isNull(), eq(10))).thenReturn(List.of());
        Map<String, Object> result = tools.searchIssues(null, null, null, null);
        assertThat(result.get("count")).isEqualTo(0);
        verify(repository).searchIssues(isNull(), isNull(), isNull(), eq(10));
    }
}
