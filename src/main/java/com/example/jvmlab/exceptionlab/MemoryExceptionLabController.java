package com.example.jvmlab.exceptionlab;

import com.example.jvmlab.exceptionlab.model.ScenarioDetail;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类说明 / Class Description:
 * 中文：JVM 内存异常实验的统一控制器，提供场景查询与执行接口。
 * English: Central controller for JVM memory exception experiments, exposing scenario listing and execution endpoints.
 *
 * 使用场景 / Use Cases:
 * 中文：用于在学习或面试场景下触发堆、栈、元空间、直接内存等异常实验，并返回可视化指标。
 * English: Trigger heap/stack/metaspace/direct memory experiments in learning or interview contexts and return metrics.
 *
 * 设计目的 / Design Purpose:
 * 中文：将所有异常场景统一到一个 REST 控制器，简化前端路由与调用方式，支持 Dry-Run 引导。
 * English: Unify all exception scenarios under a single REST controller, simplifying routing/invocation and supporting Dry-Run guidance.
 */
@Slf4j
@RestController
@RequestMapping(path = "/memory-exception-lab", produces = MediaType.APPLICATION_JSON_VALUE)
public class MemoryExceptionLabController {

    private final MemoryExceptionLabService labService;

    /**
     * 方法说明 / Method Description:
     * 中文：构造函数，注入实验服务，用于场景注册与调度。
     * English: Constructor injecting the lab service responsible for scenario registration and dispatching.
     *
     * 参数 / Parameters:
     * @param labService 中文：异常实验服务 / English: Service handling exception scenarios
     *
     * 返回值 / Return:
     * 中文：无 / English: None
     *
     * 异常 / Exceptions:
     * 中文：无 / English: None
     */
    public MemoryExceptionLabController(MemoryExceptionLabService labService) {
        this.labService = labService;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：列出所有可用异常实验的元信息，便于前端渲染导航与分类。
     * English: List metadata for all available exception scenarios to power UI navigation and categorization.
     *
     * 参数 / Parameters:
     * 无
     *
     * 返回值 / Return:
     * 中文：场景元信息列表 / English: List of scenario metadata
     *
     * 异常 / Exceptions:
     * 中文：无 / English: None
     */
    @GetMapping("/scenarios")
    public List<ScenarioMetadata> listScenarios() {
        // 中文：委托服务层聚合注册的场景并排序后返回
        // English: Delegate to service to aggregate and sort registered scenarios
        return labService.listScenarios();
    }

    /**
     * 方法说明 / Method Description:
     * 中文：根据场景 ID 查询详细指引信息，包括原理、复现步骤与工具建议。
     * English: Fetch scenario details by ID, including principles, reproduction steps and tooling tips.
     *
     * 参数 / Parameters:
     * @param id 中文：场景唯一标识 / English: Unique scenario identifier
     *
     * 返回值 / Return:
     * 中文：场景详情模型 / English: Scenario detail model
     *
     * 异常 / Exceptions:
     * 中文：若场景不存在，则服务层可能抛出 NoSuchElementException / English: May throw NoSuchElementException if not found
     */
    @GetMapping("/scenarios/{id}")
    public ScenarioDetail getScenarioDetail(@PathVariable String id) {
        // 中文：调用服务获取场景详情
        // English: Delegate to service to get scenario details
        return labService.getScenarioDetail(id);
    }

    /**
     * 方法说明 / Method Description:
     * 中文：执行指定异常实验；默认 Dry-Run 返回指引，dryRun=false 时正式触发实验行为。
     * English: Execute the specified experiment; defaults to Dry-Run guidance, triggers real behavior when dryRun=false.
     *
     * 参数 / Parameters:
     * @param id 中文：场景 ID / English: Scenario ID
     * @param dryRun 中文：是否仅演练不触发异常 / English: Whether to dry-run without triggering
     * @param bodyParams 中文：请求体参数，用于定制实验 / English: Request body parameters for customization
     *
     * 返回值 / Return:
     * 中文：统一的执行结果模型 / English: Unified execution result model
     *
     * 异常 / Exceptions:
     * 中文：实验执行可能抛出运行时异常或受检异常 / English: May throw runtime or checked exceptions during execution
     */
    @PostMapping("/scenarios/{id}/execute")
    public ScenarioExecutionResult executeScenario(@PathVariable String id,
                                                   @RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun,
                                                   @RequestBody(required = false) Map<String, Object> bodyParams) throws Exception {
        // 中文：合并查询参数与请求体，统一传递给服务层
        // English: Merge query and body parameters to pass to service
        Map<String, Object> params = new HashMap<>();
        if (bodyParams != null) {
            params.putAll(bodyParams);
        }
        // 中文：显式透传 Dry-Run 标记，用于模板方法控制分支
        // English: Pass Dry-Run flag explicitly to control template logic
        params.put("dryRun", dryRun);
        log.info("Executing scenario {} with params {}", id, params);
        return labService.execute(id, params);
    }
}
