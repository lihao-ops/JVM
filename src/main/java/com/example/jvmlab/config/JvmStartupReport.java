package com.example.jvmlab.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JvmStartupReport implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("===================== JVM 参数自检 =====================");

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        List<String> args = runtime.getInputArguments();

        printHeapFlags(args);
        printThreadStack(args);
        printMetaspaceFlags(args);
        printDirectMemoryFlags(args);
        printXlog(args);
        printOomDump(args);
        printGcInfo();

        log.info("\n===================== JVM 运行时内存区域 =====================");
        printRuntimeMemoryAreas();

        log.info("\n========================== 结束 ==========================\n");
    }

    // ----------------------------- 参数区 -----------------------------

    private void printHeapFlags(List<String> args) {
        log.info("\n【堆内存（Heap）】");
        print(args, "-Xms", "初始堆大小");
        print(args, "-Xmx", "最大堆大小");
        print(args, "-Xmn", "新生代大小");
    }

    private void printThreadStack(List<String> args) {
        log.info("\n【线程栈（Thread Stack）】");
        print(args, "-Xss", "每线程栈大小");
    }

    private void printMetaspaceFlags(List<String> args) {
        log.info("\n【元空间（Metaspace）】");
        print(args, "MetaspaceSize", "初始元空间大小");
        print(args, "MaxMetaspaceSize", "最大元空间大小");
    }

    private void printDirectMemoryFlags(List<String> args) {
        log.info("\n【直接内存（Direct Memory）】");
        print(args, "MaxDirectMemorySize", "最大直接内存");
    }

    private void printXlog(List<String> args) {
        log.info("\n【GC 日志配置】");
        String xlog = args.stream()
                .filter(arg -> arg.startsWith("-Xlog:"))
                .findFirst()
                .orElse("(未配置)");
        log.info("  Xlog = {}", xlog);
    }

    private void printOomDump(List<String> args) {
        log.info("\n【OOM Dump】");
        print(args, "HeapDumpOnOutOfMemoryError", "是否开启");
        print(args, "HeapDumpPath", "Dump 文件路径");
    }

    private void printGcInfo() {
        log.info("\n【垃圾收集器】");
        String gcNames = String.join(", ",
                ManagementFactory.getGarbageCollectorMXBeans()
                        .stream().map(GarbageCollectorMXBean::getName)
                        .collect(Collectors.toList())
        );
        log.info("  使用的 GC = {}", gcNames);
    }

    private void print(List<String> args, String key, String desc) {
        String value = args.stream()
                .filter(arg -> arg.contains(key))
                .findFirst()
                .orElse("(未配置)");
        log.info("  {} : {} ({})", desc, pretty(value), value);
    }

    private String pretty(String raw) {
        if (raw.contains("m") || raw.contains("M") || raw.contains("k") || raw.contains("K")) {
            return raw;
        }
        return raw;
    }

    // ----------------------------- 运行时内存区域 -----------------------------

    private void printRuntimeMemoryAreas() {

        // 堆
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        log.info("【堆（Heap）】");
        log.info("  Init     : {} MB", heap.getInit() / 1024 / 1024);
        log.info("  Used     : {} MB", heap.getUsed() / 1024 / 1024);
        log.info("  Committed: {} MB", heap.getCommitted() / 1024 / 1024);
        log.info("  Max      : {} MB", heap.getMax() / 1024 / 1024);

        // 非堆（包括元空间）
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        log.info("\n【非堆（Non-Heap，包含元空间 Metaspace）】");
        log.info("  Init     : {} MB", nonHeap.getInit() / 1024 / 1024);
        log.info("  Used     : {} MB", nonHeap.getUsed() / 1024 / 1024);
        log.info("  Committed: {} MB", nonHeap.getCommitted() / 1024 / 1024);
        log.info("  Max      : {} MB", nonHeap.getMax() / 1024 / 1024);

        // 程序计数器（PC Register）
        log.info("\n【程序计数器（PC Register）】");
        log.info("  每个线程一个 PC 寄存器，用于保存下一条要执行的指令地址（JVM 规范固定，无可配参数）。");

        // 虚拟机栈（Java Stack）
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        log.info("\n【虚拟机栈（Java Virtual Machine Stack）】");
        log.info("  当前线程数量     : {}", threadBean.getThreadCount());
        log.info("  守护线程数量     : {}", threadBean.getDaemonThreadCount());
        log.info("  峰值线程数量     : {}", threadBean.getPeakThreadCount());
        log.info("  总创建线程数     : {}", threadBean.getTotalStartedThreadCount());
        log.info("  每线程栈大小 (-Xss): 请查看 JVM 参数配置");

        // 本地方法栈（Native Stack）
        log.info("\n【本地方法栈（Native Method Stack）】");
        log.info("  与 JVM Stack 共用栈空间，大小同 -Xss（依赖具体 JVM 实现）。");
    }
}
