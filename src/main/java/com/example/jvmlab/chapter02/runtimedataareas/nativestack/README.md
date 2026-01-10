# 本地方法栈 (Native Method Stack)

> **注意**：本项目没有针对本地方法栈的独立实验代码。

## 为什么没有代码？

在 **HotSpot 虚拟机**（目前最主流的 JVM）中，**虚拟机栈 (VM Stack)** 和 **本地方法栈 (Native Method Stack)** 是**合二为一**的。

### 核心原理
1.  **JVM 规范**：允许将虚拟机栈和本地方法栈实现为同一个栈。
2.  **HotSpot 实现**：
    *   虽然 JVM 规范定义了 `-Xoss` 参数来设置本地方法栈大小，但在 HotSpot 中，这个参数是**无效的 (Ignored)**。
    *   栈容量只由 `-Xss` 参数统一控制。
    *   无论是 Java 方法调用还是 Native 方法调用，都使用同一个栈空间。

### 结论
因此，我们在 `vmstack` 目录下进行的 `StackOverflowError` 实验，实际上已经涵盖了本地方法栈溢出的场景。

*   **Java 方法递归过深** -> `StackOverflowError`
*   **Native 方法递归过深** -> `StackOverflowError` (在 HotSpot 中表现一致)

无需单独编写针对 Native Stack 的测试用例。
