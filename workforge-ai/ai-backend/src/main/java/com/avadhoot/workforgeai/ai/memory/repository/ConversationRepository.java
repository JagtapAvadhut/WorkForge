package com.avadhoot.workforgeai.ai.memory.repository;

import com.avadhoot.workforgeai.ai.memory.model.ConversationRecord;
import com.avadhoot.workforgeai.ai.memory.model.MessageRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ConversationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ConversationRecord insert(String sessionId, String title) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO ai_conversations (id, session_id, title)
                VALUES (?, ?, ?)
                """,
                id, sessionId, title);
        return findById(id).orElseThrow();
    }

    public Optional<ConversationRecord> findById(UUID id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    SELECT id, session_id, title, created_at, updated_at
                    FROM ai_conversations
                    WHERE id = ?
                    """,
                    conversationMapper(),
                    id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<ConversationRecord> listBySession(String sessionId, int limit) {
        return jdbcTemplate.query(
                """
                SELECT id, session_id, title, created_at, updated_at
                FROM ai_conversations
                WHERE session_id = ?
                ORDER BY updated_at DESC
                LIMIT ?
                """,
                conversationMapper(),
                sessionId,
                limit);
    }

    public void touch(UUID id) {
        jdbcTemplate.update(
                "UPDATE ai_conversations SET updated_at = NOW() WHERE id = ?",
                id);
    }

    public void updateTitleIfBlank(UUID id, String title) {
        jdbcTemplate.update(
                """
                UPDATE ai_conversations
                SET title = ?, updated_at = NOW()
                WHERE id = ? AND (title IS NULL OR title = '')
                """,
                title, id);
    }

    public boolean delete(UUID id) {
        return jdbcTemplate.update("DELETE FROM ai_conversations WHERE id = ?", id) > 0;
    }

    public MessageRecord insertMessage(UUID conversationId, String role, String content) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO ai_messages (id, conversation_id, role, content)
                VALUES (?, ?, ?, ?)
                """,
                id, conversationId, role, content);
        touch(conversationId);
        return findMessage(id).orElseThrow();
    }

    public Optional<MessageRecord> findMessage(UUID id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    SELECT id, conversation_id, role, content, created_at
                    FROM ai_messages WHERE id = ?
                    """,
                    messageMapper(),
                    id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<MessageRecord> listMessages(UUID conversationId) {
        return jdbcTemplate.query(
                """
                SELECT id, conversation_id, role, content, created_at
                FROM ai_messages
                WHERE conversation_id = ?
                ORDER BY created_at ASC
                """,
                messageMapper(),
                conversationId);
    }

    public List<MessageRecord> listRecentMessages(UUID conversationId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<MessageRecord> newestFirst = jdbcTemplate.query(
                """
                SELECT id, conversation_id, role, content, created_at
                FROM ai_messages
                WHERE conversation_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                messageMapper(),
                conversationId,
                limit);
        return newestFirst.reversed();
    }

    private static RowMapper<ConversationRecord> conversationMapper() {
        return (rs, rowNum) -> new ConversationRecord(
                rs.getObject("id", UUID.class),
                rs.getString("session_id"),
                rs.getString("title"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private static RowMapper<MessageRecord> messageMapper() {
        return (rs, rowNum) -> new MessageRecord(
                rs.getObject("id", UUID.class),
                rs.getObject("conversation_id", UUID.class),
                rs.getString("role"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toInstant());
    }
}
