@echo off
title Kafka & Redis Starter

set "KAFKA_HOME=D:\development\kafka_2.13-4.2.0"
set "REDIS_HOME=D:\development\Redis-8.6.2-Windows-x64-cygwin-with-Service"

echo ============================================
echo    HIS Middleware Starter
echo    Kafka: localhost:9092 (KRaft)
echo    Redis: localhost:6379
echo ============================================
echo.

:: Check paths
if not exist "%REDIS_HOME%\redis-server.exe" (
    echo [ERROR] Redis not found at: %REDIS_HOME%
    pause
    exit /b 1
)
if not exist "%KAFKA_HOME%\bin\windows\kafka-server-start.bat" (
    echo [ERROR] Kafka not found at: %KAFKA_HOME%
    pause
    exit /b 1
)

:: ---- Redis ----
echo [1/2] Starting Redis ...
start "Redis-Server" /D "%REDIS_HOME%" redis-server.exe redis.conf
echo [OK] Redis launched (localhost:6379)
echo.

:: ---- Kafka ----
echo [2/2] Starting Kafka ...
cd /d "%KAFKA_HOME%"

:: Fix log.dirs for Windows
echo Fixing log.dirs path ...
powershell -Command "$c=Get-Content .\config\server.properties; $c=$c -replace '^log.dirs=/kafka-logs','log.dirs=kafka-logs'; $c|Set-Content .\config\server.properties"

:: Clean stale lock file (left by crashed/killed Kafka process)
if exist "kafka-logs\.lock" (
    echo Removing stale lock file...
    del "kafka-logs\.lock" 2>nul
)

:: Format if needed
if exist "kafka-logs\meta.properties" (
    echo [Skip] Kafka already formatted
) else (
    echo First run - formatting ...
    for /f "delims=" %%i in ('powershell -Command [guid]::NewGuid().ToString()') do set CID=%%i
    call .\bin\windows\kafka-storage.bat format --standalone -c .\config\server.properties -t %CID%
    if errorlevel 1 (
        echo [ERROR] Kafka format failed!
        pause
        exit /b 1
    )
    echo [OK] Formatted
)

start "Kafka-Server" .\bin\windows\kafka-server-start.bat .\config\server.properties
echo [OK] Kafka launched (localhost:9092)
echo.

echo ============================================
echo    All services started!
echo    Redis:  localhost:6379
echo    Kafka:  localhost:9092
echo ============================================
echo.
pause
