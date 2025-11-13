package com.example.jvmlab.common;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类说明 / Class Description:
 * 中文：基于 ASM 的运行时类生成工具，提供轻量的动态类构建能力以替代外部依赖。
 * English: ASM-based utility for generating lightweight runtime classes, replacing external dependencies.
 *
 * 使用场景 / Use Cases:
 * 中文：在无法使用 ByteBuddy 的环境下，生成简单的接口实现或固定 toString 的类。
 * English: Generate simple interface implementations or constant toString classes when ByteBuddy is unavailable.
 *
 * 设计目的 / Design Purpose:
 * 中文：以最小功能集满足实验需求，保持字节码生成过程可控且易于维护。
 * English: Provide a minimal feature set for experiments, keeping bytecode generation controlled and maintainable.
 */
public final class AsmDynamicClassBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    /**
     * 方法说明 / Method Description:
     * 中文：私有构造，禁止实例化工具类。
     * English: Private constructor to prevent instantiation.
     */
    private AsmDynamicClassBuilder() {
    }

    /**
     * 方法说明 / Method Description:
     * 中文：为指定接口生成一个零参数且返回 String 的方法实现，返回动态类。
     * English: Generate an implementation of a zero-argument, String-returning method for the given interface.
     *
     * 参数 / Parameters:
     * @param interfaceType 中文：待实现的接口类型 / English: Target interface type
     * @param methodName 中文：待实现的方法名 / English: Method name to implement
     * @param returnValue 中文：返回的常量字符串 / English: Constant string to return
     * @param parentLoader 中文：父类加载器 / English: Parent class loader
     *
     * 返回值 / Return:
     * 中文：实现该接口的动态类 / English: Dynamically generated class implementing the interface
     *
     * 异常 / Exceptions:
     * 中文：若方法签名不满足零参且返回 String，将抛出 IllegalArgumentException / English: IllegalArgumentException if signature invalid
     */
    public static <T> Class<? extends T> createConstantImplementation(
            Class<T> interfaceType,
            String methodName,
            String returnValue,
            ClassLoader parentLoader) {
        // 中文：定位目标方法，确保满足零参且返回 String 的约束
        // English: Locate target method ensuring zero-arg and String-returning constraints
        Method targetMethod = findTargetMethod(interfaceType, methodName);
        if (targetMethod.getParameterCount() != 0 || targetMethod.getReturnType() != String.class) {
            throw new IllegalArgumentException("Only zero-argument String methods are supported");
        }

        String generatedClassName = interfaceType.getName() + "$AsmImpl$" + COUNTER.incrementAndGet();
        // 中文：生成方法体字节码并定义为可加载类
        // English: Generate method bytecode and define it as a loadable class
        byte[] bytecode = generateConstantImplementation(interfaceType, targetMethod, generatedClassName, returnValue);
        return defineClass(parentLoader, generatedClassName, bytecode).asSubclass(interfaceType);
    }

    /**
     * 方法说明 / Method Description:
     * 中文：生成一个简单类，使其 toString() 返回常量字符串。
     * English: Generate a simple class whose toString() returns a constant value.
     *
     * 参数 / Parameters:
     * @param parentLoader 中文：父类加载器 / English: Parent class loader
     * @param className 中文：生成类的全限定名 / English: Fully qualified generated class name
     * @param returnValue 中文：toString 返回的常量 / English: Constant returned from toString()
     *
     * 返回值 / Return:
     * 中文：生成的类对象 / English: Generated class
     * 异常 / Exceptions: 无
     */
    public static Class<?> createConstantToStringClass(
            ClassLoader parentLoader,
            String className,
            String returnValue) {
        // 中文：生成类字节码并定义为可加载类
        // English: Generate class bytecode and define it as a loadable class
        byte[] bytecode = generateToStringClass(className, returnValue);
        return defineClass(parentLoader, className, bytecode);
    }

    /**
     * 方法说明 / Method Description:
     * 中文：查找接口中的目标方法，若不存在抛出非法参数异常。
     * English: Find the target method in the interface or throw an illegal argument exception.
     */
    private static Method findTargetMethod(Class<?> interfaceType, String methodName) {
        try {
            return interfaceType.getMethod(methodName);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException("Method " + methodName + " not found on " + interfaceType.getName(), ex);
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：生成实现接口方法的类字节码，方法返回常量字符串。
     * English: Generate bytecode for a class implementing the interface method that returns a constant string.
     */
    private static byte[] generateConstantImplementation(Class<?> interfaceType,
                                                          Method method,
                                                          String className,
                                                          String returnValue) {
        String internalName = className.replace('.', '/');
        String interfaceInternalName = Type.getInternalName(interfaceType);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName, null,
                "java/lang/Object", new String[]{interfaceInternalName});

        // 中文：生成默认构造器
        // English: Generate default constructor
        generateDefaultConstructor(writer);

        String descriptor = Type.getType(method).getDescriptor();
        MethodVisitor methodVisitor = writer.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), descriptor, null, null);
        // 中文：将常量入栈并返回
        // English: Push constant and return
        methodVisitor.visitLdcInsn(returnValue);
        methodVisitor.visitInsn(Opcodes.ARETURN);
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }

    /**
     * 方法说明 / Method Description:
     * 中文：生成包含固定 toString() 的类字节码。
     * English: Generate bytecode for a class with a fixed toString().
     */
    private static byte[] generateToStringClass(String className, String returnValue) {
        String internalName = className.replace('.', '/');
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName, null,
                "java/lang/Object", null);

        // 中文：生成默认构造器
        // English: Generate default constructor
        generateDefaultConstructor(writer);

        MethodVisitor toStringMethod = writer.visitMethod(Opcodes.ACC_PUBLIC, "toString",
                Type.getMethodDescriptor(Type.getType(String.class)), null, null);
        // 中文：返回常量字符串
        // English: Return constant string
        toStringMethod.visitLdcInsn(returnValue);
        toStringMethod.visitInsn(Opcodes.ARETURN);
        toStringMethod.visitMaxs(0, 0);
        toStringMethod.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }

    /**
     * 方法说明 / Method Description:
     * 中文：在生成类中添加无参默认构造器。
     * English: Add a parameterless default constructor to the generated class.
     */
    private static void generateDefaultConstructor(ClassWriter writer) {
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
    }

    /**
     * 方法说明 / Method Description:
     * 中文：通过自定义 ClassLoader 将字节码定义为可加载类。
     * English: Define bytecode as a loadable class via a custom ClassLoader.
     */
    private static Class<?> defineClass(ClassLoader parentLoader, String className, byte[] bytecode) {
        DynamicClassLoader loader = new DynamicClassLoader(parentLoader);
        return loader.define(className, bytecode);
    }

    /**
     * 类说明 / Class Description:
     * 中文：用于定义运行时生成类的轻量类加载器。
     * English: Lightweight class loader for defining runtime-generated classes.
     */
    private static final class DynamicClassLoader extends ClassLoader {
        private DynamicClassLoader(ClassLoader parent) {
            super(parent);
        }

        /**
         * 方法说明 / Method Description:
         * 中文：将字节码转换为可用的 Class 对象。
         * English: Convert bytecode into a usable Class object.
         */
        private Class<?> define(String name, byte[] bytecode) {
            return defineClass(name, bytecode, 0, bytecode.length);
        }
    }
}
