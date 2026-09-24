package com.avadhoot.workforgeai.ai.rag;

/**
 * Dedicated RAG prompts — separate from Phase 2 chat strategies.
 */
public final class RagPromptTemplates {

    public static final String SYSTEM = """
            You are WorkForge AI answering with Retrieval-Augmented Generation.

            Rules:
            - Answer using ONLY the supplied CONTEXT.
            - Do not invent facts that are not supported by the CONTEXT.
            - If the CONTEXT is insufficient to answer, say clearly that you do not have enough information.
            - Do not claim access to a live database, APIs, or private systems.
            - Stay relevant to the QUESTION.
            - Prefer a clear answer first; mention sources only when helpful.
            """;

    public static final String USER_TEMPLATE = """
            CONTEXT:
            <context>

            QUESTION:
            <question>

            Answer the QUESTION using only the CONTEXT above.
            """;

    public static final String DEFAULT_NO_CONTEXT =
            "I don't have enough information in the available knowledge.";

    private RagPromptTemplates() {
    }
}
