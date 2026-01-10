package com.example.jvmlab.chapter02.runtimedataareas.vmstack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 虚拟机栈实验服务
 * 用于演示 Stack Frame 的入栈与出栈，以及 StackOverflowError
 * <p>
 * 【对应书籍】 2.2.2 Java虚拟机栈
 * <p>
 * 【实验目标】
 * 1. 观察栈帧的创建 (Push) 和销毁 (Pop)。
 * 2. 观察局部变量表 (Local Variable Table) 的独立性。
 * 3. 验证 -Xss 参数对栈深度的影响。
 */
@Slf4j
@Service
public class StackLabService {

    // 计数器，用于记录当前递归深度
    private final AtomicInteger depthCounter = new AtomicInteger(0);

    /**
     * 递归方法：模拟入栈和出栈
     * @param limit 递归限制深度（防止直接溢出，用于观察出栈）
     */
    public void recursiveDive(int limit) {
        // 1. 记录当前深度
        int currentDepth = depthCounter.incrementAndGet();

        // --- 局部变量表观察区 (Local Variable Table Observation) ---
        // int 类型，占用 1 个 Slot
        int intVal = 100;
        // double 类型，占用 2 个 Slot (面试考点)
        double doubleVal = 999.99;
        // 引用类型，占用 1 个 Slot
        Object refObj = new Object();

        log.info(">>> [入栈 Push] 当前深度: {}, 准备进入下一层栈帧...", currentDepth);

        // TODO: 【断点 1】在此处打断点。
        // 观察 IDE 的 "Frames" 面板，你会发现随着递归，列表越来越长（栈帧在增加）。
        // 观察 "Variables" 面板，验证 intVal, doubleVal, refObj 是否存在于当前栈帧。

        if (currentDepth < limit) {
            // 继续递归调用，触发新的栈帧创建 (Push)
            this.recursiveDive(limit);
        } else {
            log.warn("=== 达到预设深度 limit: {}，准备开始回溯（出栈） ===", limit);
        }

        // TODO: 【断点 2】在此处打断点。
        // 此时递归触底反弹。每放行一次断点，你会发现 "Frames" 面板最上面的一行消失了。
        // 这就是 栈帧出栈 (Pop) 的过程，方法执行结束，内存释放。

        log.info("<<< [出栈 Pop] 离开深度: {}, 返回上一层...", currentDepth);
        // 清理计数器（为了多次实验）
        if (currentDepth == 1) {
            depthCounter.set(0);
        }
    }

    /**
     * 无限递归：用于触发 StackOverflowError
     */
    public void infiniteDive() {
        int depth = depthCounter.incrementAndGet();
        // 每 1000 层打印一次，避免刷屏
        if (depth % 1000 == 0) {
            log.info("当前递归深度: {}", depth);
        }
        // 这里没有停止条件，纯粹为了撑爆栈空间
        infiniteDive();
    }
    
    public int getCurrentDepth() {
        return depthCounter.get();
    }
    
    public void resetCounter() {
        depthCounter.set(0);
    }

    // ========================================================================
    // 新增实验一：验证局部变量表大小对栈深度的影响（回答面试追问）
    // ========================================================================

    /**
     * 实验方法：臃肿的栈帧递归
     * <p>
     * 目的：对比普通的 infiniteDive，验证当方法内定义了大量局部变量时，
     * 单个栈帧（Stack Frame）变大，导致同样的 -Xss 内存下，能容纳的递归深度显著减少。
     */
    public void recursionWithBloatedStackFrame(int depth) {
        // 记录深度
        depthCounter.set(depth);
        
        // --- 制造“臃肿”的局部变量表 ---
        // 在 64位 JVM 上，long 占用 2个 Slot (槽位)
        // 这里定义 50 个 long 变量，大约占用 100 个 Slot
        long v1=1, v2=2, v3=3, v4=4, v5=5, v6=6, v7=7, v8=8, v9=9, v10=10;
        long v11=1, v12=2, v13=3, v14=4, v15=5, v16=6, v17=7, v18=8, v19=9, v20=10;
        long v21=1, v22=2, v23=3, v24=4, v25=5, v26=6, v27=7, v28=8, v29=9, v30=10;
        long v31=1, v32=2, v33=3, v34=4, v35=5, v36=6, v37=7, v38=8, v39=9, v40=10;
        long v41=1, v42=2, v43=3, v44=4, v45=5, v46=6, v47=7, v48=8, v49=9, v50=10;

        // 使用一下变量，防止被 JIT 编译器优化掉（Dead Code Elimination）
        long sum = v1+v10+v20+v30+v40+v50;

        if (depth % 100 == 0) {
            log.info(">> [臃肿栈帧模式] 当前深度: {}", depth);
        }

        // 继续递归，直到 StackOverflow
        recursionWithBloatedStackFrame(depth + 1);
    }

    // ========================================================================
    // 新增实验二：高并发场景模拟（线程创建）
    // ========================================================================

    /**
     * 实验方法：模拟高并发长连接
     * <p>
     * 警告：此方法会创建大量线程。
     * 在 -Xss 较大（如 1MB）时，很容易抛出 OOM: unable to create new native thread。
     * 调优目标：将 -Xss 调小（如 256k），观察能否创建更多线程。
     */
    public void simulateConcurrentConnections(int threadCount) {
        log.warn("=== 开始压力测试：尝试创建 {} 个线程 ===", threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            try {
                Thread t = new Thread(() -> {
                    try {
                        // 模拟长连接，持有栈内存不释放
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, "Conn-Thread-" + i);
                t.start();

                if (index % 1000 == 0) {
                    log.info("已成功创建 {} 个线程...", index);
                }
            } catch (OutOfMemoryError e) {
                log.error("!!! 发生 OOM !!! 在创建第 {} 个线程时失败。", index);
                log.error("错误信息: {}", e.getMessage());
                log.error("当前 JVM 参数建议：尝试减小 -Xss 参数（如 -Xss256k）以容纳更多线程。");
                throw e; // 抛出异常中断测试
            }
        }
        log.info("=== 成功创建所有 {} 个线程，未发生 OOM ===", threadCount);
    }
}
