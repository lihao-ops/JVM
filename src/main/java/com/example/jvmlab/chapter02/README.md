# 第2章 Java 内存区域与内存溢出异常

> **对应书籍**: 《深入理解Java虚拟机（第3版）》第2章  
> **核心主题**: 运行时数据区、对象创建与内存布局、OOM 实战

---

## 📖 核心内容概述

### 2.1 运行时数据区

```
┌─────────────────────────────────────────────────────────────┐
│                    JVM 运行时数据区                          │
├─────────────────────────────────────────────────────────────┤
│  线程共享区域                                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  堆 (Heap)                                          │   │
│  │  - 对象实例                                          │   │
│  │  - 数组                                              │   │
│  │  - 新生代 (Eden + S0 + S1) + 老年代                   │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  元空间 (Metaspace) - JDK8+                         │   │
│  │  - 类元信息                                          │   │
│  │  - 方法元数据                                        │   │
│  │  - 常量池                                            │   │
│  └─────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│  线程私有区域 (每个线程独立拥有)                              │
│  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐     │
│  │  程序计数器    │ │  虚拟机栈      │ │  本地方法栈    │     │
│  │  (PC Register)│ │ (VM Stack)    │ │(Native Stack) │     │
│  │  - 当前指令地址 │ │ - 栈帧        │ │ - Native 方法  │     │
│  │  - 线程切换恢复 │ │ - 局部变量表   │ │               │     │
│  └───────────────┘ └───────────────┘ └───────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 栈帧结构

```
┌─────────────────────────────────────┐
│            栈帧 (Stack Frame)        │
├─────────────────────────────────────┤
│  局部变量表 (Local Variable Table)   │
│  - Slot 为最小单位                   │
│  - long/double 占 2 个 Slot          │
├─────────────────────────────────────┤
│  操作数栈 (Operand Stack)            │
│  - 方法执行的工作区                   │
├─────────────────────────────────────┤
│  动态链接 (Dynamic Linking)          │
│  - 指向运行时常量池的方法引用          │
├─────────────────────────────────────┤
│  方法返回地址 (Return Address)        │
│  - 正常返回 / 异常返回                │
└─────────────────────────────────────┘
```

### 2.3 六种内存溢出场景

| 异常类型 | 触发条件 | 典型原因 |
| :--- | :--- | :--- |
| `Java heap space` | 堆内存耗尽 | 内存泄漏、大对象、缓存过大 |
| `StackOverflowError` | 栈深度超限 | 无限递归、栈太小 |
| `Metaspace` | 元空间耗尽 | 动态代理过多、类加载泄漏 |
| `Direct buffer memory` | 直接内存耗尽 | NIO 缓冲区未释放 |
| `unable to create new native thread` | 线程数超限 | 线程池失控、栈太大 |
| `GC overhead limit exceeded` | GC 效率过低 | 内存即将耗尽 |

---

## 💻 代码实践清单

### 实验1: 堆内存溢出

```bash
# VM 参数
-Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError

# 接口调用
GET /chapter02/heap-oom
```

**实验代码**: `Chapter02Controller.java:47`

```java
// 核心原理：静态集合持有对象，GC Roots 可达无法回收
List<byte[]> list = new ArrayList<>();
while (true) {
    list.add(new byte[1024]);  // 持续分配
}
```

### 实验2: 栈溢出

```bash
# VM 参数
-Xss128k

# 接口调用
GET /chapter02/stack-overflow
```

**实验代码**: `Chapter02Controller.java:78`

```java
// 核心原理：无终止递归导致栈帧耗尽
public void recursiveCall() {
    recursiveCall();  // 每次调用创建新栈帧
}
```

### 实验3: 元空间溢出

```bash
# VM 参数
-XX:MetaspaceSize=10m -XX:MaxMetaspaceSize=10m

# 接口调用
GET /chapter02/metaspace-oom
```

**实验代码**: `Chapter02Controller.java:145`

### 实验4: 直接内存溢出

```bash
# VM 参数
-XX:MaxDirectMemorySize=10m

# 接口调用
GET /chapter02/direct-memory-oom
```

**实验代码**: `Chapter02Controller.java:201`

### 实验5: 程序计数器观察

**实验代码**: `ProgramCounterTest.java`

```bash
# 1. 生成字节码
scripts/chapter02/view_bytecode_pc.bat

# 2. 在 IDEA 中 Debug，观察线程切换时指令位置变化
```

---

## 🏭 生产实践建议

### 1. 堆内存配置

```bash
# 生产推荐：Xms = Xmx 避免动态扩容
-Xms4g -Xmx4g

# 新生代配置
-Xmn1g  # 或 -XX:NewRatio=2 (老年代:新生代 = 2:1)

# OOM 时自动 dump
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/logs/heapdump.hprof
```

### 2. 栈配置与线程数估算

```bash
# 默认 1M，可适当减小
-Xss256k

# 最大线程数 ≈ (系统内存 - 堆 - 元空间) / Xss
# 例：8G 系统 - 4G 堆 - 512M 元空间 = 3.5G / 256K ≈ 14000 线程
```

### 3. 直接内存管理

```java
// 问题：DirectByteBuffer 依赖 GC 回收
ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);

// 最佳实践：使用池化或手动释放
// Netty: PooledByteBufAllocator
// 手动: ((DirectBuffer) buffer).cleaner().clean();
```

### 4. OOM 快速定位

```bash
# 1. 自动 dump
-XX:+HeapDumpOnOutOfMemoryError

# 2. 手动 dump
jmap -dump:format=b,file=heap.hprof <pid>

# 3. MAT 分析
# - Dominator Tree: 找出占用最大的对象
# - Leak Suspects: 自动分析泄漏嫌疑
# - GC Roots: 追溯引用链
```

---

## 🎯 面试考点提炼

### 高频问题

1. **"JVM 内存区域有哪些？哪些是线程共享的？"**
   - 线程共享：堆、元空间
   - 线程私有：程序计数器、虚拟机栈、本地方法栈

2. **"堆和栈的区别？"**
   - 堆：存对象，GC 管理，共享，大
   - 栈：存栈帧（局部变量），自动释放，私有，小

3. **"什么是栈帧？包含什么？"**
   - 方法调用的数据结构
   - 局部变量表、操作数栈、动态链接、返回地址

4. **"元空间和永久代的区别？"**
   - 永久代：JDK7 及之前，堆的一部分，受 -Xmx 限制
   - 元空间：JDK8+，使用本地内存，不受 -Xmx 限制

5. **"直接内存是什么？如何管理？"**
   - 堆外内存，通过 `Unsafe.allocateMemory` 分配
   - NIO 的 `DirectByteBuffer` 使用
   - 不受 GC 直接管理，通过 Cleaner 机制释放

### 进阶问题

6. **"OOM 有哪些类型？如何区分？"**
   - 看异常信息：heap space / Metaspace / Direct buffer
   - 分析 dump 文件确定具体原因

7. **"如何定位内存泄漏？"**
   - dump + MAT 分析
   - 找 Dominator Tree 大对象
   - 追溯 GC Roots 引用链

8. **"程序计数器为什么不会 OOM？"**
   - 它只是一个指针，指向当前指令地址
   - 空间极小且固定，不会溢出

---

## 📚 相关资源

- 书籍章节: 《深入理解JVM》第2章 2.1-2.4
- 脚本位置: `scripts/chapter02/`
- 异常场景: `exceptionlab/scenario/` 目录
