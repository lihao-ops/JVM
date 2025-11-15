package com.example.jvmlab.exceptionlab.scenario;

import com.example.jvmlab.common.AsmDynamicClassBuilder;
import com.example.jvmlab.exceptionlab.AbstractMemoryExceptionScenario;
import com.example.jvmlab.exceptionlab.model.JvmMemoryArea;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioGuide;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 类说明 / Class Description:
 * 中文：通过 ASM 动态生成大量 Class 并保留引用，消耗 Metaspace 触发 OOM。
 * English: Dynamically generate many classes via ASM and retain references to consume Metaspace until OOM.
 *
 * 使用场景 / Use Cases:
 * 中文：演示 Class 元数据分配与 ClassLoader 对类卸载的影响。
 * English: Demonstrate class metadata allocation and class unloading behavior via ClassLoader references.
 *
 * 设计目的 / Design Purpose:
 * 中文：使用稳定的类生成器与列表缓存，确保可控地增加元空间占用。
 * English: Use a stable class builder and list caches to increase Metaspace usage in a controlled way.
 */
@Slf4j
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

    /**
     * 方法说明 / Method Description:
     * 中文：生成指定数量的类并保存，捕获 OOM 或完成后返回指标与建议。
     * English: Generate the target number of classes, retain them, and return metrics upon OOM or completion.
     *
     * 参数 / Parameters:
     * @param requestParams 中文：classCount 目标生成数量 / English: classCount target number of classes
     *
     * 返回值 / Return:
     * 中文：执行结果与指标 / English: Execution result with metrics
     *
     * 异常 / Exceptions:
     * 中文：可能抛出 OutOfMemoryError / English: May throw OutOfMemoryError
     */
    @Override
    protected ScenarioExecutionResult doExecute(Map<String, Object> requestParams) {
        int target = Math.max(1, parseInt(requestParams, "classCount", 50_000));
        int generated = 0;
        try {
            while (generated < target) {
                // 中文：生成唯一类名并创建返回常量 toString 的类
                // English: Generate unique class name and create a class with constant toString
                String className = "com.example.jvmlab.exceptionlab.dynamic.DynamicClass" +
                        UUID.randomUUID().toString().replace("-", "");
                Class<?> clazz = AsmDynamicClassBuilder.createConstantToStringClass(
                        getClass().getClassLoader(),
                        className,
                        "metaspace" + generated);
                // 中文：保存类引用，防止类卸载释放元空间
                // English: Retain class reference to prevent unloading and releasing Metaspace
                GENERATED_CLASSES.add(clazz);
                generated++;
            }
            return new ScenarioExecutionResult(getId(), false, false,
                    "Generated " + generated + " classes without triggering OOM",
                    Map.of("generatedClasses", generated),
                    List.of("提高 classCount 或收紧 MaxMetaspaceSize"));
        } catch (OutOfMemoryError error) {
            // 中文：成功触发 Metaspace OOM，打印成功确认日志
            // English: Successfully triggered Metaspace OOM; print success confirmation log
            log.info("【成功】Metaspace OOM 触发，已生成类数量={} / Success: Metaspace OOM triggered", generated);
            return new ScenarioExecutionResult(getId(), false, true,
                    "Metaspace OOM after generating " + generated + " classes",
                    Map.of("generatedClasses", generated),
                    List.of("执行 jcmd 查看 Class Space 使用情况"));
        }
    }
}
