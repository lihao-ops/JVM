package com.example.jvmlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * JVM Learning Lab 应用启动类。
 * <p>
 * 实现思路：
 * 1. 基于Spring Boot 3.5.3提供RESTful接口，便于触发和观察各种JVM实验。
 * 2. 通过Spring框架管理控制器、监控组件和工具类，快速组织《深入理解Java虚拟机》的学习案例。
 * 3. 作为面试准备项目，保持结构清晰、注释详细，帮助学习者迅速定位实验入口。
 * </p>
 */
@SpringBootApplication
public class JvmLearningApplication {

    /**
     * 应用主入口：启动Spring Boot容器并输出启动日志。
     *
     * @param args 命令行参数。
     */
    public static void main(String[] args) {
        // 启动Spring Boot应用，构建完整的JVM学习实验环境。
        SpringApplication.run(JvmLearningApplication.class, args);
    }
}
