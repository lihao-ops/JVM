package com.example.jvmlab.chapter07;

/**
 * 自定义类加载器实验用类。
 * <p>
 * 实现思路：保持类体简单，仅提供一个toString方法，便于观察类加载器信息。
 * </p>
 */
public class CustomLoadedClass {

    /**
     * 返回类加载来源描述。
     *
     * @return 描述字符串。
     */
    @Override
    public String toString() {
        return "CustomLoadedClass from " + getClass().getClassLoader();
    }
}
