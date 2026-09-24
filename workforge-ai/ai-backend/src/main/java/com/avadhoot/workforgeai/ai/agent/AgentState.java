package com.avadhoot.workforgeai.ai.agent;

/**
 * Minimal mutable agent loop state.
 */
public class AgentState {

    private final String userMessage;
    private final java.util.List<AgentStep> steps = new java.util.ArrayList<>();
    private final java.util.List<String> observations = new java.util.ArrayList<>();
    private int iteration;
    private boolean finished;
    private String finalAnswer;
    private String stopReason = "completed";

    public AgentState(String userMessage) {
        this.userMessage = userMessage;
    }

    public String userMessage() {
        return userMessage;
    }

    public java.util.List<AgentStep> steps() {
        return steps;
    }

    public java.util.List<String> observations() {
        return observations;
    }

    public int iteration() {
        return iteration;
    }

    public void incrementIteration() {
        iteration++;
    }

    public boolean finished() {
        return finished;
    }

    public void finish(String answer, String reason) {
        this.finished = true;
        this.finalAnswer = answer;
        this.stopReason = reason;
    }

    public String finalAnswer() {
        return finalAnswer;
    }

    public String stopReason() {
        return stopReason;
    }

    public void addStep(AgentStep step) {
        steps.add(step);
    }

    public void addObservation(String observation) {
        observations.add(observation);
    }
}
