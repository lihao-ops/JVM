package com.example.jvmlab.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 类说明 / Class Description:
 * 中文：全局异常处理器，统一格式化 REST 异常响应并打印双语日志，提升可观测性与一致性。
 * English: Global exception handler that formats REST error responses and prints bilingual logs for observability and consistency.
 *
 * 使用场景 / Use Cases:
 * 中文：拦截控制器层抛出的异常，返回结构化错误信息，避免前端处理差异。
 * English: Intercept exceptions thrown by controllers and return structured error info to avoid frontend handling differences.
 *
 * 设计目的 / Design Purpose:
 * 中文：面向大厂面试官视角，规范错误输出与日志，便于问题复盘与定位。
 * English: From a big-tech interviewer perspective, standardize error outputs and logs to aid postmortems and troubleshooting.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 方法说明 / Method Description:
     * 中文：处理未捕获的运行时异常，返回 500 错误响应并记录日志。
     * English: Handle uncaught runtime exceptions, return 500 error and log the details.
     *
     * 参数 / Parameters:
     * @param ex 中文：运行时异常 / English: Runtime exception
     *
     * 返回值 / Return:
     * 中文：结构化错误响应 Map / English: Structured error response map
     *
     * 异常 / Exceptions: 无
     */
    @ExceptionHandler(RuntimeException.class)
    public Map<String, Object> handleRuntime(RuntimeException ex) {
        log.error("全局异常 RuntimeException: {}", ex.getMessage(), ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "RuntimeException");
        body.put("message", ex.getMessage());
        return body;
    }

    /**
     * 方法说明 / Method Description:
     * 中文：兜底处理所有异常，避免泄漏栈信息给前端。
     * English: Fallback handler for all exceptions to avoid leaking stack details to clients.
     *
     * 参数 / Parameters:
     * @param ex 中文：通用异常 / English: Generic exception
     * 返回值 / Return: 中文：错误响应 Map / English: Error response map
     */
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleGeneric(Exception ex) {
        log.warn("全局异常 Exception: {}", ex.getMessage(), ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", ex.getClass().getSimpleName());
        body.put("message", ex.getMessage());
        return body;
    }
}
