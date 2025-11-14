package com.example.jvmlab.chapter05;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第5章控制器的 CPU 热点与内存抖动接口可正常执行并打印成功日志。
 * English: Verify Chapter 05 controller CPU hotspot and memory churn endpoints execute successfully with success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：返回包含耗时或分配数量的字符串，并打印成功确认。
 * English: Return strings containing duration or allocation count with success confirmations.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志输出。
 * English: Run main method and observe logs.
 */
@Slf4j
public class Chapter05ControllerTest {

    public void testCpuHotspot() {
        Chapter05Controller ctrl = new Chapter05Controller();
        String msg = ctrl.cpuHotspot(10000);
        if (msg.contains("duration")) {
            log.info("【成功】cpu-hotspot 返回耗时信息 / Success: duration present");
        } else {
            log.error("cpu-hotspot 返回不含耗时 / Failure: duration missing: {}", msg);
        }
    }

    public void testMemoryChurn() {
        Chapter05Controller ctrl = new Chapter05Controller();
        String msg = ctrl.memoryChurn(1000);
        if (msg.contains("Allocated")) {
            log.info("【成功】memory-churn 返回分配信息 / Success: allocation info present");
        } else {
            log.error("memory-churn 返回不含分配信息 / Failure: allocation info missing: {}", msg);
        }
    }

    public static void main(String[] args) {
        Chapter05ControllerTest t = new Chapter05ControllerTest();
        t.testCpuHotspot();
        t.testMemoryChurn();
        log.info("【成功】Chapter05ControllerTest 用例通过 / Success: cases passed");
    }
}
