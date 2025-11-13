package com.example.jvmlab.exceptionlab.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 类说明 / Class Description:
 * 中文：封装一次异常实验的执行结果，适配真实触发与 Dry-Run 两种模式。
 * English: Encapsulates the execution result of an experiment, supporting both real trigger and Dry-Run modes.
 *
 * 使用场景 / Use Cases:
 * 中文：作为控制器响应体，包含消息、指标、下一步动作与时间戳。
 * English: Used as controller response, containing message, metrics, next actions and timestamp.
 *
 * 设计目的 / Design Purpose:
 * 中文：保持不可变对象，避免并发环境中的数据竞态。
 * English: Keep the object immutable to avoid data races in concurrent contexts.
 */
public class ScenarioExecutionResult {
    private final String scenarioId;
    private final boolean dryRun;
    private final boolean triggered;
    private final String message;
    private final Map<String, Object> metrics;
    private final List<String> nextActions;
    private final Instant timestamp;

    /**
     * 方法说明 / Method Description:
     * 中文：构造函数，初始化执行结果的各字段并记录时间戳。
     * English: Constructor initializing all fields of the execution result and recording the timestamp.
     *
     * 参数 / Parameters:
     * @param scenarioId 中文：场景 ID / English: Scenario ID
     * @param dryRun 中文：是否为演练模式 / English: Whether it is a dry-run
     * @param triggered 中文：是否触发异常 / English: Whether the exception was triggered
     * @param message 中文：结果说明信息 / English: Result description message
     * @param metrics 中文：采集的指标映射 / English: Collected metrics map
     * @param nextActions 中文：下一步建议 / English: Next action recommendations
     */
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

    /**
     * 方法说明 / Method Description:
     * 中文：获取场景 ID。
     * English: Get scenario ID.
     */
    public String getScenarioId() {
        return scenarioId;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：是否为演练模式。
     * English: Whether it is a dry-run.
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：是否触发异常。
     * English: Whether the exception was triggered.
     */
    public boolean isTriggered() {
        return triggered;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取结果说明消息。
     * English: Get the result message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取采集到的指标数据。
     * English: Get collected metrics data.
     */
    public Map<String, Object> getMetrics() {
        return metrics;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取下一步操作建议列表。
     * English: Get next action recommendations.
     */
    public List<String> getNextActions() {
        return nextActions;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取执行时间戳。
     * English: Get the execution timestamp.
     */
    public Instant getTimestamp() {
        return timestamp;
    }
}
