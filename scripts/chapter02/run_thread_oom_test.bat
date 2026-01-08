@echo off
:: 切换到项目根目录
cd /d "%~dp0..\..\"

echo ========================================================
echo [JVM Lab] ⚠️ 危险实验：多线程导致 OutOfMemoryError ⚠️
echo ========================================================
echo.
echo 原理: 不断创建线程，直到耗尽操作系统内存。
echo 后果: 可能导致 Windows 卡死、鼠标无响应！
echo 参数: -Xss2M (每个线程分配 2MB 栈空间，加速内存耗尽)
echo.
echo 请做好准备，一旦看到报错或系统变卡，立即关闭此窗口！
echo.
set /p "confirm=确认要运行吗？(输入 y 继续): "
if not "%confirm%"=="y" exit

:: 编译
call mvn clean compile -DskipTests > nul

:: 运行
java -Xss2M -cp target/classes com.example.jvmlab.chapter02.stack.ThreadOomTest

echo.
pause