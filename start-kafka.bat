@echo off
title Kafka 4.x Server
color 0A

echo ========================================================
echo        Kafka 4.x 启动脚本 (KRaft模式)
echo ========================================================

:: 切换到脚本所在的当前目录，防止路径错误
cd /D:/development/kafka_2.13-4.2.0

:: 检查配置文件是否存在，防止因为路径不对导致报错
if not exist ".\config\server.properties" (
    color 0C
    echo [错误] 找不到 .\config\server.properties 文件！
    echo 请确保本脚本放在 Kafka 的根目录下（例如 D:\development\kafka_2.13-4.2.0\）。
    pause
    exit /b 1
)

echo [信息] 找到配置文件，正在启动 Kafka...
echo [提示] 停止服务请在此窗口直接按 Ctrl + C。
echo.

:: 执行 Kafka 启动命令
call .\bin\windows\kafka-server-start.bat .\config\server.properties

pause