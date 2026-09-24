package com.avadhoot.workforgeai.ai.multiagent.node;

import com.avadhoot.workforgeai.ai.multiagent.MultiAgentNames;
import com.avadhoot.workforgeai.ai.multiagent.MultiAgentPrompt;
import com.avadhoot.workforgeai.ai.multiagent.MultiAgentState;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces the final answer from structured specialist results (or a supervisor draft).
 */
public class FinalizeMultiAgentNode implements NodeAction<MultiAgentState> {

    private final ChatClient chatClient;

    public FinalizeMultiAgentNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(MultiAgentState state) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(MultiAgentState.CURRENT_NODE, MultiAgentNames.FINALIZE);

        String answer = state.finalAnswer();
        if (!StringUtils.hasText(answer) || !state.specialistResults().isEmpty()) {
            // Prefer synthesis when specialists contributed
            if (!state.specialistResults().isEmpty() || !StringUtils.hasText(answer)) {
                answer = synthesize(state, answer);
            }
        }
        if (!StringUtils.hasText(answer)) {
            answer = "Unable to produce a final answer.";
        }

        String status = state.status();
        if (MultiAgentState.STATUS_RUNNING.equals(status)) {
            status = MultiAgentState.STATUS_COMPLETED;
        }

        updates.put(MultiAgentState.FINAL_ANSWER, answer.trim());
        updates.put(MultiAgentState.COMPLETED, Boolean.TRUE);
        updates.put(MultiAgentState.STATUS, status);
        updates.put(MultiAgentState.NEXT_ROUTE, MultiAgentNames.ROUTE_FINALIZE);
        updates.put(MultiAgentState.STEPS, List.of(MultiAgentState.step(
                MultiAgentNames.FINALIZE, "completed", Map.of())));
        return updates;
    }

    private String synthesize(MultiAgentState state, String draft) {
        try {
            String content = chatClient.prompt()
                    .system(MultiAgentPrompt.finalizeSystem())
                    .user(MultiAgentPrompt.finalizeUser(state)
                            + (StringUtils.hasText(draft) ? "\n\nSupervisor draft:\n" + draft : ""))
                    .call()
                    .content();
            if (StringUtils.hasText(content)) {
                return content.trim();
            }
        } catch (Exception ignored) {
            // fall through
        }
        if (StringUtils.hasText(draft)) {
            return draft.trim();
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> result : state.specialistResults()) {
            sb.append(result.getOrDefault("agent", "")).append(": ")
                    .append(result.getOrDefault("summary", "")).append('\n');
            Object findings = result.get("findings");
            if (findings instanceof List<?> list) {
                for (Object finding : list) {
                    sb.append("- ").append(finding).append('\n');
                }
            }
        }
        return sb.toString().trim();
    }
}
