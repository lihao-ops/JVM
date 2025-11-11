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
 * 负责装配并调度所有异常实验策略。
 */
@Service
public class MemoryExceptionLabService {

    private final Map<String, MemoryExceptionScenario> scenarioRegistry = new ConcurrentHashMap<>();

    public MemoryExceptionLabService(List<MemoryExceptionScenario> scenarios) {
        scenarios.forEach(scenario -> scenarioRegistry.put(scenario.getId(), scenario));
    }

    public List<ScenarioMetadata> listScenarios() {
        return scenarioRegistry.values().stream()
                .sorted(Comparator.comparing(MemoryExceptionScenario::getId))
                .map(scenario -> new ScenarioMetadata(
                        scenario.getId(),
                        scenario.getDisplayName(),
                        scenario.getExceptionType(),
                        scenario.getMemoryArea()))
                .toList();
    }

    public ScenarioDetail getScenarioDetail(String id) {
        MemoryExceptionScenario scenario = getScenario(id);
        return new ScenarioDetail(
                scenario.getId(),
                scenario.getDisplayName(),
                scenario.getExceptionType(),
                scenario.getMemoryArea(),
                scenario.getGuide());
    }

    public ScenarioExecutionResult execute(String id, Map<String, Object> params) throws Exception {
        MemoryExceptionScenario scenario = getScenario(id);
        return scenario.execute(params);
    }

    private MemoryExceptionScenario getScenario(String id) {
        MemoryExceptionScenario scenario = scenarioRegistry.get(id);
        if (scenario == null) {
            throw new NoSuchElementException("Scenario not found: " + id);
        }
        return scenario;
    }
}
