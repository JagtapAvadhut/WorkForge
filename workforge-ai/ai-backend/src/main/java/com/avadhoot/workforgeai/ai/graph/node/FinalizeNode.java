package com.avadhoot.workforgeai.ai.graph.node;

import com.avadhoot.workforgeai.ai.graph.GraphNodeNames;
import com.avadhoot.workforgeai.ai.graph.GraphPrompt;
import com.avadhoot.workforgeai.ai.graph.WorkforgeAgentState;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces the user-facing final answer and marks the graph completed.
 */
public class FinalizeNode implements NodeAction<WorkforgeAgentState> {

    private final ChatClient chatClient;

    public FinalizeNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(WorkforgeAgentState state) {
        String answer = state.finalAnswer();
        String status = state.status();
        if (!StringUtils.hasText(answer)) {
            answer = synthesize(state);
        }
        if (!StringUtils.hasText(status) || WorkforgeAgentState.STATUS_RUNNING.equals(status)) {
            status = WorkforgeAgentState.STATUS_COMPLETED;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(WorkforgeAgentState.CURRENT_NODE, GraphNodeNames.FINALIZE);
        updates.put(WorkforgeAgentState.FINAL_ANSWER, answer == null ? "" : answer.trim());
        updates.put(WorkforgeAgentState.COMPLETED, Boolean.TRUE);
        updates.put(WorkforgeAgentState.STATUS, status);
        updates.put(WorkforgeAgentState.NEXT_ROUTE, WorkforgeAgentState.ROUTE_FINALIZE);
        updates.put(WorkforgeAgentState.STEPS, List.of(Map.of("node", GraphNodeNames.FINALIZE)));
        return updates;
    }

    private String synthesize(WorkforgeAgentState state) {
        List<String> context = state.context();
        if (context == null || context.isEmpty()) {
            String content = chatClient.prompt()
                    .system(GraphPrompt.FINALIZE_SYSTEM)
                    .user(state.userMessage())
                    .call()
                    .content();
            return StringUtils.hasText(content) ? content.trim()
                    : "I could not complete the graph run with a reliable answer.";
        }
        String content = chatClient.prompt()
                .system(GraphPrompt.FINALIZE_SYSTEM)
                .user("""
                        User request:
                        %s

                        Observations:
                        %s
                        """.formatted(state.userMessage(), String.join("\n", context)))
                .call()
                .content();
        return StringUtils.hasText(content) ? content.trim()
                : "I gathered tool results but could not produce a final answer.";
    }
}
