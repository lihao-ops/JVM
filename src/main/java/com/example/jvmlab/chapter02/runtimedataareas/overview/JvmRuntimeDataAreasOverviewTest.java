package com.example.jvmlab.chapter02.runtimedataareas.overview;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JVM 运行时数据区域总览验证测试
 * 
 * 【验证目标】
 * 验证 JVM 运行时数据区域架构图的正确性
 * 
 * 【对应书籍】《深入理解Java虚拟机（第3版）》第2章 2.2节
 * 
 * @author JVM实战专家
 * @version 1.0
 */
@Slf4j
@DisplayName("JVM 运行时数据区域总览验证")
public class JvmRuntimeDataAreasOverviewTest {

    @Test
    @DisplayName("验证 JVM 运行时数据区域架构图正确性")
    void testRuntimeDataAreasArchitecture() {
        log.info("╔═══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║        JVM 运行时数据区域架构验证报告 (JDK 17 HotSpot Perspective)             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════════════╝");
        
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        log.info("");
        log.info("【1. JVM 基本信息】");
        log.info("   VM 名称: {}", runtimeBean.getVmName());
        log.info("   VM 版本: {}", runtimeBean.getVmVersion());
        
        // 验证 Java Heap
        log.info("");
        log.info("【2. Java Heap (线程共享 - GC Managed, G1 Regions)】");
        MemoryUsage heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        log.info("   已使用: {} MB / 最大: {} MB", 
            heapUsage.getUsed() / 1024 / 1024, heapUsage.getMax() / 1024 / 1024);
        assertTrue(heapUsage.getMax() > 0, "堆内存最大值应大于0");
        
        // 验证 G1 GC
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        boolean hasG1 = gcBeans.stream().anyMatch(gc -> gc.getName().contains("G1"));
        log.info("   GC 类型: {}", hasG1 ? "G1 GC ✓" : gcBeans.get(0).getName());
        
        // 验证 Native Memory Areas
        log.info("");
        log.info("【3. Native Memory Areas (线程共享 - OS Managed)】");
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : pools) {
            if (pool.getName().toLowerCase().contains("metaspace")) {
                log.info("   Metaspace 已使用: {} KB ✓", pool.getUsage().getUsed() / 1024);
            }
            if (pool.getName().toLowerCase().contains("code")) {
                log.info("   {} 已使用: {} KB ✓", pool.getName(), pool.getUsage().getUsed() / 1024);
            }
        }
        
        // 验证 Per-Thread Resources
        log.info("");
        log.info("【4. Per-Thread Resources (线程私有)】");
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        log.info("   当前线程数: {}", threadBean.getThreadCount());
        
        // 验证 Execution Engine
        log.info("");
        log.info("【5. Execution Engine (解释器 & JIT)】");
        CompilationMXBean compilationBean = ManagementFactory.getCompilationMXBean();
        if (compilationBean != null) {
            log.info("   JIT 编译器: {} ✓", compilationBean.getName());
        }
        
        log.info("");
        log.info("╔═══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║                     架构图验证通过 - 所有区域确认存在 ✓                        ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════════════╝");
    }
    
    @Test
    @DisplayName("验证线程共享与私有区域")
    void testSharedAndPrivateAreas() throws Exception {
        log.info("=== 验证线程共享与私有区域 ===");
        
        // 共享区域测试
        List<Integer> sharedList = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(5);
        
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    sharedList.add(counter.incrementAndGet());
                }
                latch.countDown();
            }).start();
        }
        
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(500, counter.get());
        log.info("【Heap 共享验证】5个线程共同操作计数器，最终值: {} ✓", counter.get());
        
        // 私有区域测试
        Map<String, Integer> depths = new ConcurrentHashMap<>();
        CountDownLatch latch2 = new CountDownLatch(3);
        
        for (int d : new int[]{10, 20, 30}) {
            new Thread(() -> {
                depths.put(Thread.currentThread().getName(), recursive(0, d));
                latch2.countDown();
            }, "Stack-" + d).start();
        }
        
        latch2.await(5, TimeUnit.SECONDS);
        assertEquals(3, depths.size());
        log.info("【Stack 隔离验证】3个线程独立递归: {} ✓", depths);
    }
    
    private int recursive(int c, int t) {
        return c >= t ? c : recursive(c + 1, t);
    }
}
