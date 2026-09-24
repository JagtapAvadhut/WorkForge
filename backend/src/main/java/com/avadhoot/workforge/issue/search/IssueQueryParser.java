package com.avadhoot.workforge.issue.search;

import com.avadhoot.workforge.common.ErrorCode;
import com.avadhoot.workforge.exception.BusinessException;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.issue.domain.Issue;
import com.avadhoot.workforge.project.repository.ProjectRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a deliberately-restricted, JQL-like query into a JPA {@link Specification}.
 * Grammar: {@code <clause> (AND <clause>)*} where a clause is {@code field op value}.
 * Supported operators: {@code =}, {@code !=}, {@code ~} (contains). Only whitelisted
 * fields are accepted and every value is bound as a parameter, so raw user text can
 * never reach the SQL string.
 */
@Component
public class IssueQueryParser {

    private static final Pattern CLAUSE = Pattern.compile(
            "^\\s*(\\w+)\\s*(!=|=|~)\\s*(.+?)\\s*$");

    private final ProjectRepository projectRepository;

    public IssueQueryParser(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Specification<Issue> parse(String query) {
        Specification<Issue> spec = Specification.unrestricted();
        if (!StringUtils.hasText(query)) {
            return spec;
        }
        String[] clauses = query.split("(?i)\\s+AND\\s+");
        for (String raw : clauses) {
            spec = spec.and(parseClause(raw));
        }
        return spec;
    }

    private Specification<Issue> parseClause(String raw) {
        Matcher matcher = CLAUSE.matcher(raw);
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid query clause: " + raw);
        }
        String field = matcher.group(1).toLowerCase();
        String op = matcher.group(2);
        String value = unquote(matcher.group(3));

        Specification<Issue> spec = switch (field) {
            case "project" -> IssueSpecifications.projectId(resolveProjectId(value));
            case "status" -> IssueSpecifications.statusName(value);
            case "priority" -> IssueSpecifications.priorityName(value);
            case "type" -> IssueSpecifications.typeName(value);
            case "assignee" -> IssueSpecifications.assigneeId(parseLong(value));
            case "reporter" -> IssueSpecifications.reporterId(parseLong(value));
            case "sprint" -> IssueSpecifications.sprintId(parseLong(value));
            case "summary" -> IssueSpecifications.summaryContains(value);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unknown field: " + field);
        };

        if ("~".equals(op)) {
            // contains only meaningful for text fields; force summary semantics
            spec = IssueSpecifications.summaryContains(value);
        } else if ("!=".equals(op)) {
            spec = IssueSpecifications.not(spec);
        }
        return spec;
    }

    private Long resolveProjectId(String value) {
        return projectRepository.findByKey(value)
                .map(p -> p.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", value));
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Expected numeric value but got: " + value);
        }
    }

    private String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
