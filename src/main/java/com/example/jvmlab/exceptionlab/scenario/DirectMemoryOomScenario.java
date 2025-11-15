package com.example.jvmlab.exceptionlab.scenario;

import com.example.jvmlab.exceptionlab.AbstractMemoryExceptionScenario;
import com.example.jvmlab.exceptionlab.model.JvmMemoryArea;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioGuide;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 类说明 / Class Description:
 * 中文：使用 DirectByteBuffer 持续申请堆外内存并保留引用，触发直接内存 OOM。
 * English: Continuously allocate off-heap memory via DirectByteBuffer while retaining references to trigger direct memory OOM.
 *
 * 使用场景 / Use Cases:
 * 中文：演示 -XX:MaxDirectMemorySize 对行为的约束及 NMT 分析方法。
 * English: Demonstrate constraints via -XX:MaxDirectMemorySize and analysis via NMT.
 *
 * 设计目的 / Design Purpose:
 * 中文：以列表保存 ByteBuffer 引用，防止 Cleaner 提前释放，稳定复现异常。
 * English: Store ByteBuffer references to avoid early Cleaner release for stable reproduction.
 */
@Slf4j
@Component
public class DirectMemoryOomScenario extends AbstractMemoryExceptionScenario {

    private static final List<ByteBuffer> DIRECT_BUFFERS = new ArrayList<>();

    @Override
    public String getId() {
        return "direct-memory-oom";
    }

    @Override
    public String getDisplayName() {
        return "直接内存触发 OutOfMemoryError";
    }

    @Override
    public String getExceptionType() {
        return "java.lang.OutOfMemoryError: Direct buffer memory";
    }

    @Override
    public JvmMemoryArea getMemoryArea() {
        return JvmMemoryArea.DIRECT_MEMORY;
    }

    @Override
    public ScenarioGuide getGuide() {
        return new ScenarioGuide.Builder()
                .principle("DirectByteBuffer 使用堆外内存，如果未及时释放或 MaxDirectMemorySize 设置过小会触发 OOM。")
                .reproductionSteps(List.of(
                        "设置 -XX:MaxDirectMemorySize=10m。",
                        "调用 /memory-exception-lab/scenarios/direct-memory-oom/execute?dryRun=false&sizeMb=1。",
                        "观察接口快速抛出 OutOfMemoryError。"))
                .diagnosticSteps(List.of(
                        "执行 jcmd <pid> VM.native_memory summary | grep -i internal。",
                        "排查是否有 DirectByteBuffer 未被回收（可使用 Netty 泄漏检测）。"))
                .solutionSteps(List.of(
                        "复用直接内存缓冲区，或及时调用 Cleaner 释放。",
                        "在高吞吐场景下使用池化技术，例如 Netty PooledByteBufAllocator。"))
                .recommendedJvmOptions(List.of("-XX:MaxDirectMemorySize=10m"))
                .toolingTips(List.of(
                        "NMT (Native Memory Tracking) 分析直接内存使用。",
                        "JProfiler → Memory Views → Allocation Call Tree。"))
                .build();
    }

    /**
     * 方法说明 / Method Description:
     * 中文：按块大小分配直接内存，直到抛出 OOM，返回分配次数与块大小。
     * English: Allocate direct memory blocks until OOM, returning allocation count and block size.
     *
     * 参数 / Parameters:
     * @param requestParams 中文：sizeMb 每块大小参数 / English: sizeMb per-block size parameter
     *
     * 返回值 / Return:
     * 中文：执行结果与指标 / English: Execution result with metrics
     *
     * 异常 / Exceptions:
     * 中文：可能抛出 OutOfMemoryError / English: May throw OutOfMemoryError
     */
    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) {
        int sizeMb = Math.max(1, parseInt(requestParams, "sizeMb", 1));
        int allocations = 0;
        try {
            while (true) {
                // 中文：分配指定大小的直接缓冲区并保存引用，避免被回收
                // English: Allocate a direct buffer of given size and retain reference to avoid reclamation
                ByteBuffer buffer = ByteBuffer.allocateDirect(sizeMb * 1024 * 1024);
                DIRECT_BUFFERS.add(buffer);
                allocations++;
            }
        } catch (OutOfMemoryError error) {
            // 中文：成功触发 Direct Memory OOM，打印成功确认日志
            // English: Successfully triggered Direct Memory OOM; print success confirmation log
            log.info("【成功】Direct Memory OOM 触发，分配次数={}，块大小={}MB / Success: Direct OOM triggered", allocations, sizeMb);
            return new ScenarioExecutionResult(getId(), false, true,
                    "Direct buffer memory OOM after " + allocations + " allocations",
                    Map.of("allocations", allocations, "blockSizeMb", sizeMb),
                    List.of("调用 /chapter02/reset 释放已分配的直接内存"));
        }
    }
}
