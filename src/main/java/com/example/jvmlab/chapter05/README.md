# 第5章 调优案例分析与实战

> **对应书籍**: 《深入理解Java虚拟机（第3版）》第5章  
> **核心主题**: 性能调优方法论、典型案例分析、实战优化

---

## 📖 核心内容概述

### 5.1 调优目标与指标

| 指标类型 | 核心指标 | 目标值（参考） |
| :--- | :--- | :--- |
| **吞吐量** | GC 时间占比 | < 5% |
| **延迟** | GC 停顿时间 | P99 < 100ms |
| **容量** | 堆内存利用率 | 60% ~ 80% |

### 5.2 调优方法论

```
1. 明确目标 → 2. 收集数据 → 3. 分析瓶颈 → 4. 优化方案 → 5. 验证效果
     ↑                                                      │
     └──────────────────────────────────────────────────────┘
                          持续迭代
```

### 5.3 常见性能问题分类

| 问题类型 | 现象 | 典型原因 |
| :--- | :--- | :--- |
| **CPU 高** | CPU 使用率接近 100% | 死循环、正则回溯、频繁 GC |
| **内存抖动** | 频繁 Minor GC，内存忽高忽低 | 短生命周期大对象、缓存刷新 |
| **Full GC 频繁** | 老年代频繁回收 | 内存泄漏、晋升过快 |
| **响应慢** | 接口 P99 抖动 | GC 停顿、锁竞争、IO 阻塞 |

### 5.4 调优手段

| 层面 | 手段示例 |
| :--- | :--- |
| **代码层** | 对象池复用、避免大对象、减少装箱拆箱 |
| **JVM 参数** | 堆大小、GC 选择、JIT 参数 |
| **架构层** | 缓存、异步化、限流降级 |

---

## 💻 代码实践清单

### 实验1: CPU 热点模拟

```bash
GET /chapter05/cpu-hotspot
```

**实验代码**: `Chapter05Controller.java:33`

```java
// 模拟 CPU 密集型任务
// 配合 Arthas profiler 或 async-profiler 生成火焰图
while (System.currentTimeMillis() < endTime) {
    for (int i = 0; i < 1000000; i++) {
        hash ^= hash * 31 + i;
    }
}
```

### 实验2: 内存抖动模拟

```bash
GET /chapter05/memory-churn
```

**实验代码**: `Chapter05Controller.java:52`

```java
// 模拟频繁创建临时对象导致的内存抖动
// 观察 Minor GC 频率
for (int i = 0; i < iterations; i++) {
    byte[] temp = new byte[1024];  // 频繁分配
    // temp 很快成为垃圾
}
```

---

## 🏭 生产实践建议

### 1. CPU 热点定位

```bash
# 方式一：Arthas profiler
profiler start
# 等待一段时间
profiler stop --format html

# 方式二：async-profiler
./profiler.sh -d 30 -f flame.html <pid>

# 方式三：JMC Flight Recorder
jcmd <pid> JFR.start duration=60s filename=recording.jfr
```

**火焰图分析要点**:
- 横轴：采样占比（越宽越耗 CPU）
- 纵轴：调用栈深度
- 关注：最宽的"平台"区域

### 2. 内存抖动优化

```java
// 问题代码：频繁创建临时对象
for (String line : lines) {
    String[] parts = line.split(",");  // 每次创建新数组
    processLine(parts);
}

// 优化方案 1：对象池
StringBuilderPool pool = new StringBuilderPool();
StringBuilder sb = pool.borrow();
try {
    // 使用 sb
} finally {
    pool.release(sb);
}

// 优化方案 2：预编译正则
private static final Pattern COMMA = Pattern.compile(",");
String[] parts = COMMA.split(line);

// 优化方案 3：避免装箱
// 使用原始类型集合 (fastutil/eclipse-collections)
IntArrayList list = new IntArrayList();
```

### 3. GC 调优检查清单

```bash
# 1. 选择合适的 GC
# 吞吐量优先
-XX:+UseParallelGC

# 低延迟优先
-XX:+UseG1GC -XX:MaxGCPauseMillis=200

# 超大堆、极低延迟
-XX:+UseZGC

# 2. 新生代调优
# 原则：让短生命周期对象在新生代回收
-Xmn2g                     # 新生代大小
-XX:SurvivorRatio=8        # Eden:Survivor = 8:1
-XX:MaxTenuringThreshold=6 # 减少无意义的年龄拷贝

# 3. 大对象处理
# 避免大对象进入新生代再晋升
-XX:PretenureSizeThreshold=1m
-XX:G1HeapRegionSize=4m    # G1 分区大小

# 4. 监控告警
# Full GC > 1 次/小时：告警
# Young GC > 1 次/秒：优化
```

### 4. 锁竞争优化

```java
// 问题：synchronized 粒度太粗
public synchronized void process(Request req) {
    // 全部串行
}

// 优化方案 1：减小锁粒度
public void process(Request req) {
    // 非临界区代码
    synchronized(lock) {
        // 只锁必要部分
    }
}

// 优化方案 2：使用并发容器
ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();

// 优化方案 3：无锁设计
AtomicLong counter = new AtomicLong();
counter.incrementAndGet();
```

### 5. 响应时间优化

```
┌─────────────────────────────────────────────────────────────┐
│                   响应时间组成                               │
├─────────────────────────────────────────────────────────────┤
│  网络延迟 + 队列等待 + 业务计算 + GC 停顿 + DB/RPC 调用       │
└─────────────────────────────────────────────────────────────┘

优化手段：
1. 网络延迟：CDN、就近部署
2. 队列等待：线程池调优、异步化
3. 业务计算：算法优化、缓存
4. GC 停顿：GC 调优、减少对象分配
5. DB/RPC：SQL 优化、批量查询、超时控制
```

---

## 🎯 面试考点提炼

### 高频问题

1. **"做过什么 JVM 调优？"**
   - 描述场景：什么问题？怎么发现的？
   - 分析过程：用什么工具？看什么指标？
   - 解决方案：改了什么参数/代码？
   - 效果验证：优化前后对比数据

2. **"GC 调优的思路是什么？"**
   - 明确目标：吞吐量 or 延迟
   - 选择 GC 收集器
   - 调整堆大小和分代比例
   - 分析 GC 日志，持续优化

3. **"如何定位代码热点？"**
   - Arthas profiler / async-profiler
   - JMC Flight Recorder
   - 生成火焰图分析

4. **"什么是内存抖动？如何解决？"**
   - 短生命周期对象频繁创建导致频繁 Minor GC
   - 解决：对象池、减少临时对象、预分配

5. **"服务 RT 抖动怎么排查？"**
   - 看 GC 日志是否有 STW
   - 看线程是否有锁竞争
   - 看 DB/RPC 是否超时

### 进阶问题

6. **"JIT 对性能有什么影响？如何预热？"**
   - JIT 编译后性能提升 10-100 倍
   - 预热方法：启动时调用核心方法超过阈值

7. **"如何做压测？关注什么指标？"**
   - 工具：JMeter、Gatling
   - 指标：TPS、RT(P50/P95/P99)、错误率、GC

---

## 📚 相关资源

- 书籍章节: 《深入理解JVM》第5章 5.1-5.3
- 监控工具: `common/JvmMemoryMonitor.java`
- 压测入口: `jvmstress/ctrl/JvmErrorController.java`
