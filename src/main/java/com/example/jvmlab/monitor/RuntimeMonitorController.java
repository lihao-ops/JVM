package com.example.jvmlab.monitor;

import com.example.jvmlab.common.JvmMemoryMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 监控总控接口，聚合多个章节的监控指标。
 * <p>
 * 实现思路：
 * 1. 提供统一的监控入口，便于在Grafana或自定义前端展示内存、GC、线程等数据。
 * 2. 支持扩展，将章节中的实验状态与监控数据组合输出，形成学习仪表盘。
 * 3. 通过日志记录访问来源，帮助分析实验频率和调试行为。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/monitor")
public class RuntimeMonitorController {

    /**
     * 聚合内存、GC、系统信息。
     *
     * @return 监控数据。
     */
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        log.info("访问监控总览 Monitor overview requested");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("memory", JvmMemoryMonitor.getMemoryInfoMap());
        result.put("gc", JvmMemoryMonitor.getGCStats());
        result.put("system", JvmMemoryMonitor.getSystemInfo());
        result.put("leakRisk", JvmMemoryMonitor.detectMemoryLeakRisk());
        return result;
    }
}
