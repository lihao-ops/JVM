package com.example.jvmlab.chapter02.runtimedataareas.metaspace;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.stereotype.Service;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MetaspaceLabService {

    public static void main(String[] args) {
        // 请根据需要取消注释其中一个进行测试，并配合相应的 JVM 参数
        
        // 实验一：测试字符串常量池 (配合 -Xms20m -Xmx20m)
        // new MetaspaceLabService().stringPoolOOMTest();

        // 实验二：测试元空间溢出 (配合 -XX:MaxMetaspaceSize=20m)
        // new MetaspaceLabService().metaspaceOOMTest();

        // 实验三：测试直接内存溢出 (配合 -XX:MaxDirectMemorySize=100m)
        new MetaspaceLabService().directMemoryOOMTest();
    }

    /**
     * 实验一：撑爆字符串常量池 (String Table)
     * 目标：验证 JDK 7+ 字符串常量池位于【堆】中
     * 预期异常：java.lang.OutOfMemoryError: Java heap space
     */
    public void stringPoolOOMTest() {
        log.info("=== 🧪 实验一：字符串常量池 (String Table) 溢出实验 ===");
        log.info(">>> 准备疯狂 intern 字符串...");
        
        List<String> list = new ArrayList<>();
        long i = 0;
        
        try {
            while (true) {
                // String.valueOf(i++) 创建堆上新字符串
                // .intern() 尝试放入 StringTable
                // list.add 保持强引用，防止被 GC
                list.add(String.valueOf(i++).intern());
                
                if (i % 100000 == 0) {
                    log.info("已 intern {} 个字符串", i);
                }
            }
        } catch (OutOfMemoryError e) {
            log.error("🛑 捕获异常！类型: {}", e.getClass().getName());
            log.error("🛑 异常信息: {}", e.getMessage());
            log.error(">>> 结论：虽然是撑爆常量池，但报错是 Java heap space，证明 StringTable 在堆里！");
            throw e; // 抛出以便观察
        }
    }

    /**
     * 实验二：撑爆元空间 (Metaspace)
     * 目标：验证 Metaspace 存储的是【类结构 (Class Metadata)】
     * 手段：使用 CGLib 动态生成大量新类
     * 预期异常：java.lang.OutOfMemoryError: Metaspace
     */
    public void metaspaceOOMTest() {
        log.info("=== 🧪 实验二：元空间 (Metaspace) 溢出实验 ===");
        log.info(">>> 准备疯狂生成动态代理类 (Class)...");

        try {
            long count = 0;
            while (true) {
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(OOMObject.class);
                enhancer.setUseCache(false); // 💀 关键：关闭缓存，强制每次生成新的 Class
                enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> proxy.invokeSuper(obj, args));
                
                enhancer.create(); // 这一步会定义新的 Class 并加载到 Metaspace
                
                count++;
                if (count % 1000 == 0) {
                    log.info("已生成 {} 个动态代理类", count);
                }
            }
        } catch (OutOfMemoryError e) {
            log.error("🛑 捕获异常！类型: {}", e.getClass().getName());
            log.error("🛑 异常信息: {}", e.getMessage());
            log.error(">>> 结论：Metaspace 溢出，说明加载的类太多，超过了 MaxMetaspaceSize");
            throw e;
        }
    }

    /**
     * 实验三：直接内存 (Direct Memory) 溢出
     * 目标：验证直接内存溢出，包含 NIO 和 Unsafe 两种方式
     * 注意：直接内存不属于 Metaspace，但为了方便演示放在这里
     */
    public void directMemoryOOMTest() {
        log.info("=== 🧪 实验三：直接内存 (Direct Memory) 溢出实验 ===");
        
        // 方式 A：使用 NIO (受 -XX:MaxDirectMemorySize 限制)
        // 建议优先测试这个，比较安全
        testNioDirectMemory();

        // 方式 B：使用 Unsafe (不受限制，极度危险，可能导致死机)
        // testUnsafeDirectMemory();
    }

    private void testNioDirectMemory() {
        log.info(">>> 方式 A：NIO DirectByteBuffer (受 MaxDirectMemorySize 限制)");
        List<ByteBuffer> list = new ArrayList<>();
        int count = 0;
        try {
            while (true) {
                list.add(ByteBuffer.allocateDirect(1024 * 1024)); // 1MB
                count++;
                if (count % 10 == 0) log.info("已分配 {} MB NIO直接内存", count);
            }
        } catch (OutOfMemoryError e) {
            log.error("🛑 捕获异常！类型: {}", e.getClass().getName());
            log.error("🛑 异常信息: {}", e.getMessage());
            log.error(">>> 结论：Direct buffer memory 溢出");
            throw e;
        }
    }

    private void testUnsafeDirectMemory() {
        log.info(">>> 方式 B：Unsafe (不受限制，危险！)");
        try {
            Field field = Unsafe.class.getDeclaredFields()[0];
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            
            long count = 0;
            while (true) {
                unsafe.allocateMemory(1024 * 1024);
                count++;
                if (count % 100 == 0) log.info("已通过 Unsafe 分配 {} MB", count);
            }
        } catch (Exception e) {
            log.error("Unsafe 操作失败", e);
        } catch (OutOfMemoryError e) {
            log.error("🛑 捕获异常！{}", e.getMessage());
            throw e;
        }
    }

    // 仅作为一个基类使用
    static class OOMObject {}
}
