package com.example.jvmlab.chapter08;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第8章 invokedynamic 接口返回以 Echo: 前缀的字符串，并打印成功日志。
 * English: Verify Chapter 8 invokedynamic endpoint returns a string starting with Echo: and print success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：返回以 Echo: 开头的字符串。
 * English: Return starts with Echo:.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class Chapter08InvokeDynamicTest {

    public void testInvokeDynamic() throws Throwable {
        Chapter08Controller ctrl = new Chapter08Controller();
        String res = ctrl.invokeDynamic("idyn");
        if (res.startsWith("Echo:")) {
            log.info("【成功】invoke-dynamic 返回 Echo 前缀 / Success: Echo prefix");
        } else {
            log.error("invoke-dynamic 返回不符合预期 / Failure: unexpected: {}", res);
        }
    }

    public static void main(String[] args) throws Throwable {
        Chapter08InvokeDynamicTest t = new Chapter08InvokeDynamicTest();
        t.testInvokeDynamic();
        log.info("【成功】Chapter08InvokeDynamicTest 用例通过 / Success: case passed");
    }
}
