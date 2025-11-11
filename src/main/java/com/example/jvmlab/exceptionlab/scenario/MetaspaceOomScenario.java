package com.example.jvmlab.exceptionlab.scenario;

import com.example.jvmlab.common.AsmDynamicClassBuilder;
import com.example.jvmlab.exceptionlab.AbstractMemoryExceptionScenario;
import com.example.jvmlab.exceptionlab.model.JvmMemoryArea;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioGuide;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 动态生成 Class 对象占满元空间的实验实现。
 */
@Component
public class MetaspaceOomScenario extends AbstractMemoryExceptionScenario {

    private static final List<Class<?>> GENERATED_CLASSES = new ArrayList<>();

    @Override
    public String getId() {
        return "metaspace-oom";
    }

    @Override
    public String getDisplayName() {
        return "元空间触发 OutOfMemoryError";
    }

    @Override
    public String getExceptionType() {
        return "java.lang.OutOfMemoryError: Metaspace";
    }

    @Override
    public JvmMemoryArea getMemoryArea() {
        return JvmMemoryArea.METASPACE;
    }

    @Override
    public ScenarioGuide getGuide() {
        return new ScenarioGuide.Builder()
                .principle("JDK8 之后类元数据存放在本地内存的 Metaspace 中，动态生成大量 Class 会消耗这部分空间。")
                .reproductionSteps(List.of(
                        "设置 -XX:MetaspaceSize=10m -XX:MaxMetaspaceSize=10m。",
                        "调用 /memory-exception-lab/scenarios/metaspace-oom/execute?dryRun=false&classCount=100000。",
                        "观察日志中打印的 Metaspace 使用情况。"))
                .diagnosticSteps(List.of(
                        "执行 jcmd <pid> VM.native_memory summary | grep -i class。",
                        "使用 JProfiler 查看 Recorded Objects → Class 实例数量。"))
                .solutionSteps(List.of(
                        "排查自定义 ClassLoader 是否存在泄漏。",
                        "合理设置 -XX:MaxMetaspaceSize 或减少动态代理数量。"))
                .recommendedJvmOptions(List.of(
                        "-XX:MetaspaceSize=10m",
                        "-XX:MaxMetaspaceSize=10m",
                        "-XX:+HeapDumpOnOutOfMemoryError"))
                .toolingTips(List.of(
                        "jmap -clstats <pid> 统计类加载数量。",
                        "VisualVM MBeans → java.lang.ClassLoading → LoadedClassCount。"))
                .build();
    }

    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) {
        int target = Math.max(1, parseInt(requestParams, "classCount", 50_000));
        int generated = 0;
        try {
            while (generated < target) {
                String className = "com.example.jvmlab.exceptionlab.dynamic.DynamicClass" +
                        UUID.randomUUID().toString().replace("-", "");
                Class<?> clazz = AsmDynamicClassBuilder.createConstantToStringClass(
                        getClass().getClassLoader(),
                        className,
                        "metaspace" + generated);
                GENERATED_CLASSES.add(clazz);
                generated++;
            }
            return new ScenarioExecutionResult(getId(), false, false,
                    "Generated " + generated + " classes without triggering OOM",
                    Map.of("generatedClasses", generated),
                    List.of("提高 classCount 或收紧 MaxMetaspaceSize"));
        } catch (OutOfMemoryError error) {
            return new ScenarioExecutionResult(getId(), false, true,
                    "Metaspace OOM after generating " + generated + " classes",
                    Map.of("generatedClasses", generated),
                    List.of("执行 jcmd 查看 Class Space 使用情况"));
        }
    }
}
