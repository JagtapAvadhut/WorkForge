package com.avadhoot.workforgeai.ai.pipeline;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Test double that skips security/observability and runs the action.
 */
public final class PassThroughAiRequestPipeline extends AiRequestPipeline {

    public PassThroughAiRequestPipeline() {
        super(null, null);
    }

    @Override
    public <T> T execute(String feature, String input, Supplier<T> action) {
        return action.get();
    }

    @Override
    public <T> T execute(String feature, String input, Supplier<T> action, Consumer<T> enrich) {
        T result = action.get();
        if (enrich != null && result != null) {
            enrich.accept(result);
        }
        return result;
    }
}
