package com.example.jvmlab.exceptionlab.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 封装一次实验执行的结果，既可以是真实触发，也可以是Dry-Run演练。
 */
public class ScenarioExecutionResult {
    private final String scenarioId;
    private final boolean dryRun;
    private final boolean triggered;
    private final String message;
    private final Map<String, Object> metrics;
    private final List<String> nextActions;
    private final Instant timestamp;

    public ScenarioExecutionResult(String scenarioId, boolean dryRun, boolean triggered, String message,
                                   Map<String, Object> metrics, List<String> nextActions) {
        this.scenarioId = scenarioId;
        this.dryRun = dryRun;
        this.triggered = triggered;
        this.message = message;
        this.metrics = metrics == null ? Collections.emptyMap() : Map.copyOf(metrics);
        this.nextActions = nextActions == null ? Collections.emptyList() : List.copyOf(nextActions);
        this.timestamp = Instant.now();
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public List<String> getNextActions() {
        return nextActions;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
