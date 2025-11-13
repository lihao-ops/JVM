package com.example.jvmlab.exceptionlab;

import com.example.jvmlab.exceptionlab.model.JvmMemoryArea;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioGuide;

import java.util.Map;

/**
 * 类说明 / Class Description:
 * 中文：内存异常实验的统一接口，定义场景标识、展示信息、指导手册与执行入口。
 * English: Unified interface for memory exception experiments, defining identity, display info, guide and execution entry.
 *
 * 使用场景 / Use Cases:
 * 中文：通过策略模式在服务层按 ID 动态选择并执行具体异常场景。
 * English: Used by the service layer to select and execute scenarios dynamically by ID via the strategy pattern.
 *
 * 设计目的 / Design Purpose:
 * 中文：抽象不同异常类型的共同能力，便于扩展与统一对接控制器。
 * English: Abstract common capabilities across exception types for extensibility and uniform controller integration.
 */
public interface MemoryExceptionScenario {

    /**
     * 方法说明 / Method Description:
     * 中文：返回场景的唯一标识符，例如 "stack-overflow"。
     * English: Returns the unique identifier for this scenario, e.g., "stack-overflow".
     *
     * 参数 / Parameters:
     * 无
     *
     * 返回值 / Return:
     * 中文：场景 ID / English: Scenario ID
     *
     * 异常 / Exceptions:
     * 无
     */
    String getId();

    /**
     * 方法说明 / Method Description:
     * 中文：返回场景展示名称，用于前端菜单或文档标题。
     * English: Returns scenario display name for UI menus or documentation titles.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：展示名称 / English: Display name
     * 异常 / Exceptions: 无
     */
    String getDisplayName();

    /**
     * 方法说明 / Method Description:
     * 中文：返回预期触发的异常类型描述，例如 "StackOverflowError"。
     * English: Returns the expected exception type description, e.g., "StackOverflowError".
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：异常类型字符串 / English: Exception type string
     * 异常 / Exceptions: 无
     */
    String getExceptionType();

    /**
     * 方法说明 / Method Description:
     * 中文：返回该场景关联的 JVM 内存区域枚举。
     * English: Returns the JVM memory area associated with this scenario.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：JVM 内存区域枚举 / English: JVM memory area enum
     * 异常 / Exceptions: 无
     */
    JvmMemoryArea getMemoryArea();

    /**
     * 方法说明 / Method Description:
     * 中文：返回场景的操作指南，涵盖原理、复现、诊断与解决建议。
     * English: Returns the scenario guide covering principles, reproduction, diagnostics and solutions.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：指导手册对象 / English: Guide object
     * 异常 / Exceptions: 无
     */
    ScenarioGuide getGuide();

    /**
     * 方法说明 / Method Description:
     * 中文：执行实验逻辑，支持 dryRun 与自定义参数（分配大小、延迟等）。
     * English: Execute the experiment, supporting dryRun and custom params (allocation size, delays, etc.).
     *
     * 参数 / Parameters:
     * @param requestParams 中文：请求参数映射 / English: Request parameter map
     *
     * 返回值 / Return:
     * 中文：执行结果模型 / English: Execution result model
     *
     * 异常 / Exceptions:
     * 中文：可能抛出运行时异常或 OOM / English: May throw runtime exceptions or OOM
     */
    ScenarioExecutionResult execute(Map<String, Object> requestParams) throws Exception;
}
