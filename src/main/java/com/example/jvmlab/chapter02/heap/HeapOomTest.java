package com.example.jvmlab.chapter02.heap;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

/**
 * 第二章实战：堆内存溢出 (Heap OOM)
 * <p>
 * 【对应书籍】 2.4.2 Java堆溢出
 * <p>
 * 【原理说明】
 * 1. Java 堆用于存储对象实例。
 * 2. 只要不断创建对象，并且保证 GC Roots 到对象之间有可达路径（防止被 GC 回收）。
 * 3. 当对象数量达到最大堆容量限制后，就会产生 OOM。
 * <p>
 * 【VM 参数 - 必须配置！】
 * -Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError
 * (限制堆大小为 20MB，并开启 OOM 时自动生成 Dump 文件)
 */
@Slf4j
public class HeapOomTest {

    static class OOMObject {
        // 占位符，让对象稍微大一点
        private byte[] placeholder = new byte[64 * 1024]; // 64KB
    }

    public static void main(String[] args) {
        List<OOMObject> list = new ArrayList<>();
        
        log.info("开始堆内存溢出实验...");
        log.info("请确保已配置 VM 参数: -Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError");

        int count = 0;
        try {
            while (true) {
                list.add(new OOMObject());
                count++;
                // 每 50 个打印一次，避免刷屏
                if (count % 50 == 0) {
                    log.info("已创建对象数量: {}", count);
                }
            }
        } catch (OutOfMemoryError e) {
            log.error("--------------------------------------------------");
            log.error("【实验结果】发生堆内存溢出！");
            log.error("共创建对象: {}", count);
            log.error("异常信息: {}", e.getMessage());
            log.error("Dump 文件应已生成 (java_pidxxxx.hprof)");
            log.error("--------------------------------------------------");
            throw e;
        }
    }
}
