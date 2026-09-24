package com.avadhoot.workforgeai.ai.graph;

import com.avadhoot.workforgeai.ai.graph.node.AnalyzeRequestNode;
import com.avadhoot.workforgeai.ai.graph.node.DecideActionNode;
import com.avadhoot.workforgeai.ai.graph.node.ExecuteToolNode;
import com.avadhoot.workforgeai.ai.graph.node.FinalizeNode;
import com.avadhoot.workforgeai.ai.graph.node.ObserveResultNode;
import com.avadhoot.workforgeai.ai.tools.WorkforgeTools;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.EdgeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Builds and compiles the WorkForge AI LangGraph4j agent graph.
 *
 * <pre>
 * START → ANALYZE_REQUEST → DECIDE_ACTION
 *                              ├─ tool → EXECUTE_TOOL → OBSERVE_RESULT → DECIDE_ACTION
 *                              └─ finalize → FINALIZE → END
 * </pre>
 */
@Component
public class WorkforgeAgentGraph {

    private final CompiledGraph<WorkforgeAgentState> compiledGraph;

    public WorkforgeAgentGraph(
            ChatClient chatClient,
            WorkforgeTools workforgeTools,
            JsonMapper jsonMapper) {
        try {
            this.compiledGraph = build(chatClient, workforgeTools, jsonMapper);
            this.compiledGraph.setMaxIterations(40);
        } catch (GraphStateException ex) {
            throw new IllegalStateException("Failed to compile WorkForge LangGraph4j agent graph", ex);
        }
    }

    public CompiledGraph<WorkforgeAgentState> compiled() {
        return compiledGraph;
    }

    private static CompiledGraph<WorkforgeAgentState> build(
            ChatClient chatClient,
            WorkforgeTools workforgeTools,
            JsonMapper jsonMapper) throws GraphStateException {
        AnalyzeRequestNode analyze = new AnalyzeRequestNode();
        DecideActionNode decide = new DecideActionNode(chatClient, jsonMapper);
        ExecuteToolNode execute = new ExecuteToolNode(workforgeTools, jsonMapper);
        ObserveResultNode observe = new ObserveResultNode();
        FinalizeNode finalize = new FinalizeNode(chatClient);

        EdgeAction<WorkforgeAgentState> afterDecide = state -> {
            if (WorkforgeAgentState.ROUTE_TOOL.equals(state.nextRoute())
                    && !state.completed()
                    && state.iteration() <= state.maxIterations()) {
                return WorkforgeAgentState.ROUTE_TOOL;
            }
            return WorkforgeAgentState.ROUTE_FINALIZE;
        };

        StateGraph<WorkforgeAgentState> graph = new StateGraph<>(
                WorkforgeAgentState.SCHEMA,
                WorkforgeAgentState::new)
                .addNode(GraphNodeNames.ANALYZE_REQUEST, node_async(analyze))
                .addNode(GraphNodeNames.DECIDE_ACTION, node_async(decide))
                .addNode(GraphNodeNames.EXECUTE_TOOL, node_async(execute))
                .addNode(GraphNodeNames.OBSERVE_RESULT, node_async(observe))
                .addNode(GraphNodeNames.FINALIZE, node_async(finalize))
                .addEdge(START, GraphNodeNames.ANALYZE_REQUEST)
                .addEdge(GraphNodeNames.ANALYZE_REQUEST, GraphNodeNames.DECIDE_ACTION)
                .addConditionalEdges(
                        GraphNodeNames.DECIDE_ACTION,
                        edge_async(afterDecide),
                        Map.of(
                                WorkforgeAgentState.ROUTE_TOOL, GraphNodeNames.EXECUTE_TOOL,
                                WorkforgeAgentState.ROUTE_FINALIZE, GraphNodeNames.FINALIZE))
                .addEdge(GraphNodeNames.EXECUTE_TOOL, GraphNodeNames.OBSERVE_RESULT)
                .addEdge(GraphNodeNames.OBSERVE_RESULT, GraphNodeNames.DECIDE_ACTION)
                .addEdge(GraphNodeNames.FINALIZE, END);

        return graph.compile();
    }
}
