package com.example.jvmlab.exceptionlab.scenario;

import com.example.jvmlab.exceptionlab.AbstractMemoryExceptionScenario;
import com.example.jvmlab.exceptionlab.model.JvmMemoryArea;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioGuide;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 类说明 / Class Description:
 * 中文：模拟 Java 堆溢出的场景实现，通过持续分配对象并保持引用触发 OOM。
 * English: Scenario implementation simulating Java heap OOM by continuously allocating objects while retaining references.
 *
 * 使用场景 / Use Cases:
 * 中文：演示堆空间不足导致的 OutOfMemoryError，配合 heap dump 与分析工具学习内存泄漏定位。
 * English: Demonstrate OutOfMemoryError due to heap exhaustion; used with heap dump tools to learn leak detection.
 *
 * 设计目的 / Design Purpose:
 * 中文：以线程安全的集合持有分配块，控制分配速度与块大小，稳定复现 OOM。
 * English: Use a thread-safe collection to retain allocation blocks and control speed/size to reliably reproduce OOM.
 */
@Slf4j
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

    /**
     * 方法说明 / Method Description:
     * 中文：执行堆 OOM 实验，按指定块大小持续分配并可选延迟，直到抛出 OOM。
     * English: Execute heap OOM experiment by continuously allocating blocks with optional delay until OOM occurs.
     *
     * 参数 / Parameters:
     * @param requestParams 中文：sizeMb 每块大小、delayMs 分配间隔等参数 / English: sizeMb per block, delayMs between allocations, etc.
     *
     * 返回值 / Return:
     * 中文：异常捕获后返回执行结果与指标 / English: Returns execution result with metrics after catching OOM
     *
     * 异常 / Exceptions:
     * 中文：可能抛出 InterruptedException（休眠中断）或 OOM / English: May throw InterruptedException during sleep or OOM
     */
    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) throws InterruptedException {
        int sizeMb = Math.max(1, parseInt(requestParams, "sizeMb", 1));
        int delayMs = Math.max(0, parseInt(requestParams, "delayMs", 50));
        long allocationCount = 0;
        try {
            while (true) {
                // 中文：分配指定大小的字节块并保存引用，防止 GC 回收
                // English: Allocate a byte block of given size and retain reference to prevent GC reclamation
                byte[] block = new byte[sizeMb * 1024 * 1024];
                // 中文：使用线程安全集合保存，适应并发调用场景
                // English: Use thread-safe collection to store for potential concurrent calls
                HEAP_STORAGE.add(block);
                allocationCount++;
                if (delayMs > 0) {
                    // 中文：可选延迟以控制触发速度并便于观察监控曲线
                    // English: Optional delay to control trigger speed and observe monitoring curves
                    Thread.sleep(delayMs);
                }
            }
        } catch (OutOfMemoryError error) {
            // 中文：成功触发 Heap OOM，打印成功确认日志
            // English: Successfully triggered Heap OOM; print success confirmation log
            log.info("【成功】Heap OOM 触发，分配次数={}，块大小={}MB / Success: Heap OOM triggered", allocationCount, sizeMb);
            // 中文：采集核心指标用于复盘（分配次数、块大小、累计占用）
            // English: Collect key metrics for post-mortem (allocations, block size, total)
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
