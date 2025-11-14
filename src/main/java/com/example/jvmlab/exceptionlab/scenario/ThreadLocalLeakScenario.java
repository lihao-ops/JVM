package com.example.jvmlab.exceptionlab.scenario;

import com.example.jvmlab.exceptionlab.AbstractMemoryExceptionScenario;
import com.example.jvmlab.exceptionlab.model.JvmMemoryArea;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioGuide;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 类说明 / Class Description:
 * 中文：模拟 ThreadLocal 泄漏导致堆内存压力的场景，通过在线程中放置大对象且不清理，观察泄漏行为。
 * English: Simulate heap pressure due to ThreadLocal leak by placing large objects in threads without cleanup and observing behavior.
 *
 * 使用场景 / Use Cases:
 * 中文：与《深入理解 Java 虚拟机》第3版中的内存泄漏讨论相呼应，演示 ThreadLocal 使用不当的后果。
 * English: Echo discussion in the book about memory leaks, demonstrating consequences of improper ThreadLocal usage.
 *
 * 设计目的 / Design Purpose:
 * 中文：通过持有 ThreadLocal 引用和静态集合，稳定复现泄漏并提供诊断与解决方案指引。
 * English: Use ThreadLocal and static holders to reliably reproduce leaks and provide diagnostic/solution guidance.
 */
@Slf4j
@Component
public class ThreadLocalLeakScenario extends AbstractMemoryExceptionScenario {

    private static final ThreadLocal<List<byte[]>> HOLDER = ThreadLocal.withInitial(ArrayList::new);
    private static final List<List<byte[]>> LEAK_GUARD = new ArrayList<>();

    @Override
    public String getId() {
        return "threadlocal-leak";
    }

    @Override
    public String getDisplayName() {
        return "ThreadLocal 泄漏导致堆压力";
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
                .principle("ThreadLocal 的 Entry 使用弱引用保存键，但值对象若被静态集合间接持有，将导致无法回收，产生泄漏。")
                .reproductionSteps(List.of(
                        "设置 -Xms64m -Xmx64m",
                        "调用 /memory-exception-lab/scenarios/threadlocal-leak/execute?dryRun=false&entries=500&sizeKb=256",
                        "观察接口返回的分配统计与服务端日志"))
                .diagnosticSteps(List.of(
                        "通过 jmap -histo 查看 byte[] 或自定义对象占比",
                        "使用 MAT 查看 GC Roots 链路，确认 ThreadLocalMap 中的 Value 被静态集合间接引用"))
                .solutionSteps(List.of(
                        "在使用 ThreadLocal 后及时调用 remove()",
                        "避免将 ThreadLocal 值对象放入静态集合"))
                .recommendedJvmOptions(List.of("-Xms64m -Xmx64m"))
                .toolingTips(List.of(
                        "JProfiler → Heap Walker → Reference Graph",
                        "VisualVM → Sampler → Memory"))
                .build();
    }

    /**
     * 方法说明 / Method Description:
     * 中文：向 ThreadLocal 中追加指定数量的大对象，并将其列表加入静态集合，模拟值对象无法释放的泄漏场景。
     * English: Append specified number of large objects to ThreadLocal and keep the list in a static collection to simulate leaks.
     *
     * 参数 / Parameters:
     * @param requestParams 中文：entries 追加数量；sizeKb 每对象大小（KB） / English: entries count; sizeKb per-object size in KB
     *
     * 返回值 / Return:
     * 中文：执行结果与指标 / English: Execution result with metrics
     *
     * 异常 / Exceptions:
     * 中文：可能抛出 OutOfMemoryError / English: May throw OutOfMemoryError
     */
    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) {
        int entries = Math.max(1, parseInt(requestParams, "entries", 1000));
        int sizeKb = Math.max(1, parseInt(requestParams, "sizeKb", 256));
        List<byte[]> list = HOLDER.get();
        int added = 0;
        try {
            for (int i = 0; i < entries; i++) {
                // 中文：创建指定大小的字节数组并放入 ThreadLocal 列表
                // English: Create a byte array of given size and store into ThreadLocal list
                list.add(new byte[sizeKb * 1024]);
                added++;
            }
            // 中文：将列表放入静态集合，模拟业务误用导致的值对象无法释放
            // English: Put list into static collection to simulate misuse keeping value objects alive
            LEAK_GUARD.add(list);
            return new ScenarioExecutionResult(getId(), false, false,
                    "Appended " + added + " entries into ThreadLocal list",
                    Map.of("entries", added, "sizeKb", sizeKb, "totalLists", LEAK_GUARD.size()),
                    List.of("调用 ThreadLocal.remove() 并清理静态集合以解除引用"));
        } catch (OutOfMemoryError error) {
            log.info("【成功】ThreadLocal 泄漏触发 Heap OOM，entries={} sizeKb={} / Success: ThreadLocal leak OOM", added, sizeKb);
            return new ScenarioExecutionResult(getId(), false, true,
                    "Heap OOM due to ThreadLocal leak after appending " + added + " entries",
                    Map.of("entries", added, "sizeKb", sizeKb, "totalLists", LEAK_GUARD.size()),
                    List.of("调用 /chapter02/reset 释放静态集合并移除 ThreadLocal"));
        }
    }
}
