@echo off
:: 切换到项目根目录
cd /d "%~dp0..\..\"

echo ========================================================
echo [JVM Lab] 正在运行 Heap OOM 实验...
echo ========================================================
echo.
echo 关键参数:
echo 1. -Xms20m -Xmx20m : 堆内存限制为 20MB (快速触发)
echo 2. -XX:+HeapDumpOnOutOfMemoryError : OOM 时自动生成快照
echo.

:: 编译
call mvn clean compile -DskipTests > nul

:: 运行 (注意包名变化: .heap.HeapOomTest)
java -Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError -cp target/classes;D:\maven3.9.9\repository\org\slf4j\slf4j-api\2.0.17\slf4j-api-2.0.17.jar;D:\maven3.9.9\repository\ch\qos\logback\logback-classic\1.5.18\logback-classic-1.5.18.jar;D:\maven3.9.9\repository\ch\qos\logback\logback-core\1.5.18\logback-core-1.5.18.jar;D:\maven3.9.9\repository\org\projectlombok\lombok\1.18.38\lombok-1.18.38.jar com.example.jvmlab.chapter02.heap.HeapOomTest

echo.
echo ========================================================
echo 实验结束。
echo 请在项目根目录下寻找 .hprof 文件 (例如 java_pid12345.hprof)
echo 可以使用 JProfiler 或 Eclipse MAT 打开分析。
echo ========================================================
pause