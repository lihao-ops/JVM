package com.example.jvmlab.chapter02;

/**
 * 第二章实战：程序计数器 (PC Register) 的模拟观察
 * <p>
 * 目标：
 * 1. 结合 javap 查看字节码指令（PC 指向的地址）。
 * 2. 使用 Debugger 模拟线程切换，观察不同线程停留在不同的代码行（PC 位置）。
 */
public class ProgramCounterTest {

    public static void main(String[] args) {
        // 线程 A：模拟“看书” (做加法)
        Thread threadA = new Thread(() -> {
            int a = 100; // 对应 bipush 100
            int b = 200; // 对应 sipush 200
            while (true) {
                int c = a + b; // 对应 iadd
                try { Thread.sleep(100); } catch (InterruptedException e) {}
            }
        }, "Thread-A-Reading");

        // 线程 B：模拟“打游戏” (做乘法)
        Thread threadB = new Thread(() -> {
            int x = 50;
            int y = 10;
            while (true) {
                int z = x * y; // 对应 imul
                try { Thread.sleep(100); } catch (InterruptedException e) {}
            }
        }, "Thread-B-Gaming");

        threadA.start();
        threadB.start();
    }
}
