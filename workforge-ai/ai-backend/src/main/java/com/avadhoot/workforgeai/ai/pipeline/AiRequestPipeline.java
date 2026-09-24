package com.avadhoot.workforgeai.ai.pipeline;

import com.avadhoot.workforgeai.ai.observability.AiExecutionContext;
import com.avadhoot.workforgeai.ai.observability.ObservabilityService;
import com.avadhoot.workforgeai.ai.security.SecurityBlockedException;
import com.avadhoot.workforgeai.ai.security.SecurityPolicyService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Request → Security Check → Trace Start → AI Feature → Trace Complete → Response
 */
@Component
public class AiRequestPipeline {

    private final SecurityPolicyService securityPolicyService;
    private final ObservabilityService observabilityService;

    public AiRequestPipeline(
            SecurityPolicyService securityPolicyService,
            ObservabilityService observabilityService) {
        this.securityPolicyService = securityPolicyService;
        this.observabilityService = observabilityService;
    }

    public <T> T execute(String feature, String input, Supplier<T> action) {
        return execute(feature, input, action, ignored -> {
        });
    }

    public <T> T execute(String feature, String input, Supplier<T> action, Consumer<T> enrich) {
        securityPolicyService.assertAllowedForExecution(input);
        Instant started = Instant.now();
        AiExecutionContext ctx = observabilityService.begin(feature, input);
        try {
            T result = action.get();
            if (enrich != null && result != null) {
                enrich.accept(result);
            }
            observabilityService.completeSuccess(ctx, started);
            return result;
        } catch (SecurityBlockedException ex) {
            observabilityService.completeFailure(ctx, started, "SECURITY_" + ex.status().name());
            throw ex;
        } catch (IllegalArgumentException ex) {
            observabilityService.completeFailure(ctx, started, "BAD_REQUEST");
            throw ex;
        } catch (RuntimeException ex) {
            observabilityService.completeFailure(ctx, started, errorCode(ex));
            throw ex;
        } finally {
            AiExecutionContext.clear();
        }
    }

    private static String errorCode(RuntimeException ex) {
        String name = ex.getClass().getSimpleName();
        if (name.length() > 60) {
            return "ERROR";
        }
        return name.replace("Exception", "").toUpperCase();
    }
}
