package com.avadhoot.workforgeai.ai.multiagent;

import com.avadhoot.workforgeai.ai.multiagent.SpecialistToolBridge.ToolCallOutcome;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Project analysis specialist — MCP/tools only.
 */
@Component
public class ProjectAnalysisAgent {

    private static final Pattern ISSUE_KEY = Pattern.compile("\\b([A-Z][A-Z0-9]+-\\d+)\\b");
    private static final Pattern PROJECT_HINT = Pattern.compile(
            "\\b(?:project\\s+)?([A-Z][A-Z0-9]{1,10})\\b");

    private final SpecialistToolBridge toolBridge;

    public ProjectAnalysisAgent(SpecialistToolBridge toolBridge) {
        this.toolBridge = toolBridge;
    }

    public SpecialistResult analyze(String task, String userRequest) {
        String text = join(task, userRequest);
        String projectKey = resolveProjectKey(text);
        if (!StringUtils.hasText(projectKey)) {
            return SpecialistResult.failure(
                    MultiAgentNames.PROJECT_AGENT,
                    "No project key found in the request.");
        }

        List<Map<String, Object>> toolCalls = new ArrayList<>();
        List<String> findings = new ArrayList<>();

        ToolCallOutcome project = toolBridge.call("getProject", Map.of("projectKey", projectKey));
        toolCalls.add(project.toMap());
        if (project.ok()) {
            findings.add("Project " + projectKey + ": " + project.result());
        } else {
            findings.add("Failed to load project " + projectKey + ": " + project.result());
        }

        ToolCallOutcome issues = toolBridge.call("searchIssues", Map.of(
                "projectKey", projectKey,
                "limit", 10));
        toolCalls.add(issues.toMap());
        if (issues.ok()) {
            findings.add("Issue summary for " + projectKey + ": " + issues.result());
        } else {
            findings.add("Issue summary failed for " + projectKey + ": " + issues.result());
        }

        boolean anyOk = toolCalls.stream().anyMatch(call -> Boolean.TRUE.equals(call.get("ok")));
        return new SpecialistResult(
                MultiAgentNames.PROJECT_AGENT,
                anyOk ? SpecialistResult.SUCCESS : SpecialistResult.FAILURE,
                anyOk ? "Project analysis for " + projectKey : "Project analysis failed for " + projectKey,
                findings,
                List.of(),
                toolCalls);
    }

    private static String resolveProjectKey(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher issue = ISSUE_KEY.matcher(text);
        if (issue.find()) {
            String key = issue.group(1);
            return key.substring(0, key.indexOf('-'));
        }
        if (text.toUpperCase().contains("MWS")) {
            return "MWS";
        }
        Matcher hint = PROJECT_HINT.matcher(text.toUpperCase());
        while (hint.find()) {
            String candidate = hint.group(1);
            if (candidate.length() >= 2 && candidate.length() <= 6
                    && !List.of("RAG", "API", "MCP", "LLM", "GET", "AND", "THE", "FOR", "GIVE").contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String join(String task, String userRequest) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(task)) {
            sb.append(task).append('\n');
        }
        if (StringUtils.hasText(userRequest)) {
            sb.append(userRequest);
        }
        return sb.toString().trim();
    }
}
