package com.example.jvmstress.ctrl;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JVM 错误压力测试控制器。
 * <p>
 * 实现思路：
 * 1. 通过 REST API 暴露常见 JVM 异常触发场景，便于在学习或排查时手动复现。<br>
 * 2. 使用静态集合缓存内存、类、线程引用，防止资源被垃圾回收，从而稳定触发 OOM。<br>
 * 3. 针对堆、直接内存、元空间、GC 开销、线程、虚拟机栈、Native 方法栈等典型问题提供多种触发方式。<br>
 * 4. 每个入口均提供参数化能力和详细注释，帮助使用者理解触发机制和注意事项。<br>
 * </p>
 */
@RestController
@RequestMapping("/trigger")
public class JvmErrorController {

    /**
     * 实现思路：
     * 1. 使用静态集合保存堆内存分配，避免引用被回收。<br>
     * 2. 提供 reset 参数用于在测试前清理历史数据。<br>
     * 3. 每个 chunk 的大小可通过请求参数指定，从而控制 OOM 发生的速度。<br>
     */
    private static final List<byte[]> HEAP_HOLD = new ArrayList<>();

    /**
     * 实现思路：
     * 1. 直接内存使用 ByteBuffer.allocateDirect 申请，避免进入堆区。<br>
     * 2. 使用静态集合缓存 ByteBuffer，确保引用不被 GC。<br>
     */
    private static final List<ByteBuffer> DIRECT_HOLD = new ArrayList<>();

    /**
     * 实现思路：
     * 1. 通过 ByteBuddy 生成大量类，快速消耗元空间。<br>
     * 2. 同时缓存类和 ClassLoader 引用，防止类被卸载。<br>
     */
    private static final List<Class<?>> METASPACE_CLASSES = new ArrayList<>();

    /**
     * 实现思路：与 {@link #METASPACE_CLASSES} 配合缓存类加载器，避免 ClassLoader 被 GC。
     */
    private static final List<ClassLoader> METASPACE_LOADERS = new ArrayList<>();

    /**
     * 实现思路：
     * 1. 线程列表统一缓存，便于 reset 时统一中断。<br>
     * 2. 多个触发入口公用，以免重复保存。<br>
     */
    private static final List<Thread> SPAWNED_THREADS = new ArrayList<>();

    /**
     * 实现思路：缓存热点 Map 数据，模拟 GC Overhead 的不断创建和保留对象场景。
     */
    private static final Map<Integer, String> HOT_MAP = new HashMap<>();

    /**
     * 触发堆内存溢出。
     * <p>
     * 实现思路：
     * 1. 按照指定 chunk 大小持续分配字节数组并保存引用。<br>
     * 2. 使用无限循环，直至 JVM 抛出 {@link OutOfMemoryError}。<br>
     * 3. 当 reset=true 时先清空历史引用，避免旧数据干扰测试。<br>
     * </p>
     *
     * @param mbPerChunk 每次分配的内存大小(MB)。
     * @param reset      是否重置之前分配的内存。
     * @return 不会返回，最终会抛出 {@link OutOfMemoryError}。
     */
    @GetMapping("/oom/heap")
    public String heapOom(@RequestParam(name = "mb", defaultValue = "10") int mbPerChunk,
                          @RequestParam(name = "reset", defaultValue = "false") boolean reset) {
        if (reset) {
            HEAP_HOLD.clear();
        }
        try {
            while (true) {
                HEAP_HOLD.add(new byte[Math.max(1, mbPerChunk) * 1024 * 1024]);
            }
        } catch (Throwable t) {
            if (t instanceof OutOfMemoryError) {
                throw (OutOfMemoryError) t;
            }
            throw new RuntimeException(t);
        }
    }

    /**
     * 触发直接内存溢出。
     * <p>
     * 实现思路：
     * 1. 按照指定大小不断申请直接缓冲区，并放入集合保持引用。<br>
     * 2. 重复申请直到 Direct Memory 被耗尽，触发 {@link OutOfMemoryError}。<br>
     * </p>
     *
     * @param mbPerChunk 每次分配的直接内存大小(MB)。
     * @param reset      是否重置之前分配的直接内存。
     * @return 不会返回，最终会抛出 {@link OutOfMemoryError}。
     */
    @GetMapping("/oom/direct")
    public String directOom(@RequestParam(name = "mb", defaultValue = "10") int mbPerChunk,
                            @RequestParam(name = "reset", defaultValue = "false") boolean reset) {
        if (reset) {
            DIRECT_HOLD.clear();
        }
        try {
            while (true) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(Math.max(1, mbPerChunk) * 1024 * 1024);
                DIRECT_HOLD.add(buffer);
            }
        } catch (Throwable t) {
            if (t instanceof OutOfMemoryError) {
                throw (OutOfMemoryError) t;
            }
            throw new RuntimeException(t);
        }
    }

    /**
     * 触发元空间溢出。
     * <p>
     * 实现思路：
     * 1. 借助 ByteBuddy 持续生成唯一类名的类，并通过 WRAPPER 策略加载。<br>
     * 2. 缓存生成的 Class 以及对应 ClassLoader，防止类被卸载释放元空间。<br>
     * 3. 支持 count 参数控制生成数量，便于逐步观察内存变化。<br>
     * </p>
     *
     * @param count 生成类的数量。
     * @return 成功生成的类数量描述。
     */
    @GetMapping("/oom/metaspace")
    public String metaspaceOom(@RequestParam(name = "count", defaultValue = "100000") int count) {
        int created = 0;
        try {
            while (created < count) {
                String name = "com.example.jvmstress.Generated" + UUID.randomUUID().toString().replace("-", "");
                Class<?> clazz = new ByteBuddy()
                        .subclass(Object.class)
                        .name(name)
                        .make()
                        .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                        .getLoaded();
                METASPACE_CLASSES.add(clazz);
                METASPACE_LOADERS.add(clazz.getClassLoader());
                created++;
            }
            return "Generated classes: " + created;
        } catch (Throwable t) {
            if (t instanceof OutOfMemoryError) {
                throw (OutOfMemoryError) t;
            }
            throw new RuntimeException(t);
        }
    }

    /**
     * 触发 GC Overhead Limit Exceeded。
     * <p>
     * 实现思路：
     * 1. 构造不断增长的 Map，并向其中存入重复字符串对象。<br>
     * 2. 通过 payload 参数控制字符串长度，模拟大量短期对象被创建的情况。<br>
     * </p>
     *
     * @param payload 每个字符串的重复次数。
     * @return 不会返回，最终会抛出 {@link OutOfMemoryError}。
     */
    @GetMapping("/oom/gc-overhead")
    public String gcOverhead(@RequestParam(name = "payload", defaultValue = "100") int payload) {
        int index = HOT_MAP.size();
        try {
            while (true) {
                HOT_MAP.put(index, ("X" + index).repeat(Math.max(1, payload)));
                index++;
            }
        } catch (Throwable t) {
            if (t instanceof OutOfMemoryError) {
                throw (OutOfMemoryError) t;
            }
            throw new RuntimeException(t);
        }
    }

    /**
     * 触发请求数组大小超出 VM 限制的异常。
     * <p>
     * 实现思路：
     * 1. 直接尝试分配指定长度的 int 数组。<br>
     * 2. 当长度超出虚拟机限制时抛出 {@link OutOfMemoryError} 或 {@link NegativeArraySizeException}。<br>
     * </p>
     *
     * @param length 数组长度。
     * @return 分配成功的数组信息。
     */
    @GetMapping("/oom/array-limit")
    public String arrayLimit(@RequestParam(name = "length", defaultValue = "2147483647") int length) {
        int[] array = new int[length];
        return "Allocated int[" + array.length + "]";
    }

    /**
     * 触发虚拟机栈溢出。
     * <p>
     * 实现思路：
     * 1. 通过无限递归迅速消耗当前线程的虚拟机栈。<br>
     * 2. 发生 {@link StackOverflowError} 后直接抛出，便于外部观察。<br>
     * </p>
     *
     * @return 不会返回，会抛出 {@link StackOverflowError}。
     */
    @GetMapping("/stack-overflow")
    public String stackOverflow() {
        recurse();
        return "unreachable";
    }

    /**
     * 触发虚拟机栈溢出（可配置深度）。
     * <p>
     * 实现思路：
     * 1. 允许通过 depth 参数控制递归深度，便于课堂演示。<br>
     * 2. depth=0 时与 {@link #stackOverflow()} 等效，持续递归直到溢出。<br>
     * </p>
     *
     * @param depth 限制递归深度，0 表示无限递归。
     * @return 正常情况下不会返回。
     */
    @GetMapping("/stack-overflow/vm")
    public String vmStackOverflow(@RequestParam(name = "depth", defaultValue = "0") int depth) {
        try {
            recursiveVmStack(depth, 0);
        } catch (StackOverflowError error) {
            throw error;
        }
        return "completed without overflow";
    }

    /**
     * 触发虚拟机栈 OutOfMemoryError（大量线程导致）。
     * <p>
     * 实现思路：
     * 1. 按照请求参数持续创建线程，每个线程保持睡眠状态以保留栈空间。<br>
     * 2. 通过监控创建数量，观察虚拟机在创建过多线程时的行为。<br>
     * </p>
     *
     * @param count   要创建的线程数量。
     * @param sleepMs 线程保持存活的睡眠时间。
     * @return 成功创建的线程数量描述。
     */
    @GetMapping("/oom/vm-stack")
    public String vmStackOom(@RequestParam(name = "count", defaultValue = "100000") int count,
                             @RequestParam(name = "sleepMs", defaultValue = "600000") long sleepMs) {
        try {
            int created = spawnThreads(count, sleepMs, 0L, "vm-stack-thread-");
            return "Spawned VM stack threads: " + created;
        } catch (Throwable t) {
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw new RuntimeException(t);
        }
    }

    /**
     * 触发本地方法栈溢出。
     * <p>
     * 实现思路：
     * 1. 创建自定义栈大小的线程，在该线程中执行无限递归。<br>
     * 2. 使用 {@link CountDownLatch} 和 {@link AtomicReference} 回传子线程抛出的异常。<br>
     * 3. 通过 stackKb 参数控制栈容量，便于观察不同设置下的行为。<br>
     * </p>
     *
     * @param stackKb 子线程的栈大小（KB）。
     * @return 正常情况下不会返回。
     */
    @GetMapping("/stack-overflow/native")
    public String nativeStackOverflow(@RequestParam(name = "stackKb", defaultValue = "64") int stackKb) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> holder = new AtomicReference<>();
        Thread thread = new Thread(null, () -> {
            try {
                nativeRecurse();
            } catch (Throwable throwable) {
                holder.set(throwable);
            } finally {
                latch.countDown();
            }
        }, "native-stack-overflow", Math.max(32L, stackKb) * 1024);
        thread.setDaemon(false);
        thread.start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待本地方法栈线程结束时被中断", e);
        }
        Throwable throwable = holder.get();
        if (throwable instanceof StackOverflowError) {
            throw (StackOverflowError) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        return "native stack overflow simulation finished";
    }

    /**
     * 触发本地方法栈 OutOfMemoryError。
     * <p>
     * 实现思路：
     * 1. 自定义线程栈大小（通常大于默认值），大量创建线程以快速耗尽本地方法栈。<br>
     * 2. 若线程创建失败将抛出 {@link OutOfMemoryError}，此时错误会被直接向上抛出。<br>
     * </p>
     *
     * @param count     要创建的线程数量。
     * @param stackMb   每个线程的栈大小（MB）。
     * @param sleepMs   线程休眠时间。
     * @return 成功创建的线程数量描述。
     */
    @GetMapping("/oom/native-stack")
    public String nativeStackOom(@RequestParam(name = "count", defaultValue = "1000") int count,
                                 @RequestParam(name = "stackMb", defaultValue = "8") int stackMb,
                                 @RequestParam(name = "sleepMs", defaultValue = "600000") long sleepMs) {
        long stackBytes = Math.max(1L, stackMb) * 1024 * 1024;
        try {
            int created = spawnThreads(count, sleepMs, stackBytes, "native-stack-thread-");
            return "Spawned native stack threads: " + created;
        } catch (Throwable t) {
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw new RuntimeException(t);
        }
    }

    /**
     * 触发无法创建新的本地线程错误。
     * <p>
     * 实现思路：
     * 1. 不断创建线程并保持存活，最终触发 "unable to create new native thread"。<br>
     * 2. 该方法与栈相关错误互相补充，演示线程数量达到系统上限时的表现。<br>
     * </p>
     *
     * @param count   要创建的线程数量。
     * @param sleepMs 每个线程的休眠时间(毫秒)。
     * @return 成功创建的线程数量描述。
     */
    @GetMapping("/oom/native-threads")
    public String nativeThreads(@RequestParam(name = "count", defaultValue = "100000") int count,
                                @RequestParam(name = "sleepMs", defaultValue = "600000") long sleepMs) {
        try {
            int created = spawnThreads(count, sleepMs, 0L, "stress-thread-");
            return "Spawned threads: " + created;
        } catch (Throwable t) {
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw new RuntimeException(t);
        }
    }

    /**
     * 重置/清理所有分配的资源。
     * <p>
     * 实现思路：
     * 1. 清空所有静态集合，释放堆、直接内存以及元空间对象引用。<br>
     * 2. 中断已创建的线程并触发 GC，保证下一次测试环境干净。<br>
     * </p>
     *
     * @return 清理结果。
     */
    @GetMapping("/reset")
    public String reset() {
        HEAP_HOLD.clear();
        DIRECT_HOLD.clear();
        METASPACE_CLASSES.clear();
        METASPACE_LOADERS.clear();
        HOT_MAP.clear();
        for (Thread thread : SPAWNED_THREADS) {
            thread.interrupt();
        }
        SPAWNED_THREADS.clear();
        System.gc();
        return "cleared";
    }

    /**
     * 无限递归方法，用于触发虚拟机栈溢出。
     * <p>
     * 实现思路：在同一线程中调用自身，迅速耗尽当前线程的栈空间。
     * </p>
     */
    private void recurse() {
        recurse();
    }

    /**
     * 控制递归深度的虚拟机栈溢出方法。
     * <p>
     * 实现思路：
     * 1. 当 depth=0 时不断递归导致溢出。<br>
     * 2. depth>0 可用于演示栈帧逐步增长的情况。<br>
     * </p>
     */
    private void recursiveVmStack(int depth, int current) {
        if (depth > 0 && current >= depth) {
            return;
        }
        recursiveVmStack(depth, current + 1);
    }

    /**
     * 无限递归方法，配合自定义线程栈大小触发本地方法栈溢出。
     */
    private void nativeRecurse() {
        nativeRecurse();
    }

    /**
     * 批量创建线程的通用逻辑。
     * <p>
     * 实现思路：
     * 1. 根据 stackBytes 参数决定是否使用自定义栈大小构造线程。<br>
     * 2. 线程体简单休眠，确保栈空间长期被占用。<br>
     * 3. 将线程引用保存至 {@link #SPAWNED_THREADS}，便于统一清理。<br>
     * </p>
     *
     * @param count      创建线程数量。
     * @param sleepMs    每个线程的休眠时间。
     * @param stackBytes 线程栈大小，0 表示使用 JVM 默认值。
     * @param namePrefix 线程名前缀。
     * @return 成功创建的线程数量。
     */
    private int spawnThreads(int count, long sleepMs, long stackBytes, String namePrefix) {
        int created = 0;
        try {
            for (int i = 0; i < count; i++) {
                Runnable task = () -> {
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                };
                Thread thread;
                if (stackBytes > 0) {
                    thread = new Thread(null, task, namePrefix + i, stackBytes);
                } else {
                    thread = new Thread(task, namePrefix + i);
                }
                thread.setDaemon(false);
                thread.start();
                SPAWNED_THREADS.add(thread);
                created++;
            }
            return created;
        } catch (Throwable t) {
            throw t;
        }
    }
}
