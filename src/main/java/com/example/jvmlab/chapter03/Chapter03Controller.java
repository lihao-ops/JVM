package com.example.jvmlab.chapter03;

import com.example.jvmlab.common.JvmMemoryMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * 第3章：垃圾收集器与内存分配策略实验控制器。
 * <p>
 * 实现思路：
 * 1. 为书中每个核心概念设计实验接口，如引用类型、对象晋升、空间分配担保等。
 * 2. 使用详细日志输出中文+英文说明，引导学习者理解GC日志与行为之间的关联。
 * 3. 支持手动触发GC和查询统计数据，便于面试现场演示和复盘。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/chapter03")
public class Chapter03Controller {

    /**
     * 通过相互引用模拟引用计数算法的缺陷，验证可达性分析的优势。
     *
     * @return 测试完成提示。
     */
    @GetMapping("/circular-reference")
    public String testCircularReference() {
        log.info("开始循环引用实验 Testing circular reference scenario");
        JvmMemoryMonitor.printMemoryInfo("Before circular reference 前置状态");
        ObjectA objA = new ObjectA();
        ObjectB objB = new ObjectB();
        objA.ref = objB;
        objB.ref = objA;
        log.info("已创建互相引用对象 Objects created with mutual references");
        objA = null;
        objB = null;
        log.info("释放外部引用 External references cleared");
        System.gc();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        JvmMemoryMonitor.printMemoryInfo("After circular reference experiment 实验结束");
        return "Circular reference test completed. Check GC logs.";
    }

    /**
     * 验证四种引用类型的回收特性。
     *
     * @return 各引用类型的存活状态。
     */
    @GetMapping("/reference-types")
    public Map<String, String> testReferenceTypes() {
        log.info("开始测试引用类型 Testing reference types");
        Map<String, String> result = new LinkedHashMap<>();
        byte[] strongRef = new byte[1024 * 1024];
        result.put("strongRef", "Created创建");
        System.gc();
        result.put("strongRef-afterGC", strongRef != null ? "Alive存活" : "Collected回收");
        SoftReference<byte[]> softRef = new SoftReference<>(new byte[1024 * 1024]);
        result.put("softRef", "Created创建");
        System.gc();
        result.put("softRef-afterGC", softRef.get() != null ? "Alive存活" : "Collected回收");
        WeakReference<byte[]> weakRef = new WeakReference<>(new byte[1024 * 1024]);
        result.put("weakRef", "Created创建");
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        result.put("weakRef-afterGC", weakRef.get() != null ? "Alive存活" : "Collected回收");
        ReferenceQueue<byte[]> queue = new ReferenceQueue<>();
        PhantomReference<byte[]> phantomRef = new PhantomReference<>(new byte[1024 * 1024], queue);
        result.put("phantomRef", "Created创建");
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        result.put("phantomRef-queue", queue.poll() != null ? "Enqueued已入队" : "Waiting等待");
        log.info("引用类型实验结果 Reference type result: {}", result);
        return result;
    }

    /**
     * 演示finalize自我拯救的机制与局限。
     *
     * @return 两次回收的结果。
     * @throws InterruptedException 中断异常。
     */
    @GetMapping("/finalize-rescue")
    public String testFinalizeRescue() throws InterruptedException {
        log.info("开始finalize自救实验 Testing finalize self-rescue");
        FinalizableObject.instance = new FinalizableObject("First");
        FinalizableObject.instance = null;
        System.gc();
        Thread.sleep(500);
        String firstResult = FinalizableObject.instance != null ? "First rescue success成功" : "First rescue failed失败";
        log.info(firstResult);
        FinalizableObject.instance = null;
        System.gc();
        Thread.sleep(500);
        String secondResult = FinalizableObject.instance != null ? "Second rescue success成功" : "Second rescue failed失败";
        log.info(secondResult);
        return firstResult + " | " + secondResult;
    }

    /**
     * 演示Eden区的对象分配行为。
     *
     * @return 提示信息。
     */
    @GetMapping("/allocation-eden")
    public String testAllocationEden() {
        log.info("开始Eden分配实验 Testing Eden allocation");
        JvmMemoryMonitor.printMemoryInfo("Before Eden allocation");
        final int _1MB = 1024 * 1024;
        List<byte[]> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            list.add(new byte[2 * _1MB]);
            log.info("分配第{}个2MB对象 Allocated 2MB object #{}", i + 1, i + 1);
            JvmMemoryMonitor.printMemoryInfo("After allocation" + (i + 1));
        }
        return "Allocated 4 objects in Eden. Check GC logs.";
    }

    /**
     * 演示大对象直接进入老年代。
     *
     * @return 提示信息。
     */
    @GetMapping("/allocation-large-object")
    public String testLargeObjectAllocation() {
        log.info("开始大对象分配实验 Testing large object allocation");
        final int _1MB = 1024 * 1024;
        byte[] largeObject = new byte[4 * _1MB];
        log.debug("大对象引用地址 Large object reference: {}", System.identityHashCode(largeObject));
        JvmMemoryMonitor.printMemoryInfo("After large object allocation");
        return "Large object allocated. Observe Old Gen usage.";
    }

    /**
     * 演示对象年龄晋升到老年代的过程。
     *
     * @return 提示信息。
     */
    @GetMapping("/allocation-tenuring")
    public String testTenuring() {
        log.info("开始对象晋升实验 Testing object tenuring");
        final int _1MB = 1024 * 1024;
        List<byte[]> survivor = new ArrayList<>();
        survivor.add(new byte[_1MB]);
        for (int i = 0; i < 3; i++) {
            byte[] temp = new byte[4 * _1MB];
            log.info("触发Minor GC Attempting to trigger Minor GC #{}", i + 1);
            JvmMemoryMonitor.printMemoryInfo("Minor GC round" + (i + 1));
        }
        return "Tenuring experiment executed. Check PrintTenuringDistribution.";
    }

    /**
     * 演示动态年龄判定机制。
     *
     * @return 提示信息。
     */
    @GetMapping("/allocation-dynamic-age")
    public String testDynamicAge() {
        log.info("开始动态年龄判定实验 Testing dynamic age determination");
        final int _1MB = 1024 * 1024;
        List<byte[]> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            list.add(new byte[_1MB / 4]);
        }
        byte[] trigger = new byte[4 * _1MB];
        log.debug("触发对象引用 Trigger object hash: {}", System.identityHashCode(trigger));
        JvmMemoryMonitor.printMemoryInfo("After dynamic age test");
        return "Dynamic age threshold test completed.";
    }

    /**
     * 验证空间分配担保机制。
     *
     * @return 提示信息。
     */
    @GetMapping("/allocation-guarantee")
    public String testAllocationGuarantee() {
        log.info("开始空间分配担保实验 Testing allocation guarantee");
        final int _1MB = 1024 * 1024;
        List<byte[]> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            list.add(new byte[2 * _1MB]);
        }
        System.gc();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        byte[] eden1 = new byte[2 * _1MB];
        byte[] eden2 = new byte[2 * _1MB];
        byte[] eden3 = new byte[2 * _1MB];
        byte[] eden4 = new byte[2 * _1MB];
        log.debug("Eden对象引用 Eden refs: {} {} {} {}", System.identityHashCode(eden1), System.identityHashCode(eden2), System.identityHashCode(eden3), System.identityHashCode(eden4));
        JvmMemoryMonitor.printMemoryInfo("After allocation guarantee test");
        return "Space allocation guarantee tested. Review GC logs.";
    }

    /**
     * 手动触发GC以便观察不同场景。
     *
     * @param full 是否期望Full GC。
     * @return 提示信息。
     */
    @PostMapping("/trigger-gc")
    public String triggerGC(@RequestParam(defaultValue = "false") boolean full) {
        log.info("手动触发GC Manual GC trigger, full={} ", full);
        JvmMemoryMonitor.printMemoryInfo("Before manual GC");
        if (full) {
            System.gc();
        } else {
            byte[] temp = new byte[1024 * 1024];
            log.debug("Minor GC触发对象 Temporary buffer hash: {}", System.identityHashCode(temp));
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        JvmMemoryMonitor.printMemoryInfo("After manual GC");
        return "GC triggered";
    }

    /**
     * 查询GC统计信息。
     *
     * @return GC信息Map。
     */
    @GetMapping("/gc-stats")
    public Map<String, Map<String, Object>> getGCStats() {
        log.info("查询GC统计信息 Fetching GC stats");
        return JvmMemoryMonitor.getGCStats();
    }

    /**
     * 循环引用示例类A。
     */
    @SuppressWarnings("removal")
    static class ObjectA {
        Object ref;
        byte[] data = new byte[1024 * 1024];
        @Override
        protected void finalize() {
            log.info("ObjectA finalize invoked 对象A执行finalize");
        }
    }

    /**
     * 循环引用示例类B。
     */
    @SuppressWarnings("removal")
    static class ObjectB {
        Object ref;
        byte[] data = new byte[1024 * 1024];
        @Override
        protected void finalize() {
            log.info("ObjectB finalize invoked 对象B执行finalize");
        }
    }

    /**
     * 可自救对象示例。
     */
    @SuppressWarnings("removal")
    static class FinalizableObject {
        static FinalizableObject instance;
        private final String name;
        FinalizableObject(String name) {
            this.name = name;
        }
        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            log.info("finalize执行 Finalize called for {}", name);
            FinalizableObject.instance = this;
        }
    }
}
