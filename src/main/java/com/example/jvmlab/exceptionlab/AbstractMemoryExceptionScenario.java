package com.example.jvmlab.exceptionlab;

import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;

import java.util.List;
import java.util.Map;

/**
 * 类说明 / Class Description:
 * 中文：异常场景的模板方法基类，统一处理 Dry-Run 分支与参数解析，屏蔽通用流程差异。
 * English: Template base class for exception scenarios, unifying Dry-Run branch and parameter parsing to hide common flow differences.
 *
 * 使用场景 / Use Cases:
 * 中文：所有内存异常场景继承此类，以减少样板代码并保持行为一致性。
 * English: All memory exception scenarios extend this class to reduce boilerplate and ensure consistent behavior.
 *
 * 设计目的 / Design Purpose:
 * 中文：将执行入口固定为模板方法，子类专注于具体触发逻辑与指标返回。
 * English: Fix execution entry as a template method so subclasses focus on trigger logic and metrics.
 */
public abstract class AbstractMemoryExceptionScenario implements MemoryExceptionScenario {

    protected static final String PARAM_DRY_RUN = "dryRun";

    /**
     * 方法说明 / Method Description:
     * 中文：模板方法，统一处理 Dry-Run 与真实执行分支，并委托子类实现具体触发逻辑。
     * English: Template method that handles Dry-Run vs real execution branch and delegates to subclass for trigger logic.
     *
     * 参数 / Parameters:
     * @param requestParams 中文：请求参数（包含 dryRun 等） / English: Request params including dryRun
     *
     * 返回值 / Return:
     * 中文：统一的执行结果对象 / English: Unified execution result object
     *
     * 异常 / Exceptions:
     * 中文：子类逻辑可能抛出受检或运行时异常 / English: Subclass logic may throw checked or runtime exceptions
     */
    @Override
    public final ScenarioExecutionResult execute(Map<String, Object> requestParams) throws Exception {
        // 中文：解析 dryRun 参数，默认 true，以安全地提供操作指引
        // English: Parse dryRun parameter (default true) to safely provide guidance
        boolean dryRun = parseBoolean(requestParams, PARAM_DRY_RUN, true);
        if (dryRun) {
            // 中文：返回演练结果，提示如何触发真实异常
            // English: Return dry-run result with hints for real trigger
            return buildDryRunResult();
        }
        // 中文：进入真实执行路径，触发异常场景
        // English: Go into real execution path to trigger scenario
        return doExecute(requestParams);
    }

    /**
     * 方法说明 / Method Description:
     * 中文：抽象方法，子类实现具体异常触发与指标采集。
     * English: Abstract method for subclasses to implement actual trigger logic and metric collection.
     *
     * 参数 / Parameters:
     * @param requestParams 中文：请求参数（大小、延迟等） / English: Request params (sizes, delays, etc.)
     *
     * 返回值 / Return:
     * 中文：执行结果对象 / English: Execution result object
     *
     * 异常 / Exceptions:
     * 中文：可能抛出 OOM 或业务异常 / English: May throw OOM or business exceptions
     */
    protected abstract ScenarioExecutionResult doExecute(Map<String, Object> requestParams) throws Exception;

    /**
     * 方法说明 / Method Description:
     * 中文：构建 Dry-Run 的统一返回内容，指导用户如何配置与触发真实异常。
     * English: Build a unified Dry-Run return payload guiding users to configure and trigger the real exception.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：执行结果对象 / English: Execution result object
     * 异常 / Exceptions: 无
     */
    protected ScenarioExecutionResult buildDryRunResult() {
        // 中文：将场景标识、异常类型与下一步操作指引一起返回
        // English: Return scenario id, exception type and next actions together
        return new ScenarioExecutionResult(getId(), true, false,
                "Dry-Run completed. Set dryRun=false to trigger the real scenario.",
                Map.of("exceptionType", getExceptionType()),
                List.of("按照返回的指南配置 JVM 参数并重新调用接口。"));
    }

    /**
     * 方法说明 / Method Description:
     * 中文：解析布尔参数，兼容字符串/布尔类型并提供默认值。
     * English: Parse a boolean parameter, supporting string/boolean types with a default fallback.
     *
     * 参数 / Parameters:
     * @param params 中文：参数映射 / English: Parameter map
     * @param key 中文：参数键 / English: Parameter key
     * @param defaultValue 中文：默认值 / English: Default value
     *
     * 返回值 / Return:
     * 中文：解析后的布尔值 / English: Parsed boolean value
     *
     * 异常 / Exceptions:
     * 中文：无，异常情况走默认值 / English: None; falls back to default on errors
     */
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

    /**
     * 方法说明 / Method Description:
     * 中文：解析整型参数，支持数字/字符串输入并在异常时使用默认值。
     * English: Parse an integer parameter, supporting numeric/string input and defaulting on errors.
     *
     * 参数 / Parameters:
     * @param params 中文：参数映射 / English: Parameter map
     * @param key 中文：参数键 / English: Parameter key
     * @param defaultValue 中文：默认值 / English: Default value
     *
     * 返回值 / Return:
     * 中文：解析后的整型值 / English: Parsed integer value
     *
     * 异常 / Exceptions:
     * 中文：捕获 NumberFormatException 并返回默认值 / English: Catches NumberFormatException and returns default
     */
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
