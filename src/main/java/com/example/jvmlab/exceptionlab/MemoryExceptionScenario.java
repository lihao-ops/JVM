package com.example.jvmlab.exceptionlab;

import com.example.jvmlab.exceptionlab.model.JvmMemoryArea;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioGuide;

import java.util.Map;

/**
 * 所有内存异常实验的统一接口，便于通过策略模式动态选择实验。
 */
public interface MemoryExceptionScenario {

    /**
     * 场景的唯一标识符，例如 "stack-overflow"。
     */
    String getId();

    /**
     * 场景的展示名称，适合用于前端菜单或文档标题。
     */
    String getDisplayName();

    /**
     * 对应的异常类型，例如 "StackOverflowError"。
     */
    String getExceptionType();

    /**
     * 当前异常覆盖的 JVM 内存区域。
     */
    JvmMemoryArea getMemoryArea();

    /**
     * 返回面试官视角的指导手册。
     */
    ScenarioGuide getGuide();

    /**
     * 执行实验逻辑，允许通过参数自定义行为（如dryRun、分配大小等）。
     *
     * @param requestParams Web 层传入的参数。
     * @return 执行结果对象。
     * @throws Exception 业务执行过程中出现的任何异常。
     */
    ScenarioExecutionResult execute(Map<String, Object> requestParams) throws Exception;
}
