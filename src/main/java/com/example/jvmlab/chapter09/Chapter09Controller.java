package com.example.jvmlab.chapter09;

import com.example.jvmlab.common.AsmDynamicClassBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
/**
 * 类说明 / Class Description:
 * 中文：第9章控制器，演示类加载与执行子系统中的 JDK 动态代理与 ASM 生成类对比。
 * English: Chapter 09 controller demonstrating JDK dynamic proxies and ASM-generated classes in the class loading/execution subsystem.
 *
 * 使用场景 / Use Cases:
 * 中文：理解代理与字节码增强的适用场景与差异，观察加载器隔离效果。
 * English: Understand use cases and differences between proxies and bytecode enhancement, and loader isolation effects.
 *
 * 设计目的 / Design Purpose:
 * 中文：以最小示例呈现关键原理，便于课堂与面试演示。
 * English: Present key principles in minimal examples for class/interview demos.
 */
@Slf4j
@RestController
@RequestMapping("/chapter09")
public class Chapter09Controller {

    /**
     * 方法说明 / Method Description:
     * 中文：使用 JDK 动态代理包装接口调用，追加前后日志以观察调用流程。
     * English: Wrap interface calls with JDK dynamic proxy, adding pre/post logs to observe invocation flow.
     *
     * 参数 / Parameters:
     * @param message 中文：输入消息 / English: Input message
     * 返回值 / Return: 中文：代理返回结果 / English: Proxy execution result
     * 异常 / Exceptions: 无
     */
    @GetMapping("/jdk-proxy")
    public String jdkProxy(@RequestParam(defaultValue = "proxy") String message) {
        log.info("创建JDK动态代理 Creating JDK dynamic proxy, message={}", message);
        SampleService service = () -> "origin:" + message;
        SampleService proxy = (SampleService) Proxy.newProxyInstance(
                service.getClass().getClassLoader(),
                new Class[]{SampleService.class},
                new LoggingInvocationHandler(service));
        String result = proxy.process();
        log.info("代理返回结果 Proxy result: {}", result);
        return result;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：使用 ASM 生成接口实现类，验证运行时类生成与调用流程。
     * English: Use ASM to generate an interface implementation class and validate runtime generation and invocation flow.
     *
     * 参数 / Parameters:
     * @param message 中文：输入消息 / English: Input message
     * 返回值 / Return: 中文：动态类执行结果 / English: Result returned by the generated class
     * 异常 / Exceptions: 中文：可能抛出反射相关异常 / English: May throw reflection-related exceptions
     */
    @GetMapping("/bytebuddy-impl")
    public String byteBuddy(@RequestParam(defaultValue = "bytebuddy") String message)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        log.info("使用ByteBuddy生成类 Generating class via ByteBuddy, message={}", message);
        Class<? extends SampleService> clazz = AsmDynamicClassBuilder.createConstantImplementation(
                SampleService.class,
                "process",
                "bytebuddy:" + message,
                getClass().getClassLoader());
        SampleService instance = clazz.getDeclaredConstructor().newInstance();
        String result = instance.process();
        log.info("ByteBuddy结果 ByteBuddy result: {}", result);
        return result;
    }

    /** 样例服务接口。 */
    interface SampleService {
        String process();
    }

    /** 带日志的InvocationHandler实现。 */
    static class LoggingInvocationHandler implements InvocationHandler {
        private final SampleService target;
        LoggingInvocationHandler(SampleService target) {
            this.target = target;
        }
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            log.info("JDK代理调用前 Before invoke: {}", method.getName());
            Object result = method.invoke(target, args);
            log.info("JDK代理调用后 After invoke: {}", result);
            return "proxy:" + result;
        }
    }
}
