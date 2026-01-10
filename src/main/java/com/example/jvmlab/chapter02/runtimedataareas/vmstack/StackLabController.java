package com.example.jvmlab.chapter02.runtimedataareas.vmstack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StackLabController {

    private final StackLabService stackLabService;

    /**
     * 实验 1：Debug 观察栈帧入栈出栈
     * 访问示例：http://localhost:8080/jvm/stack/debug?limit=5
     */
    @GetMapping("/jvm/stack/debug")
    public String debugStack(@RequestParam(defaultValue = "5") int limit) {
        log.info("开始栈帧 Debug 实验，目标深度: {}", limit);
        stackLabService.resetCounter();
        stackLabService.recursiveDive(limit);
        return "实验结束，请查看控制台日志或 IDE 调试器";
    }

    /**
     * 实验 2：触发 StackOverflowError
     * 访问示例：http://localhost:8080/jvm/stack/overflow
     */
    @GetMapping("/jvm/stack/overflow")
    public String triggerOverflow() {
        log.info("开始 StackOverflowError 触发实验...");
        stackLabService.resetCounter();
        try {
            stackLabService.infiniteDive();
        } catch (StackOverflowError e) {
            int depth = stackLabService.getCurrentDepth();
            log.error("成功捕获栈溢出异常！StackOverflowError 发生，最终深度: {}", depth);
            return "成功捕获栈溢出异常！最终深度: " + depth;
        }
        return "未发生异常";
    }
}
