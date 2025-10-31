package com.example.jvmlab.chapter05;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 第5章：调优案例分析与实战。
 * <p>
 * 实现思路：
 * 1. 构造CPU与内存热点场景，模拟真实线上问题，帮助学习排查流程。
 * 2. 通过REST接口动态调整参数，观察不同调优策略对吞吐与延迟的影响。
 * 3. 所有日志使用中英文描述，方便团队分享调优经验。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/chapter05")
public class Chapter05Controller {

    /**
     * 模拟CPU热点循环，用于演练jstack + jfr定位CPU飙升问题。
     *
     * @param iterations 执行次数。
     * @return 耗时信息。
     */
    @GetMapping("/cpu-hotspot")
    public String cpuHotspot(@RequestParam(defaultValue = "1000000") int iterations) {
        log.warn("开始CPU热点模拟 Simulating CPU hotspot, iterations={}", iterations);
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < iterations; i++) {
            sum += ThreadLocalRandom.current().nextInt(100);
        }
        long duration = System.nanoTime() - start;
        log.info("CPU热点模拟结束 CPU hotspot finished, sum={}, duration={}ns", sum, duration);
        return "CPU hotspot duration ns: " + duration;
    }

    /**
     * 模拟内存抖动案例，演示对象池和缓存调优。
     *
     * @param size 生成对象数量。
     * @return 占用内存提示。
     */
    @GetMapping("/memory-churn")
    public String memoryChurn(@RequestParam(defaultValue = "10000") int size) {
        log.warn("开始内存抖动模拟 Simulating memory churn, size={}", size);
        List<byte[]> buffers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            buffers.add(new byte[1024]);
            if (i % 2000 == 0) {
                log.info("已创建缓冲区 Buffers created: {}", i);
            }
        }
        log.info("内存抖动模拟完成 Memory churn complete");
        return "Allocated buffers: " + buffers.size();
    }
}
