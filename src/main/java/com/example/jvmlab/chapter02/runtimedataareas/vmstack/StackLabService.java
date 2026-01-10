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
}
