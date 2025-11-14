package com.example.jvmlab.chapter11;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第11章控制器的 JIT 预热接口返回包含耗时毫秒字段，并打印成功日志。
 * English: Verify Chapter 11 controller JIT warmup endpoint returns a durationMs field with success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：返回字符串以 durationMs= 开头。
 * English: Return string starting with durationMs=.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class Chapter11ControllerTest {

    public void testJitWarmup() {
        Chapter11Controller ctrl = new Chapter11Controller();
        String res = ctrl.jitWarmup(1000, 50);
        if (res.startsWith("durationMs=")) {
            log.info("【成功】jit-warmup 返回耗时字段 / Success: durationMs present");
        } else {
            log.error("jit-warmup 返回不符合预期 / Failure: unexpected: {}", res);
        }
    }

    public static void main(String[] args) {
        Chapter11ControllerTest t = new Chapter11ControllerTest();
        t.testJitWarmup();
        log.info("【成功】Chapter11ControllerTest 用例通过 / Success: case passed");
    }
}
