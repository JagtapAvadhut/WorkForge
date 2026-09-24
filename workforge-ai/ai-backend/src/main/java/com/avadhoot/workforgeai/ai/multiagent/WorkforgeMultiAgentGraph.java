package com.avadhoot.workforgeai.ai.multiagent;

import com.avadhoot.workforgeai.ai.multiagent.node.FinalizeMultiAgentNode;
import com.avadhoot.workforgeai.ai.multiagent.node.SpecialistNode;
import com.avadhoot.workforgeai.ai.multiagent.node.SupervisorNode;
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
 * Multi-agent LangGraph4j orchestration.
 *
 * <pre>
 * START → SUPERVISOR
 *            ├─ ISSUE_AGENT ──┐
 *            ├─ KNOWLEDGE ────┼─→ (next pending or SUPERVISOR)
 *            ├─ PROJECT ──────┘
 *            └─ FINALIZE → END
 * </pre>
 */
@Component
public class WorkforgeMultiAgentGraph {

    private final CompiledGraph<MultiAgentState> compiledGraph;

    public WorkforgeMultiAgentGraph(
            ChatClient chatClient,
            JsonMapper jsonMapper,
            IssueInvestigationAgent issueInvestigationAgent,
            KnowledgeRagAgent knowledgeRagAgent,
            ProjectAnalysisAgent projectAnalysisAgent) {
        try {
            this.compiledGraph = build(
                    chatClient,
                    jsonMapper,
                    issueInvestigationAgent,
                    knowledgeRagAgent,
                    projectAnalysisAgent);
            this.compiledGraph.setMaxIterations(60);
        } catch (GraphStateException ex) {
            throw new IllegalStateException("Failed to compile WorkForge multi-agent graph", ex);
        }
    }

    public CompiledGraph<MultiAgentState> compiled() {
        return compiledGraph;
    }

    private static CompiledGraph<MultiAgentState> build(
            ChatClient chatClient,
            JsonMapper jsonMapper,
            IssueInvestigationAgent issueInvestigationAgent,
            KnowledgeRagAgent knowledgeRagAgent,
            ProjectAnalysisAgent projectAnalysisAgent) throws GraphStateException {

        SupervisorNode supervisor = new SupervisorNode(chatClient, jsonMapper);
        SpecialistNode issue = SpecialistNode.issue(issueInvestigationAgent);
        SpecialistNode knowledge = SpecialistNode.knowledge(knowledgeRagAgent);
        SpecialistNode project = SpecialistNode.project(projectAnalysisAgent);
        FinalizeMultiAgentNode finalize = new FinalizeMultiAgentNode(chatClient);

        EdgeAction<MultiAgentState> route = MultiAgentState::nextRoute;

        Map<String, String> destinations = Map.of(
                MultiAgentNames.ROUTE_ISSUE, MultiAgentNames.ISSUE_AGENT,
                MultiAgentNames.ROUTE_KNOWLEDGE, MultiAgentNames.KNOWLEDGE_AGENT,
                MultiAgentNames.ROUTE_PROJECT, MultiAgentNames.PROJECT_AGENT,
                MultiAgentNames.ROUTE_SUPERVISOR, MultiAgentNames.SUPERVISOR,
                MultiAgentNames.ROUTE_FINALIZE, MultiAgentNames.FINALIZE);

        StateGraph<MultiAgentState> graph = new StateGraph<>(
                MultiAgentState.SCHEMA,
                MultiAgentState::new)
                .addNode(MultiAgentNames.SUPERVISOR, node_async(supervisor))
                .addNode(MultiAgentNames.ISSUE_AGENT, node_async(issue))
                .addNode(MultiAgentNames.KNOWLEDGE_AGENT, node_async(knowledge))
                .addNode(MultiAgentNames.PROJECT_AGENT, node_async(project))
                .addNode(MultiAgentNames.FINALIZE, node_async(finalize))
                .addEdge(START, MultiAgentNames.SUPERVISOR)
                .addConditionalEdges(MultiAgentNames.SUPERVISOR, edge_async(route), destinations)
                .addConditionalEdges(MultiAgentNames.ISSUE_AGENT, edge_async(route), destinations)
                .addConditionalEdges(MultiAgentNames.KNOWLEDGE_AGENT, edge_async(route), destinations)
                .addConditionalEdges(MultiAgentNames.PROJECT_AGENT, edge_async(route), destinations)
                .addEdge(MultiAgentNames.FINALIZE, END);

        return graph.compile();
    }
}
