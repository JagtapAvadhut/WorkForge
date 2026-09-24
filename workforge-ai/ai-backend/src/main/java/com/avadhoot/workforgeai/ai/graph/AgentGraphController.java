package com.avadhoot.workforgeai.ai.graph;

import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentRequest;
import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentResponse;
import com.avadhoot.workforgeai.ai.observability.AiExecutionContext;
import com.avadhoot.workforgeai.ai.pipeline.AiRequestPipeline;
import com.avadhoot.workforgeai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AgentGraphController {

    private final AgentGraphService agentGraphService;
    private final AiRequestPipeline aiRequestPipeline;

    public AgentGraphController(AgentGraphService agentGraphService, AiRequestPipeline aiRequestPipeline) {
        this.agentGraphService = agentGraphService;
        this.aiRequestPipeline = aiRequestPipeline;
    }

    @PostMapping("/graph-agent")
    public ApiResponse<GraphAgentResponse> run(@Valid @RequestBody GraphAgentRequest request) {
        return ApiResponse.ok(aiRequestPipeline.execute(
                "GRAPH",
                request.message(),
                () -> agentGraphService.run(request),
                response -> {
                    AiExecutionContext ctx = AiExecutionContext.current();
                    if (ctx == null) {
                        return;
                    }
                    if (response.steps() != null) {
                        ctx.addAgentSteps(response.steps().size());
                    }
                    if (response.toolCalls() != null) {
                        ctx.addToolCalls(response.toolCalls().size());
                    }
                }));
    }
}
