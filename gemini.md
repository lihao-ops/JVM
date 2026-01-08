# JVM 核心实战实验室 (JVM Core Lab) - 项目规范与指南

> **版本**: 2.0
> **对应书籍**: 《深入理解Java虚拟机（第3版）》 (周志明 著)
> **角色设定**: 阿里 P8 / 字节 2-2 架构师 & JVM 领域专家
> **核心目标**: 拒绝死记硬背，通过代码实战“复现”底层原理，通过脚本“观测”虚拟机行为。

---

## 1. 项目架构与目录映射 (Project Structure)

本项目基于 Spring Boot 构建，每个章节对应一个独立的 package 和脚本目录。

### 📂 核心代码目录 (`src/main/java/com/example/jvmlab/`)
| 目录/包名 | 对应书籍章节 | 核心内容 & 实验类 | 面试考察点 |
| :--- | :--- | :--- | :--- |
| **`chapter01`** | **第1章 走近Java** | `Chapter01Controller`: JDK/JRE关系, 混合模式<br>`JitPerformanceTest`: 解释器 vs JIT 性能对比 | JIT 编译阈值、混合模式原理、热点探测 |
| **`chapter02`** | **第2章 内存区域** | `JvmRuntimeDataAreaController`: 堆/栈/元空间 OOM 实战<br>`ProgramCounterTest`: PC 寄存器与线程切换<br>`stack/StackOverflowTest`: 栈溢出<br>`stack/ThreadOomTest`: 线程耗尽 | 内存溢出排查、栈帧结构、直接内存、PC 寄存器特性 |
| `chapter03` | 第3章 GC与分配 | *(待建设)* GC 算法、垃圾收集器、对象分配策略 | CMS vs G1 vs ZGC、三色标记、跨代引用 |
| `chapter04` | 第4章 监控工具 | *(待建设)* jstat, jmap, arthas 使用实战 | 线上 CPU 100% 排查、内存泄漏定位 |

### 📂 脚本工具目录 (`scripts/`)
为了方便在无 IDE 环境（模拟生产服务器）下验证，所有实验必须配备 `.bat` (Windows) 或 `.sh` (Linux) 脚本。

- **`scripts/chapter01/`**
  - `check_jit_threshold.bat`: 查看 JIT 编译阈值 (默认 10000)。
  - `run_with_jit_log.bat`: 开启 `-XX:+PrintCompilation` 观察 JIT "代码雨"。
- **`scripts/chapter02/`**
  - `view_bytecode_pc.bat`: 使用 `javap -c -l` 反汇编，配合 Debug 观察程序计数器。
  - `run_sof_test.bat`: 栈溢出实验 (-Xss160k)。
  - `run_thread_oom_test.bat`: 线程 OOM 实验 (-Xss2M)。

---

## 2. 代码开发规范 (Coding Standards)

### 2.1 实验类设计原则
所有的实验代码不是为了“跑通业务”，而是为了**“触发特定现象”**。
- **Controller 模式**: 对于 OOM、高并发测试，优先使用 `@RestController` 暴露接口，方便通过浏览器/Postman 触发。
- **Main 方法模式**: 对于纯计算、性能对比（如 JIT）、底层指令观察（如 PC 寄存器），使用 `public static void main`。

### 2.2 日志规范 (Logging)
**【强制】** 生产环境禁止使用 `System.out.println`。
- 必须使用 SLF4J 接口配合 Logback/Log4j2 实现。
- 类上使用 Lombok 的 `@Slf4j` 注解。
- 实验代码中的关键输出（如实验结果、异常信息）应使用 `log.info()` 或 `log.error()`。

### 2.3 注释规范 (The "Why" over "What")
注释必须解释**背后的 JVM 原理**，而不是解释 Java 语法。

*   ❌ **错误示范**:
    ```java
    // 创建一个列表
    List<byte[]> list = new ArrayList<>();
    // 循环添加数据
    while(true) list.add(new byte[1024]);
    ```

*   ✅ **正确示范 (面试官视角)**:
    ```java
    // 【堆内存溢出实验】
    // 保持对新对象的强引用，防止 GC 回收。
    // 模拟场景：缓存未设置过期时间，或 ThreadLocal 未清理。
    List<byte[]> list = new ArrayList<>();
    while(true) {
        // 分配 1KB 内存，快速填满 Heap
        list.add(new byte[1024]); 
    }
    ```

### 2.4 常用 JVM 参数 (VM Options)
在实验代码的类注释中，必须标注该实验需要的 VM 参数。
- **OOM 实验**: `-Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError`
- **栈溢出实验**: `-Xss128k`
- **JIT 观察**: `-XX:+PrintCompilation`
- **GC 日志**: `-Xlog:gc*` (JDK 9+) 或 `-XX:+PrintGCDetails` (JDK 8)

---

## 3. 交互与回答规范 (Interaction Protocol)

当用户提问时，AI 助手应遵循以下逻辑：

1.  **场景代入 (Context)**: 先判断用户遇到的问题属于哪个 JVM 内存区域或生命周期阶段。
2.  **原理降维 (Analogy)**: 使用“厨房理论”、“龟兔赛跑”等比喻进行通俗解释。
3.  **实战验证 (Action)**: **这是最重要的**。不要只给理论，要给出：
    *   对应的代码位置（如 `src/.../JitPerformanceTest.java`）。
    *   对应的脚本位置（如 `scripts/chapter01/run_with_jit_log.bat`）。
    *   预期的控制台输出或现象。
4.  **面试考点 (Interview Checkpoint)**: 指出该知识点在面试中通常怎么问（例如：“对象一定在堆上分配吗？” -> 逃逸分析）。

---

## 4. 当前项目进度检查点 (Checkpoints)

- [x] **第1章**: 了解 JDK/JRE/JVM 区别。
- [x] **第1章**: 验证 HotSpot 混合模式 (Interpreter + C1 + C2)。
- [x] **第1章**: 脚本化 JIT 编译日志观察。
- [x] **第2章**: 建立运行时数据区 Controller 框架。
- [x] **第2章**: 验证程序计数器 (PC) 的线程私有性 (通过 Debug + javap)。
- [x] **第2章**: 完善栈 (Stack) SOF 实战与 `-Xss` 调优。
- [x] **第2章**: 完善栈 (Stack) 线程 OOM 实战。
- [ ] **第2章**: 完善堆 (Heap) OOM 实战与 Heap Dump 分析。
- [ ] **第2章**: 完善元空间 (Metaspace) OOM 实战 (CGLib 动态代理)。

---

> **致开发者**: 
> JVM 不再是黑盒。通过本项目，我们要把每一个参数、每一次 GC、每一行字节码都变得“肉眼可见”。
> Keep Coding, Keep Tuning.
