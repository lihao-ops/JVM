package com.example.jvmlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 类说明 / Class Description:
 * 中文：JVM 学习实验室的应用主入口，启动 Spring Boot 容器，装配所有章节与异常实验的 REST 接口。
 * English: Application entry point for the JVM Learning Lab. Boots Spring Boot and wires all chapter and exception lab REST endpoints.
 *
 * 使用场景 / Use Cases:
 * 中文：本项目作为学习与面试演示环境，用于快速启动并访问各类 JVM 行为复现与监控接口。
 * English: Used to start the lab environment for learning and interview demos, enabling access to JVM experiments and monitoring endpoints.
 *
 * 设计目的 / Design Purpose:
 * 中文：统一应用启动与包扫描范围，确保所有控制器、服务与工具类被正确注册，便于集中管理和扩展。
 * English: Centralize bootstrapping and package scanning to ensure controllers, services, and utilities are registered, simplifying management and extension.
 *
 * 涉及的核心组件说明 / Core Components:
 * 中文：SpringBootApplication 注解（包扫描）、SpringApplication（容器启动）。
 * English: SpringBootApplication (package scan) and SpringApplication (container bootstrap).
 */
@SpringBootApplication(scanBasePackages = {"com.example.jvmlab", "com.example.jvmstress"})
public class JvmLearningApplication {

    /**
     * 方法说明 / Method Description:
     * 中文：应用主入口，启动 Spring Boot 容器并初始化所有实验与监控端点。
     * English: Main entry method that starts the Spring Boot container and initializes all experiment and monitoring endpoints.
     *
     * 参数 / Parameters:
     * @param args 中文说明：命令行参数，用于自定义启动行为 / English: Command-line arguments for customizing boot behavior
     *
     * 返回值 / Return:
     * 中文：无返回值，方法完成后应用处于运行状态 / English: No return value; the application keeps running after start
     *
     * 异常 / Exceptions:
     * 中文：底层启动过程可能抛出运行时异常（如端口占用、配置错误） / English: Runtime exceptions may occur during startup (e.g., port conflict, misconfiguration)
     */
    public static void main(String[] args) {
        // 中文：启动 Spring Boot 应用，容器装配所有控制器与服务
        // English: Start the Spring Boot app; the container wires all controllers and services
        SpringApplication.run(JvmLearningApplication.class, args);
    }
}
