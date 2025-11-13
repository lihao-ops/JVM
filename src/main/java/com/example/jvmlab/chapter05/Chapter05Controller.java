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
 * 类说明 / Class Description:
 * 中文：第5章控制器，提供 CPU 热点与内存抖动的模拟接口，用于调优案例演练。
 * English: Chapter 05 controller with endpoints simulating CPU hotspots and memory churn for optimization practice.
 *
 * 使用场景 / Use Cases:
 * 中文：课堂/面试中动态调整参数，观察吞吐与延迟的变化以总结调优策略。
 * English: Adjust parameters during class/interviews to observe throughput/latency changes and derive optimization strategies.
 *
 * 设计目的 / Design Purpose:
 * 中文：以最小可复现场景帮助理解热点定位与缓存/对象池优化的价值。
 * English: Minimal reproducible scenarios to understand hotspot localization and cache/pool optimization.
 */
@Slf4j
@RestController
@RequestMapping("/chapter05")
public class Chapter05Controller {

    /**
     * 方法说明 / Method Description:
     * 中文：模拟 CPU 热点循环，便于使用 jstack/JFR 定位 CPU 飙升问题。
     * English: Simulate a CPU hotspot loop for jstack/JFR analysis of CPU spikes.
     *
     * 参数 / Parameters:
     * @param iterations 中文：循环次数 / English: Number of iterations
     * 返回值 / Return: 中文：耗时纳秒字符串 / English: Duration in nanoseconds as string
     * 异常 / Exceptions: 无
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
     * 方法说明 / Method Description:
     * 中文：模拟内存抖动，演示对象池与缓存调优的基本思路。
     * English: Simulate memory churn to demonstrate basics of object pooling and cache tuning.
     *
     * 参数 / Parameters:
     * @param size 中文：生成对象数量 / English: Number of objects to generate
     * 返回值 / Return: 中文：分配数量描述 / English: Allocation count description
     * 异常 / Exceptions: 无
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
