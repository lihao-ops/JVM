package com.example.jvmlab.exceptionlab.scenario;

import com.example.jvmlab.exceptionlab.AbstractMemoryExceptionScenario;
import com.example.jvmlab.exceptionlab.model.JvmMemoryArea;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioGuide;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模拟直接内存（堆外内存）溢出的实验实现。
 */
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

    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) {
        int sizeMb = Math.max(1, parseInt(requestParams, "sizeMb", 1));
        int allocations = 0;
        try {
            while (true) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(sizeMb * 1024 * 1024);
                DIRECT_BUFFERS.add(buffer);
                allocations++;
            }
        } catch (OutOfMemoryError error) {
            return new ScenarioExecutionResult(getId(), false, true,
                    "Direct buffer memory OOM after " + allocations + " allocations",
                    Map.of("allocations", allocations, "blockSizeMb", sizeMb),
                    List.of("调用 /chapter02/reset 释放已分配的直接内存"));
        }
    }
}
