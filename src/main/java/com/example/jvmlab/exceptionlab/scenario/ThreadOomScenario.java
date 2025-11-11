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
 * 模拟 "unable to create new native thread" 的实验实现。
 */
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

    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) {
        int maxThreads = Math.max(1, parseInt(requestParams, "maxThreads", 5_000));
        List<Thread> startedThreads = new ArrayList<>();
        int count = 0;
        try {
            while (count < maxThreads) {
                Thread thread = new Thread(() -> {
                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }, "oom-thread-" + count);
                thread.setDaemon(false);
                thread.start();
                startedThreads.add(thread);
                count++;
            }
            return new ScenarioExecutionResult(getId(), false, false,
                    "Created " + count + " threads without hitting OS limit",
                    Map.of("createdThreads", count),
                    List.of("提高 maxThreads 或调整 ulimit 限制"));
        } catch (OutOfMemoryError error) {
            startedThreads.forEach(Thread::interrupt);
            return new ScenarioExecutionResult(getId(), false, true,
                    "Unable to create new native thread after " + count + " threads",
                    Map.of("createdThreads", count),
                    List.of("检查线程池配置或系统限制"));
        }
    }
}
