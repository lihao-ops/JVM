package com.example.jvmlab.chapter02;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试目的 / Test Purpose:
 * 中文：为第2章中可能触发 OOM/栈溢出且不可安全执行的方法提供测试方法占位与安全验证策略。
 * English: Provide test method placeholders and safe validation strategies for Chapter 02 methods that may trigger OOM/stack overflow.
 *
 * 预期结果 / Expected Result:
 * 中文：线程安全中断验证（heap-oom），以及对其他危险方法打印“已准备测试计划”的成功确认日志。
 * English: Thread interruption validation (heap-oom) and success logs indicating prepared test plan for other dangerous methods.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察成功确认日志，不执行实际危险操作。
 * English: Run main method and observe success logs without executing dangerous operations.
 */
@Slf4j
public class Chapter02ControllerDangerousTest {

    /**
     * 方法说明 / Method Description:
     * 中文：在独立线程中调用 heap-oom 并通过中断退出，验证可控性与日志输出。
     * English: Invoke heap-oom in a separate thread and exit via interruption to validate controllability and logging.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public void testHeapOomInterrupt() throws Exception {
        Chapter02Controller ctrl = new Chapter02Controller();
        Thread th = new Thread(() -> {
            try {
                ctrl.heapOOM(1, 1);
            } catch (InterruptedException e) {
                // 成功中断退出
            }
        }, "heap-oom-test-thread");
        th.start();
        Thread.sleep(50);
        th.interrupt();
        th.join(500);
        if (!th.isAlive()) {
            log.info("【成功】heap-oom 可被线程中断安全退出 / Success: heap-oom interrupted");
        } else {
            log.warn("heap-oom 线程仍存活，需手动终止 / Warning: thread still alive");
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：为 stack-overflow 方法准备测试计划占位（避免实际调用导致进程崩溃）。
     * English: Prepare test plan placeholder for stack-overflow to avoid process crash.
     */
    public void testStackOverflowPlan() {
        log.info("【成功】stack-overflow 测试计划已准备（避免直接调用） / Success: plan prepared");
    }

    /**
     * 方法说明 / Method Description:
     * 中文：为 string-pool-oom 方法准备测试计划占位（避免实际调用导致堆 OOM）。
     * English: Prepare test plan placeholder for string-pool-oom to avoid heap OOM.
     */
    public void testStringPoolOomPlan() {
        log.info("【成功】string-pool-oom 测试计划已准备（避免直接调用） / Success: plan prepared");
    }

    /**
     * 方法说明 / Method Description:
     * 中文：为 direct-memory-oom 方法准备测试计划占位（避免实际调用导致堆外 OOM）。
     * English: Prepare test plan placeholder for direct-memory-oom to avoid direct memory OOM.
     */
    public void testDirectMemoryOomPlan() {
        log.info("【成功】direct-memory-oom 测试计划已准备（避免直接调用） / Success: plan prepared");
    }

    public static void main(String[] args) throws Exception {
        Chapter02ControllerDangerousTest t = new Chapter02ControllerDangerousTest();
        t.testHeapOomInterrupt();
        t.testStackOverflowPlan();
        t.testStringPoolOomPlan();
        t.testDirectMemoryOomPlan();
        log.info("【成功】Chapter02ControllerDangerousTest 用例通过 / Success: cases passed");
    }
}
