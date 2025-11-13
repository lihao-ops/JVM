package com.example.jvmlab.chapter07;

/**
 * 类说明 / Class Description:
 * 中文：类卸载实验的目标类，包含少量字段与方法，配合自定义 ClassLoader 验证卸载行为。
 * English: Target class for class unloading experiments, with minimal fields/methods for verifying unloading via a custom ClassLoader.
 *
 * 使用场景 / Use Cases:
 * 中文：在类加载/卸载实验中实例化并释放引用，观察卸载日志。
 * English: Instantiate and release references in load/unload experiments and observe logs.
 *
 * 设计目的 / Design Purpose:
 * 中文：简化对象结构，降低干扰因素以突出卸载过程。
 * English: Simplify object structure to reduce noise and highlight the unloading process.
 */
public class UnloadableClass {

    /** 当前实例的编号。 */
    private final long id = System.nanoTime();

    /**
     * 方法说明 / Method Description:
     * 中文：返回当前实例的调试描述字符串，包含唯一编号。
     * English: Return a debug description string of the instance including a unique id.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：描述字符串 / English: Description string
     * 异常 / Exceptions: 无
     */
    public String describe() {
        return "UnloadableClass id=" + id;
    }
}
