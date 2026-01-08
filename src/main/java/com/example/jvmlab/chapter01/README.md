# 第1章 走近 Java

> **对应书籍**: 《深入理解Java虚拟机（第3版）》第1章  
> **核心主题**: JDK/JRE/JVM 关系、HotSpot 虚拟机、混合执行模式

---

## 📖 核心内容概述

### 1.1 JDK、JRE、JVM 三者关系

```
┌─────────────────────────────────────────────────┐
│  JDK (Java Development Kit)                     │
│  ┌───────────────────────────────────────────┐  │
│  │  JRE (Java Runtime Environment)           │  │
│  │  ┌─────────────────────────────────────┐  │  │
│  │  │  JVM (Java Virtual Machine)         │  │  │
│  │  │  - 类加载器                          │  │  │
│  │  │  - 执行引擎                          │  │  │
│  │  │  - 运行时数据区                       │  │  │
│  │  └─────────────────────────────────────┘  │  │
│  │  + 核心类库 (rt.jar)                      │  │
│  └───────────────────────────────────────────┘  │
│  + 开发工具 (javac, javap, jstack, jmap...)     │
└─────────────────────────────────────────────────┘
```

**面试常问**: "生产环境只装 JRE 可以吗？"
- 可以运行，但排查问题时缺少 jstack、jmap 等诊断工具
- 推荐：生产环境安装完整 JDK

### 1.2 HotSpot 混合执行模式

HotSpot 虚拟机采用**解释器 + JIT 编译器**的混合模式：

| 组件 | 特点 | 适用场景 |
| :--- | :--- | :--- |
| **解释器** | 启动快，逐行解释执行 | 程序启动阶段、冷代码路径 |
| **C1 编译器** | 编译快，优化少 | 客户端程序、对启动速度敏感 |
| **C2 编译器** | 编译慢，深度优化 | 服务端程序、热点方法 |

**热点探测**: 基于计数器（方法调用计数器 + 回边计数器），默认阈值 10000 次

### 1.3 JIT 编译触发条件

```java
// 热点代码示例：循环执行超过阈值后触发 JIT
for (int i = 0; i < 15000; i++) {
    hotMethod();  // 第 10001 次调用时触发 C1/C2 编译
}
```

---

## 💻 代码实践清单

### 实验1: JDK/JRE/JVM 概念演示

```bash
# 接口调用
GET /chapter01/jdk-jre-jvm
```

**实验代码**: `Chapter01Controller.java:28`

### 实验2: 混合模式验证

```bash
# 接口调用
GET /chapter01/mixed-mode
```

**实验代码**: `Chapter01Controller.java:48`

### 实验3: JIT 性能对比

**实验代码**: `JitPerformanceTest.java`

```bash
# 1. 纯解释模式运行（慢）
java -Xint JitPerformanceTest

# 2. 混合模式运行（快）
java -XX:+PrintCompilation JitPerformanceTest

# 对比：混合模式通常快 10-100 倍
```

### 实验4: 观察 JIT 编译日志

使用脚本 `scripts/chapter01/run_with_jit_log.bat`:

```bash
# 输出示例（"代码雨"）
    123   1       3       java.lang.String::hashCode (55 bytes)
    125   2       3       java.lang.String::charAt (29 bytes)
    ...
```

---

## 🏭 生产实践建议

### 1. JIT 预热 (Warm-up)

**问题**: 服务刚启动时，热点代码尚未编译，响应慢

**解决方案**:
```java
@PostConstruct
public void warmup() {
    // 启动时预热核心方法
    for (int i = 0; i < 15000; i++) {
        criticalBusinessMethod();
    }
    log.info("JIT warmup completed");
}
```

**应用场景**: 
- 网关服务重启后的 P99 抖动
- 定时任务首次执行慢

### 2. 分层编译控制

```bash
# JDK 8+ 默认开启分层编译
-XX:+TieredCompilation

# 只使用 C2 编译器（服务端推荐）
-XX:-TieredCompilation -server

# 查看编译阈值
-XX:+PrintFlagsFinal | grep CompileThreshold
```

### 3. 代码缓存监控

```bash
# 代码缓存不足会导致 JIT 停止编译
-XX:ReservedCodeCacheSize=256m  # 默认 240m
-XX:+UseCodeCacheFlushing       # 缓存满时清理
```

---

## 🎯 面试考点提炼

### 高频问题

1. **"JDK、JRE、JVM 的区别？"**
   - JVM 是虚拟机，负责执行字节码
   - JRE = JVM + 类库，可运行程序
   - JDK = JRE + 开发工具，可开发程序

2. **"什么是 JIT 编译？为什么需要它？"**
   - JIT 将热点字节码编译为本地机器码
   - 解释执行慢但启动快，JIT 编译后执行快
   - 两者结合兼顾启动速度和运行效率

3. **"热点代码是如何被识别的？"**
   - 基于计数器的热点探测
   - 方法调用计数器：统计方法调用次数
   - 回边计数器：统计循环次数
   - 超过阈值（默认 10000）触发编译

4. **"C1 和 C2 编译器的区别？"**
   - C1：编译快，优化浅（内联、去虚拟化）
   - C2：编译慢，优化深（逃逸分析、标量替换）
   - 分层编译：先 C1 再 C2，平衡响应与性能

### 进阶问题

5. **"如何判断方法是否被 JIT 编译？"**
   ```bash
   -XX:+PrintCompilation
   # 或使用 JMC/Arthas 查看编译状态
   ```

6. **"服务重启后延迟抖动如何解决？"**
   - JIT 预热
   - AOT 编译（GraalVM）
   - CDS（类数据共享）加速启动

---

## 📚 相关资源

- 书籍章节: 《深入理解JVM》第1章 1.1-1.5
- 脚本位置: `scripts/chapter01/`
- 测试类: `src/test/java/com/example/jvmlab/chapter01/`
