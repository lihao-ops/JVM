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
 * 模拟 GC Overhead Limit Exceeded 的实验实现。
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

    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) {
        boolean internStrings = parseBoolean(requestParams, "internStrings", true);
        Map<Integer, String> pressureMap = new HashMap<>();
        int counter = 0;
        try {
            while (true) {
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
