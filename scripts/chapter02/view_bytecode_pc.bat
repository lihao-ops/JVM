@echo off
:: 切换到项目根目录 (假设脚本在 scripts/chapter02/)
cd /d "%~dp0..\..\"

echo ========================================================
echo [JVM Lab] 正在反汇编 ProgramCounterTest...
echo ========================================================
echo 当前工作目录: %cd%
echo.

:: 编译
call mvn clean compile -DskipTests > nul

:: 反汇编
javap -c -l -p target/classes/com/example/jvmlab/chapter02/ProgramCounterTest.class

echo.
echo ========================================================
echo 请对照上面的输出，在 IDEA Debug 时观察。
echo ========================================================
pause