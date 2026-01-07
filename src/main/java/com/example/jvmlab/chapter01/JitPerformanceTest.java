package com.example.jvmlab.chapter01;

/**
 * 第一章实战：JIT 编译器 vs 解释器 性能对比
 * <p>
 * 实验目标：
 * 1. 体验纯解释模式 (-Xint) 的慢。
 * 2. 体验混合模式/JIT 的快。
 * 3. 观察 -XX:+PrintCompilation 的输出。
 */
public class JitPerformanceTest {

    // 循环次数：从 100万 提升到 1亿！
    // 解释模式下，这可能会跑好几秒甚至十几秒。
    // JIT模式下，依然会非常快。
    private static final int LOOP_COUNT = 100_000_000;

    public static void main(String[] args) {
        System.out.println("开始测试...");
        System.out.println("当前 JVM 运行模式: " + System.getProperty("java.vm.info"));

        long startTime = System.nanoTime();

        // 执行热点代码
        long result = heavyComputation();

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;

        System.out.println("--------------------------------------------------");
        System.out.println("计算结果: " + result);
        System.out.println("耗时: " + durationMs + " ms");
        System.out.println("--------------------------------------------------");
    }

    /**
     * 模拟一个 CPU 密集型任务
     * 包含大量的算术运算，容易触发 JIT 优化
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
