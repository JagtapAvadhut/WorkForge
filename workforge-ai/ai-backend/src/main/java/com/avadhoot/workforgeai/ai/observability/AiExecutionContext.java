package com.avadhoot.workforgeai.ai.observability;

/**
 * Request-scoped counters for one AI execution. No chain-of-thought storage.
 */
public final class AiExecutionContext {

    private static final ThreadLocal<AiExecutionContext> CURRENT = new ThreadLocal<>();

    private final String traceId;
    private final String feature;
    private String model;
    private int toolCallCount;
    private int agentStepCount;
    private int mcpCallCount;
    private int retrievalCount;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;

    private AiExecutionContext(String traceId, String feature) {
        this.traceId = traceId;
        this.feature = feature;
    }

    public static AiExecutionContext start(String traceId, String feature) {
        AiExecutionContext ctx = new AiExecutionContext(traceId, feature);
        CURRENT.set(ctx);
        return ctx;
    }

    public static AiExecutionContext current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public String traceId() {
        return traceId;
    }

    public String feature() {
        return feature;
    }

    public String model() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void addToolCall() {
        toolCallCount++;
    }

    public void addToolCalls(int n) {
        if (n > 0) {
            toolCallCount += n;
        }
    }

    public void addAgentSteps(int n) {
        if (n > 0) {
            agentStepCount += n;
        }
    }

    public void addMcpCall() {
        mcpCallCount++;
    }

    public void addMcpCalls(int n) {
        if (n > 0) {
            mcpCallCount += n;
        }
    }

    public void addRetrievals(int n) {
        if (n > 0) {
            retrievalCount += n;
        }
    }

    public void setTokenUsage(Integer input, Integer output, Integer total) {
        this.inputTokens = input;
        this.outputTokens = output;
        this.totalTokens = total;
    }

    public int toolCallCount() {
        return toolCallCount;
    }

    public int agentStepCount() {
        return agentStepCount;
    }

    public int mcpCallCount() {
        return mcpCallCount;
    }

    public int retrievalCount() {
        return retrievalCount;
    }

    public Integer inputTokens() {
        return inputTokens;
    }

    public Integer outputTokens() {
        return outputTokens;
    }

    public Integer totalTokens() {
        return totalTokens;
    }
}
