package com.avadhoot.workforgeai.ai.agent;

import com.avadhoot.workforgeai.ai.agent.dto.AgentRequest;
import com.avadhoot.workforgeai.ai.agent.dto.AgentResponse;
import com.avadhoot.workforgeai.ai.observability.AiExecutionContext;
import com.avadhoot.workforgeai.ai.pipeline.AiRequestPipeline;
import com.avadhoot.workforgeai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/agent")
public class AgentController {

    private final AgentService agentService;
    private final AiRequestPipeline aiRequestPipeline;

    public AgentController(AgentService agentService, AiRequestPipeline aiRequestPipeline) {
        this.agentService = agentService;
        this.aiRequestPipeline = aiRequestPipeline;
    }

    @PostMapping("/run")
    public ApiResponse<AgentResponse> run(@Valid @RequestBody AgentRequest request) {
        return ApiResponse.ok(aiRequestPipeline.execute(
                "AGENT",
                request.message(),
                () -> agentService.run(request),
                response -> {
                    AiExecutionContext ctx = AiExecutionContext.current();
                    if (ctx == null || response.steps() == null) {
                        return;
                    }
                    ctx.addAgentSteps(response.steps().size());
                    long tools = response.steps().stream()
                            .filter(step -> step.tool() != null && !step.tool().isBlank())
                            .count();
                    ctx.addToolCalls((int) tools);
                }));
    }
}
