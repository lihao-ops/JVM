package com.example.jvmlab.exceptionlab.scenario;

import com.example.jvmlab.exceptionlab.AbstractMemoryExceptionScenario;
import com.example.jvmlab.exceptionlab.model.JvmMemoryArea;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioGuide;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 递归触发栈溢出的实验实现。
 */
@Component
public class StackOverflowScenario extends AbstractMemoryExceptionScenario {

    private final ThreadLocal<Integer> depthHolder = ThreadLocal.withInitial(() -> 0);
    private volatile int lastObservedDepth = 0;

    @Override
    public String getId() {
        return "stack-overflow";
    }

    @Override
    public String getDisplayName() {
        return "递归触发 StackOverflowError";
    }

    @Override
    public String getExceptionType() {
        return "java.lang.StackOverflowError";
    }

    @Override
    public JvmMemoryArea getMemoryArea() {
        return JvmMemoryArea.THREAD_PRIVATE;
    }

    @Override
    public ScenarioGuide getGuide() {
        return new ScenarioGuide.Builder()
                .principle("方法调用会在线程栈中分配栈帧，递归无终止条件会不断入栈直至耗尽线程栈空间。")
                .reproductionSteps(List.of(
                        "设置 JVM 参数 -Xss128k 缩小线程栈容量。",
                        "调用 /memory-exception-lab/scenarios/stack-overflow/execute?dryRun=false。",
                        "观察接口返回的 depth 指标以及服务端日志。"))
                .diagnosticSteps(List.of(
                        "使用 jstack <pid> 查看异常线程的调用栈。",
                        "在 IDE 中设置断点，验证递归终止条件是否生效。"))
                .solutionSteps(List.of(
                        "补充递归终止条件或改写为迭代实现。",
                        "若业务确实需要深递归，可适当提升 -Xss，但需评估线程数与内存占用。"))
                .recommendedJvmOptions(List.of("-Xss128k", "-XX:+PrintFlagsFinal | grep ThreadStackSize"))
                .toolingTips(List.of(
                        "JProfiler → CPU Views → Call Tree 查看调用链深度。",
                        "VisualVM → Sampler → CPU，定位热点递归方法。"))
                .build();
    }

    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) {
        depthHolder.set(0);
        try {
            triggerRecursiveCall();
        } catch (StackOverflowError error) {
            int depth = lastObservedDepth;
            Map<String, Object> metrics = Map.of(
                    "depth", depth,
                    "thread", Thread.currentThread().getName());
            return new ScenarioExecutionResult(getId(), false, true,
                    "StackOverflowError captured at depth " + depth,
                    metrics,
                    List.of("扩展栈容量或修复递归逻辑"));
        } finally {
            depthHolder.remove();
        }
        return new ScenarioExecutionResult(getId(), false, false,
                "Unexpectedly completed without StackOverflowError",
                Map.of(),
                List.of("确认 -Xss 是否配置过大"));
    }

    private void triggerRecursiveCall() {
        int currentDepth = depthHolder.get() + 1;
        depthHolder.set(currentDepth);
        lastObservedDepth = Math.max(lastObservedDepth, currentDepth);
        long padding1 = currentDepth;
        long padding2 = padding1 * 2;
        long padding3 = padding1 + padding2;
        if (padding3 % 5 == 0) {
            lastObservedDepth = Math.max(lastObservedDepth, currentDepth);
        }
        triggerRecursiveCall();
    }
}
