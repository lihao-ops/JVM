package com.example.jvmlab.chapter07;

/**
 * 类卸载实验的目标类。
 * <p>
 * 实现思路：类中包含少量字段与方法，确保当自定义类加载器被回收时该类也能被卸载。
 * </p>
 */
public class UnloadableClass {

    /** 当前实例的编号。 */
    private final long id = System.nanoTime();

    /**
     * 输出调试信息。
     *
     * @return 描述字符串。
     */
    public String describe() {
        return "UnloadableClass id=" + id;
    }
}
