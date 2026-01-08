# 第10章 前端编译与优化

> **对应书籍**: 《深入理解Java虚拟机（第3版）》第10章  
> **核心主题**: 编译器分类、语法糖、编译期优化

---

## 📖 核心内容概述

### 10.1 编译器分类

| 编译器类型 | 输入 | 输出 | 代表 |
| :--- | :--- | :--- | :--- |
| **前端编译器** | .java 源码 | .class 字节码 | javac、ECJ |
| **后端编译器** | .class 字节码 | 机器码 | JIT (C1/C2)、AOT |
| **静态编译器** | .java 源码 | 机器码 | GraalVM Native Image |

### 10.2 javac 编译过程

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ 解析与   │ → │ 填充    │ → │ 注解    │ → │ 语义    │ → 生成字节码
│ 填充符号表│   │ 符号表  │   │ 处理    │   │ 分析    │
└─────────┘    └─────────┘    └─────────┘    └─────────┘
    ↓              ↓              ↓              ↓
  词法分析       类型检查      执行 APT       数据流分析
  语法分析       注解处理      生成代码       解语法糖
  生成 AST
```

### 10.3 常见语法糖

| 语法糖 | 源代码 | 编译后 |
| :--- | :--- | :--- |
| **泛型** | `List<String>` | `List`（类型擦除） |
| **自动装箱** | `Integer i = 1` | `Integer.valueOf(1)` |
| **条件编译** | `if (常量)` | 移除不可达分支 |
| **增强 for** | `for (T t : list)` | 迭代器或数组遍历 |
| **try-with-resources** | `try (res)` | 自动关闭 finally |
| **var 关键字** | `var s = "hi"` | 推断为 `String s` |
| **Switch 表达式** | `switch ->` | 传统 switch 语句 |

### 10.4 泛型类型擦除

```java
// 源代码
List<String> list = new ArrayList<>();
list.add("hello");
String s = list.get(0);

// 编译后（类型擦除）
List list = new ArrayList();
list.add("hello");
String s = (String) list.get(0);  // 编译器插入强制转换

// 证明：运行时类型相同
new ArrayList<String>().getClass() == new ArrayList<Integer>().getClass()  // true
```

---

## 💻 代码实践清单

### 实验1: 运行期动态编译

```bash
GET /chapter10/dynamic-compile
```

**实验代码**: `Chapter10Controller.java:39`

```java
// 使用 JavaCompiler API 动态编译
JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
int result = compiler.run(null, null, null, sourceFile);

// 加载并执行编译后的类
URLClassLoader loader = new URLClassLoader(new URL[]{outputDir.toURI().toURL()});
Class<?> clazz = loader.loadClass("DynamicClass");
Object instance = clazz.getDeclaredConstructor().newInstance();
```

### 实验2: 源码生成

```bash
GET /chapter10/generate-source
```

**实验代码**: `Chapter10Controller.java:74`

### 实验3: 观察语法糖解糖

```bash
# 使用 javap 查看编译后的字节码
javap -c -p ClassName.class

# 或使用 CFR 反编译工具
java -jar cfr.jar ClassName.class
```

---

## 🏭 生产实践建议

### 1. 注解处理器 (APT)

```java
// 定义注解
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface GenerateBuilder {}

// 实现处理器
@SupportedAnnotationTypes("com.example.GenerateBuilder")
public class BuilderProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, 
                           RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(GenerateBuilder.class)) {
            // 生成 Builder 类源码
            generateBuilder(element);
        }
        return true;
    }
}

// 典型框架：Lombok、MapStruct、AutoValue
```

### 2. 条件编译优化

```java
// 问题：开发环境需要日志，生产环境需要性能
if (DEBUG) {
    logger.debug("Processing item: " + item);  // 字符串拼接有开销
}

// 最佳实践：使用常量 + 条件编译
public static final boolean DEBUG = false;  // 编译时移除整个分支

if (DEBUG) {
    // 这段代码不会出现在字节码中
}

// 或使用日志框架的惰性求值
logger.debug("Processing item: {}", item);  // 不触发时不拼接
```

### 3. 泛型擦除的陷阱

```java
// 陷阱 1：不能创建泛型数组
// T[] array = new T[10];  // 编译错误

// 陷阱 2：不能用 instanceof 检查泛型类型
// if (list instanceof List<String>)  // 编译错误

// 陷阱 3：类型参数不保留
public class MyList<T> {
    private Class<T> type;  // 运行时无法获取 T 的实际类型
    
    // 解决：显式传入 Class
    public MyList(Class<T> type) {
        this.type = type;
    }
}

// 陷阱 4：桥方法
class StringList extends ArrayList<String> {
    @Override
    public String get(int index) { ... }  // 实际生成桥方法
    // synthetic bridge: Object get(int) 调用 String get(int)
}
```

### 4. 运行时编译应用

```java
// 场景 1：规则引擎（将业务规则编译为代码）
String rule = "return price > 100 && category.equals(\"Electronics\");";
Class<?> ruleClass = compileRule(rule);
Predicate<Product> filter = (Predicate<Product>) ruleClass.newInstance();

// 场景 2：表达式计算器
String expression = "a * b + c";
Function<Map<String, Double>, Double> calculator = compile(expression);

// 场景 3：动态模板引擎
String template = "Hello, ${name}!";
BiFunction<String, Map<String, Object>, String> renderer = compileTemplate(template);

// 注意：
// 1. 编译是昂贵操作，应缓存编译结果
// 2. 注意类加载器泄漏
// 3. 生产环境需要沙箱隔离
```

---

## 🎯 面试考点提炼

### 高频问题

1. **"Java 编译器有哪些？区别是什么？"**
   - javac：源码→字节码，语法糖处理
   - JIT：字节码→机器码，运行时优化
   - GraalVM：源码→机器码，AOT 编译

2. **"什么是泛型擦除？有什么影响？"**
   - 编译后泛型信息被移除
   - 运行时无法获知泛型实际类型
   - 不能创建泛型数组、instanceof 检查

3. **"常见的语法糖有哪些？"**
   - 泛型、自动装箱、增强 for、try-with-resources、Lambda、var

4. **"自动装箱有什么陷阱？"**
   ```java
   Integer a = 127, b = 127;
   a == b  // true (缓存池 -128~127)
   
   Integer c = 128, d = 128;
   c == d  // false (新对象)
   
   Integer e = 1;
   e == 1  // true (拆箱比较)
   ```

5. **"什么是注解处理器？如何实现？"**
   - 编译期处理注解，生成代码
   - 继承 AbstractProcessor，实现 process 方法
   - 典型：Lombok、MapStruct

### 进阶问题

6. **"如何查看编译后的代码？"**
   - javap -c：查看字节码
   - CFR/Procyon：反编译工具
   - IDEA 反编译

7. **"Lambda 和匿名内部类有什么区别？"**
   - 匿名内部类：编译生成 Outer$1.class
   - Lambda：invokedynamic + 运行时生成

8. **"什么是桥方法？"**
   - 泛型擦除后为保持多态生成的合成方法
   -子类重写泛型方法时自动生成

---

## 📚 相关资源

- 书籍章节: 《深入理解JVM》第10章 10.1-10.4
- API: javax.tools.JavaCompiler
- 工具: javac、javap、CFR、Procyon
