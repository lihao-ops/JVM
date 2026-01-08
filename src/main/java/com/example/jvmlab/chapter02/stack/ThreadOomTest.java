package com.example.jvmlab.chapter02.stack;

import lombok.extern.slf4j.Slf4j;

/**
 * 第二章实战：虚拟机栈 - OutOfMemoryError (unable to create new native thread)
 * <p>
 * 【对应书籍】 2.4.1 Java虚拟机栈和本地方法栈溢出
 * <p>
 * 【原理说明】
 * 1. HotSpot 的栈容量不可动态扩展。
 * 2. 但如果在创建新线程时，操作系统没有足够的内存为新线程分配栈空间，就会抛出 OOM。
 * 3. 公式：操作系统剩余内存 < (-Xss * 新线程数)
 * <p>
 * 【危险预警】
 * ⚠️ 运行此代码会导致操作系统卡死（Windows/Linux 都会）！
 * ⚠️ 因为它会耗尽系统的线程资源和内存资源。
 * ⚠️ 请在运行几秒钟后立即强制停止！
 * <p>
 * 【VM 参数建议】
 * -Xss2M (增大每个线程的栈大小，让内存更快耗尽，更容易触发 OOM)
 */
@Slf4j
public class ThreadOomTest {

    private void dontStop() {
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
        }
    }

    public void stackLeakByThread() {
        int i = 0;
        while (true) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    dontStop();
                }
            });
            thread.start();
            i++;
            // 每创建 100 个线程打印一次，防止刷屏
            if (i % 100 == 0) {
                log.info("已创建线程数: {}", i);
            }
        }
    }

    public static void main(String[] args) {
        ThreadOomTest oom = new ThreadOomTest();
        try {
            oom.stackLeakByThread();
        } catch (Throwable e) {
            log.error("--------------------------------------------------");
            log.error("【实验结果】无法创建新线程！");
            log.error("异常信息: {}", e.getMessage());
            log.error("--------------------------------------------------");
            System.exit(1); // 强制退出，释放资源
        }
    }
}
