@echo off
:: 切换到项目根目录
cd /d "%~dp0..\..\"

echo ========================================================
echo [JVM Lab] 正在运行 StackOverflowError 实验...
echo ========================================================
echo.
echo 关键参数: -Xss160k (限制栈容量为 160KB)
echo 预期结果: 抛出 java.lang.StackOverflowError，并打印最大深度。
echo.

:: 编译
call mvn clean compile -DskipTests > nul

:: 运行
java -Xss160k -cp target/classes com.example.jvmlab.chapter02.stack.StackOverflowTest

echo.
pause