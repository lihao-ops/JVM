package com.example.jvmlab.exceptionlab;

import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;

import java.util.List;
import java.util.Map;

/**
 * 模板方法基类，统一 Dry-Run 逻辑与参数解析。
 */
public abstract class AbstractMemoryExceptionScenario implements MemoryExceptionScenario {

    protected static final String PARAM_DRY_RUN = "dryRun";

    @Override
    public final ScenarioExecutionResult execute(Map<String, Object> requestParams) throws Exception {
        boolean dryRun = parseBoolean(requestParams, PARAM_DRY_RUN, true);
        if (dryRun) {
            return buildDryRunResult();
        }
        return doExecute(requestParams);
    }

    /**
     * 子类实现真正的异常触发逻辑。
     */
    protected abstract ScenarioExecutionResult doExecute(Map<String, Object> requestParams) throws Exception;

    /**
     * 构建 Dry-Run 场景的统一返回，提示如何真正触发异常。
     */
    protected ScenarioExecutionResult buildDryRunResult() {
        return new ScenarioExecutionResult(getId(), true, false,
                "Dry-Run completed. Set dryRun=false to trigger the real scenario.",
                Map.of("exceptionType", getExceptionType()),
                List.of("按照返回的指南配置 JVM 参数并重新调用接口。"));
    }

    protected boolean parseBoolean(Map<String, Object> params, String key, boolean defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        Object value = params.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    protected int parseInt(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
