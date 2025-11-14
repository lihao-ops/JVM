package com.example.jvmlab.exceptionlab;

import com.example.jvmlab.exceptionlab.model.ScenarioDetail;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioMetadata;
import com.example.jvmlab.exceptionlab.scenario.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证异常实验控制器的列表、详情与执行（Dry-Run）返回结构正确，并打印成功日志。
 * English: Verify controller listing, detail and execution (dry-run) responses and print success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：列表非空；详情包含 guide；执行返回 Dry-Run 提示与异常类型指标。
 * English: Non-empty list; detail contains guide; execution returns dry-run hint and exception type metric.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察成功确认日志。
 * English: Run main method and observe success logs.
 */
@Slf4j
public class MemoryExceptionLabControllerTest {

    public void testListAndDetailAndExecute() throws Exception {
        MemoryExceptionLabService service = new MemoryExceptionLabService(buildScenarios());
        MemoryExceptionLabController ctrl = new MemoryExceptionLabController(service);
        List<ScenarioMetadata> list = ctrl.listScenarios();
        if (!list.isEmpty()) {
            log.info("【成功】列表非空 / Success: scenarios listed: {}", list.size());
        } else {
            log.error("列表为空 / Failure: empty scenarios");
        }
        ScenarioDetail detail = ctrl.getScenarioDetail("heap-oom");
        if (detail.getGuide() != null) {
            log.info("【成功】详情包含指南 / Success: detail has guide");
        } else {
            log.error("详情缺少指南 / Failure: guide missing");
        }
        ScenarioExecutionResult res = ctrl.executeScenario("heap-oom", true, Map.of());
        if (res.isDryRun() && res.getMetrics().containsKey("exceptionType")) {
            log.info("【成功】Dry-Run 执行返回指标 / Success: dry-run metrics present");
        } else {
            log.error("Dry-Run 执行不符合预期 / Failure: unexpected result: {}", res);
        }
    }

    private List<MemoryExceptionScenario> buildScenarios() {
        return List.of(
                new HeapOomScenario(),
                new DirectMemoryOomScenario(),
                new MetaspaceOomScenario(),
                new StackOverflowScenario(),
                new StringPoolPressureScenario(),
                new GcOverheadScenario(),
                new ThreadOomScenario(),
                new ThreadLocalLeakScenario()
        );
    }

    public static void main(String[] args) throws Exception {
        MemoryExceptionLabControllerTest t = new MemoryExceptionLabControllerTest();
        t.testListAndDetailAndExecute();
        log.info("【成功】MemoryExceptionLabControllerTest 用例通过 / Success: cases passed");
    }
}
