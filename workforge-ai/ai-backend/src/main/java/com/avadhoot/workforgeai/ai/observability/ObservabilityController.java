package com.avadhoot.workforgeai.ai.observability;

import com.avadhoot.workforgeai.ai.observability.dto.TraceDto;
import com.avadhoot.workforgeai.ai.observability.dto.TracePageDto;
import com.avadhoot.workforgeai.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/observability")
public class ObservabilityController {

    private final ObservabilityService observabilityService;

    public ObservabilityController(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    @GetMapping("/traces")
    public ApiResponse<TracePageDto> traces(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeOffset = Math.max(0, offset);
        return ApiResponse.ok(new TracePageDto(
                observabilityService.listTraces(safeLimit, safeOffset).stream().map(this::toDto).toList(),
                observabilityService.totalTraces(),
                safeLimit,
                safeOffset));
    }

    @GetMapping("/traces/{traceId}")
    public ApiResponse<TraceDto> trace(@PathVariable String traceId) {
        return ApiResponse.ok(toDto(observabilityService.getRequired(traceId)));
    }

    @GetMapping("/summary")
    public ApiResponse<ObservabilitySummary> summary() {
        return ApiResponse.ok(observabilityService.summary());
    }

    private TraceDto toDto(AiExecutionTrace trace) {
        return new TraceDto(
                trace.id(),
                trace.traceId(),
                trace.feature(),
                trace.model(),
                trace.startedAt(),
                trace.completedAt(),
                trace.latencyMs(),
                trace.success(),
                trace.errorCode(),
                trace.toolCallCount(),
                trace.agentStepCount(),
                trace.mcpCallCount(),
                trace.retrievalCount(),
                trace.inputTokens(),
                trace.outputTokens(),
                trace.totalTokens(),
                trace.requestSummary());
    }
}
