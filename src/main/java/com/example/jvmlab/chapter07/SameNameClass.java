package com.example.jvmlab.chapter07;

/**
 * 类说明 / Class Description:
 * 中文：用于打破双亲委派实验的同名类，通过比较不同 ClassLoader 加载的类对象来验证隔离效果。
 * English: Same-named class used to demonstrate breaking parental delegation by comparing classes loaded by different ClassLoaders.
 *
 * 使用场景 / Use Cases:
 * 中文：配合自定义加载器分别加载，验证类对象是否相等。
 * English: Load via separate custom loaders to check class object equality.
 *
 * 设计目的 / Design Purpose:
 * 中文：保持最小字段集，聚焦类来源的比较。
 * English: Keep minimal fields to focus on source comparison.
 */
public class SameNameClass {

    /** 唯一标识。 */
    private final String marker = "default";

    /**
     * 方法说明 / Method Description:
     * 中文：返回类内标识字符串，用于区分实例。
     * English: Return the internal marker string to distinguish instances.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：标识字符串 / English: Marker string
     * 异常 / Exceptions: 无
     */
    public String marker() {
        return marker;
    }
}
