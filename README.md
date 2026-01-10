# JVM 学习实验室：实验代码与《深入理解 Java 虚拟机（第3版）》章节映射

> 说明：本文件逐章列出项目中的类与方法如何对应原书的知识点，给出每个实验的目的与论证结论，便于学习与复盘。路径后附带 `file_path:line_number` 便于快速定位代码。

## 第1章 走近 Java
- `Chapter01Controller` 概念演示（JDK/JRE/JVM、混合模式）
  - JDK/JRE/JVM 关系：`src/main/java/com/example/jvmlab/chapter01/Chapter01Controller.java:28`
    - 目的：通过比喻（厨房理论）清晰区分三者概念
    - 论证：生产环境排查问题通常需要 JDK 工具，仅运行则 JRE 足矣
  - 混合模式（Mixed Mode）：`src/main/java/com/example/jvmlab/chapter01/Chapter01Controller.java:48`
    - 目的：展示 HotSpot 虚拟机默认的运行模式（解释器 + JIT）
    - 论证：解释器保证启动响应，JIT 保证热点代码执行效率
  - 热点代码触发：`src/main/java/com/example/jvmlab/chapter01/Chapter01Controller.java:68`
    - 目的：模拟热点代码执行，配合 `-XX:+PrintCompilation` 可观察 JIT 编译行为
- `JitPerformanceTest` 性能对比实战
  - 解释器 vs JIT：`src/main/java/com/example/jvmlab/chapter01/JitPerformanceTest.java`
    - 目的：通过 CPU 密集型任务，直观对比纯解释模式 (-Xint) 与混合模式的性能差异
    - 实验方法：在 IDEA 中分别配置 VM 参数 `-Xint` 和 `-XX:+PrintCompilation` 运行

## 第2章 运行时数据区与对象内存
- `Chapter02Controller` 实验入口（堆/栈/线程/元空间/常量池/直接内存）
  - 堆 OOM：`src/main/java/com/example/jvmlab/chapter02/Chapter02Controller.java:47`
    - 对应：Java 堆（Heap）
    - 实验目的：通过静态集合持有对象触发 `Java heap space`，观察 heap dump 与引用链
    - 论证：集合持有与对象生命周期管理是堆 OOM 的常见根因
  - 栈溢出：`src/main/java/com/example/jvmlab/chapter02/Chapter02Controller.java:78`
    - 对应：线程私有 - 虚拟机栈
    - 目的：无终止条件递归触发 `StackOverflowError`，观察最大深度与 `-Xss`
    - 论证：递归终止条件与栈容量直接决定栈溢出风险
  - 线程资源耗尽：`src/main/java/com/example/jvmlab/chapter02/Chapter02Controller.java:109`
    - 对应：本地线程资源/线程栈
    - 目的：大量创建非守护线程触发 `unable to create new native thread`
    - 论证：线程上限受 OS 资源与 `-Xss` 大小共同影响
  - 元空间 OOM：`src/main/java/com/example/jvmlab/chapter02/Chapter02Controller.java:145`
    - 对应：方法区/元空间（JDK8+）
    - 目的：动态生成大量类消耗 Metaspace
    - 论证：ClassLoader/类引用的释放决定元空间回收
  - 字符串常量池压力：`src/main/java/com/example/jvmlab/chapter02/Chapter02Controller.java:176`
    - 对应：常量池（JDK8 后位于堆）
    - 目的：`intern()` 增长常量池并与业务对象竞争堆空间
    - 论证：过度 `intern` 易造成堆压力与 OOM
  - 直接内存 OOM：`src/main/java/com/example/jvmlab/chapter02/Chapter02Controller.java:201`
    - 对应：直接内存（堆外）
    - 目的：`ByteBuffer.allocateDirect` 持续分配触发 OOM
    - 论证：`-XX:MaxDirectMemorySize` 与引用释放影响堆外内存
  - 状态重置：`src/main/java/com/example/jvmlab/chapter02/Chapter02Controller.java:225`
    - 目的：清理静态集合、触发 GC，便于复实验
  - 监控查询：`src/main/java/com/example/jvmlab/chapter02/Chapter02Controller.java:241`
    - 对应：与第4章工具联动，输出内存结构化信息
- `ProgramCounterTest` 程序计数器实战
  - 模拟上下文切换：`src/main/java/com/example/jvmlab/chapter02/ProgramCounterTest.java`
    - 目的：通过 Debugger 模拟线程切换，结合 javap 观察 PC 寄存器（字节码行号）的变化
    - 实验方法：运行 `view_bytecode_pc.bat` 获取字节码，然后在 IDEA 中 Debug 并切换线程
- `StackOverflowTest` 栈溢出实战
  - 递归导致 SOF：`src/main/java/com/example/jvmlab/chapter02/stack/StackOverflowTest.java`
    - 目的：演示无限递归如何撑爆虚拟机栈
    - 实验方法：运行 `scripts/chapter02/run_sof_test.bat` (参数 -Xss160k)
- `ThreadOomTest` 线程耗尽实战
  - 多线程导致 OOM：`src/main/java/com/example/jvmlab/chapter02/stack/ThreadOomTest.java`
    - 目的：演示创建过多线程导致操作系统内存耗尽 (unable to create new native thread)
    - 实验方法：运行 `scripts/chapter02/run_thread_oom_test.bat` (参数 -Xss2M)
- `HeapOomTest` 堆溢出实战
  - 堆内存耗尽：`src/main/java/com/example/jvmlab/chapter02/heap/HeapOomTest.java`
    - 目的：演示不断创建对象且不释放，最终导致 Java heap space 溢出
    - 实验方法：运行 `scripts/chapter02/run_heap_oom_test.bat` (参数 -Xms20m -Xmx20m)
- `StackLabController` 栈帧深潜实战
  - 栈帧入栈出栈观察：`src/main/java/com/example/jvmlab/chapter02/stack/StackLabController.java`
    - 目的：通过 Debugger 观察 Frames 列表变化，验证栈帧的 Push/Pop 过程
    - 实验方法：运行 `scripts/chapter02/run_stack_debug.bat`，访问 `/jvm/stack/debug`

- 异常场景（策略实现）
  - 堆 OOM：`src/main/java/com/example/jvmlab/exceptionlab/scenario/HeapOomScenario.java:90`
    - 目的：线程安全集合持有字节块，控制大小与速率
    - 论证：对象保留链导致堆无法回收
  - 直接内存 OOM：`src/main/java/com/example/jvmlab/exceptionlab/scenario/DirectMemoryOomScenario.java:88`
    - 目的：分配 DirectByteBuffer 至 OOM，结合 NMT 分析
  - 元空间 OOM：`src/main/java/com/example/jvmlab/exceptionlab/scenario/MetaspaceOomScenario.java:92`
    - 目的：ASM 动态生成类并保留引用至 OOM
  - 字符串常量池压力：`src/main/java/com/example/jvmlab/exceptionlab/scenario/StringPoolPressureScenario.java:87`
    - 目的：批量 `intern` 字符串增长常量池
  - 线程资源耗尽：`src/main/java/com/example/jvmlab/exceptionlab/scenario/ThreadOomScenario.java:85`
    - 目的：非守护线程保持睡眠，触发线程资源上限
  - 栈溢出：`src/main/java/com/example/jvmlab/exceptionlab/scenario/StackOverflowScenario.java:87`
    - 目的：无终止递归触发 `StackOverflowError`
  - ThreadLocal 泄漏：`src/main/java/com/example/jvmlab/exceptionlab/scenario/ThreadLocalLeakScenario.java:63`
    - 对应：内存泄漏专题（第2/3章相关讨论）
    - 目的：ThreadLocal 值被静态集合间接持有导致无法回收
    - 论证：及时 `ThreadLocal.remove()` 与避免静态持有是关键

## 第3章 垃圾收集器与内存分配策略
- 控制器实验
  - 引用类型：`src/main/java/com/example/jvmlab/chapter03/Chapter03Controller.java:71`
    - 目的：强/软/弱/虚引用的回收特性对比
  - Eden 分配：`src/main/java/com/example/jvmlab/chapter03/Chapter03Controller.java:134`
    - 目的：小对象在新生代分配与日志观察
  - 大对象直接进入老年代：`src/main/java/com/example/jvmlab/chapter03/Chapter03Controller.java:153`
  - 年龄晋升：`src/main/java/com/example/jvmlab/chapter03/Chapter03Controller.java:168`
  - 动态年龄判定：`src/main/java/com/example/jvmlab/chapter03/Chapter03Controller.java:187`
  - 分配担保机制：`src/main/java/com/example/jvmlab/chapter03/Chapter03Controller.java:206`
  - 手动触发 GC：`src/main/java/com/example/jvmlab/chapter03/Chapter03Controller.java:235`
  - GC 统计：`src/main/java/com/example/jvmlab/chapter03/Chapter03Controller.java:263`
  - TLAB 演示：`src/main/java/com/example/jvmlab/chapter03/Chapter03Controller.java:279`
    - 目的：小对象分配下的 TLAB 效果与耗时观察，建议对比 `-XX:+/-UseTLAB`
  - 字符串去重：`src/main/java/com/example/jvmlab/chapter03/Chapter03Controller.java:309`
    - 目的：G1 下 String Deduplication 效果，建议启用 `-XX:+UseStringDeduplication`

- GC Overhead 限制：`src/main/java/com/example/jvmlab/exceptionlab/scenario/GcOverheadScenario.java:88`
  - 目的：高频 GC 且回收无效，触发 `GC overhead limit exceeded`
  - 论证：持续高位堆占用与频繁 GC 是该异常的触发基础

## 第4章 监控与故障处理工具
- JVM 参数：`src/main/java/com/example/jvmlab/chapter04/Chapter04Controller.java:35`
- 线程快照（jstack）：`src/main/java/com/example/jvmlab/chapter04/Chapter04Controller.java:48`
-监控聚合：`src/main/java/com/example/jvmlab/chapter04/Chapter04Controller.java:65`
- 运行时监控总览：`src/main/java/com/example/jvmlab/monitor/RuntimeMonitorController.java:31`
- 监控工具：`src/main/java/com/example/jvmlab/common/JvmMemoryMonitor.java:33/145/200/285`
  - `printMemoryInfo`、`getMemoryInfoMap`、`getGCStats`、`printJvmArguments`
- 全局异常处理器：`src/main/java/com/example/jvmlab/common/GlobalExceptionHandler.java:1`
  - 统一错误响应与日志规范，提升可观测性

## 第5章 虚拟机性能优化实践
- CPU 热点：`src/main/java/com/example/jvmlab/chapter05/Chapter05Controller.java:33`
- 内存抖动：`src/main/java/com/example/jvmlab/chapter05/Chapter05Controller.java:52`

## 第6章 类文件结构
- 类结构解析（ASM）：`src/main/java/com/example/jvmlab/chapter06/Chapter06Controller.java:36`

## 第7章 类加载机制
- 主动引用（new）：`src/main/java/com/example/jvmlab/chapter07/Chapter07Controller.java:31`
- 静态字段触发初始化 vs 常量被动引用：`src/main/java/com/example/jvmlab/chapter07/Chapter07Controller.java:45`
- 静态方法触发初始化：`src/main/java/com/example/jvmlab/chapter07/Chapter07Controller.java:59`
- 子类引用父类字段（被动引用）：`src/main/java/com/example/jvmlab/chapter07/Chapter07Controller.java:72`
- 数组引用不触发初始化：`src/main/java/com/example/jvmlab/chapter07/Chapter07Controller.java:85`
- 准备阶段与初始化阶段：`src/main/java/com/example/jvmlab/chapter07/Chapter07Controller.java:98`
- 初始化顺序：`src/main/java/com/example/jvmlab/chapter07/Chapter07Controller.java:111`
- 类加载器层次：`src/main/java/com/example/jvmlab/chapter07/Chapter07Controller.java:123`
- 自定义类加载器：`src/main/java/com/example/jvmlab/chapter07/Chapter07Controller.java:145`
- 打破双亲委派：`src/main/java/com/example/jvmlab/chapter07/Chapter07Controller.java:165`
- 类卸载演示：`src/main/java/com/example/jvmlab/chapter07/Chapter07Controller.java:187`

## 第8章 字节码执行引擎
- MethodHandle：`src/main/java/com/example/jvmlab/chapter08/Chapter08Controller.java:37`
- invokedynamic（LambdaMetafactory）：`src/main/java/com/example/jvmlab/chapter08/Chapter08Controller.java:80`
- 对象内存布局（JOL）：`src/main/java/com/example/jvmlab/chapter08/Chapter08Controller.java:53`

## 第9章 类加载与执行子系统实践
- JDK 动态代理：`src/main/java/com/example/jvmlab/chapter09/Chapter09Controller.java:34`
- ASM 动态类生成：`src/main/java/com/example/jvmlab/chapter09/Chapter09Controller.java:57`
- ASM 工具：`src/main/java/com/example/jvmlab/common/AsmDynamicClassBuilder.java:37/60`

## 第10章 编译期优化
- 运行期动态编译与执行：`src/main/java/com/example/jvmlab/chapter10/Chapter10Controller.java:39`
- 源码生成：`src/main/java/com/example/jvmlab/chapter10/Chapter10Controller.java:74`

## 第11章 运行期优化（JIT）
- JIT 预热：`src/main/java/com/example/jvmlab/chapter11/Chapter11Controller.java:33`
- 代码缓存与编译器统计：`src/main/java/com/example/jvmlab/chapter11/Chapter11Controller.java:95`
- 逃逸分析与标量替换：`src/main/java/com/example/jvmlab/chapter11/Chapter11Controller.java:128`
- 锁竞争/偏向锁撤销：`src/main/java/com/example/jvmlab/chapter11/Chapter11Controller.java:165`

## 压测与快速触发入口
- `JvmErrorController` 快速触发各类 OOM/栈/线程异常：`src/main/java/com/example/jvmstress/ctrl/JvmErrorController.java:85/115/147/182/209/225/243/265/291/338/365`

## 使用建议
- 建议根据章节运行相应接口与测试类的 `main` 方法，观察日志中的“【成功】...”确认信息
- 结合 JVM 参数进行 A/B 对比：
  - 堆/常量池：`-Xms/-Xmx`、`-XX:+HeapDumpOnOutOfMemoryError`
  - 直接内存：`-XX:MaxDirectMemorySize`
  - GC：`-Xlog:gc*`、`-XX:+UseG1GC -XX:+UseStringDeduplication`
  - 线程/栈：`-Xss` 与操作系统线程上限（`ulimit -u`）
  - 运行期优化：`-XX:+/-DoEscapeAnalysis`、代码缓存与 JIT 监控选项

