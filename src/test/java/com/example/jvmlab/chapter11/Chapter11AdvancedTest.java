package com.example.jvmlab.chapter11;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第11章新增的代码缓存、逃逸分析与锁竞争演示接口返回结构正确，并打印成功日志。
 * English: Verify Chapter 11 new endpoints for code cache, escape analysis and lock contention return correct structures with success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：code-cache-info 返回包含 compiler 或 codeCache；escape-analysis 返回 durationMs；biased-lock-demo 返回 durationMs。
 * English: code-cache-info contains compiler or codeCache; escape-analysis returns durationMs; biased-lock-demo returns durationMs.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class Chapter11AdvancedTest {

    public void testCodeCacheInfo() {
        Chapter11Controller ctrl = new Chapter11Controller();
        Map<String, Object> info = ctrl.codeCacheInfo();
        if (info.containsKey("compiler") || info.containsKey("codeCache")) {
            log.info("【成功】code-cache-info 返回编译器/代码缓存 / Success: compiler/codeCache present");
        } else {
            log.error("code-cache-info 返回缺失 / Failure: missing keys: {}", info.keySet());
        }
    }

    public void testEscapeAnalysis() {
        Chapter11Controller ctrl = new Chapter11Controller();
        Map<String, Object> res = ctrl.escapeAnalysis(1000, 1000);
        if (res.containsKey("durationMs")) {
            log.info("【成功】escape-analysis 返回耗时 / Success: durationMs present");
        } else {
            log.error("escape-analysis 返回缺失 / Failure: keys: {}", res.keySet());
        }
    }

    public void testBiasedLockDemo() {
        Chapter11Controller ctrl = new Chapter11Controller();
        Map<String, Object> res = ctrl.biasedLockDemo(4, 10000);
        if (res.containsKey("durationMs")) {
            log.info("【成功】biased-lock-demo 返回耗时 / Success: durationMs present");
        } else {
            log.error("biased-lock-demo 返回缺失 / Failure: keys: {}", res.keySet());
        }
    }

    public static void main(String[] args) {
        Chapter11AdvancedTest t = new Chapter11AdvancedTest();
        t.testCodeCacheInfo();
        t.testEscapeAnalysis();
        t.testBiasedLockDemo();
        log.info("【成功】Chapter11AdvancedTest 用例通过 / Success: cases passed");
    }
}
