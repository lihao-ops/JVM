package com.example.jvmlab.chapter02;

import com.example.jvmlab.common.AsmDynamicClassBuilder;
import com.example.jvmlab.common.JvmMemoryMonitor;
import com.example.jvmlab.common.ExperimentSafetyGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * 类说明 / Class Description:
 * 中文：第2章控制器，涵盖堆、栈、元空间、直接内存与字符串常量池等 OOM 场景的实验接口。
 * English: Chapter 02 controller providing experiment endpoints for heap, stack, metaspace, direct memory and string intern pool OOM scenarios.
 *
 * 使用场景 / Use Cases:
 * 中文：用于课堂教学与面试演示，快速复现典型内存问题并配合监控输出进行分析。
 * English: For teaching and interview demos to quickly reproduce typical memory issues and analyze with monitoring outputs.
 *
 * 设计目的 / Design Purpose:
 * 中文：使用静态集合与可配置参数稳定触发异常，搭配监控日志增强可观测性。
 * English: Use static holders and configurable parameters to reliably trigger exceptions with enhanced observability via logs.
 */
@Slf4j
@RestController
@RequestMapping("/chapter02")
public class Chapter02Controller {

    /** 持有堆对象的静态集合，防止GC提前回收。 */
    private static final List<HeapOOMObject> HEAP_OBJECTS = new ArrayList<>();
    /** 持有直接内存缓冲区，模拟Direct Memory泄漏。 */
    private static final List<ByteBuffer> DIRECT_BUFFERS = new ArrayList<>();
    /** 持有动态生成的Class，模拟元空间膨胀。 */
    private static final List<Class<?>> CLASS_HOLDER = new ArrayList<>();

    /** 栈溢出测试时用于记录递归深度。 */
    private int stackDepth = 0;

    /**
     * 方法说明 / Method Description:
     * 中文：触发堆内存溢出，通过静态集合持有对象引用，防止 GC 回收。
     * English: Trigger heap OOM by retaining object references in a static collection to prevent GC reclamation.
     *
     * 章节标注 / Book Correlation:
     * 中文：第2章 运行时数据区 → Java 堆（Heap）
     * English: Chapter 2 Runtime Data Area → Java Heap
     *
     * 参数 / Parameters:
     * @param sizeMB 中文：每次分配对象的大小（MB） / English: Object allocation size per loop (MB)
     * @param delayMs 中文：分配后的休眠时间（毫秒） / English: Sleep after each allocation (ms)
     *
     * 返回值 / Return:
     * 中文：无（预期抛出 OOM） / English: None (expected to throw OOM)
     *
     * 异常 / Exceptions:
     * 中文：OutOfMemoryError；可能抛出 InterruptedException / English: OutOfMemoryError; may throw InterruptedException
     */
    @GetMapping("/heap-oom")
    public String heapOOM(@RequestParam(defaultValue = "1") int sizeMB,
                          @RequestParam(defaultValue = "100") int delayMs) throws InterruptedException {
        ExperimentSafetyGuard.assertEnabled();
        log.warn("开始执行堆内存溢出测试 Starting heap OOM test, size={}MB, delay={}ms", sizeMB, delayMs);
        JvmMemoryMonitor.printMemoryInfo("Before Heap OOM Test 测试前");
        int count = 0;
        try {
            while (true) {
                // 创建指定大小的对象并放入静态集合，阻止GC。
                HEAP_OBJECTS.add(new HeapOOMObject(sizeMB * 1024 * 1024));
                count++;
                if (count % 10 == 0) {
                    log.info("已分配对象数 Allocated objects: {} -> 约{} MB", count, count * sizeMB);
                    JvmMemoryMonitor.printMemoryInfo("Heap Allocation In Progress 堆分配中");
                }
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
            }
        } catch (OutOfMemoryError error) {
            log.error("触发堆内存溢出 Heap OOM triggered after {} allocations", count);
            JvmMemoryMonitor.printMemoryInfo("Heap OOM 时刻 At OOM");
            throw error;
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：通过无终止条件递归触发 StackOverflowError，并返回捕获时的深度信息。
     * English: Trigger StackOverflowError via unbounded recursion and return depth information upon capture.
     *
     * 章节标注 / Book Correlation:
     * 中文：第2章 运行时数据区 → 线程私有：虚拟机栈
     * English: Chapter 2 Runtime Data Area → Thread-Private: VM Stack
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：栈溢出发生时的深度描述 / English: Depth description at overflow
     * 异常 / Exceptions: 中文：StackOverflowError / English: StackOverflowError
     */
    @GetMapping("/stack-overflow")
    public String stackOverflow() {
        ExperimentSafetyGuard.assertEnabled();
        log.warn("开始执行栈溢出测试 Starting stack overflow test");
        stackDepth = 0;
        try {
            recursiveCall();
        } catch (StackOverflowError error) {
            log.error("栈溢出已触发 Stack overflow triggered, depth={}", stackDepth);
            return "StackOverflowError at depth: " + stackDepth;
        }
        return "unreachable";
    }

    /**
     * 方法说明 / Method Description:
     * 中文：递归调用自身以快速消耗线程栈，并通过局部变量增加栈帧大小。
     * English: Recursively call itself to consume thread stack, using locals to enlarge stack frames.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 中文：StackOverflowError / English: StackOverflowError
     */
    private void recursiveCall() {
        stackDepth++;
        long local1 = stackDepth;
        long local2 = stackDepth * 2L;
        long local3 = local1 + local2;
        log.debug("递归深度 Stack depth: {} | 临时变量 temp variables: {}, {}", stackDepth, local2, local3);
        recursiveCall();
    }

    /**
     * 方法说明 / Method Description:
     * 中文：通过创建大量非守护线程并保持睡眠，模拟无法创建新线程的错误。
     * English: Simulate "unable to create new native thread" by creating many non-daemon sleeping threads.
     *
     * 章节标注 / Book Correlation:
     * 中文：第2章 运行时数据区 → 本地线程资源/线程栈
     * English: Chapter 2 Runtime Data Area → Native thread resources/thread stack
     *
     * 参数 / Parameters:
     * @param maxThreads 中文：目标创建线程数 / English: Target number of threads to create
     * 返回值 / Return: 中文：实际创建的线程数 / English: Number of threads created
     * 异常 / Exceptions: 中文：OutOfMemoryError / English: OutOfMemoryError
     */
    @GetMapping("/thread-oom")
    public String threadOOM(@RequestParam(defaultValue = "10000") int maxThreads) {
        ExperimentSafetyGuard.assertEnabled();
        log.warn("开始执行线程OOM测试 Starting thread OOM test, target threads={}", maxThreads);
        List<Thread> threads = new ArrayList<>();
        int count = 0;
        try {
            while (count < maxThreads) {
                Thread thread = new Thread(() -> {
                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }, "stress-thread-" + count);
                thread.setDaemon(false);
                thread.start();
                threads.add(thread);
                count++;
                if (count % 100 == 0) {
                    log.info("已创建线程数 Created threads: {}", count);
                }
            }
            return "Created threads: " + count;
        } catch (OutOfMemoryError error) {
            log.error("线程创建失败 Thread creation failed after {} threads", count);
            threads.forEach(Thread::interrupt);
            return "OutOfMemoryError after creating " + count + " threads";
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：使用 ASM 生成大量类以消耗 Metaspace，触发 OOM。
     * English: Use ASM to generate many classes consuming Metaspace to trigger OOM.
     *
     * 章节标注 / Book Correlation:
     * 中文：第2章 运行时数据区 → 方法区/元空间（JDK8+）
     * English: Chapter 2 Runtime Data Area → Method Area/Metaspace (JDK8+)
     *
     * 参数 / Parameters:
     * @param classCount 中文：目标生成类数量 / English: Target number of classes to generate
     * 返回值 / Return: 中文：成功生成的类数量描述 / English: Description of generated class count
     * 异常 / Exceptions: 中文：OutOfMemoryError / English: OutOfMemoryError
     */
    @GetMapping("/metaspace-oom")
    public String metaspaceOOM(@RequestParam(defaultValue = "100000") int classCount) {
        ExperimentSafetyGuard.assertEnabled();
        log.warn("开始执行元空间溢出测试 Starting metaspace OOM test, target classes={}", classCount);
        int count = 0;
        try {
            while (count < classCount) {
                String className = "com.example.jvmlab.generated.DynamicClass" + UUID.randomUUID().toString().replace("-", "");
                Class<?> clazz = AsmDynamicClassBuilder.createConstantToStringClass(
                        getClass().getClassLoader(),
                        className,
                        "Dynamic:" + count);
                CLASS_HOLDER.add(clazz);
                count++;
                if (count % 1000 == 0) {
                    log.info("已生成类数 Generated classes: {}", count);
                    JvmMemoryMonitor.printMetaspaceInfo();
                }
            }
            return "Generated classes: " + count;
        } catch (OutOfMemoryError error) {
            log.error("元空间溢出已触发 Metaspace OOM triggered after {} classes", count);
            JvmMemoryMonitor.printMetaspaceInfo();
            throw error;
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：测试字符串常量池占用，通过不断调用 intern() 增加常量池内容。
     * English: Test string pool occupancy by repeatedly calling intern() to grow pool contents.
     *
     * 章节标注 / Book Correlation:
     * 中文：第2章 运行时数据区 → 常量池（JDK8 之后位于堆）
     * English: Chapter 2 Runtime Data Area → String intern pool (on heap since JDK8)
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：最终加入的字符串数量描述 / English: Description of total strings added
     * 异常 / Exceptions: 中文：OutOfMemoryError / English: OutOfMemoryError
     */
    @GetMapping("/string-pool-oom")
    public String stringPoolOOM() {
        ExperimentSafetyGuard.assertEnabled();
        log.warn("开始执行字符串常量池测试 Starting string pool pressure test");
        List<String> list = new ArrayList<>();
        int count = 0;
        try {
            while (true) {
                String value = String.valueOf(count++).intern();
                list.add(value);
                if (count % 10000 == 0) {
                    log.info("常量池中字符串数量 Strings in pool: {}", count);
                }
            }
        } catch (OutOfMemoryError error) {
            log.error("字符串常量池溢出 String pool OOM after {} entries", count);
            return "OutOfMemoryError after creating " + count + " strings";
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：通过 DirectByteBuffer 分配堆外内存直到 OOM。
     * English: Allocate off-heap memory via DirectByteBuffer until OOM.
     *
     * 章节标注 / Book Correlation:
     * 中文：第2章 运行时数据区 → 直接内存（堆外内存）
     * English: Chapter 2 Runtime Data Area → Direct (off-heap) memory
     *
     * 参数 / Parameters:
     * @param sizeMB 中文：每次分配大小（MB） / English: Allocation size per chunk (MB)
     * 返回值 / Return: 中文：无（预期 OOM） / English: None (expected OOM)
     * 异常 / Exceptions: 中文：OutOfMemoryError / English: OutOfMemoryError
     */
    @GetMapping("/direct-memory-oom")
    public String directMemoryOOM(@RequestParam(defaultValue = "1") int sizeMB) {
        ExperimentSafetyGuard.assertEnabled();
        log.warn("开始执行直接内存溢出测试 Starting direct memory OOM test, chunk={}MB", sizeMB);
        int count = 0;
        try {
            while (true) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(sizeMB * 1024 * 1024);
                DIRECT_BUFFERS.add(buffer);
                count++;
                if (count % 10 == 0) {
                    log.info("已分配DirectBuffer数量 Allocated direct buffers: {}", count);
                }
            }
        } catch (OutOfMemoryError error) {
            log.error("直接内存溢出 Direct memory OOM triggered after {} buffers", count);
            throw error;
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：重置各静态集合并触发 GC，用于清理实验状态与释放内存。
     * English: Reset static collections and trigger GC to clean experiment state and free memory.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：重置结果说明 / English: Reset result message
     * 异常 / Exceptions: 无
     */
    @PostMapping("/reset")
    public String reset() {
        log.info("重置Chapter02实验 Reset Chapter02 state");
        HEAP_OBJECTS.clear();
        DIRECT_BUFFERS.clear();
        CLASS_HOLDER.clear();
        System.gc();
        JvmMemoryMonitor.printMemoryInfo("After Reset 重置后");
        return "Chapter02 state cleared";
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取当前 JVM 内存使用情况的结构化数据，用于前端展示。
     * English: Get structured current JVM memory usage for frontend display.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：内存信息 Map / English: Memory info map
     * 异常 / Exceptions: 无
     */
    @GetMapping("/memory-info")
    public Map<String, Object> getMemoryInfo() {
        log.info("查询内存监控信息 Fetching memory metrics");
        return JvmMemoryMonitor.getMemoryInfoMap();
    }

    /**
     * 用于占用堆内存的对象定义。
     */
    static class HeapOOMObject {
        private final byte[] data;

        /**
         * 构造函数：创建指定大小的字节数组。
         *
         * @param size 字节数组长度。
         */
        HeapOOMObject(int size) {
            this.data = new byte[size];
        }
    }
}
