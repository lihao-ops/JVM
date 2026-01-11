package com.example.jvmlab.chapter02.runtimedataareas.heap.expansion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class HeapLabService {

    public static void main(String[] args) {
        new HeapLabService().heapExpansionTest();
    }

    /**
     * 实验：可视化堆内存震荡 (Heap Fluctuation Visualization)
     * 目标：通过控制台进度条，亲眼看到 totalMemory 像“爬楼梯”一样变大，又像“跳水”一样变小。
     */
    public void heapExpansionTest() {
        log.info("===================================================================");
        log.info(">>> 🚀 堆内存震荡实验启动 (Heap Fluctuation Experiment)");
        log.info(">>> 观察重点：关注 [Total/Committed] 的水位线变化");
        log.info("===================================================================");

        printVisualHeapStatus("实验开始");

        List<byte[]> list = new ArrayList<>();

        // === 阶段 1：疯狂分配 (模拟流量洪峰) ===
        // 强迫 JVM 不断向 OS 申请内存 (扩容)
        for (int i = 1; i <= 10; i++) {
            // 每次塞入 10MB
            list.add(new byte[10 * 1024 * 1024]);

            // 模拟业务处理耗时，让你看清过程
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
            }

            printVisualHeapStatus("分配中... (第" + i + "次/共10次)");
        }

        log.info("-------------------------------------------------------------------");
        log.info(">>> 🛑 流量高峰结束，准备释放对象...");
        log.info("-------------------------------------------------------------------");

        // === 阶段 2：释放资源 (模拟流量低谷) ===
        list.clear();
        log.info(">>> 对象已 Clear (变成垃圾)，等待 GC...");

        // === 阶段 3：触发 GC (诱发缩容) ===
        System.gc(); // 建议 JVM 进行垃圾回收

        // 给 JVM 一点时间去归还内存给 OS
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        printVisualHeapStatus("GC完成 & 闲置后");
        log.info("===================================================================");
    }

    /**
     * 核心：可视化打印堆内存状态
     */
    private void printVisualHeapStatus(String phase) {
        // 1. 获取 JVM 内存数据 (单位转换成 MB)
        long totalBytes = Runtime.getRuntime().totalMemory(); // 当前已从 OS 拿到的
        long maxBytes = Runtime.getRuntime().maxMemory();     // 最大能拿多少 (-Xmx)
        long freeBytes = Runtime.getRuntime().freeMemory();   // 当前 Total 里还没用的
        long usedBytes = totalBytes - freeBytes;              // 实际存了多少对象

        long totalMB = totalBytes / 1024 / 1024;
        long maxMB = maxBytes / 1024 / 1024;
        long usedMB = usedBytes / 1024 / 1024;

        // 2. 计算进度条 (以 Max 为总长度 50格)
        int barLength = 50;
        // 防止除以0异常
        if (maxBytes == 0) maxBytes = 1;

        int totalPercent = (int) ((double) totalBytes / maxBytes * 100);
        int totalChars = (int) ((double) totalBytes / maxBytes * barLength);

        // 构造进度条：[################.........]
        StringBuilder progressBar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < totalChars) {
                progressBar.append("█"); // 代表已申请的内存 (Total)
            } else {
                progressBar.append("-"); // 代表还未申请的空闲空间
            }
        }
        progressBar.append("]");

        // 3. 打印可视化日志
        String status = String.format("%-20s | %s %3d%% | Used: %3dMB | Total(当前向OS申请): %3dMB | Max: %3dMB",
                phase, progressBar.toString(), totalPercent, usedMB, totalMB, maxMB);

        log.info(status);
    }
}
