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
 * 类说明 / Class Description:
 * 中文：压力测试字符串常量池（JDK8 之后位于堆中），观察 intern 行为与堆占用。
 * English: Stress test the string intern pool (on heap since JDK8), observing intern behavior and heap usage.
 *
 * 使用场景 / Use Cases:
 * 中文：评估过度使用 String.intern() 对内存的影响，学习常量池引用链分析。
 * English: Evaluate impact of excessive String.intern() and learn constant pool reference chain analysis.
 *
 * 设计目的 / Design Purpose:
 * 中文：通过批量追加 intern 字符串到静态集合，稳定复现内存压力或 OOM。
 * English: Append interned strings to a static collection to reliably reproduce memory pressure or OOM.
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

    /**
     * 方法说明 / Method Description:
     * 中文：按批次参数生成并 intern 字符串，统计总量并在 OOM 时返回指标。
     * English: Generate and intern strings per batch, track totals and return metrics upon OOM.
     *
     * 参数 / Parameters:
     * @param requestParams 中文：batch 追加数量、起始下标等参数 / English: batch count, starting index parameters
     *
     * 返回值 / Return:
     * 中文：执行结果与总字符串数量 / English: Result with total string count
     *
     * 异常 / Exceptions:
     * 中文：可能抛出 OutOfMemoryError / English: May throw OutOfMemoryError
     */
    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) {
        int batch = Math.max(1, parseInt(requestParams, "batch", 10_000));
        int startIndex = STRING_HOLDER.size();
        try {
            for (int i = 0; i < batch; i++) {
                // 中文：生成唯一字符串并放入常量池，保留引用避免回收
                // English: Generate unique string, intern it and retain reference to avoid reclamation
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
