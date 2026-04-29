@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ============================================================
echo    HIS 缓存性能对比测试
echo    使用前请确保: MySQL + Redis 已启动, 后端服务运行在 8080 端口
echo ============================================================
echo.

set BASE_URL=http://localhost:8080/HIS
set ROUNDS=10

echo [1/3] 测试药品列表接口 (有缓存 vs 无缓存)
echo --------------------------------------------

:: 第一次请求 (无缓存/冷启动)
echo 首次请求 (冷启动, 无缓存):
curl -s -w "  响应时间: %%{time_total}s\n" -o nul "%BASE_URL%/medicine/list?page=1&size=10"

:: 连续请求 (缓存命中)
echo.
echo 后续请求 (缓存命中):
for /L %%i in (1,1,%ROUNDS%) do (
    curl -s -w "  第%%i次: %%{time_total}s\n" -o nul "%BASE_URL%/medicine/list?page=1&size=10"
)

echo.
echo [2/3] 测试科室列表接口
echo --------------------------------------------
echo 首次请求 (冷启动):
curl -s -w "  响应时间: %%{time_total}s\n" -o nul "%BASE_URL%/department/list"

echo.
echo 后续请求 (缓存命中):
for /L %%i in (1,1,%ROUNDS%) do (
    curl -s -w "  第%%i次: %%{time_total}s\n" -o nul "%BASE_URL%/department/list"
)

echo.
echo [3/3] 测试科室详情接口
echo --------------------------------------------
echo 首次请求 (冷启动):
curl -s -w "  响应时间: %%{time_total}s\n" -o nul "%BASE_URL%/department/1"

echo.
echo 后续请求 (缓存命中):
for /L %%i in (1,1,%ROUNDS%) do (
    curl -s -w "  第%%i次: %%{time_total}s\n" -o nul "%BASE_URL%/department/1"
)

echo.
echo ============================================================
echo    测试完成！对比首次请求(无缓存)与后续请求(有缓存)的时间差异
echo ============================================================
pause
