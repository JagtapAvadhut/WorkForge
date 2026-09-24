package com.avadhoot.workforgeai.ai.multiagent;

import com.avadhoot.workforgeai.ai.multiagent.dto.MultiAgentResponse;
import com.avadhoot.workforgeai.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MultiAgentControllerTest {

    private MockMvc mockMvc;
    private MultiAgentService multiAgentService;

    @BeforeEach
    void setUp() {
        multiAgentService = mock(MultiAgentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MultiAgentController(multiAgentService, new com.avadhoot.workforgeai.ai.pipeline.PassThroughAiRequestPipeline()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void runMultiAgent() throws Exception {
        when(multiAgentService.run(any())).thenReturn(new MultiAgentResponse(
                "Final",
                List.of(MultiAgentNames.ISSUE_AGENT),
                List.of(),
                2,
                1,
                MultiAgentState.STATUS_COMPLETED,
                true,
                List.of(),
                List.of(),
                List.of(),
                "conv-1"));

        mockMvc.perform(post("/api/v1/ai/multi-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Get details of MWS-1.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("Final"))
                .andExpect(jsonPath("$.data.agentsUsed[0]").value("ISSUE_AGENT"))
                .andExpect(jsonPath("$.data.conversationId").value("conv-1"));
    }
}
