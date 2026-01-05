package com.example.jvmlab.chapter01;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 第一章：走近Java
 * <p>
 * 本章主要介绍Java技术体系、JVM发展史以及未来趋势。
 * 虽然没有太多代码实战，但理解这些概念对于后续章节至关重要。
 * <p>
 * 核心知识点：
 * 1. JDK vs JRE vs JVM
 * 2. 解释器 vs 编译器 (JIT) vs 混合模式
 * 3. HotSpot 虚拟机
 */
@RestController
@RequestMapping("/chapter01")
public class Chapter01Controller {

    /**
     * 模拟面试：JDK, JRE, JVM 的关系
     * <p>
     * 访问地址: http://localhost:8080/chapter01/jdk-jre-jvm
     */
    @GetMapping("/jdk-jre-jvm")
    public Map<String, String> explainJdkJreJvm() {
        Map<String, String> analogy = new HashMap<>();
        analogy.put("Java语言", "菜谱（源代码）");
        analogy.put("JVM (Java虚拟机)", "厨师（负责运行/做菜，跨平台的核心）");
        analogy.put("JRE (Java运行时环境)", "厨房（包含厨师JVM + 核心类库，能运行程序但不能开发）");
        analogy.put("JDK (Java开发工具包)", "餐饮研发中心（包含厨房JRE + 研发工具javac/jmap等）");
        analogy.put("结论", "生产环境排查问题通常需要JDK工具，仅运行则JRE足矣。");
        return analogy;
    }

    /**
     * 演示：HotSpot 的混合模式 (Mixed Mode)
     * <p>
     * HotSpot 虚拟机默认采用混合模式：
     * 1. 解释执行：启动快，执行慢。
     * 2. JIT编译执行：启动慢，执行快（热点代码编译为本地机器码）。
     * <p>
     * 访问地址: http://localhost:8080/chapter01/mixed-mode
     */
    @GetMapping("/mixed-mode")
    public Map<String, Object> explainMixedMode() {
        Map<String, Object> info = new HashMap<>();
        info.put("当前虚拟机", System.getProperty("java.vm.name"));
        info.put("虚拟机版本", System.getProperty("java.vm.version"));
        info.put("运行模式", System.getProperty("java.vm.info")); // 通常显示 mixed mode
        
        info.put("解释器 (Interpreter)", "逐行翻译执行，启动响应快，但效率低。");
        info.put("JIT编译器 (Just-In-Time)", "将热点代码编译成本地机器码，执行效率高。");
        info.put("混合模式 (Mixed Mode)", "HotSpot的默认策略：解释器负责启动和非热点代码，JIT负责热点代码优化。");
        
        return info;
    }

    /**
     * 简单的热点代码模拟
     * <p>
     * 循环执行多次，触发 JIT 编译（虽然在单次请求中很难直观看到JIT介入，但逻辑上如此）。
     * 可以配合 -XX:+PrintCompilation 参数观察。
     */
    @GetMapping("/hotspot-trigger")
    public String triggerHotSpot() {
        long startTime = System.nanoTime();
        
        // 模拟热点代码：空循环
        int sum = 0;
        for (int i = 0; i < 100000; i++) {
            sum += i;
        }
        
        long endTime = System.nanoTime();
        return "循环执行完成，耗时: " + (endTime - startTime) + " ns. 频繁执行此方法可触发JIT编译。";
    }
}
