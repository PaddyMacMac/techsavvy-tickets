@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0azure-run-bicep.ps1" %*
exit /b %ERRORLEVEL%
