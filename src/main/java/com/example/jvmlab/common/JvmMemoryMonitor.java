package com.example.jvmlab.common;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.*;
import java.util.*;

/**
 * 类说明 / Class Description:
 * 中文：JVM 内存与运行时监控工具，提供堆/非堆/GC/线程/类加载等指标采集与格式化输出。
 * English: JVM memory and runtime monitoring utility that collects and formats heap/non-heap/GC/thread/class-loading metrics.
 *
 * 使用场景 / Use Cases:
 * 中文：作为监控接口的数据来源与调试日志工具，用于分析内存压力与 GC 行为。
 * English: Serves as data source for monitoring endpoints and diagnostic logging for memory pressure and GC behavior.
 *
 * 设计目的 / Design Purpose:
 * 中文：以静态方法暴露监控能力，确保调用简单与线程安全；返回结构化 Map 便于序列化。
 * English: Expose monitoring via static methods for simplicity and thread safety; return structured maps for serialization.
 */
@Slf4j
public final class JvmMemoryMonitor {

    /**
     * 方法说明 / Method Description:
     * 中文：私有构造，禁止实例化工具类。
     * English: Private constructor to prevent instantiation of utility class.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    private JvmMemoryMonitor() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 方法说明 / Method Description:
     * 中文：打印完整的 JVM 内存快照，包含堆、非堆、GC 与线程信息。
     * English: Print a full JVM memory snapshot including heap, non-heap, GC and thread info.
     *
     * 参数 / Parameters:
     * @param tag 中文：快照标签（如执行前/执行后） / English: Snapshot tag (e.g., before/after execution)
     *
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public static void printMemoryInfo(String tag) {
        log.info("==================== JVM内存信息快照 Memory Snapshot: {} ====================", tag);
        // 输出堆内存详情，观察Java堆的实时压力。
        printHeapMemory();
        // 输出非堆内存详情，关注元空间、代码缓存等区域。
        printNonHeapMemory();
        // 输出GC统计信息，了解垃圾回收器的运行效率。
        printGCInfo();
        // 输出线程指标，帮助定位线程过多或死锁等问题。
        printThreadInfo();
        log.info("======================================================================");
    }

    /**
     * 打印堆内存（Heap）使用情况。
     * 实现思路：通过MemoryMXBean获取堆的Init/Used/Committed/Max指标。
     */
    private static void printHeapMemory() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        log.info("Heap堆内存信息:");
        log.info("  Init初始值: {}", formatSize(heapUsage.getInit()));
        log.info("  Used已用: {} ({} %)",
                formatSize(heapUsage.getUsed()),
                heapUsage.getMax() > 0 ? String.format(Locale.ENGLISH, "%.2f", (double) heapUsage.getUsed() / heapUsage.getMax() * 100) : "N/A");
        log.info("  Committed提交: {}", formatSize(heapUsage.getCommitted()));
        log.info("  Max最大值: {}", formatSize(heapUsage.getMax()));
        // 输出所有堆内存池的详细占用情况。
        printMemoryPools("Heap");
    }

    /**
     * 打印非堆内存（Non-Heap）使用情况，包括元空间、代码缓存等。
     */
    private static void printNonHeapMemory() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        log.info("Non-Heap非堆内存信息:");
        log.info("  Init初始值: {}", formatSize(nonHeapUsage.getInit()));
        log.info("  Used已用: {}", formatSize(nonHeapUsage.getUsed()));
        log.info("  Committed提交: {}", formatSize(nonHeapUsage.getCommitted()));
        log.info("  Max最大值: {}", nonHeapUsage.getMax() == -1 ? "Undefined未定义" : formatSize(nonHeapUsage.getMax()));
        printMemoryPools("Non-Heap");
    }

    /**
     * 根据内存池类型输出详细信息，帮助理解Eden、Survivor、Old Gen、Metaspace等区域的占用情况。
     *
     * @param type 目标类型（Heap或Non-Heap）。
     */
    private static void printMemoryPools(String type) {
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : memoryPoolMXBeans) {
            MemoryType poolType = pool.getType();
            if (("Heap".equals(type) && poolType == MemoryType.HEAP) ||
                    ("Non-Heap".equals(type) && poolType == MemoryType.NON_HEAP)) {
                MemoryUsage usage = pool.getUsage();
                log.info("  [{}] 区域Usage:", pool.getName());
                log.info("    Used已用: {}", formatSize(usage.getUsed()));
                log.info("    Max最大值: {}",
                        usage.getMax() == -1 ? "Undefined未定义" : formatSize(usage.getMax()));
            }
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：打印元空间使用指标，观察动态代理、类加载等行为的影响。
     * English: Print Metaspace usage metrics to observe impacts of dynamic proxies and class loading.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public static void printMetaspaceInfo() {
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : memoryPoolMXBeans) {
            if (pool.getName().contains("Metaspace")) {
                MemoryUsage usage = pool.getUsage();
                log.info("Metaspace元空间使用情况: {} / {}",
                        formatSize(usage.getUsed()),
                        usage.getMax() == -1 ? "Undefined未定义" : formatSize(usage.getMax()));
                if (usage.getMax() > 0) {
                    double percentage = (double) usage.getUsed() / usage.getMax() * 100;
                    log.info("Metaspace使用率Usage Ratio: {}%", String.format(Locale.ENGLISH, "%.2f", percentage));
                }
            }
        }
    }

    /**
     * 打印垃圾回收器统计信息。
     * 实现思路：遍历所有GC MXBean，输出名称、回收次数和耗时。
     */
    private static void printGCInfo() {
        List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        log.info("Garbage Collection垃圾回收统计:");
        for (GarbageCollectorMXBean gcBean : gcMXBeans) {
            log.info("  [{}] 次数Count: {} 耗时Time: {} ms", gcBean.getName(), gcBean.getCollectionCount(), gcBean.getCollectionTime());
        }
    }

    /**
     * 打印线程运行状况，快速定位线程爆炸或死锁问题。
     */
    private static void printThreadInfo() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        log.info("Thread线程信息:");
        log.info("  当前线程数Current: {}", threadMXBean.getThreadCount());
        log.info("  峰值线程数Peak: {}", threadMXBean.getPeakThreadCount());
        log.info("  守护线程数Daemon: {}", threadMXBean.getDaemonThreadCount());
        log.info("  累计启动线程数Total Started: {}", threadMXBean.getTotalStartedThreadCount());
    }

    /**
     * 方法说明 / Method Description:
     * 中文：采集并返回堆/非堆/内存池/GC/线程/类加载的结构化监控数据。
     * English: Collect and return structured monitoring data for heap/non-heap/pools/GC/threads/class-loading.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：监控信息 Map / English: Monitoring info map
     * 异常 / Exceptions: 无
     */
    public static Map<String, Object> getMemoryInfoMap() {
        Map<String, Object> info = new LinkedHashMap<>();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        Map<String, String> heapInfo = new LinkedHashMap<>();
        heapInfo.put("init", formatSize(heapUsage.getInit()));
        heapInfo.put("used", formatSize(heapUsage.getUsed()));
        heapInfo.put("committed", formatSize(heapUsage.getCommitted()));
        heapInfo.put("max", formatSize(heapUsage.getMax()));
        heapInfo.put("usagePercent", heapUsage.getMax() > 0 ?
                String.format(Locale.ENGLISH, "%.2f%%", (double) heapUsage.getUsed() / heapUsage.getMax() * 100) : "N/A");
        info.put("heap", heapInfo);

        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        Map<String, String> nonHeapInfo = new LinkedHashMap<>();
        nonHeapInfo.put("init", formatSize(nonHeapUsage.getInit()));
        nonHeapInfo.put("used", formatSize(nonHeapUsage.getUsed()));
        nonHeapInfo.put("committed", formatSize(nonHeapUsage.getCommitted()));
        nonHeapInfo.put("max", nonHeapUsage.getMax() == -1 ? "Undefined" : formatSize(nonHeapUsage.getMax()));
        info.put("nonHeap", nonHeapInfo);

        Map<String, Map<String, String>> pools = new LinkedHashMap<>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            MemoryUsage usage = pool.getUsage();
            Map<String, String> poolInfo = new LinkedHashMap<>();
            poolInfo.put("type", pool.getType().toString());
            poolInfo.put("used", formatSize(usage.getUsed()));
            poolInfo.put("max", usage.getMax() == -1 ? "Undefined" : formatSize(usage.getMax()));
            pools.put(pool.getName(), poolInfo);
        }
        info.put("pools", pools);

        info.put("gc", getGCStats());

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> threadInfo = new LinkedHashMap<>();
        threadInfo.put("count", threadMXBean.getThreadCount());
        threadInfo.put("peak", threadMXBean.getPeakThreadCount());
        threadInfo.put("daemon", threadMXBean.getDaemonThreadCount());
        threadInfo.put("totalStarted", threadMXBean.getTotalStartedThreadCount());
        info.put("threads", threadInfo);

        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        Map<String, Object> classInfo = new LinkedHashMap<>();
        classInfo.put("loadedCount", classLoadingMXBean.getLoadedClassCount());
        classInfo.put("totalLoadedCount", classLoadingMXBean.getTotalLoadedClassCount());
        classInfo.put("unloadedCount", classLoadingMXBean.getUnloadedClassCount());
        info.put("classes", classInfo);

        return info;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：返回所有垃圾回收器的统计信息，包括次数、耗时与相关内存池。
     * English: Return statistics of all garbage collectors including counts, times and related memory pools.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：GC 信息 Map / English: GC info map
     * 异常 / Exceptions: 无
     */
    public static Map<String, Map<String, Object>> getGCStats() {
        Map<String, Map<String, Object>> gcInfo = new LinkedHashMap<>();
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("count", gcBean.getCollectionCount());
            stats.put("time", gcBean.getCollectionTime());
            stats.put("memoryPoolNames", Arrays.toString(gcBean.getMemoryPoolNames()));
            gcInfo.put(gcBean.getName(), stats);
        }
        return gcInfo;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：将字节数格式化为 KB/MB/GB，便于阅读。
     * English: Format bytes into KB/MB/GB for readability.
     *
     * 参数 / Parameters:
     * @param bytes 中文：字节数 / English: Byte count
     *
     * 返回值 / Return:
     * 中文：可读的容量字符串 / English: Human-readable capacity string
     *
     * 异常 / Exceptions: 无
     */
    public static String formatSize(long bytes) {
        if (bytes < 0) {
            return "N/A";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ENGLISH, "%.2f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.ENGLISH, "%.2f MB", bytes / (1024.0 * 1024));
        }
        return String.format(Locale.ENGLISH, "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取基础系统信息（OS/JVM）供性能排查与环境记录。
     * English: Get basic system info (OS/JVM) for performance diagnosis and environment recording.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：系统属性 Map / English: System properties map
     * 异常 / Exceptions: 无
     */
    public static Map<String, String> getSystemInfo() {
        Map<String, String> info = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        info.put("os.name", System.getProperty("os.name"));
        info.put("os.version", System.getProperty("os.version"));
        info.put("os.arch", System.getProperty("os.arch"));
        info.put("java.version", System.getProperty("java.version"));
        info.put("java.vendor", System.getProperty("java.vendor"));
        info.put("java.home", System.getProperty("java.home"));
        info.put("processors", String.valueOf(runtime.availableProcessors()));
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        info.put("jvm.name", runtimeMXBean.getVmName());
        info.put("jvm.version", runtimeMXBean.getVmVersion());
        info.put("jvm.vendor", runtimeMXBean.getVmVendor());
        info.put("uptime", formatDuration(runtimeMXBean.getUptime()));
        return info;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：将毫秒时长转换为友好的可读格式字符串。
     * English: Convert milliseconds into a human-readable duration string.
     *
     * 参数 / Parameters:
     * @param millis 中文：时长毫秒数 / English: Duration in milliseconds
     * 返回值 / Return: 中文：格式化时长 / English: Formatted duration string
     * 异常 / Exceptions: 无
     */
    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        if (days > 0) {
            return String.format(Locale.ENGLISH, "%dd %dh %dm", days, hours % 24, minutes % 60);
        }
        if (hours > 0) {
            return String.format(Locale.ENGLISH, "%dh %dm %ds", hours, minutes % 60, seconds % 60);
        }
        if (minutes > 0) {
            return String.format(Locale.ENGLISH, "%dm %ds", minutes, seconds % 60);
        }
        return String.format(Locale.ENGLISH, "%ds", seconds);
    }

    /**
     * 方法说明 / Method Description:
     * 中文：打印 JVM 启动参数，辅助排查参数配置问题。
     * English: Print JVM startup arguments to aid configuration troubleshooting.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public static void printJvmArguments() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMXBean.getInputArguments();
        log.info("==================== JVM启动参数 JVM Arguments ====================");
        arguments.forEach(arg -> log.info("  {}", arg));
        log.info("======================================================================");
    }

    /**
     * 方法说明 / Method Description:
     * 中文：基于堆使用率与 GC 指标进行简单泄漏风险评估，返回结构化建议。
     * English: Perform a simple leak risk assessment based on heap usage and GC metrics, returning structured advice.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：风险评估 Map / English: Risk assessment map
     * 异常 / Exceptions: 无
     */
    public static Map<String, Object> detectMemoryLeakRisk() {
        Map<String, Object> result = new LinkedHashMap<>();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        double usagePercent = heapUsage.getMax() > 0 ? (double) heapUsage.getUsed() / heapUsage.getMax() * 100 : 0;
        result.put("heapUsagePercent", String.format(Locale.ENGLISH, "%.2f%%", usagePercent));
        String riskLevel;
        if (usagePercent > 90) {
            riskLevel = "HIGH 高风险 - 内存占用接近极限";
        } else if (usagePercent > 75) {
            riskLevel = "MEDIUM 中风险 - 内存使用偏高";
        } else if (usagePercent > 60) {
            riskLevel = "LOW 低风险 - 内存波动可接受";
        } else {
            riskLevel = "SAFE 安全 - 内存状态良好";
        }
        result.put("riskLevel", riskLevel);
        long totalGCCount = 0;
        long totalGCTime = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            totalGCCount += gcBean.getCollectionCount();
            totalGCTime += gcBean.getCollectionTime();
        }
        result.put("totalGCCount", totalGCCount);
        result.put("totalGCTimeMs", totalGCTime);
        if (totalGCCount > 100 && usagePercent > 70) {
            result.put("warning", "Frequent GC with high heap usage 频繁GC且堆使用率高，需检查内存泄漏");
        }
        return result;
    }
}
