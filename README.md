# JVM Learning Lab - 深入理解Java虚拟机学习实验室

## 项目简介

这是一个基于Spring Boot 3.5.3与JDK 17的教学型项目，通过实战代码验证《深入理解Java虚拟机》中的核心知识点。所有控制器均按照章节划分，提供详细注释与中英日志，方便学习、面试复盘与团队分享。

## 特性亮点

- ✅ **章节对照**：每个控制器与书中章节一一对应，接口名称即实验主题。
- ✅ **内存异常作战图**：新增 `memory-exception-lab` 控制器，以大厂面试官视角串联「原理 → 复现 → 排查 → 解决」。
- ✅ **双语注释**：类、方法、核心代码均附中文+英文说明，降低沟通成本。
- ✅ **监控完备**：集成Actuator与Prometheus指标，快速观察JVM运行状态。
- ✅ **实战导向**：提供OOM、GC、类加载、JIT等典型案例，复现真实问题场景。

## 快速开始

```bash
git clone <repository-url>
cd jvm-learning-lab
mvn clean package
java -jar target/jvm-learning-lab-1.0.0.jar
```

应用默认运行在 `http://localhost:8080/jvm-lab`，通过章节路径即可访问各实验接口，例如 `http://localhost:8080/jvm-lab/chapter02/heap-oom`。

## 推荐JVM参数

### 学习/开发环境

```bash
-Xms512m -Xmx512m \
-XX:MaxDirectMemorySize=256m \
-XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m \
-Xss256k \
-XX:+PrintGCDetails -XX:+PrintGCDateStamps \
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs/heapdump.hprof \
-XX:+UseG1GC -XX:+PrintCommandLineFlags
```

### 压测/生产模拟

```bash
-Xms4g -Xmx4g \
-XX:MaxDirectMemorySize=2g \
-XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m \
-Xss1m \
-XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
-XX:+ParallelRefProcEnabled \
-XX:+UnlockExperimentalVMOptions \
-XX:+UseCGroupMemoryLimitForHeap \
-Xlog:gc*:file=logs/gc.log:time,uptime:filecount=10,filesize=100m
```

## 章节导航

| 章节 | 控制器 | 实验亮点 |
|------|--------|----------|
| 面试实战 | `memory-exception-lab` | 7 大 JVM 内存异常一站式指引，支持 Dry-Run 与实测触发 |
| 第2章 | `chapter02` | 堆/栈/元空间/直接内存 OOM 实验与内存监控 |
| 第3章 | `chapter03` | 引用类型、对象晋升、空间分配担保、GC统计 |
| 第4章 | `chapter04` | JVM参数查询、线程快照、监控汇总 |
| 第5章 | `chapter05` | CPU热点与内存抖动调优案例 |
| 第6章 | `chapter06` | ASM解析类结构，理解ClassFile格式 |
| 第7章 | `chapter07` | 类初始化时机、双亲委派、自定义类加载器 |
| 第8章 | `chapter08` | MethodHandle 与 JOL 对象布局实验 |
| 第9章 | `chapter09` | JDK代理与ByteBuddy动态生成类 |
| 第10章 | `chapter10` | JavaCompiler动态编译演示编译期优化 |
| 第11章 | `chapter11` | JIT预热循环，体验运行期优化 |
| 监控 | `monitor` | JVM内存、GC、系统信息一键查看 |

## 内存异常实验中枢速览

`memory-exception-lab` 控制器将面试中高频的 JVM 内存问题抽象为可执行的策略：

| 场景 ID | JVM 区域 | 对应异常 | 亮点 |
|---------|-----------|----------|------|
| `stack-overflow` | 线程私有 | `StackOverflowError` | 线程栈深度实时统计，支持 -Xss 压测 |
| `heap-oom` | 堆 | `OutOfMemoryError: Java heap space` | 支持自定义块大小与节奏，方便对比 GC 行为 |
| `gc-overhead` | 堆 | `OutOfMemoryError: GC overhead limit exceeded` | 复现 Full GC 风暴，配套 GC 日志分析清单 |
| `metaspace-oom` | 元空间 | `OutOfMemoryError: Metaspace` | 基于 ASM 动态生类，观察 Class Space 膨胀 |
| `direct-memory-oom` | 直接内存 | `OutOfMemoryError: Direct buffer memory` | 输出 allocate 次数，指导使用 NMT 排查 |
| `string-pool-pressure` | 堆 | `OutOfMemoryError: Java heap space` | 高速填充常量池，演示 intern() 误用风险 |
| `thread-oom` | 本地线程 | `OutOfMemoryError: unable to create new native thread` | 统计线程创建数量并给出 ulimit 调优建议 |

> 所有接口默认 `dryRun=true` 返回操作指引，设置 `dryRun=false` 才会真正触发异常，避免误操作拖垮教学环境。

## 常用诊断命令

- `jps -l`：查看Java进程列表。
- `jstat -gc <pid> 1000`：实时观察GC统计。
- `jmap -dump:live,format=b,file=heapdump.hprof <pid>`：生成堆转储。
- `jstack <pid>`：打印线程堆栈，定位死锁或CPU热点。
- `jcmd <pid> VM.flags`：查看JVM启动参数。

## 注意事项

1. 某些实验（如线程爆炸、直接内存OOM）具有较大风险，建议在容器或虚拟机中执行。
2. 使用前请阅读 `application.yml` 中的JVM参数建议，合理控制资源。
3. 日志默认输出到控制台和 `logs/jvm-lab.log`，请确保磁盘空间充足。

祝学习顺利，面试成功！
