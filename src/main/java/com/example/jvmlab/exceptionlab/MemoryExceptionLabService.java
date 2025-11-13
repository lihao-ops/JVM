package com.example.jvmlab.exceptionlab;

import com.example.jvmlab.exceptionlab.model.ScenarioDetail;
import com.example.jvmlab.exceptionlab.model.ScenarioExecutionResult;
import com.example.jvmlab.exceptionlab.model.ScenarioMetadata;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类说明 / Class Description:
 * 中文：异常实验服务，负责场景的注册、查询与执行调度。
 * English: Service orchestrating registration, lookup and execution of exception scenarios.
 *
 * 使用场景 / Use Cases:
 * 中文：供控制器调用，返回场景列表、详情，并根据 ID 执行具体策略。
 * English: Called by controllers to list scenarios, fetch details, and execute strategies by ID.
 *
 * 设计目的 / Design Purpose:
 * 中文：通过策略模式统一管理不同异常场景，保证扩展性与线程安全。
 * English: Use strategy pattern to manage different scenarios uniformly with scalability and thread safety.
 */
@Service
public class MemoryExceptionLabService {

    private final Map<String, MemoryExceptionScenario> scenarioRegistry = new ConcurrentHashMap<>();

    /**
     * 方法说明 / Method Description:
     * 中文：构造函数，接收 Spring 注入的场景列表并注册到并发字典。
     * English: Constructor receiving Spring-injected scenario list and registering into a concurrent map.
     *
     * 参数 / Parameters:
     * @param scenarios 中文：所有实现了异常场景接口的 Bean 列表 / English: List of beans implementing the scenario interface
     *
     * 返回值 / Return:
     * 中文：无 / English: None
     *
     * 异常 / Exceptions:
     * 中文：无 / English: None
     */
    public MemoryExceptionLabService(List<MemoryExceptionScenario> scenarios) {
        // 中文：以场景 ID 作为键注册，保证查找高效
        // English: Register by scenario ID for efficient lookup
        scenarios.forEach(scenario -> scenarioRegistry.put(scenario.getId(), scenario));
    }

    /**
     * 方法说明 / Method Description:
     * 中文：返回场景元数据列表（排序后），用于前端展示。
     * English: Return a sorted list of scenario metadata for UI rendering.
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
    public List<ScenarioMetadata> listScenarios() {
        // 中文：按场景 ID 排序后映射为元信息
        // English: Sort by ID then map to metadata
        return scenarioRegistry.values().stream()
                .sorted(Comparator.comparing(MemoryExceptionScenario::getId))
                .map(scenario -> new ScenarioMetadata(
                        scenario.getId(),
                        scenario.getDisplayName(),
                        scenario.getExceptionType(),
                        scenario.getMemoryArea()))
                .toList();
    }

    /**
     * 方法说明 / Method Description:
     * 中文：根据场景 ID 返回详细信息与指导手册。
     * English: Return detailed info and guide for scenario by ID.
     *
     * 参数 / Parameters:
     * @param id 中文：场景 ID / English: Scenario ID
     *
     * 返回值 / Return:
     * 中文：场景详情对象 / English: Scenario detail object
     *
     * 异常 / Exceptions:
     * 中文：若场景不存在将抛出 NoSuchElementException / English: Throws NoSuchElementException when not found
     */
    public ScenarioDetail getScenarioDetail(String id) {
        // 中文：查找目标场景并构造详情对象
        // English: Lookup scenario and build detail object
        MemoryExceptionScenario scenario = getScenario(id);
        return new ScenarioDetail(
                scenario.getId(),
                scenario.getDisplayName(),
                scenario.getExceptionType(),
                scenario.getMemoryArea(),
                scenario.getGuide());
    }

    /**
     * 方法说明 / Method Description:
     * 中文：执行指定场景的实验逻辑，支持 Dry-Run 与参数化行为。
     * English: Execute the specified scenario; supports Dry-Run and parameterized behavior.
     *
     * 参数 / Parameters:
     * @param id 中文：场景 ID / English: Scenario ID
     * @param params 中文：执行参数（dryRun、大小、延迟等） / English: Execution params (dryRun, sizes, delays, etc.)
     *
     * 返回值 / Return:
     * 中文：统一的执行结果对象 / English: Unified execution result
     *
     * 异常 / Exceptions:
     * 中文：场景内部可能抛出业务异常或 OOM 等错误 / English: May throw business exceptions or OOM errors inside scenarios
     */
    public ScenarioExecutionResult execute(String id, Map<String, Object> params) throws Exception {
        // 中文：按 ID 查找场景并委托执行
        // English: Lookup by ID and delegate execution
        MemoryExceptionScenario scenario = getScenario(id);
        return scenario.execute(params);
    }

    /**
     * 方法说明 / Method Description:
     * 中文：内部查找场景的工具方法，未找到时抛出异常。
     * English: Internal helper to fetch scenario or throw when missing.
     *
     * 参数 / Parameters:
     * @param id 中文：场景 ID / English: Scenario ID
     *
     * 返回值 / Return:
     * 中文：对应的场景实例 / English: The scenario instance
     *
     * 异常 / Exceptions:
     * 中文：NoSuchElementException 当场景未注册 / English: NoSuchElementException when not registered
     */
    private MemoryExceptionScenario getScenario(String id) {
        // 中文：并发字典查找场景，缺失则明确抛出异常
        // English: Lookup from concurrent map; throw explicit exception if missing
        MemoryExceptionScenario scenario = scenarioRegistry.get(id);
        if (scenario == null) {
            throw new NoSuchElementException("Scenario not found: " + id);
        }
        return scenario;
    }
}
