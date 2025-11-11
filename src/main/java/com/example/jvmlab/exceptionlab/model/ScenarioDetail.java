package com.example.jvmlab.exceptionlab.model;

/**
 * 返回给前端/文档的详细视图，包含操作指南。
 */
public class ScenarioDetail extends ScenarioMetadata {
    private final ScenarioGuide guide;

    public ScenarioDetail(String id, String displayName, String exceptionType, JvmMemoryArea memoryArea,
                          ScenarioGuide guide) {
        super(id, displayName, exceptionType, memoryArea);
        this.guide = guide;
    }

    public ScenarioGuide getGuide() {
        return guide;
    }
}
