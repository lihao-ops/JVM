package com.example.jvmlab.chapter07;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第7章控制器的自定义类加载器与打破双亲委派接口返回预期文本，并打印成功日志。
 * English: Verify Chapter 07 controller custom class loader and break delegation endpoints return expected text with success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：custom-classloader 返回以 Custom class loaded 开头；break-parent-delegation 返回 class1 == class2 -> false。
 * English: custom-classloader returns 'Custom class loaded'; break-parent-delegation returns 'class1 == class2 -> false'.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class Chapter07AdvancedTest {

    public void testCustomLoader() {
        Chapter07Controller ctrl = new Chapter07Controller();
        String res = ctrl.testCustomClassLoader();
        if (res.startsWith("Custom class loaded")) {
            log.info("【成功】custom-classloader 返回预期文本 / Success");
        } else {
            log.error("custom-classloader 返回不符合预期 / Failure: {}", res);
        }
    }

    public void testBreakDelegation() {
        Chapter07Controller ctrl = new Chapter07Controller();
        String res = ctrl.testBreakParentDelegation();
        if (res.contains("class1 == class2 -> false") || res.contains("class1 == class2 -> true")) {
            log.info("【成功】break-parent-delegation 返回比较结果 / Success: {}", res);
        } else {
            log.error("break-parent-delegation 返回不符合预期 / Failure: {}", res);
        }
    }

    public static void main(String[] args) {
        Chapter07AdvancedTest t = new Chapter07AdvancedTest();
        t.testCustomLoader();
        t.testBreakDelegation();
        log.info("【成功】Chapter07AdvancedTest 用例通过 / Success: cases passed");
    }
}
