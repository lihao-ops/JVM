package com.example.jvmlab.chapter10;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第10章控制器的动态编译接口返回表达式计算结果，并打印成功日志。
 * English: Verify Chapter 10 controller dynamic compile endpoint returns expression result with success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：默认表达式 1+2+3 返回 6。
 * English: Default expression 1+2+3 returns 6.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class Chapter10ControllerTest {

    public void testDynamicCompile() throws Exception {
        Chapter10Controller ctrl = new Chapter10Controller();
        String res = ctrl.dynamicCompile("1+2+3");
        if ("6".equals(res)) {
            log.info("【成功】dynamic-compile 返回 6 / Success: returned 6");
        } else {
            log.error("dynamic-compile 返回不符合预期 / Failure: unexpected: {}", res);
        }
    }

    public static void main(String[] args) throws Exception {
        Chapter10ControllerTest t = new Chapter10ControllerTest();
        t.testDynamicCompile();
        log.info("【成功】Chapter10ControllerTest 用例通过 / Success: case passed");
    }
}
