# 第6章 类文件结构

> **对应书籍**: 《深入理解Java虚拟机（第3版）》第6章  
> **核心主题**: Class 文件格式、常量池、字段表、方法表、属性表

---

## 📖 核心内容概述

### 6.1 Class 文件结构总览

```
ClassFile {
    u4             magic;           // 魔数: 0xCAFEBABE
    u2             minor_version;   // 次版本号
    u2             major_version;   // 主版本号 (52=JDK8, 55=JDK11, 61=JDK17)
    u2             constant_pool_count;
    cp_info        constant_pool[constant_pool_count-1];
    u2             access_flags;    // 访问标志: public/final/abstract...
    u2             this_class;      // 类索引
    u2             super_class;     // 父类索引
    u2             interfaces_count;
    u2             interfaces[interfaces_count];
    u2             fields_count;
    field_info     fields[fields_count];
    u2             methods_count;
    method_info    methods[methods_count];
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
```

### 6.2 常量池项类型

| 类型 | 标志值 | 说明 |
| :--- | :--- | :--- |
| CONSTANT_Utf8 | 1 | UTF-8 字符串 |
| CONSTANT_Integer | 3 | 整型字面量 |
| CONSTANT_Float | 4 | 浮点字面量 |
| CONSTANT_Long | 5 | 长整型字面量 |
| CONSTANT_Double | 6 | 双精度浮点 |
| CONSTANT_Class | 7 | 类或接口符号引用 |
| CONSTANT_String | 8 | 字符串字面量 |
| CONSTANT_Fieldref | 9 | 字段符号引用 |
| CONSTANT_Methodref | 10 | 方法符号引用 |
| CONSTANT_MethodHandle | 15 | 方法句柄 |
| CONSTANT_InvokeDynamic | 18 | 动态调用点 |

### 6.3 访问标志

| 标志名称 | 值 | 说明 |
| :--- | :--- | :--- |
| ACC_PUBLIC | 0x0001 | public 类 |
| ACC_FINAL | 0x0010 | final 类，不可继承 |
| ACC_SUPER | 0x0020 | 使用新的 invokespecial 语义 |
| ACC_INTERFACE | 0x0200 | 接口 |
| ACC_ABSTRACT | 0x0400 | 抽象类 |
| ACC_SYNTHETIC | 0x1000 | 编译器生成，非源码 |
| ACC_ANNOTATION | 0x2000 | 注解类型 |
| ACC_ENUM | 0x4000 | 枚举类型 |

### 6.4 方法表与 Code 属性

```
method_info {
    u2             access_flags;
    u2             name_index;       // 方法名
    u2             descriptor_index; // 方法描述符 (I)V
    u2             attributes_count;
    attribute_info attributes[];     // 包含 Code 属性
}

Code_attribute {
    u2 max_stack;   // 操作数栈最大深度
    u2 max_locals;  // 局部变量表大小
    u4 code_length;
    u1 code[];      // 字节码指令
    // 异常表、行号表、局部变量表...
}
```

---

## 💻 代码实践清单

### 实验1: ASM 类结构解析

```bash
GET /chapter06/parse-class?className=java.lang.String
```

**实验代码**: `Chapter06Controller.java:36`

### 实验2: 使用 javap 查看字节码

```bash
# 编译
javac -g HelloWorld.java

# 查看字节码
javap -v -p HelloWorld.class

# 输出关键部分：
# - Constant pool (常量池)
# - access_flags (访问标志)
# - Methods (方法表)
# - Code (字节码指令)
```

### 实验3: 手动解析 Class 文件

```java
// 读取 Class 文件头
try (DataInputStream dis = new DataInputStream(new FileInputStream("Test.class"))) {
    int magic = dis.readInt();
    System.out.printf("Magic: 0x%X%n", magic);  // 0xCAFEBABE
    
    int minorVersion = dis.readUnsignedShort();
    int majorVersion = dis.readUnsignedShort();
    System.out.printf("Version: %d.%d%n", majorVersion, minorVersion);
    
    int constantPoolCount = dis.readUnsignedShort();
    System.out.printf("Constant Pool Count: %d%n", constantPoolCount);
}
```

---

## 🏭 生产实践建议

### 1. 字节码增强框架对比

| 框架 | 特点 | 使用场景 |
| :--- | :--- | :--- |
| **ASM** | 底层、高性能、学习曲线陡 | 追求极致性能 |
| **Javassist** | 基于源码级操作，易上手 | 快速开发 |
| **Byte Buddy** | 流式 API，类型安全 | 现代框架首选 |
| **cglib** | 基于 ASM，创建代理 | Spring AOP |

### 2. 常见字节码增强场景

```java
// 1. AOP 切面：方法前后增加逻辑
// Spring AOP、AspectJ

// 2. 热部署：替换类定义
// JRebel、Spring DevTools

// 3. 链路追踪：无侵入埋点
// SkyWalking、Pinpoint

// 4. Mock 框架：运行时创建 Mock 类
// Mockito、PowerMock

// 5. ORM 框架：延迟加载代理
// Hibernate、MyBatis
```

### 3. ASM 类生成示例

```java
// 使用 ASM 动态生成类
ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
cw.visit(V17, ACC_PUBLIC, "com/example/Generated", null, "java/lang/Object", null);

// 生成构造函数
MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
mv.visitCode();
mv.visitVarInsn(ALOAD, 0);
mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
mv.visitInsn(RETURN);
mv.visitMaxs(1, 1);
mv.visitEnd();

// 生成方法
mv = cw.visitMethod(ACC_PUBLIC, "sayHello", "()V", null, null);
mv.visitCode();
mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
mv.visitLdcInsn("Hello, ASM!");
mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
mv.visitInsn(RETURN);
mv.visitMaxs(2, 1);
mv.visitEnd();

byte[] bytecode = cw.toByteArray();
```

### 4. 类文件版本兼容

```bash
# 查看 Class 文件版本
javap -v SomeClass.class | grep "major version"

# 版本对应关系
# major version 52 = JDK 8
# major version 55 = JDK 11
# major version 61 = JDK 17

# 低版本 JDK 运行高版本 Class：
# UnsupportedClassVersionError

# 解决：使用 --release 编译
javac --release 8 SomeClass.java
```

---

## 🎯 面试考点提炼

### 高频问题

1. **"Class 文件的结构是什么？"**
   - 魔数 → 版本号 → 常量池 → 访问标志
   - 类/父类/接口索引 → 字段表 → 方法表 → 属性表

2. **"常量池有什么作用？"**
   - 存放字面量（数字、字符串）
   - 存放符号引用（类、字段、方法的引用）
   - 类加载时解析为直接引用

3. **"方法描述符怎么表示？"**
   ```
   ()V        - 无参数返回 void
   (I)V       - int 参数返回 void
   (II)I      - 两个 int 参数返回 int
   ([Ljava/lang/String;)V - String 数组参数返回 void
   ```

4. **"什么是符号引用和直接引用？"**
   - 符号引用：用文本描述的引用（类名、方法名）
   - 直接引用：运行时实际内存地址/偏移量
   - 类加载的解析阶段完成转换

5. **"为什么 JVM 使用 Class 文件格式？"**
   - 平台无关性：一次编译，到处运行
   - 语言无关性：Kotlin、Scala 都编译为 Class
   - 紧凑高效：二进制格式，加载快

### 进阶问题

6. **"invokespecial 和 invokevirtual 的区别？"**
   - invokespecial：调用构造函数、私有方法、super 方法
   - invokevirtual：调用普通虚方法，支持多态

7. **"invokedynamic 有什么作用？"**
   - JDK7 引入，支持动态语言
   - Lambda 表达式底层实现
   - 首次调用时通过 BootstrapMethod 绑定

---

## 📚 相关资源

- 书籍章节: 《深入理解JVM》第6章 6.1-6.4
- 工具类: `common/AsmDynamicClassBuilder.java`
- 参考: Oracle JVM Specification, Chapter 4
