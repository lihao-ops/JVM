package com.example.jvmlab.chapter10;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 类说明 / Class Description:
 * 中文：第10章控制器，演示编译期优化相关能力（运行期动态编译与加载执行）。
 * English: Chapter 10 controller demonstrating compile-time optimization via runtime compilation and execution.
 *
 * 使用场景 / Use Cases:
 * 中文：观察常量折叠、内联等优化的效果，理解编译与执行的衔接。
 * English: Observe effects of constant folding and inlining, understanding the bridge between compilation and execution.
 *
 * 设计目的 / Design Purpose:
 * 中文：以简单类为载体，降低复杂度并突出关键步骤。
 * English: Use a simple class to reduce complexity and highlight key steps.
 */
@Slf4j
@RestController
@RequestMapping("/chapter10")
public class Chapter10Controller {

    /**
     * 方法说明 / Method Description:
     * 中文：动态编写、编译并加载执行包含表达式计算的 Java 源码。
     * English: Dynamically write, compile and load-execute Java source with an expression computation.
     *
     * 参数 / Parameters:
     * @param expression 中文：要计算的表达式文本 / English: Expression text to compute
     * 返回值 / Return: 中文：计算结果字符串 / English: Computation result as string
     * 异常 / Exceptions: 中文：可能抛出编译与反射执行异常 / English: May throw compilation and reflective execution exceptions
     */
    @GetMapping("/dynamic-compile")
    public String dynamicCompile(@RequestParam(defaultValue = "1+2+3") String expression) throws Exception {
        log.info("开始动态编译 Dynamic compiling expression: {}", expression);
        Path tempDir = Files.createTempDirectory("dynamic-compile");
        File sourceFile = new File(tempDir.toFile(), "DynamicCalc.java");
        try (FileWriter writer = new FileWriter(sourceFile)) {
            writer.write(generateSource(expression));
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("JDK compiler not available");
        }
        int result = compiler.run(null, null, null, sourceFile.getPath());
        if (result != 0) {
            throw new IllegalStateException("Compilation failed, exit code=" + result);
        }
        try (URLClassLoader loader = new URLClassLoader(new URL[]{tempDir.toUri().toURL()})) {
            Class<?> clazz = loader.loadClass("DynamicCalc");
            Object value = clazz.getDeclaredMethod("compute").invoke(null);
            log.info("动态编译执行结果 Result from compiled class: {}", value);
            return String.valueOf(value);
        } finally {
            Files.walk(tempDir)
                    .map(Path::toFile)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(File::delete);
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：生成包含静态方法 compute() 的 Java 源码，方法返回表达式计算结果。
     * English: Generate Java source with a static compute() method returning the expression result.
     *
     * 参数 / Parameters:
     * @param expression 中文：表达式文本 / English: Expression text
     * 返回值 / Return: 中文：源码字符串 / English: Source code string
     * 异常 / Exceptions: 无
     */
    private String generateSource(String expression) {
        return "public class DynamicCalc {\n" +
                "    public static Object compute() {\n" +
                "        return " + expression + ";\n" +
                "    }\n" +
                "}\n";
    }
}
