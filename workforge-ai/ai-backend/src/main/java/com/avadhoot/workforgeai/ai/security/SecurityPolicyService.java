package com.avadhoot.workforgeai.ai.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Central AI security decisions for WorkForge AI (local learning scope).
 * Prompt-injection heuristics are intentionally simple and not foolproof.
 */
@Service
public class SecurityPolicyService {

    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
            Pattern.compile("(?i)\\b(rm\\s+-rf|drop\\s+table|format\\s+c:|powershell\\s+-enc)\\b"),
            Pattern.compile("(?i)\\b(execute\\s+any\\s+available\\s+tool|run\\s+shell|open\\s+a\\s+shell)\\b"),
            Pattern.compile("(?i)\\b(disable\\s+your\\s+safety\\s+rules|bypass\\s+all\\s+guards)\\b"),
            Pattern.compile("(?i)\\b(delete\\s+all\\s+issues|wipe\\s+the\\s+database)\\b")
    );

    private static final List<Pattern> SUSPICIOUS_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|prior|above)\\s+instructions"),
            Pattern.compile("(?i)reveal\\s+(your\\s+)?(system\\s+)?prompt"),
            Pattern.compile("(?i)show\\s+(me\\s+)?(the\\s+)?system\\s+prompt"),
            Pattern.compile("(?i)disregard\\s+(your\\s+)?(rules|instructions|guardrails)"),
            Pattern.compile("(?i)jailbreak"),
            Pattern.compile("(?i)pretend\\s+you\\s+have\\s+no\\s+restrictions")
    );

    private static final Set<String> DANGEROUS_TOOLS = Set.of(
            "delete", "update", "create", "shell", "exec", "filesystem", "email", "payment",
            "deleteissue", "updateissue", "createissue", "writefile", "runcommand"
    );

    private final Set<String> allowedTools;
    private final int maxInputChars;
    private final int maxToolCalls;
    private final int maxAgentIterations;
    private final int maxSpecialistCalls;
    private final boolean blockSuspicious;

    public SecurityPolicyService(
            @Value("${workforge.ai.mcp.allowed-tools:getIssue,searchIssues,getProject}") String allowedToolsCsv,
            @Value("${workforge.ai.security.max-input-chars:4000}") int maxInputChars,
            @Value("${workforge.ai.security.max-tool-calls:10}") int maxToolCalls,
            @Value("${workforge.ai.security.max-agent-iterations:5}") int maxAgentIterations,
            @Value("${workforge.ai.security.max-specialist-calls:10}") int maxSpecialistCalls,
            @Value("${workforge.ai.security.block-suspicious:true}") boolean blockSuspicious) {
        this.allowedTools = Arrays.stream(allowedToolsCsv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        this.maxInputChars = Math.max(100, maxInputChars);
        this.maxToolCalls = Math.max(1, maxToolCalls);
        this.maxAgentIterations = Math.max(1, maxAgentIterations);
        this.maxSpecialistCalls = Math.max(1, maxSpecialistCalls);
        this.blockSuspicious = blockSuspicious;
    }

    public SecurityCheckResult checkInput(String input) {
        if (!StringUtils.hasText(input)) {
            return SecurityCheckResult.blocked("Input must not be blank");
        }
        String text = input.trim();
        if (text.length() > maxInputChars) {
            return SecurityCheckResult.blocked(
                    "Input exceeds maximum length of " + maxInputChars + " characters");
        }
        if (containsSecretLike(text)) {
            return SecurityCheckResult.blocked("Input appears to contain secrets or credentials");
        }
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return SecurityCheckResult.blocked(
                        "Blocked by safety policy (dangerous instruction pattern)");
            }
        }
        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return SecurityCheckResult.suspicious(
                        "Suspicious prompt-injection style pattern detected (heuristic, not perfect)");
            }
        }
        return SecurityCheckResult.safe();
    }

    /**
     * Enforces policy before AI feature execution.
     */
    public void assertAllowedForExecution(String input) {
        SecurityCheckResult result = checkInput(input);
        if (result.status() == SecurityStatus.BLOCKED) {
            throw new SecurityBlockedException(result.status(), result.reason());
        }
        if (result.status() == SecurityStatus.SUSPICIOUS && blockSuspicious) {
            throw new SecurityBlockedException(result.status(), result.reason());
        }
    }

    public boolean isToolAllowed(String toolName) {
        if (!StringUtils.hasText(toolName)) {
            return false;
        }
        String normalized = toolName.trim().toLowerCase(Locale.ROOT);
        if (isDangerousTool(normalized)) {
            return false;
        }
        return allowedTools.contains(normalized);
    }

    public void assertToolAllowed(String toolName) {
        if (!isToolAllowed(toolName)) {
            throw new SecurityBlockedException(
                    SecurityStatus.BLOCKED,
                    "Tool not allowlisted: " + toolName);
        }
    }

    public boolean isDangerousTool(String toolName) {
        if (!StringUtils.hasText(toolName)) {
            return true;
        }
        String normalized = toolName.trim().toLowerCase(Locale.ROOT);
        for (String dangerous : DANGEROUS_TOOLS) {
            if (normalized.equals(dangerous) || normalized.contains(dangerous)) {
                return true;
            }
        }
        return false;
    }

    public String sanitizeErrorMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "AI request failed";
        }
        String cleaned = message
                .replaceAll("(?i)password[=:].*", "password=[redacted]")
                .replaceAll("(?i)(bearer\\s+)[a-z0-9._\\-]+", "$1[redacted]")
                .replaceAll("(?i)(jwt|token|secret)[=:]\\s*\\S+", "$1=[redacted]")
                .replaceAll("(?i)jdbc:[^\\s]+", "jdbc:[redacted]")
                .replaceAll("[A-Za-z]:\\\\[^\\s]+", "[path]")
                .replaceAll("/home/[^\\s]+", "[path]")
                .replaceAll("/Users/[^\\s]+", "[path]");
        if (cleaned.length() > 240) {
            return cleaned.substring(0, 237) + "...";
        }
        return cleaned;
    }

    public String sanitizeSummary(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        String cleaned = sanitizeErrorMessage(input.trim().replaceAll("\\s+", " "));
        return cleaned.length() > 240 ? cleaned.substring(0, 237) + "..." : cleaned;
    }

    public Map<String, Object> policies() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("allowedTools", List.copyOf(allowedTools));
        map.put("dangerousToolsBlocked", List.copyOf(DANGEROUS_TOOLS));
        map.put("maxInputChars", maxInputChars);
        map.put("maxToolCalls", maxToolCalls);
        map.put("maxAgentIterations", maxAgentIterations);
        map.put("maxSpecialistCalls", maxSpecialistCalls);
        map.put("blockSuspicious", blockSuspicious);
        map.put("promptInjectionNote",
                "Heuristic pattern matching only — not a perfect classifier");
        map.put("mcpAllowlist", List.copyOf(allowedTools));
        return map;
    }

    public int maxInputChars() {
        return maxInputChars;
    }

    public int maxToolCalls() {
        return maxToolCalls;
    }

    public int maxAgentIterations() {
        return maxAgentIterations;
    }

    public int maxSpecialistCalls() {
        return maxSpecialistCalls;
    }

    public Set<String> allowedTools() {
        return Set.copyOf(allowedTools);
    }

    private static boolean containsSecretLike(String text) {
        return text.matches("(?is).*\\b(api[_-]?key|password|refresh[_-]?token)\\s*[:=].*")
                || text.matches("(?is).*\\beyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\b.*");
    }
}
