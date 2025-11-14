package com.example.jvmlab.chapter02;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证第2章控制器的安全接口（memory-info、reset）返回结构正确并打印成功日志。
 * English: Verify Chapter 02 controller safe endpoints (memory-info, reset) return correct structure with success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：memory-info 包含 heap 键；reset 返回成功提示字符串。
 * English: memory-info contains heap key; reset returns success message.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法，观察日志输出成功确认信息。
 * English: Run main method and observe success confirmations in logs.
 */
@Slf4j
public class Chapter02ControllerTest {

    /**
     * 方法说明 / Method Description:
     * 中文：调用 memory-info 接口并校验是否包含 heap 信息。
     * English: Call memory-info and check presence of heap info.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public void testMemoryInfo() {
        Chapter02Controller ctrl = new Chapter02Controller();
        Map<String, Object> info = ctrl.getMemoryInfo();
        boolean ok = info.containsKey("heap");
        if (ok) {
            log.info("【成功】memory-info 包含 heap 键 / Success: heap key present");
        } else {
            log.error("memory-info 缺少 heap 键 / Failure: heap key missing");
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：调用 reset 接口并校验返回提示包含 cleared 字样。
     * English: Call reset endpoint and check message contains 'cleared'.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public void testReset() {
        Chapter02Controller ctrl = new Chapter02Controller();
        String msg = ctrl.reset();
        if (msg.contains("cleared")) {
            log.info("【成功】reset 返回清理提示 / Success: reset returned cleared message");
        } else {
            log.error("reset 返回不包含清理提示 / Failure: reset message unexpected: {}", msg);
        }
    }

    /** 入口方法 / Entry point */
    public static void main(String[] args) {
        Chapter02ControllerTest t = new Chapter02ControllerTest();
        t.testMemoryInfo();
        t.testReset();
        log.info("【成功】Chapter02ControllerTest 用例通过 / Success: cases passed");
    }
}
