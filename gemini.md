# JVM 核心实战实验室 (JVM Core Lab) - 项目规范与指南

请以一个互联网大厂面试官的思想，你还熟读深入理解java虚拟机第三版，扫描整个项目的每个目录和结构以及文件，帮我优化和完善gemini.md，并且每个章节的目录下都要有一个说明文档说明本章节的核心内容并且说明在代码中可以实践什么？哪些可以应用到实际项目中。这些文件单独增加在每个细化的章节目录中

> **版本**: 2.0  
> **对应书籍**: 《深入理解Java虚拟机（第3版）》 (周志明 著)  
> **角色设定**: 阿里 P8 / 字节 2-2 架构师 & JVM 领域专家  
> **核心目标**: 拒绝死记硬背，通过代码实战"复现"底层原理，通过脚本"观测"虚拟机行为。

---

## 1. 项目架构与目录映射 (Project Structure)

本项目基于 Spring Boot 3.x + JDK 17 构建，每个章节对应一个独立的 package 和脚本目录。

### 📂 核心代码目录 (`src/main/java/com/example/jvmlab/`)

| 目录/包名 | 对应书籍章节 | 核心内容 & 实验类 | 面试考察点 |
| :--- | :--- | :--- | :--- |
| **`chapter01`** | **第1章 走近Java** | `Chapter01Controller`: JDK/JRE 关系, 混合模式<br>`JitPerformanceTest`: 解释器 vs JIT 性能对比 | JIT 编译阈值、混合模式原理、热点探测 |
| **`chapter02`** | **第2章 内存区域** | `Chapter02Controller`: 堆/栈/元空间 OOM 实战<br>`ProgramCounterTest`: PC 寄存器与线程切换 | 内存溢出排查、栈帧结构、直接内存、PC 寄存器特性 |
| **`chapter03`** | **第3章 GC与分配** | `Chapter03Controller`: 引用类型、Eden分配、年龄晋升<br>TLAB 演示、字符串去重 | CMS vs G1 vs ZGC、三色标记、跨代引用、分配担保 |
| **`chapter04`** | **第4章 监控工具** | `Chapter04Controller`: jstat, jstack 集成<br>`JvmMemoryMonitor`: 内存/GC/线程监控 | 线上 CPU 100% 排查、内存泄漏定位、Arthas 使用 |
| **`chapter05`** | **第5章 性能优化** | `Chapter05Controller`: CPU 热点、内存抖动模拟 | 调优方法论、性能指标采集、火焰图分析 |
| **`chapter06`** | **第6章 类文件结构** | `Chapter06Controller`: ASM 类结构解析 | Class 文件魔数、常量池、字段表、方法表 |
| **`chapter07`** | **第7章 类加载机制** | `Chapter07Controller`: 主动/被动引用、自定义 ClassLoader<br>双亲委派打破、类卸载 | 类加载时机、双亲委派模型、SPI 机制、热部署 |
| **`chapter08`** | **第8章 字节码执行** | `Chapter08Controller`: MethodHandle、invokedynamic<br>JOL 对象内存布局 | 方法调用指令、动态分派、Lambda 底层实现 |
| **`chapter09`** | **第9章 执行子系统实践** | `Chapter09Controller`: JDK 动态代理、ASM 类生成 | 代理模式实现原理、字节码增强框架对比 |
| **`chapter10`** | **第10章 编译期优化** | `Chapter10Controller`: 运行期动态编译与执行 | 语法糖、泛型擦除、条件编译 |
| **`chapter11`** | **第11章 运行期优化** | `Chapter11Controller`: JIT 预热、逃逸分析<br>锁竞争/偏向锁撤销 | 逃逸分析、标量替换、锁消除、OSR 编译 |

### 📂 专项实验模块

| 目录/包名 | 模块说明 | 核心功能 | 面试考察点 |
| :--- | :--- | :--- | :--- |
| **`exceptionlab`** | **异常场景实验室** | 8 种 OOM/SOF 场景的策略模式实现<br>支持 Dry-Run 指引与真实触发 | 各类 OOM 区分、排查思路、工具使用 |
| **`common`** | **公共工具** | `JvmMemoryMonitor`: 监控采集<br>`AsmDynamicClassBuilder`: 动态类生成<br>`GlobalExceptionHandler`: 统一异常处理 | 监控体系设计、字节码操作 |
| **`monitor`** | **运行时监控** | `RuntimeMonitorController`: 监控面板 | 可观测性建设 |
| **`config`** | **配置管理** | Spring 配置类 | 配置中心设计 |

### 📂 压力测试模块 (`jvmstress`)

| 目录/包名 | 模块说明 | 核心功能 |
| :--- | :--- | :--- |
| **`jvmstress/ctrl`** | **快速触发控制器** | `JvmErrorController`: 一键触发各类 OOM/栈/线程异常 |

### 📂 脚本工具目录 (`scripts/`)

为了方便在无 IDE 环境（模拟生产服务器）下验证，所有实验必须配备 `.bat` (Windows) 或 `.sh` (Linux) 脚本。

- **`scripts/chapter01/`**
  - `check_jit_threshold.bat`: 查看 JIT 编译阈值 (默认 10000)
  - `run_with_jit_log.bat`: 开启 `-XX:+PrintCompilation` 观察 JIT "代码雨"
- **`scripts/chapter02/`**
  - `view_bytecode_pc.bat`: 使用 `javap -c -l` 反汇编，配合 Debug 观察程序计数器

---

## 2. ExceptionLab 异常实验室详解

### 2.1 场景策略模式设计

`exceptionlab` 模块采用策略模式，所有场景实现 `MemoryExceptionScenario` 接口：

| 场景类 | 触发异常 | 核心原理 | 面试考点 |
| :--- | :--- | :--- | :--- |
| `HeapOomScenario` | `java.lang.OutOfMemoryError: Java heap space` | 静态集合持有对象，GC Roots 可达 | 堆内存溢出排查、MAT 分析 |
| `StackOverflowScenario` | `java.lang.StackOverflowError` | 无终止递归导致栈帧耗尽 | 栈深度与 `-Xss` 关系 |
| `MetaspaceOomScenario` | `java.lang.OutOfMemoryError: Metaspace` | ASM 动态生成类消耗元空间 | 元空间 vs 永久代、类卸载条件 |
| `DirectMemoryOomScenario` | `java.lang.OutOfMemoryError: Direct buffer memory` | `ByteBuffer.allocateDirect` 堆外内存 | 直接内存回收机制、NMT 分析 |
| `ThreadOomScenario` | `java.lang.OutOfMemoryError: unable to create new native thread` | 大量线程创建耗尽系统资源 | 线程数与栈大小权衡、ulimit |
| `StringPoolPressureScenario` | 堆内存压力 | 批量 `String.intern()` 增长常量池 | JDK7+ 常量池在堆中 |
| `GcOverheadScenario` | `java.lang.OutOfMemoryError: GC overhead limit exceeded` | GC 时间占比过高但回收效果差 | GC 效率与内存分配速率 |
| `ThreadLocalLeakScenario` | 内存泄漏（非立即 OOM） | ThreadLocal 值被静态集合间接持有 | Entry 弱引用机制、线程池复用问题 |

### 2.2 使用方式

```bash
# 列出所有场景
GET /memory-exception-lab/scenarios

# 查看场景详细指引（Dry-Run）
GET /memory-exception-lab/scenarios/{id}

# 执行实验（dryRun=false 真实触发）
POST /memory-exception-lab/scenarios/{id}/execute?dryRun=false
```

---

## 3. 代码开发规范 (Coding Standards)

### 3.1 实验类设计原则

所有的实验代码不是为了"跑通业务"，而是为了**"触发特定现象"**。

- **Controller 模式**: 对于 OOM、高并发测试，优先使用 `@RestController` 暴露接口，方便通过浏览器/Postman 触发
- **Main 方法模式**: 对于纯计算、性能对比（如 JIT）、底层指令观察（如 PC 寄存器），使用 `public static void main`

### 3.2 注释规范 (The "Why" over "What")

注释必须解释**背后的 JVM 原理**，采用面试官视角：

```java
/**
 * 【面试官视角】为什么 ThreadLocal 会导致内存泄漏？
 * 
 * 回答框架：
 * 1. 数据结构：ThreadLocalMap 使用 Entry[] 存储，Entry extends WeakReference<ThreadLocal>
 * 2. 泄漏原因：Key(ThreadLocal) 是弱引用会被 GC，但 Value 是强引用不会被回收
 * 3. 线程池场景：线程复用导致 Entry 永远不会被清理
 * 4. 解决方案：使用后必须调用 remove()
 * 
 * @see ThreadLocalLeakScenario 实验验证
 */
```

### 3.3 常用 JVM 参数 (VM Options)

在实验代码的类注释中，必须标注该实验需要的 VM 参数：

| 实验类型 | 推荐参数 | 说明 |
| :--- | :--- | :--- |
| **堆 OOM** | `-Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError` | 限制堆大小，自动生成 dump |
| **栈溢出** | `-Xss128k` | 减小栈空间加速溢出 |
| **元空间 OOM** | `-XX:MetaspaceSize=10m -XX:MaxMetaspaceSize=10m` | 限制元空间 |
| **直接内存** | `-XX:MaxDirectMemorySize=10m` | 限制堆外内存 |
| **JIT 观察** | `-XX:+PrintCompilation -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining` | 观察编译与内联 |
| **GC 日志** | `-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10m` | JDK 9+ 统一日志 |
| **逃逸分析** | `-XX:+DoEscapeAnalysis -XX:+EliminateAllocations` | 开启标量替换 |
| **锁优化** | `-XX:+UseBiasedLocking` (JDK15 前) | 偏向锁（JDK15+ 已废弃） |

---

## 4. 高频面试问题与回答框架

### 4.1 内存模型篇

| 问题 | 考察点 | 对应实验 | 回答要点 |
| :--- | :--- | :--- | :--- |
| "对象一定在堆上分配吗？" | 逃逸分析 | `Chapter11Controller.escapeAnalysis()` | 不一定。JIT 逃逸分析后，未逃逸对象可标量替换在栈上分配 |
| "堆和栈的区别？" | 内存区域 | `Chapter02Controller` | 堆：共享、存对象；栈：私有、存栈帧（局部变量表、操作数栈等） |
| "元空间和永久代区别？" | JDK8 变化 | `MetaspaceOomScenario` | 元空间用本地内存，不受 `-Xmx` 限制，类卸载条件不变 |

### 4.2 GC 篇

| 问题 | 考察点 | 对应实验 | 回答要点 |
| :--- | :--- | :--- | :--- |
| "CMS 和 G1 的区别？" | 垃圾收集器 | `Chapter03Controller` | CMS 标记清除有碎片；G1 Region 化，可预测停顿 |
| "三色标记是什么？" | 并发标记 | `Chapter03Controller` | 白（未访问）、灰（访问中）、黑（已完成）；SATB/增量更新解决漏标 |
| "对象进入老年代的条件？" | 分代策略 | `testTenuring()` / `testDynamicAge()` | 年龄阈值（15）、大对象直接入老、动态年龄判定 |

### 4.3 类加载篇

| 问题 | 考察点 | 对应实验 | 回答要点 |
| :--- | :--- | :--- | :--- |
| "双亲委派模型能打破吗？" | 类加载器 | `Chapter07Controller.testBreakParentDelegation()` | 可以。重写 `loadClass()` 跳过父加载器；SPI、OSGI、热部署都打破 |
| "类的主动使用有哪些？" | 初始化时机 | `Chapter07Controller` | new、静态字段/方法、反射、main 类、子类触发父类 |
| "如何实现热部署？" | 类卸载 | `testClassUnloading()` | 自定义 ClassLoader + 类实例/ClassLoader/Class 都无引用 |

### 4.4 性能调优篇

| 问题 | 考察点 | 对应实验 | 回答要点 |
| :--- | :--- | :--- | :--- |
| "线上 CPU 100% 怎么排查？" | 故障定位 | `Chapter04Controller` / `Chapter05Controller` | top→jstack→定位线程→分析代码；或 Arthas thread 命令 |
| "内存泄漏怎么定位？" | 堆分析 | `HeapOomScenario` | jmap dump→MAT 分析 Dominator Tree→追溯 GC Roots |
| "Full GC 频繁怎么办？" | GC 调优 | `Chapter03Controller` | 增大堆/检查大对象/调整晋升阈值/更换 G1 或 ZGC |

---

## 5. JVM 调优实战场景

### 5.1 堆内存调优黄金比例

```bash
# 生产环境推荐配置
-Xms4g -Xmx4g              # 堆初始=最大，避免扩容开销
-Xmn1g                      # 新生代 1/4 ~ 1/3
-XX:SurvivorRatio=8         # Eden:Survivor = 8:1:1
-XX:MaxTenuringThreshold=15 # 晋升年龄阈值
```

### 5.2 GC 选择策略

| 场景 | 推荐 GC | 参数 |
| :--- | :--- | :--- |
| 吞吐量优先（批处理） | Parallel GC | `-XX:+UseParallelGC` |
| 低延迟（Web 服务） | G1 / ZGC | `-XX:+UseG1GC -XX:MaxGCPauseMillis=200` |
| 超大堆（>32G） | ZGC / Shenandoah | `-XX:+UseZGC` |

### 5.3 元空间调优

```bash
-XX:MetaspaceSize=256m      # 初始大小（触发 Full GC 的阈值）
-XX:MaxMetaspaceSize=512m   # 最大限制（防止内存泄漏）
-XX:MinMetaspaceFreeRatio=40
-XX:MaxMetaspaceFreeRatio=70
```

### 5.4 线程栈调优

```bash
-Xss256k                    # 默认 1M，可适当减小
# 线程数估算：(系统内存 - 堆 - 元空间) / Xss
```

---

## 6. 交互与回答规范 (Interaction Protocol)

当用户提问时，AI 助手应遵循以下逻辑：

1. **场景代入 (Context)**: 先判断用户遇到的问题属于哪个 JVM 内存区域或生命周期阶段
2. **原理降维 (Analogy)**: 使用"厨房理论"、"龟兔赛跑"等比喻进行通俗解释
3. **实战验证 (Action)**: **这是最重要的**。不要只给理论，要给出：
    * 对应的代码位置（如 `chapter07/Chapter07Controller.java`）
    * 对应的脚本位置（如 `scripts/chapter01/run_with_jit_log.bat`）
    * 预期的控制台输出或现象
4. **面试考点 (Interview Checkpoint)**: 指出该知识点在面试中通常怎么问

---

## 7. 当前项目进度检查点 (Checkpoints)

### 第一部分：走近 Java (第 1-4 章)
- [x] **第1章**: JDK/JRE/JVM 区别、混合模式、JIT 性能对比
- [x] **第2章**: 运行时数据区框架、6 种 OOM 场景实战
- [x] **第3章**: GC 算法、四种引用类型、TLAB、字符串去重
- [x] **第4章**: 监控工具集成、JvmMemoryMonitor 封装

### 第二部分：虚拟机执行子系统 (第 5-8 章)
- [x] **第5章**: 性能优化（CPU 热点、内存抖动）
- [x] **第6章**: 类文件结构（ASM 解析）
- [x] **第7章**: 类加载机制（主动/被动引用、自定义 ClassLoader、类卸载）
- [x] **第8章**: 字节码执行引擎（MethodHandle、invokedynamic、JOL）

### 第三部分：程序编译与优化 (第 9-11 章)
- [x] **第9章**: 动态代理（JDK 代理 + ASM 类生成）
- [x] **第10章**: 编译期优化（运行期动态编译）
- [x] **第11章**: JIT 优化（预热、逃逸分析、锁竞争）

### 专项模块
- [x] **ExceptionLab**: 8 种异常场景策略模式实现
- [x] **JvmStress**: 一键触发控制器（11 个接口）
- [x] **Monitor**: RuntimeMonitorController
- [x] **Common**: JvmMemoryMonitor + AsmDynamicClassBuilder

---

## 8. 各章节 README 说明文档

每个章节目录下均有 `README.md`，包含：
- 📖 **核心内容概述**: 本章节覆盖的 JVM 知识点
- 💻 **代码实践清单**: 可运行的实验接口与方法
- 🏭 **生产实践建议**: 如何应用到实际项目中
- 🎯 **面试考点提炼**: 本章节高频面试问题

详见各 `chapter*/README.md` 文件。

---

> **致开发者**:  
> JVM 不再是黑盒。通过本项目，我们要把每一个参数、每一次 GC、每一行字节码都变得"肉眼可见"。  
> Keep Coding, Keep Tuning.
