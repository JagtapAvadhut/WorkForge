package com.avadhoot.workforgeai.ai.tools.repository;

import com.avadhoot.workforgeai.ai.tools.model.IssueRecord;
import com.avadhoot.workforgeai.ai.tools.model.ProjectRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class WorkforgeSampleRepository {

    private final JdbcTemplate jdbcTemplate;

    public WorkforgeSampleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ProjectRecord> findProjectByKey(String projectKey) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    SELECT p.project_key, p.name, p.description, p.lead_username,
                           (SELECT COUNT(*) FROM wf_issues i WHERE i.project_key = p.project_key) AS issue_count
                    FROM wf_projects p
                    WHERE UPPER(p.project_key) = UPPER(?)
                    """,
                    projectMapper(),
                    projectKey));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<IssueRecord> findIssueByKey(String issueKey) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    SELECT issue_key, summary, status, priority, assignee, project_key, issue_type, sprint_name
                    FROM wf_issues
                    WHERE UPPER(issue_key) = UPPER(?)
                    """,
                    issueMapper(),
                    issueKey));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<IssueRecord> searchIssues(String projectKey, String status, String assignee, int limit) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT issue_key, summary, status, priority, assignee, project_key, issue_type, sprint_name
                FROM wf_issues
                WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();

        if (projectKey != null && !projectKey.isBlank()) {
            sql.append(" AND UPPER(project_key) = UPPER(?)");
            args.add(projectKey.trim());
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND UPPER(status) = UPPER(?)");
            args.add(status.trim());
        }
        if (assignee != null && !assignee.isBlank()) {
            sql.append(" AND LOWER(assignee) = LOWER(?)");
            args.add(assignee.trim());
        }
        sql.append(" ORDER BY issue_key ASC LIMIT ?");
        args.add(limit);

        return jdbcTemplate.query(sql.toString(), issueMapper(), args.toArray());
    }

    private static RowMapper<ProjectRecord> projectMapper() {
        return (rs, rowNum) -> new ProjectRecord(
                rs.getString("project_key"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("lead_username"),
                rs.getLong("issue_count"));
    }

    private static RowMapper<IssueRecord> issueMapper() {
        return (rs, rowNum) -> new IssueRecord(
                rs.getString("issue_key"),
                rs.getString("summary"),
                rs.getString("status"),
                rs.getString("priority"),
                rs.getString("assignee"),
                rs.getString("project_key"),
                rs.getString("issue_type"),
                rs.getString("sprint_name"));
    }
}
