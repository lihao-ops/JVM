package com.example.jvmlab.exceptionlab.scenario;

import com.example.jvmlab.exceptionlab.AbstractMemoryExceptionScenario;
import com.example.jvmlab.exceptionlab.model.JvmMemoryArea;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioGuide;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类说明 / Class Description:
 * 中文：通过构造高频 GC 且回收效果有限的压力场景，模拟 GC Overhead Limit Exceeded。
 * English: Simulate GC Overhead Limit Exceeded by creating pressure where GC is frequent but ineffective.
 *
 * 使用场景 / Use Cases:
 * 中文：学习如何从 GC 日志与监控中识别 GC 过度开销问题。
 * English: Learn to identify excessive GC overhead via logs and monitoring.
 *
 * 设计目的 / Design Purpose:
 * 中文：通过 Map 持有大量短生命周期对象或 intern 字符串，压迫堆空间。
 * English: Use a Map to hold many short-lived or interned strings to pressure the heap.
 */
@Component
public class GcOverheadScenario extends AbstractMemoryExceptionScenario {

    @Override
    public String getId() {
        return "gc-overhead";
    }

    @Override
    public String getDisplayName() {
        return "GC Overhead Limit Exceeded";
    }

    @Override
    public String getExceptionType() {
        return "java.lang.OutOfMemoryError: GC overhead limit exceeded";
    }

    @Override
    public JvmMemoryArea getMemoryArea() {
        return JvmMemoryArea.HEAP;
    }

    @Override
    public ScenarioGuide getGuide() {
        return new ScenarioGuide.Builder()
                .principle("当 GC 时间占比超过 98% 且仅回收不到 2% 的堆空间时，JVM 会抛出该异常以阻止系统进入长时间 Full GC。")
                .reproductionSteps(List.of(
                        "设置 -Xms10m -Xmx10m 并启用 -XX:+UseParallelGC。",
                        "调用 /memory-exception-lab/scenarios/gc-overhead/execute?dryRun=false&internStrings=true。",
                        "持续观察 GC 日志或 JMX 指标，直至抛出异常。"))
                .diagnosticSteps(List.of(
                        "开启 -Xlog:gc* 观察 Full GC 触发频率。",
                        "在 VisualVM 中查看堆使用率是否在高位反复波动。"))
                .solutionSteps(List.of(
                        "与 Heap OOM 类似，排查是否有对象无法释放。",
                        "优化数据结构或增加堆内存，避免频繁 Full GC。"))
                .recommendedJvmOptions(List.of(
                        "-Xms10m -Xmx10m",
                        "-XX:+UseParallelGC",
                        "-XX:+PrintGCDetails"))
                .toolingTips(List.of(
                        "VisualVM → Monitor → GC 图表。",
                        "JProfiler → Telemetries → Garbage Collector。"))
                .build();
    }

    /**
     * 方法说明 / Method Description:
     * 中文：构造不断增长的 Map，以可选 intern 行为提高 GC 压力，捕获 OOM 后返回指标。
     * English: Grow a Map continuously with optional intern behavior to increase GC pressure, then return metrics after OOM.
     *
     * 参数 / Parameters:
     * @param requestParams 中文：internStrings 是否调用 intern / English: internStrings whether to intern strings
     *
     * 返回值 / Return:
     * 中文：执行结果与指标 / English: Execution result with metrics
     *
     * 异常 / Exceptions:
     * 中文：可能抛出 OutOfMemoryError / English: May throw OutOfMemoryError
     */
    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) {
        boolean internStrings = parseBoolean(requestParams, "internStrings", true);
        Map<Integer, String> pressureMap = new HashMap<>();
        int counter = 0;
        try {
            while (true) {
                // 中文：生成字符串并根据参数决定是否放入常量池
                // English: Generate a string and decide whether to intern based on parameter
                String value = "value" + counter;
                pressureMap.put(counter, internStrings ? value.intern() : value);
                counter++;
            }
        } catch (OutOfMemoryError error) {
            Map<String, Object> metrics = Map.of(
                    "entries", counter,
                    "usedIntern", internStrings);
            return new ScenarioExecutionResult(getId(), false, true,
                    "GC overhead limit exceeded after inserting " + counter + " entries",
                    metrics,
                    List.of("结合 GC 日志确认是堆压力而非其他原因"));
        }
    }
}
