package com.example.jvmlab.exceptionlab.scenario;

import com.example.jvmlab.exceptionlab.AbstractMemoryExceptionScenario;
import com.example.jvmlab.exceptionlab.model.JvmMemoryArea;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioGuide;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 类说明 / Class Description:
 * 中文：通过无终止条件递归触发线程栈溢出，采集最大递归深度与线程信息。
 * English: Triggers stack overflow via unbounded recursion, collecting max depth and thread info.
 *
 * 使用场景 / Use Cases:
 * 中文：演示 -Xss 配置影响与调用栈诊断，教学递归与迭代的差异。
 * English: Demonstrate impact of -Xss and call stack diagnostics; teach recursion vs iteration.
 *
 * 设计目的 / Design Purpose:
 * 中文：使用 ThreadLocal 记录递归深度，保证线程安全且便于复用。
 * English: Use ThreadLocal to record recursion depth for thread safety and reuse.
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

    /**
     * 方法说明 / Method Description:
     * 中文：执行递归触发逻辑并在捕获 StackOverflowError 后返回指标。
     * English: Execute recursive trigger logic and return metrics after capturing StackOverflowError.
     *
     * 参数 / Parameters:
     * @param requestParams 中文：无需特别参数 / English: No special parameters needed
     *
     * 返回值 / Return:
     * 中文：包含深度与线程名的执行结果 / English: Result including depth and thread name
     *
     * 异常 / Exceptions:
     * 中文：可能抛出 StackOverflowError / English: May throw StackOverflowError
     */
    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) {
        depthHolder.set(0);
        try {
            // 中文：进入无终止条件递归，持续入栈直至栈空间耗尽
            // English: Enter unbounded recursion, pushing stack frames until exhaustion
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
            // 中文：移除 ThreadLocal，避免线程复用导致的污染
            // English: Remove ThreadLocal to avoid contamination across reused threads
            depthHolder.remove();
        }
        return new ScenarioExecutionResult(getId(), false, false,
                "Unexpectedly completed without StackOverflowError",
                Map.of(),
                List.of("确认 -Xss 是否配置过大"));
    }

    /**
     * 方法说明 / Method Description:
     * 中文：递归方法，更新最大深度并创建若干局部变量以增加栈帧大小。
     * English: Recursive method updating max depth and creating locals to enlarge stack frame.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 递归至极限时抛出 StackOverflowError
     */
    private void triggerRecursiveCall() {
        int currentDepth = depthHolder.get() + 1;
        // 中文：写入当前深度到 ThreadLocal，便于跨方法读取
        // English: Write current depth into ThreadLocal for cross-method access
        depthHolder.set(currentDepth);
        // 中文：更新观测到的最大深度
        // English: Update last observed max depth
        lastObservedDepth = Math.max(lastObservedDepth, currentDepth);
        // 中文：填充若干局部变量，人工增大栈帧占用
        // English: Create local variables to artificially increase stack frame size
        long padding1 = currentDepth;
        long padding2 = padding1 * 2;
        long padding3 = padding1 + padding2;
        if (padding3 % 5 == 0) {
            // 中文：条件分支进一步保持访问，避免被编译器过度优化
            // English: Conditional branch to prevent excessive compiler optimization
            lastObservedDepth = Math.max(lastObservedDepth, currentDepth);
        }
        // 中文：尾部继续递归，直到抛出 StackOverflowError
        // English: Recurse again until StackOverflowError is thrown
        triggerRecursiveCall();
    }
}
