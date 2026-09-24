package com.avadhoot.workforgeai.ai.graph;

import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentResponse;
import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentStep;
import com.avadhoot.workforgeai.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentGraphControllerTest {

    private MockMvc mockMvc;
    private AgentGraphService agentGraphService;

    @BeforeEach
    void setUp() {
        agentGraphService = mock(AgentGraphService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AgentGraphController(agentGraphService, new com.avadhoot.workforgeai.ai.pipeline.PassThroughAiRequestPipeline()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void run_returnsGraphResponse() throws Exception {
        when(agentGraphService.run(any())).thenReturn(new GraphAgentResponse(
                "MWS-1 is a checkout bug.",
                true,
                2,
                WorkforgeAgentState.STATUS_COMPLETED,
                GraphNodeNames.FINALIZE,
                List.of(
                        new GraphAgentStep(GraphNodeNames.DECIDE_ACTION, "getIssue"),
                        new GraphAgentStep(GraphNodeNames.EXECUTE_TOOL, "getIssue"),
                        new GraphAgentStep(GraphNodeNames.FINALIZE, null)),
                List.of(Map.of("tool", "getIssue")),
                List.of(Map.of("tool", "getIssue", "ok", true))));

        mockMvc.perform(post("/api/v1/ai/graph-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Tell me about MWS-1 and its project."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.response").value("MWS-1 is a checkout bug."))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.iterations").value(2))
                .andExpect(jsonPath("$.data.steps[0].action").value("getIssue"));
    }

    @Test
    void run_rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/ai/graph-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
