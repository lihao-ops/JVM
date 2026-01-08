# 第11章 后端编译与优化

> **对应书籍**: 《深入理解Java虚拟机（第3版）》第11章  
> **核心主题**: JIT 编译、逃逸分析、锁优化、编译器优化技术

---

## 📖 核心内容概述

### 11.1 即时编译器 (JIT)

| 编译器 | 特点 | 触发条件 |
| :--- | :--- | :--- |
| **C1 编译器** | 编译快，优化少 | 方法调用 >1500 次 |
| **C2 编译器** | 编译慢，优化深 | 方法调用 >10000 次 |
| **分层编译** | C1→C2 逐步优化 | JDK8+ 默认开启 |
| **Graal 编译器** | Java 实现，可扩展 | JDK10+ 可选 |

### 11.2 热点探测

```
┌─────────────────────────────────────────────────────────────┐
│                    热点探测机制                              │
├─────────────────────────────────────────────────────────────┤
│  方法调用计数器 (Invocation Counter)                         │
│  - 统计方法调用次数                                          │
│  - 超过阈值触发 JIT 编译                                     │
│  - 有热度衰减（半衰期）                                      │
├─────────────────────────────────────────────────────────────┤
│  回边计数器 (Back Edge Counter)                              │
│  - 统计循环次数                                              │
│  - 触发 OSR (栈上替换) 编译                                  │
│  - 循环体内直接切换到编译代码                                 │
└─────────────────────────────────────────────────────────────┘
```

### 11.3 逃逸分析

| 逃逸状态 | 定义 | 优化效果 |
| :--- | :--- | :--- |
| **未逃逸** | 对象仅在方法内使用 | 标量替换、栈上分配 |
| **方法逃逸** | 对象作为参数传递 | 部分优化 |
| **线程逃逸** | 对象被其他线程访问 | 不能优化 |

```java
// 逃逸分析示例
public int sum() {
    Point p = new Point(1, 2);  // 未逃逸
    return p.x + p.y;
}

// 优化后（标量替换）
public int sum() {
    int p_x = 1;  // 对象被拆解为标量
    int p_y = 2;
    return p_x + p_y;  // 无对象分配！
}
```

### 11.4 锁优化

| 优化技术 | 原理 | 条件 |
| :--- | :--- | :--- |
| **锁消除** | 逃逸分析确认对象未逃逸，移除锁 | `-XX:+EliminateLocks` |
| **锁粗化** | 合并连续的加锁解锁操作 | 连续操作同一对象 |
| **偏向锁** | 无竞争时仅记录线程 ID | JDK15 前默认开启 |
| **轻量级锁** | CAS 替代重量级锁 | 无竞争或短时竞争 |
| **自适应自旋** | 根据历史情况决定自旋次数 | 默认开启 |

```
┌─────────────────────────────────────────────────────────────┐
│                    锁状态升级                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   无锁 → 偏向锁 → 轻量级锁 → 重量级锁                         │
│    ↓        ↓         ↓           ↓                        │
│  无同步   单线程   CAS竞争    互斥量                          │
│           反复获取   短时等待   阻塞等待                       │
│                                                             │
│   注意：JDK15+ 默认禁用偏向锁 -XX:-UseBiasedLocking           │
└─────────────────────────────────────────────────────────────┘
```

---

## 💻 代码实践清单

### 实验1: JIT 预热

```bash
GET /chapter11/warmup?warmup=100000
```

**实验代码**: `Chapter11Controller.java:36`

```java
// 观察预热前后性能差异
// 预热前：解释执行，慢
// 预热后：JIT 编译，快 10-100 倍
for (int i = 0; i < warmup; i++) {
    compute(payload);
}
```

### 实验2: 代码缓存信息

```bash
GET /chapter11/code-cache
```

**实验代码**: `Chapter11Controller.java:88`

### 实验3: 逃逸分析演示

```bash
# VM 参数（对比开启/关闭）
-XX:+DoEscapeAnalysis -XX:+EliminateAllocations
-XX:-DoEscapeAnalysis

GET /chapter11/escape-analysis?iterations=100000
```

**实验代码**: `Chapter11Controller.java:127`

```java
// 逃逸分析目标：Point 对象未逃逸
for (int i = 0; i < iterations; i++) {
    Point p = new Point(i, i + 1);  // 可被标量替换
    sum += p.length();
}
```

### 实验4: 锁竞争演示

```bash
# VM 参数
-XX:+UseBiasedLocking (JDK15 前)
-XX:-UseBiasedLocking (关闭偏向锁)

GET /chapter11/lock-demo?threads=8&iterations=200000
```

**实验代码**: `Chapter11Controller.java:164`

---

## 🏭 生产实践建议

### 1. JIT 预热策略

```java
// 问题：服务刚启动时，热点代码未编译，响应慢

// 方案 1：主动预热
@PostConstruct
public void warmup() {
    for (int i = 0; i < 15000; i++) {
        criticalBusinessMethod();
    }
    log.info("JIT warmup completed");
}

// 方案 2：预热脚本
// 启动后自动调用核心接口

// 方案 3：Class Data Sharing (CDS)
// 共享类元数据，加速启动

// 方案 4：GraalVM Native Image
// AOT 编译，无需预热
```

### 2. 代码缓存优化

```bash
# 默认代码缓存 240MB，热点代码多时可能不够
-XX:ReservedCodeCacheSize=256m

# 代码缓存满时刷新旧代码
-XX:+UseCodeCacheFlushing

# 监控代码缓存使用
jcmd <pid> Compiler.codecache
```

### 3. 逃逸分析最佳实践

```java
// 利用逃逸分析，让临时对象不逃逸
public int processOrder(Order order) {
    // 好：临时对象在方法内
    OrderValidator validator = new OrderValidator();
    validator.validate(order);
    
    // 差：返回临时对象（方法逃逸）
    // return validator;
    
    return order.getTotal();
}

// 性能敏感场景：避免装箱
// 差：Integer result = list.stream().reduce(0, Integer::sum);
// 好：int result = list.stream().mapToInt(Integer::intValue).sum();
```

### 4. 锁优化建议

```java
// 1. 减少锁粒度
// 差
public synchronized void process() {
    // 大量非临界区代码
    criticalSection();
    // 大量非临界区代码
}

// 好
public void process() {
    // 非临界区代码
    synchronized(lock) {
        criticalSection();
    }
    // 非临界区代码
}

// 2. 使用并发容器替代同步容器
ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();

// 3. 使用原子类替代锁
AtomicLong counter = new AtomicLong();
counter.incrementAndGet();

// 4. 考虑读写锁
ReadWriteLock rwLock = new ReentrantReadWriteLock();
```

### 5. 编译日志分析

```bash
# 开启编译日志
-XX:+PrintCompilation

# 输出格式：时间戳 编译ID 属性 层级 方法名
#  123   1  b    3       java.lang.String::hashCode

# 属性含义：
# b = 阻塞（等待编译）
# s = 同步（包含 synchronized）
# ! = 有异常处理
# % = OSR 编译
# n = 本地方法

# 层级：
# 0 = 解释执行
# 1-3 = C1 编译
# 4 = C2 编译
```

---

## 🎯 面试考点提炼

### 高频问题

1. **"什么是 JIT？为什么需要它？"**
   - 即时编译器，将热点字节码编译为机器码
   - 解释执行慢，编译后快 10-100 倍
   - 结合解释器兼顾启动速度和运行效率

2. **"什么是逃逸分析？有什么作用？"**
   - 分析对象的动态作用域
   - 未逃逸对象可以：栈上分配、标量替换、锁消除

3. **"JVM 对锁做了哪些优化？"**
   - 锁消除：逃逸分析后移除不必要的锁
   - 锁粗化：合并连续加锁
   - 偏向锁→轻量级锁→重量级锁

4. **"服务启动慢/首次请求慢怎么优化？"**
   - JIT 预热：启动后主动调用热点方法
   - CDS：类数据共享
   - AOT：GraalVM Native Image

5. **"什么是 OSR 编译？"**
   - On-Stack Replacement，栈上替换
   - 循环体内直接切换到编译代码
   - 不需要等方法返回

### 进阶问题

6. **"C1 和 C2 编译器的区别？"**
   - C1：编译快、优化少、适合客户端
   - C2：编译慢、优化深、适合服务端
   - 分层编译：先 C1 后 C2

7. **"偏向锁为什么被废弃？"**
   - JDK15 默认禁用，JDK18 移除
   - 现代应用多线程场景多，偏向锁撤销开销大
   - 维护成本高于收益

8. **"如何查看方法是否被编译？"**
   - `-XX:+PrintCompilation`
   - JMC Flight Recorder
   - Arthas: jad 查看是否有 JIT 标记

---

## 📚 相关资源

- 书籍章节: 《深入理解JVM》第11章 11.1-11.4
- 参数参考: `-XX:+PrintCompilation`、`-XX:+DoEscapeAnalysis`
- 工具: JMC、async-profiler、JITWatch
