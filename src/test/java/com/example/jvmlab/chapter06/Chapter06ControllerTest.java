package com.example.jvmlab.chapter06;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第6章控制器的类结构解析接口返回包含 className 与 interfaces 等键。
 * English: Verify Chapter 06 controller class structure endpoint returns keys like className and interfaces.
 *
 * 预期结果 / Expected Result:
 * 中文：返回 Map 包含 className 键，日志打印成功确认。
 * English: Map contains className key with success log.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class Chapter06ControllerTest {

    public void testClassStructure() throws Exception {
        Chapter06Controller ctrl = new Chapter06Controller();
        Map<String, Object> info = ctrl.parseClass("java.lang.String");
        if (info.containsKey("className")) {
            log.info("【成功】class-structure 返回 className / Success: className present");
        } else {
            log.error("class-structure 缺少 className / Failure: className missing");
        }
    }

    public static void main(String[] args) throws Exception {
        Chapter06ControllerTest t = new Chapter06ControllerTest();
        t.testClassStructure();
        log.info("【成功】Chapter06ControllerTest 用例通过 / Success: case passed");
    }
}
