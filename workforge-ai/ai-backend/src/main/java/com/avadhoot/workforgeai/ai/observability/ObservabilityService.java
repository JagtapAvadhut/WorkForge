package com.avadhoot.workforgeai.ai.observability;

import com.avadhoot.workforgeai.ai.security.SecurityPolicyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ObservabilityService {

    private final TraceRepository traceRepository;
    private final SecurityPolicyService securityPolicyService;
    private final String defaultModel;

    public ObservabilityService(
            TraceRepository traceRepository,
            SecurityPolicyService securityPolicyService,
            @Value("${spring.ai.ollama.chat.options.model:qwen2.5-coder:3b}") String defaultModel) {
        this.traceRepository = traceRepository;
        this.securityPolicyService = securityPolicyService;
        this.defaultModel = defaultModel;
    }

    public AiExecutionContext begin(String feature, String requestInput) {
        String traceId = "tr-" + UUID.randomUUID();
        AiExecutionContext ctx = AiExecutionContext.start(traceId, feature);
        ctx.setModel(defaultModel);
        Instant started = Instant.now();
        traceRepository.insert(new AiExecutionTrace(
                UUID.randomUUID(),
                traceId,
                feature,
                defaultModel,
                started,
                null,
                null,
                false,
                "IN_PROGRESS",
                0,
                0,
                0,
                0,
                null,
                null,
                null,
                securityPolicyService.sanitizeSummary(requestInput)));
        return ctx;
    }

    public AiExecutionTrace completeSuccess(AiExecutionContext ctx, Instant startedAt) {
        return complete(ctx, startedAt, true, null);
    }

    public AiExecutionTrace completeFailure(AiExecutionContext ctx, Instant startedAt, String errorCode) {
        return complete(ctx, startedAt, false, errorCode == null ? "ERROR" : errorCode);
    }

    private AiExecutionTrace complete(
            AiExecutionContext ctx,
            Instant startedAt,
            boolean success,
            String errorCode) {
        Instant completed = Instant.now();
        long latency = Math.max(0, completed.toEpochMilli() - startedAt.toEpochMilli());
        String model = ctx.model() == null ? defaultModel : ctx.model();
        int updated = traceRepository.updateCompletion(
                ctx.traceId(),
                model,
                completed,
                latency,
                success,
                errorCode,
                ctx.toolCallCount(),
                ctx.agentStepCount(),
                ctx.mcpCallCount(),
                ctx.retrievalCount(),
                ctx.inputTokens(),
                ctx.outputTokens(),
                ctx.totalTokens());
        if (updated == 0) {
            return traceRepository.insert(new AiExecutionTrace(
                    UUID.randomUUID(),
                    ctx.traceId(),
                    ctx.feature(),
                    model,
                    startedAt,
                    completed,
                    latency,
                    success,
                    errorCode,
                    ctx.toolCallCount(),
                    ctx.agentStepCount(),
                    ctx.mcpCallCount(),
                    ctx.retrievalCount(),
                    ctx.inputTokens(),
                    ctx.outputTokens(),
                    ctx.totalTokens(),
                    null));
        }
        return traceRepository.findByTraceId(ctx.traceId()).orElseThrow();
    }

    public List<AiExecutionTrace> listTraces(int limit, int offset) {
        return traceRepository.list(limit, offset);
    }

    public long totalTraces() {
        return traceRepository.count();
    }

    public AiExecutionTrace getRequired(String traceId) {
        return traceRepository.findByTraceId(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Trace not found: " + traceId));
    }

    public ObservabilitySummary summary() {
        return traceRepository.summary();
    }

    public String defaultModel() {
        return defaultModel;
    }
}
