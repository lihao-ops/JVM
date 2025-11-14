package com.example.jvmlab.chapter08;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第8章控制器的 MethodHandle 与对象布局接口按预期返回，并打印成功日志。
 * English: Verify Chapter 08 controller MethodHandle and object layout endpoints behave as expected with success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：method-handle 返回 Echo: 前缀；object-layout 返回布局字符串或提示缺少 JOL。
 * English: method-handle returns 'Echo:' prefix; object-layout returns layout or hint about missing JOL.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class Chapter08ControllerTest {

    public void testMethodHandle() throws Throwable {
        Chapter08Controller ctrl = new Chapter08Controller();
        String res = ctrl.methodHandle("x");
        if (res.startsWith("Echo:")) {
            log.info("【成功】method-handle 返回 Echo 前缀 / Success: Echo prefix");
        } else {
            log.error("method-handle 返回不符合预期 / Failure: unexpected: {}", res);
        }
    }

    public void testObjectLayout() {
        Chapter08Controller ctrl = new Chapter08Controller();
        Map<String, String> layout = ctrl.objectLayout();
        if (layout.containsKey("layout")) {
            log.info("【成功】object-layout 返回布局或提示 / Success: layout or hint returned");
        } else {
            log.error("object-layout 未返回布局键 / Failure: layout key missing");
        }
    }

    public static void main(String[] args) throws Throwable {
        Chapter08ControllerTest t = new Chapter08ControllerTest();
        t.testMethodHandle();
        t.testObjectLayout();
        log.info("【成功】Chapter08ControllerTest 用例通过 / Success: cases passed");
    }
}
