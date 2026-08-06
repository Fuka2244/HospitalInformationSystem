@echo off
title Kafka Format Tool

set "KAFKA_HOME=D:\development\kafka_2.13-4.2.0"

echo ============================================
echo    Kafka KRaft Storage Format
echo ============================================
echo.

if not exist "%KAFKA_HOME%\bin\windows\kafka-storage.bat" (
    echo [ERROR] Kafka not found!
    pause
    exit /b
)
cd /d "%KAFKA_HOME%"

echo [1] Fix log.dirs ...
powershell -Command "$c=Get-Content .\config\server.properties; $c=$c -replace '^log.dirs=/kafka-logs','log.dirs=kafka-logs'; $c|Set-Content .\config\server.properties"

echo [2] Generate UUID and format ...
powershell -Command "$env:CID=[guid]::NewGuid().ToString(); .\bin\windows\kafka-storage.bat format --standalone -c .\config\server.properties -t $env:CID"

if exist "kafka-logs\meta.properties" (
    echo.
    echo [SUCCESS] Kafka formatted!
    echo Now run: start-kafka-redis.bat
) else (
    echo.
    echo [FAILED] Format failed. Check JAVA_HOME.
)

pause
