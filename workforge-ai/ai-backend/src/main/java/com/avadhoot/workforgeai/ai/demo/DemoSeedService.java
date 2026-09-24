package com.avadhoot.workforgeai.ai.demo;

import com.avadhoot.workforgeai.ai.document.dto.CreateDocumentRequest;
import com.avadhoot.workforgeai.ai.document.service.DocumentIngestionService;
import com.avadhoot.workforgeai.ai.document.service.DocumentQueryService;
import com.avadhoot.workforgeai.ai.memory.MemoryService;
import com.avadhoot.workforgeai.ai.memory.model.MemoryRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Idempotent demo seed for documents and optional long-term memory.
 */
@Service
public class DemoSeedService {

    public static final List<DemoDocument> DEMO_DOCUMENTS = List.of(
            doc("workforge", "What is WorkForge",
                    "WorkForge is a project and issue tracking system similar to Jira. Teams organize work into projects, issues, sprints, and boards."),
            doc("project", "What is a project",
                    "A project in WorkForge is a container for related issues. Each project has a unique key such as MWS and owns its board and workflows."),
            doc("issue", "What is an issue",
                    "An issue represents a unit of work such as a story, bug, or task. Issues have status, priority, assignee, and may belong to a sprint."),
            doc("sprint", "What is a sprint",
                    "A sprint is a fixed-length development period used to plan and deliver work. Sprint statuses include FUTURE, ACTIVE, and COMPLETE."),
            doc("backlog", "What is a backlog",
                    "A backlog is the ordered list of planned work that has not yet been completed. Teams pull items from the backlog into sprints."),
            doc("board", "What is a board",
                    "A board visualizes issue workflow columns such as To Do, In Progress, and Done so teams can track progress."),
            doc("workflow", "What is a workflow",
                    "A workflow defines allowed status transitions for issues, for example from OPEN to IN_PROGRESS to DONE."),
            doc("mcp", "What is MCP",
                    "MCP (Model Context Protocol) lets an AI client discover and call tools on a separate MCP server using a standard protocol."),
            doc("rag", "What is RAG",
                    "RAG (Retrieval-Augmented Generation) retrieves relevant documents from a vector store and uses them as context for the language model."),
            doc("tool-calling", "What is tool calling",
                    "Tool calling lets a model request structured function calls such as getIssue or getProject instead of inventing data.")
    );

    private final DocumentIngestionService ingestionService;
    private final DocumentQueryService documentQueryService;
    private final MemoryService memoryService;

    public DemoSeedService(
            DocumentIngestionService ingestionService,
            DocumentQueryService documentQueryService,
            MemoryService memoryService) {
        this.ingestionService = ingestionService;
        this.documentQueryService = documentQueryService;
        this.memoryService = memoryService;
    }

    public DemoSeedResult seed(String sessionId) {
        int createdDocs = 0;
        int skippedDocs = 0;
        List<String> topics = new ArrayList<>();

        for (DemoDocument demo : DEMO_DOCUMENTS) {
            if (existsByDemoKey(demo.demoKey())) {
                skippedDocs++;
                topics.add(demo.demoKey() + " (exists)");
                continue;
            }
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("title", demo.title());
            metadata.put("topic", demo.demoKey());
            metadata.put("demoKey", demo.demoKey());
            metadata.put("source", "demo-seed");
            ingestionService.ingest(new CreateDocumentRequest(demo.content(), metadata));
            createdDocs++;
            topics.add(demo.demoKey() + " (created)");
        }

        boolean memoryCreated = false;
        String memoryId = null;
        if (StringUtils.hasText(sessionId)) {
            List<MemoryRecord> existing = memoryService.list(sessionId, "preference", null);
            boolean hasPref = existing.stream().anyMatch(m ->
                    m.content() != null && m.content().toLowerCase().contains("simple explanations"));
            if (!hasPref) {
                MemoryRecord remembered = memoryService.remember(
                        sessionId, "preference", "User prefers simple explanations", 5);
                memoryCreated = true;
                memoryId = remembered.id().toString();
            }
        }

        return new DemoSeedResult(createdDocs, skippedDocs, topics, memoryCreated, memoryId);
    }

    private boolean existsByDemoKey(String demoKey) {
        return documentQueryService.list(0, 100).items().stream()
                .anyMatch(doc -> {
                    Object key = doc.metadata() == null ? null : doc.metadata().get("demoKey");
                    return demoKey.equals(String.valueOf(key));
                });
    }

    private static DemoDocument doc(String key, String title, String content) {
        return new DemoDocument(key, title, content);
    }

    public record DemoDocument(String demoKey, String title, String content) {
    }

    public record DemoSeedResult(
            int documentsCreated,
            int documentsSkipped,
            List<String> documentTopics,
            boolean memoryCreated,
            String memoryId
    ) {
    }
}
