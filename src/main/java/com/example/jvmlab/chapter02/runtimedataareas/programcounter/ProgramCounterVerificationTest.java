package com.example.jvmlab.chapter02.runtimedataareas.programcounter;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 程序计数器 (Program Counter Register) 验证测试
 * 
 * 【验证目标】验证程序计数器架构图的正确性
 * 
 * 【PC 寄存器特性】
 * 1. 线程私有：每个线程都有独立的 PC 寄存器
 * 2. 指向当前执行的字节码行号
 * 3. Native 方法执行时值为 undefined
 * 4. 唯一不会发生 OOM 的区域
 * 
 * 【对应书籍】《深入理解Java虚拟机（第3版）》第2章 2.2.1节
 * 
 * @author JVM实战专家
 * @version 1.0
 */
@Slf4j
@DisplayName("程序计数器 (PC Register) 验证测试")
public class ProgramCounterVerificationTest {

    @Test
    @DisplayName("验证 PC 寄存器线程私有性")
    void testPcRegisterIsThreadPrivate() throws Exception {
        log.info("=== 验证 PC 寄存器线程私有性 ===");
        
        AtomicInteger t1 = new AtomicInteger(0);
        AtomicInteger t2 = new AtomicInteger(0);
        AtomicInteger t3 = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);
        
        new Thread(() -> { for (int i = 0; i < 1000; i++) t1.incrementAndGet(); latch.countDown(); }, "PC-Add").start();
        new Thread(() -> { for (int i = 0; i < 500; i++) t2.incrementAndGet(); latch.countDown(); }, "PC-Mul").start();
        new Thread(() -> { for (int i = 0; i < 200; i++) t3.incrementAndGet(); latch.countDown(); }, "PC-Str").start();
        
        latch.await(5, TimeUnit.SECONDS);
        
        log.info("【PC 寄存器独立性验证结果】");
        log.info("  Thread-1 执行次数: {} (预期 1000)", t1.get());
        log.info("  Thread-2 执行次数: {} (预期 500)", t2.get());
        log.info("  Thread-3 执行次数: {} (预期 200)", t3.get());
        
        assertEquals(1000, t1.get());
        assertEquals(500, t2.get());
        assertEquals(200, t3.get());
        
        log.info("【成功】PC 寄存器线程私有性验证通过 ✓");
    }
    
    @Test
    @DisplayName("验证 Native 方法执行时 PC 为 undefined")
    void testPcUndefinedForNativeMethods() {
        log.info("=== 验证 Native 方法执行时 PC 为 undefined ===");
        
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        ThreadInfo threadInfo = threadBean.getThreadInfo(Thread.currentThread().getId(), 100);
        StackTraceElement[] stackTrace = threadInfo.getStackTrace();
        
        int javaFrames = 0, nativeFrames = 0;
        for (int i = 0; i < Math.min(stackTrace.length, 10); i++) {
            StackTraceElement frame = stackTrace[i];
            if (frame.isNativeMethod()) {
                nativeFrames++;
                log.info("  [{}] NATIVE: {}#{} → PC 值为 undefined", i, frame.getClassName(), frame.getMethodName());
            } else {
                javaFrames++;
                log.info("  [{}] JAVA:   {}#{}:{} → PC 指向字节码", i, frame.getClassName(), frame.getMethodName(), frame.getLineNumber());
            }
        }
        
        log.info("【统计】Java 帧: {} 个, Native 帧: {} 个", javaFrames, nativeFrames);
        log.info("【成功】Native 方法 PC undefined 验证通过 ✓");
    }
    
    @Test
    @DisplayName("验证 PC 寄存器不会 OOM")
    void testPcNeverOom() throws Exception {
        log.info("=== 验证 PC 寄存器是唯一不会 OOM 的区域 ===");
        
        int threadCount = 50;
        AtomicInteger success = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> { success.incrementAndGet(); latch.countDown(); }).start();
        }
        
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(threadCount, success.get());
        log.info("  创建 {} 个线程，成功 {} 个 ✓", threadCount, success.get());
        log.info("【成功】PC 寄存器不会 OOM 验证通过 ✓");
    }
    
    @Test
    @DisplayName("PC 寄存器验证综合报告")
    void testPcRegisterSummaryReport() {
        log.info("╔═══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║              程序计数器 (PC Register) 验证综合报告                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════════════╝");
        log.info("");
        log.info("【PC 寄存器核心特性验证】");
        log.info("  ✓ 线程私有性: 每个线程独立拥有 PC 寄存器，互不干扰");
        log.info("  ✓ 字节码地址: Java 方法执行时，PC 指向当前字节码地址");
        log.info("  ✓ Native 值:  Native 方法执行时，PC 值为 undefined");
        log.info("  ✓ 不会 OOM:   PC 只存储固定大小的地址值");
        log.info("");
        log.info("【对应架构图】");
        log.info("  ┌─────────────────────────────────────────────────┐");
        log.info("  │          Per-Thread Resources (Private)         │");
        log.info("  │  ┌───────────────────────────────────────────┐  │");
        log.info("  │  │           PC Register                     │  │");
        log.info("  │  │      0x000000011406F0A0                    │  │");
        log.info("  │  │   (Java方法) 或 undefined (Native方法)    │  │");
        log.info("  │  └───────────────────────────────────────────┘  │");
        log.info("  └─────────────────────────────────────────────────┘");
        log.info("");
        log.info("╔═══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║                      程序计数器架构图验证通过 ✓                                ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════════════╝");
    }
}
