@echo off
echo ========================================================
echo [JVM Lab] 正在以 JIT 监控模式启动 Spring Boot...
echo ========================================================
echo.
echo 关键参数说明：
echo 1. -XX:+PrintCompilation : 打印 JIT 编译日志（你会看到大量输出）
echo 2. -XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation : (可选) 输出更详细的 XML 日志
echo.
echo 正在启动... 请观察控制台输出的 "代码雨"...
echo 每一行代表一个被 JIT 编译器优化的方法。
echo.

:: 编译项目（确保最新代码生效）
call mvn clean package -DskipTests

:: 启动 jar 包，带上 JIT 打印参数
java -XX:+PrintCompilation -jar target/jvm-learning-0.0.1-SNAPSHOT.jar

pause