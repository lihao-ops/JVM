package com.example.jvmlab.exceptionlab.model;

import java.util.Collections;
import java.util.List;

/**
 * 类说明 / Class Description:
 * 中文：场景指导手册模型，包含原理、复现步骤、诊断步骤、解决步骤、推荐 JVM 参数与工具提示。
 * English: Scenario guide model containing principles, reproduction steps, diagnostics, solutions, recommended JVM options and tooling tips.
 *
 * 使用场景 / Use Cases:
 * 中文：控制器返回详细页面所需的结构化内容，便于教学与面试演示。
 * English: Structured content for detailed pages returned by controllers for teaching and interview demos.
 *
 * 设计目的 / Design Purpose:
 * 中文：不可变对象，保障并发访问安全，配套构建器简化创建流程。
 * English: Immutable object for safe concurrent access with a builder to simplify creation.
 */
public final class ScenarioGuide {

    private final String principle;
    private final List<String> reproductionSteps;
    private final List<String> diagnosticSteps;
    private final List<String> solutionSteps;
    private final List<String> recommendedJvmOptions;
    private final List<String> toolingTips;

    /**
     * 方法说明 / Method Description:
     * 中文：私有构造函数，仅供 Builder 使用，复制构建器中的字段。
     * English: Private constructor used by Builder, copying fields from the builder.
     *
     * 参数 / Parameters:
     * @param builder 中文：构建器实例 / English: Builder instance
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    private ScenarioGuide(Builder builder) {
        this.principle = builder.principle;
        this.reproductionSteps = List.copyOf(builder.reproductionSteps);
        this.diagnosticSteps = List.copyOf(builder.diagnosticSteps);
        this.solutionSteps = List.copyOf(builder.solutionSteps);
        this.recommendedJvmOptions = List.copyOf(builder.recommendedJvmOptions);
        this.toolingTips = List.copyOf(builder.toolingTips);
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取场景原理文本。
     * English: Get principle text.
     */
    public String getPrinciple() {
        return principle;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取复现步骤列表。
     * English: Get reproduction steps.
     */
    public List<String> getReproductionSteps() {
        return reproductionSteps;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取诊断步骤列表。
     * English: Get diagnostic steps.
     */
    public List<String> getDiagnosticSteps() {
        return diagnosticSteps;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取解决步骤列表。
     * English: Get solution steps.
     */
    public List<String> getSolutionSteps() {
        return solutionSteps;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取推荐的 JVM 参数集合。
     * English: Get recommended JVM options.
     */
    public List<String> getRecommendedJvmOptions() {
        return recommendedJvmOptions;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：获取工具提示列表。
     * English: Get tooling tips.
     */
    public List<String> getToolingTips() {
        return toolingTips;
    }

    /**
     * 类说明 / Class Description:
     * 中文：构建器类，分步设置字段并最终构建不可变的 ScenarioGuide 实例。
     * English: Builder class that sets fields step-by-step and builds an immutable ScenarioGuide instance.
     *
     * 使用场景 / Use Cases:
     * 中文：用于在控制器或服务层灵活拼装指导手册内容。
     * English: Used in controllers or services to flexibly assemble guide content.
     *
     * 设计目的 / Design Purpose:
     * 中文：避免长参数列表，提升可读性与可维护性。
     * English: Avoid long parameter lists to improve readability and maintainability.
     */
    public static class Builder {
        private String principle = "";
        private List<String> reproductionSteps = Collections.emptyList();
        private List<String> diagnosticSteps = Collections.emptyList();
        private List<String> solutionSteps = Collections.emptyList();
        private List<String> recommendedJvmOptions = Collections.emptyList();
        private List<String> toolingTips = Collections.emptyList();

        /**
         * 方法说明 / Method Description:
         * 中文：设置原理描述。
         * English: Set the principle description.
         *
         * 参数 / Parameters:
         * @param principle 中文：原理文本 / English: Principle text
         * 返回值 / Return: 中文：构建器自身 / English: This builder
         * 异常 / Exceptions: 无
         */
        public Builder principle(String principle) {
            this.principle = principle;
            return this;
        }

        /**
         * 方法说明 / Method Description:
         * 中文：设置复现步骤列表。
         * English: Set reproduction steps.
         */
        public Builder reproductionSteps(List<String> reproductionSteps) {
            this.reproductionSteps = reproductionSteps;
            return this;
        }

        /**
         * 方法说明 / Method Description:
         * 中文：设置诊断步骤列表。
         * English: Set diagnostic steps.
         */
        public Builder diagnosticSteps(List<String> diagnosticSteps) {
            this.diagnosticSteps = diagnosticSteps;
            return this;
        }

        /**
         * 方法说明 / Method Description:
         * 中文：设置解决步骤列表。
         * English: Set solution steps.
         */
        public Builder solutionSteps(List<String> solutionSteps) {
            this.solutionSteps = solutionSteps;
            return this;
        }

        /**
         * 方法说明 / Method Description:
         * 中文：设置推荐 JVM 参数。
         * English: Set recommended JVM options.
         */
        public Builder recommendedJvmOptions(List<String> recommendedJvmOptions) {
            this.recommendedJvmOptions = recommendedJvmOptions;
            return this;
        }

        /**
         * 方法说明 / Method Description:
         * 中文：设置工具提示列表。
         * English: Set tooling tips.
         */
        public Builder toolingTips(List<String> toolingTips) {
            this.toolingTips = toolingTips;
            return this;
        }

        /**
         * 方法说明 / Method Description:
         * 中文：构建不可变的 ScenarioGuide 实例。
         * English: Build an immutable ScenarioGuide instance.
         *
         * 参数 / Parameters: 无
         * 返回值 / Return: 中文：ScenarioGuide 实例 / English: ScenarioGuide instance
         * 异常 / Exceptions: 无
         */
        public ScenarioGuide build() {
            return new ScenarioGuide(this);
        }
    }
}
