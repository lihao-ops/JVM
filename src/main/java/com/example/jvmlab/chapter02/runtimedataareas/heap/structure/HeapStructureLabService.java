package com.example.jvmlab.chapter02.runtimedataareas.heap.structure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class HeapStructureLabService {

    // 存放一点“垃圾”来占用空间
    private List<byte[]> container = new ArrayList<>();

    public static void main(String[] args) {
        new HeapStructureLabService().allocateAndObserve();
    }

//    /**
//     * 实验：观察年轻代、老年代、Survivor区的分布与流转
//     */
//    public void allocateAndObserve() {
//        log.info("=== 🔬 开始堆内存结构解剖实验 (Heap Structure Analysis) ===");
//
//        // 1. 初始状态
//        printHeapLayout("1. 初始状态 (Empty)");
//
//        // 2. 填充 Eden 区 (分配一些对象)
//        log.info(">>> 正在向 Eden 区注入对象...");
//        // 修改：改为分配 10 个 1MB 的对象（总共 10MB），这样它们肯定会先呆在 Eden
//        for (int i = 0; i < 10; i++) {
//            container.add(new byte[1 * 1024 * 1024]); // 1MB
//        }
//        printHeapLayout("2. Eden 区半满");
//
//        // 3. 触发 GC (观察对象如何从 Eden -> Survivor/Old)
//        log.info(">>> 触发 System.gc()...");
//        System.gc();
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//        }
//
//        printHeapLayout("3. GC 之后 (观察对象晋升)");
//    }

    /**
     * 实验：观察对象在 Minor GC 后的去向 (Eden -> Survivor)
     */
    public void allocateAndObserve() {
        log.info("=== 🔬 开始堆内存结构解剖实验 (Heap Structure Analysis) ===");

        // 1. 初始状态
        printHeapLayout("1. 初始状态 (Empty)");

        // 2. 制造“金贵”的对象 (我们希望它们留下的)
        log.info(">>> 正在分配 10MB 核心存活对象...");
        // 强引用，GC 不会回收它们
        for (int i = 0; i < 10; i++) {
            container.add(new byte[1 * 1024 * 1024]);
        }
        printHeapLayout("2. 核心对象已分配 (在 Eden)");

        // 3. 制造“垃圾”来填满 Eden，迫使 JVM 触发 Minor GC
        log.info(">>> 正在分配垃圾对象，填满 Eden 以触发 Minor GC...");
        // Eden 总共 33MB，已用 ~15MB (核心+基础)。再分配约 20MB 垃圾就能撑爆它。
        for (int i = 0; i < 25; i++) {
            // 这些对象没有放入 container，是垃圾，GC 时会被回收
            byte[] garbage = new byte[1 * 1024 * 1024];
        }

        // 给一点时间让 GC 发生
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        printHeapLayout("3. Minor GC 之后 (见证奇迹)");
    }

    /**
     * 核心：扫描并可视化打印所有堆内存区域
     */
    private void printHeapLayout(String phase) {
        log.info("\n--- [ {} ] -------------------------------------------", phase);

        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();

        for (MemoryPoolMXBean pool : memoryPoolMXBeans) {
            // 我们只关心 堆内存 (Heap)，过滤掉非堆 (Non-Heap) 如 Metaspace
            if (pool.getType() == MemoryType.HEAP) {
                printPoolBar(pool);
            }
        }
        log.info("----------------------------------------------------------\n");
    }

    private void printPoolBar(MemoryPoolMXBean pool) {
        String name = pool.getName(); // 例如: "PS Eden Space", "G1 Old Gen"
        MemoryUsage usage = pool.getUsage();

        long used = usage.getUsed();
        long max = usage.getMax();

        // 某些区域可能 max 为 -1 (未定义)，做个保护
        if (max < 0) max = used;
        // 防止除以0
        if (max == 0) max = 1;

        long usedMB = used / 1024 / 1024;
        long maxMB = max / 1024 / 1024;
        int percent = (int) ((double) used / max * 100);

        // 绘制进度条
        int totalLength = 40;
        int filledLength = (int) ((double) used / max * totalLength);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < totalLength; i++) {
            if (i < filledLength) bar.append("█");
            else bar.append("-");
        }
        bar.append("]");

        // 格式化输出
        // 名字对齐，进度条，数值
        log.info(String.format("%-20s | %s %3d%% | %4dMB / %4dMB",
                name, bar.toString(), percent, usedMB, maxMB));
    }
}
