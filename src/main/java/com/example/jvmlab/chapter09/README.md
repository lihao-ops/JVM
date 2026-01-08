# 第9章 类加载及执行子系统的案例与实战

> **对应书籍**: 《深入理解Java虚拟机（第3版）》第9章  
> **核心主题**: 动态代理、字节码生成、热部署

---

## 📖 核心内容概述

### 9.1 动态代理对比

| 技术 | 原理 | 优缺点 |
| :--- | :--- | :--- |
| **JDK 动态代理** | 基于接口，反射调用 | 简单，必须有接口 |
| **CGLIB** | 基于继承，ASM 生成子类 | 无需接口，不能代理 final |
| **Javassist** | 源码级操作 | 易用，性能略低 |
| **Byte Buddy** | 流式 API，类型安全 | 现代首选，性能好 |

### 9.2 JDK 动态代理原理

```java
// 代理生成过程
// 1. 生成代理类的字节码
byte[] classBytes = ProxyGenerator.generateProxyClass(
    proxyName, interfaces, accessFlags
);

// 2. 加载代理类
Class<?> proxyClass = defineClass(proxyName, classBytes);

// 3. 创建代理实例
Constructor<?> cons = proxyClass.getConstructor(InvocationHandler.class);
return cons.newInstance(handler);

// 生成的代理类结构
public final class $Proxy0 extends Proxy implements UserService {
    public String findUser(Long id) {
        // 调用 InvocationHandler.invoke()
        return (String) super.h.invoke(this, m3, new Object[]{id});
    }
}
```

### 9.3 CGLIB 代理原理

```java
// CGLIB 通过 ASM 生成子类
public class UserService$$EnhancerByCGLIB extends UserService {
    private MethodInterceptor interceptor;
    
    @Override
    public String findUser(Long id) {
        // 调用拦截器
        return (String) interceptor.intercept(
            this, 
            findUserMethod, 
            new Object[]{id},
            methodProxy
        );
    }
}
```

### 9.4 字节码增强应用场景

| 场景 | 典型框架 | 技术实现 |
| :--- | :--- | :--- |
| **AOP 切面** | Spring AOP | JDK Proxy / CGLIB |
| **ORM 延迟加载** | Hibernate | Javassist |
| **Mock 测试** | Mockito | Byte Buddy |
| **APM 监控** | SkyWalking | Agent + ASM |
| **热部署** | JRebel | Agent + 类重定义 |

---

## 💻 代码实践清单

### 实验1: JDK 动态代理

```bash
GET /chapter09/jdk-proxy
```

**实验代码**: `Chapter09Controller.java:34`

```java
// 创建代理
UserService proxy = (UserService) Proxy.newProxyInstance(
    UserService.class.getClassLoader(),
    new Class[]{UserService.class},
    (proxyObj, method, args) -> {
        System.out.println("Before: " + method.getName());
        Object result = method.invoke(target, args);
        System.out.println("After: " + method.getName());
        return result;
    }
);
```

### 实验2: ASM 动态类生成

```bash
GET /chapter09/asm-generate
```

**实验代码**: `Chapter09Controller.java:57`

### 实验3: 使用 AsmDynamicClassBuilder

```java
// 使用封装好的工具类
AsmDynamicClassBuilder builder = new AsmDynamicClassBuilder();
Class<?> clazz = builder.generateClass("com.example.Generated");
Object instance = clazz.getDeclaredConstructor().newInstance();
```

**工具位置**: `common/AsmDynamicClassBuilder.java`

---

## 🏭 生产实践建议

### 1. Spring AOP 代理选择

```java
// 默认策略：
// - 有接口：JDK 动态代理
// - 无接口：CGLIB

// 强制使用 CGLIB
@EnableAspectJAutoProxy(proxyTargetClass = true)

// 注意事项：
// 1. CGLIB 不能代理 final 方法
// 2. 自调用问题（this.method() 不走代理）
@Service
public class UserService {
    public void methodA() {
        this.methodB();  // 不会触发切面！
    }
    
    @Transactional
    public void methodB() {}
}

// 解决方案：
// 1. 注入自己
@Autowired private UserService self;
public void methodA() {
    self.methodB();  // 走代理
}

// 2. 使用 AopContext
((UserService) AopContext.currentProxy()).methodB();
```

### 2. APM 无侵入埋点

```java
// Java Agent + ASM 实现无侵入监控
// premain 方法在应用启动前执行
public static void premain(String args, Instrumentation inst) {
    inst.addTransformer(new ClassFileTransformer() {
        @Override
        public byte[] transform(ClassLoader loader, String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain domain, byte[] classBytes) {
            if (shouldTransform(className)) {
                // 使用 ASM 增强字节码
                return enhanceClass(classBytes);
            }
            return null;
        }
    });
}

// 启动时加载 Agent
java -javaagent:my-agent.jar -jar app.jar
```

### 3. 热部署实现

```java
// 热部署核心：Instrumentation.redefineClasses()
public void hotSwap(String className, byte[] newClassData) {
    Class<?> clazz = Class.forName(className);
    ClassDefinition def = new ClassDefinition(clazz, newClassData);
    instrumentation.redefineClasses(def);
}

// 限制：
// 1. 不能改变类结构（字段、方法签名）
// 2. 不能改变继承关系
// 3. 已加载的对象不会自动更新

// 商业方案（JRebel）突破限制：
// 使用新 ClassLoader 加载新版本
// 代理模式转发调用
```

### 4. 字节码增强框架选型

```java
// 推荐：Byte Buddy（现代、类型安全、高性能）
Class<?> dynamicType = new ByteBuddy()
    .subclass(Object.class)
    .method(named("toString"))
    .intercept(FixedValue.value("Hello Byte Buddy!"))
    .make()
    .load(getClass().getClassLoader())
    .getLoaded();

// 简单场景：Javassist（源码级操作）
CtClass cc = ClassPool.getDefault().get("com.example.Target");
CtMethod m = cc.getDeclaredMethod("process");
m.insertBefore("System.out.println(\"Before\");");

// 极致性能：ASM（底层操作）
// 需要熟悉字节码指令
```

---

## 🎯 面试考点提炼

### 高频问题

1. **"JDK 动态代理和 CGLIB 的区别？"**
   - JDK：基于接口，反射调用
   - CGLIB：基于继承，ASM 生成子类
   - JDK 适合有接口；CGLIB 适合无接口、需要代理 protected 方法

2. **"Spring AOP 默认使用哪种代理？"**
   - 有接口用 JDK，无接口用 CGLIB
   - Spring Boot 2.x 默认全部用 CGLIB

3. **"为什么 CGLIB 不能代理 final 方法？"**
   - CGLIB 通过继承生成子类
   - final 方法不能被重写

4. **"什么是自调用问题？如何解决？"**
   - this.method() 不经过代理
   - 解决：注入自己、AopContext、拆分服务

5. **"APM 工具如何无侵入埋点？"**
   - Java Agent + Instrumentation API
   - 启动时/运行时增强字节码
   - 拦截方法调用，采集指标

### 进阶问题

6. **"如何实现热部署？有什么限制？"**
   - Instrumentation.redefineClasses()
   - 限制：不能改变类结构、继承关系

7. **"字节码增强框架如何选择？"**
   - 简单易用：Javassist
   - 现代首选：Byte Buddy
   - 极致性能：ASM

8. **"动态代理性能如何优化？"**
   - 缓存生成的代理类
   - 使用 MethodHandle 替代反射
   - CGLIB FastClass 避免反射调用

---

## 📚 相关资源

- 书籍章节: 《深入理解JVM》第9章 9.1-9.3
- 工具类: `common/AsmDynamicClassBuilder.java`
- 依赖: ASM、Byte Buddy、Javassist
