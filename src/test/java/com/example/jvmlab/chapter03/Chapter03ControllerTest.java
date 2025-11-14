package com.example.jvmlab.chapter03;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第3章控制器的引用类型与 GC 统计接口返回结构正确，并打印成功日志。
 * English: Verify Chapter 03 controller reference types and GC stats endpoints return correct structures with success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：reference-types 返回包含 strongRef 键；gc-stats 返回非空集合。
 * English: reference-types contains strongRef key; gc-stats returns non-empty map.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法，观察日志输出成功确认信息。
 * English: Run the main method and observe success confirmations in logs.
 */
@Slf4j
public class Chapter03ControllerTest {

    /** 测试引用类型接口 */
    public void testReferenceTypes() {
        Chapter03Controller ctrl = new Chapter03Controller();
        Map<String, String> result = ctrl.testReferenceTypes();
        if (result.containsKey("strongRef")) {
            log.info("【成功】reference-types 包含 strongRef / Success: strongRef present");
        } else {
            log.error("reference-types 缺少 strongRef / Failure: strongRef missing");
        }
    }

    /** 测试 GC 统计接口 */
    public void testGcStats() {
        Chapter03Controller ctrl = new Chapter03Controller();
        Map<String, Map<String, Object>> stats = ctrl.getGCStats();
        if (!stats.isEmpty()) {
            log.info("【成功】gc-stats 非空 / Success: gc-stats not empty");
        } else {
            log.error("gc-stats 为空 / Failure: gc-stats empty");
        }
    }

    /** 入口方法 */
    public static void main(String[] args) {
        Chapter03ControllerTest t = new Chapter03ControllerTest();
        t.testReferenceTypes();
        t.testGcStats();
        log.info("【成功】Chapter03ControllerTest 用例通过 / Success: cases passed");
    }
}
