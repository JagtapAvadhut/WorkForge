package com.avadhoot.workforgeai.ai.multiagent;

import com.avadhoot.workforgeai.ai.rag.RagService;
import com.avadhoot.workforgeai.ai.rag.dto.RagQueryRequest;
import com.avadhoot.workforgeai.ai.rag.dto.RagQueryResponse;
import com.avadhoot.workforgeai.ai.rag.dto.RagSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Knowledge/RAG specialist — reuses Phase 5 RagService only.
 */
@Component
public class KnowledgeRagAgent {

    private final RagService ragService;

    public KnowledgeRagAgent(RagService ragService) {
        this.ragService = ragService;
    }

    public SpecialistResult investigate(String task, String userRequest) {
        String question = StringUtils.hasText(task) ? task.trim() : (userRequest == null ? "" : userRequest.trim());
        if (!StringUtils.hasText(question)) {
            return SpecialistResult.failure(MultiAgentNames.KNOWLEDGE_AGENT, "No question provided for RAG.");
        }
        try {
            RagQueryResponse response = ragService.query(new RagQueryRequest(question, 5));
            List<Map<String, Object>> sources = new ArrayList<>();
            List<String> findings = new ArrayList<>();
            findings.add(response.answer());
            if (response.sources() != null) {
                for (RagSource source : response.sources()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", source.id());
                    row.put("content", truncate(source.content(), 240));
                    row.put("similarity", source.similarity());
                    if (source.metadata() != null) {
                        row.put("metadata", source.metadata());
                    }
                    sources.add(row);
                }
            }
            boolean grounded = response.sources() != null && !response.sources().isEmpty();
            return new SpecialistResult(
                    MultiAgentNames.KNOWLEDGE_AGENT,
                    SpecialistResult.SUCCESS,
                    grounded ? "Retrieved grounded knowledge with " + sources.size() + " sources"
                            : "No strong document matches; returned fallback knowledge reply",
                    findings,
                    sources,
                    List.of());
        } catch (Exception ex) {
            return SpecialistResult.failure(
                    MultiAgentNames.KNOWLEDGE_AGENT,
                    "RAG failed: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max - 3) + "...";
    }
}
