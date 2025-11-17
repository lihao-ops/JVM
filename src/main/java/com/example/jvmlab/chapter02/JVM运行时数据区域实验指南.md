# JVM运行时数据区域深度实验指南

> 基于《深入理解Java虚拟机：JVM高级特性与最佳实践（第3版）》第2章
>
> **作者**：周志明  
> **对应章节**：第2章 Java内存区域与内存溢出异常

---

## 📚 实验概述

本实验平台提供了完整的JVM运行时数据区域实践环境，通过真实的代码和可观测的现象，帮助你深入理解JVM内存模型。

### 涵盖的知识点

1. **程序计数器**（Program Counter Register）
2. **虚拟机栈**（VM Stack）
3. **本地方法栈**（Native Method Stack）
4. **堆**（Heap）⭐ 重点
5. **方法区/元空间**（Method Area/Metaspace）⭐ 重点
6. **直接内存**（Direct Memory）

---

## 🛠️ 实验环境准备

### 1. JVM参数配置

在启动SpringBoot应用前，必须配置以下JVM参数（IDEA配置或命令行启动）：
```bash
-Xms512m
-Xmx512m
-Xmn256m
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=256m
-Xss512k
-XX:MaxDirectMemorySize=256m
-Xlog:gc*,gc+cpu=debug:file=./logs/gc.log:time,uptime,level,tags
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=./dumps
-XX:+UseG1GC
```

```bash

# ===================== 堆内存（Heap） =====================
-Xms512m                     # 初始堆大小（越大越减少扩容停顿）
-Xmx512m                     # 最大堆大小（与 Xms 相同避免动态扩容）
-Xmn256m                     # 新生代大小，控制 Young GC 频率（G1 中为建议值）

# ===================== 元空间（Metaspace） =====================

-XX:MetaspaceSize=128m       # 初始元空间大小（达到后触发首次 GC）
-XX:MaxMetaspaceSize=256m    # 最大元空间，避免类加载过多占爆内存

# ===================== 线程栈（Stack） =====================
-Xss512k                     # 每个线程栈大小，大线程池时要留意内存占用

# ===================== 直接内存（Direct Memory） =====================
-XX:MaxDirectMemorySize=256m # 最大直接内存（Netty / NIO 堆外内存依赖）

# ===================== GC 日志（JDK 17+ 正确写法） =====================
# -Xlog 是 JDK9+ 新日志系统，统一覆盖所有 GC 日志打印，
# “gc*” 打印所有 GC 事件，“gc+cpu” 打印 GC 期间 CPU 信息，
# 输出到 ./logs/gc.log，追加时间戳、运行时长、日志级别、标签。
-Xlog:gc*,gc+cpu=debug:file=./logs/gc.log:time,uptime,level,tags

# ===================== OOM 处理 =====================
-XX:+HeapDumpOnOutOfMemoryError   # OOM 时自动生成 heap dump（排查内存泄漏必备）
-XX:HeapDumpPath=./dumps          # dump 文件输出路径

# ===================== 垃圾回收器 =====================
-XX:+UseG1GC                      # 使用 G1 垃圾收集器（JDK17 默认，低延迟）

```


### 2. 工具准备

- **JProfiler**（推荐）：实时监控内存变化
- **JVisualVM**：JDK自带，免费
- **Arthas**：阿里开源，适合线上诊断
- **MAT**（Memory Analyzer Tool）：分析heap dump文件
- **Postman/curl**：调用REST接口

### 3. 启动应用

```bash
mvn spring-boot:run
# 或
java -jar jvm-experiment.jar
```

访问首页查看所有实验接口：
```
http://localhost:8080/jvm-experiment/
```

---

## 📖 实验指南

### 🎯 推荐的实验顺序

```
第一步：观察初始状态（必做）
  ↓
第二步：理解程序计数器（理论）
  ↓
第三步：虚拟机栈实验
  ↓
第四步：本地方法栈实验
  ↓
第五步：堆内存实验（重点）
  ↓
第六步：方法区/元空间实验（重点）
  ↓
第七步：直接内存实验
  ↓
第八步：综合实战演练
```

---

## 📝 详细实验步骤

### 【第一步】观察初始状态（必做）

#### 对应书籍：第2章 开篇

#### 实验目的
建立baseline，了解应用启动后各内存区域的初始状态。

#### 操作步骤

1. **启动JProfiler并attach到应用进程**
2. **调用接口获取初始状态**
   ```bash
   GET http://localhost:8080/jvm-experiment/all/memory-status
   ```

3. **在JProfiler中记录以下信息**：
    - Memory视图：堆内存分布（Eden、Survivor、Old）
    - Heap Walker：当前对象数量和大小
    - Telemetries：内存使用曲线
    - Threads：线程数量

#### 观察重点
- 堆内存使用率（通常<10%）
- 已加载类数量（SpringBoot框架类）
- 线程数（10-30个是正常的）

---

### 【第二步】程序计数器（理论学习）

#### 对应书籍：第2.2.1节

#### 实验目的
理解程序计数器的概念和作用（无法直接观测）。

#### 操作步骤
```bash
GET http://localhost:8080/jvm-experiment/program-counter/explain
```

#### 核心知识点
1. **线程私有**：每个线程都有独立的程序计数器
2. **记录位置**：当前线程执行的字节码行号
3. **唯一不会OOM**：JVM规范中唯一没有规定OutOfMemoryError的区域

#### 为什么无法观测？
程序计数器是JVM内部使用的寄存器级别结构，不暴露给用户态。

#### 扩展学习
使用 `javap -c` 命令查看字节码，理解程序计数器如何指向字节码指令：
```bash
javac YourClass.java
javap -c YourClass.class
```

---

### 【第三步】虚拟机栈实验

#### 对应书籍：第2.2.2节

#### 📌 实验3.1：局部变量表

**实验目的**：观察局部变量表占用栈帧空间

**对应知识点**：
- Slot：局部变量表的容量单位
- long和double占用2个Slot
- 其他类型占用1个Slot

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/stack/local-variable-table?depth=10
```

**观察方法**：
1. JProfiler的Threads视图查看栈深度
2. 使用 `javap -v` 查看字节码中的LocalVariableTable

**预期结果**：成功执行，理解局部变量在栈中的存储

---

#### 📌 实验3.2：操作数栈

**实验目的**：理解操作数栈的入栈出栈过程

**对应知识点**：
- 操作数栈保存计算过程的中间结果
- 栈深度在编译期确定

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/stack/operand-stack?operations=100
```

**观察方法**：
使用 `javap -v` 查看字节码中的 `stack=?` 值

---

#### 📌 实验3.3：动态连接

**实验目的**：理解符号引用转换为直接引用的过程

**对应知识点**：
- 每个栈帧包含指向运行时常量池的引用
- 支持方法调用的动态连接

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/stack/dynamic-linking
```

---

#### 📌 实验3.4：StackOverflowError ⚠️

**实验目的**：触发栈溢出异常

**对应知识点**：
- 栈深度超过虚拟机允许的深度
- -Xss 参数影响栈大小

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/stack/stack-overflow?depth=10000
```

**观察方法**：
1. 查看异常堆栈信息
2. JProfiler观察栈深度
3. 查看达到的递归深度

**预期结果**：抛出 `StackOverflowError`

**排查思路**：
- 检查是否有无限递归
- 评估是否需要增大-Xss参数
- 优化递归算法（改为循环）

---

#### 📌 实验3.5：线程栈内存

**实验目的**：验证每个线程都有独立的虚拟机栈

**对应知识点**：
- 虚拟机栈是线程私有的
- 总栈内存 = 线程数 × 栈大小

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/stack/thread-stack-memory?threadCount=100
```

**观察方法**：
1. JProfiler的Threads视图观察线程数
2. 系统内存监控观察非堆内存变化

**预期结果**：创建N个线程，非堆内存增加约 N × Xss

---

### 【第四步】本地方法栈实验

#### 对应书籍：第2.2.3节

#### 实验目的
理解本地方法栈的作用（为Native方法服务）

#### 操作步骤

1. **查看原理说明**
   ```bash
   GET http://localhost:8080/jvm-experiment/native-stack/explain
   ```

2. **调用Native方法**
   ```bash
   POST http://localhost:8080/jvm-experiment/native-stack/native-method-call
   ```

#### 核心知识点
- HotSpot虚拟机将本地方法栈和虚拟机栈合二为一
- Native方法用C/C++实现
- 常见Native方法：`System.currentTimeMillis()`, `Object.hashCode()`, `System.arraycopy()`

---

### 【第五步】堆内存实验 ⭐ 重点

#### 对应书籍：第2.2.1节

#### 📌 实验5.1：查看堆内存信息

**操作步骤**：
```bash
GET http://localhost:8080/jvm-experiment/heap/info
GET http://localhost:8080/jvm-experiment/heap/explain
```

**观察重点**：
- 最大堆内存（-Xmx设置的值）
- 新生代（Eden + Survivor）
- 老年代（Old Generation）
- 各区域使用情况

---

#### 📌 实验5.2：新生代对象分配

**实验目的**：观察新生代的对象分配过程

**对应知识点**：
- 新对象优先在Eden区分配
- Eden区满时触发Minor GC
- 存活对象移动到Survivor区

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/heap/allocate-young-gen?objectCount=10000
```

**观察方法**：
1. JProfiler的Memory视图：观察Eden区使用情况
2. GC日志：查看是否触发Minor GC
3. Telemetries：观察内存曲线

**预期结果**：Eden区使用率快速上升

---

#### 📌 实验5.3：大对象直接进老年代

**实验目的**：验证大对象分配机制

**对应知识点**：
- 大对象直接进入老年代
- 避免在Survivor区来回复制
- -XX:PretenureSizeThreshold 参数控制阈值

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/heap/allocate-old-gen?sizeMB=100
```

**观察方法**：
JProfiler观察Old Generation使用率直接上升

**预期结果**：老年代使用量增加，新生代基本不变

---

#### 📌 实验5.4：触发Minor GC

**实验目的**：观察新生代GC过程

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/heap/trigger-minor-gc
```

**观察方法**：
1. GC日志中查看Minor GC记录
2. JProfiler观察Eden区内存回收
3. 观察Survivor区对象晋升

**预期结果**：Eden区内存被回收

---

#### 📌 实验5.5：触发Full GC

**实验目的**：观察Full GC的过程和影响

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/heap/trigger-full-gc
```

**观察方法**：
1. GC日志中的Full GC记录
2. 注意STW（Stop-The-World）时间
3. 观察整个堆的清理

**预期结果**：整个堆被清理，停顿时间较长

---

#### 📌 实验5.6：堆内存溢出 ⚠️

**实验目的**：演示 `OutOfMemoryError: Java heap space`

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/heap/heap-oom
```

**观察方法**：
1. 查看异常堆栈
2. 分析heap dump文件（自动生成）
3. JProfiler观察内存持续增长

**预期结果**：抛出 `OutOfMemoryError: Java heap space`

**排查思路**：
1. 使用MAT打开heap dump文件
2. 查看Histogram找出占用内存最多的类
3. 使用Dominator Tree分析对象引用链
4. 检查是否存在内存泄漏
5. 评估是否需要增大-Xmx参数

---

#### 📌 实验5.7：清理堆内存

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/heap/clear
```

---

### 【第六步】方法区/元空间实验 ⭐ 重点

#### 对应书籍：第2.2.5节

#### 📌 实验6.1：查看元空间信息

**操作步骤**：
```bash
GET http://localhost:8080/jvm-experiment/metaspace/info
GET http://localhost:8080/jvm-experiment/metaspace/explain
```

**观察重点**：
- 初始大小（-XX:MetaspaceSize）
- 最大大小（-XX:MaxMetaspaceSize）
- 已加载类数量

---

#### 📌 实验6.2：动态加载类

**实验目的**：观察类加载对元空间的影响

**对应知识点**：
- 每个类的元数据存储在元空间
- 类加载越多，元空间使用越多

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/metaspace/load-classes?count=1000
```

**观察方法**：
1. JProfiler观察Metaspace增长
2. 观察类加载数量变化

**预期结果**：元空间使用量增加

---

#### 📌 实验6.3：运行时常量池

**实验目的**：理解字符串常量池的工作机制

**对应知识点**：
- JDK7后，字符串常量池从永久代移到堆中
- String.intern()方法操作常量池

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/metaspace/constant-pool?stringCount=100000
```

**观察方法**：观察堆内存变化（不是元空间）

---

#### 📌 实验6.4：元空间溢出 ⚠️

**实验目的**：演示 `OutOfMemoryError: Metaspace`

**前提条件**：需要设置较小的MaxMetaspaceSize才容易观察到

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/metaspace/metaspace-oom
```

**预期结果**：抛出 `OutOfMemoryError: Metaspace`

**排查思路**：
- 检查是否动态生成大量类
- 检查是否有类加载器泄漏
- 使用JProfiler查看加载的类
- 考虑增大-XX:MaxMetaspaceSize

---

### 【第七步】直接内存实验

#### 对应书籍：第2.2.6节

#### 📌 实验7.1：分配直接内存

**实验目的**：理解DirectByteBuffer使用堆外内存

**操作步骤**：
```bash
GET http://localhost:8080/jvm-experiment/direct-memory/explain
POST http://localhost:8080/jvm-experiment/direct-memory/allocate?sizeMB=50
```

**观察方法**：
1. 堆内存基本不变
2. 系统监控观察进程内存增长
3. JProfiler无法直接观测直接内存

---

#### 📌 实验7.2：直接内存溢出 ⚠️

**实验目的**：演示 `OutOfMemoryError: Direct buffer memory`

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/direct-memory/direct-oom
```

**预期结果**：抛出 `OutOfMemoryError: Direct buffer memory`

---

### 【第八步】综合实战演练

#### 📌 实验8.1：模拟生产环境内存泄漏

**实验目的**：学习内存泄漏的排查思路

**泄漏场景**：
- 静态集合持续添加对象不清理
- ThreadLocal使用后未清理

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/comprehensive/simulate-production-leak
```

**排查方法**：
1. JProfiler的Heap Walker查看对象数量
2. 使用'Biggest Objects'找出大对象
3. 查看'Incoming References'分析引用链
4. 找到持有对象的根源

---

#### 📌 实验8.2：模拟高并发场景

**实验目的**：观察高并发下各内存区域的表现

**操作步骤**：
```bash
POST http://localhost:8080/jvm-experiment/comprehensive/simulate-high-concurrency
```

**观察重点**：
- 堆内存分配速度
- GC触发频率
- 线程数变化
- 响应时间

---

#### 📌 实验8.3：生成内存分析报告

**操作步骤**：
```bash
GET http://localhost:8080/jvm-experiment/comprehensive/generate-report
```

---

## 🎓 学习建议

### 1. 理论 + 实践结合
- 先阅读书中对应章节
- 再执行相应实验
- 对比理论和实际现象

### 2. 工具熟练使用
- JProfiler的各个视图都要熟悉
- 学会分析heap dump
- 掌握GC日志分析

### 3. 实验记录
建议建立实验笔记，记录：
- 实验前的内存状态
- 实验操作
- 观察到的现象
- 与理论的对比
- 自己的理解和疑问

### 4. 深入源码
对感兴趣的部分，可以：
- 查看字节码（javap -v）
- 阅读JVM源码
- 研究GC日志

---

## 🔍 常见问题

### Q1: 为什么有些实验看不到明显效果？
A: 可能原因：
- JVM参数设置过大
- JIT编译器优化
- GC策略不同

建议：适当减小内存参数，更容易观察现象

### Q2: OOM实验会影响应用吗？
A: OOM实验会导致应用崩溃，建议：
- 在测试环境执行
- 保存好当前数据
- 执行OOM实验后重启应用

### Q3: JProfiler连接不上怎么办？
A: 检查：
- 应用是否正常启动
- 端口是否被占用
- 防火墙设置

### Q4: 如何分析heap dump文件？
A: 推荐使用：
- Eclipse MAT（Memory Analyzer Tool）
- JProfiler的Open Snapshot功能
- VisualVM的Load功能

---

## 📚 参考资料

1. **书籍**
    - 《深入理解Java虚拟机（第3版）》周志明著
    - 《Java性能优化权威指南》
    - 《Java性能调优实战》

2. **在线资源**
    - [Oracle官方JVM文档](https://docs.oracle.com/javase/specs/jvms/se11/html/)
    - [JVM参数大全](https://www.oracle.com/java/technologies/javase/vmoptions-jsp.html)

3. **工具文档**
    - [JProfiler官方文档](https://www.ej-technologies.com/products/jprofiler/overview.html)
    - [Arthas用户文档](https://arthas.aliyun.com/doc/)

---

## 💡 进阶建议

完成本章实验后，可以继续学习：

1. **第3章**：垃圾收集器与内存分配策略
2. **第4章**：虚拟机性能监控与故障处理工具
3. **第5章**：调优案例分析与实战

祝学习顺利！💪