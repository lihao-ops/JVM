package com.example.jvmlab.chapter02.runtimedataareas.vmstack;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 第二章实战：StackLabController 接口测试 (直接注入模式)
 * <p>
 * 【对应书籍】 2.2.2 Java虚拟机栈
 * <p>
 * 【测试目标】
 * 1. 直接调用 Controller 方法，触发 JVM 栈行为。
 * 2. 验证 StackOverflowError 是否被正确捕获和处理。
 */
@Slf4j
@SpringBootTest
public class StackLabControllerTest {

    // 直接注入 Controller，像调用普通 Java 对象一样测试它
    @Autowired
    private StackLabController stackLabController;

    @Test
    @DisplayName("测试栈帧Debug逻辑")
    public void testDebugStack() {
        log.info("=== 开始测试 debugStack 方法 ===");
        
        // 直接调用方法
        String result = stackLabController.debugStack(3);
        
        log.info("执行结果: {}", result);
        Assertions.assertEquals("实验结束，请查看控制台日志或 IDE 调试器", result);
        log.info("=== 测试通过：递归逻辑执行正常 ===");
    }

    @Test
    @DisplayName("测试栈溢出触发逻辑")
    public void testTriggerOverflow() {
        log.info("=== 开始测试 triggerOverflow 方法 ===");
        
        // 直接调用方法，验证是否捕获了异常并返回了特定字符串
        String result = stackLabController.triggerOverflow();
        
        log.info("执行结果: {}", result);
        
        // 验证结果中是否包含"成功捕获"字样
        Assertions.assertTrue(result.contains("成功捕获栈溢出异常"), "应当捕获 StackOverflowError");
        log.info("=== 测试通过：成功捕获 StackOverflowError ===");
    }
}
