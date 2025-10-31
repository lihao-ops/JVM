package com.example.jvmlab.chapter11;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 第11章：晚期（运行期）优化实验控制器。
 * <p>
 * 实现思路：
 * 1. 模拟JIT编译的预热与观察阶段，展示HotSpot如何基于热点统计进行优化。
 * 2. 提供简单的吞吐量测试接口，可搭配JFR或async-profiler分析运行期优化效果。
 * 3. 日志使用中英文，强调各步骤的意义和调优指标。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/chapter11")
public class Chapter11Controller {

    /**
     * 执行指定次数的热身循环，帮助触发JIT编译。
     *
     * @param warmup  热身次数。
     * @param payload 循环中执行的计算量。
     * @return 总耗时毫秒。
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
     * 模拟业务计算，用于触发JIT优化。
     *
     * @param payload 计算量。
     * @return 计算结果。
     */
    private long compute(int payload) {
        long acc = 0;
        for (int i = 0; i < payload; i++) {
            acc += (i * 31L) ^ (payload - i);
        }
        return acc;
    }
}
