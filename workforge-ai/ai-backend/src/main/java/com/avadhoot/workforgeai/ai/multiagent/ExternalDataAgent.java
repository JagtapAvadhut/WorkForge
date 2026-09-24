package com.avadhoot.workforgeai.ai.multiagent;

/**
 * Optional future read-only external data specialist.
 * Not wired into the production graph until a free/public API is configured.
 */
public interface ExternalDataAgent {

    String agentName();

    boolean available();

    SpecialistResult investigate(String task, String userRequest);
}
