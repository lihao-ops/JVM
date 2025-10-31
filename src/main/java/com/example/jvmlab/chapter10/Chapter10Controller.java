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
 * 第10章：早期（编译期）优化实践。
 * <p>
 * 实现思路：
 * 1. 通过JavaCompiler在运行期动态编译源码，观察编译阶段的常量折叠、内联等优化。
 * 2. 将编译结果加载执行，展示即时编译前的字节码优化策略。
 * 3. 为避免复杂性，该示例编译一个简单类，并在日志中解释关键步骤。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/chapter10")
public class Chapter10Controller {

    /**
     * 动态编译一段Java代码，并执行其中的方法。
     *
     * @param expression 需要计算的表达式。
     * @return 运行结果。
     * @throws Exception 动态编译或执行异常。
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
     * 生成用于动态编译的Java源码。
     *
     * @param expression 需要计算的表达式。
     * @return Java源码字符串。
     */
    private String generateSource(String expression) {
        return "public class DynamicCalc {\n" +
                "    public static Object compute() {\n" +
                "        return " + expression + ";\n" +
                "    }\n" +
                "}\n";
    }
}
