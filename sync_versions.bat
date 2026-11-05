@echo off
setlocal EnableExtensions
title Zyphuel Version Sync
cd /d "%~dp0"

where python >nul 2>nul
if errorlevel 1 (
  echo [x] Python 3 was not found. Install Python, then re-run this file.
  exit /b 1
)

if /I "%~1"=="--check" goto :check
if /I "%~1"=="--bump" goto :bump

echo Checking versions across Android, iOS and documentation...
python scripts\sync_versions.py
goto :end

:check
python scripts\sync_versions.py --check
goto :end

:bump
echo Bumping Android AND iOS to the same next version...
python scripts\sync_versions.py --bump
goto :end

:end
echo.
pause
endlocal
