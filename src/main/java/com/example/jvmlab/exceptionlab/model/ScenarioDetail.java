package com.example.jvmlab.exceptionlab.model;

/**
 * 类说明 / Class Description:
 * 中文：场景详情模型，继承基础元信息并补充操作指南。
 * English: Scenario detail model extending metadata with an operational guide.
 *
 * 使用场景 / Use Cases:
 * 中文：接口返回详细页面数据，包含原理、复现、诊断与解决方案。
 * English: API returns detailed page data with principles, reproduction, diagnostics and solutions.
 *
 * 设计目的 / Design Purpose:
 * 中文：复用元信息结构，统一对外返回格式。
 * English: Reuse metadata structure to unify external response format.
 */
public class ScenarioDetail extends ScenarioMetadata {
    private final ScenarioGuide guide;

    /**
     * 方法说明 / Method Description:
     * 中文：构造函数，初始化场景详情。
     * English: Constructor initializing scenario details.
     *
     * 参数 / Parameters:
     * @param id 中文：场景 ID / English: Scenario ID
     * @param displayName 中文：展示名称 / English: Display name
     * @param exceptionType 中文：异常类型 / English: Exception type
     * @param memoryArea 中文：内存区域 / English: Memory area
     * @param guide 中文：操作指南 / English: Operational guide
     */
    public ScenarioDetail(String id, String displayName, String exceptionType, JvmMemoryArea memoryArea,
                          ScenarioGuide guide) {
        super(id, displayName, exceptionType, memoryArea);
        this.guide = guide;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取操作指南对象。
     * English: Get the operational guide.
     */
    public ScenarioGuide getGuide() {
        return guide;
    }
}
