package com.example.jvmlab.common;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证全局异常处理器返回的结构化错误响应包含关键字段，并打印成功日志。
 * English: Verify global exception handler returns structured error response with key fields and print success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：返回包含 status/error/message 键。
 * English: Response contains status/error/message.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class GlobalExceptionHandlerTest {

    public void testRuntimeHandler() {
        GlobalExceptionHandler h = new GlobalExceptionHandler();
        Map<String, Object> resp = h.handleRuntime(new RuntimeException("x"));
        boolean ok = resp.containsKey("status") && resp.containsKey("error") && resp.containsKey("message");
        if (ok) {
            log.info("【成功】handleRuntime 返回结构化错误响应 / Success");
        } else {
            log.error("handleRuntime 返回缺失 / Failure: {}", resp);
        }
    }

    public void testGenericHandler() {
        GlobalExceptionHandler h = new GlobalExceptionHandler();
        Map<String, Object> resp = h.handleGeneric(new Exception("y"));
        boolean ok = resp.containsKey("status") && resp.containsKey("error") && resp.containsKey("message");
        if (ok) {
            log.info("【成功】handleGeneric 返回结构化错误响应 / Success");
        } else {
            log.error("handleGeneric 返回缺失 / Failure: {}", resp);
        }
    }

    public static void main(String[] args) {
        GlobalExceptionHandlerTest t = new GlobalExceptionHandlerTest();
        t.testRuntimeHandler();
        t.testGenericHandler();
        log.info("【成功】GlobalExceptionHandlerTest 用例通过 / Success: cases passed");
    }
}
