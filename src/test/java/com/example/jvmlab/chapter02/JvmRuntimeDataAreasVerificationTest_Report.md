# JVM 运行时数据区域全面验证测试报告

> **测试日期**: 2026-01-10 17:20:30  
> **测试环境**: JDK 21.0.6+8-LTS-188 (HotSpot 64-Bit Server VM)  
> **操作系统**: Windows 11 (amd64)  
> **对应书籍**: 《深入理解Java虚拟机（第3版）》第2章

---

## 📊 测试摘要

| 指标 | 结果 |
|------|------|
| **测试状态** | ✅ 全部通过 |
| **退出代码** | 0 |
| **测试耗时** | ~500ms |
| **验证区域** | 6 个核心区域 |

---

## 🏗️ JVM 基本信息

| 属性 | 值 |
|------|-----|
| VM 名称 | Java HotSpot(TM) 64-Bit Server VM |
| VM 版本 | 21.0.6+8-LTS-188 |
| VM 供应商 | Oracle Corporation |
| 启动时间 | 666 ms |

---

## 📋 验证结果详情

### 1. Java Heap (线程共享 - GC Managed)

| 指标 | 值 |
|------|-----|
| 已使用 | 6 MB → 14 MB |
| 最大值 | 12224 MB |
| 初始大小 | 768 MB |
| 已提交 | 768 MB |

**验证项目**:
- [x] Java Heap 存在性验证 ✓
- [x] 堆内存分配与 GC 管理验证 ✓
- [x] G1 垃圾收集器验证 ✓

### 2. Native Memory Areas (线程共享 - OS Managed)

#### 2.1 Metaspace
| 指标 | 值 |
|------|-----|
| 已使用 | 10134 KB → 10540 KB |
| 已提交 | 10816 KB |
| 最大值 | 无限制 (-1) |

**验证项目**:
- [x] Metaspace 区域存在验证 ✓
- [x] 类元数据存储验证 ✓

**类加载统计**:
| 指标 | 值 |
|------|-----|
| 已加载类 | 2461 → 2561 |
| 已卸载类 | 0 |

#### 2.2 Code Cache
| 区域 | 已使用 | 最大值 |
|------|--------|--------|
| CodeHeap 'non-nmethods' | 3137 KB | 7488 KB |
| CodeHeap 'profiled nmethods' | 4457 KB | 119104 KB |
| CodeHeap 'non-profiled nmethods' | 724 KB | 119168 KB |

**验证项目**:
- [x] Code Cache 区域存在验证 ✓
- [x] JIT 编译代码存储验证 ✓

### 3. Per-Thread Resources (线程私有)

| 指标 | 值 |
|------|-----|
| 当前线程数 | 8 |
| 峰值线程数 | 8 |
| 守护线程数 | 7 |

**验证项目**:
- [x] 线程栈私有性验证 ✓
- [x] 统一线程栈验证 (Java & Native Frames) ✓
- [x] PC 寄存器线程私有性验证 ✓
- [x] 私有资源线程隔离验证 ✓

### 4. Execution Engine (解释器 & JIT)

| 指标 | 值 |
|------|-----|
| 编译器名称 | HotSpot 64-Bit Tiered Compilers |
| 编译前累计时间 | 907 ms |
| 编译后累计时间 | 932 ms |
| 本次触发编译时间 | 8 ms |

**验证项目**:
- [x] 解释器模式存在验证 ✓
- [x] JIT 编译器工作验证 ✓

### 5. JNI (Native Interface)

**测试的 Native 方法**:
| 方法 | 结果 |
|------|------|
| `System.currentTimeMillis()` | 1768036830165 ms |
| `System.nanoTime()` | 248285404957600 ns |
| `Runtime.availableProcessors()` | 24 核 |
| `Runtime.freeMemory()` | 755 MB |
| `Runtime.maxMemory()` | 12224 MB |

**验证项目**:
- [x] JNI 本地接口验证 ✓

### 6. Native Method Libraries

**本地库搜索路径** (java.library.path):
- `D:\JDK21\bin`
- `C:\Windows\Sun\Java\bin`
- `C:\Windows\system32`
- ... 共 32 个路径

**验证项目**:
- [x] 本地方法库加载验证 ✓

---

## 🔄 GC 收集器状态

| 收集器 | 收集次数 | 收集时间 |
|--------|----------|----------|
| G1 Young Generation | 1 | 4 ms |
| G1 Concurrent GC | 0 | 0 ms |
| G1 Old Generation | 0 | 0 ms |

---

## 🧵 线程隔离验证

**多线程递归深度测试**:
| 线程名 | 递归深度 |
|--------|----------|
| Isolation-10 | 10 |
| Isolation-20 | 20 |
| Isolation-30 | 30 |

**共享堆内存访问测试**:
- 5 个线程共同操作的计数器值: 500
- 共享列表大小: 500

---

## 📐 栈帧信息示例

```
[NATIVE] sun.management.ThreadImpl#getThreadInfo1 (Native)
[JAVA]   sun.management.ThreadImpl#getThreadInfo:187
[JAVA]   JvmRuntimeDataAreasVerificationTest$PerThreadResourcesTests#testUnifiedThreadStack:501
[JAVA]   java.lang.invoke.LambdaForm$DMH/0x000001738d134000#invokeVirtual:-1
[JAVA]   jdk.internal.reflect.DirectMethodHandleAccessor#invokeImpl:153
...
```

---

## ✅ 总结

所有 JVM 运行时数据区域验证测试均已通过，验证了以下核心知识点：

1. **堆内存 (Heap)** - 对象实例分配、GC 管理正常
2. **元空间 (Metaspace)** - 类元数据存储、类加载正常
3. **代码缓存 (Code Cache)** - JIT 编译代码存储正常
4. **线程栈 (Thread Stack)** - 线程私有性、栈帧结构正常
5. **PC 寄存器** - 线程独立性验证正常
6. **执行引擎** - 解释器与 JIT 编译器正常工作
7. **JNI 接口** - Native 方法调用正常
8. **本地方法库** - 库加载正常

---

> **面试考点提示**:
> - Q: 为什么 PC 寄存器是唯一不会 OOM 的区域？  
>   A: 因为它只存储一个地址值，占用空间极小且固定。
> - Q: JDK 17+ 默认使用什么 GC？  
>   A: G1 GC，适合大堆内存，提供可预测的停顿时间。
