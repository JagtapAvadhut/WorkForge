package com.avadhoot.workforgeai.ai.agent;

import com.avadhoot.workforgeai.ai.agent.dto.AgentResponse;
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

class AgentControllerTest {

    private MockMvc mockMvc;
    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = mock(AgentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AgentController(agentService, new com.avadhoot.workforgeai.ai.pipeline.PassThroughAiRequestPipeline()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void run_returnsAnswerAndSteps() throws Exception {
        when(agentService.run(any())).thenReturn(new AgentResponse(
                "MWS is Mobile Web Store.",
                List.of(new AgentStep(1, "need project", "tool", "getProject",
                        Map.of("projectKey", "MWS"), "{\"found\":true}", "ok")),
                "completed",
                1));

        mockMvc.perform(post("/api/v1/ai/agent/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Tell me about MWS","maxSteps":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").value("MWS is Mobile Web Store."))
                .andExpect(jsonPath("$.data.steps[0].tool").value("getProject"))
                .andExpect(jsonPath("$.data.stopReason").value("completed"));
    }

    @Test
    void run_rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/ai/agent/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
