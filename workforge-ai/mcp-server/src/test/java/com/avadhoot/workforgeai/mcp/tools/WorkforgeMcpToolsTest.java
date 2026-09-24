package com.avadhoot.workforgeai.mcp.tools;

import com.avadhoot.workforgeai.ai.tools.model.IssueRecord;
import com.avadhoot.workforgeai.ai.tools.model.ProjectRecord;
import com.avadhoot.workforgeai.ai.tools.service.IssueToolService;
import com.avadhoot.workforgeai.ai.tools.service.ProjectToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkforgeMcpToolsTest {

    private IssueToolService issueToolService;
    private ProjectToolService projectToolService;
    private WorkforgeMcpTools tools;

    @BeforeEach
    void setUp() {
        issueToolService = mock(IssueToolService.class);
        projectToolService = mock(ProjectToolService.class);
        tools = new WorkforgeMcpTools(issueToolService, projectToolService);
    }

    @Test
    void getIssue_returnsIssue() {
        when(issueToolService.getIssue("MWS-1")).thenReturn(Optional.of(
                new IssueRecord("MWS-1", "Fix checkout", "OPEN", "HIGH", "avadhoot", "MWS", "Bug", "Sprint 12")));

        Map<String, Object> result = tools.getIssue("MWS-1");

        assertThat(result.get("found")).isEqualTo(true);
        assertThat(result.get("issueKey")).isEqualTo("MWS-1");
    }

    @Test
    void searchIssues_returnsList() {
        when(issueToolService.searchIssues("MWS", "OPEN", null, null)).thenReturn(List.of(
                new IssueRecord("MWS-1", "Fix checkout", "OPEN", "HIGH", "avadhoot", "MWS", "Bug", "Sprint 12")));

        Map<String, Object> result = tools.searchIssues("MWS", "OPEN", null, null);

        assertThat(result.get("count")).isEqualTo(1);
    }

    @Test
    void getProject_returnsProject() {
        when(projectToolService.getProject("MWS")).thenReturn(Optional.of(
                new ProjectRecord("MWS", "Mobile Web Store", "demo", "avadhoot", 5)));

        Map<String, Object> result = tools.getProject("MWS");

        assertThat(result.get("found")).isEqualTo(true);
        assertThat(result.get("key")).isEqualTo("MWS");
    }

    @Test
    void getIssue_invalidArgs_returnsError() {
        when(issueToolService.getIssue("")).thenThrow(new IllegalArgumentException("issueKey is required"));

        Map<String, Object> result = tools.getIssue("");

        assertThat(result.get("found")).isEqualTo(false);
        assertThat(result.get("error")).isEqualTo("issueKey is required");
    }
}
