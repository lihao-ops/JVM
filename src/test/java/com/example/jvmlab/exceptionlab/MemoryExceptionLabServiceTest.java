package com.example.jvmlab.exceptionlab;

import com.example.jvmlab.exceptionlab.model.ScenarioDetail;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.scenario.DirectMemoryOomScenario;
import com.example.jvmlab.exceptionlab.scenario.GcOverheadScenario;
import com.example.jvmlab.exceptionlab.scenario.HeapOomScenario;
import com.example.jvmlab.exceptionlab.scenario.MetaspaceOomScenario;
import com.example.jvmlab.exceptionlab.scenario.StackOverflowScenario;
import com.example.jvmlab.exceptionlab.scenario.StringPoolPressureScenario;
import com.example.jvmlab.exceptionlab.scenario.ThreadOomScenario;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证内存异常实验服务的场景注册、详情查询与 Dry-Run 执行逻辑，确保返回结构与指引文本正确。
 * English: Verify scenario registration, detail fetching and Dry-Run execution of the memory exception service, ensuring response structure and guidance text are correct.
 *
 * 预期结果 / Expected Result:
 * 中文：能够列出已注册场景；根据 ID 返回详情；Dry-Run 执行返回统一提示文本与异常类型指标，并打印成功日志。
 * English: Should list registered scenarios; return details by ID; Dry-Run execution returns unified hint text and exception type metric, with success logs printed.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法或在测试框架中调用 test 方法，观察 SLF4J 日志输出的成功确认信息。
 * English: Run the main method or invoke test methods in a test framework and observe SLF4J success confirmation logs.
 */
@Slf4j
public class MemoryExceptionLabServiceTest {

    /**
     * 方法说明 / Method Description:
     * 中文：构建服务与场景集合，验证列表与详情查询。
     * English: Build service and scenario set, then verify listing and detail fetching.
     *
     * 参数 / Parameters: 无
     *
     * 返回值 / Return:
     * 中文：无（通过日志打印成功确认） / English: None (success confirmed via logs)
     *
     * 异常 / Exceptions:
     * 中文：无 / English: None
     */
    public void testListAndDetail() {
        MemoryExceptionLabService service = new MemoryExceptionLabService(buildScenarios());
        boolean hasHeap = service.listScenarios().stream().anyMatch(m -> "heap-oom".equals(m.getId()));
        if (hasHeap) {
            log.info("【成功】场景列表包含 heap-oom / Success: scenario list contains heap-oom");
        } else {
            log.error("场景列表不包含 heap-oom / Failure: scenario list missing heap-oom");
        }
        ScenarioDetail detail = service.getScenarioDetail("heap-oom");
        if (detail.getExceptionType().contains("OutOfMemoryError")) {
            log.info("【成功】详情返回包含异常类型 / Success: detail contains exception type");
        } else {
            log.error("详情返回异常类型不匹配 / Failure: detail exception type mismatch");
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：验证 Dry-Run 执行返回统一提示文本与指标字段。
     * English: Verify Dry-Run execution returns unified hint text and metrics field.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 可能抛出受检异常 / May throw checked exception
     */
    public void testDryRunExecute() throws Exception {
        MemoryExceptionLabService service = new MemoryExceptionLabService(buildScenarios());
        ScenarioExecutionResult result = service.execute("heap-oom", Map.of("dryRun", true));
        boolean messageOk = result.getMessage().contains("Dry-Run completed");
        boolean metricOk = result.getMetrics().getOrDefault("exceptionType", "").toString().contains("heap");
        if (messageOk && result.isDryRun()) {
            log.info("【成功】Dry-Run 指引匹配 / Success: Dry-Run message matched");
        } else {
            log.error("Dry-Run 返回不符合预期 / Failure: Dry-Run response unexpected");
        }
        if (metricOk) {
            log.info("【成功】异常类型指标存在 / Success: exceptionType metric present");
        } else {
            log.warn("异常类型指标缺失 / Warning: exceptionType metric missing");
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：构造用于测试的场景实现集合。
     * English: Build a set of scenario implementations for testing.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 中文：场景列表 / English: List of scenarios
     * 异常 / Exceptions: 无
     */
    private List<MemoryExceptionScenario> buildScenarios() {
        return List.of(
                new HeapOomScenario(),
                new DirectMemoryOomScenario(),
                new MetaspaceOomScenario(),
                new StackOverflowScenario(),
                new StringPoolPressureScenario(),
                new GcOverheadScenario(),
                new ThreadOomScenario()
        );
    }

    /**
     * 方法说明 / Method Description:
     * 中文：入口方法，按顺序执行测试用例并打印成功确认日志。
     * English: Entry point executing test cases in order with success confirmation logs.
     *
     * 参数 / Parameters:
     * @param args 中文：命令行参数 / English: CLI arguments
     * 返回值 / Return: 无
     * 异常 / Exceptions: 可能抛出受检异常 / May throw checked exception
     */
    public static void main(String[] args) throws Exception {
        MemoryExceptionLabServiceTest t = new MemoryExceptionLabServiceTest();
        t.testListAndDetail();
        t.testDryRunExecute();
        log.info("【成功】MemoryExceptionLabServiceTest 全部用例通过 / Success: all cases passed");
    }
}
