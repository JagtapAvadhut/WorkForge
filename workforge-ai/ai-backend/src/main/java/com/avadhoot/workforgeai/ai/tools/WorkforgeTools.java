package com.avadhoot.workforgeai.ai.tools;

import com.avadhoot.workforgeai.ai.tools.model.IssueRecord;
import com.avadhoot.workforgeai.ai.tools.model.ProjectRecord;
import com.avadhoot.workforgeai.ai.tools.service.IssueToolService;
import com.avadhoot.workforgeai.ai.tools.service.ProjectToolService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only Spring AI tools. Tools call application services — never repositories directly.
 */
@Component
public class WorkforgeTools {

    private final IssueToolService issueToolService;
    private final ProjectToolService projectToolService;

    public WorkforgeTools(IssueToolService issueToolService, ProjectToolService projectToolService) {
        this.issueToolService = issueToolService;
        this.projectToolService = projectToolService;
    }

    @Tool(
            name = "getIssue",
            description = """
                    Look up a single WorkForge issue by its exact issue key (for example MWS-1 or OPS-2).
                    Use this when the user asks about one specific issue key.
                    Returns issueKey, summary, status, priority, assignee, project, issueType, and sprint.
                    If the issue does not exist, returns a not-found result.
                    """)
    public Map<String, Object> getIssue(
            @ToolParam(description = "Exact issue key such as MWS-1. Required.", required = true)
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

    @Tool(
            name = "searchIssues",
            description = """
                    Search WorkForge issues with optional filters.
                    Use this when the user asks for a list of issues (open issues, issues by assignee, issues in a project).
                    Parameters:
                    - projectKey: project key such as MWS or OPS; omit/null for all projects
                    - status: issue status such as OPEN, IN_PROGRESS, DONE; omit/null for any status
                    - assignee: username such as avadhoot; omit/null for any assignee
                    - limit: max results to return (positive integer; server enforces a maximum)
                    Returns a concise list of matching issue summaries.
                    """)
    public Map<String, Object> searchIssues(
            @ToolParam(description = "Optional project key filter, e.g. MWS. Null/blank means any project.", required = false)
            String projectKey,
            @ToolParam(description = "Optional status filter, e.g. OPEN, IN_PROGRESS, DONE. Null/blank means any status.", required = false)
            String status,
            @ToolParam(description = "Optional assignee username filter, e.g. avadhoot. Null/blank means any assignee.", required = false)
            String assignee,
            @ToolParam(description = "Maximum number of issues to return. Defaults to 10. Must be > 0 and within server max.", required = false)
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

    @Tool(
            name = "getProject",
            description = """
                    Look up a WorkForge project by project key (for example MWS).
                    Use this when the user asks about a project, its lead, description, or issue count.
                    Returns key, name, description, lead, and issueCount.
                    If the project does not exist, returns a not-found result.
                    """)
    public Map<String, Object> getProject(
            @ToolParam(description = "Exact project key such as MWS. Required.", required = true)
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
