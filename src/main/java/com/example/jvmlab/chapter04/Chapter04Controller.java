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
 * 类说明 / Class Description:
 * 中文：第4章控制器，演示虚拟机性能监控与故障处理工具的核心功能（参数、线程快照、监控）。
 * English: Chapter 04 controller demonstrating core tooling features (arguments, thread dump, monitoring) for JVM performance and troubleshooting.
 *
 * 使用场景 / Use Cases:
 * 中文：替代 jinfo、jstack 的部分能力，以 REST 输出用于可视化与教学。
 * English: Provide REST outputs for capabilities similar to jinfo/jstack for visualization and teaching.
 *
 * 设计目的 / Design Purpose:
 * 中文：统一接口返回结构，利于前端展示与后续章节的数据聚合。
 * English: Unify response structures to aid frontend display and data aggregation for later chapters.
 */
@Slf4j
@RestController
@RequestMapping("/chapter04")
public class Chapter04Controller {

    /**
     * 方法说明 / Method Description:
     * 中文：获取 JVM 启动参数与系统属性，模拟 jinfo 输出。
     * English: Get JVM input arguments and system properties, simulating jinfo output.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：启动参数 Map / English: Startup arguments map
     * 异常 / Exceptions: 无
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
     * 方法说明 / Method Description:
     * 中文：生成线程快照信息，模拟 jstack 功能。
     * English: Generate thread dump information, simulating jstack.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：线程信息 Map / English: Thread information map
     * 异常 / Exceptions: 无
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
