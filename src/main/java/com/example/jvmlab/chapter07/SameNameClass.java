package com.example.jvmlab.chapter07;

/**
 * 打破双亲委派实验用的同名类。
 * <p>
 * 实现思路：类中只包含一个标识字段，真正关注的是类加载器是否一致。
 * </p>
 */
public class SameNameClass {

    /** 唯一标识。 */
    private final String marker = "default";

    /**
     * 返回标识，用于确认实例来源。
     *
     * @return 标识字符串。
     */
    public String marker() {
        return marker;
    }
}
