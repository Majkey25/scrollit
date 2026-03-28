@echo off
where gradle >NUL 2>&1
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)

echo Gradle is not installed. Install Gradle 8.7+ or use Android Studio. 1>&2
exit /b 1
