package com.avadhoot.workforgeai.ai.rag;

import com.avadhoot.workforgeai.ai.rag.dto.RagQueryRequest;
import com.avadhoot.workforgeai.ai.rag.dto.RagQueryResponse;
import com.avadhoot.workforgeai.ai.rag.dto.RagSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Retrieval-Augmented Generation: search → ground prompt → LLM.
 * Separate from {@code AiChatService} Phase-1/2 chat.
 */
@Service
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final RagPromptBuilder promptBuilder;
    private final int defaultTopK;
    private final int maxTopK;
    private final double similarityThreshold;
    private final String noContextMessage;

    public RagService(
            VectorStore vectorStore,
            ChatClient chatClient,
            RagPromptBuilder promptBuilder,
            @Value("${workforge.ai.rag.default-top-k:5}") int defaultTopK,
            @Value("${workforge.ai.rag.max-top-k:20}") int maxTopK,
            @Value("${workforge.ai.rag.similarity-threshold:0.45}") double similarityThreshold,
            @Value("${workforge.ai.rag.no-context-message:I don't have enough information in the available knowledge.}")
            String noContextMessage) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.promptBuilder = promptBuilder;
        this.defaultTopK = defaultTopK;
        this.maxTopK = maxTopK;
        this.similarityThreshold = similarityThreshold;
        this.noContextMessage = noContextMessage;
    }

    public RagQueryResponse query(RagQueryRequest request) {
        if (request == null || !StringUtils.hasText(request.question())) {
            throw new IllegalArgumentException("question must not be blank");
        }

        int topK = resolveTopK(request.topK());
        String question = request.question().trim();

        List<RagSource> sources = retrieve(question, topK);
        var ctx = com.avadhoot.workforgeai.ai.observability.AiExecutionContext.current();
        if (ctx != null) {
            ctx.addRetrievals(sources.size());
        }
        if (sources.isEmpty()) {
            return new RagQueryResponse(noContextMessage, List.of());
        }

        String answer = chatClient.prompt()
                .system(promptBuilder.systemPrompt())
                .user(promptBuilder.userPrompt(question, sources))
                .call()
                .content();

        if (!StringUtils.hasText(answer)) {
            throw new IllegalStateException("The model returned an empty RAG response");
        }

        return new RagQueryResponse(answer.trim(), sources);
    }

    /**
     * Exposed for tests: retrieval + threshold + dedupe without calling the LLM.
     */
    List<RagSource> retrieve(String question, int topK) {
        List<Document> hits = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build());

        Map<UUID, RagSource> unique = new LinkedHashMap<>();
        for (Document document : hits) {
            RagSource source = toSource(document);
            if (source.similarity() < similarityThreshold) {
                continue;
            }
            unique.putIfAbsent(source.id(), source);
        }

        List<RagSource> ranked = new ArrayList<>(unique.values());
        ranked.sort(Comparator.comparingDouble(RagSource::similarity).reversed());
        return List.copyOf(ranked);
    }

    private int resolveTopK(Integer topK) {
        int value = topK == null ? defaultTopK : topK;
        if (value <= 0) {
            throw new IllegalArgumentException("topK must be greater than 0");
        }
        if (value > maxTopK) {
            throw new IllegalArgumentException("topK must be at most " + maxTopK);
        }
        return value;
    }

    private static RagSource toSource(Document document) {
        UUID id = UUID.fromString(document.getId());
        double similarity = document.getScore() == null ? 0.0 : document.getScore();
        Map<String, Object> metadata = document.getMetadata() == null ? Map.of() : document.getMetadata();
        return new RagSource(id, document.getText(), metadata, similarity);
    }

    double similarityThreshold() {
        return similarityThreshold;
    }

    String noContextMessage() {
        return noContextMessage;
    }
}
