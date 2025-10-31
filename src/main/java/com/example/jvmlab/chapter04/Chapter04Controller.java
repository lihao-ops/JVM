package com.example.jvmlab.chapter04;

import com.example.jvmlab.common.JvmMemoryMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第4章：虚拟机性能监控与故障处理工具示例控制器。
 * <p>
 * 实现思路：
 * 1. 使用JDK自带的MBeans模拟jstack、jinfo等工具的核心功能，提供可视化的REST接口。
 * 2. 结合中文+英文日志说明如何定位线程死锁、查看JVM参数，贴近实际运维场景。
 * 3. 为后续章节的监控页面提供数据接口，形成完整的诊断链路。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/chapter04")
public class Chapter04Controller {

    /**
     * 获取JVM启动参数信息，模拟jinfo输出。
     *
     * @return 启动参数Map。
     */
    @GetMapping("/jvm-arguments")
    public Map<String, Object> getJvmArguments() {
        log.info("查询JVM启动参数 Fetching JVM startup arguments");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("inputArguments", ManagementFactory.getRuntimeMXBean().getInputArguments());
        info.put("systemProperties", System.getProperties());
        return info;
    }

    /**
     * 获取线程快照信息，模拟jstack。
     *
     * @return 线程信息。
     */
    @GetMapping("/thread-dump")
    public Map<String, Object> getThreadDump() {
        log.info("生成线程快照 Generating thread dump");
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("threadCount", threadMXBean.getThreadCount());
        result.put("daemonCount", threadMXBean.getDaemonThreadCount());
        result.put("threads", threadInfos);
        return result;
    }

    /**
     * 获取内存与GC监控信息，为性能分析提供入口。
     *
     * @return 监控数据。
     */
    @GetMapping("/monitor")
    public Map<String, Object> monitor() {
        log.info("汇总JVM监控信息 Aggregating JVM metrics");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("memory", JvmMemoryMonitor.getMemoryInfoMap());
        data.put("gc", JvmMemoryMonitor.getGCStats());
        data.put("system", JvmMemoryMonitor.getSystemInfo());
        return data;
    }
}
