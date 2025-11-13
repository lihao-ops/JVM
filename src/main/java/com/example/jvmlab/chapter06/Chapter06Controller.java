package com.example.jvmlab.chapter06;

import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 类说明 / Class Description:
 * 中文：第6章控制器，读取并展示类文件结构信息，辅助理解 ClassFile 格式。
 * English: Chapter 06 controller that reads and displays class file structure to aid understanding of the ClassFile format.
 *
 * 使用场景 / Use Cases:
 * 中文：与 javap 输出对照学习字节码结构、常量池、访问标志等。
 * English: Learn bytecode structure, constant pool, access flags, etc., by comparing with javap output.
 *
 * 设计目的 / Design Purpose:
 * 中文：提供 REST 接口快速查看类结构，搭配日志解释字段含义。
 * English: Provide a REST endpoint to quickly inspect class structure with logs explaining field meanings.
 */
@Slf4j
@RestController
@RequestMapping("/chapter06")
public class Chapter06Controller {

    /**
     * 方法说明 / Method Description:
     * 中文：解析指定类的结构信息（名称、父类、接口、访问标志、常量池大小）。
     * English: Parse the structure of the specified class (name, super, interfaces, access flags, constant pool size).
     *
     * 参数 / Parameters:
     * @param className 中文：待解析的完整类名 / English: Fully qualified class name to parse
     *
     * 返回值 / Return:
     * 中文：类结构元数据 Map / English: Class structure metadata map
     *
     * 异常 / Exceptions:
     * 中文：IOException 当读取失败 / English: IOException when reading fails
     */
    @GetMapping("/class-structure")
    public Map<String, Object> parseClass(@RequestParam(defaultValue = "java.lang.String") String className) throws IOException {
        log.info("解析类结构 Parsing class structure: {}", className);
        ClassReader reader = new ClassReader(className);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("className", reader.getClassName());
        info.put("superName", reader.getSuperName());
        info.put("interfaces", Arrays.asList(reader.getInterfaces()));
        info.put("access", reader.getAccess());
        info.put("itemCount", reader.getItemCount());
        log.debug("ASM常量池容量 Constant pool size: {}", reader.getItemCount());
        return info;
    }
}
