package com.example.jvmlab.chapter02.stack;

/**
 * 第二章实战：虚拟机栈 - StackOverflowError 演示
 * <p>
 * 【对应书籍】 2.4.1 Java虚拟机栈和本地方法栈溢出
 * <p>
 * 【原理说明】
 * 1. 线程请求的栈深度 > 虚拟机所允许的深度。
 * 2. 每次方法调用都会创建一个“栈帧”(Stack Frame) 入栈。
 * 3. 如果无限递归，栈帧只进不出，最终会把栈空间（-Xss）撑爆。
 * <p>
 * 【面试考点】
 * 问：StackOverflowError 是内存溢出吗？
 * 答：严格来说是“栈溢出”。在 HotSpot 中，栈容量不可动态扩展，所以通常是因为递归过深导致的。
 *     它只会导致当前线程崩溃，不会影响整个 JVM 进程（除非是主线程）。
 * <p>
 * 【VM 参数建议】
 * -Xss160k (减小栈容量，让错误更快发生)
 */
public class StackOverflowTest {

    private int stackLength = 1;

    public void stackLeak() {
        stackLength++;
        // 无限递归，不断压入栈帧
        stackLeak();
    }

    public static void main(String[] args) {
        StackOverflowTest test = new StackOverflowTest();
        try {
            test.stackLeak();
        } catch (Throwable e) {
            System.out.println("--------------------------------------------------");
            System.out.println("【实验结果】发生栈溢出！");
            System.out.println("当前栈深度: " + test.stackLength);
            System.out.println("异常类型: " + e.getClass().getName());
            System.out.println("--------------------------------------------------");
            // e.printStackTrace(); // 堆栈太长，建议注释掉，否则控制台刷屏
        }
    }
}
