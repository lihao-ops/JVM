package com.example.jvmlab.exceptionlab.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 类说明 / Class Description:
 * 中文：JVM 运行时内存区域枚举，提供语义化展示名称以便前端与文档直接引用。
 * English: JVM runtime memory area enum with human-friendly display names for UI and docs.
 *
 * 使用场景 / Use Cases:
 * 中文：场景元信息与详情返回体中标识内存区域分类。
 * English: Used in scenario metadata/detail responses to denote memory area categories.
 *
 * 设计目的 / Design Purpose:
 * 中文：统一术语与可视化名称，便于沟通“线程私有/线程共享”等维度。
 * English: Standardize terminology and display names for dimensions like "thread-private/shared".
 */
public enum JvmMemoryArea {
    /** 程序计数器、虚拟机栈、本地方法栈等线程私有区域。 */
    THREAD_PRIVATE("Thread-Private Area"),
    /** Java 堆，用于存放对象实例。 */
    HEAP("Java Heap"),
    /** 方法区 / 元空间，承载类元数据与常量。 */
    METASPACE("Metaspace"),
    /** 直接内存（堆外内存）。 */
    DIRECT_MEMORY("Direct Memory"),
    /** 操作系统线程资源，代表"无法创建新线程"异常场景。 */
    NATIVE_THREAD("Native Thread Resources");

    private final String displayName;

    JvmMemoryArea(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：返回语义化展示名称，便于前端直接渲染。
     * English: Return a human-friendly display name for UI rendering.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：展示名称 / English: Display name
     * 异常 / Exceptions: 无
     */
    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
