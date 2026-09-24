package com.avadhoot.workforgeai.ai.multiagent;

import com.avadhoot.workforgeai.ai.multiagent.dto.MultiAgentRequest;
import com.avadhoot.workforgeai.ai.multiagent.dto.MultiAgentResponse;
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
public class MultiAgentController {

    private final MultiAgentService multiAgentService;
    private final AiRequestPipeline aiRequestPipeline;

    public MultiAgentController(MultiAgentService multiAgentService, AiRequestPipeline aiRequestPipeline) {
        this.multiAgentService = multiAgentService;
        this.aiRequestPipeline = aiRequestPipeline;
    }

    @PostMapping("/multi-agent")
    public ApiResponse<MultiAgentResponse> run(@Valid @RequestBody MultiAgentRequest request) {
        return ApiResponse.ok(aiRequestPipeline.execute(
                "MULTI_AGENT",
                request.message(),
                () -> multiAgentService.run(request),
                response -> {
                    AiExecutionContext ctx = AiExecutionContext.current();
                    if (ctx != null && response.steps() != null) {
                        ctx.addAgentSteps(response.steps().size());
                    }
                }));
    }
}
