package com.example.jvmlab.exceptionlab.model;

import java.util.Collections;
import java.util.List;

/**
 * 面试官式指导手册，串联「原理 → 复现 → 工具 → 解决方案」。
 * <p>
 * 该模型直接映射为文档/接口返回的 JSON，帮助候选人快速按步骤操作。
 * </p>
 */
public final class ScenarioGuide {

    private final String principle;
    private final List<String> reproductionSteps;
    private final List<String> diagnosticSteps;
    private final List<String> solutionSteps;
    private final List<String> recommendedJvmOptions;
    private final List<String> toolingTips;

    private ScenarioGuide(Builder builder) {
        this.principle = builder.principle;
        this.reproductionSteps = List.copyOf(builder.reproductionSteps);
        this.diagnosticSteps = List.copyOf(builder.diagnosticSteps);
        this.solutionSteps = List.copyOf(builder.solutionSteps);
        this.recommendedJvmOptions = List.copyOf(builder.recommendedJvmOptions);
        this.toolingTips = List.copyOf(builder.toolingTips);
    }

    public String getPrinciple() {
        return principle;
    }

    public List<String> getReproductionSteps() {
        return reproductionSteps;
    }

    public List<String> getDiagnosticSteps() {
        return diagnosticSteps;
    }

    public List<String> getSolutionSteps() {
        return solutionSteps;
    }

    public List<String> getRecommendedJvmOptions() {
        return recommendedJvmOptions;
    }

    public List<String> getToolingTips() {
        return toolingTips;
    }

    /**
     * Builder 保障字段不可变，避免多线程访问时出现数据竞争。
     */
    public static class Builder {
        private String principle = "";
        private List<String> reproductionSteps = Collections.emptyList();
        private List<String> diagnosticSteps = Collections.emptyList();
        private List<String> solutionSteps = Collections.emptyList();
        private List<String> recommendedJvmOptions = Collections.emptyList();
        private List<String> toolingTips = Collections.emptyList();

        public Builder principle(String principle) {
            this.principle = principle;
            return this;
        }

        public Builder reproductionSteps(List<String> reproductionSteps) {
            this.reproductionSteps = reproductionSteps;
            return this;
        }

        public Builder diagnosticSteps(List<String> diagnosticSteps) {
            this.diagnosticSteps = diagnosticSteps;
            return this;
        }

        public Builder solutionSteps(List<String> solutionSteps) {
            this.solutionSteps = solutionSteps;
            return this;
        }

        public Builder recommendedJvmOptions(List<String> recommendedJvmOptions) {
            this.recommendedJvmOptions = recommendedJvmOptions;
            return this;
        }

        public Builder toolingTips(List<String> toolingTips) {
            this.toolingTips = toolingTips;
            return this;
        }

        public ScenarioGuide build() {
            return new ScenarioGuide(this);
        }
    }
}
