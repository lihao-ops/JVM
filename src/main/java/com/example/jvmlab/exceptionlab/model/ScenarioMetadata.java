package com.example.jvmlab.exceptionlab.model;

/**
 * 用于列表视图的基础元信息，方便快速浏览所有异常实验。
 */
public class ScenarioMetadata {
    private final String id;
    private final String displayName;
    private final String exceptionType;
    private final JvmMemoryArea memoryArea;

    public ScenarioMetadata(String id, String displayName, String exceptionType, JvmMemoryArea memoryArea) {
        this.id = id;
        this.displayName = displayName;
        this.exceptionType = exceptionType;
        this.memoryArea = memoryArea;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public JvmMemoryArea getMemoryArea() {
        return memoryArea;
    }
}
