package com.example.jvmlab.chapter07;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 第7章：虚拟机类加载机制实验控制器。
 * <p>
 * 实现思路：
 * 1. 覆盖主动引用、被动引用、自定义类加载器、类卸载等知识点，提供REST接口方便演示。
 * 2. 通过日志输出类初始化顺序和类加载器信息，结合中文+英文注释帮助快速理解。
 * 3. 设计多个内部静态类模拟书中案例，辅以自定义ClassLoader的示例实现。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/chapter07")
public class Chapter07Controller {

    /**
     * 通过new关键字触发主动引用。
     *
     * @return 提示信息。
     */
    @GetMapping("/active-use-new")
    public String testActiveUseByNew() {
        log.info("通过new触发类初始化 Testing class initialization via new");
        ActiveUseClass obj = new ActiveUseClass();
        log.debug("对象引用 hash code: {}", System.identityHashCode(obj));
        return "Class initialized via new. See logs for order.";
    }

    /**
     * 通过访问静态字段触发初始化，并对比常量的被动引用。
     *
     * @return 字段值描述。
     */
    @GetMapping("/active-use-static-field")
    public String testActiveUseByStaticField() {
        log.info("访问静态字段触发初始化 Testing static field initialization");
        int value = StaticFieldClass.VALUE;
        String constant = ConstantClass.CONSTANT;
        log.info("静态字段值 Static field value: {}", value);
        log.info("常量值 Constant value (no init): {}", constant);
        return "value=" + value + ", constant=" + constant;
    }

    /**
     * 调用静态方法触发初始化。
     *
     * @return 方法返回值。
     */
    @GetMapping("/active-use-static-method")
    public String testActiveUseByStaticMethod() {
        log.info("调用静态方法触发初始化 Testing static method initialization");
        String result = StaticMethodClass.staticMethod();
        log.info("静态方法返回值 Static method result: {}", result);
        return result;
    }

    /**
     * 子类引用父类静态字段，不触发子类初始化。
     *
     * @return 父类字段值。
     */
    @GetMapping("/passive-use-parent-field")
    public String testPassiveUseParentField() {
        log.info("子类引用父类字段 Testing passive reference via child");
        int value = Child.parentValue;
        log.info("父类字段值 Parent field value: {}", value);
        return "Parent value: " + value;
    }

    /**
     * 数组定义不会触发类初始化。
     *
     * @return 数组类型信息。
     */
    @GetMapping("/passive-use-array")
    public String testPassiveUseArray() {
        log.info("数组定义测试 Testing array reference without initialization");
        ArrayElementClass[] array = new ArrayElementClass[10];
        log.info("数组类型 Array type: {}", array.getClass().getName());
        return "Array type: " + array.getClass().getName();
    }

    /**
     * 演示类加载过程中的准备阶段与初始化阶段差异。
     *
     * @return 提示信息。
     */
    @GetMapping("/class-loading-preparation")
    public String testClassLoadingPreparation() {
        log.info("测试类加载准备阶段 Testing preparation phase");
        PreparationPhaseClass.printValues();
        return "Check logs for zero-value vs assigned-value.";
    }

    /**
     * 演示类初始化顺序。
     *
     * @return 提示信息。
     */
    @GetMapping("/initialization-order")
    public String testInitializationOrder() {
        log.info("开始类初始化顺序实验 Testing initialization order");
        InitOrderChild instance = new InitOrderChild();
        log.debug("实验对象引用 Instance hash: {}", System.identityHashCode(instance));
        return "Initialization order logged.";
    }

    /**
     * 查看类加载器的层次结构。
     *
     * @return 类加载器信息Map。
     */
    @GetMapping("/classloader-hierarchy")
    public Map<String, String> testClassLoaderHierarchy() {
        log.info("查看类加载器层次结构 Inspecting class loader hierarchy");
        Map<String, String> result = new LinkedHashMap<>();
        ClassLoader appLoader = this.getClass().getClassLoader();
        ClassLoader platformLoader = appLoader != null ? appLoader.getParent() : null;
        ClassLoader bootstrapLoader = platformLoader != null ? platformLoader.getParent() : null;
        result.put("Application", String.valueOf(appLoader));
        result.put("Platform", String.valueOf(platformLoader));
        result.put("Bootstrap", bootstrapLoader == null ? "null (C++ loader)" : bootstrapLoader.toString());
        result.put("String.class", String.valueOf(String.class.getClassLoader()));
        result.put("ArrayList.class", String.valueOf(ArrayList.class.getClassLoader()));
        log.info("类加载器结果 ClassLoader map: {}", result);
        return result;
    }

    /**
     * 使用自定义类加载器加载类。
     *
     * @return 加载结果描述。
     */
    @GetMapping("/custom-classloader")
    public String testCustomClassLoader() {
        log.info("测试自定义类加载器 Testing custom class loader");
        try {
            CustomClassLoader loader = new CustomClassLoader();
            Class<?> clazz = loader.findClass("com.example.jvmlab.chapter07.CustomLoadedClass");
            log.info("已加载类 Loaded class: {}", clazz.getName());
            log.info("类加载器 Class loader: {}", clazz.getClassLoader());
            return "Custom class loaded: " + clazz.getName();
        } catch (Exception e) {
            log.error("自定义类加载器失败 Custom class loader failed", e);
            return "Failed: " + e.getMessage();
        }
    }

    /**
     * 演示打破双亲委派模型的效果。
     *
     * @return 类比较结果。
     */
    @GetMapping("/break-parent-delegation")
    public String testBreakParentDelegation() {
        log.info("测试打破双亲委派模型 Testing breaking parent delegation");
        try {
            BreakDelegationClassLoader loader1 = new BreakDelegationClassLoader();
            BreakDelegationClassLoader loader2 = new BreakDelegationClassLoader();
            Class<?> class1 = loader1.loadClass("com.example.jvmlab.chapter07.SameNameClass");
            Class<?> class2 = loader2.loadClass("com.example.jvmlab.chapter07.SameNameClass");
            log.info("类1加载器 Class1 loader: {}", class1.getClassLoader());
            log.info("类2加载器 Class2 loader: {}", class2.getClassLoader());
            log.info("类对象是否相等 Are classes equal: {}", class1 == class2);
            return "class1 == class2 -> " + (class1 == class2);
        } catch (Exception e) {
            log.error("打破双亲委派失败 Break delegation failed", e);
            return "Failed: " + e.getMessage();
        }
    }

    /**
     * 演示类卸载，需结合-XX:+TraceClassUnloading。
     *
     * @return 提示信息。
     */
    @GetMapping("/class-unloading")
    public String testClassUnloading() {
        log.info("测试类卸载 Testing class unloading");
        try {
            CustomClassLoader loader = new CustomClassLoader();
            Class<?> clazz = loader.findClass("com.example.jvmlab.chapter07.UnloadableClass");
            Object instance = clazz.getDeclaredConstructor().newInstance();
            log.info("加载类 Loaded class: {}", clazz.getName());
            instance = null;
            clazz = null;
            loader = null;
            System.gc();
            Thread.sleep(1000);
            return "Class unloading requested. Check JVM logs.";
        } catch (Exception e) {
            log.error("类卸载实验失败 Class unloading test failed", e);
            return "Failed: " + e.getMessage();
        }
    }

    /** 主动引用测试类。 */
    static class ActiveUseClass {
        static {
            log.info("ActiveUseClass静态块执行 Static block executed");
        }
        ActiveUseClass() {
            log.info("ActiveUseClass构造器执行 Constructor executed");
        }
    }

    /** 静态字段测试类。 */
    static class StaticFieldClass {
        static {
            log.info("StaticFieldClass静态块执行 Static block executed");
        }
        static final int VALUE = 123;
    }

    /** 常量类。 */
    static class ConstantClass {
        static {
            log.info("ConstantClass静态块执行（不会触发） Static block (should not run)");
        }
        static final String CONSTANT = "CONSTANT_VALUE";
    }

    /** 静态方法测试类。 */
    static class StaticMethodClass {
        static {
            log.info("StaticMethodClass静态块执行 Static block executed");
        }
        static String staticMethod() {
            return "Static method invoked";
        }
    }

    /** 父类，用于被动引用实验。 */
    static class Parent {
        static {
            log.info("Parent静态块执行 Parent static block executed");
        }
        static int parentValue = 100;
    }

    /** 子类，用于被动引用实验。 */
    static class Child extends Parent {
        static {
            log.info("Child静态块执行 Child static block executed");
        }
        static int childValue = 200;
    }

    /** 数组元素类，用于验证数组引用不会初始化类。 */
    static class ArrayElementClass {
        static {
            log.info("ArrayElementClass静态块执行 Static block executed");
        }
    }

    /** 准备阶段测试类。 */
    static class PreparationPhaseClass {
        static int value = 123;
        static {
            log.info("PreparationPhaseClass静态块执行 value={} Static block", value);
        }
        static void printValues() {
            log.info("Preparation阶段验证 Value after initialization: {}", value);
        }
    }

    /** 初始化顺序父类。 */
    static class InitOrderParent {
        static int parentStatic = initParentStatic();
        static {
            log.info("InitOrderParent静态块 Parent static block");
        }
        int parentInstance = initParentInstance();
        {
            log.info("InitOrderParent实例代码块 Parent instance block");
        }
        InitOrderParent() {
            log.info("InitOrderParent构造器 Parent constructor");
        }
        private static int initParentStatic() {
            log.info("InitOrderParent静态字段初始化 Parent static field init");
            return 1;
        }
        private int initParentInstance() {
            log.info("InitOrderParent实例字段初始化 Parent instance field init");
            return 1;
        }
    }

    /** 初始化顺序子类。 */
    static class InitOrderChild extends InitOrderParent {
        static int childStatic = initChildStatic();
        static {
            log.info("InitOrderChild静态块 Child static block");
        }
        int childInstance = initChildInstance();
        {
            log.info("InitOrderChild实例代码块 Child instance block");
        }
        InitOrderChild() {
            log.info("InitOrderChild构造器 Child constructor");
        }
        private static int initChildStatic() {
            log.info("InitOrderChild静态字段初始化 Child static field init");
            return 2;
        }
        private int initChildInstance() {
            log.info("InitOrderChild实例字段初始化 Child instance field init");
            return 2;
        }
    }

    /** 自定义类加载器实现。 */
    static class CustomClassLoader extends ClassLoader {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                byte[] data = loadClassData(name);
                if (data == null) {
                    throw new ClassNotFoundException(name);
                }
                return defineClass(name, data, 0, data.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
        private byte[] loadClassData(String className) throws IOException {
            String path = className.replace('.', '/') + ".class";
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    return null;
                }
                return is.readAllBytes();
            }
        }
    }

    /** 打破双亲委派的类加载器。 */
    static class BreakDelegationClassLoader extends ClassLoader {
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith("com.example.jvmlab.chapter07.SameNameClass")) {
                return findClass(name);
            }
            return super.loadClass(name);
        }
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                byte[] data = loadClassData(name);
                if (data == null) {
                    data = new byte[0];
                }
                return defineClass(name, data, 0, data.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
        private byte[] loadClassData(String className) throws IOException {
            String path = className.replace('.', '/') + ".class";
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    return null;
                }
                return is.readAllBytes();
            }
        }
    }
}
