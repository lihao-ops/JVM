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
 * 中文：不断创建非守护线程并保持睡眠占用资源，模拟“无法创建新本地线程”。
 * English: Continuously create non-daemon threads that sleep to occupy resources, simulating "unable to create new native thread".
 *
 * 使用场景 / Use Cases:
 * 中文：演示操作系统线程上限与 -Xss 栈大小设置的关系，辅助排查线程泄漏。
 * English: Demonstrate OS thread limits and -Xss stack size relation, aiding thread leak diagnosis.
 *
 * 设计目的 / Design Purpose:
 * 中文：以可配置的目标数量与命名前缀创建线程，便于监控与定位。
 * English: Create threads with configurable target count and name prefix for monitoring and identification.
 */
@Slf4j
@Component
public class ThreadOomScenario extends AbstractMemoryExceptionScenario {

    @Override
    public String getId() {
        return "thread-oom";
    }

    @Override
    public String getDisplayName() {
        return "线程资源耗尽";
    }

    @Override
    public String getExceptionType() {
        return "java.lang.OutOfMemoryError: unable to create new native thread";
    }

    @Override
    public JvmMemoryArea getMemoryArea() {
        return JvmMemoryArea.NATIVE_THREAD;
    }

    @Override
    public ScenarioGuide getGuide() {
        return new ScenarioGuide.Builder()
                .principle("每个 Java 线程都需要向操作系统申请本地线程和线程栈空间，数量达到系统上限时会抛出该异常。")
                .reproductionSteps(List.of(
                        "设置 -Xss256k 减少单线程的栈内存，提升可创建数量。",
                        "调用 /memory-exception-lab/scenarios/thread-oom/execute?dryRun=false&maxThreads=10000。",
                        "关注接口返回的创建数量，并结合系统 ulimit -u 设置。"))
                .diagnosticSteps(List.of(
                        "执行 jstack <pid> | grep 'tid' 统计线程数。",
                        "使用 top -H -p <pid> 观察线程资源占用。"))
                .solutionSteps(List.of(
                        "排查是否存在线程泄漏或线程池滥用。",
                        "在容器环境中调整 ulimit 或 cgroup 的线程数限制。"))
                .recommendedJvmOptions(List.of("-Xss256k"))
                .toolingTips(List.of(
                        "JProfiler → Threads → Thread History。",
                        "VisualVM → Threads 面板。"))
                .build();
    }

    /**
     * 方法说明 / Method Description:
     * 中文：按参数创建线程并保持存活，捕获 OOM 后返回创建数量与建议。
     * English: Create threads per parameter and keep them alive; return counts and advice after OOM.
     *
     * 参数 / Parameters:
     * @param requestParams 中文：maxThreads 目标线程数 / English: maxThreads target thread count
     *
     * 返回值 / Return:
     * 中文：执行结果与指标 / English: Execution result with metrics
     *
     * 异常 / Exceptions:
     * 中文：可能抛出 OutOfMemoryError / English: May throw OutOfMemoryError
     */
    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) {
        int maxThreads = Math.max(1, parseInt(requestParams, "maxThreads", 5_000));
        List<Thread> startedThreads = new ArrayList<>();
        int count = 0;
        try {
            while (count < maxThreads) {
                // 中文：每个线程休眠以保持栈与本地资源占用
                // English: Each thread sleeps to keep stack and native resources occupied
                Thread thread = new Thread(() -> {
                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }, "oom-thread-" + count);
                thread.setDaemon(false);
                // 中文：启动线程并保存引用以便统一中断
                // English: Start thread and retain reference for unified interruption
                thread.start();
                startedThreads.add(thread);
                count++;
            }
            return new ScenarioExecutionResult(getId(), false, false,
                    "Created " + count + " threads without hitting OS limit",
                    Map.of("createdThreads", count),
                    List.of("提高 maxThreads 或调整 ulimit 限制"));
        } catch (OutOfMemoryError error) {
            // 中文：在异常场景下尽可能中断已创建线程，降低资源占用
            // English: Interrupt created threads to reduce resource usage upon error
            startedThreads.forEach(Thread::interrupt);
            // 中文：成功触发 unable to create new native thread，打印成功确认日志
            // English: Successfully triggered unable to create new native thread; print success confirmation log
            log.info("【成功】Native Thread OOM 触发，已创建线程数={} / Success: native thread OOM triggered", count);
            return new ScenarioExecutionResult(getId(), false, true,
                    "Unable to create new native thread after " + count + " threads",
                    Map.of("createdThreads", count),
                    List.of("检查线程池配置或系统限制"));
        }
    }
}
