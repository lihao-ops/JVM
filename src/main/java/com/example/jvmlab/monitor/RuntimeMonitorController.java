package com.example.jvmlab.monitor;

import com.example.jvmlab.common.JvmMemoryMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 类说明 / Class Description:
 * 中文：运行时监控控制器，聚合内存、GC、系统与风险评估指标，提供统一概览接口。
 * English: Runtime monitoring controller aggregating memory, GC, system and leak risk metrics, providing a unified overview endpoint.
 *
 * 使用场景 / Use Cases:
 * 中文：为前端监控面板或 Grafana 数据源提供结构化监控数据。
 * English: Provide structured monitoring data for frontend dashboards or Grafana data sources.
 *
 * 设计目的 / Design Purpose:
 * 中文：统一监控入口，便于扩展与对接各章节实验结果。
 * English: Centralize monitoring endpoints for extension and chapter experiment integration.
 */
@Slf4j
@RestController
@RequestMapping("/monitor")
public class RuntimeMonitorController {

    /**
     * 方法说明 / Method Description:
     * 中文：聚合内存、GC、系统与泄漏风险信息，返回监控总览。
     * English: Aggregate memory, GC, system and leak risk information and return an overview.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：监控数据 Map / English: Monitoring data map
     * 异常 / Exceptions: 无
     */
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        // 中文：记录访问并构造分组监控数据
        // English: Log access and build grouped monitoring data
        log.info("访问监控总览 Monitor overview requested");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("memory", JvmMemoryMonitor.getMemoryInfoMap());
        result.put("gc", JvmMemoryMonitor.getGCStats());
        result.put("system", JvmMemoryMonitor.getSystemInfo());
        result.put("leakRisk", JvmMemoryMonitor.detectMemoryLeakRisk());
        return result;
    }
}
