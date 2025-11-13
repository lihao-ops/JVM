package com.example.jvmlab.chapter07;

/**
 * 类说明 / Class Description:
 * 中文：用于自定义类加载器实验的示例类，通过 toString 输出加载器来源信息。
 * English: Sample class for custom class loader experiments, printing loader source via toString.
 *
 * 使用场景 / Use Cases:
 * 中文：加载后验证类加载器层次与来源，辅助理解双亲委派。
 * English: Verify loader hierarchy and origin after loading to understand parental delegation.
 *
 * 设计目的 / Design Purpose:
 * 中文：保持类体简单，聚焦加载器信息的观察。
 * English: Keep the class simple to focus on observing loader information.
 */
public class CustomLoadedClass {

    /**
     * 方法说明 / Method Description:
     * 中文：返回包含当前类加载器信息的描述字符串。
     * English: Return a description string containing the current class loader information.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：描述字符串 / English: Description string
     * 异常 / Exceptions: 无
     */
    @Override
    public String toString() {
        return "CustomLoadedClass from " + getClass().getClassLoader();
    }
}
