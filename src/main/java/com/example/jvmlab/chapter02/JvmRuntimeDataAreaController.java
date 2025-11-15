package com.example.jvmlab.chapter02;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.lang.management.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JVM运行时数据区域深度实验Controller
 * 
 * ===================================================================================
 * 【实验设计思路】
 * ===================================================================================
 * 本类基于《深入理解Java虚拟机（第3版）》第2章"Java内存区域与内存溢出异常"设计
 * 
 * 1. 实验准备阶段（必读）：
 *    - 启动应用前配置JVM参数（见README.md）
 *    - 启动JProfiler并attach到应用进程
 *    - 访问 /jvm-experiment/ 查看所有可用实验接口
 * 
 * 2. 实验执行顺序（建议）：
 *    第一步：观察初始状态 -> GET /all/memory-status
 *    第二步：程序计数器实验 -> 理论学习为主（无法直接观测）
 *    第三步：虚拟机栈实验 -> POST /stack/* 系列接口
 *    第四步：本地方法栈实验 -> POST /native-stack/test
 *    第五步：堆内存实验 -> POST /heap/* 系列接口（重点）
 *    第六步：方法区实验 -> POST /metaspace/* 系列接口（重点）
 *    第七步：直接内存实验 -> POST /direct-memory/* 系列接口
 *    第八步：综合实战 -> POST /comprehensive/* 系列接口
 * 
 * 3. 每个实验的标准流程：
 *    ① 调用接口前：在JProfiler中记录当前内存状态
 *    ② 调用接口：触发特定场景
 *    ③ 调用接口后：观察JProfiler中内存变化
 *    ④ 对比分析：验证理论知识
 * 
 * 4. 实验验证目标：
 *    - 理解各个运行时数据区域的作用和特点
 *    - 掌握内存溢出的触发条件和排查方法
 *    - 学会使用JProfiler等工具进行内存分析
 *    - 积累真实的生产环境内存问题排查经验
 * 
 * ===================================================================================
 * 【JVM参数配置】（启动时必须配置）
 * ===================================================================================
 * -Xms512m                                    # 初始堆大小
 * -Xmx512m                                    # 最大堆大小
 * -Xmn256m                                    # 新生代大小
 * -XX:MetaspaceSize=128m                      # 初始元空间大小
 * -XX:MaxMetaspaceSize=256m                   # 最大元空间大小
 * -Xss512k                                    # 每个线程栈大小
 * -XX:MaxDirectMemorySize=256m                # 最大直接内存
 * -XX:+PrintGCDetails                         # 打印GC详情
 * -XX:+PrintGCDateStamps                      # 打印GC时间戳
 * -Xloggc:gc.log                             # GC日志文件
 * -XX:+HeapDumpOnOutOfMemoryError            # OOM时dump堆
 * -XX:HeapDumpPath=./dumps                   # dump文件路径
 * -XX:+UseG1GC                               # 使用G1垃圾收集器
 * 
 * @author JVM实战专家
 * @version 3.0
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/jvm-experiment")
public class JvmRuntimeDataAreaController {

    // ==================== 数据持有容器（用于模拟内存占用） ====================
    
    /**
     * 堆内存持有容器 - 模拟堆内存占用
     * 在实际生产环境中，这类似于缓存、集合等占用大量堆内存的对象
     */
    private final List<byte[]> heapMemoryHolder = new CopyOnWriteArrayList<>();
    
    /**
     * 对象持有容器 - 用于防止对象被GC回收
     */
    private final List<Object> objectHolder = new CopyOnWriteArrayList<>();
    
    /**
     * 线程池 - 用于线程相关实验
     */
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * 直接内存持有容器 - 用于模拟直接内存占用
     */
    private final List<java.nio.ByteBuffer> directBufferHolder = new CopyOnWriteArrayList<>();

    // ==================== 首页接口（实验导航） ====================
    
    /**
     * 实验平台首页 - 展示所有可用的实验接口
     * 
     * 【使用说明】
     * 1. 启动应用后首先访问此接口
     * 2. 根据实验需求选择对应的接口进行测试
     * 3. 建议按照推荐顺序逐个实验
     * 
     * @return 实验接口列表和使用说明
     */
    @GetMapping("/")
    public String index() {
        log.info("访问JVM实验平台首页");
        return """
            ═══════════════════════════════════════════════════════════════
                        JVM运行时数据区域深度实验平台
            ═══════════════════════════════════════════════════════════════
            
            【实验前准备】
            1. 确保已配置JVM参数（见类注释）
            2. 启动JProfiler并attach到当前进程
            3. 准备好Postman或curl进行接口调用
            
            ═══════════════════════════════════════════════════════════════
            【第一步：初始状态观察】
            ═══════════════════════════════════════════════════════════════
            GET  /jvm-experiment/all/memory-status
                 → 查看所有内存区域的初始状态（必须先执行）
            
            ═══════════════════════════════════════════════════════════════
            【第二步：程序计数器（理论学习）】
            ═══════════════════════════════════════════════════════════════
            GET  /jvm-experiment/program-counter/explain
                 → 程序计数器理论说明（无法直接观测）
            
            ═══════════════════════════════════════════════════════════════
            【第三步：虚拟机栈实验】（对应书2.2.2节）
            ═══════════════════════════════════════════════════════════════
            GET  /jvm-experiment/stack/explain
                 → 虚拟机栈的原理说明
            
            POST /jvm-experiment/stack/local-variable-table?depth=10
                 → 观察局部变量表占用栈帧空间
                 → 验证：不同类型变量的Slot占用
            
            POST /jvm-experiment/stack/operand-stack?operations=100
                 → 观察操作数栈的工作过程
                 → 验证：方法调用时的数据入栈出栈
            
            POST /jvm-experiment/stack/dynamic-linking
                 → 观察动态连接过程
                 → 验证：方法调用时符号引用转换为直接引用
            
            POST /jvm-experiment/stack/stack-overflow?depth=10000
                 → 触发StackOverflowError
                 → 验证：栈深度超限导致的异常
            
            POST /jvm-experiment/stack/thread-stack-memory?threadCount=100
                 → 创建大量线程观察栈内存占用
                 → 验证：每个线程独立拥有虚拟机栈
            
            ═══════════════════════════════════════════════════════════════
            【第四步：本地方法栈实验】（对应书2.2.3节）
            ═══════════════════════════════════════════════════════════════
            GET  /jvm-experiment/native-stack/explain
                 → 本地方法栈原理说明
            
            POST /jvm-experiment/native-stack/native-method-call
                 → 调用本地方法观察本地方法栈
                 → 验证：Native方法执行时使用本地方法栈
            
            ═══════════════════════════════════════════════════════════════
            【第五步：堆内存实验】（对应书2.2.1节 - 重点）
            ═══════════════════════════════════════════════════════════════
            GET  /jvm-experiment/heap/info
                 → 查看堆内存详细信息
            
            GET  /jvm-experiment/heap/explain
                 → 堆内存的原理说明
            
            POST /jvm-experiment/heap/allocate-young-gen?objectCount=10000
                 → 在新生代分配大量小对象
                 → 验证：Eden区分配对象的过程
            
            POST /jvm-experiment/heap/allocate-old-gen?sizeMB=100
                 → 分配大对象直接进入老年代
                 → 验证：大对象直接进入老年代的机制
            
            POST /jvm-experiment/heap/trigger-minor-gc
                 → 触发Minor GC
                 → 验证：新生代GC的过程
            
            POST /jvm-experiment/heap/trigger-full-gc
                 → 触发Full GC
                 → 验证：老年代GC的过程
            
            POST /jvm-experiment/heap/heap-oom
                 → 触发堆内存溢出
                 → 验证：Java heap space OOM
            
            POST /jvm-experiment/heap/clear
                 → 清空持有的对象
            
            ═══════════════════════════════════════════════════════════════
            【第六步：方法区/元空间实验】（对应书2.2.5节 - 重点）
            ═══════════════════════════════════════════════════════════════
            GET  /jvm-experiment/metaspace/info
                 → 查看元空间详细信息
            
            GET  /jvm-experiment/metaspace/explain
                 → 方法区/元空间原理说明
            
            POST /jvm-experiment/metaspace/load-classes?count=1000
                 → 动态加载大量类
                 → 验证：类信息存储在元空间
            
            POST /jvm-experiment/metaspace/constant-pool?stringCount=100000
                 → 向运行时常量池添加大量字符串
                 → 验证：字符串常量池的工作机制
            
            POST /jvm-experiment/metaspace/metaspace-oom
                 → 触发元空间溢出
                 → 验证：Metaspace OOM
            
            ═══════════════════════════════════════════════════════════════
            【第七步：直接内存实验】（对应书2.2.6节）
            ═══════════════════════════════════════════════════════════════
            GET  /jvm-experiment/direct-memory/explain
                 → 直接内存原理说明
            
            POST /jvm-experiment/direct-memory/allocate?sizeMB=50
                 → 分配直接内存
                 → 验证：NIO的DirectByteBuffer使用堆外内存
            
            POST /jvm-experiment/direct-memory/direct-oom
                 → 触发直接内存溢出
                 → 验证：Direct buffer memory OOM
            
            POST /jvm-experiment/direct-memory/clear
                 → 清理直接内存
            
            ═══════════════════════════════════════════════════════════════
            【第八步：综合实战演练】
            ═══════════════════════════════════════════════════════════════
            POST /jvm-experiment/comprehensive/simulate-production-leak
                 → 模拟生产环境内存泄漏场景
                 → 验证：内存泄漏的排查思路
            
            POST /jvm-experiment/comprehensive/simulate-high-concurrency
                 → 模拟高并发场景下的内存表现
                 → 验证：高并发对各个区域的影响
            
            GET  /jvm-experiment/comprehensive/generate-report
                 → 生成内存分析报告
                 → 输出：完整的内存状态报告
            
            ═══════════════════════════════════════════════════════════════
            【工具推荐】
            ═══════════════════════════════════════════════════════════════
            1. JProfiler - 实时监控内存变化（推荐）
            2. JVisualVM - JDK自带的监控工具
            3. Arthas - 阿里开源的Java诊断工具
            4. MAT - Eclipse Memory Analyzer（分析heap dump）
            
            ═══════════════════════════════════════════════════════════════
            """;
    }

    // ==================== 初始状态观察 ====================
    
    /**
     * 获取所有内存区域的初始状态
     * 
     * 【实验目的】
     * 作为实验的baseline，记录各个内存区域的初始状态
     * 
     * 【观察重点】
     * 1. JProfiler中的Memory视图：堆内存分布
     * 2. Telemetries视图：实时内存趋势
     * 3. 与后续实验对比：观察各区域的变化
     * 
     * 【预期结果】
     * - 堆内存：使用率较低（通常<10%）
     * - 元空间：已加载SpringBoot相关类
     * - 线程数：10-30个（SpringBoot默认线程）
     * 
     * @return 完整的内存状态报告
     */
    @GetMapping("/all/memory-status")
    public Map<String, Object> getAllMemoryStatus() {
        log.info("============ 开始获取所有内存区域状态 ============");
        
        Map<String, Object> status = new LinkedHashMap<>();
        
        // 1. 堆内存状态
        status.put("1_heap", getHeapInfo());
        log.info("堆内存状态已收集");
        
        // 2. 元空间状态
        status.put("2_metaspace", getMetaspaceInfo());
        log.info("元空间状态已收集");
        
        // 3. 线程信息（反映虚拟机栈）
        status.put("3_threads", getThreadInfo());
        log.info("线程信息已收集");
        
        // 4. GC信息
        status.put("4_gc", getGCInfo());
        log.info("GC信息已收集");
        
        // 5. 直接内存（估算）
        status.put("5_direct_memory", getDirectMemoryInfo());
        log.info("直接内存信息已收集");
        
        // 6. 系统信息
        status.put("6_system", getSystemInfo());
        log.info("系统信息已收集");
        
        log.info("============ 内存状态收集完成 ============");
        return status;
    }

    // ==================== 第二步：程序计数器 ====================
    
    /**
     * 程序计数器原理说明
     * 
     * 【对应书籍】《深入理解Java虚拟机》2.2.1节
     * 
     * 【实验说明】
     * 程序计数器（Program Counter Register）是唯一一个在JVM规范中
     * 没有规定任何OutOfMemoryError情况的区域。
     * 
     * 【核心知识点】
     * 1. 线程私有：每个线程都有独立的程序计数器
     * 2. 记录位置：当前线程执行的字节码行号指示器
     * 3. 分支控制：实现循环、跳转、异常处理、线程恢复等功能
     * 4. 方法区分：
     *    - 执行Java方法：记录正在执行的字节码指令地址
     *    - 执行Native方法：值为undefined
     * 5. 内存占用：很小，可以忽略不计
     * 
     * 【为什么无法观测】
     * 程序计数器是JVM内部使用的寄存器级别的结构，
     * 不会暴露给用户态，因此无法通过工具直接观测。
     * 
     * @return 程序计数器原理说明
     */
    @GetMapping("/program-counter/explain")
    public Map<String, Object> explainProgramCounter() {
        log.info("============ 程序计数器原理说明 ============");
        
        Map<String, Object> explanation = new LinkedHashMap<>();
        
        explanation.put("定义", "线程私有的内存区域，记录当前线程执行的字节码行号");
        explanation.put("作用", List.of(
            "1. 字节码解释器通过改变计数器值来选取下一条需要执行的字节码指令",
            "2. 分支、循环、跳转、异常处理、线程恢复等功能都依赖程序计数器",
            "3. 多线程切换后能恢复到正确的执行位置"
        ));
        explanation.put("特点", List.of(
            "线程私有 - 每个线程都有独立的程序计数器",
            "内存很小 - 占用内存可以忽略不计",
            "唯一不会OOM - JVM规范中唯一没有规定OutOfMemoryError的区域"
        ));
        explanation.put("观测说明", "程序计数器无法直接观测，属于JVM底层实现细节");
        
        // 演示多线程时程序计数器的独立性
        explanation.put("代码演示", demonstrateProgramCounter());
        
        log.info("程序计数器原理说明完成");
        return explanation;
    }
    
    /**
     * 演示程序计数器在多线程环境下的独立性
     * 
     * 【验证目标】
     * 虽然无法直接观测程序计数器，但可以通过多线程执行
     * 不同的代码来验证每个线程都有独立的程序计数器
     */
    private Map<String, Object> demonstrateProgramCounter() {
        Map<String, Object> demo = new LinkedHashMap<>();
        AtomicInteger thread1Counter = new AtomicInteger(0);
        AtomicInteger thread2Counter = new AtomicInteger(0);
        
        // 线程1：执行加法循环
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                thread1Counter.incrementAndGet();
                // 模拟字节码指令执行
            }
        }, "PC-Demo-Thread-1");
        
        // 线程2：执行减法循环
        Thread t2 = new Thread(() -> {
            for (int i = 100; i > 0; i--) {
                thread2Counter.incrementAndGet();
                // 模拟字节码指令执行
            }
        }, "PC-Demo-Thread-2");
        
        t1.start();
        t2.start();
        
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        demo.put("说明", "两个线程独立执行不同的代码逻辑，各自的程序计数器记录各自的执行位置");
        demo.put("线程1执行次数", thread1Counter.get());
        demo.put("线程2执行次数", thread2Counter.get());
        demo.put("结论", "每个线程都有独立的程序计数器，互不干扰");
        
        return demo;
    }

    // ==================== 第三步：虚拟机栈实验 ====================
    
    /**
     * 虚拟机栈原理说明
     * 
     * 【对应书籍】《深入理解Java虚拟机》2.2.2节
     * 
     * 【核心概念】
     * 1. 栈帧（Stack Frame）：每个方法对应一个栈帧
     * 2. 栈帧结构：
     *    - 局部变量表（Local Variable Table）
     *    - 操作数栈（Operand Stack）
     *    - 动态连接（Dynamic Linking）
     *    - 方法返回地址（Return Address）
     * 
     * 【内存异常】
     * 1. StackOverflowError：栈深度超过虚拟机允许的深度
     * 2. OutOfMemoryError：栈扩展时无法申请到足够内存（较少见）
     * 
     * @return 虚拟机栈原理说明
     */
    @GetMapping("/stack/explain")
    public Map<String, Object> explainVMStack() {
        log.info("============ 虚拟机栈原理说明 ============");
        
        Map<String, Object> explanation = new LinkedHashMap<>();
        
        explanation.put("定义", "线程私有的内存区域，描述Java方法执行的内存模型");
        explanation.put("生命周期", "与线程相同");
        
        Map<String, String> stackFrame = new LinkedHashMap<>();
        stackFrame.put("局部变量表", "存储方法参数和局部变量，编译期可知大小");
        stackFrame.put("操作数栈", "方法执行过程中的操作数临时存储区域");
        stackFrame.put("动态连接", "支持方法调用过程中的动态连接（符号引用→直接引用）");
        stackFrame.put("方法返回地址", "方法退出后返回到调用该方法的位置");
        explanation.put("栈帧结构", stackFrame);
        
        explanation.put("可能异常", List.of(
            "StackOverflowError - 线程请求的栈深度大于虚拟机允许的深度",
            "OutOfMemoryError - 栈扩展时无法申请到足够的内存（HotSpot不会出现）"
        ));
        
        log.info("虚拟机栈原理说明完成");
        return explanation;
    }
    
    /**
     * 实验1：局部变量表占用栈帧空间
     * 
     * 【实验目的】
     * 观察不同类型和数量的局部变量对栈帧大小的影响
     * 
     * 【知识点】
     * 1. Slot：局部变量表的容量单位
     * 2. 64位类型（long、double）占用2个Slot
     * 3. 其他类型占用1个Slot
     * 4. 实例方法的第0个Slot存放this引用
     * 
     * 【观察方法】
     * 1. 使用 javap -v 查看字节码中的 LocalVariableTable
     * 2. 在JProfiler中观察线程栈的深度和大小
     * 
     * 【预期结果】
     * 局部变量越多，栈帧越大，相同栈空间下能递归的深度越小
     * 
     * @param depth 递归深度
     * @return 局部变量表测试结果
     */
    @PostMapping("/stack/local-variable-table")
    public Map<String, Object> testLocalVariableTable(@RequestParam(defaultValue = "10") int depth) {
        log.info("============ 开始局部变量表实验，递归深度: {} ============", depth);
        
        Map<String, Object> result = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            // 调用包含大量局部变量的递归方法
            int finalResult = recursiveWithManyLocalVariables(0, depth);
            
            result.put("status", "success");
            result.put("递归深度", depth);
            result.put("最终结果", finalResult);
            result.put("执行时间ms", System.currentTimeMillis() - startTime);
            result.put("说明", List.of(
                "每个栈帧包含多个局部变量（byte、short、int、long、float、double、char、boolean、Object）",
                "long和double类型占用2个Slot，其他类型占用1个Slot",
                "局部变量越多，栈帧越大，能递归的深度越小"
            ));
            
            log.info("局部变量表实验成功完成");
        } catch (StackOverflowError e) {
            result.put("status", "overflow");
            result.put("错误", "StackOverflowError");
            result.put("达到深度", "约" + depth);
            result.put("说明", "栈深度超限，这证明了局部变量表占用栈空间");
            log.error("发生栈溢出，深度: {}", depth);
        }
        
        return result;
    }
    
    /**
     * 包含多种类型局部变量的递归方法
     * 目的：演示局部变量表的Slot分配
     */
    private int recursiveWithManyLocalVariables(int current, int target) {
        // 各种类型的局部变量（演示Slot分配）
        byte b = 1;           // 占用1个Slot
        short s = 2;          // 占用1个Slot
        int i = 3;            // 占用1个Slot
        long l = 4L;          // 占用2个Slot
        float f = 5.0f;       // 占用1个Slot
        double d = 6.0;       // 占用2个Slot
        char c = '7';         // 占用1个Slot
        boolean bool = true;  // 占用1个Slot
        Object obj = new Object(); // 占用1个Slot（引用类型）
        String str = "test";  // 占用1个Slot（引用类型）
        
        if (current >= target) {
            return current;
        }
        
        // 使用局部变量（防止被编译器优化掉）
        int sum = b + s + i + (int)l + (int)f + (int)d + c + (bool ? 1 : 0);
        
        return recursiveWithManyLocalVariables(current + 1, target) + sum;
    }
    
    /**
     * 实验2：操作数栈的工作过程
     * 
     * 【实验目的】
     * 通过复杂计算演示操作数栈的入栈出栈过程
     * 
     * 【知识点】
     * 1. 操作数栈用于保存计算过程的中间结果
     * 2. 同时作为计算过程中变量临时的存储空间
     * 3. 栈深度在编译期就已经确定
     * 
     * 【观察方法】
     * 使用 javap -v 查看字节码，观察 stack=? 的值
     * 
     * @param operations 执行的计算次数
     * @return 操作数栈测试结果
     */
    @PostMapping("/stack/operand-stack")
    public Map<String, Object> testOperandStack(@RequestParam(defaultValue = "100") int operations) {
        log.info("============ 开始操作数栈实验，操作次数: {} ============", operations);
        
        Map<String, Object> result = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();
        
        int sum = 0;
        for (int i = 0; i < operations; i++) {
            // 复杂计算，演示操作数栈的使用
            // 在字节码层面，每个操作符都会涉及操作数的入栈出栈
            int temp = complexCalculation(i);
            sum += temp;
        }
        
        result.put("status", "success");
        result.put("操作次数", operations);
        result.put("计算结果", sum);
        result.put("执行时间ms", System.currentTimeMillis() - startTime);
        result.put("说明", List.of(
            "每次计算都涉及多次操作数入栈出栈",
            "操作数栈深度在编译期确定",
            "可通过 javap -v 查看字节码中的 stack 值"
        ));
        
        log.info("操作数栈实验完成，结果: {}", sum);
        return result;
    }
    
    /**
     * 复杂计算方法 - 演示操作数栈的频繁使用
     */
    private int complexCalculation(int x) {
        // 复杂的数学表达式，会产生多次操作数栈的入栈出栈
        int a = x * 2;
        int b = x + 10;
        int c = a - b;
        int d = c * 3;
        int e = d / 2;
        return (a + b) * (c - d) + e;
    }
    
    /**
     * 实验3：动态连接过程
     * 
     * 【实验目的】
     * 演示方法调用时符号引用转换为直接引用的过程
     * 
     * 【知识点】
     * 1. 每个栈帧都包含一个指向运行时常量池中该栈帧所属方法的引用
     * 2. 这个引用的作用是支持方法调用过程中的动态连接
     * 3. 符号引用在运行期转化为直接引用
     * 
     * 【观察方法】
     * 1. 使用 -XX:+TraceClassLoading 观察类加载
     * 2. 使用 javap -v 查看字节码中的常量池
     * 
     * @return 动态连接测试结果
     */
    @PostMapping("/stack/dynamic-linking")
    public Map<String, Object> testDynamicLinking() {
        log.info("============ 开始动态连接实验 ============");
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        // 第一次调用：符号引用转换为直接引用
        long firstCallStart = System.nanoTime();
        String result1 = dynamicMethod("第一次调用");
        long firstCallTime = System.nanoTime() - firstCallStart;
        
        // 第二次调用：直接使用已解析的直接引用（更快）
        long secondCallStart = System.nanoTime();
        String result2 = dynamicMethod("第二次调用");
        long secondCallTime = System.nanoTime() - secondCallStart;
        
        result.put("status", "success");
        result.put("第一次调用结果", result1);
        result.put("第一次调用耗时ns", firstCallTime);
        result.put("第二次调用结果", result2);
        result.put("第二次调用耗时ns", secondCallTime);
        result.put("说明", List.of(
            "第一次调用时，符号引用转换为直接引用",
            "后续调用直接使用已解析的直接引用",
            "这就是动态连接的过程",
            "注意：实际性能差异可能因JIT编译而不明显"
        ));
        
        log.info("动态连接实验完成");
        return result;
    }
    
    /**
     * 用于演示动态连接的方法
     */
    private String dynamicMethod(String param) {
        return "动态连接演示: " + param + ", 时间: " + System.currentTimeMillis();
    }
    
    /**
     * 实验4：触发StackOverflowError
     * 
     * 【实验目的】
     * 演示栈深度超限导致的StackOverflowError
     * 
     * 【触发条件】
     * 1. 线程请求的栈深度大于虚拟机允许的深度
     * 2. 常见原因：无限递归、递归深度过大
     * 
     * 【JVM参数影响】
     * -Xss 设置栈大小，值越小越容易溢出
     * 
     * 【观察重点】
     * 1. 异常堆栈信息
     * 2. 达到的递归深度
     * 3. 在JProfiler中观察线程栈的使用情况
     * 
     * 【预期结果】
     * 抛出 StackOverflowError
     * 
     * @param depth 目标递归深度
     * @return 测试结果
     */
    @PostMapping("/stack/stack-overflow")
    public Map<String, Object> testStackOverflow(@RequestParam(defaultValue = "10000") int depth) {
        log.info("============ 开始StackOverflow实验，目标深度: {} ============", depth);
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            int finalDepth = recursiveMethod(0, depth);
            result.put("status", "completed");
            result.put("达到深度", finalDepth);
            result.put("说明", "在目标深度内未发生栈溢出");
            log.info("递归完成，深度: {}", finalDepth);
        } catch (StackOverflowError e) {
            result.put("status", "overflow");
            result.put("异常类型", "StackOverflowError");
            result.put("异常消息", e.getMessage());
            result.put("目标深度", depth);
            result.put("说明", List.of(
                "栈深度超过虚拟机允许的深度",
                "每个方法调用都会创建一个栈帧",
                "栈帧包含局部变量表、操作数栈等信息",
                "递归调用会不断创建新栈帧，最终导致栈溢出",
                "可通过 -Xss 参数调整栈大小"
            ));
            log.error("发生StackOverflowError，深度: {}", depth, e);
        }
        
        return result;
    }
    
    /**
     * 简单的递归方法 - 用于触发栈溢出
     */
    private int recursiveMethod(int current, int target) {
        if (current >= target) {
            return current;
        }
        // 添加一些局部变量增加栈帧大小
        int a = current;
        String b = "depth-" + current;
        double c = current * 1.5;
        return recursiveMethod(current + 1, target);
    }
    
    /**
     * 实验5：线程栈内存占用
     * 
     * 【实验目的】
     * 验证每个线程都有独立的虚拟机栈
     * 
     * 【知识点】
     * 1. 虚拟机栈是线程私有的
     * 2. 每个线程都有独立的虚拟机栈
     * 3. 线程数 × 栈大小 = 总栈内存占用
     * 
     * 【观察方法】
     * 1. 在JProfiler的Threads视图中观察线程数量
     * 2. 观察系统内存的变化（非堆内存）
     * 3. 使用 jstack 命令查看线程栈信息
     * 
     * 【预期结果】
     * 创建N个线程，非堆内存增加约 N × Xss
     * 
     * @param threadCount 要创建的线程数
     * @return 测试结果
     */
    @PostMapping("/stack/thread-stack-memory")
    public Map<String, Object> testThreadStackMemory(@RequestParam(defaultValue = "100") int threadCount) {
        log.info("============ 开始线程栈内存实验，线程数: {} ============", threadCount);
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        // 记录创建线程前的状态
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        int beforeThreadCount = threadMXBean.getThreadCount();
        
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Thread> threads = new ArrayList<>();
        
        try {
            // 创建指定数量的线程
            for (int i = 0; i < threadCount; i++) {
                final int threadNum = i;
                Thread thread = new Thread(() -> {
                    try {
                        log.debug("线程 {} 启动", threadNum);
                        latch.countDown();
                        // 保持线程存活30秒，便于观察
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.debug("线程 {} 被中断", threadNum);
                    }
                }, "StackTest-" + i);
                
                thread.start();
                threads.add(thread);
            }
            
            // 等待所有线程启动完成
            latch.await(5, TimeUnit.SECONDS);
            
            // 记录创建线程后的状态
            int afterThreadCount = threadMXBean.getThreadCount();
            
            result.put("status", "success");
            result.put("创建前线程数", beforeThreadCount);
            result.put("创建后线程数", afterThreadCount);
            result.put("新增线程数", afterThreadCount - beforeThreadCount);
            result.put("目标线程数", threadCount);
            result.put("说明", List.of(
                "每个线程都有独立的虚拟机栈",
                "总栈内存 ≈ 线程数 × 栈大小(-Xss)",
                "这些线程将保持30秒，请在JProfiler中观察",
                "30秒后线程会自动结束"
            ));
            
            log.info("线程创建完成，请在JProfiler中观察，30秒后线程将结束");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.put("status", "interrupted");
            result.put("错误", "线程创建被中断");
            log.error("线程创建被中断", e);
        }
        
        return result;
    }

    // ==================== 第四步：本地方法栈实验 ====================
    
    /**
     * 本地方法栈原理说明
     * 
     * 【对应书籍】《深入理解Java虚拟机》2.2.3节
     * 
     * 【核心概念】
     * 1. 为Native方法服务
     * 2. HotSpot虚拟机将本地方法栈和虚拟机栈合二为一
     * 3. 同样会抛出StackOverflowError和OutOfMemoryError
     * 
     * @return 本地方法栈原理说明
     */
    @GetMapping("/native-stack/explain")
    public Map<String, Object> explainNativeMethodStack() {
        log.info("============ 本地方法栈原理说明 ============");
        
        Map<String, Object> explanation = new LinkedHashMap<>();
        
        explanation.put("定义", "为虚拟机使用到的Native方法服务");
        explanation.put("实现差异", "JVM规范对本地方法栈没有强制规定，不同虚拟机可自由实现");
        explanation.put("HotSpot实现", "HotSpot虚拟机将本地方法栈和虚拟机栈合二为一");
        explanation.put("可能异常", List.of(
            "StackOverflowError - 栈深度超限",
            "OutOfMemoryError - 无法分配足够内存"
        ));
        explanation.put("常见Native方法", List.of(
            "System.currentTimeMillis() - 获取当前时间",
            "Object.hashCode() - 计算对象哈希码",
            "Thread.start() - 启动线程",
            "System.arraycopy() - 数组拷贝"
        ));
        
        log.info("本地方法栈原理说明完成");
        return explanation;
    }
    
    /**
     * 本地方法调用实验
     * 
     * 【实验目的】
     * 通过调用Native方法，演示本地方法栈的使用
     * 
     * 【观察方法】
     * 虽然无法直接观测本地方法栈，但可以通过调用Native方法
     * 来理解本地方法栈的存在和作用
     * 
     * @return 测试结果
     */
    @PostMapping("/native-stack/native-method-call")
    public Map<String, Object> testNativeMethodCall() {
        log.info("============ 开始本地方法调用实验 ============");
        
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> nativeCalls = new ArrayList<>();
        
        // 1. System.currentTimeMillis() - Native方法
        long start1 = System.nanoTime();
        long currentTime = System.currentTimeMillis();
        long time1 = System.nanoTime() - start1;
        nativeCalls.add(Map.of(
            "方法", "System.currentTimeMillis()",
            "返回值", currentTime,
            "耗时ns", time1,
            "说明", "获取当前时间的Native方法"
        ));
        
        // 2. Object.hashCode() - Native方法
        Object obj = new Object();
        long start2 = System.nanoTime();
        int hashCode = obj.hashCode();
        long time2 = System.nanoTime() - start2;
        nativeCalls.add(Map.of(
            "方法", "Object.hashCode()",
            "返回值", hashCode,
            "耗时ns", time2,
            "说明", "计算对象哈希码的Native方法"
        ));
        
        // 3. System.arraycopy() - Native方法
        int[] src = new int[]{1, 2, 3, 4, 5};
        int[] dest = new int[5];
        long start3 = System.nanoTime();
        System.arraycopy(src, 0, dest, 0, 5);
        long time3 = System.nanoTime() - start3;
        nativeCalls.add(Map.of(
            "方法", "System.arraycopy()",
            "返回值", Arrays.toString(dest),
            "耗时ns", time3,
            "说明", "数组拷贝的Native方法"
        ));
        
        result.put("status", "success");
        result.put("Native方法调用", nativeCalls);
        result.put("说明", List.of(
            "这些方法的实现都是用C/C++编写的",
            "调用时使用本地方法栈",
            "HotSpot虚拟机中本地方法栈和虚拟机栈合并",
            "因此在实际使用中无需区分"
        ));
        
        log.info("本地方法调用实验完成");
        return result;
    }

    // ==================== 第五步：堆内存实验 ====================
    
    /**
     * 堆内存原理说明
     * 
     * 【对应书籍】《深入理解Java虚拟机》2.2.1节
     * 
     * 【核心概念】
     * 1. Java堆是被所有线程共享的一块内存区域
     * 2. 虚拟机启动时创建
     * 3. 存放对象实例，几乎所有的对象实例都在这里分配内存
     * 4. 是垃圾收集器管理的主要区域
     * 
     * @return 堆内存原理说明
     */
    @GetMapping("/heap/explain")
    public Map<String, Object> explainHeap() {
        log.info("============ 堆内存原理说明 ============");
        
        Map<String, Object> explanation = new LinkedHashMap<>();
        
        explanation.put("定义", "所有线程共享的内存区域，用于存放对象实例");
        explanation.put("生命周期", "虚拟机启动时创建");
        explanation.put("主要用途", "存放对象实例和数组");
        
        Map<String, String> heapStructure = new LinkedHashMap<>();
        heapStructure.put("新生代(Young Generation)", "包括Eden区和两个Survivor区(From、To)");
        heapStructure.put("老年代(Old Generation)", "存放长期存活的对象");
        heapStructure.put("永久代/元空间", "JDK8之前是永久代，JDK8及以后是元空间（不在堆中）");
        explanation.put("堆结构", heapStructure);
        
        explanation.put("GC分类", Map.of(
            "Minor GC", "发生在新生代的GC，频繁且速度快",
            "Major GC", "发生在老年代的GC",
            "Full GC", "清理整个堆空间和方法区"
        ));
        
        explanation.put("可能异常", "OutOfMemoryError: Java heap space");
        
        log.info("堆内存原理说明完成");
        return explanation;
    }
    
    /**
     * 获取堆内存详细信息
     * 
     * 【观察重点】
     * 1. 最大堆内存(MaxMemory)：-Xmx设置的值
     * 2. 当前堆大小(TotalMemory)：当前已分配的堆内存
     * 3. 已使用(UsedMemory)：实际使用的内存
     * 4. 各分区详情：Eden、Survivor、Old区的使用情况
     * 
     * @return 堆内存详细信息
     */
    @GetMapping("/heap/info")
    public Map<String, Object> getHeapInfo() {
        log.info("============ 获取堆内存详细信息 ============");
        
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> heapInfo = new LinkedHashMap<>();
        
        // 基本信息
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        heapInfo.put("最大堆内存MB", maxMemory / 1024 / 1024);
        heapInfo.put("当前堆大小MB", totalMemory / 1024 / 1024);
        heapInfo.put("已使用内存MB", usedMemory / 1024 / 1024);
        heapInfo.put("空闲内存MB", freeMemory / 1024 / 1024);
        heapInfo.put("内存使用率", String.format("%.2f%%", (usedMemory * 100.0 / maxMemory)));
        
        // 详细的堆内存池信息
        List<Map<String, Object>> poolInfo = new ArrayList<>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP) {
                MemoryUsage usage = pool.getUsage();
                Map<String, Object> poolData = new LinkedHashMap<>();
                poolData.put("名称", pool.getName());
                poolData.put("初始大小MB", usage.getInit() / 1024 / 1024);
                poolData.put("已使用MB", usage.getUsed() / 1024 / 1024);
                poolData.put("已提交MB", usage.getCommitted() / 1024 / 1024);
                poolData.put("最大值MB", usage.getMax() / 1024 / 1024);
                poolData.put("使用率", String.format("%.2f%%", 
                    usage.getMax() > 0 ? (usage.getUsed() * 100.0 / usage.getMax()) : 0));
                poolInfo.add(poolData);
            }
        }
        heapInfo.put("堆内存池详情", poolInfo);
        
        log.info("堆内存信息获取完成");
        return heapInfo;
    }
    
    /**
     * 实验1：在新生代分配大量小对象
     * 
     * 【实验目的】
     * 观察新生代的对象分配和Minor GC过程
     * 
     * 【知识点】
     * 1. 新对象优先在Eden区分配
     * 2. Eden区满时触发Minor GC
     * 3. 存活对象移动到Survivor区
     * 4. Survivor区采用复制算法
     * 
     * 【观察方法】
     * 1. JProfiler的Memory视图：观察Eden区使用情况
     * 2. GC日志：观察Minor GC的触发
     * 3. Telemetries：观察内存使用曲线
     * 
     * 【预期结果】
     * Eden区使用率快速上升，触发Minor GC后下降
     * 
     * @param objectCount 要创建的对象数量
     * @return 测试结果
     */
    @PostMapping("/heap/allocate-young-gen")
    public Map<String, Object> allocateInYoungGen(@RequestParam(defaultValue = "10000") int objectCount) {
        log.info("============ 开始新生代分配实验，对象数量: {} ============", objectCount);
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        // 记录分配前的内存状态
        MemoryUsage beforeUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long beforeUsed = beforeUsage.getUsed();
        
        // 分配大量小对象（典型的新生代分配场景）
        List<Object> tempObjects = new ArrayList<>();
        for (int i = 0; i < objectCount; i++) {
            // 创建小对象（一般小于PretenureSizeThreshold）
            tempObjects.add(new byte[1024]); // 1KB的小对象
        }
        
        // 记录分配后的内存状态
        MemoryUsage afterUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long afterUsed = afterUsage.getUsed();
        long allocated = afterUsed - beforeUsed;
        
        result.put("status", "success");
        result.put("分配对象数", objectCount);
        result.put("分配前内存MB", beforeUsed / 1024 / 1024);
        result.put("分配后内存MB", afterUsed / 1024 / 1024);
        result.put("新增内存MB", allocated / 1024 / 1024);
        result.put("说明", List.of(
            "小对象优先在Eden区分配",
            "请在JProfiler中观察Eden区的使用情况",
            "如果Eden区满了会触发Minor GC",
            "存活对象会被复制到Survivor区"
        ));
        
        // 清空引用，使对象可以被GC
        tempObjects.clear();
        
        log.info("新生代分配实验完成，分配了约{}MB内存", allocated / 1024 / 1024);
        return result;
    }
    
    /**
     * 实验2：分配大对象直接进入老年代
     * 
     * 【实验目的】
     * 验证大对象直接分配到老年代的机制
     * 
     * 【知识点】
     * 1. 大对象直接进入老年代（避免在Survivor区复制）
     * 2. -XX:PretenureSizeThreshold 参数控制大对象阈值
     * 3. 该参数只对Serial和ParNew收集器有效
     * 
     * 【观察方法】
     * 在JProfiler中观察Old区的使用情况直接上升
     * 
     * 【预期结果】
     * 老年代使用量增加，新生代基本不变
     * 
     * @param sizeMB 要分配的内存大小(MB)
     * @return 测试结果
     */
    @PostMapping("/heap/allocate-old-gen")
    public Map<String, Object> allocateInOldGen(@RequestParam(defaultValue = "100") int sizeMB) {
        log.info("============ 开始老年代分配实验，大小: {}MB ============", sizeMB);
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            // 记录分配前的状态
            long beforeUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            
            // 分配大对象
            byte[] largeObject = new byte[sizeMB * 1024 * 1024];
            heapMemoryHolder.add(largeObject);
            
            // 记录分配后的状态
            long afterUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            
            result.put("status", "success");
            result.put("分配大小MB", sizeMB);
            result.put("分配前内存MB", beforeUsed / 1024 / 1024);
            result.put("分配后内存MB", afterUsed / 1024 / 1024);
            result.put("实际分配MB", (afterUsed - beforeUsed) / 1024 / 1024);
            result.put("当前持有对象数", heapMemoryHolder.size());
            result.put("说明", List.of(
                "大对象（超过一定阈值）直接分配到老年代",
                "避免在新生代来回复制，提高效率",
                "请在JProfiler中观察Old Generation的变化",
                "G1收集器会将大对象分配到Humongous区"
            ));
            
            log.info("成功分配{}MB大对象到老年代", sizeMB);
        } catch (OutOfMemoryError e) {
            result.put("status", "error");
            result.put("错误类型", "OutOfMemoryError");
            result.put("错误消息", e.getMessage());
            result.put("说明", "堆内存不足，无法分配指定大小的对象");
            log.error("分配大对象失败: {}MB", sizeMB, e);
        }
        
        return result;
    }
    
    /**
     * 实验3：触发Minor GC
     * 
     * 【实验目的】
     * 观察新生代GC的过程和效果
     * 
     * 【触发条件】
     * Eden区空间不足时自动触发
     * 
     * 【观察方法】
     * 1. GC日志中查看Minor GC记录
     * 2. JProfiler观察新生代内存变化
     * 3. 观察Survivor区的对象晋升
     * 
     * 【预期结果】
     * Eden区内存被回收，存活对象移到Survivor区
     * 
     * @return 测试结果
     */
    @PostMapping("/heap/trigger-minor-gc")
    public Map<String, Object> triggerMinorGC() {
        log.info("============ 开始触发Minor GC实验 ============");
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        // 获取GC前的状态
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        Map<String, Long> beforeGC = new LinkedHashMap<>();
        for (GarbageCollectorMXBean gc : gcBeans) {
            beforeGC.put(gc.getName(), gc.getCollectionCount());
        }
        
        long beforeMemory = Runtime.getRuntime().freeMemory();
        
        // 创建大量临时对象填满Eden区
        for (int i = 0; i < 1000; i++) {
            byte[] temp = new byte[100 * 1024]; // 100KB临时对象
            // 不保持引用，使其成为垃圾
        }
        
        // 建议JVM执行GC
        System.gc();
        
        try {
            Thread.sleep(100); // 等待GC完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 获取GC后的状态
        Map<String, Long> afterGC = new LinkedHashMap<>();
        Map<String, Long> gcCounts = new LinkedHashMap<>();
        for (GarbageCollectorMXBean gc : gcBeans) {
            long before = beforeGC.getOrDefault(gc.getName(), 0L);
            long after = gc.getCollectionCount();
            afterGC.put(gc.getName(), after);
            gcCounts.put(gc.getName(), after - before);
        }
        
        long afterMemory = Runtime.getRuntime().freeMemory();
        long freedMemory = (afterMemory - beforeMemory) / 1024 / 1024;
        
        result.put("status", "success");
        result.put("GC前空闲内存MB", beforeMemory / 1024 / 1024);
        result.put("GC后空闲内存MB", afterMemory / 1024 / 1024);
        result.put("释放内存MB", freedMemory);
        result.put("GC执行次数", gcCounts);
        result.put("说明", List.of(
            "Minor GC发生在新生代（Eden区满时）",
            "采用复制算法，速度快",
            "存活对象移动到Survivor区",
            "请查看GC日志了解详细信息"
        ));
        
        log.info("Minor GC触发完成，释放了约{}MB内存", freedMemory);
        return result;
    }
    
    /**
     * 实验4：触发Full GC
     * 
     * 【实验目的】
     * 观察Full GC的过程和影响
     * 
     * 【触发条件】
     * 1. 老年代空间不足
     * 2. 元空间不足
     * 3. System.gc()建议
     * 4. CMS GC promotion failed或concurrent mode failure
     * 
     * 【观察方法】
     * 1. GC日志中的Full GC记录
     * 2. JProfiler观察整个堆的内存变化
     * 3. 注意STW（Stop-The-World）时间
     * 
     * 【预期结果】
     * 整个堆被清理，停顿时间较长
     * 
     * @return 测试结果
     */
    @PostMapping("/heap/trigger-full-gc")
    public Map<String, Object> triggerFullGC() {
        log.info("============ 开始触发Full GC实验 ============");
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        // 获取GC前的状态
        long beforeUsedHeap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        long beforeUsedNonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
        
        Map<String, Long> beforeGCCounts = new LinkedHashMap<>();
        Map<String, Long> beforeGCTime = new LinkedHashMap<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            beforeGCCounts.put(gc.getName(), gc.getCollectionCount());
            beforeGCTime.put(gc.getName(), gc.getCollectionTime());
        }
        
        long startTime = System.currentTimeMillis();
        
        // 显式调用Full GC
        System.gc();
        
        try {
            // 等待GC完成
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long gcTime = System.currentTimeMillis() - startTime;
        
        // 获取GC后的状态
        long afterUsedHeap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        long afterUsedNonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
        
        Map<String, Map<String, Long>> gcDetails = new LinkedHashMap<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Long> detail = new LinkedHashMap<>();
            detail.put("执行次数增加", gc.getCollectionCount() - beforeGCCounts.getOrDefault(gc.getName(), 0L));
            detail.put("耗时增加ms", gc.getCollectionTime() - beforeGCTime.getOrDefault(gc.getName(), 0L));
            gcDetails.put(gc.getName(), detail);
        }
        
        result.put("status", "success");
        result.put("GC前堆内存MB", beforeUsedHeap / 1024 / 1024);
        result.put("GC后堆内存MB", afterUsedHeap / 1024 / 1024);
        result.put("释放堆内存MB", (beforeUsedHeap - afterUsedHeap) / 1024 / 1024);
        result.put("GC前非堆内存MB", beforeUsedNonHeap / 1024 / 1024);
        result.put("GC后非堆内存MB", afterUsedNonHeap / 1024 / 1024);
        result.put("总耗时ms", gcTime);
        result.put("各GC详情", gcDetails);
        result.put("说明", List.of(
            "Full GC会清理整个堆空间和方法区",
            "会造成Stop-The-World（STW），暂停所有应用线程",
            "应该尽量避免Full GC的发生",
            "Full GC是性能问题的常见原因"
        ));
        
        log.info("Full GC完成，释放了约{}MB堆内存", (beforeUsedHeap - afterUsedHeap) / 1024 / 1024);
        return result;
    }
    
    /**
     * 实验5：触发堆内存溢出
     * 
     * 【实验目的】
     * 演示java.lang.OutOfMemoryError: Java heap space
     * 
     * 【触发条件】
     * 堆内存无法满足对象分配需求
     * 
     * 【观察方法】
     * 1. 异常堆栈信息
     * 2. heap dump文件（配置了-XX:+HeapDumpOnOutOfMemoryError）
     * 3. JProfiler的Memory视图看到内存持续增长
     * 
     * 【排查思路】
     * 1. 分析heap dump找出占用内存最多的对象
     * 2. 检查是否存在内存泄漏
     * 3. 评估是否需要增大堆内存
     * 
     * 【预期结果】
     * 抛出OutOfMemoryError: Java heap space
     * 
     * @return 测试结果
     */
    @PostMapping("/heap/heap-oom")
    public Map<String, Object> triggerHeapOOM() {
        log.warn("============ 警告：准备触发堆内存溢出！ ============");
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            List<byte[]> list = new ArrayList<>();
            int count = 0;
            
            // 不断分配内存直到OOM
            while (true) {
                // 每次分配1MB
                byte[] memory = new byte[1024 * 1024];
                list.add(memory);
                count++;
                
                if (count % 100 == 0) {
                    log.info("已分配{}MB内存", count);
                }
            }
        } catch (OutOfMemoryError e) {
            result.put("status", "oom");
            result.put("异常类型", "OutOfMemoryError: Java heap space");
            result.put("异常消息", e.getMessage());
            result.put("说明", List.of(
                "堆内存不足，无法分配新对象",
                "已自动生成heap dump文件（如果配置了-XX:+HeapDumpOnOutOfMemoryError）",
                "可使用MAT(Memory Analyzer Tool)分析heap dump",
                "常见原因：内存泄漏、堆内存设置过小、对象生命周期过长"
            ));
            result.put("排查建议", List.of(
                "1. 使用MAT打开heap dump文件",
                "2. 查看Histogram找出占用内存最多的类",
                "3. 使用Dominator Tree分析对象引用链",
                "4. 检查是否有集合类持续增长未清理",
                "5. 评估是否需要调大-Xmx参数"
            ));
            
            log.error("堆内存溢出！", e);
        }
        
        return result;
    }
    
    /**
     * 清空堆内存持有的对象
     * 
     * 【使用场景】
     * 实验后清理，避免影响后续实验
     * 
     * @return 清理结果
     */
    @PostMapping("/heap/clear")
    public Map<String, Object> clearHeapMemory() {
        log.info("============ 清空堆内存持有对象 ============");
        
        int size = heapMemoryHolder.size();
        heapMemoryHolder.clear();
        objectHolder.clear();
        
        System.gc();
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("清理对象数", size);
        result.put("说明", "已清空所有持有的对象并建议执行GC");
        
        log.info("已清空{}个持有的对象", size);
        return result;
    }

    // ==================== 第六步：方法区/元空间实验 ====================
    
    /**
     * 方法区/元空间原理说明
     * 
     * 【对应书籍】《深入理解Java虚拟机》2.2.5节
     * 
     * 【核心概念】
     * 1. JDK8之前：永久代（PermGen）
     * 2. JDK8及以后：元空间（Metaspace）使用本地内存
     * 3. 存储内容：类信息、常量、静态变量、即时编译器编译后的代码
     * 
     * @return 方法区原理说明
     */
    @GetMapping("/metaspace/explain")
    public Map<String, Object> explainMetaspace() {
        log.info("============ 方法区/元空间原理说明 ============");
        
        Map<String, Object> explanation = new LinkedHashMap<>();
        
        explanation.put("定义", "存储已被虚拟机加载的类信息、常量、静态变量等数据");
        explanation.put("JDK版本差异", Map.of(
            "JDK7及之前", "永久代(PermGen)，使用堆内存",
            "JDK8及之后", "元空间(Metaspace)，使用本地内存"
        ));
        
        explanation.put("存储内容", List.of(
            "类型信息（类名、访问修饰符、字段描述、方法描述等）",
            "运行时常量池（字面量和符号引用）",
            "字段信息（字段名、类型、修饰符）",
            "方法信息（方法名、返回类型、参数、字节码、异常表等）",
            "静态变量",
            "即时编译器编译后的代码缓存"
        ));
        
        explanation.put("为什么移除永久代", List.of(
            "永久代大小难以确定（类和方法的信息大小难以预测）",
            "永久代会导致内存溢出问题",
            "永久代的GC比较复杂",
            "为了与JRockit虚拟机统一"
        ));
        
        explanation.put("元空间优势", List.of(
            "使用本地内存，不受堆大小限制",
            "默认情况下只受系统内存限制",
            "可通过-XX:MaxMetaspaceSize限制最大值",
            "类的元数据和类静态变量分离存储"
        ));
        
        explanation.put("可能异常", "OutOfMemoryError: Metaspace");
        
        log.info("方法区/元空间原理说明完成");
        return explanation;
    }
    
    /**
     * 获取元空间详细信息
     * 
     * 【观察重点】
     * 1. 初始大小：-XX:MetaspaceSize设置的值
     * 2. 最大大小：-XX:MaxMetaspaceSize设置的值
     * 3. 已使用：当前加载的类占用的空间
     * 4. 类加载数量：已加载、总共加载、已卸载的类数量
     * 
     * @return 元空间详细信息
     */
    @GetMapping("/metaspace/info")
    public Map<String, Object> getMetaspaceInfo() {
        log.info("============ 获取元空间详细信息 ============");
        
        Map<String, Object> metaspaceInfo = new LinkedHashMap<>();
        
        // 元空间内存信息
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getName().contains("Metaspace")) {
                MemoryUsage usage = pool.getUsage();
                Map<String, Object> memoryInfo = new LinkedHashMap<>();
                memoryInfo.put("池名称", pool.getName());
                memoryInfo.put("初始大小MB", usage.getInit() / 1024 / 1024);
                memoryInfo.put("已使用MB", usage.getUsed() / 1024 / 1024);
                memoryInfo.put("已提交MB", usage.getCommitted() / 1024 / 1024);
                memoryInfo.put("最大值MB", usage.getMax() < 0 ? "无限制" : usage.getMax() / 1024 / 1024);
                memoryInfo.put("使用率", String.format("%.2f%%", 
                    usage.getMax() > 0 ? (usage.getUsed() * 100.0 / usage.getMax()) : 0));
                
                metaspaceInfo.put("内存信息", memoryInfo);
            }
        }
        
        // 类加载信息
        ClassLoadingMXBean classLoading = ManagementFactory.getClassLoadingMXBean();
        Map<String, Object> classInfo = new LinkedHashMap<>();
        classInfo.put("当前已加载类数量", classLoading.getLoadedClassCount());
        classInfo.put("总共加载类数量", classLoading.getTotalLoadedClassCount());
        classInfo.put("已卸载类数量", classLoading.getUnloadedClassCount());
        classInfo.put("是否详细输出", classLoading.isVerbose());
        
        metaspaceInfo.put("类加载信息", classInfo);
        
        log.info("元空间信息获取完成");
        return metaspaceInfo;
    }
    
    /**
     * 实验1：动态加载大量类
     * 
     * 【实验目的】
     * 观察类加载对元空间的影响
     * 
     * 【知识点】
     * 1. 每个类的元数据都存储在元空间
     * 2. 类加载越多，元空间使用越多
     * 3. 类可以被卸载（需满足特定条件）
     * 
     * 【观察方法】
     * 1. JProfiler观察Metaspace的增长
     * 2. 观察类加载数量的变化
     * 
     * 【预期结果】
     * 元空间使用量增加，类加载数量增加
     * 
     * @param count 要加载的类数量
     * @return 测试结果
     */
    @PostMapping("/metaspace/load-classes")
    public Map<String, Object> loadClasses(@RequestParam(defaultValue = "1000") int count) {
        log.info("============ 开始动态加载类实验，数量: {} ============", count);
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        // 记录加载前的状态
        ClassLoadingMXBean classLoading = ManagementFactory.getClassLoadingMXBean();
        int beforeLoadedCount = classLoading.getLoadedClassCount();
        
        MemoryUsage beforeMetaspace = null;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getName().contains("Metaspace") && pool.getType() == MemoryType.NON_HEAP) {
                beforeMetaspace = pool.getUsage();
                break;
            }
        }
        
        try {
            // 使用动态代理生成类（模拟大量类加载）
            for (int i = 0; i < count; i++) {
                Object proxy = Proxy.newProxyInstance(
                    this.getClass().getClassLoader(),
                    new Class[]{Runnable.class},
                    (p, method, args) -> {
                        // 代理方法实现
                        return null;
                    }
                );
                objectHolder.add(proxy); // 持有引用，防止类被卸载
            }
            
            // 记录加载后的状态
            int afterLoadedCount = classLoading.getLoadedClassCount();
            
            MemoryUsage afterMetaspace = null;
            for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                if (pool.getName().contains("Metaspace") && pool.getType() == MemoryType.NON_HEAP) {
                    afterMetaspace = pool.getUsage();
                    break;
                }
            }
            
            result.put("status", "success");
            result.put("目标加载数", count);
            result.put("加载前类数量", beforeLoadedCount);
            result.put("加载后类数量", afterLoadedCount);
            result.put("实际新增类数", afterLoadedCount - beforeLoadedCount);
            
            if (beforeMetaspace != null && afterMetaspace != null) {
                result.put("元空间使用增加MB", 
                    (afterMetaspace.getUsed() - beforeMetaspace.getUsed()) / 1024 / 1024);
            }
            
            result.put("说明", List.of(
                "每个类的元数据存储在元空间",
                "类加载会增加元空间的使用",
                "请在JProfiler中观察Metaspace的变化",
                "类在满足特定条件下可以被卸载"
            ));
            
            log.info("成功加载{}个类", afterLoadedCount - beforeLoadedCount);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("错误", e.getMessage());
            log.error("类加载失败", e);
        }
        
        return result;
    }
    
    /**
     * 实验2：运行时常量池实验
     * 
     * 【实验目的】
     * 观察字符串常量池的工作机制
     * 
     * 【知识点】
     * 1. JDK7后，字符串常量池从永久代移到堆中
     * 2. String.intern()方法用于操作常量池
     * 3. 常量池可以节省内存空间
     * 
     * 【观察方法】
     * 观察堆内存的变化（因为字符串常量池在堆中）
     * 
     * @param stringCount 要创建的字符串数量
     * @return 测试结果
     */
    @PostMapping("/metaspace/constant-pool")
    public Map<String, Object> testConstantPool(@RequestParam(defaultValue = "100000") int stringCount) {
        log.info("============ 开始运行时常量池实验，字符串数量: {} ============", stringCount);
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        long beforeHeap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        
        // 创建大量字符串并加入常量池
        Set<String> internedStrings = new HashSet<>();
        for (int i = 0; i < stringCount; i++) {
            String str = ("ConstantPoolTest_" + i).intern();
            internedStrings.add(str);
        }
        
        long afterHeap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        
        result.put("status", "success");
        result.put("创建字符串数", stringCount);
        result.put("堆内存增加MB", (afterHeap - beforeHeap) / 1024 / 1024);
        result.put("说明", List.of(
            "JDK7后，字符串常量池移到堆中",
            "String.intern()将字符串添加到常量池",
            "常量池中的字符串可以被共享，节省内存",
            "请观察堆内存的变化而非元空间"
        ));
        
        // 清空引用
        internedStrings.clear();
        
        log.info("运行时常量池实验完成");
        return result;
    }
    
    /**
     * 实验3：触发元空间溢出
     * 
     * 【实验目的】
     * 演示java.lang.OutOfMemoryError: Metaspace
     * 
     * 【触发条件】
     * 元空间无法满足类元数据的分配需求
     * 
     * 【观察方法】
     * 1. 异常堆栈信息
     * 2. JProfiler观察Metaspace持续增长
     * 
     * 【预期结果】
     * 抛出OutOfMemoryError: Metaspace
     * 
     * @return 测试结果
     */
    @PostMapping("/metaspace/metaspace-oom")
    public Map<String, Object> triggerMetaspaceOOM() {
        log.warn("============ 警告：准备触发元空间溢出！ ============");
        log.warn("注意：需要设置较小的MaxMetaspaceSize才能观察到效果");
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            int count = 0;
            // 使用CGLib等工具不断生成类
            while (true) {
                // 使用动态代理生成大量类
                for (int i = 0; i < 100; i++) {
                    Object proxy = Proxy.newProxyInstance(
                        this.getClass().getClassLoader(),
                        new Class[]{Runnable.class, Callable.class},
                        (p, method, args) -> null
                    );
                    objectHolder.add(proxy);
                }
                
                count += 100;
                if (count % 10000 == 0) {
                    log.info("已生成{}个代理类", count);
                }
            }
        } catch (OutOfMemoryError e) {
            result.put("status", "oom");
            result.put("异常类型", "OutOfMemoryError: Metaspace");
            result.put("异常消息", e.getMessage());
            result.put("说明", List.of(
                "元空间不足，无法加载新的类",
                "常见原因：动态生成大量类、使用大量反射、类加载器泄漏",
                "解决方案：增大-XX:MaxMetaspaceSize，或优化代码减少类生成"
            ));
            result.put("排查建议", List.of(
                "1. 检查是否有大量动态代理类生成",
                "2. 检查是否有类加载器泄漏",
                "3. 检查是否使用了过多的第三方库",
                "4. 使用JProfiler查看加载的类",
                "5. 考虑增大-XX:MaxMetaspaceSize参数"
            ));
            
            log.error("元空间溢出！", e);
        }
        
        return result;
    }

    // ==================== 第七步：直接内存实验 ====================
    
    /**
     * 直接内存原理说明
     * 
     * 【对应书籍】《深入理解Java虚拟机》2.2.6节
     * 
     * 【核心概念】
     * 1. 不是JVM运行时数据区的一部分
     * 2. 在Java堆外直接分配的内存
     * 3. NIO使用Native函数库直接分配堆外内存
     * 
     * @return 直接内存原理说明
     */
    @GetMapping("/direct-memory/explain")
    public Map<String, Object> explainDirectMemory() {
        log.info("============ 直接内存原理说明 ============");
        
        Map<String, Object> explanation = new LinkedHashMap<>();
        
        explanation.put("定义", "在Java堆外、直接向系统申请的内存空间");
        explanation.put("主要用途", "NIO中的DirectByteBuffer");
        
        explanation.put("优势", List.of(
            "避免了Java堆和Native堆之间的数据复制",
            "在某些场景下可以显著提高性能",
            "适合频繁的I/O操作"
        ));
        
        explanation.put("劣势", List.of(
            "分配和回收成本较高",
            "不受JVM内存管理",
            "容易造成内存泄漏",
            "可能导致OutOfMemoryError"
        ));
        
        explanation.put("参数控制", "-XX:MaxDirectMemorySize限制最大直接内存");
        explanation.put("可能异常", "OutOfMemoryError: Direct buffer memory");
        
        log.info("直接内存原理说明完成");
        return explanation;
    }
    
    /**
     * 实验1：分配直接内存
     * 
     * 【实验目的】
     * 观察直接内存的分配和使用
     * 
     * 【知识点】
     * 1. DirectByteBuffer使用堆外内存
     * 2. 直接内存不会被GC直接管理
     * 3. 直接内存的回收依赖于DirectByteBuffer对象的GC
     * 
     * 【观察方法】
     * 1. JProfiler无法直接观测直接内存
     * 2. 可通过系统内存监控观察进程内存增长
     * 3. 堆内存基本不变，但进程内存增加
     * 
     * @param sizeMB 要分配的直接内存大小(MB)
     * @return 测试结果
     */
    @PostMapping("/direct-memory/allocate")
    public Map<String, Object> allocateDirectMemory(@RequestParam(defaultValue = "50") int sizeMB) {
        log.info("============ 开始分配直接内存，大小: {}MB ============", sizeMB);
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            long beforeHeap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            
            // 分配直接内存
            java.nio.ByteBuffer directBuffer = java.nio.ByteBuffer.allocateDirect(sizeMB * 1024 * 1024);
            directBufferHolder.add(directBuffer);
            
            long afterHeap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            
            result.put("status", "success");
            result.put("分配大小MB", sizeMB);
            result.put("堆内存变化MB", (afterHeap - beforeHeap) / 1024 / 1024);
            result.put("当前持有DirectBuffer数", directBufferHolder.size());
            result.put("说明", List.of(
                "DirectByteBuffer使用堆外内存",
                "堆内存基本没有变化（只有很小的对象引用）",
                "实际内存在堆外分配",
                "请通过系统监控观察进程内存增长",
                "JProfiler无法直接观测直接内存"
            ));
            
            log.info("成功分配{}MB直接内存", sizeMB);
        } catch (OutOfMemoryError e) {
            result.put("status", "error");
            result.put("错误类型", "OutOfMemoryError");
            result.put("错误消息", e.getMessage());
            result.put("说明", "直接内存不足或超过-XX:MaxDirectMemorySize限制");
            log.error("直接内存分配失败", e);
        }
        
        return result;
    }
    
    /**
     * 实验2：触发直接内存溢出
     * 
     * 【实验目的】
     * 演示java.lang.OutOfMemoryError: Direct buffer memory
     * 
     * 【触发条件】
     * 直接内存超过-XX:MaxDirectMemorySize限制
     * 
     * 【观察方法】
     * 1. 异常堆栈信息
     * 2. 系统内存监控
     * 
     * 【预期结果】
     * 抛出OutOfMemoryError: Direct buffer memory
     * 
     * @return 测试结果
     */
    @PostMapping("/direct-memory/direct-oom")
    public Map<String, Object> triggerDirectMemoryOOM() {
        log.warn("============ 警告：准备触发直接内存溢出！ ============");
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            int count = 0;
            // 不断分配直接内存直到溢出
            while (true) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(10 * 1024 * 1024); // 10MB
                directBufferHolder.add(buffer);
                count++;
                
                if (count % 10 == 0) {
                    log.info("已分配{}个DirectBuffer，总计{}MB", count, count * 10);
                }
            }
        } catch (OutOfMemoryError e) {
            result.put("status", "oom");
            result.put("异常类型", "OutOfMemoryError: Direct buffer memory");
            result.put("异常消息", e.getMessage());
            result.put("说明", List.of(
                "直接内存超过-XX:MaxDirectMemorySize限制",
                "直接内存不受JVM堆内存限制",
                "但受限于物理内存和MaxDirectMemorySize参数",
                "常见于大量使用NIO的应用"
            ));
            result.put("排查建议", List.of(
                "1. 检查是否创建了大量DirectByteBuffer未释放",
                "2. 检查NIO操作是否有内存泄漏",
                "3. 考虑增大-XX:MaxDirectMemorySize参数",
                "4. 使用完DirectByteBuffer后及时清理"
            ));
            
            log.error("直接内存溢出！", e);
        }
        
        return result;
    }
    
    /**
     * 清理直接内存
     * 
     * @return 清理结果
     */
    @PostMapping("/direct-memory/clear")
    public Map<String, Object> clearDirectMemory() {
        log.info("============ 清理直接内存 ============");
        
        int size = directBufferHolder.size();
        directBufferHolder.clear();
        
        // 建议GC清理DirectByteBuffer对象
        System.gc();
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("清理DirectBuffer数", size);
        result.put("说明", "已清空DirectBuffer引用并建议执行GC");
        
        log.info("已清理{}个DirectBuffer", size);
        return result;
    }
    
    /**
     * 获取直接内存信息（估算）
     */
    private Map<String, Object> getDirectMemoryInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("当前持有DirectBuffer数", directBufferHolder.size());
        info.put("说明", "直接内存无法精确统计，仅显示持有的DirectBuffer数量");
        return info;
    }

    // ==================== 第八步：综合实战演练 ====================
    
    /**
     * 综合实验1：模拟生产环境内存泄漏
     * 
     * 【实验目的】
     * 模拟真实的内存泄漏场景，学习排查思路
     * 
     * 【泄漏场景】
     * 1. 集合类持续添加对象不清理
     * 2. 监听器注册后未注销
     * 3. ThreadLocal使用后未清理
     * 4. 静态集合持有对象引用
     * 
     * 【观察方法】
     * 1. JProfiler的Heap Walker
     * 2. 观察内存持续增长不回收
     * 3. 分析对象引用链
     * 
     * @return 测试结果
     */
    @PostMapping("/comprehensive/simulate-production-leak")
    public Map<String, Object> simulateProductionMemoryLeak() {
        log.info("============ 开始模拟生产环境内存泄漏 ============");
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        long beforeUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        
        // 场景1：静态集合持续增长（最常见的泄漏原因）
        for (int i = 0; i < 10000; i++) {
            UserSession session = new UserSession("user_" + i, System.currentTimeMillis());
            objectHolder.add(session); // 模拟缓存
        }
        
        // 场景2：ThreadLocal未清理
        ThreadLocal<List<byte[]>> threadLocal = new ThreadLocal<>();
        for (int i = 0; i < 10; i++) {
            executorService.submit(() -> {
                List<byte[]> list = new ArrayList<>();
                for (int j = 0; j < 100; j++) {
                    list.add(new byte[1024]); // 1KB
                }
                threadLocal.set(list);
                // 注意：这里故意不调用 threadLocal.remove()
            });
        }
        
        try {
            Thread.sleep(1000); // 等待任务完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long afterUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        
        result.put("status", "leak_simulated");
        result.put("泄漏前内存MB", beforeUsed / 1024 / 1024);
        result.put("泄漏后内存MB", afterUsed / 1024 / 1024);
        result.put("泄漏内存MB", (afterUsed - beforeUsed) / 1024 / 1024);
        result.put("泄漏场景", List.of(
            "场景1：静态集合持续添加对象不清理",
            "场景2：ThreadLocal使用后未调用remove()"
        ));
        result.put("排查方法", List.of(
            "1. 使用JProfiler的Heap Walker查看对象数量",
            "2. 找出数量异常增长的对象类型",
            "3. 使用'Biggest Objects'找出占用内存最多的对象",
            "4. 右键对象选择'Show Selection In Heap Walker'",
            "5. 查看'Incoming References'分析引用链",
            "6. 找到持有对象的根源（通常是静态变量或长生命周期对象）"
        ));
        result.put("说明", "这些对象会一直被持有，无法被GC回收，请在JProfiler中观察");
        
        log.info("内存泄漏模拟完成，泄漏了约{}MB", (afterUsed - beforeUsed) / 1024 / 1024);
        return result;
    }
    
    /**
     * 用户会话类 - 用于模拟内存泄漏
     */
    static class UserSession {
        private String userId;
        private long loginTime;
        private byte[] sessionData = new byte[1024]; // 1KB
        
        public UserSession(String userId, long loginTime) {
            this.userId = userId;
            this.loginTime = loginTime;
        }
    }
    
    /**
     * 综合实验2：模拟高并发场景
     * 
     * 【实验目的】
     * 观察高并发下各个内存区域的表现
     * 
     * 【观察重点】
     * 1. 堆内存：对象分配速度加快
     * 2. 虚拟机栈：线程数增加，栈内存占用增加
     * 3. GC频率：Minor GC频率增加
     * 
     * @return 测试结果
     */
    @PostMapping("/comprehensive/simulate-high-concurrency")
    public Map<String, Object> simulateHighConcurrency() {
        log.info("============ 开始模拟高并发场景 ============");
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        // 记录初始状态
        long beforeHeap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        int beforeThreads = ManagementFactory.getThreadMXBean().getThreadCount();
        
        Map<String, Long> beforeGC = new LinkedHashMap<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            beforeGC.put(gc.getName(), gc.getCollectionCount());
        }
        
        // 模拟100个并发请求
        int concurrentRequests = 100;
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestId = i;
            executorService.submit(() -> {
                try {
                    // 模拟业务处理：创建对象、数据库操作、计算等
                    List<byte[]> localData = new ArrayList<>();
                    for (int j = 0; j < 100; j++) {
                        localData.add(new byte[1024]); // 每个请求分配100KB
                    }
                    
                    // 模拟业务逻辑耗时
                    Thread.sleep(100);
                    
                    // 模拟返回结果
                    String response = "Request_" + requestId + "_Processed";
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // 记录结束状态
        long afterHeap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        int afterThreads = ManagementFactory.getThreadMXBean().getThreadCount();
        
        Map<String, Long> gcCounts = new LinkedHashMap<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long before = beforeGC.getOrDefault(gc.getName(), 0L);
            long after = gc.getCollectionCount();
            gcCounts.put(gc.getName(), after - before);
        }
        
        result.put("status", "success");
        result.put("并发请求数", concurrentRequests);
        result.put("总执行时间ms", executionTime);
        result.put("堆内存变化MB", (afterHeap - beforeHeap) / 1024 / 1024);
        result.put("线程数变化", afterThreads - beforeThreads);
        result.put("GC执行次数", gcCounts);
        result.put("观察要点", List.of(
            "1. 堆内存分配速度：高并发下对象创建速度快",
            "2. GC频率：观察Minor GC触发频率",
            "3. 线程数：每个请求对应一个线程（取决于线程池配置）",
            "4. 响应时间：GC可能影响响应时间",
            "5. 在JProfiler的Telemetries视图观察实时变化"
        ));
        
        log.info("高并发模拟完成，{}个请求耗时{}ms", concurrentRequests, executionTime);
        return result;
    }
    
    /**
     * 综合实验3：生成完整的内存分析报告
     * 
     * 【实验目的】
     * 综合展示所有内存区域的详细信息
     * 
     * @return 完整的内存分析报告
     */
    @GetMapping("/comprehensive/generate-report")
    public Map<String, Object> generateMemoryReport() {
        log.info("============ 生成内存分析报告 ============");
        
        Map<String, Object> report = new LinkedHashMap<>();
        
        // 1. 报告头
        report.put("报告生成时间", new java.util.Date().toString());
        report.put("JVM版本", System.getProperty("java.version"));
        report.put("JVM厂商", System.getProperty("java.vendor"));
        
        // 2. 堆内存详情
        report.put("堆内存", getHeapInfo());
        
        // 3. 元空间详情
        report.put("元空间", getMetaspaceInfo());
        
        // 4. 线程信息
        report.put("线程", getThreadInfo());
        
        // 5. GC信息
        report.put("GC统计", getGCInfo());
        
        // 6. 系统信息
        report.put("系统信息", getSystemInfo());
        
        // 7. 内存使用总结
        Map<String, Object> summary = new LinkedHashMap<>();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        
        summary.put("堆内存使用率", String.format("%.2f%%", heapUsed * 100.0 / heapMax));
        summary.put("非堆内存使用MB", nonHeapUsed / 1024 / 1024);
        summary.put("总线程数", ManagementFactory.getThreadMXBean().getThreadCount());
        summary.put("已加载类数", ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());
        
        report.put("总结", summary);
        
        // 8. 健康建议
        List<String> suggestions = new ArrayList<>();
        if (heapUsed * 100.0 / heapMax > 80) {
            suggestions.add("堆内存使用率超过80%，建议检查是否存在内存泄漏或增大堆内存");
        }
        if (ManagementFactory.getThreadMXBean().getThreadCount() > 500) {
            suggestions.add("线程数过多，可能存在线程泄漏或需要优化线程池配置");
        }
        
        long totalGCTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            totalGCTime += gc.getCollectionTime();
        }
        if (totalGCTime > 10000) {
            suggestions.add("GC总耗时超过10秒，建议优化GC参数或应用代码");
        }
        
        if (suggestions.isEmpty()) {
            suggestions.add("内存状态良好，各项指标正常");
        }
        
        report.put("健康建议", suggestions);
        
        log.info("内存分析报告生成完成");
        return report;
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 获取线程信息
     */
    private Map<String, Object> getThreadInfo() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> threadInfo = new LinkedHashMap<>();
        
        threadInfo.put("当前线程数", threadMXBean.getThreadCount());
        threadInfo.put("峰值线程数", threadMXBean.getPeakThreadCount());
        threadInfo.put("已启动线程总数", threadMXBean.getTotalStartedThreadCount());
        threadInfo.put("守护线程数", threadMXBean.getDaemonThreadCount());
        
        return threadInfo;
    }
    
    /**
     * 获取GC信息
     */
    private Map<String, Object> getGCInfo() {
        List<Map<String, Object>> gcList = new ArrayList<>();
        
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Object> gcInfo = new LinkedHashMap<>();
            gcInfo.put("名称", gc.getName());
            gcInfo.put("收集次数", gc.getCollectionCount());
            gcInfo.put("收集总耗时ms", gc.getCollectionTime());
            gcInfo.put("内存池", Arrays.toString(gc.getMemoryPoolNames()));
            gcList.add(gcInfo);
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("垃圾收集器", gcList);
        return result;
    }
    
    /**
     * 获取系统信息
     */
    private Map<String, Object> getSystemInfo() {
        Map<String, Object> systemInfo = new LinkedHashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        systemInfo.put("可用处理器数", runtime.availableProcessors());
        systemInfo.put("JVM最大内存MB", runtime.maxMemory() / 1024 / 1024);
        systemInfo.put("JVM已分配内存MB", runtime.totalMemory() / 1024 / 1024);
        systemInfo.put("JVM空闲内存MB", runtime.freeMemory() / 1024 / 1024);
        
        systemInfo.put("操作系统", System.getProperty("os.name"));
        systemInfo.put("系统架构", System.getProperty("os.arch"));
        systemInfo.put("Java版本", System.getProperty("java.version"));
        systemInfo.put("Java主目录", System.getProperty("java.home"));
        
        return systemInfo;
    }
}