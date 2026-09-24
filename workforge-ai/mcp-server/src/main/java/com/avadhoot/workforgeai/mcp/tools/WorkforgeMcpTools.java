package com.avadhoot.workforgeai.mcp.tools;

import com.avadhoot.workforgeai.ai.tools.model.IssueRecord;
import com.avadhoot.workforgeai.ai.tools.model.ProjectRecord;
import com.avadhoot.workforgeai.ai.tools.service.IssueToolService;
import com.avadhoot.workforgeai.ai.tools.service.ProjectToolService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only MCP tools. Business logic stays in application services (sample-domain).
 */
@Component
public class WorkforgeMcpTools {

    private final IssueToolService issueToolService;
    private final ProjectToolService projectToolService;

    public WorkforgeMcpTools(IssueToolService issueToolService, ProjectToolService projectToolService) {
        this.issueToolService = issueToolService;
        this.projectToolService = projectToolService;
    }

    @McpTool(
            name = "getIssue",
            description = """
                    Look up a single WorkForge issue by exact issue key (e.g. MWS-1).
                    READ-ONLY. Returns summary, status, priority, assignee, project, type, sprint.
                    """)
    public Map<String, Object> getIssue(
            @McpToolParam(description = "Exact issue key such as MWS-1", required = true)
            String issueKey) {
        try {
            return issueToolService.getIssue(issueKey)
                    .map(this::issueToMap)
                    .orElseGet(() -> Map.of(
                            "found", false,
                            "issueKey", issueKey == null ? "" : issueKey.trim(),
                            "message", "Issue not found"));
        } catch (IllegalArgumentException ex) {
            return Map.of("found", false, "error", ex.getMessage());
        }
    }

    @McpTool(
            name = "searchIssues",
            description = """
                    Search WorkForge issues with optional filters (projectKey, status, assignee, limit).
                    READ-ONLY. Returns a concise list of matching issues.
                    """)
    public Map<String, Object> searchIssues(
            @McpToolParam(description = "Optional project key filter, e.g. MWS", required = false)
            String projectKey,
            @McpToolParam(description = "Optional status filter, e.g. OPEN", required = false)
            String status,
            @McpToolParam(description = "Optional assignee username filter", required = false)
            String assignee,
            @McpToolParam(description = "Max results (positive integer)", required = false)
            Integer limit) {
        try {
            List<IssueRecord> issues = issueToolService.searchIssues(projectKey, status, assignee, limit);
            List<Map<String, Object>> summaries = issues.stream().map(this::issueSummary).toList();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("count", summaries.size());
            result.put("issues", summaries);
            return result;
        } catch (IllegalArgumentException ex) {
            return Map.of("count", 0, "issues", List.of(), "error", ex.getMessage());
        }
    }

    @McpTool(
            name = "getProject",
            description = """
                    Look up a WorkForge project by key (e.g. MWS).
                    READ-ONLY. Returns name, description, lead, and issue count.
                    """)
    public Map<String, Object> getProject(
            @McpToolParam(description = "Exact project key such as MWS", required = true)
            String projectKey) {
        try {
            return projectToolService.getProject(projectKey)
                    .map(this::projectToMap)
                    .orElseGet(() -> Map.of(
                            "found", false,
                            "key", projectKey == null ? "" : projectKey.trim(),
                            "message", "Project not found"));
        } catch (IllegalArgumentException ex) {
            return Map.of("found", false, "error", ex.getMessage());
        }
    }

    private Map<String, Object> issueToMap(IssueRecord issue) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("found", true);
        map.put("issueKey", issue.issueKey());
        map.put("summary", issue.summary());
        map.put("status", issue.status());
        map.put("priority", issue.priority());
        map.put("assignee", issue.assignee());
        map.put("project", issue.project());
        map.put("issueType", issue.issueType());
        map.put("sprint", issue.sprint());
        return map;
    }

    private Map<String, Object> issueSummary(IssueRecord issue) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("issueKey", issue.issueKey());
        map.put("summary", issue.summary());
        map.put("status", issue.status());
        map.put("priority", issue.priority());
        map.put("assignee", issue.assignee());
        map.put("project", issue.project());
        return map;
    }

    private Map<String, Object> projectToMap(ProjectRecord project) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("found", true);
        map.put("key", project.key());
        map.put("name", project.name());
        map.put("description", project.description());
        map.put("lead", project.lead());
        map.put("issueCount", project.issueCount());
        return map;
    }
}
