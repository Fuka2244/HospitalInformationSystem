@echo off
setlocal

set TESTS=AppointmentServiceWhiteBoxTest,AiAppointmentServiceWhiteBoxTest
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%\..\..") do set "PROJECT_ROOT=%%~fI"

echo Running white-box unit tests: %TESTS%
pushd "%PROJECT_ROOT%"
call "%PROJECT_ROOT%\mvnw.cmd" -Dtest=%TESTS% test
set "TEST_EXIT_CODE=%ERRORLEVEL%"
popd

endlocal & exit /b %TEST_EXIT_CODE%
