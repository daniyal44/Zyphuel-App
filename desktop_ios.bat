@echo off
setlocal EnableExtensions
title Zyphuel iOS Desktop Tool (wrapper)
cd /d "%~dp0"
rem Full Windows preflight/fetch/staging orchestrator (IPA export requires macOS/CI).
if exist "desktop_ios_download.bat" (
  call "desktop_ios_download.bat" %*
  exit /b %errorlevel%
)
echo [x] desktop_ios_download.bat not found.
exit /b 1
