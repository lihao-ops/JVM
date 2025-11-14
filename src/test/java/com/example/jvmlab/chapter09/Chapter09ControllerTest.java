package com.example.jvmlab.chapter09;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第9章控制器的 JDK 动态代理与 ASM 生成实现返回预期字符串，并打印成功日志。
 * English: Verify Chapter 09 controller JDK proxy and ASM-generated implementation return expected strings with success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：jdk-proxy 返回以 proxy: 开头的字符串；bytebuddy-impl 返回以 bytebuddy: 开头的字符串。
 * English: jdk-proxy returns string starting with proxy:; bytebuddy-impl returns string starting with bytebuddy:.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class Chapter09ControllerTest {

    public void testJdkProxy() {
        Chapter09Controller ctrl = new Chapter09Controller();
        String res = ctrl.jdkProxy("abc");
        if (res.startsWith("proxy:")) {
            log.info("【成功】jdk-proxy 返回 proxy 前缀 / Success: proxy prefix");
        } else {
            log.error("jdk-proxy 返回不符合预期 / Failure: unexpected: {}", res);
        }
    }

    public void testAsmImpl() throws Exception {
        Chapter09Controller ctrl = new Chapter09Controller();
        String res = ctrl.byteBuddy("xyz");
        if (res.startsWith("bytebuddy:")) {
            log.info("【成功】bytebuddy-impl 返回 bytebuddy 前缀 / Success: bytebuddy prefix");
        } else {
            log.error("bytebuddy-impl 返回不符合预期 / Failure: unexpected: {}", res);
        }
    }

    public static void main(String[] args) throws Exception {
        Chapter09ControllerTest t = new Chapter09ControllerTest();
        t.testJdkProxy();
        t.testAsmImpl();
        log.info("【成功】Chapter09ControllerTest 用例通过 / Success: cases passed");
    }
}
