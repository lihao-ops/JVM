package com.example.jvmlab.exceptionlab.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * JVM 运行时内存区域的语义化枚举。
 * <p>
 * 面试场景下，面试官通常希望候选人能够按照 "线程私有 / 线程共享" 的维度拆解内存模型，
 * 因此每个枚举值都包含一个易读的展示名称以及英文标识，方便前端或文档直接引用。
 * </p>
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
     * 返回一个友好的展示名称，便于前端直接渲染。
     */
    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
