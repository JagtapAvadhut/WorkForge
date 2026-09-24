package com.avadhoot.workforgeai.ai.evaluation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EvaluationRunState {

    public enum Status {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    private final String runId;
    private final String suite;
    private final int totalCases;
    private volatile Status status;
    private volatile String currentCaseId;
    private volatile String currentCategory;
    private volatile int completedCount;
    private volatile int passedCount;
    private volatile int failedCount;
    private volatile String error;
    private final Instant startedAt;
    private volatile Instant completedAt;
    private final List<EvaluationCaseResult> results = new CopyOnWriteArrayList<>();

    public EvaluationRunState(String runId, String suite, int totalCases) {
        this.runId = runId;
        this.suite = suite;
        this.totalCases = totalCases;
        this.status = Status.QUEUED;
        this.startedAt = Instant.now();
    }

    public String runId() {
        return runId;
    }

    public String suite() {
        return suite;
    }

    public int totalCases() {
        return totalCases;
    }

    public Status status() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String currentCaseId() {
        return currentCaseId;
    }

    public String currentCategory() {
        return currentCategory;
    }

    public void setCurrent(String caseId, String category) {
        this.currentCaseId = caseId;
        this.currentCategory = category;
    }

    public int completedCount() {
        return completedCount;
    }

    public int passedCount() {
        return passedCount;
    }

    public int failedCount() {
        return failedCount;
    }

    public String error() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public List<EvaluationCaseResult> results() {
        return List.copyOf(results);
    }

    public synchronized void addResult(EvaluationCaseResult result) {
        results.add(result);
        completedCount = results.size();
        if (result.passed()) {
            passedCount++;
        } else {
            failedCount++;
        }
    }

    public double passRate() {
        if (completedCount == 0) {
            return 0.0;
        }
        return (passedCount * 100.0) / completedCount;
    }

    public long averageLatencyMs() {
        if (results.isEmpty()) {
            return 0L;
        }
        return Math.round(results.stream().mapToLong(EvaluationCaseResult::latencyMs).average().orElse(0));
    }

    public long elapsedMs() {
        Instant end = completedAt == null ? Instant.now() : completedAt;
        return Math.max(0, end.toEpochMilli() - startedAt.toEpochMilli());
    }

    public EvaluationRunSnapshot snapshot() {
        return new EvaluationRunSnapshot(
                runId,
                suite,
                status.name(),
                totalCases,
                completedCount,
                passedCount,
                failedCount,
                passRate(),
                averageLatencyMs(),
                elapsedMs(),
                currentCaseId,
                currentCategory,
                error,
                startedAt,
                completedAt,
                new ArrayList<>(results));
    }
}
