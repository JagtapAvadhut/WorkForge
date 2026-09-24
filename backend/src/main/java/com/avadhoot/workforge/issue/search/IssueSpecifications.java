package com.avadhoot.workforge.issue.search;

import com.avadhoot.workforge.issue.domain.Issue;
import org.springframework.data.jpa.domain.Specification;

/**
 * Type-safe, parameterised {@link Specification} builders for {@link Issue}.
 * User input is only ever bound as query parameters, never concatenated into SQL.
 */
public final class IssueSpecifications {

    private IssueSpecifications() {
    }

    public static Specification<Issue> projectId(Long projectId) {
        return (root, query, cb) -> cb.equal(root.get("projectId"), projectId);
    }

    public static Specification<Issue> statusId(Long statusId) {
        return (root, query, cb) -> cb.equal(root.get("status").get("id"), statusId);
    }

    public static Specification<Issue> statusCategory(com.avadhoot.workforge.issue.domain.StatusCategory category) {
        return (root, query, cb) -> cb.equal(root.get("status").get("category"), category);
    }

    public static Specification<Issue> sprintIdIsNull() {
        return (root, query, cb) -> cb.isNull(root.get("sprintId"));
    }

    public static Specification<Issue> statusName(String name) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("status").get("name")), name.toLowerCase());
    }

    public static Specification<Issue> priorityName(String name) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("priority").get("name")), name.toLowerCase());
    }

    public static Specification<Issue> typeName(String name) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("issueType").get("name")), name.toLowerCase());
    }

    public static Specification<Issue> assigneeId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("assigneeId"), userId);
    }

    public static Specification<Issue> reporterId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("reporterId"), userId);
    }

    public static Specification<Issue> sprintId(Long sprintId) {
        return (root, query, cb) -> cb.equal(root.get("sprintId"), sprintId);
    }

    public static Specification<Issue> summaryContains(String text) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("summary")), "%" + text.toLowerCase() + "%");
    }

    public static Specification<Issue> not(Specification<Issue> spec) {
        return (root, query, cb) -> cb.not(spec.toPredicate(root, query, cb));
    }
}
