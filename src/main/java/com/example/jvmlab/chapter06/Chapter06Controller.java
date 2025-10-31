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
 * 第6章：类文件结构学习控制器。
 * <p>
 * 实现思路：
 * 1. 借助ASM读取类文件字节码，解析魔数、常量池、访问标志等核心信息。
 * 2. 通过REST接口传入类名即可查看结构，方便与javap命令输出对比学习。
 * 3. 结合日志说明每个字段含义，强化对ClassFile格式的理解。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/chapter06")
public class Chapter06Controller {

    /**
     * 解析指定类的基本结构信息。
     *
     * @param className 完整类名。
     * @return 类元数据。
     * @throws IOException 读取异常。
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
