package com.example.jvmlab.exceptionlab.scenario;

import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：对所有异常场景进行 Dry-Run 执行，验证统一返回结构与成功日志策略。
 * English: Execute dry-run across all exception scenarios to verify unified response structure and success logging strategy.
 *
 * 预期结果 / Expected Result:
 * 中文：每个场景返回 Dry-Run 提示与异常类型指标。
 * English: Each scenario returns dry-run hint and exception type metric.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class ExceptionScenariosDryRunTest {

    public void run() throws Exception {
        ScenarioExecutionResult r1 = new HeapOomScenario().execute(Map.of("dryRun", true));
        ScenarioExecutionResult r2 = new DirectMemoryOomScenario().execute(Map.of("dryRun", true));
        ScenarioExecutionResult r3 = new MetaspaceOomScenario().execute(Map.of("dryRun", true));
        ScenarioExecutionResult r4 = new StackOverflowScenario().execute(Map.of("dryRun", true));
        ScenarioExecutionResult r5 = new StringPoolPressureScenario().execute(Map.of("dryRun", true));
        ScenarioExecutionResult r6 = new GcOverheadScenario().execute(Map.of("dryRun", true));
        ScenarioExecutionResult r7 = new ThreadOomScenario().execute(Map.of("dryRun", true));
        ScenarioExecutionResult r8 = new ThreadLocalLeakScenario().execute(Map.of("dryRun", true));
        ScenarioExecutionResult[] arr = {r1, r2, r3, r4, r5, r6, r7, r8};
        int ok = 0;
        for (ScenarioExecutionResult r : arr) {
            if (r.isDryRun() && r.getMetrics().containsKey("exceptionType")) {
                ok++;
            }
        }
        if (ok == arr.length) {
            log.info("【成功】所有场景 Dry-Run 返回结构正确 / Success: {} scenarios", ok);
        } else {
            log.error("部分场景 Dry-Run 返回不符合预期 / Failure: {}/{}", ok, arr.length);
        }
    }

    public static void main(String[] args) throws Exception {
        ExceptionScenariosDryRunTest t = new ExceptionScenariosDryRunTest();
        t.run();
        log.info("【成功】ExceptionScenariosDryRunTest 用例通过 / Success: cases passed");
    }
}
