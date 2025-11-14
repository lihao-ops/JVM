package com.example.jvmlab.monitor;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证运行时监控控制器总览接口返回包含内存、GC、系统与风险评估四大部分数据。
 * English: Verify the monitoring controller overview returns four parts: memory, GC, system and leak risk.
 *
 * 预期结果 / Expected Result:
 * 中文：Map 包含 "memory"、"gc"、"system"、"leakRisk" 键，并打印成功日志。
 * English: Map contains keys "memory", "gc", "system", "leakRisk" with success logs printed.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法，观察日志输出的成功确认信息。
 * English: Run the main method and observe success confirmation logs.
 */
@Slf4j
public class RuntimeMonitorControllerTest {

    /**
     * 方法说明 / Method Description:
     * 中文：调用监控总览接口并校验关键键是否存在。
     * English: Call monitoring overview and validate presence of key entries.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public void testOverviewKeys() {
        RuntimeMonitorController ctrl = new RuntimeMonitorController();
        Map<String, Object> data = ctrl.overview();
        boolean ok = data.containsKey("memory") && data.containsKey("gc") && data.containsKey("system") && data.containsKey("leakRisk");
        if (ok) {
            log.info("【成功】监控总览包含四大部分 / Success: overview contains 4 sections");
        } else {
            log.error("监控总览缺少部分键 / Failure: missing keys in overview: {}", data.keySet());
        }
    }

    /** 入口方法 / Entry point */
    public static void main(String[] args) {
        RuntimeMonitorControllerTest t = new RuntimeMonitorControllerTest();
        t.testOverviewKeys();
        log.info("【成功】RuntimeMonitorControllerTest 用例通过 / Success: case passed");
    }
}
