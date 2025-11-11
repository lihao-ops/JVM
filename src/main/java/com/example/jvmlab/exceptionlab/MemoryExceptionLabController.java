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
 * 面向大厂面试的 JVM 内存异常实验中枢。
 */
@Slf4j
@RestController
@RequestMapping(path = "/memory-exception-lab", produces = MediaType.APPLICATION_JSON_VALUE)
public class MemoryExceptionLabController {

    private final MemoryExceptionLabService labService;

    public MemoryExceptionLabController(MemoryExceptionLabService labService) {
        this.labService = labService;
    }

    /**
     * 列出所有可用的异常实验，便于前端渲染导航树。
     */
    @GetMapping("/scenarios")
    public List<ScenarioMetadata> listScenarios() {
        return labService.listScenarios();
    }

    /**
     * 查询单个异常的详细指南。
     */
    @GetMapping("/scenarios/{id}")
    public ScenarioDetail getScenarioDetail(@PathVariable String id) {
        return labService.getScenarioDetail(id);
    }

    /**
     * 执行指定异常实验。默认以 Dry-Run 方式返回操作指引，只有当 dryRun=false 时才真正触发。
     */
    @PostMapping("/scenarios/{id}/execute")
    public ScenarioExecutionResult executeScenario(@PathVariable String id,
                                                   @RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun,
                                                   @RequestBody(required = false) Map<String, Object> bodyParams) throws Exception {
        Map<String, Object> params = new HashMap<>();
        if (bodyParams != null) {
            params.putAll(bodyParams);
        }
        params.put("dryRun", dryRun);
        log.info("Executing scenario {} with params {}", id, params);
        return labService.execute(id, params);
    }
}
