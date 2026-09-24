package com.avadhoot.workforgeai.ai.memory.repository;

import com.avadhoot.workforgeai.ai.memory.model.MemoryRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MemoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public MemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MemoryRecord insert(
            String sessionId,
            String category,
            String content,
            String source,
            int importance) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO ai_memories (id, session_id, category, content, source, importance)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                id, sessionId, category, content, source, importance);
        return findById(id).orElseThrow();
    }

    public Optional<MemoryRecord> findById(UUID id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    SELECT id, session_id, category, content, source, importance, active, created_at, updated_at
                    FROM ai_memories
                    WHERE id = ?
                    """,
                    mapper(),
                    id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<MemoryRecord> list(
            String sessionId,
            String category,
            Integer minImportance,
            int limit) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT id, session_id, category, content, source, importance, active, created_at, updated_at
                FROM ai_memories
                WHERE session_id = ? AND active = TRUE
                """);
        List<Object> args = new ArrayList<>();
        args.add(sessionId);
        if (StringUtils.hasText(category)) {
            sql.append(" AND LOWER(category) = LOWER(?)");
            args.add(category.trim());
        }
        if (minImportance != null) {
            sql.append(" AND importance >= ?");
            args.add(minImportance);
        }
        sql.append(" ORDER BY importance DESC, updated_at DESC LIMIT ?");
        args.add(limit);
        return jdbcTemplate.query(sql.toString(), mapper(), args.toArray());
    }

    public boolean softDelete(UUID id) {
        return jdbcTemplate.update(
                """
                UPDATE ai_memories
                SET active = FALSE, updated_at = NOW()
                WHERE id = ? AND active = TRUE
                """,
                id) > 0;
    }

    public boolean hardDelete(UUID id) {
        return jdbcTemplate.update("DELETE FROM ai_memories WHERE id = ?", id) > 0;
    }

    private static RowMapper<MemoryRecord> mapper() {
        return (rs, rowNum) -> new MemoryRecord(
                rs.getObject("id", UUID.class),
                rs.getString("session_id"),
                rs.getString("category"),
                rs.getString("content"),
                rs.getString("source"),
                rs.getInt("importance"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
