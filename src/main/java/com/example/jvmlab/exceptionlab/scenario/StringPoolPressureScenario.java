package com.example.jvmlab.exceptionlab.scenario;

import com.example.jvmlab.exceptionlab.AbstractMemoryExceptionScenario;
import com.example.jvmlab.exceptionlab.model.JvmMemoryArea;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioGuide;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 压测字符串常量池的实验实现（JDK8 之后常量池位于堆中）。
 */
@Component
public class StringPoolPressureScenario extends AbstractMemoryExceptionScenario {

    private static final List<String> STRING_HOLDER = new ArrayList<>();

    @Override
    public String getId() {
        return "string-pool-pressure";
    }

    @Override
    public String getDisplayName() {
        return "字符串常量池压力测试";
    }

    @Override
    public String getExceptionType() {
        return "java.lang.OutOfMemoryError: Java heap space";
    }

    @Override
    public JvmMemoryArea getMemoryArea() {
        return JvmMemoryArea.HEAP;
    }

    @Override
    public ScenarioGuide getGuide() {
        return new ScenarioGuide.Builder()
                .principle("JDK8 之后 intern() 会将字符串放入堆上的常量池，过量调用会与业务对象竞争堆空间。")
                .reproductionSteps(List.of(
                        "设置 -Xms32m -Xmx32m。",
                        "调用 /memory-exception-lab/scenarios/string-pool-pressure/execute?dryRun=false&batch=5000。",
                        "观察接口返回的常量池条目数量。"))
                .diagnosticSteps(List.of(
                        "在 MAT 中通过 Histogram 查看 java.lang.String 的 Retained Heap。",
                        "分析 GC Roots 链路，确认是否存在静态集合持有。"))
                .solutionSteps(List.of(
                        "避免无上限地调用 String.intern()。",
                        "必要时清理缓存或引入 LRU 策略。"))
                .recommendedJvmOptions(List.of("-Xms32m -Xmx32m"))
                .toolingTips(List.of(
                        "VisualVM → Sampler → Memory，关注 String 占比。",
                        "JProfiler → Heap Walker → String 引用链。"))
                .build();
    }

    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) {
        int batch = Math.max(1, parseInt(requestParams, "batch", 10_000));
        int startIndex = STRING_HOLDER.size();
        try {
            for (int i = 0; i < batch; i++) {
                String value = (startIndex + i) + "-jvm-lab";
                STRING_HOLDER.add(value.intern());
            }
            return new ScenarioExecutionResult(getId(), false, false,
                    "Appended " + batch + " strings to the intern pool",
                    Map.of("totalStrings", STRING_HOLDER.size()),
                    List.of("继续调用直至堆溢出，或通过 /chapter02/reset 清理静态集合"));
        } catch (OutOfMemoryError error) {
            return new ScenarioExecutionResult(getId(), false, true,
                    "Heap OOM after adding " + STRING_HOLDER.size() + " interned strings",
                    Map.of("totalStrings", STRING_HOLDER.size()),
                    List.of("分析常量池引用链，确认是否需要限流"));
        }
    }
}
