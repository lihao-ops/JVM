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
 * 第9章：类加载及执行子系统案例与实战。
 * <p>
 * 实现思路：
 * 1. 演示JDK动态代理与ByteBuddy生成类的差异，理解类加载与字节码增强。
 * 2. 通过接口调用展示自定义ClassLoader如何隔离业务模块。
 * 3. 所有输出附带中英文解释，方便对比不同增强技术的适用场景。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/chapter09")
public class Chapter09Controller {

    /**
     * 使用JDK动态代理包装目标接口。
     *
     * @param message 消息。
     * @return 代理执行结果。
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
     * 使用ByteBuddy动态生成实现类。
     *
     * @param message 消息。
     * @return 动态类执行结果。
     * @throws InstantiationException 构造异常。
     * @throws IllegalAccessException 访问异常。
     * @throws NoSuchMethodException 构造方法不存在异常。
     * @throws InvocationTargetException 反射调用异常。
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
