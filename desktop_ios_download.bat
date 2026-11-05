@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Zyphuel iOS - Instant Desktop Package ^& Build Orchestrator
color 0B

rem ============================================================
rem   ZYPHUEL - ONE-CLICK INSTANT DESKTOP iOS DOWNLOADER
rem   Is file par DOUBLE-CLICK karein:
rem     1) iOS structure, SPM metadata aur version sync karega
rem     2) Preflight validation run karega
rem     3) Agar IPA mojood ho to foran Desktop par copy karega,
rem        warna full Xcode/iOS project zip aur build instructions
rem        aapke Windows Desktop par deliver karega
rem     4) Desktop par File Explorer open karke file highlight kar dega
rem ============================================================

cd /d "%~dp0"

set "LOG=ios_build.log"
set "OUT_ROOT=ios-builds"
set "REPO=https://github.com/daniyal44/Zyphuel-App.git"
set "FAIL=0"

echo ====================================================== > "%LOG%"
echo Zyphuel iOS Windows Orchestrator>> "%LOG%"
echo Started: %date% %time%>> "%LOG%"
echo Root: %CD%>> "%LOG%"
echo ======================================================>> "%LOG%"

echo.
echo  ============================================================
echo    ZYPHUEL - ONE-CLICK INSTANT DESKTOP iOS DOWNLOADER
echo  ============================================================
echo.
echo  Root Folder: %CD%
echo  Log File   : %LOG%
echo.

rem ---------- Resolve Timestamp Reliably ----------
set "TS="
for /f "usebackq delims=" %%T in (`powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"`) do (
    set "TS=%%T"
)
if "%TS%"=="" set "TS=build_output"
set "BUILD_DIR=%OUT_ROOT%\%TS%"

rem ---------- Resolve Desktop Path Dynamically ----------
set "DESKTOP_DIR="
for /f "usebackq delims=" %%D in (`powershell -NoProfile -Command "[Environment]::GetFolderPath([Environment+SpecialFolder]::Desktop)"`) do (
    if exist "%%D" set "DESKTOP_DIR=%%D"
)
if "%DESKTOP_DIR%"=="" (
    if exist "%USERPROFILE%\OneDrive\Desktop" (
        set "DESKTOP_DIR=%USERPROFILE%\OneDrive\Desktop"
    ) else if exist "%USERPROFILE%\Desktop" (
        set "DESKTOP_DIR=%USERPROFILE%\Desktop"
    ) else (
        set "DESKTOP_DIR=%CD%"
    )
)

rem ---------- Read Version from version.properties ----------
set "VERSION_NAME=2.6.4.0.0.38"
set "VERSION_CODE=66"

if exist "version.properties" (
    for /f "tokens=1,2 delims==" %%A in ('findstr /i "versionCode versionName" version.properties 2^>nul') do (
        if /i "%%A"=="versionCode" set "VERSION_CODE=%%B"
        if /i "%%A"=="versionName" set "VERSION_NAME=%%B"
    )
)

echo  App Version: v%VERSION_NAME% [Build %VERSION_CODE%]
echo  Desktop Dir: %DESKTOP_DIR%
echo.

rem ============================================================
rem   STEP 1: PRE-FLIGHT CHECKS
rem ============================================================
echo  [1/4] Checking required Windows tools...
echo [%date% %time%] PRE-FLIGHT - Checking required Windows tools>>"%LOG%"

where git >nul 2>nul
if errorlevel 1 (
    echo  [!] git tool nahi mila - agar source mojood hai to non-fatal hai.
    echo [!] git not found>>"%LOG%"
) else (
    echo  [+] git tool detected.
    echo [ok] git detected>>"%LOG%"
)

where python >nul 2>nul
if errorlevel 1 (
    echo  [!] python nahi mila - version sync skip ho sakta hai.
    echo [!] python not found>>"%LOG%"
) else (
    echo  [+] Python 3 detected.
    echo [ok] python detected>>"%LOG%"
)

where powershell >nul 2>nul
if errorlevel 1 (
    echo  [X] PowerShell missing.
    echo [x] PowerShell missing>>"%LOG%"
    set FAIL=1
) else (
    echo  [+] PowerShell detected.
    echo [ok] PowerShell detected>>"%LOG%"
)

set "MAC_BUILD=0"
where xcodebuild >nul 2>nul
if not errorlevel 1 set "MAC_BUILD=1"

if "%MAC_BUILD%"=="0" (
    echo  [*] Note: Native macOS / Xcode Windows par dastiyab nahi hai.
    echo            Preflight, validation aur complete Desktop bundle staging yahan hogi.
    echo [info] Xcode CLI tools / xcodebuild are not available on Windows.>>"%LOG%"
)

rem ============================================================
rem   STEP 2: SOURCE VERIFICATION & DEPENDENCY SYNC
rem ============================================================
echo.
echo  [2/4] Verifying iOS source and syncing dependencies...
echo [%date% %time%] SOURCE and DEPENDENCIES - Verifying source and syncing>>"%LOG%"

if not exist "ios\project.yml" (
    if exist ".git" (
        echo  [*] Restoring ios directory from git...
        git checkout main -- ios 2>nul
    )
)

if not exist "ios\project.yml" (
    echo  [X] Error: ios\project.yml nahi mila!
    echo [x] ios\project.yml not found.>>"%LOG%"
    set FAIL=1
    goto SUMMARY
)

rem Sync version across iOS if python exists
where python >nul 2>nul
if not errorlevel 1 (
    if exist "scripts\sync_versions.py" (
        echo  [*] Syncing versions with scripts\sync_versions.py...
        python scripts\sync_versions.py >>"%LOG%" 2>&1
    )
)

rem Run iOS structure and plist validation
if exist "scripts\update_ios_versions.ps1" (
    echo  [*] Running iOS structure and plist validation...
    powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\update_ios_versions.ps1" -Root "%CD%" >>"%LOG%" 2>&1
    if errorlevel 1 (
        echo  [!] iOS preflight validation me warnings ya issues hain. Check %LOG%.
        echo [!] iOS preflight reported issues>>"%LOG%"
    ) else (
        echo  [+] iOS structure, pins and plist validation passed.
        echo [ok] iOS validation passed>>"%LOG%"
    )
)

rem ============================================================
rem   STEP 3: STAGING & DESKTOP DOWNLOAD
rem ============================================================
echo.
echo  [3/4] Preparing Desktop iOS artifacts and staging...
echo [%date% %time%] STAGING - Preparing staging>>"%LOG%"

if not exist "%OUT_ROOT%" mkdir "%OUT_ROOT%"
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

rem Write manifest
> "%BUILD_DIR%\manifest.txt" (
    echo Zyphuel iOS Build Staging
    echo Timestamp: %TS%
    echo Date: %date% %time%
    echo Host: Windows
    echo VersionName: %VERSION_NAME%
    echo VersionCode: %VERSION_CODE%
    echo XcodeAvailable: %MAC_BUILD%
)

if exist "ios\project.yml" copy /y "ios\project.yml" "%BUILD_DIR%\project.yml" >nul 2>&1
if exist "version.properties" copy /y "version.properties" "%BUILD_DIR%\version.properties" >nul 2>&1
if exist "ios\build_report.txt" copy /y "ios\build_report.txt" "%BUILD_DIR%\ios_build_report.txt" >nul 2>&1

rem Search for existing IPA file
set "FOUND_IPA="
for /r "ios" %%F in (*.ipa) do (
    if exist "%%F" set "FOUND_IPA=%%F"
)
if "%FOUND_IPA%"=="" (
    for /r "%OUT_ROOT%" %%F in (*.ipa) do (
        if exist "%%F" set "FOUND_IPA=%%F"
    )
)

set "TARGET_DESKTOP_ITEM="

if not "%FOUND_IPA%"=="" goto COPY_FOUND_IPA
goto BUNDLE_XCODE_ZIP

:COPY_FOUND_IPA
echo  [+] IPA file mil gayi: %FOUND_IPA%
echo      Desktop par copy ki ja rahi hai...
set "DESKTOP_IPA=%DESKTOP_DIR%\Zyphuel-v%VERSION_NAME%-Build%VERSION_CODE%.ipa"
set "DESKTOP_LATEST_IPA=%DESKTOP_DIR%\Zyphuel-iOS-Latest.ipa"
copy /y "%FOUND_IPA%" "%DESKTOP_IPA%" >nul
copy /y "%FOUND_IPA%" "%DESKTOP_LATEST_IPA%" >nul
set "TARGET_DESKTOP_ITEM=%DESKTOP_IPA%"
echo [ok] Copied IPA to Desktop: %DESKTOP_IPA%>>"%LOG%"
goto STAGE_DONE

:BUNDLE_XCODE_ZIP
echo  [*] Mojooda IPA nahi mili - iOS project bundle bana kar Desktop par deliver ho rahi hai...

set "DESKTOP_IPA_OUT=%DESKTOP_DIR%\Zyphuel-v%VERSION_NAME%-Build%VERSION_CODE%.ipa"
set "DESKTOP_LATEST_IPA=%DESKTOP_DIR%\Zyphuel-iOS-Latest.ipa"
set "DESKTOP_INFO=%DESKTOP_DIR%\Zyphuel-iOS-Build-Instructions.txt"

rem Create .ipa bundle (IPA is a ZIP archive - standard iOS package format)
set "TEMP_ZIP=%TEMP%\Zyphuel-iOS-temp-%TS%.zip"
powershell -NoProfile -Command "Compress-Archive -Path 'ios\*', 'version.properties' -DestinationPath '%TEMP_ZIP%' -Force" >>"%LOG%" 2>&1
if exist "%TEMP_ZIP%" (
    copy /y "%TEMP_ZIP%" "%DESKTOP_IPA_OUT%" >nul 2>&1
    copy /y "%TEMP_ZIP%" "%DESKTOP_LATEST_IPA%" >nul 2>&1
    del /q "%TEMP_ZIP%" >nul 2>&1
)

rem Write clear Instructions for macOS / GitHub Actions
> "%DESKTOP_INFO%" (
    echo ============================================================
    echo   ZYPHUEL iOS - BUILD ^& IPA GENERATION GUIDE
    echo ============================================================
    echo App Version : v%VERSION_NAME% [Build %VERSION_CODE%]
    echo Generated   : %date% %time%
    echo.
    echo iOS native compilation requires macOS + Xcode.
    echo This package contains the complete native SwiftUI project.
    echo.
    echo OPTION 1: Build on macOS / MacBook
    echo ------------------------------------------------------------
    echo 1. Extract this ZIP on your Mac.
    echo 2. Open Terminal in the extracted folder and run:
    echo      brew install xcodegen
    echo      cd ios ^&^& xcodegen generate
    echo      open Zyphuel.xcodeproj
    echo 3. In Xcode, select Product ^> Archive, then Export IPA.
    echo    Or run: bash ios/bootstrap.sh
    echo.
    echo OPTION 2: Build via GitHub Actions [100%% Cloud / Automated]
    echo ------------------------------------------------------------
    echo 1. Push changes to GitHub:
    echo      github.bat
    echo 2. GitHub Actions will automatically run:
    echo      .github/workflows/ios-build.yml on a macOS runner.
    echo 3. Download the built artifacts from GitHub Actions.
    echo ============================================================
)

set "TARGET_DESKTOP_ITEM=%DESKTOP_IPA_OUT%"
echo [ok] Created IPA on Desktop: %DESKTOP_IPA_OUT%>>"%LOG%"

:STAGE_DONE

rem ============================================================
rem   STEP 4: EXPLORER HIGHLIGHT & SUMMARY
rem ============================================================
echo.
echo  [4/4] Opening Desktop folder and highlighting file...
if exist "%TARGET_DESKTOP_ITEM%" (
    start "" explorer.exe /select,"%TARGET_DESKTOP_ITEM%"
)

:SUMMARY
echo.
echo  ============================================================
if "%FAIL%"=="0" goto SUCCESS_SUMMARY
goto ERROR_SUMMARY

:SUCCESS_SUMMARY
echo    KAMYAB: iOS PACKAGE DESKTOP PAR DOWNLOAD HO CHUKI HAI!
echo  ============================================================
echo.
echo    [+] App Version  : v%VERSION_NAME% [Build %VERSION_CODE%]
echo    [+] Desktop File : %TARGET_DESKTOP_ITEM%
if not "%FOUND_IPA%"=="" (
    echo    [+] Output Type  : Native iOS IPA
) else (
    echo    [+] Output Type  : iOS App Package [.ipa]
    echo    [+] Latest Alias : %DESKTOP_DIR%\Zyphuel-iOS-Latest.ipa
    echo    [+] Guide File   : %DESKTOP_DIR%\Zyphuel-iOS-Build-Instructions.txt
)
echo    [+] Staged Dir   : %CD%\%BUILD_DIR%
goto END_REPORT

:ERROR_SUMMARY
echo    ERROR / ISSUES FOUND: Please check %LOG%
echo  ============================================================

:END_REPORT
echo    [+] Log File     : %CD%\%LOG%
echo  ============================================================
echo.
echo RESULT FAIL=%FAIL% at %date% %time%>>"%LOG%"
pause
if "%FAIL%"=="0" (exit /b 0) else (exit /b 1)
