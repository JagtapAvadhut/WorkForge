package com.avadhoot.workforgeai.ai.mcp;

import com.avadhoot.workforgeai.ai.graph.GraphNodeNames;
import com.avadhoot.workforgeai.ai.graph.WorkforgeAgentState;
import com.avadhoot.workforgeai.ai.graph.node.AnalyzeRequestNode;
import com.avadhoot.workforgeai.ai.graph.node.FinalizeNode;
import com.avadhoot.workforgeai.ai.graph.node.ObserveResultNode;
import com.avadhoot.workforgeai.ai.mcp.node.McpDecideActionNode;
import com.avadhoot.workforgeai.ai.mcp.node.McpExecuteToolNode;
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
 * LangGraph4j agent that executes tools only through MCP.
 */
@Component
public class WorkforgeMcpAgentGraph {

    private final CompiledGraph<WorkforgeAgentState> compiledGraph;

    public WorkforgeMcpAgentGraph(
            ChatClient chatClient,
            McpClientGateway mcpClientGateway,
            JsonMapper jsonMapper) {
        try {
            this.compiledGraph = build(chatClient, mcpClientGateway, jsonMapper);
            this.compiledGraph.setMaxIterations(40);
        } catch (GraphStateException ex) {
            throw new IllegalStateException("Failed to compile WorkForge MCP agent graph", ex);
        }
    }

    public CompiledGraph<WorkforgeAgentState> compiled() {
        return compiledGraph;
    }

    private static CompiledGraph<WorkforgeAgentState> build(
            ChatClient chatClient,
            McpClientGateway mcpClientGateway,
            JsonMapper jsonMapper) throws GraphStateException {
        AnalyzeRequestNode analyze = new AnalyzeRequestNode();
        McpDecideActionNode decide = new McpDecideActionNode(chatClient, jsonMapper);
        McpExecuteToolNode execute = new McpExecuteToolNode(mcpClientGateway);
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

        return new StateGraph<>(WorkforgeAgentState.SCHEMA, WorkforgeAgentState::new)
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
                .addEdge(GraphNodeNames.FINALIZE, END)
                .compile();
    }
}
