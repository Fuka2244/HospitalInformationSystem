@echo off
title Stopping Kafka & Redis

set "KAFKA_HOME=D:\development\kafka_2.13-4.2.0"

echo ============================================
echo    Stopping Kafka & Redis
echo ============================================
echo.

echo [1] Stopping Kafka ...
cd /d "%KAFKA_HOME%"
call .\bin\windows\kafka-server-stop.bat
echo [OK] Kafka stopped
echo.

echo [2] Stopping Redis ...
taskkill /im redis-server.exe /f >nul 2>&1
echo [OK] Redis stopped
echo.

echo All stopped.
pause
