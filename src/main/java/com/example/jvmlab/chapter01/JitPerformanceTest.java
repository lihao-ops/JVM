package com.example.jvmlab.chapter01;

import lombok.extern.slf4j.Slf4j;

/**
 * 第一章实战：JIT 编译器 vs 解释器 性能对比
 * 
 * 【实验目标】
 * 1. 体验纯解释模式 (-Xint) 的慢。
 * 2. 体验混合模式/JIT 的快。
 * 3. 观察 -XX:+PrintCompilation 的输出。
 * 
 * 【推荐 VM 参数】
 * - 解释模式: -Xint (很慢)
 * - JIT 模式: 默认 (很快)
 * - 观察编译: -XX:+PrintCompilation
 * 
 * 【对应书籍】《深入理解Java虚拟机（第3版）》第1章
 * 
 * @author JVM实战专家
 * @version 2.0
 */
@Slf4j
public class JitPerformanceTest {

    // 循环次数：从 100万 提升到 1亿！
    // 解释模式下，这可能会跑好几秒甚至十几秒。
    // JIT模式下，依然会非常快。
    private static final int LOOP_COUNT = 100_000_000;

    public static void main(String[] args) {
        log.info("开始测试...");
        log.info("当前 JVM 运行模式: {}", System.getProperty("java.vm.info"));

        long startTime = System.nanoTime();

        // 执行热点代码
        long result = heavyComputation();

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;

        log.info("--------------------------------------------------");
        log.info("计算结果: {}", result);
        log.info("耗时: {} ms", durationMs);
        log.info("--------------------------------------------------");
        
        // 【面试考点】
        // Q: 为什么 JIT 模式比解释模式快这么多？
        // A: JIT 编译器会将热点代码编译为本地机器码，避免了解释执行的开销。
        //    HotSpot 使用 C1 (Client) 和 C2 (Server) 两个编译器进行分层编译。
    }

    /**
     * 模拟一个 CPU 密集型任务
     * 包含大量的算术运算，容易触发 JIT 优化
     * 
     * 【JVM 原理】
     * 当方法被调用次数超过阈值（默认 10000），JIT 会将其编译为本地代码。
     * 使用 -XX:CompileThreshold=N 可调整阈值。
     */
    private static long heavyComputation() {
        long sum = 0;
        for (int i = 0; i < LOOP_COUNT; i++) {
            // 复杂的数学运算，消耗 CPU
            sum += (i % 3) * (i + 1);
        }
        return sum;
    }
}
