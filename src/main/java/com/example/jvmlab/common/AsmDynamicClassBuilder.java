package com.example.jvmlab.common;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility helpers that rely on ASM to generate small runtime classes.
 * <p>
 * The project originally used ByteBuddy for these demos, but in environments
 * without Maven Central access the ByteBuddy dependency cannot be resolved.
 * These helpers replace the very small subset of features we relied on so that
 * the demos keep functioning without the external library.
 * </p>
 */
public final class AsmDynamicClassBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private AsmDynamicClassBuilder() {
    }

    /**
     * Generate an implementation of a zero-argument String-returning method for the given interface.
     *
     * @param interfaceType the interface that should be implemented.
     * @param methodName    the method to implement; it must be zero-arg and return {@link String}.
     * @param returnValue   the constant value returned by the generated implementation.
     * @param parentLoader  the parent class loader.
     * @param <T>           interface type parameter.
     * @return a dynamically generated class that implements {@code interfaceType}.
     */
    public static <T> Class<? extends T> createConstantImplementation(
            Class<T> interfaceType,
            String methodName,
            String returnValue,
            ClassLoader parentLoader) {
        Method targetMethod = findTargetMethod(interfaceType, methodName);
        if (targetMethod.getParameterCount() != 0 || targetMethod.getReturnType() != String.class) {
            throw new IllegalArgumentException("Only zero-argument String methods are supported");
        }

        String generatedClassName = interfaceType.getName() + "$AsmImpl$" + COUNTER.incrementAndGet();
        byte[] bytecode = generateConstantImplementation(interfaceType, targetMethod, generatedClassName, returnValue);
        return defineClass(parentLoader, generatedClassName, bytecode).asSubclass(interfaceType);
    }

    /**
     * Generate a simple class whose {@code toString()} returns a constant value.
     *
     * @param parentLoader the parent class loader.
     * @param className    fully qualified name for the generated class.
     * @param returnValue  constant returned from {@code toString()}.
     * @return the generated class.
     */
    public static Class<?> createConstantToStringClass(
            ClassLoader parentLoader,
            String className,
            String returnValue) {
        byte[] bytecode = generateToStringClass(className, returnValue);
        return defineClass(parentLoader, className, bytecode);
    }

    private static Method findTargetMethod(Class<?> interfaceType, String methodName) {
        try {
            return interfaceType.getMethod(methodName);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException("Method " + methodName + " not found on " + interfaceType.getName(), ex);
        }
    }

    private static byte[] generateConstantImplementation(Class<?> interfaceType,
                                                          Method method,
                                                          String className,
                                                          String returnValue) {
        String internalName = className.replace('.', '/');
        String interfaceInternalName = Type.getInternalName(interfaceType);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName, null,
                "java/lang/Object", new String[]{interfaceInternalName});

        generateDefaultConstructor(writer);

        String descriptor = Type.getType(method).getDescriptor();
        MethodVisitor methodVisitor = writer.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), descriptor, null, null);
        methodVisitor.visitLdcInsn(returnValue);
        methodVisitor.visitInsn(Opcodes.ARETURN);
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] generateToStringClass(String className, String returnValue) {
        String internalName = className.replace('.', '/');
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName, null,
                "java/lang/Object", null);

        generateDefaultConstructor(writer);

        MethodVisitor toStringMethod = writer.visitMethod(Opcodes.ACC_PUBLIC, "toString",
                Type.getMethodDescriptor(Type.getType(String.class)), null, null);
        toStringMethod.visitLdcInsn(returnValue);
        toStringMethod.visitInsn(Opcodes.ARETURN);
        toStringMethod.visitMaxs(0, 0);
        toStringMethod.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void generateDefaultConstructor(ClassWriter writer) {
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
    }

    private static Class<?> defineClass(ClassLoader parentLoader, String className, byte[] bytecode) {
        DynamicClassLoader loader = new DynamicClassLoader(parentLoader);
        return loader.define(className, bytecode);
    }

    private static final class DynamicClassLoader extends ClassLoader {
        private DynamicClassLoader(ClassLoader parent) {
            super(parent);
        }

        private Class<?> define(String name, byte[] bytecode) {
            return defineClass(name, bytecode, 0, bytecode.length);
        }
    }
}
