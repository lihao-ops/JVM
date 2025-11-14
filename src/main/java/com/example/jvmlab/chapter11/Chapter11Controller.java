package com.example.jvmlab.chapter11;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 类说明 / Class Description:
 * 中文：第11章控制器，演示运行期（JIT）优化的预热与观测，提供简单吞吐测试接口。
 * English: Chapter 11 controller demonstrating runtime (JIT) optimization warmup and observation with simple throughput testing.
 *
 * 使用场景 / Use Cases:
 * 中文：配合 JFR/async-profiler 观察热点与优化效果，评估吞吐与延迟。
 * English: Use with JFR/async-profiler to observe hotspots and optimization effects, assessing throughput and latency.
 *
 * 设计目的 / Design Purpose:
 * 中文：以最小计算负载触发 JIT，并输出关键时长指标。
 * English: Trigger JIT with minimal compute load and output key duration metrics.
 */
 @Slf4j
 @RestController
 @RequestMapping("/chapter11")
public class Chapter11Controller {

    /**
     * 方法说明 / Method Description:
     * 中文：执行指定次数的热身循环以触发 JIT，返回总耗时毫秒数。
     * English: Run warmup loops to trigger JIT and return total duration in milliseconds.
     *
     * 章节标注 / Book Correlation:
     * 中文：第11章 运行期优化（JIT 预热与观察）
     * English: Chapter 11 Runtime Optimization (JIT warmup & observation)
     *
     * 参数 / Parameters:
     * @param warmup 中文：热身次数 / English: Number of warmup iterations
     * @param payload 中文：每次循环的计算量 / English: Compute payload per iteration
     * 返回值 / Return: 中文：耗时毫秒字符串 / English: Duration in milliseconds as string
     * 异常 / Exceptions: 无
     */
    @GetMapping("/jit-warmup")
    public String jitWarmup(@RequestParam(defaultValue = "100000") int warmup,
                             @RequestParam(defaultValue = "100") int payload) {
        log.info("开始JIT预热 Warmup iterations={}, payload={}", warmup, payload);
        long start = System.nanoTime();
        long value = 0;
        for (int i = 0; i < warmup; i++) {
            value += compute(payload);
        }
        long durationNs = System.nanoTime() - start;
        long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNs);
        log.info("JIT预热结束 Warmup finished, result={}, duration={}ms", value, durationMs);
        return "durationMs=" + durationMs;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：模拟业务计算以触发 JIT 优化（整数运算、位运算混合）。
     * English: Simulate business computation to trigger JIT optimizations (integer arithmetic and bit ops).
     *
     * 章节标注 / Book Correlation:
     * 中文：第11章 运行期优化（热点探测与编译）
     * English: Chapter 11 Runtime Optimization (hotspot detection & compilation)
     *
     * 参数 / Parameters:
     * @param payload 中文：循环内执行的计算量 / English: Computation payload inside the loop
     * 返回值 / Return: 中文：累积结果 / English: Accumulated result
     * 异常 / Exceptions: 无
     */
    private long compute(int payload) {
        long acc = 0;
        for (int i = 0; i < payload; i++) {
            acc += (i * 31L) ^ (payload - i);
        }
        return acc;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：查询代码缓存（Code Cache）使用与编译器统计信息。
     * English: Query code cache usage and compiler statistics.
     *
     * 章节标注 / Book Correlation:
     * 中文：第11章 运行期优化 → 代码缓存
     * English: Chapter 11 Runtime Optimization → Code Cache
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：代码缓存与编译器信息 Map / English: Map of code cache and compiler info
     * 异常 / Exceptions: 无
     */
    @GetMapping("/code-cache-info")
    public Map<String, Object> codeCacheInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getName().toLowerCase().contains("code cache")) {
                MemoryUsage u = pool.getUsage();
                Map<String, Object> cc = new LinkedHashMap<>();
                cc.put("name", pool.getName());
                cc.put("used", u.getUsed());
                cc.put("max", u.getMax());
                cc.put("committed", u.getCommitted());
                info.put("codeCache", cc);
                break;
            }
        }
        CompilationMXBean comp = ManagementFactory.getCompilationMXBean();
        if (comp != null) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("name", comp.getName());
            c.put("totalCompilationTime", comp.isCompilationTimeMonitoringSupported() ? comp.getTotalCompilationTime() : -1);
            info.put("compiler", c);
        }
        log.info("【成功】代码缓存信息收集完成 / Success: code cache info collected: {}", info);
        return info;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：逃逸分析演示：方法内创建的短生命周期对象，仅在局部使用，结合 JVM 参数观察标量替换效果。
     * English: Escape analysis demo: short-lived objects created and used locally; observe scalar replacement via JVM flags.
     *
     * 章节标注 / Book Correlation:
     * 中文：第11章 运行期优化 → 逃逸分析与标量替换
     * English: Chapter 11 Runtime Optimization → Escape Analysis & Scalar Replacement
     *
     * 参数 / Parameters:
     * @param warmup 中文：预热循环次数 / English: Warmup iterations
     * @param iterations 中文：测量循环次数 / English: Measurement iterations
     * 返回值 / Return: 中文：耗时毫秒与结果摘要 / English: Duration ms and result summary
     * 异常 / Exceptions: 无
     */
    @GetMapping("/escape-analysis")
    public Map<String, Object> escapeAnalysis(@RequestParam(defaultValue = "100000") int warmup,
                                              @RequestParam(defaultValue = "100000") int iterations) {
        for (int i = 0; i < warmup; i++) {
            Point p = new Point(i, i + 1);
            blackhole(p.length());
        }
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < iterations; i++) {
            Point p = new Point(i, i + 2);
            sum += p.length();
        }
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("durationMs", durationMs);
        result.put("sum", sum);
        result.put("hint", "建议对比 -XX:-DoEscapeAnalysis 与 -XX:+DoEscapeAnalysis / Compare flags");
        log.info("【成功】逃逸分析演示完成，durationMs={} sum={} / Success", durationMs, sum);
        return result;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：偏向锁撤销/锁竞争演示：多线程竞争同一锁对象，观察锁膨胀与性能影响（JDK17 及以后偏向锁移除）。
     * English: Biased lock revocation/lock contention demo: multiple threads contend on one lock to observe inflation and performance (biased locking removed since JDK17).
     *
     * 章节标注 / Book Correlation:
     * 中文：第11章 运行期优化 → 同步优化与锁撤销
     * English: Chapter 11 Runtime Optimization → Synchronization Optimization & Lock Revocation
     *
     * 参数 / Parameters:
     * @param threads 中文：线程数 / English: Number of threads
     * @param iterations 中文：每线程进入临界区次数 / English: Critical section iterations per thread
     * 返回值 / Return: 中文：耗时毫秒与进入次数 / English: Duration ms and total enters
     * 异常 / Exceptions: 无
     */
    @GetMapping("/biased-lock-demo")
    public Map<String, Object> biasedLockDemo(@RequestParam(defaultValue = "8") int threads,
                                              @RequestParam(defaultValue = "200000") int iterations) {
        final Object lock = new Object();
        List<Thread> list = new java.util.ArrayList<>();
        long start = System.nanoTime();
        for (int t = 0; t < threads; t++) {
            Thread th = new Thread(() -> {
                long local = 0;
                for (int i = 0; i < iterations; i++) {
                    // 中文：高频进入临界区，触发锁竞争
                    // English: High-frequency critical section to trigger lock contention
                    synchronized (lock) {
                        local += i;
                    }
                }
            }, "biased-demo-" + t);
            th.start();
            list.add(th);
        }
        for (Thread th : list) {
            try {
                th.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("durationMs", durationMs);
        result.put("threads", threads);
        result.put("iterations", iterations);
        result.put("hint", "JDK8 可使用 -XX:+UseBiasedLocking 观察；JDK17 以后关注锁竞争与膨胀");
        log.info("【成功】锁竞争演示完成，durationMs={} threads={} iterations={} / Success", durationMs, threads, iterations);
        return result;
    }

    private static volatile Object SINK;
    private void blackhole(Object o) {
        SINK = o;
    }

    /** 逃逸分析示例类 */
    static class Point {
        final int x;
        final int y;
        Point(int x, int y) { this.x = x; this.y = y; }
        int length() { return (x * x) + (y * y); }
    }
}
