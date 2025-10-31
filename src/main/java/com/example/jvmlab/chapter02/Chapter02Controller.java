package com.example.jvmlab.chapter02;

import com.example.jvmlab.common.JvmMemoryMonitor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * 第2章：Java内存区域与内存溢出异常。
 * <p>
 * 实现思路：
 * 1. 提供多个REST接口模拟书中常见的OOM场景，帮助理解堆、栈、方法区、直接内存的行为差异。
 * 2. 通过静态集合持有引用，确保内存不会被回收，从而稳定地复现异常。
 * 3. 使用详细的日志和内存监控工具，辅以中文+英文说明，方便面试或教学时讲解。
 * </p>
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
     * 触发堆内存溢出（Java heap space）。
     *
     * @param sizeMB  每次分配对象的大小（MB）。
     * @param delayMs 每次分配后的休眠时间（毫秒），便于观察。
     * @return 永远不会返回，预期抛出OutOfMemoryError。
     * @throws InterruptedException 如果线程被中断。
     */
    @GetMapping("/heap-oom")
    public String heapOOM(@RequestParam(defaultValue = "1") int sizeMB,
                          @RequestParam(defaultValue = "100") int delayMs) throws InterruptedException {
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
     * 触发栈溢出（StackOverflowError）。
     *
     * @return 返回栈深度信息。
     */
    @GetMapping("/stack-overflow")
    public String stackOverflow() {
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
     * 递归调用自身，模拟无限深度的调用栈。
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
     * 通过创建大量线程触发“unable to create new native thread”。
     *
     * @param maxThreads 期望创建的线程数量。
     * @return 成功创建的线程数。
     */
    @GetMapping("/thread-oom")
    public String threadOOM(@RequestParam(defaultValue = "10000") int maxThreads) {
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
     * 使用ByteBuddy动态生成类以占用元空间，触发Metaspace OOM。
     *
     * @param classCount 需要生成的类数量。
     * @return 成功生成的类数量。
     */
    @GetMapping("/metaspace-oom")
    public String metaspaceOOM(@RequestParam(defaultValue = "100000") int classCount) {
        log.warn("开始执行元空间溢出测试 Starting metaspace OOM test, target classes={}", classCount);
        int count = 0;
        try {
            while (count < classCount) {
                String className = "com.example.jvmlab.generated.DynamicClass" + UUID.randomUUID().toString().replace("-", "");
                Class<?> clazz = new ByteBuddy()
                        .subclass(Object.class)
                        .name(className)
                        .defineMethod("toString", String.class)
                        .intercept(FixedValue.value("Dynamic:" + count))
                        .make()
                        .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                        .getLoaded();
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
     * 测试字符串常量池的内存占用行为。
     *
     * @return 常量池中加入的字符串数量。
     */
    @GetMapping("/string-pool-oom")
    public String stringPoolOOM() {
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
     * 使用堆外内存（DirectByteBuffer）触发直接内存溢出。
     *
     * @param sizeMB 每次分配的大小（MB）。
     * @return 永远不会返回。
     */
    @GetMapping("/direct-memory-oom")
    public String directMemoryOOM(@RequestParam(defaultValue = "1") int sizeMB) {
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
     * 重置所有静态集合，帮助实验结束后释放内存。
     *
     * @return 重置结果说明。
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
     * 获取当前JVM内存使用情况。
     *
     * @return 内存信息Map。
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
