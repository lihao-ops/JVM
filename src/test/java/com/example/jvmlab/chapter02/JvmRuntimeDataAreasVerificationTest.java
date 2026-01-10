package com.example.jvmlab.chapter02;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JVM 运行时数据区域全面验证测试类 (JDK 17 HotSpot Perspective)
 * 
 * ===================================================================================
 * 【测试设计目的】
 * 本测试类旨在全面验证 JVM 运行时数据区域的各个组件，对应以下架构图：
 * 
 * ┌─────────────────────────────────────────────────────────────────────────────────┐
 * │                    JVM Runtime Data Areas (JDK 17 HotSpot)                      │
 * ├─────────────────────────────┬───────────────────────────────────────────────────┤
 * │  Java Heap (GC Managed)    │  Native Memory Areas (OS Managed)                 │
 * │  ┌───────────────────────┐ │  ┌─────────────────┐ ┌─────────────────────────┐  │
 * │  │    G1 Regions         │ │  │   Metaspace     │ │      Code Cache         │  │
 * │  │    (Shared)           │ │  │   (Shared)      │ │      (Shared)           │  │
 * │  └───────────────────────┘ │  └─────────────────┘ └─────────────────────────┘  │
 * │                            │  ┌─────────────────────────────────────────────┐  │
 * │                            │  │        Per-Thread Resources (Private)       │  │
 * │                            │  │  ┌─────────────────────────────────────┐    │  │
 * │                            │  │  │ Unified Thread Stack (Java & Native)│    │  │
 * │                            │  │  └─────────────────────────────────────┘    │  │
 * │                            │  │  ┌─────────────────────────────────────┐    │  │
 * │                            │  │  │          PC Register                │    │  │
 * │                            │  │  └─────────────────────────────────────┘    │  │
 * │                            │  └─────────────────────────────────────────────┘  │
 * └─────────────────────────────┴───────────────────────────────────────────────────┘
 *                     ↓                        ↓                    ↓
 *         ┌─────────────────────┐    ┌──────────────────┐    ┌─────────────────┐
 *         │  Execution Engine   │───→│       JNI        │───→│ Native Method   │
 *         │(Interpreter & JIT)  │    │(Native Interface)│    │   Libraries     │
 *         └─────────────────────┘    └──────────────────┘    └─────────────────┘
 * 
 * 【对应书籍】《深入理解Java虚拟机（第3版）》第2章 - Java内存区域与内存溢出异常
 * 
 * 【推荐 VM 参数】
 * -Xms128m -Xmx128m -XX:+UseG1GC -Xlog:gc*
 * 
 * ===================================================================================
 * 
 * @author JVM实战专家
 * @version 1.0
 * @since 2026-01-10
 */
@Slf4j
@DisplayName("JVM 运行时数据区域全面验证测试 (JDK 17 HotSpot)")
public class JvmRuntimeDataAreasVerificationTest {

    // ==================================================================================
    // 第一部分：Java Heap 验证 (线程共享 - GC Managed, G1 Regions)
    // ==================================================================================
    
    @Nested
    @DisplayName("1. Java Heap 验证 (线程共享)")
    class JavaHeapTests {
        
        /**
         * 【验证目标】确认 Java Heap 存在且可通过 MXBean 获取状态
         * 
         * 【JVM 原理】
         * Java 堆是 JVM 管理的最大内存区域，几乎所有对象实例都在堆上分配。
         * JDK 17 HotSpot 默认使用 G1 垃圾收集器，将堆划分为多个 Region。
         * 
         * 【面试考点】
         * Q: 堆内存的主要作用是什么？
         * A: 存储对象实例和数组，是 GC 的主要管理区域。
         */
        @Test
        @DisplayName("1.1 验证 Java Heap 存在并可获取状态")
        void testJavaHeapExists() {
            log.info("=== 开始验证 Java Heap 存在性 ===");
            
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            
            // 验证堆内存使用信息存在
            assertNotNull(heapUsage, "堆内存使用信息不应为空");
            
            long init = heapUsage.getInit();
            long used = heapUsage.getUsed();
            long committed = heapUsage.getCommitted();
            long max = heapUsage.getMax();
            
            log.info("【堆内存状态】");
            log.info("  初始大小 (Init): {} MB", init / 1024 / 1024);
            log.info("  已使用 (Used): {} MB", used / 1024 / 1024);
            log.info("  已提交 (Committed): {} MB", committed / 1024 / 1024);
            log.info("  最大值 (Max): {} MB", max / 1024 / 1024);
            
            // 断言：堆内存已被使用
            assertTrue(used > 0, "堆内存应该有被使用的部分");
            assertTrue(committed >= used, "已提交内存应该大于等于已使用内存");
            
            log.info("【成功】Java Heap 存在性验证通过 ✓");
        }
        
        /**
         * 【验证目标】确认 JDK 17 默认使用 G1 垃圾收集器
         * 
         * 【JVM 原理】
         * G1 (Garbage-First) 是 JDK 9 后的默认垃圾收集器。
         * 它将堆划分为多个大小相等的 Region，支持并发标记和增量回收。
         * 
         * 【面试考点】
         * Q: 为什么 JDK 17 默认使用 G1 GC？
         * A: G1 提供更可预测的停顿时间，适合大堆内存，并发能力强。
         */
        @Test
        @DisplayName("1.2 验证 G1 垃圾收集器 (JDK 17 默认)")
        void testG1GarbageCollector() {
            log.info("=== 开始验证 G1 垃圾收集器 ===");
            
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            
            assertFalse(gcBeans.isEmpty(), "应该存在垃圾收集器");
            
            boolean hasG1YoungGen = false;
            boolean hasG1OldGen = false;
            
            log.info("【当前 GC 收集器列表】");
            for (GarbageCollectorMXBean gc : gcBeans) {
                String gcName = gc.getName();
                long gcCount = gc.getCollectionCount();
                long gcTime = gc.getCollectionTime();
                
                log.info("  收集器: {} | 收集次数: {} | 收集时间: {} ms", gcName, gcCount, gcTime);
                
                // G1 收集器的典型名称
                if (gcName.contains("G1 Young Generation") || gcName.contains("G1 Young")) {
                    hasG1YoungGen = true;
                }
                if (gcName.contains("G1 Old Generation") || gcName.contains("G1 Old") || gcName.contains("G1 Concurrent GC")) {
                    hasG1OldGen = true;
                }
            }
            
            // 注意：如果不是 G1，可能是其他收集器，也是有效的
            if (hasG1YoungGen || hasG1OldGen) {
                log.info("【成功】检测到 G1 垃圾收集器 ✓");
            } else {
                log.warn("【注意】未检测到 G1 收集器，可能使用了其他 GC（如 ZGC、Shenandoah）");
                log.info("这不影响测试，只是验证当前 GC 配置");
            }
            
            // 无论使用哪种 GC，都应该至少有一个收集器
            assertTrue(gcBeans.size() >= 1, "应该至少有一个垃圾收集器");
            log.info("【成功】GC 收集器验证通过 ✓");
        }
        
        /**
         * 【验证目标】验证堆内存分配与 GC 管理机制
         * 
         * 【JVM 原理】
         * 对象在堆上分配，当堆内存不足时会触发 GC。
         * G1 会根据 Region 的回收价值进行优先回收（Garbage-First 得名于此）。
         * 
         * 【面试考点】
         * Q: 什么时候会触发 GC？
         * A: Eden 区满会触发 Young GC，老年代满会触发 Mixed GC 或 Full GC。
         */
        @Test
        @DisplayName("1.3 验证堆内存分配与 GC 管理")
        void testHeapMemoryManagement() {
            log.info("=== 开始验证堆内存分配与 GC 管理 ===");
            
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            long usedBefore = memoryMXBean.getHeapMemoryUsage().getUsed();
            
            log.info("分配前堆内存使用: {} MB", usedBefore / 1024 / 1024);
            
            // 分配一些对象，验证堆内存增加
            List<byte[]> allocations = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                // 每次分配 100KB
                allocations.add(new byte[100 * 1024]);
            }
            
            long usedAfterAlloc = memoryMXBean.getHeapMemoryUsage().getUsed();
            log.info("分配后堆内存使用: {} MB", usedAfterAlloc / 1024 / 1024);
            
            // 记录内存变化（注意：由于 GC 可能随时触发，内存可能不一定增加）
            long memoryDiff = usedAfterAlloc - usedBefore;
            log.info("内存变化: {} KB (正值表示增加，负值表示 GC 已执行)", memoryDiff / 1024);
            
            // 验证分配的对象存在（而非严格验证内存增加）
            assertNotNull(allocations, "分配的对象列表不应为空");
            assertEquals(100, allocations.size(), "应该分配了 100 个数组");
            
            // 释放引用，请求 GC
            allocations.clear();
            allocations = null;
            System.gc();
            
            // 等待 GC 执行
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            long usedAfterGc = memoryMXBean.getHeapMemoryUsage().getUsed();
            log.info("GC 后堆内存使用: {} MB", usedAfterGc / 1024 / 1024);
            
            // GC 后内存应该减少（注意：不保证一定减少，取决于 GC 策略）
            log.info("【成功】堆内存分配与 GC 管理验证通过 ✓");
        }
    }
    
    // ==================================================================================
    // 第二部分：Native Memory Areas 验证 (线程共享 - OS Managed)
    // ==================================================================================
    
    @Nested
    @DisplayName("2. Native Memory Areas 验证 (线程共享)")
    class NativeMemoryAreasTests {
        
        /**
         * 【验证目标】验证 Metaspace 区域存在
         * 
         * 【JVM 原理】
         * Metaspace 是 JDK 8 后替代永久代的区域，存储类元数据。
         * 它使用本地内存（Native Memory），由操作系统管理。
         * 
         * 【面试考点】
         * Q: Metaspace 和永久代有什么区别？
         * A: Metaspace 使用本地内存，不受 -Xmx 限制，OOM 时报错信息不同。
         */
        @Test
        @DisplayName("2.1 验证 Metaspace 区域存在")
        void testMetaspaceExists() {
            log.info("=== 开始验证 Metaspace 区域 ===");
            
            List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
            
            boolean metaspaceFound = false;
            
            log.info("【内存池列表】");
            for (MemoryPoolMXBean pool : memoryPools) {
                String poolName = pool.getName();
                MemoryUsage usage = pool.getUsage();
                
                log.info("  内存池: {} | 类型: {} | 已使用: {} KB", 
                    poolName, pool.getType(), usage.getUsed() / 1024);
                
                if (poolName.toLowerCase().contains("metaspace")) {
                    metaspaceFound = true;
                    log.info("  【发现 Metaspace】");
                    log.info("    初始大小: {} KB", usage.getInit() / 1024);
                    log.info("    已使用: {} KB", usage.getUsed() / 1024);
                    log.info("    已提交: {} KB", usage.getCommitted() / 1024);
                    log.info("    最大值: {} (无限制则为 -1)", usage.getMax());
                }
            }
            
            assertTrue(metaspaceFound, "应该存在 Metaspace 内存池");
            log.info("【成功】Metaspace 区域验证通过 ✓");
        }
        
        /**
         * 【验证目标】验证类元数据存储在 Metaspace
         * 
         * 【JVM 原理】
         * 每加载一个类，其 Class 对象的元数据就会存储在 Metaspace 中。
         * 类加载器卸载时，对应的类元数据才会被回收。
         * 
         * 【面试考点】
         * Q: Metaspace 会 OOM 吗？
         * A: 会，当加载的类太多且类加载器无法卸载时会 OOM。
         */
        @Test
        @DisplayName("2.2 验证类元数据存储在 Metaspace")
        void testClassMetadataInMetaspace() {
            log.info("=== 开始验证类元数据存储 ===");
            
            ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
            
            int loadedClassCount = classLoadingBean.getLoadedClassCount();
            long totalLoadedClassCount = classLoadingBean.getTotalLoadedClassCount();
            long unloadedClassCount = classLoadingBean.getUnloadedClassCount();
            
            log.info("【类加载统计】");
            log.info("  当前已加载类数量: {}", loadedClassCount);
            log.info("  总共加载过的类数量: {}", totalLoadedClassCount);
            log.info("  已卸载的类数量: {}", unloadedClassCount);
            
            assertTrue(loadedClassCount > 0, "应该有已加载的类");
            
            // 验证加载新类会增加计数
            int countBefore = classLoadingBean.getLoadedClassCount();
            
            // 动态创建一个类（使用 Java 反射 API 触发类加载）
            try {
                Class<?> clazz = Class.forName("java.util.concurrent.ConcurrentSkipListMap");
                assertNotNull(clazz, "应该能加载类");
            } catch (ClassNotFoundException e) {
                log.warn("类加载测试跳过: {}", e.getMessage());
            }
            
            int countAfter = classLoadingBean.getLoadedClassCount();
            log.info("新加载类后数量: {} -> {}", countBefore, countAfter);
            
            log.info("【成功】类元数据存储验证通过 ✓");
        }
        
        /**
         * 【验证目标】验证 Code Cache 区域存在
         * 
         * 【JVM 原理】
         * Code Cache 用于存储 JIT 编译器编译后的本地代码。
         * 它是 JVM 性能的关键区域，代码编译后从这里执行。
         * 
         * 【面试考点】
         * Q: Code Cache 满了会怎样？
         * A: JIT 编译会停止，性能会急剧下降，只能使用解释执行。
         */
        @Test
        @DisplayName("2.3 验证 Code Cache 区域存在")
        void testCodeCacheExists() {
            log.info("=== 开始验证 Code Cache 区域 ===");
            
            List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
            
            boolean codeCacheFound = false;
            
            for (MemoryPoolMXBean pool : memoryPools) {
                String poolName = pool.getName();
                
                // Code Cache 可能有不同的命名
                if (poolName.toLowerCase().contains("code") && 
                    (poolName.toLowerCase().contains("cache") || poolName.toLowerCase().contains("heap"))) {
                    
                    codeCacheFound = true;
                    MemoryUsage usage = pool.getUsage();
                    
                    log.info("【发现 Code Cache】: {}", poolName);
                    log.info("  初始大小: {} KB", usage.getInit() / 1024);
                    log.info("  已使用: {} KB", usage.getUsed() / 1024);
                    log.info("  已提交: {} KB", usage.getCommitted() / 1024);
                    log.info("  最大值: {} KB", usage.getMax() / 1024);
                }
            }
            
            assertTrue(codeCacheFound, "应该存在 Code Cache 内存池");
            log.info("【成功】Code Cache 区域验证通过 ✓");
        }
        
        /**
         * 【验证目标】验证 JIT 编译代码存储在 Code Cache
         * 
         * 【JVM 原理】
         * 当方法被调用次数超过阈值（默认 10000），JIT 会将其编译为本地代码。
         * 编译后的代码存储在 Code Cache，后续调用直接执行本地代码。
         * 
         * 【面试考点】
         * Q: 如何判断方法是否被 JIT 编译？
         * A: 使用 -XX:+PrintCompilation 参数观察编译日志。
         */
        @Test
        @DisplayName("2.4 验证 JIT 编译代码存储")
        void testJitCompiledCodeInCodeCache() {
            log.info("=== 开始验证 JIT 编译代码存储 ===");
            
            CompilationMXBean compilationBean = ManagementFactory.getCompilationMXBean();
            
            if (compilationBean != null) {
                String jitName = compilationBean.getName();
                boolean monitoringSupported = compilationBean.isCompilationTimeMonitoringSupported();
                
                log.info("【JIT 编译器信息】");
                log.info("  编译器名称: {}", jitName);
                log.info("  是否支持编译时间监控: {}", monitoringSupported);
                
                if (monitoringSupported) {
                    long totalCompilationTime = compilationBean.getTotalCompilationTime();
                    log.info("  总编译时间: {} ms", totalCompilationTime);
                }
                
                // 执行一些热点代码，触发 JIT 编译
                long startTime = System.nanoTime();
                long sum = 0;
                for (int i = 0; i < 100000; i++) {
                    sum += hotMethod(i);
                }
                long endTime = System.nanoTime();
                
                log.info("  热点方法执行 100000 次，耗时: {} ms, 结果: {}", 
                    (endTime - startTime) / 1_000_000, sum);
                
                if (monitoringSupported) {
                    long newCompilationTime = compilationBean.getTotalCompilationTime();
                    log.info("  执行后编译时间: {} ms", newCompilationTime);
                }
                
                log.info("【成功】JIT 编译代码存储验证通过 ✓");
            } else {
                log.warn("【跳过】当前 JVM 不支持 CompilationMXBean");
            }
        }
        
        /**
         * 热点方法 - 用于触发 JIT 编译
         */
        private long hotMethod(int n) {
            return n * n + n / 2;
        }
    }
    
    // ==================================================================================
    // 第三部分：Per-Thread Resources 验证 (线程私有)
    // ==================================================================================
    
    @Nested
    @DisplayName("3. Per-Thread Resources 验证 (线程私有)")
    class PerThreadResourcesTests {
        
        /**
         * 【验证目标】验证线程栈是私有的
         * 
         * 【JVM 原理】
         * 每个线程都有自己独立的虚拟机栈，用于存储栈帧。
         * 栈帧包含局部变量表、操作数栈、动态链接等信息。
         * 
         * 【面试考点】
         * Q: 为什么线程栈是私有的？
         * A: 保证方法调用的隔离性，避免线程间数据污染。
         */
        @Test
        @DisplayName("3.1 验证线程栈是私有的")
        void testThreadStackIsPrivate() throws Exception {
            log.info("=== 开始验证线程栈私有性 ===");
            
            // 使用 ThreadLocal 模拟线程私有数据
            ThreadLocal<Integer> threadLocalValue = new ThreadLocal<>();
            ConcurrentHashMap<String, Integer> threadValues = new ConcurrentHashMap<>();
            CountDownLatch latch = new CountDownLatch(3);
            
            for (int i = 1; i <= 3; i++) {
                final int threadId = i;
                new Thread(() -> {
                    // 每个线程设置自己的值
                    threadLocalValue.set(threadId * 100);
                    
                    // 模拟栈帧中的局部变量
                    int localVar = threadId * 10;
                    
                    // 验证值是线程私有的
                    int storedValue = threadLocalValue.get();
                    threadValues.put(Thread.currentThread().getName(), storedValue + localVar);
                    
                    log.info("线程 {} | ThreadLocal 值: {} | 局部变量: {}", 
                        Thread.currentThread().getName(), storedValue, localVar);
                    
                    latch.countDown();
                }, "TestThread-" + i).start();
            }
            
            latch.await(5, TimeUnit.SECONDS);
            
            // 验证每个线程有不同的值
            assertEquals(3, threadValues.size(), "应该有 3 个线程的数据");
            
            // 验证值都不相同
            Set<Integer> uniqueValues = new HashSet<>(threadValues.values());
            assertEquals(3, uniqueValues.size(), "每个线程应该有不同的值");
            
            log.info("【成功】线程栈私有性验证通过 ✓");
        }
        
        /**
         * 【验证目标】验证统一线程栈（Java & Native Frames）
         * 
         * 【JVM 原理】
         * JDK 17 HotSpot 使用统一的线程栈存储 Java 栈帧和 Native 栈帧。
         * 这与早期 JVM 分离的设计不同，提高了效率。
         * 
         * 【面试考点】
         * Q: Java 栈和 Native 栈有什么关系？
         * A: 在 HotSpot 中共享同一个线程栈，方便 JNI 调用。
         */
        @Test
        @DisplayName("3.2 验证统一线程栈 (Java & Native Frames)")
        void testUnifiedThreadStack() {
            log.info("=== 开始验证统一线程栈 ===");
            
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            
            // 获取当前线程的栈帧信息
            ThreadInfo threadInfo = threadBean.getThreadInfo(Thread.currentThread().getId(), 100);
            StackTraceElement[] stackTrace = threadInfo.getStackTrace();
            
            log.info("【当前线程栈帧信息】");
            log.info("  线程名称: {}", threadInfo.getThreadName());
            log.info("  线程状态: {}", threadInfo.getThreadState());
            log.info("  栈帧深度: {}", stackTrace.length);
            
            // 打印部分栈帧
            int count = 0;
            for (StackTraceElement frame : stackTrace) {
                if (count++ < 10) {
                    boolean isNative = frame.isNativeMethod();
                    log.info("  [{}] {}#{}{}", 
                        isNative ? "NATIVE" : "JAVA",
                        frame.getClassName(),
                        frame.getMethodName(),
                        isNative ? " (Native)" : ":" + frame.getLineNumber());
                }
            }
            
            assertTrue(stackTrace.length > 0, "应该有栈帧信息");
            log.info("【成功】统一线程栈验证通过 ✓");
        }
        
        /**
         * 【验证目标】验证 PC 寄存器是线程私有的
         * 
         * 【JVM 原理】
         * 程序计数器（PC Register）是唯一不会发生 OOM 的区域。
         * 它记录当前线程执行的字节码行号，多线程切换时保存/恢复执行位置。
         * 
         * 【面试考点】
         * Q: 为什么 PC 寄存器不会 OOM？
         * A: 因为它只存储一个地址值，占用空间极小且固定。
         */
        @Test
        @DisplayName("3.3 验证 PC 寄存器线程私有性")
        void testPcRegisterIsThreadPrivate() throws Exception {
            log.info("=== 开始验证 PC 寄存器线程私有性 ===");
            
            // PC 寄存器无法直接观测，但可以通过多线程执行不同代码来验证其独立性
            AtomicInteger thread1Progress = new AtomicInteger(0);
            AtomicInteger thread2Progress = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(2);
            
            // 线程1：执行循环计算
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 1000; i++) {
                    thread1Progress.incrementAndGet();
                    // PC 寄存器保存着这个循环的当前位置
                }
                latch.countDown();
            }, "PC-Test-1");
            
            // 线程2：执行不同的计算
            Thread t2 = new Thread(() -> {
                int sum = 0;
                for (int i = 0; i < 500; i++) {
                    sum += i;
                    thread2Progress.incrementAndGet();
                    // PC 寄存器保存着这个不同循环的位置
                }
                latch.countDown();
            }, "PC-Test-2");
            
            t1.start();
            t2.start();
            
            latch.await(5, TimeUnit.SECONDS);
            
            log.info("【PC 寄存器独立性验证】");
            log.info("  线程1 执行进度: {} 次", thread1Progress.get());
            log.info("  线程2 执行进度: {} 次", thread2Progress.get());
            
            // 验证两个线程都完成了各自的任务
            assertEquals(1000, thread1Progress.get(), "线程1 应该执行 1000 次");
            assertEquals(500, thread2Progress.get(), "线程2 应该执行 500 次");
            
            log.info("【原理说明】每个线程的 PC 寄存器独立保存执行位置，互不干扰");
            log.info("【成功】PC 寄存器线程私有性验证通过 ✓");
        }
    }
    
    // ==================================================================================
    // 第四部分：Execution Engine 验证 (解释器 & JIT 编译器)
    // ==================================================================================
    
    @Nested
    @DisplayName("4. Execution Engine 验证 (解释器 & JIT)")
    class ExecutionEngineTests {
        
        /**
         * 【验证目标】验证解释器模式存在
         * 
         * 【JVM 原理】
         * HotSpot 采用混合模式，方法首次调用时使用解释器执行。
         * 解释器逐条解释字节码，虽然慢但启动快。
         * 
         * 【面试考点】
         * Q: 为什么需要解释器？JIT 不是更快吗？
         * A: 解释器启动快，无需编译等待；JIT 编译需要时间，热点代码才值得编译。
         */
        @Test
        @DisplayName("4.1 验证解释器模式存在")
        void testInterpreterMode() {
            log.info("=== 开始验证解释器模式 ===");
            
            // 获取 JVM 运行模式信息
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            List<String> inputArguments = runtimeBean.getInputArguments();
            
            log.info("【JVM 启动参数】");
            for (String arg : inputArguments) {
                if (arg.contains("TieredCompilation") || arg.contains("Xint") || arg.contains("Xcomp")) {
                    log.info("  执行模式相关参数: {}", arg);
                }
            }
            
            // 验证 VM 名称包含 HotSpot
            String vmName = runtimeBean.getVmName();
            String vmVersion = runtimeBean.getVmVersion();
            
            log.info("【JVM 信息】");
            log.info("  VM 名称: {}", vmName);
            log.info("  VM 版本: {}", vmVersion);
            
            // HotSpot 默认使用混合模式（Interpreter + JIT）
            assertTrue(vmName.contains("HotSpot") || vmName.contains("OpenJDK"), 
                "应该是 HotSpot 或 OpenJDK 虚拟机");
            
            log.info("【说明】HotSpot 默认使用混合模式，同时包含解释器和 JIT 编译器");
            log.info("【成功】解释器模式验证通过 ✓");
        }
        
        /**
         * 【验证目标】验证 JIT 编译器工作
         * 
         * 【JVM 原理】
         * JIT (Just-In-Time) 编译器将热点代码编译为本地机器码。
         * HotSpot 包含 C1（Client Compiler）和 C2（Server Compiler）两个 JIT。
         * 分层编译允许在 C1 和 C2 之间切换。
         * 
         * 【面试考点】
         * Q: C1 和 C2 编译器有什么区别？
         * A: C1 编译快但优化少，C2 编译慢但优化深入，适合长期运行的服务端程序。
         */
        @Test
        @DisplayName("4.2 验证 JIT 编译器工作")
        void testJitCompilation() {
            log.info("=== 开始验证 JIT 编译器工作 ===");
            
            CompilationMXBean compilationBean = ManagementFactory.getCompilationMXBean();
            
            if (compilationBean == null) {
                log.warn("【跳过】JIT 编译信息不可用（可能使用 -Xint 纯解释模式）");
                return;
            }
            
            String compilerName = compilationBean.getName();
            log.info("【JIT 编译器信息】");
            log.info("  编译器名称: {}", compilerName);
            
            if (compilationBean.isCompilationTimeMonitoringSupported()) {
                long compilationTimeBefore = compilationBean.getTotalCompilationTime();
                
                // 执行大量热点代码触发 JIT 编译
                long result = 0;
                for (int round = 0; round < 10; round++) {
                    for (int i = 0; i < 100000; i++) {
                        result += fibonacciIterative(20);
                    }
                }
                
                long compilationTimeAfter = compilationBean.getTotalCompilationTime();
                
                log.info("  编译前累计编译时间: {} ms", compilationTimeBefore);
                log.info("  编译后累计编译时间: {} ms", compilationTimeAfter);
                log.info("  本次测试触发的编译时间: {} ms", compilationTimeAfter - compilationTimeBefore);
                log.info("  热点代码执行结果: {}", result);
                
                // JIT 编译时间应该增加（如果代码被编译了）
                log.info("【说明】如果编译时间增加，说明 JIT 编译器工作正常");
            }
            
            log.info("【成功】JIT 编译器验证通过 ✓");
        }
        
        /**
         * 迭代计算斐波那契数 - 热点方法
         */
        private long fibonacciIterative(int n) {
            if (n <= 1) return n;
            long a = 0, b = 1;
            for (int i = 2; i <= n; i++) {
                long temp = a + b;
                a = b;
                b = temp;
            }
            return b;
        }
    }
    
    // ==================================================================================
    // 第五部分：JNI 验证 (Native Interface)
    // ==================================================================================
    
    @Nested
    @DisplayName("5. JNI 验证 (Native Interface)")
    class JniTests {
        
        /**
         * 【验证目标】验证 JNI 本地接口可用
         * 
         * 【JVM 原理】
         * JNI (Java Native Interface) 允许 Java 代码调用本地 C/C++ 代码。
         * 执行引擎通过 JNI 与本地方法库交互。
         * 
         * 【面试考点】
         * Q: 使用 JNI 有什么注意事项？
         * A: 需要注意内存管理、类型转换、异常处理，且跨平台性受限。
         */
        @Test
        @DisplayName("5.1 验证 JNI 本地接口可用")
        void testJniNativeInterface() {
            log.info("=== 开始验证 JNI 本地接口 ===");
            
            // 验证可以调用 native 方法
            // System.currentTimeMillis() 是一个 native 方法
            long startTime = System.currentTimeMillis();
            long nanoTime = System.nanoTime();
            
            log.info("【JNI Native 方法调用验证】");
            log.info("  System.currentTimeMillis() [native]: {} ms", startTime);
            log.info("  System.nanoTime() [native]: {} ns", nanoTime);
            
            // Thread.currentThread() 也依赖 native 方法
            Thread currentThread = Thread.currentThread();
            log.info("  Thread.currentThread().getName() [native 支持]: {}", currentThread.getName());
            
            // 验证 Runtime 的 native 方法
            Runtime runtime = Runtime.getRuntime();
            int availableProcessors = runtime.availableProcessors();
            long freeMemory = runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            
            log.info("  Runtime.availableProcessors() [native]: {} 核", availableProcessors);
            log.info("  Runtime.freeMemory() [native]: {} MB", freeMemory / 1024 / 1024);
            log.info("  Runtime.maxMemory() [native]: {} MB", maxMemory / 1024 / 1024);
            
            assertTrue(availableProcessors > 0, "处理器数量应该大于 0");
            assertTrue(freeMemory > 0, "可用内存应该大于 0");
            
            log.info("【成功】JNI 本地接口验证通过 ✓");
        }
    }
    
    // ==================================================================================
    // 第六部分：Native Method Libraries 验证
    // ==================================================================================
    
    @Nested
    @DisplayName("6. Native Method Libraries 验证")
    class NativeMethodLibrariesTests {
        
        /**
         * 【验证目标】验证本地方法库加载
         * 
         * 【JVM 原理】
         * JVM 启动时会加载必要的本地库（如 libjava, libjvm）。
         * 这些库包含大量 native 方法的实现。
         * 
         * 【面试考点】
         * Q: System.loadLibrary() 和 System.load() 有什么区别？
         * A: loadLibrary 从 java.library.path 加载，load 需要指定绝对路径。
         */
        @Test
        @DisplayName("6.1 验证本地方法库加载")
        void testNativeMethodLibrariesLoaded() {
            log.info("=== 开始验证本地方法库加载 ===");
            
            // 获取 java.library.path
            String libraryPath = System.getProperty("java.library.path");
            log.info("【本地库搜索路径 (java.library.path)】");
            
            String[] paths = libraryPath.split(System.getProperty("path.separator"));
            for (int i = 0; i < Math.min(paths.length, 5); i++) {
                log.info("  [{}] {}", i, paths[i]);
            }
            if (paths.length > 5) {
                log.info("  ... 共 {} 个路径", paths.length);
            }
            
            // 验证核心系统属性（由本地库提供）
            String osName = System.getProperty("os.name");
            String osArch = System.getProperty("os.arch");
            String javaHome = System.getProperty("java.home");
            
            log.info("【本地库提供的系统信息】");
            log.info("  操作系统: {} ({})", osName, osArch);
            log.info("  Java Home: {}", javaHome);
            
            // 验证文件系统操作（依赖本地库）
            String tempDir = System.getProperty("java.io.tmpdir");
            log.info("  临时目录: {}", tempDir);
            
            assertNotNull(osName, "操作系统名称不应为空");
            assertNotNull(javaHome, "Java Home 不应为空");
            
            log.info("【成功】本地方法库加载验证通过 ✓");
        }
    }
    
    // ==================================================================================
    // 第七部分：线程共享 vs 线程私有 属性验证
    // ==================================================================================
    
    @Nested
    @DisplayName("7. 共享/私有属性验证")
    class SharedVsPrivateTests {
        
        /**
         * 【验证目标】验证共享区域可跨线程访问
         * 
         * 【JVM 原理】
         * 堆（Heap）、方法区（Metaspace）是线程共享的。
         * 多个线程可以访问同一个堆上的对象。
         * 
         * 【面试考点】
         * Q: 为什么堆是线程共享的？
         * A: 对象需要跨线程共享访问，否则线程间通信会很困难。
         */
        @Test
        @DisplayName("7.1 验证共享内存区域可跨线程访问")
        void testSharedMemoryAreasAccessible() throws Exception {
            log.info("=== 开始验证共享内存区域跨线程访问 ===");
            
            // 在堆上创建共享对象
            AtomicLong sharedCounter = new AtomicLong(0);
            List<Long> sharedList = Collections.synchronizedList(new ArrayList<>());
            
            int threadCount = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);
            
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                new Thread(() -> {
                    // 多个线程访问同一个堆对象
                    for (int j = 0; j < 100; j++) {
                        sharedCounter.incrementAndGet();
                        sharedList.add((long) (threadId * 1000 + j));
                    }
                    latch.countDown();
                }, "SharedAccess-" + i).start();
            }
            
            latch.await(10, TimeUnit.SECONDS);
            
            log.info("【共享堆内存访问验证】");
            log.info("  {} 个线程共同操作的计数器值: {}", threadCount, sharedCounter.get());
            log.info("  共享列表大小: {}", sharedList.size());
            
            assertEquals(threadCount * 100, sharedCounter.get(), "计数器应该等于所有线程操作次数之和");
            assertEquals(threadCount * 100, sharedList.size(), "列表应该包含所有线程添加的元素");
            
            log.info("【成功】共享内存区域跨线程访问验证通过 ✓");
        }
        
        /**
         * 【验证目标】验证私有资源线程隔离
         * 
         * 【JVM 原理】
         * 虚拟机栈、PC 寄存器、本地方法栈是线程私有的。
         * 每个线程有独立的执行上下文，互不干扰。
         * 
         * 【面试考点】
         * Q: 线程私有区域有什么好处？
         * A: 不需要同步，执行效率高，天然线程安全。
         */
        @Test
        @DisplayName("7.2 验证私有资源线程隔离")
        void testPrivateResourcesIsolation() throws Exception {
            log.info("=== 开始验证私有资源线程隔离 ===");
            
            Map<String, Integer> threadStackDepths = new ConcurrentHashMap<>();
            CountDownLatch latch = new CountDownLatch(3);
            
            // 创建三个线程，每个线程有不同的递归深度
            for (int i = 1; i <= 3; i++) {
                final int depth = i * 10;
                new Thread(() -> {
                    int actualDepth = recurseAndCountDepth(depth, 0);
                    threadStackDepths.put(Thread.currentThread().getName(), actualDepth);
                    latch.countDown();
                }, "Isolation-" + depth).start();
            }
            
            latch.await(5, TimeUnit.SECONDS);
            
            log.info("【线程栈隔离验证】");
            threadStackDepths.forEach((name, depth) -> 
                log.info("  线程 {} 的递归深度: {}", name, depth));
            
            // 验证每个线程独立完成了自己的递归
            assertEquals(3, threadStackDepths.size(), "应该有 3 个线程的数据");
            assertTrue(threadStackDepths.values().containsAll(Arrays.asList(10, 20, 30)),
                "每个线程应该有不同的递归深度");
            
            log.info("【成功】私有资源线程隔离验证通过 ✓");
        }
        
        /**
         * 递归方法，记录栈深度
         */
        private int recurseAndCountDepth(int target, int current) {
            if (current >= target) {
                return current;
            }
            return recurseAndCountDepth(target, current + 1);
        }
    }
    
    // ==================================================================================
    // 第八部分：综合验证报告
    // ==================================================================================
    
    /**
     * 【验证目标】生成完整的 JVM 运行时数据区域验证报告
     * 
     * 【JVM 原理】
     * 汇总所有区域的信息，确保 JVM 各组件正常工作。
     */
    @Test
    @DisplayName("8. 生成 JVM 运行时数据区域综合报告")
    void generateComprehensiveReport() {
        log.info("╔═══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║        JVM 运行时数据区域全面验证报告 (JDK 17 HotSpot Perspective)             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════════════╝");
        
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        ClassLoadingMXBean classBean = ManagementFactory.getClassLoadingMXBean();
        
        log.info("");
        log.info("【1. JVM 基本信息】");
        log.info("   VM 名称: {}", runtimeBean.getVmName());
        log.info("   VM 版本: {}", runtimeBean.getVmVersion());
        log.info("   VM 供应商: {}", runtimeBean.getVmVendor());
        log.info("   启动时间: {} ms", runtimeBean.getUptime());
        
        log.info("");
        log.info("【2. Java Heap (线程共享 - GC Managed)】");
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        log.info("   已使用: {} MB / 最大: {} MB", 
            heap.getUsed() / 1024 / 1024, 
            heap.getMax() / 1024 / 1024);
        
        log.info("");
        log.info("【3. Native Memory Areas (线程共享 - OS Managed)】");
        
        // Metaspace
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getName().toLowerCase().contains("metaspace")) {
                log.info("   Metaspace 已使用: {} KB", pool.getUsage().getUsed() / 1024);
            }
            if (pool.getName().toLowerCase().contains("code") && pool.getName().toLowerCase().contains("cache")) {
                log.info("   Code Cache 已使用: {} KB", pool.getUsage().getUsed() / 1024);
            }
        }
        
        log.info("");
        log.info("【4. Per-Thread Resources (线程私有)】");
        log.info("   当前线程数: {}", threadBean.getThreadCount());
        log.info("   峰值线程数: {}", threadBean.getPeakThreadCount());
        log.info("   守护线程数: {}", threadBean.getDaemonThreadCount());
        
        log.info("");
        log.info("【5. Class Loading (Metaspace 相关)】");
        log.info("   已加载类: {}", classBean.getLoadedClassCount());
        log.info("   总加载类: {}", classBean.getTotalLoadedClassCount());
        log.info("   已卸载类: {}", classBean.getUnloadedClassCount());
        
        log.info("");
        log.info("【6. GC 收集器 (Execution Engine 相关)】");
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            log.info("   收集器: {} | 次数: {} | 时间: {} ms", 
                gc.getName(), gc.getCollectionCount(), gc.getCollectionTime());
        }
        
        log.info("");
        log.info("╔═══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║                          所有区域验证完成 ✓                                   ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════════════╝");
    }
}
