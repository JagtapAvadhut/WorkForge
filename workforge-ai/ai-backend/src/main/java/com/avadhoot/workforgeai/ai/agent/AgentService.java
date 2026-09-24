package com.avadhoot.workforgeai.ai.agent;

import com.avadhoot.workforgeai.ai.agent.dto.AgentRequest;
import com.avadhoot.workforgeai.ai.agent.dto.AgentResponse;
import com.avadhoot.workforgeai.ai.dto.HistoryMessage;
import com.avadhoot.workforgeai.ai.memory.ConversationMemoryFacade;
import com.avadhoot.workforgeai.ai.tools.RecordingToolCallback;
import com.avadhoot.workforgeai.ai.tools.WorkforgeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlled AI agent loop: decide → (optional) tool → observe → repeat → final answer.
 * Tool sequence is chosen dynamically; not hardcoded per question.
 */
@Service
public class AgentService {

    private final ChatClient chatClient;
    private final JsonMapper jsonMapper;
    private final Map<String, ToolCallback> callbacksByName;
    private final int defaultMaxSteps;
    private final ConversationMemoryFacade conversationMemoryFacade;
    private String lastDecisionRaw;

    public AgentService(
            ChatClient chatClient,
            WorkforgeTools workforgeTools,
            JsonMapper jsonMapper,
            ConversationMemoryFacade conversationMemoryFacade,
            @Value("${workforge.ai.agent.max-steps:5}") int defaultMaxSteps) {
        this.chatClient = chatClient;
        this.jsonMapper = jsonMapper;
        this.conversationMemoryFacade = conversationMemoryFacade;
        this.defaultMaxSteps = Math.max(1, defaultMaxSteps);
        ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
                .toolObjects(workforgeTools)
                .build()
                .getToolCallbacks();
        this.callbacksByName = new LinkedHashMap<>();
        for (ToolCallback callback : callbacks) {
            callbacksByName.put(callback.getToolDefinition().name(), callback);
        }
    }

    public AgentResponse run(AgentRequest request) {
        var prepared = conversationMemoryFacade.prepare(
                request.conversationId(),
                request.sessionId(),
                request.message());
        String enriched = enrichMessage(request.message().trim(), prepared);
        AgentResult result = execute(new AgentRequest(enriched, request.maxSteps(), null, null));
        conversationMemoryFacade.completeTurn(
                prepared.conversationId(),
                request.message().trim(),
                result.answer());
        return new AgentResponse(
                result.answer(),
                result.steps(),
                result.stopReason(),
                result.iterations(),
                prepared.conversationIdString());
    }

    private String enrichMessage(String message, ConversationMemoryFacade.PreparedContext prepared) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(prepared.memoryBlock())) {
            sb.append(prepared.memoryBlock()).append("\n\n");
        }
        List<HistoryMessage> history = prepared.recentHistory();
        if (history != null && !history.isEmpty()) {
            sb.append("Recent conversation:\n");
            for (HistoryMessage item : history) {
                sb.append("- ").append(item.role()).append(": ").append(item.content()).append('\n');
            }
            sb.append('\n');
        }
        sb.append("Current user request:\n").append(message);
        return sb.toString().trim();
    }

    AgentResult execute(AgentRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new IllegalArgumentException("message must not be blank");
        }
        int maxSteps = resolveMaxSteps(request.maxSteps());
        AgentState state = new AgentState(request.message().trim());

        while (!state.finished() && state.iteration() < maxSteps) {
            state.incrementIteration();
            AgentDecisionParser.AgentDecision decision = decide(state);
            if (decision == null) {
                String raw = lastDecisionRaw;
                // Broken JSON from local models: one corrective re-ask before giving up.
                if (StringUtils.hasText(raw) && looksLikeJson(raw)) {
                    decision = decideWithCorrection(state, raw);
                }
            }
            if (decision == null) {
                String raw = lastDecisionRaw;
                if (!StringUtils.hasText(raw) || looksLikeJson(raw)) {
                    state.addStep(new AgentStep(
                            state.iteration(),
                            "Could not parse a valid agent decision",
                            "error",
                            null,
                            Map.of(),
                            null,
                            "invalid_decision"));
                    String fallback = synthesizeFinal(state);
                    state.finish(fallback, "invalid_decision");
                    break;
                }
                // Local models sometimes answer conceptually in plain text.
                state.addStep(new AgentStep(
                        state.iteration(),
                        "Model returned a direct answer",
                        "final",
                        null,
                        Map.of(),
                        null,
                        "ok"));
                state.finish(raw.trim(), "completed");
                break;
            }

            if (decision.isFinal()) {
                state.addStep(new AgentStep(
                        state.iteration(),
                        decision.thought(),
                        "final",
                        null,
                        Map.of(),
                        null,
                        "ok"));
                state.finish(decision.answer(), "completed");
                break;
            }

            if (decision.isTool()) {
                ToolCallback callback = callbacksByName.get(decision.tool());
                if (callback == null) {
                    String observation = "Unknown tool: " + decision.tool();
                    state.addObservation(observation);
                    state.addStep(new AgentStep(
                            state.iteration(),
                            decision.thought(),
                            "tool",
                            decision.tool(),
                            decision.arguments(),
                            observation,
                            "unknown_tool"));
                    continue;
                }

                String argsJson = writeArgs(decision.arguments());
                List<RecordingToolCallback.RecordedToolCall> sink = RecordingToolCallback.newSink();
                String observation = new RecordingToolCallback(callback, sink, jsonMapper).call(argsJson);
                state.addObservation(decision.tool() + " => " + observation);
                state.addStep(new AgentStep(
                        state.iteration(),
                        decision.thought(),
                        "tool",
                        decision.tool(),
                        decision.arguments(),
                        summarize(observation),
                        "ok"));
            }
        }

        if (!state.finished()) {
            String answer = synthesizeFinal(state);
            state.finish(answer, "max_steps");
        }

        return new AgentResult(
                state.finalAnswer(),
                List.copyOf(state.steps()),
                state.stopReason(),
                state.iteration());
    }

    private AgentDecisionParser.AgentDecision decide(AgentState state) {
        String content = chatClient.prompt()
                .system(AgentPrompt.DECISION_SYSTEM)
                .user(buildDecisionUserPrompt(state))
                .call()
                .content();
        lastDecisionRaw = content;
        return AgentDecisionParser.parse(content, jsonMapper).orElse(null);
    }

    private AgentDecisionParser.AgentDecision decideWithCorrection(AgentState state, String previousRaw) {
        String content = chatClient.prompt()
                .system(AgentPrompt.DECISION_SYSTEM)
                .user("""
                        Your previous response was not valid agent JSON.
                        Reply with ONLY one JSON object matching the schema (tool or final).
                        No markdown fences. No extra text.

                        Previous invalid response:
                        %s

                        %s
                        """.formatted(truncate(previousRaw, 800), buildDecisionUserPrompt(state)))
                .call()
                .content();
        lastDecisionRaw = content;
        return AgentDecisionParser.parse(content, jsonMapper).orElse(null);
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

    private static boolean looksLikeJson(String content) {
        String trimmed = content == null ? "" : content.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    private String synthesizeFinal(AgentState state) {
        if (state.observations().isEmpty()) {
            String content = chatClient.prompt()
                    .system("""
                            You are WorkForge AI.
                            Answer the user in plain language.
                            Do not output JSON and do not invent live issue/project data.
                            """)
                    .user(state.userMessage())
                    .call()
                    .content();
            return StringUtils.hasText(content) ? content.trim()
                    : "I could not complete the agent run with a reliable answer.";
        }

        String content = chatClient.prompt()
                .system("""
                        You are WorkForge AI Agent.
                        Produce the final answer using the tool observations.
                        Do not invent facts. Do not output JSON.
                        """)
                .user("""
                        User request:
                        %s

                        Observations:
                        %s
                        """.formatted(state.userMessage(), String.join("\n", state.observations())))
                .call()
                .content();
        return StringUtils.hasText(content) ? content.trim()
                : "I gathered tool results but could not produce a final answer.";
    }

    private String buildDecisionUserPrompt(AgentState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("User request:\n").append(state.userMessage()).append("\n\n");
        sb.append("Iteration: ").append(state.iteration()).append("\n\n");
        if (state.observations().isEmpty()) {
            sb.append("Observations so far: (none)\n\n");
        } else {
            sb.append("Observations so far:\n");
            for (String observation : state.observations()) {
                sb.append("- ").append(observation).append('\n');
            }
            sb.append('\n');
        }
        if (!state.steps().isEmpty()) {
            sb.append("Previous steps:\n");
            for (AgentStep step : state.steps()) {
                sb.append("- step ").append(step.stepNumber())
                        .append(" action=").append(step.action());
                if (step.tool() != null) {
                    sb.append(" tool=").append(step.tool());
                }
                sb.append(" status=").append(step.status()).append('\n');
            }
            sb.append('\n');
        }
        sb.append("Decide the next action as a single JSON object.");
        return sb.toString();
    }

    private int resolveMaxSteps(Integer maxSteps) {
        if (maxSteps == null) {
            return defaultMaxSteps;
        }
        if (maxSteps <= 0) {
            throw new IllegalArgumentException("maxSteps must be greater than 0");
        }
        if (maxSteps > 10) {
            throw new IllegalArgumentException("maxSteps must be at most 10");
        }
        return maxSteps;
    }

    private String writeArgs(Map<String, Object> arguments) {
        try {
            return jsonMapper.writeValueAsString(arguments == null ? Map.of() : arguments);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private static String summarize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 500) {
            return trimmed;
        }
        return trimmed.substring(0, 497) + "...";
    }
}
