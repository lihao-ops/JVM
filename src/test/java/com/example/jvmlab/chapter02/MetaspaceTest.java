package com.example.jvmlab.chapter02;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 * JVM参数设置：
 * -XX:MetaspaceSize=64M
 * -XX:MaxMetaspaceSize=128M
 * -XX:+PrintGCDetails
 */
// 实验目标：观察类加载、元空间大小变化
@SpringBootTest
class MetaspaceTest {

    private static final Logger logger = LoggerFactory.getLogger(MetaspaceTest.class);

    // 获取元空间的当前内存使用情况
    @Test
    void testMetaspaceSize() {
        // 获取内存MXBean
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage metaspaceUsage = memoryMXBean.getNonHeapMemoryUsage();

        // 输出元空间初始、已使用、最大值
        logger.info("初始元空间大小: {} MB", metaspaceUsage.getInit() / 1024 / 1024);
        logger.info("已使用: {} MB", metaspaceUsage.getUsed() / 1024 / 1024);
        logger.info("最大值: {} MB", metaspaceUsage.getMax() / 1024 / 1024);
    }

    // 动态加载大量类，观察元空间变化
    @Test
    void testClassLoading() throws Exception {
        // 获取类加载MXBean
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();

        logger.info("初始加载类数量: {}", classLoadingMXBean.getLoadedClassCount());

        // 模拟类加载，观察元空间变化
        List<Class<?>> classList = new ArrayList<>();
        int counter = 0;

        // 循环加载类，直到元空间触发扩展
        while (true) {
            // 动态加载一个类
            classList.add(Class.forName("com.example.jvmlab.chapter02.MetaspaceTest"));
            counter++;
            if (counter % 1000 == 0) {
                logger.info("已加载 {} 个类", counter);
            }

            // 每加载一定数量的类，查看元空间状态
            if (counter % 5000 == 0) {
                logger.info("加载到 {} 类时的元空间状态：", counter);
                testMetaspaceSize();  // 输出元空间使用情况
            }
        }
    }
}
