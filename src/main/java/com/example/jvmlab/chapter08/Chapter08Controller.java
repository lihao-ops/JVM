package com.example.jvmlab.chapter08;

import lombok.extern.slf4j.Slf4j;
import org.openjdk.jol.info.ClassLayout;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

/**
 * 第8章：虚拟机字节码执行引擎示例。
 * <p>
 * 实现思路：
 * 1. 使用MethodHandle演示字节码层面的动态调用，验证invokedynamic原理。
 * 2. 使用JOL打印对象布局，观察对象头、字段对齐等底层细节。
 * 3. 结合日志解释每一步的执行流程，帮助理解解释执行与即时编译的连接点。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/chapter08")
public class Chapter08Controller {

    /**
     * 使用MethodHandle调用目标方法，模拟invokedynamic绑定过程。
     *
     * @param message 输入消息。
     * @return 方法返回值。
     * @throws Throwable 反射异常。
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
     * 打印指定对象的内存布局。
     *
     * @return 布局信息。
     */
    @GetMapping("/object-layout")
    public Map<String, String> objectLayout() {
        log.info("打印对象内存布局 Printing object layout using JOL");
        SampleObject obj = new SampleObject();
        String layout = ClassLayout.parseInstance(obj).toPrintable();
        Map<String, String> result = new HashMap<>();
        result.put("layout", layout);
        log.debug("对象布局 Object layout:\n{}", layout);
        return result;
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
