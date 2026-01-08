# 第8章 虚拟机字节码执行引擎

> **对应书籍**: 《深入理解Java虚拟机（第3版）》第8章  
> **核心主题**: 运行时栈帧、方法调用、方法分派、MethodHandle

---

## 📖 核心内容概述

### 8.1 运行时栈帧结构

```
┌─────────────────────────────────────────────────────────────┐
│                       栈帧 (Stack Frame)                     │
├─────────────────────────────────────────────────────────────┤
│  局部变量表 (Local Variable Table)                           │
│  ┌─────┬─────┬─────┬─────┬─────┐                            │
│  │  0  │  1  │  2  │  3  │ ... │  Slot 数组                 │
│  │this │arg1 │arg2 │local│     │                            │
│  └─────┴─────┴─────┴─────┴─────┘                            │
├─────────────────────────────────────────────────────────────┤
│  操作数栈 (Operand Stack)                                    │
│  ┌─────┬─────┬─────┐                                        │
│  │ val │ val │ ... │  LIFO 栈结构                           │
│  └─────┴─────┴─────┘                                        │
├─────────────────────────────────────────────────────────────┤
│  动态链接 (Dynamic Linking)                                  │
│  → 指向运行时常量池的方法引用                                  │
├─────────────────────────────────────────────────────────────┤
│  方法返回地址 (Return Address)                               │
│  → 正常返回 / 异常返回                                       │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 方法调用指令

| 指令 | 调用对象 | 说明 |
| :--- | :--- | :--- |
| **invokestatic** | 静态方法 | 编译期确定，无需对象实例 |
| **invokespecial** | 构造函数、私有方法、super 方法 | 编译期确定 |
| **invokevirtual** | 普通虚方法 | 运行时动态分派 |
| **invokeinterface** | 接口方法 | 运行时动态分派 |
| **invokedynamic** | 动态调用点 | JDK7+，Lambda 底层实现 |

### 8.3 方法分派

| 分派类型 | 确定时机 | 依据 | 示例 |
| :--- | :--- | :--- | :--- |
| **静态分派** | 编译期 | 变量的静态类型 | 方法重载 |
| **动态分派** | 运行期 | 对象的实际类型 | 方法重写 |

```java
// 静态分派示例（重载）
void say(Object o) { System.out.println("Object"); }
void say(String s) { System.out.println("String"); }

Object obj = "hello";
say(obj);  // 输出 "Object"，编译期根据静态类型决定

// 动态分派示例（重写）
class Animal { void speak() { System.out.println("Animal"); }}
class Dog extends Animal { void speak() { System.out.println("Dog"); }}

Animal a = new Dog();
a.speak();  // 输出 "Dog"，运行期根据实际类型决定
```

### 8.4 invokedynamic 与方法句柄

```java
// Lambda 表达式底层使用 invokedynamic
Runnable r = () -> System.out.println("Hello");

// 反编译后：
// invokedynamic #0:run:()Ljava/lang/Runnable;
// BootstrapMethod: LambdaMetafactory.metafactory

// MethodHandle 示例
MethodHandles.Lookup lookup = MethodHandles.lookup();
MethodHandle mh = lookup.findVirtual(
    String.class, 
    "length", 
    MethodType.methodType(int.class)
);
int len = (int) mh.invoke("hello");  // 5
```

---

## 💻 代码实践清单

### 实验1: MethodHandle 使用

```bash
GET /chapter08/method-handle
```

**实验代码**: `Chapter08Controller.java:37`

```java
// 通过 MethodHandle 调用方法
MethodHandles.Lookup lookup = MethodHandles.lookup();
MethodHandle mh = lookup.findVirtual(
    String.class,
    "substring",
    MethodType.methodType(String.class, int.class, int.class)
);
String result = (String) mh.invoke("Hello World", 0, 5);
```

### 实验2: 对象内存布局 (JOL)

```bash
GET /chapter08/object-layout
```

**实验代码**: `Chapter08Controller.java:53`

```java
// 使用 JOL 查看对象内存布局
System.out.println(ClassLayout.parseInstance(new Object()).toPrintable());

// 输出示例：
// OFFSET  SIZE   TYPE DESCRIPTION
//      0     4        (object header)  Mark Word
//      4     4        (object header)  Mark Word
//      8     4        (object header)  Class Pointer
//     12     4        (alignment gap)
// Instance size: 16 bytes
```

### 实验3: invokedynamic 与 Lambda

```bash
GET /chapter08/invoke-dynamic
```

**实验代码**: `Chapter08Controller.java:80`

```java
// Lambda 底层实现
Runnable r = () -> System.out.println("Lambda");

// 等价于（简化版）：
// 1. invokedynamic 首次调用
// 2. LambdaMetafactory.metafactory 生成内部类
// 3. 后续调用直接使用生成的类
```

---

## 🏭 生产实践建议

### 1. 理解虚方法表 (vtable)

```
┌─────────────────────────────────────────────────────────────┐
│  JVM 优化方法调用：虚方法表 (Virtual Method Table)            │
├─────────────────────────────────────────────────────────────┤
│  每个类在方法区维护一个 vtable                               │
│  存储该类所有虚方法的实际入口地址                            │
│  子类 vtable 复制父类内容，覆盖被重写的方法地址               │
│  invokevirtual 通过 vtable 查找实际方法                      │
└─────────────────────────────────────────────────────────────┘

优化效果：
- 避免每次调用都遍历继承链
- O(1) 时间复杂度查找方法
```

### 2. Lambda 性能考量

```java
// 问题：在热点循环中创建 Lambda
for (int i = 0; i < 1000000; i++) {
    list.forEach(item -> process(item));  // 每次创建新实例？
}

// 实际上：JVM 会优化
// 1. 无状态 Lambda 只生成一个单例
// 2. 有状态 Lambda 可能每次创建新实例

// 最佳实践：复杂场景使用方法引用
list.forEach(this::process);  // 通常更高效
```

### 3. MethodHandle vs Reflection

| 特性 | MethodHandle | Reflection |
| :--- | :--- | :--- |
| 性能 | 接近直接调用 | 有额外开销 |
| 类型安全 | 编译期检查 | 运行时检查 |
| 灵活性 | 可组合变换 | 固定 API |
| JVM 优化 | 可被内联 | 难以优化 |

```java
// 高性能场景推荐 MethodHandle
MethodHandle mh = lookup.findVirtual(...);
// mh.invoke() 可被 JIT 内联

// 简单场景反射更方便
Method m = clazz.getMethod("foo");
m.invoke(obj);  // 有安全检查开销
```

### 4. 对象内存布局优化

```java
// 问题：字段排列影响内存占用
class Unoptimized {
    byte a;    // 1 byte + 7 padding
    long b;    // 8 bytes
    byte c;    // 1 byte + 7 padding
    // 总计：24 bytes
}

// 优化：合理排列字段
class Optimized {
    long b;    // 8 bytes
    byte a;    // 1 byte
    byte c;    // 1 byte + 6 padding
    // 总计：16 bytes
}

// JVM 自动做字段重排（-XX:+CompactFields），但了解原理有助于极端优化
```

---

## 🎯 面试考点提炼

### 高频问题

1. **"方法调用指令有哪些？区别是什么？"**
   - invokestatic：静态方法
   - invokespecial：构造器、私有、super
   - invokevirtual：普通虚方法
   - invokeinterface：接口方法
   - invokedynamic：动态调用（Lambda）

2. **"静态分派和动态分派的区别？"**
   - 静态分派：编译期确定，看静态类型（重载）
   - 动态分派：运行期确定，看实际类型（重写）

3. **"Lambda 表达式底层实现原理？"**
   - 使用 invokedynamic 指令
   - 首次调用时 LambdaMetafactory 生成实现类
   - 后续调用复用生成的类

4. **"栈帧包含哪些内容？"**
   - 局部变量表、操作数栈、动态链接、返回地址

5. **"MethodHandle 和反射的区别？"**
   - MethodHandle 更轻量、可被 JIT 优化
   - 反射有安全检查开销，更灵活

### 进阶问题

6. **"什么是虚方法表？有什么作用？"**
   - 每个类维护的方法地址表
   - 加速 invokevirtual 方法查找

7. **"为什么接口调用比类调用慢？"**
   - 接口方法需要在 itable 中查找
   - 类方法在 vtable 中偏移量固定

8. **"对象头中 Mark Word 有什么内容？"**
   - 哈希码、GC 年龄、锁标志位
   - 偏向锁/轻量级锁状态

---

## 📚 相关资源

- 书籍章节: 《深入理解JVM》第8章 8.1-8.4
- 依赖: JOL (Java Object Layout) - `jol-core`
- 参考: JMH 基准测试框架
