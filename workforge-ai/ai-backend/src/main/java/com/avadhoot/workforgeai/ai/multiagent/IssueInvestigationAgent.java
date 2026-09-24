package com.avadhoot.workforgeai.ai.multiagent;

import com.avadhoot.workforgeai.ai.multiagent.SpecialistToolBridge.ToolCallOutcome;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Issue investigation specialist — MCP/tools only, no repository access.
 */
@Component
public class IssueInvestigationAgent {

    private static final Pattern ISSUE_KEY = Pattern.compile("\\b([A-Z][A-Z0-9]+-\\d+)\\b");
    private static final Pattern PROJECT_KEY = Pattern.compile("\\bproject\\s+([A-Z][A-Z0-9]{1,10})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BARE_PROJECT = Pattern.compile("\\b([A-Z]{2,10})\\b");

    private final SpecialistToolBridge toolBridge;

    public IssueInvestigationAgent(SpecialistToolBridge toolBridge) {
        this.toolBridge = toolBridge;
    }

    public SpecialistResult investigate(String task, String userRequest) {
        String text = join(task, userRequest);
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        List<String> findings = new ArrayList<>();

        List<String> issueKeys = extractIssueKeys(text);
        for (String issueKey : issueKeys) {
            ToolCallOutcome outcome = toolBridge.call("getIssue", Map.of("issueKey", issueKey));
            toolCalls.add(outcome.toMap());
            if (outcome.ok()) {
                findings.add("Issue " + issueKey + ": " + outcome.result());
            } else {
                findings.add("Failed to load issue " + issueKey + ": " + outcome.result());
            }
        }

        String projectKey = extractProjectKey(text, issueKeys);
        if (StringUtils.hasText(projectKey) && issueKeys.isEmpty()) {
            ToolCallOutcome search = toolBridge.call("searchIssues", Map.of(
                    "projectKey", projectKey,
                    "limit", 10));
            toolCalls.add(search.toMap());
            if (search.ok()) {
                findings.add("Issues in " + projectKey + ": " + search.result());
            } else {
                findings.add("Issue search failed for " + projectKey + ": " + search.result());
            }
        } else if (issueKeys.isEmpty() && !StringUtils.hasText(projectKey)) {
            // Broad open-issue probe only when request clearly asks about issues
            if (text.toLowerCase().contains("issue")) {
                ToolCallOutcome search = toolBridge.call("searchIssues", Map.of("limit", 5));
                toolCalls.add(search.toMap());
                if (search.ok()) {
                    findings.add("Issue search: " + search.result());
                }
            }
        }

        if (findings.isEmpty()) {
            return SpecialistResult.failure(
                    MultiAgentNames.ISSUE_AGENT,
                    "No issue keys or searchable project context found in the request.");
        }

        boolean anyOk = toolCalls.stream().anyMatch(call -> Boolean.TRUE.equals(call.get("ok")));
        String summary = anyOk
                ? "Investigated " + (issueKeys.isEmpty() ? "project issues" : String.join(", ", issueKeys))
                : "Issue investigation tools failed";
        return new SpecialistResult(
                MultiAgentNames.ISSUE_AGENT,
                anyOk ? SpecialistResult.SUCCESS : SpecialistResult.FAILURE,
                summary,
                findings,
                List.of(),
                toolCalls);
    }

    private static List<String> extractIssueKeys(String text) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        Matcher matcher = ISSUE_KEY.matcher(text == null ? "" : text);
        while (matcher.find()) {
            keys.add(matcher.group(1).toUpperCase());
        }
        return List.copyOf(keys);
    }

    private static String extractProjectKey(String text, List<String> issueKeys) {
        if (!issueKeys.isEmpty()) {
            String issue = issueKeys.getFirst();
            int dash = issue.indexOf('-');
            if (dash > 0) {
                return issue.substring(0, dash);
            }
        }
        Matcher named = PROJECT_KEY.matcher(text == null ? "" : text);
        if (named.find()) {
            return named.group(1).toUpperCase();
        }
        // common demo key
        if (text != null && text.toUpperCase().contains("MWS") && !text.toUpperCase().contains("MWS-")) {
            return "MWS";
        }
        Matcher bare = BARE_PROJECT.matcher(text == null ? "" : text);
        while (bare.find()) {
            String candidate = bare.group(1).toUpperCase();
            if (candidate.length() >= 2 && candidate.length() <= 6
                    && !List.of("RAG", "API", "MCP", "LLM", "GET", "AND", "THE", "FOR").contains(candidate)) {
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
