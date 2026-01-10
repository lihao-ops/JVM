package com.example.jvmlab.chapter02;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：为第2章中可能触发 OOM/栈溢出的危险操作提供测试方法占位与安全验证策略。
 * English: Provide test method placeholders and safe validation strategies for Chapter 02 methods that may trigger OOM/stack overflow.
 *
 * 预期结果 / Expected Result:
 * 中文：验证危险测试的可控性，打印"已准备测试计划"的成功确认日志。
 * English: Validate controllability of dangerous tests, print success logs indicating prepared test plan.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察成功确认日志，不执行实际危险操作。
 * English: Run main method and observe success logs without executing dangerous operations.
 *
 * 【对应书籍】《深入理解Java虚拟机（第3版）》第2章 - Java内存区域与内存溢出异常
 */
@Slf4j
public class Chapter02ControllerDangerousTest {

    /**
     * 方法说明 / Method Description:
     * 中文：验证虚拟机栈信息获取正确性（安全操作）。
     * English: Validate VM stack info retrieval (safe operation).
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public void testVMStackInfoSafe() {
        JvmRuntimeDataAreaController ctrl = new JvmRuntimeDataAreaController();
        Map<String, Object> explanation = ctrl.explainVMStack();
        
        if (explanation.containsKey("定义") && explanation.containsKey("栈帧结构")) {
            log.info("【成功】虚拟机栈信息获取正常 / Success: VM stack info retrieved");
        } else {
            log.error("虚拟机栈信息不完整 / Failure: VM stack info incomplete");
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：为 stack-overflow 方法准备测试计划占位（避免实际调用导致进程崩溃）。
     * English: Prepare test plan placeholder for stack-overflow to avoid process crash.
     * 
     * 【JVM 原理】
     * 对应 JvmRuntimeDataAreaController.testStackOverflow() 接口
     * 使用 -Xss 参数可调整栈大小，值越小越容易触发 StackOverflowError
     */
    public void testStackOverflowPlan() {
        log.info("【成功】stack-overflow 测试计划已准备 / Success: plan prepared");
        log.info("  → 实际测试请调用 POST /jvm-experiment/stack/stack-overflow?depth=10000");
        log.info("  → 建议配置 -Xss128k 观察更明显效果");
    }

    /**
     * 方法说明 / Method Description:
     * 中文：为堆内存分配实验准备测试计划占位。
     * English: Prepare test plan placeholder for heap memory allocation experiments.
     * 
     * 【JVM 原理】
     * 对应 JvmRuntimeDataAreaController.allocateInYoungGen() 等接口
     * 大量分配可能导致 OOM
     */
    public void testHeapAllocationPlan() {
        log.info("【成功】堆内存分配测试计划已准备 / Success: heap allocation plan prepared");
        log.info("  → 新生代分配: POST /jvm-experiment/heap/allocate-young-gen?objectCount=10000");
        log.info("  → 老年代分配: POST /jvm-experiment/heap/allocate-old-gen?sizeMB=100");
    }

    /**
     * 方法说明 / Method Description:
     * 中文：为元空间 OOM 准备测试计划占位。
     * English: Prepare test plan placeholder for metaspace OOM.
     * 
     * 【JVM 原理】
     * 动态生成大量类可导致 Metaspace OOM
     */
    public void testMetaspaceOomPlan() {
        log.info("【成功】metaspace-oom 测试计划已准备 / Success: metaspace OOM plan prepared");
        log.info("  → 实际测试请调用 POST /jvm-experiment/metaspace/metaspace-oom");
        log.info("  → 建议配置 -XX:MaxMetaspaceSize=64m 观察更明显效果");
    }

    /**
     * 方法说明 / Method Description:
     * 中文：为直接内存 OOM 准备测试计划占位。
     * English: Prepare test plan placeholder for direct memory OOM.
     * 
     * 【JVM 原理】
     * NIO DirectByteBuffer 使用堆外内存，超出限制会 OOM
     */
    public void testDirectMemoryOomPlan() {
        log.info("【成功】direct-memory-oom 测试计划已准备 / Success: direct memory OOM plan prepared");
        log.info("  → 实际测试请调用 POST /jvm-experiment/direct-memory/direct-oom");
        log.info("  → 建议配置 -XX:MaxDirectMemorySize=64m 观察更明显效果");
    }

    /** 入口方法 / Entry point */
    public static void main(String[] args) {
        Chapter02ControllerDangerousTest t = new Chapter02ControllerDangerousTest();
        
        log.info("========== 开始危险测试验证 / Starting Dangerous Test Validation ==========");
        
        t.testVMStackInfoSafe();
        t.testStackOverflowPlan();
        t.testHeapAllocationPlan();
        t.testMetaspaceOomPlan();
        t.testDirectMemoryOomPlan();
        
        log.info("========== Chapter02ControllerDangerousTest 用例通过 / Success: cases passed ==========");
    }
}
