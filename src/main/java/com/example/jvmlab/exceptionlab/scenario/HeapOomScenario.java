package com.example.jvmlab.exceptionlab.scenario;

import com.example.jvmlab.exceptionlab.AbstractMemoryExceptionScenario;
import com.example.jvmlab.exceptionlab.model.JvmMemoryArea;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioGuide;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 模拟 Java 堆溢出的实验实现。
 */
@Component
public class HeapOomScenario extends AbstractMemoryExceptionScenario {

    private static final List<byte[]> HEAP_STORAGE = new CopyOnWriteArrayList<>();

    @Override
    public String getId() {
        return "heap-oom";
    }

    @Override
    public String getDisplayName() {
        return "堆空间触发 OutOfMemoryError";
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
                .principle("堆中对象无法及时回收且持续分配，最终触发 Java heap space OOM。")
                .reproductionSteps(List.of(
                        "设置 -Xms20m -Xmx20m 固定堆大小。",
                        "调用 /memory-exception-lab/scenarios/heap-oom/execute?dryRun=false&sizeMb=1。",
                        "等待接口抛出 OutOfMemoryError，并查看生成的 heap dump (需配置 -XX:+HeapDumpOnOutOfMemoryError)。"))
                .diagnosticSteps(List.of(
                        "使用 MAT/JProfiler 打开 heap dump，查看 Retained Size 最大的对象。",
                        "定位是否存在 GC Roots 无法断开的引用链。"))
                .solutionSteps(List.of(
                        "修复集合未清理、ThreadLocal 泄漏等问题。",
                        "确认内存确实不足时扩大堆或优化对象生命周期。"))
                .recommendedJvmOptions(List.of(
                        "-Xms20m -Xmx20m",
                        "-XX:+HeapDumpOnOutOfMemoryError",
                        "-XX:HeapDumpPath=logs/heapdump.hprof"))
                .toolingTips(List.of(
                        "JProfiler → Heap Walker → Biggest Objects。",
                        "VisualVM → Heap Dump → Objects 标签页。"))
                .build();
    }

    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) throws InterruptedException {
        int sizeMb = Math.max(1, parseInt(requestParams, "sizeMb", 1));
        int delayMs = Math.max(0, parseInt(requestParams, "delayMs", 50));
        long allocationCount = 0;
        try {
            while (true) {
                byte[] block = new byte[sizeMb * 1024 * 1024];
                HEAP_STORAGE.add(block);
                allocationCount++;
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
            }
        } catch (OutOfMemoryError error) {
            Map<String, Object> metrics = Map.of(
                    "allocations", allocationCount,
                    "eachBlockMb", sizeMb,
                    "storageSizeMb", allocationCount * sizeMb);
            return new ScenarioExecutionResult(getId(), false, true,
                    "Java heap space OOM after " + allocationCount + " allocations",
                    metrics,
                    List.of("分析 heap dump 或调整 sizeMb/delayMs 控制触发速度"));
        }
    }
}
