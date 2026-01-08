# 第3章 垃圾收集器与内存分配策略

> **对应书籍**: 《深入理解Java虚拟机（第3版）》第3章  
> **核心主题**: 对象存活判定、GC 算法、垃圾收集器、内存分配策略

---

## 📖 核心内容概述

### 3.1 对象存活判定

| 算法 | 原理 | 优缺点 |
| :--- | :--- | :--- |
| **引用计数法** | 对象被引用时计数+1，引用失效时-1 | 简单高效，但无法解决循环引用 |
| **可达性分析** | 从 GC Roots 出发，不可达对象即为垃圾 | 主流 JVM 采用，可解决循环引用 |

**GC Roots 包括**:
- 虚拟机栈中引用的对象
- 方法区中静态属性引用的对象
- 方法区中常量引用的对象
- 本地方法栈中 JNI 引用的对象
- 同步锁持有的对象

### 3.2 四种引用类型

| 引用类型 | 回收时机 | 使用场景 |
| :--- | :--- | :--- |
| **强引用** | 永不回收（除非置 null） | 普通对象引用 |
| **软引用** | 内存不足时回收 | 缓存（如图片缓存） |
| **弱引用** | 下次 GC 时回收 | WeakHashMap、ThreadLocal |
| **虚引用** | 随时回收，仅用于回收通知 | 堆外内存回收跟踪 |

### 3.3 GC 算法

| 算法 | 原理 | 优缺点 |
| :--- | :--- | :--- |
| **标记-清除** | 标记存活对象，清除未标记对象 | 简单，但有内存碎片 |
| **标记-复制** | 存活对象复制到另一半空间 | 无碎片，但浪费一半空间 |
| **标记-整理** | 存活对象向一端移动，清理边界外内存 | 无碎片，但需要移动对象 |
| **分代收集** | 新生代用复制，老年代用标记-整理 | 综合各算法优点 |

### 3.4 主流垃圾收集器

```
┌───────────────────────────────────────────────────────────┐
│                新生代                   老年代              │
├───────────────────────────────────────────────────────────┤
│  Serial ────────────────────────── Serial Old             │
│  ParNew ────────────────────────── CMS (已废弃)           │
│  Parallel Scavenge ─────────────── Parallel Old           │
│  G1 (整堆，Region 化)                                      │
│  ZGC / Shenandoah (超低延迟)                               │
└───────────────────────────────────────────────────────────┘
```

| 收集器 | 特点 | 适用场景 |
| :--- | :--- | :--- |
| **Serial** | 单线程，STW | 客户端、小内存 |
| **ParNew** | 多线程版 Serial | 配合 CMS 使用 |
| **Parallel Scavenge** | 吞吐量优先 | 后台计算任务 |
| **CMS** | 并发标记清除，低延迟 | Web 服务（已废弃） |
| **G1** | Region 化，可预测停顿 | 大堆、低延迟要求 |
| **ZGC** | 着色指针，<10ms 停顿 | 超大堆、极低延迟 |

### 3.5 内存分配策略

1. **对象优先在 Eden 分配**
2. **大对象直接进入老年代** (`-XX:PretenureSizeThreshold`)
3. **长期存活对象进入老年代** (年龄阈值默认 15)
4. **动态年龄判定** (Survivor 区相同年龄对象超过一半)
5. **空间分配担保** (Minor GC 前检查老年代空间)

---

## 💻 代码实践清单

### 实验1: 循环引用与可达性分析

```bash
GET /chapter03/circular-reference
```

**实验代码**: `Chapter03Controller.java:35`

```java
// 对象 A 和 B 相互引用，但从 GC Roots 不可达
ObjectA a = new ObjectA();
ObjectB b = new ObjectB();
a.ref = b;
b.ref = a;
a = null;
b = null;
System.gc();  // 可以被回收！证明 JVM 使用可达性分析
```

### 实验2: 四种引用类型对比

```bash
GET /chapter03/reference-types
```

**实验代码**: `Chapter03Controller.java:66`

### 实验3: finalize 自救机制

```bash
GET /chapter03/finalize-rescue
```

**实验代码**: `Chapter03Controller.java:106`

### 实验4: Eden 区分配

```bash
# VM 参数
-Xms20m -Xmx20m -Xmn10m -XX:+PrintGCDetails -XX:SurvivorRatio=8

GET /chapter03/allocation-eden
```

**实验代码**: `Chapter03Controller.java:129`

### 实验5: 大对象直接进入老年代

```bash
# VM 参数
-XX:PretenureSizeThreshold=3145728 -XX:+UseSerialGC

GET /chapter03/large-object
```

**实验代码**: `Chapter03Controller.java:148`

### 实验6: 年龄晋升

```bash
# VM 参数
-XX:MaxTenuringThreshold=1 -XX:+PrintGCDetails

GET /chapter03/tenuring
```

**实验代码**: `Chapter03Controller.java:163`

### 实验7: TLAB 分配

```bash
# 对比开启/关闭 TLAB
-XX:+UseTLAB
-XX:-UseTLAB

GET /chapter03/tlab?iterations=200000
```

**实验代码**: `Chapter03Controller.java:269`

### 实验8: G1 字符串去重

```bash
# VM 参数（JDK9+）
-XX:+UseG1GC -XX:+UseStringDeduplication

GET /chapter03/string-dedup?count=500000
```

**实验代码**: `Chapter03Controller.java:299`

---

## 🏭 生产实践建议

### 1. GC 收集器选择

```bash
# 吞吐量优先（批处理、后台计算）
-XX:+UseParallelGC

# 低延迟优先（Web 服务）
-XX:+UseG1GC -XX:MaxGCPauseMillis=200

# 超大堆 + 极低延迟（>32G）
-XX:+UseZGC
```

### 2. 新生代调优

```bash
# Eden:S0:S1 = 8:1:1
-XX:SurvivorRatio=8

# 晋升年龄（默认 15）
-XX:MaxTenuringThreshold=15

# 新生代大小建议：整堆的 1/4 ~ 1/3
-Xmn2g  # 或 -XX:NewRatio=2
```

### 3. GC 日志分析

```bash
# JDK 9+ 统一日志
-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10m

# 关键指标
# - Minor GC 频率和耗时
# - Full GC 频率（应该很少）
# - 晋升到老年代的数据量
```

### 4. 内存泄漏排查要点

```java
// 常见泄漏场景
// 1. 静态集合持有对象
static List<Object> cache = new ArrayList<>();

// 2. 忘记关闭资源
Connection conn = getConnection();  // 未关闭

// 3. 内部类持有外部类引用
class Outer {
    class Inner {}  // Inner 隐式持有 Outer.this
}

// 4. ThreadLocal 未清理
threadLocal.set(value);  // 忘记 remove()
```

---

## 🎯 面试考点提炼

### 高频问题

1. **"如何判断对象是否可以被回收？"**
   - 可达性分析：从 GC Roots 出发，不可达即可回收
   - GC Roots：栈中引用、静态变量、常量、JNI 引用

2. **"强软弱虚引用的区别？"**
   - 强：不回收
   - 软：内存不足回收（缓存）
   - 弱：下次 GC 回收（WeakHashMap）
   - 虚：随时回收，仅用于通知

3. **"CMS 和 G1 的区别？"**
   - CMS：老年代并发收集，标记-清除，有碎片
   - G1：分 Region，可预测停顿，标记-整理

4. **"什么是三色标记？如何解决漏标？"**
   - 白：未访问；灰：访问中；黑：已完成
   - 漏标条件：黑→白新增引用 + 灰→白删除引用
   - 解决：CMS 增量更新 / G1 SATB

5. **"对象进入老年代的条件？"**
   - 年龄达到阈值（默认15）
   - 大对象直接分配
   - Survivor 放不下
   - 动态年龄判定

### 进阶问题

6. **"Minor GC 触发条件？"**
   - Eden 区满

7. **"Full GC 触发条件？"**
   - 老年代空间不足
   - 元空间不足
   - 显式调用 `System.gc()`
   - CMS 并发失败

8. **"TLAB 是什么？有什么作用？"**
   - 线程本地分配缓冲区
   - 每个线程在 Eden 有私有分配区域
   - 避免多线程分配时的锁竞争

---

## 📚 相关资源

- 书籍章节: 《深入理解JVM》第3章 3.1-3.8
- 异常场景: `exceptionlab/scenario/GcOverheadScenario.java`
- 监控工具: `common/JvmMemoryMonitor.java`
