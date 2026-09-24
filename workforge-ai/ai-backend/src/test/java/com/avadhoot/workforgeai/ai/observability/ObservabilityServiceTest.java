package com.avadhoot.workforgeai.ai.observability;

import com.avadhoot.workforgeai.ai.security.SecurityPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObservabilityServiceTest {

    private TraceRepository repository;
    private ObservabilityService service;
    private final AtomicReference<AiExecutionTrace> stored = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        repository = mock(TraceRepository.class);
        SecurityPolicyService security = new SecurityPolicyService(
                "getIssue,searchIssues,getProject", 4000, 10, 5, 10, true);
        service = new ObservabilityService(repository, security, "qwen2.5-coder:3b");

        when(repository.insert(any())).thenAnswer(inv -> {
            AiExecutionTrace t = inv.getArgument(0);
            stored.set(t);
            return t;
        });
        when(repository.updateCompletion(
                anyString(), anyString(), any(), anyLong(), anyBoolean(), any(),
                anyInt(), anyInt(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(1);
        when(repository.findByTraceId(anyString())).thenAnswer(inv -> {
            AiExecutionTrace base = stored.get();
            return Optional.of(new AiExecutionTrace(
                    base.id(), base.traceId(), base.feature(), "qwen2.5-coder:3b",
                    base.startedAt(), Instant.now(), 12L, true, null,
                    2, 1, 1, 3, null, null, null, base.requestSummary()));
        });
        when(repository.list(anyInt(), anyInt())).thenReturn(List.of());
        when(repository.count()).thenReturn(1L);
        when(repository.summary()).thenReturn(new ObservabilitySummary(1, 1, 0, 12, 2, 1, 1, 3));
    }

    @Test
    void successfulTrace() {
        Instant started = Instant.now();
        var ctx = service.begin("CHAT", "What is a sprint?");
        ctx.addToolCalls(2);
        ctx.addAgentSteps(1);
        ctx.addMcpCalls(1);
        ctx.addRetrievals(3);
        var done = service.completeSuccess(ctx, started);
        assertThat(done.success()).isTrue();
        assertThat(done.latencyMs()).isNotNull();
        verify(repository).insert(any());
        AiExecutionContext.clear();
    }

    @Test
    void failedTrace() {
        Instant started = Instant.now();
        var ctx = service.begin("AGENT", "boom");
        when(repository.findByTraceId(anyString())).thenAnswer(inv -> {
            AiExecutionTrace base = stored.get();
            return Optional.of(new AiExecutionTrace(
                    base.id(), base.traceId(), base.feature(), base.model(),
                    base.startedAt(), Instant.now(), 5L, false, "ERROR",
                    0, 0, 0, 0, null, null, null, base.requestSummary()));
        });
        var done = service.completeFailure(ctx, started, "ERROR");
        assertThat(done.success()).isFalse();
        assertThat(done.errorCode()).isEqualTo("ERROR");
        AiExecutionContext.clear();
    }

    @Test
    void summaryAndPagination() {
        assertThat(service.summary().totalRequests()).isEqualTo(1);
        assertThat(service.listTraces(10, 0)).isEmpty();
        assertThat(service.totalTraces()).isEqualTo(1);
    }

    @Test
    void doesNotPersistSecretsInSummary() {
        service.begin("CHAT", "password=supersecret token=abc");
        assertThat(stored.get().requestSummary().toLowerCase()).doesNotContain("supersecret");
        AiExecutionContext.clear();
    }
}
