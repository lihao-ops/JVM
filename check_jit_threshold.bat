@echo off
echo ========================================================
echo [JVM Lab] 正在查询当前环境的 JIT 编译阈值...
echo ========================================================
echo.

:: 查询 CompileThreshold 参数
:: -XX:+PrintFlagsFinal 会打印 JVM 所有参数的最终值
:: findstr 用于过滤出我们需要的那一行
java -XX:+PrintFlagsFinal -version | findstr CompileThreshold

echo.
echo ========================================================
echo 解读：
echo 1. 如果看到 intx CompileThreshold = 10000，说明默认阈值确实是 1万次。
echo 2. 这个值是可以改的，比如启动时加 -XX:CompileThreshold=5000
echo ========================================================
pause