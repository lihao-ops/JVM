@echo off
:: 切换到项目根目录 (假设脚本在 scripts/chapter01/)
cd /d "%~dp0..\..\"

echo ========================================================
echo [JVM Lab] 正在以 JIT 监控模式启动 Spring Boot...
echo ========================================================
echo 当前工作目录: %cd%
echo.

echo 关键参数说明：
echo 1. -XX:+PrintCompilation : 打印 JIT 编译日志
echo.

:: 编译项目
call mvn clean package -DskipTests

:: 启动 jar 包
java -XX:+PrintCompilation -jar target/jvm-learning-0.0.1-SNAPSHOT.jar

pause