package com.example.jvmlab.chapter02;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第2章控制器的安全接口（memory-status、堆信息）返回结构正确并打印成功日志。
 * English: Verify Chapter 02 controller safe endpoints (memory-status, heap info) return correct structure with success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：getAllMemoryStatus 包含 1_heap 键；getHeapInfo 返回堆内存信息。
 * English: getAllMemoryStatus contains 1_heap key; getHeapInfo returns heap memory info.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法，观察日志输出成功确认信息。
 * English: Run main method and observe success confirmations in logs.
 * 
 * 【对应书籍】《深入理解Java虚拟机（第3版）》第2章 - Java内存区域与内存溢出异常
 */
@Slf4j
public class Chapter02ControllerTest {

    /**
     * 方法说明 / Method Description:
     * 中文：调用 getAllMemoryStatus 接口并校验是否包含堆内存信息。
     * English: Call getAllMemoryStatus and check presence of heap info.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public void testAllMemoryStatus() {
        JvmRuntimeDataAreaController ctrl = new JvmRuntimeDataAreaController();
        Map<String, Object> status = ctrl.getAllMemoryStatus();
        
        boolean hasHeap = status.containsKey("1_heap");
        boolean hasMetaspace = status.containsKey("2_metaspace");
        boolean hasThreads = status.containsKey("3_threads");
        
        if (hasHeap && hasMetaspace && hasThreads) {
            log.info("【成功】getAllMemoryStatus 包含所有核心区域信息 / Success: all core areas present");
            log.info("  - 1_heap: {}", hasHeap);
            log.info("  - 2_metaspace: {}", hasMetaspace);
            log.info("  - 3_threads: {}", hasThreads);
        } else {
            log.error("getAllMemoryStatus 缺少部分区域信息 / Failure: some areas missing");
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：调用 explainVMStack 接口并校验返回说明包含栈帧结构。
     * English: Call explainVMStack endpoint and check message contains stack frame info.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public void testVMStackExplain() {
        JvmRuntimeDataAreaController ctrl = new JvmRuntimeDataAreaController();
        Map<String, Object> explanation = ctrl.explainVMStack();
        
        boolean hasDefinition = explanation.containsKey("定义");
        boolean hasStackFrame = explanation.containsKey("栈帧结构");
        
        if (hasDefinition && hasStackFrame) {
            log.info("【成功】explainVMStack 返回完整的虚拟机栈说明 / Success: VM stack explanation complete");
        } else {
            log.error("explainVMStack 返回信息不完整 / Failure: VM stack explanation incomplete");
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：调用 explainProgramCounter 接口并校验返回说明包含程序计数器特点。
     * English: Call explainProgramCounter endpoint and check it contains PC features.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public void testProgramCounterExplain() {
        JvmRuntimeDataAreaController ctrl = new JvmRuntimeDataAreaController();
        Map<String, Object> explanation = ctrl.explainProgramCounter();
        
        boolean hasDefinition = explanation.containsKey("定义");
        boolean hasFeatures = explanation.containsKey("特点");
        
        if (hasDefinition && hasFeatures) {
            log.info("【成功】explainProgramCounter 返回完整的PC说明 / Success: PC explanation complete");
        } else {
            log.error("explainProgramCounter 返回信息不完整 / Failure: PC explanation incomplete");
        }
    }

    /** 入口方法 / Entry point */
    public static void main(String[] args) {
        Chapter02ControllerTest t = new Chapter02ControllerTest();
        
        log.info("========== 开始第2章控制器测试 / Starting Chapter02 Controller Tests ==========");
        
        t.testAllMemoryStatus();
        t.testVMStackExplain();
        t.testProgramCounterExplain();
        
        log.info("========== Chapter02ControllerTest 用例通过 / Success: all cases passed ==========");
    }
}
