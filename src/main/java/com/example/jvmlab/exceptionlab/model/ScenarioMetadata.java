package com.example.jvmlab.exceptionlab.model;

/**
 * 类说明 / Class Description:
 * 中文：异常场景的基础元信息模型，用于列表视图展示 ID、名称、异常类型与内存区域。
 * English: Basic metadata model for exception scenarios, exposing ID, display name, exception type, and memory area for listing views.
 *
 * 使用场景 / Use Cases:
 * 中文：前端导航与目录页渲染、服务层排序输出。
 * English: UI navigation rendering and sorted output from service layer.
 *
 * 设计目的 / Design Purpose:
 * 中文：只读数据承载，简化序列化与展示；提供必要的访问器。
 * English: Read-only data holder simplifying serialization and display; provides necessary accessors.
 */
public class ScenarioMetadata {
    private final String id;
    private final String displayName;
    private final String exceptionType;
    private final JvmMemoryArea memoryArea;

    /**
     * 方法说明 / Method Description:
     * 中文：构造函数，初始化场景元信息。
     * English: Constructor initializing scenario metadata fields.
     *
     * 参数 / Parameters:
     * @param id 中文：场景 ID / English: Scenario ID
     * @param displayName 中文：展示名称 / English: Display name
     * @param exceptionType 中文：异常类型 / English: Exception type
     * @param memoryArea 中文：内存区域 / English: Memory area
     *
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public ScenarioMetadata(String id, String displayName, String exceptionType, JvmMemoryArea memoryArea) {
        this.id = id;
        this.displayName = displayName;
        this.exceptionType = exceptionType;
        this.memoryArea = memoryArea;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取场景 ID。
     * English: Get scenario ID.
     */
    public String getId() {
        return id;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取展示名称。
     * English: Get display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取异常类型字符串。
     * English: Get exception type string.
     */
    public String getExceptionType() {
        return exceptionType;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取关联的 JVM 内存区域。
     * English: Get associated JVM memory area.
     */
    public JvmMemoryArea getMemoryArea() {
        return memoryArea;
    }
}
