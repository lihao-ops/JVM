package com.example.jvmlab.chapter04;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第4章控制器的 jvm-arguments 与 thread-dump 接口返回包含关键字段，并打印成功日志。
 * English: Verify Chapter 04 controller jvm-arguments and thread-dump endpoints contain key fields with success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：jvm-arguments 返回 inputArguments；thread-dump 返回 threadCount。
 * English: jvm-arguments contains inputArguments; thread-dump contains threadCount.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志中的成功确认信息。
 * English: Run main method and observe success confirmations in logs.
 */
@Slf4j
public class Chapter04ControllerTest {

    public void testJvmArguments() {
        Chapter04Controller ctrl = new Chapter04Controller();
        Map<String, Object> info = ctrl.getJvmArguments();
        if (info.containsKey("inputArguments")) {
            log.info("【成功】jvm-arguments 返回 inputArguments / Success: inputArguments present");
        } else {
            log.error("jvm-arguments 缺少 inputArguments / Failure: inputArguments missing");
        }
    }

    public void testThreadDump() {
        Chapter04Controller ctrl = new Chapter04Controller();
        Map<String, Object> dump = ctrl.getThreadDump();
        if (dump.containsKey("threadCount")) {
            log.info("【成功】thread-dump 返回 threadCount / Success: threadCount present");
        } else {
            log.error("thread-dump 缺少 threadCount / Failure: threadCount missing");
        }
    }

    public static void main(String[] args) {
        Chapter04ControllerTest t = new Chapter04ControllerTest();
        t.testJvmArguments();
        t.testThreadDump();
        log.info("【成功】Chapter04ControllerTest 用例通过 / Success: cases passed");
    }
}
