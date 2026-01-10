package com.example.jvmlab.chapter02.runtimedataareas.vmstack;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 第二章实战：StackLabService 业务逻辑测试
 * <p>
 * 【对应书籍】 2.2.2 Java虚拟机栈
 * <p>
 * 【测试目标】
 * 直接测试 Service 中的 JVM 实验逻辑，验证栈溢出和线程创建行为。
 */
@Slf4j
@SpringBootTest
public class StackLabServiceTest {

    @Autowired
    private StackLabService stackLabService;

    @Test
    @DisplayName("测试：Debug观察栈帧入栈出栈")
    public void testDebugStack() {
        System.out.println("\n==================================================");
        System.out.println(">>> Service测试: 栈帧Debug (Recursive Dive)");
        System.out.println("==================================================");
        
        stackLabService.resetCounter();
        // 递归 3 层，观察日志
        stackLabService.recursiveDive(3);
        
        System.out.println("<<< 递归结束，请检查上方日志中的 Push/Pop 过程");
        System.out.println("==================================================\n");
    }

    @Test
    @DisplayName("测试：普通栈溢出")
    public void testInfiniteDive() {
        System.out.println("\n==================================================");
        System.out.println(">>> Service测试: 普通栈溢出 (Infinite Dive)");
        System.out.println("==================================================");
        
        stackLabService.resetCounter();
        try {
            stackLabService.infiniteDive();
        } catch (StackOverflowError e) {
            int depth = stackLabService.getCurrentDepth();
            System.out.println("<<< 成功捕获 StackOverflowError！");
            System.out.println("<<< 最终深度: " + depth);
        }
        System.out.println("==================================================\n");
    }

    @Test
    @DisplayName("测试：臃肿栈帧溢出")
    public void testBloatedStackFrame() {
        System.out.println("\n==================================================");
        System.out.println(">>> Service测试: 臃肿栈帧溢出 (Bloated Stack Frame)");
        System.out.println("==================================================");
        
        stackLabService.resetCounter();
        try {
            stackLabService.recursionWithBloatedStackFrame(1);
        } catch (StackOverflowError e) {
            int depth = stackLabService.getCurrentDepth();
            System.out.println("<<< 成功捕获 StackOverflowError！");
            System.out.println("<<< 最终深度: " + depth);
            System.out.println("<<< (预期：此深度应显著小于普通栈溢出)");
        }
        System.out.println("==================================================\n");
    }

    @Test
    @DisplayName("测试：高并发线程创建")
    public void testConcurrentConnections() {
        System.out.println("\n==================================================");
        System.out.println(">>> Service测试: 高并发线程创建");
        System.out.println("==================================================");
        
        try {
            // 尝试创建 5000 个线程
            stackLabService.simulateConcurrentConnections(5000);
            System.out.println("<<< 成功创建 5000 个线程");
        } catch (OutOfMemoryError e) {
            System.out.println("<<< 发生 OOM: " + e.getMessage());
        }
        System.out.println("==================================================\n");
    }
}
