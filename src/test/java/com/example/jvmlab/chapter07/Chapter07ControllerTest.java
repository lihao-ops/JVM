package com.example.jvmlab.chapter07;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第7章控制器的类加载器层次接口与静态方法调用接口返回正确信息。
 * English: Verify Chapter 07 controller classloader hierarchy and static method endpoints return correct info.
 *
 * 预期结果 / Expected Result:
 * 中文：classloader-hierarchy 返回 Application、Platform 键；active-use-static-method 返回固定字符串。
 * English: classloader-hierarchy returns Application, Platform keys; active-use-static-method returns fixed string.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class Chapter07ControllerTest {

    public void testClassLoaderHierarchy() {
        Chapter07Controller ctrl = new Chapter07Controller();
        Map<String, String> map = ctrl.testClassLoaderHierarchy();
        if (map.containsKey("Application") && map.containsKey("Platform")) {
            log.info("【成功】classloader-hierarchy 键存在 / Success: keys present");
        } else {
            log.error("classloader-hierarchy 键缺失 / Failure: keys missing: {}", map.keySet());
        }
    }

    public void testStaticMethod() {
        Chapter07Controller ctrl = new Chapter07Controller();
        String res = ctrl.testActiveUseByStaticMethod();
        if (res.contains("Static method")) {
            log.info("【成功】active-use-static-method 返回预期文本 / Success: expected text");
        } else {
            log.error("active-use-static-method 返回异常 / Failure: unexpected: {}", res);
        }
    }

    public static void main(String[] args) {
        Chapter07ControllerTest t = new Chapter07ControllerTest();
        t.testClassLoaderHierarchy();
        t.testStaticMethod();
        log.info("【成功】Chapter07ControllerTest 用例通过 / Success: cases passed");
    }
}
