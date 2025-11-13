package com.example.jvmlab.chapter11;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
