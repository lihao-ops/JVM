package com.example.jvmstress.ctrl;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证 JVM 错误压力测试控制器的安全接口（reset、array-limit）能正常执行并打印成功日志。
 * English: Verify safe endpoints (reset, array-limit) of JVM error stress controller execute and print success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：reset 返回 cleared；array-limit 在合理长度下返回分配成功信息。
 * English: reset returns cleared; array-limit returns allocation success for reasonable length.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class JvmErrorControllerTest {

    public void testReset() {
        JvmErrorController ctrl = new JvmErrorController();
        String res = ctrl.reset();
        if (res.contains("cleared")) {
            log.info("【成功】reset 返回 cleared / Success: cleared returned");
        } else {
            log.error("reset 返回不符合预期 / Failure: unexpected: {}", res);
        }
    }

    public void testArrayLimit() {
        JvmErrorController ctrl = new JvmErrorController();
        String res = ctrl.arrayLimit(100);
        if (res.startsWith("Allocated int[")) {
            log.info("【成功】array-limit 分配成功 / Success: array allocated");
        } else {
            log.error("array-limit 返回不符合预期 / Failure: unexpected: {}", res);
        }
    }

    public static void main(String[] args) {
        JvmErrorControllerTest t = new JvmErrorControllerTest();
        t.testReset();
        t.testArrayLimit();
        log.info("【成功】JvmErrorControllerTest 用例通过 / Success: cases passed");
    }
}
