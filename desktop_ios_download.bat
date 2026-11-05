@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Zyphuel iOS - Fetch, Preflight, Build Orchestrator (Windows)
cd /d "%~dp0"

rem ===== Configuration =====
set "LOG=ios_build.log"
set "OUT_ROOT=ios-builds"
set "TS=%date:~-4%%date:~4,2%%date:~7,2%_%time:~0,2%%time:~3,2%%time:~6,2%"
set "TS=!TS: =0!"
set "BUILD_DIR=%OUT_ROOT%\%TS%"
set "REPO=https://github.com/daniyal44/Zyphuel-App.git"
set "FAIL=0"

echo ====================================================== > "%LOG%"
echo Zyphuel iOS Windows Orchestrator>> "%LOG%"
echo Started: %date% %time%>> "%LOG%"
echo Root: %CD%>> "%LOG%"
echo Output: %BUILD_DIR%>> "%LOG%"
echo ======================================================>> "%LOG%"

echo.
echo  Zyphuel iOS - desktop_ios_download.bat
echo  Windows preflight + source/dependency refresh + build staging
echo  Log: %LOG%
echo.

call :step "PRE-FLIGHT" "Checking required Windows tools"
where git >nul 2>nul || (echo [x] git not found>>"%LOG%" & echo [x] git is required.& set FAIL=1)
where python >nul 2>nul || (echo [x] python not found>>"%LOG%" & echo [x] Python 3 is required.& set FAIL=1)
if exist "%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" (echo [ok] PowerShell present>>"%LOG%") else (echo [!] PowerShell missing>>"%LOG%")

rem Xcode toolchain cannot run on Windows - detect and report honestly
set "MAC_BUILD=0"
where xcodebuild >nul 2>nul && set "MAC_BUILD=1"
if "%MAC_BUILD%"=="0" (
  echo [info] Xcode CLI tools / xcodebuild are NOT available on Windows.>>"%LOG%"
  echo [info] IPA build+export requires macOS + Xcode + Apple signing. This script will preflight, fetch, validate, and stage outputs.>>"%LOG%"
  echo.
  echo  NOTE: Xcode/IPA build cannot run on native Windows.
  echo        Preflight/fetch/validation/staging will run here.
  echo        For an IPA, run on macOS:  bash ios/bootstrap.sh
  echo        or trigger .github/workflows/ios-build.yml / a remote Mac.
  echo.
)

call :step "SOURCE FETCH" "Ensuring source is present and up to date"
if exist ".git" (
  echo [ok] Git repository detected.>>"%LOG%"
  echo Pulling latest from origin (non-fatal if offline/auth required)...
  git --no-pager fetch --all --prune >>"%LOG%" 2>&1
  git --no-pager pull --ff-only >>"%LOG%" 2>&1
  if errorlevel 1 echo [!] git pull did not fast-forward; working tree preserved.>>"%LOG%"
) else (
  if exist "ios\project.yml" (
    echo [ok] ios/ source present without .git in this folder.>>"%LOG%"
  ) else (
    echo Source not found. Cloning %REPO% ...
    git clone "%REPO%" zyphuel-src >>"%LOG%" 2>&1
    if errorlevel 1 (echo [x] Clone failed. See %LOG% & set FAIL=1) else (cd /d zyphuel-src)
  )
)

if not exist "ios\project.yml" (
  echo [x] ios\project.yml not found. Cannot continue.>>"%LOG%"
  echo [x] iOS project source missing.
  set FAIL=1
  goto :summary
)

call :step "DEPENDENCIES" "Refreshing iOS dependency pins (SPM metadata) and version sync"
rem SPM resolution (Package.resolved) and CocoaPods require macOS/Xcode/SwiftPM.
rem On Windows we refresh the latest package pins in project.yml, validate plists, and sync versions.
python scripts\sync_versions.py >>"%LOG%" 2>&1
if errorlevel 1 echo [!] Version sync reported issues - review log.>>"%LOG%"
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\update_ios_versions.ps1" -Root "%CD%" >>"%LOG%" 2>&1
if errorlevel 1 (
  echo [x] iOS preflight validation failed.>>"%LOG%"
  set FAIL=1
) else (
  echo [ok] iOS structure/pins/plist validation passed.>>"%LOG%"
)

call :step "STAGING" "Creating timestamped build output folder"
if not exist "%OUT_ROOT%" mkdir "%OUT_ROOT%"
mkdir "%BUILD_DIR%" >>"%LOG%" 2>&1
echo Build staging directory: %BUILD_DIR%>>"%LOG%"

rem Write a manifest for traceability
> "%BUILD_DIR%\manifest.txt" (
  echo Zyphuel iOS build staging
  echo Timestamp: %date% %time%
  echo Host: Windows
  echo Source: %CD%
  echo XcodeAvailable: %MAC_BUILD%
  for /f "tokens=1,2 delims==" %%A in ('findstr /i "versionCode versionName" version.properties 2^nul') do echo %%A=%%B
)

copy /y "ios\project.yml" "%BUILD_DIR%\project.yml" >>"%LOG%" 2>&1
copy /y "version.properties" "%BUILD_DIR%\version.properties" >>"%LOG%" 2>&1
if exist "ios\build_report.txt" copy /y "ios\build_report.txt" "%BUILD_DIR%\ios_build_report.txt" >>"%LOG%" 2>&1

call :step "BUILD" "Building / exporting IPA"
if "%MAC_BUILD%"=="1" (
  echo Running Xcode build...>>"%LOG%"
  if exist "ios\bootstrap.sh" (
    bash ios/bootstrap.sh >>"%LOG%" 2>&1
  ) else (
    echo [x] ios/bootstrap.sh missing.>>"%LOG%" & set FAIL=1
  )
  rem Copy any produced IPA/xcarchive if present
  for /r "ios" %%F in (*.ipa) do copy /y "%%F" "%BUILD_DIR%\" >>"%LOG%" 2>&1
  for /r "ios" %%F in (*.xcarchive) do echo Found archive: %%F>>"%LOG%"
) else (
  echo [info] Skipping xcodebuild/IPA export - macOS required.>>"%LOG%"
  echo [info] To produce a timestamped IPA:>>"%LOG%"
  echo [info]   1) On macOS: bash ios/bootstrap.sh>>"%LOG%"
  echo [info]   2) Archive + export via Xcode Organizer or xcodebuild -exportArchive (requires signing)>>"%LOG%"
  echo [info]   3) Copy the .ipa to: %CD%\%BUILD_DIR%\>>"%LOG%"
  echo [info]   4) Or run GitHub Actions workflow .github/workflows/ios-build.yml>>"%LOG%"
  > "%BUILD_DIR%\BUILD_ON_MACOS_REQUIRED.txt" (
    echo IPA export requires macOS + Xcode + Apple Developer signing.
    echo.
    echo macOS commands:
    echo   brew install xcodegen
    echo   cd ios ^&^& xcodegen generate
    echo   open Zyphuel.xcodeproj   # or use xcodebuild archive/export
    echo.
    echo CI: trigger .github/workflows/ios-build.yml
    echo Place resulting IPA in this folder.
  )
)

:summary
echo.
echo ======================================================
if "%FAIL%"=="0" (
  echo  RESULT: PASS (Windows preflight/staging complete)
  echo  Staged: %CD%\%BUILD_DIR%
  if "%MAC_BUILD%"=="0" echo  IPA: build on macOS or CI, then place in %BUILD_DIR%
) else (
  echo  RESULT: ISSUES FOUND - see %LOG%
)
echo  Log: %CD%\%LOG%
echo ======================================================
echo RESULT FAIL=%FAIL% at %date% %time%>>"%LOG%"
if "%FAIL%"=="0" (exit /b 0) else (exit /b 1)

:step
echo.
echo --- %~1: %~2 ---
echo [%date% %time%] %~1 - %~2>>"%LOG%"
exit /b 0
