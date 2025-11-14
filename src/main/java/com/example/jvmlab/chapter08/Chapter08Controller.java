package com.example.jvmlab.chapter08;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 类说明 / Class Description:
 * 中文：第8章控制器，演示字节码执行引擎相关能力（MethodHandle 动态绑定、对象内存布局）。
 * English: Chapter 08 controller demonstrating bytecode engine capabilities (MethodHandle dynamic binding, object memory layout).
 *
 * 使用场景 / Use Cases:
 * 中文：学习 invokedynamic 与反射差异、观察对象头与字段对齐等底层细节。
 * English: Learn differences between invokedynamic and reflection; observe object headers and field alignment.
 *
 * 设计目的 / Design Purpose:
 * 中文：通过最小可运行示例直观理解执行引擎的关键点。
 * English: Use minimal runnable examples to intuitively understand key points of the execution engine.
 */
@Slf4j
@RestController
@RequestMapping("/chapter08")
public class Chapter08Controller {

    /**
     * 方法说明 / Method Description:
     * 中文：使用 MethodHandle 调用目标静态方法，模拟 invokedynamic 的绑定过程。
     * English: Invoke a target static method via MethodHandle, simulating invokedynamic binding.
     *
     * 章节标注 / Book Correlation:
     * 中文：第8章 字节码执行引擎 → MethodHandle/InvokeDynamic
     * English: Chapter 8 Bytecode Execution Engine → MethodHandle/InvokeDynamic
     *
     * 参数 / Parameters:
     * @param message 中文：输入消息 / English: Input message
     * 返回值 / Return: 中文：方法返回值 / English: Method result
     * 异常 / Exceptions: 中文：可能抛出反射/查找异常 / English: May throw reflection/lookup errors
     */
    @GetMapping("/method-handle")
    public String methodHandle(@RequestParam(defaultValue = "hello") String message) throws Throwable {
        log.info("MethodHandle实验开始 Invoking method handle with message={}", message);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType type = MethodType.methodType(String.class, String.class);
        MethodHandle handle = lookup.findStatic(TargetMethods.class, "echo", type);
        String result = (String) handle.invokeExact(message);
        log.info("MethodHandle执行结果 Result: {}", result);
        return result;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：使用 LambdaMetafactory 通过 invokedynamic 生成函数式接口绑定，动态调用并返回结果。
     * English: Use LambdaMetafactory to bind a functional interface via invokedynamic and call dynamically.
     *
     * 章节标注 / Book Correlation:
     * 中文：第8章 字节码执行引擎 → invokedynamic 与 Lambda 表达式实现机制
     * English: Chapter 8 Bytecode Execution Engine → invokedynamic and lambda implementation
     *
     * 参数 / Parameters:
     * @param message 中文：输入消息 / English: Input message
     * 返回值 / Return: 中文：方法返回值 / English: Method result
     * 异常 / Exceptions: 中文：可能抛出反射/查找异常 / English: May throw reflection/lookup errors
     */
    @GetMapping("/invoke-dynamic")
    public String invokeDynamic(@RequestParam(defaultValue = "dyn") String message) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType targetType = MethodType.methodType(String.class, String.class);
        MethodHandle handle = lookup.findStatic(TargetMethods.class, "echo", targetType);
        CallSite callSite = LambdaMetafactory.metafactory(
                lookup,
                "apply",
                MethodType.methodType(Function.class),
                MethodType.methodType(Object.class, Object.class),
                handle,
                targetType);
        @SuppressWarnings("unchecked")
        Function<String, String> fn = (Function<String, String>) callSite.getTarget().invokeExact();
        String result = fn.apply(message);
        log.info("InvokeDynamic执行结果 Result: {}", result);
        return result;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：打印示例对象的内存布局，若缺少 JOL 依赖则给出提示。
     * English: Print the sample object's memory layout or advise to add JOL dependency if missing.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：布局信息 Map / English: Layout info map
     * 异常 / Exceptions: 中文：可能抛出反射异常 / English: May throw reflection errors
     */
    @GetMapping("/object-layout")
    public Map<String, String> objectLayout() {
        log.info("打印对象内存布局 Printing object layout using JOL");
        SampleObject obj = new SampleObject();
        Map<String, String> result = new HashMap<>();
        try {
            String layout = extractLayoutViaReflection(obj);
            result.put("layout", layout);
            log.debug("对象布局 Object layout:\n{}", layout);
        } catch (ClassNotFoundException ex) {
            String message = "JOL依赖未找到，请在构建中添加 org.openjdk.jol:jol-core";
            log.warn("JOL classpath missing: {}", ex.getMessage());
            result.put("layout", message);
        } catch (ReflectiveOperationException ex) {
            String message = "使用JOL解析对象布局失败: " + ex.getMessage();
            log.error(message, ex);
            result.put("layout", message);
        }
        return result;
    }

    private static String extractLayoutViaReflection(Object obj) throws ReflectiveOperationException {
        Class<?> classLayout = Class.forName("org.openjdk.jol.info.ClassLayout");
        Method parseInstance = classLayout.getMethod("parseInstance", Object.class);
        Object layoutInstance = parseInstance.invoke(null, obj);
        Method toPrintable = layoutInstance.getClass().getMethod("toPrintable");
        return (String) toPrintable.invoke(layoutInstance);
    }

    /** MethodHandle示例目标类。 */
    static class TargetMethods {
        static String echo(String input) {
            log.info("TargetMethods.echo执行 Executing TargetMethods.echo with input={}", input);
            return "Echo:" + input;
        }
    }

    /** 对象布局示例类。 */
    static class SampleObject {
        int intField = 42;
        long longField = 1024L;
        boolean flag = true;
    }
}
