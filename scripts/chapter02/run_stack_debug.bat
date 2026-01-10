@echo off
:: 切换到项目根目录
cd /d "%~dp0..\..\"

echo ========================================================
echo [JVM Lab] 正在启动 Spring Boot 应用 (栈实验模式)...
echo ========================================================
echo.
echo 实验指南:
echo 1. 正常启动 (默认栈大小): 直接按回车
echo 2. 栈溢出测试: 输入 -Xss160k
echo.
set /p "jvm_opts=请输入 JVM 参数 (例如 -Xss160k): "

echo.
echo 正在启动... 请等待 Spring Boot 启动完成。
echo 启动后访问:
echo - Debug 实验: http://localhost:8080/jvm/stack/debug?limit=5
echo - 溢出实验: http://localhost:8080/jvm/stack/overflow
echo.

:: 编译
call mvn clean package -DskipTests > nul

:: 运行
java %jvm_opts% -jar target/jvm-learning-0.0.1-SNAPSHOT.jar

pause