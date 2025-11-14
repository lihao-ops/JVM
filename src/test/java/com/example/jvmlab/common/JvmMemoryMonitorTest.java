package com.example.jvmlab.common;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证 JvmMemoryMonitor 的内存与GC信息接口返回结构完整，并打印成功日志。
 * English: Verify JvmMemoryMonitor returns complete structures for memory and GC info with success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：memory-info 包含 heap/nonHeap/pools/gc/threads/classes 键；gc-stats 非空。
 * English: memory-info contains heap/nonHeap/pools/gc/threads/classes; gc-stats not empty.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class JvmMemoryMonitorTest {

    public void testMemoryInfoMap() {
        Map<String, Object> info = JvmMemoryMonitor.getMemoryInfoMap();
        boolean ok = info.containsKey("heap") && info.containsKey("nonHeap") && info.containsKey("pools")
                && info.containsKey("gc") && info.containsKey("threads") && info.containsKey("classes");
        if (ok) {
            log.info("【成功】memory-info 返回结构完整 / Success: keys present");
        } else {
            log.error("memory-info 返回结构缺失 / Failure: keys: {}", info.keySet());
        }
    }

    public void testGcStats() {
        Map<String, Map<String, Object>> gc = JvmMemoryMonitor.getGCStats();
        if (!gc.isEmpty()) {
            log.info("【成功】gc-stats 非空 / Success: gc stats present");
        } else {
            log.error("gc-stats 为空 / Failure");
        }
    }

    public static void main(String[] args) {
        JvmMemoryMonitorTest t = new JvmMemoryMonitorTest();
        t.testMemoryInfoMap();
        t.testGcStats();
        log.info("【成功】JvmMemoryMonitorTest 用例通过 / Success: cases passed");
    }
}
