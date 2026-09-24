package com.avadhoot.workforgeai.ai.document.repository;

import com.avadhoot.workforgeai.ai.document.model.AiDocumentRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AiDocumentRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;
    private final int configuredDimensions;

    public AiDocumentRepository(
            JdbcTemplate jdbcTemplate,
            JsonMapper jsonMapper,
            @Value("${workforge.ai.embeddings.dimensions:768}") int configuredDimensions) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
        this.configuredDimensions = configuredDimensions;
    }

    public Optional<AiDocumentRecord> findById(UUID id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    SELECT id, content, metadata::text AS metadata_json,
                           vector_dims(embedding) AS dims, created_at, updated_at
                    FROM ai_documents
                    WHERE id = ?
                    """,
                    rowMapper(),
                    id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<AiDocumentRecord> findPage(int limit, int offset) {
        return jdbcTemplate.query(
                """
                SELECT id, content, metadata::text AS metadata_json,
                       vector_dims(embedding) AS dims, created_at, updated_at
                FROM ai_documents
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """,
                rowMapper(),
                limit,
                offset);
    }

    public long count() {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_documents", Long.class);
        return total == null ? 0L : total;
    }

    public boolean existsById(UUID id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_documents WHERE id = ?", Long.class, id);
        return count != null && count > 0;
    }

    private RowMapper<AiDocumentRecord> rowMapper() {
        return (rs, rowNum) -> new AiDocumentRecord(
                rs.getObject("id", UUID.class),
                rs.getString("content"),
                readMetadata(rs.getString("metadata_json")),
                readDimensions(rs),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")));
    }

    private int readDimensions(ResultSet rs) throws SQLException {
        int dims = rs.getInt("dims");
        if (rs.wasNull() || dims <= 0) {
            return configuredDimensions;
        }
        return dims;
    }

    private Map<String, Object> readMetadata(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse document metadata JSON", ex);
        }
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
