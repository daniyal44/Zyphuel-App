@echo off
setlocal enabledelayedexpansion
title Zyphuel - Instant Desktop APK Downloader
color 0A

rem ============================================================
rem   ZYPHUEL - ONE-CLICK INSTANT DESKTOP APK DOWNLOADER
rem   Is file par DOUBLE-CLICK karein:
rem     1) Zyphuel Android APK ko compile/build karega
rem     2) APK ko FORN aapke Windows Desktop par copy/download karega
rem     3) Desktop par File Explorer open karke APK highlight kar dega
rem ============================================================

cd /d "%~dp0"

echo.
echo  ============================================================
echo    ZYPHUEL - ONE-CLICK INSTANT DESKTOP APK DOWNLOADER
echo  ============================================================
echo.

rem ---------- Java & Environment Setup ----------
if not defined JAVA_HOME (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot"
    )
)
if exist "%JAVA_HOME%\bin" (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

rem ---------- Gradle Setup ----------
set "GRADLE_CMD="
if exist "%~dp0gradlew.bat" (
    set "GRADLE_CMD=%~dp0gradlew.bat"
) else if exist "C:\Users\mdani\.gradle\wrapper\dists\gradle-9.5.0-bin\bvnork1r7n8i6kp5cnkibsc9q\gradle-9.5.0\bin\gradle.bat" (
    set "GRADLE_CMD=C:\Users\mdani\.gradle\wrapper\dists\gradle-9.5.0-bin\bvnork1r7n8i6kp5cnkibsc9q\gradle-9.5.0\bin\gradle.bat"
)

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

rem ---------- Read Version from build.gradle.kts ----------
set "VERSION_NAME=2.6.4.0.0.31"
set "VERSION_CODE=59"

if exist "app\build.gradle.kts" (
    for /f "tokens=3 delims= " %%A in ('findstr /i "versionCode" app\build.gradle.kts') do (
        set "VERSION_CODE=%%A"
    )
    for /f "tokens=2 delims==" %%A in ('findstr /i "versionName" app\build.gradle.kts') do (
        set "TMP_VN=%%A"
        set "TMP_VN=!TMP_VN: =!"
        set "TMP_VN=!TMP_VN:"=!"
        set "VERSION_NAME=!TMP_VN!"
    )
)

set "SRC_APK=%~dp0app\build\outputs\apk\debug\app-debug.apk"
set "OUT_VERSIONED=%DESKTOP_DIR%\Zyphuel-v%VERSION_NAME%-Build%VERSION_CODE%.apk"
set "OUT_LATEST=%DESKTOP_DIR%\Zyphuel-Latest.apk"

rem ---------- Check Quick Flag ----------
if /i "%1"=="--fast" goto COPY_ONLY
if /i "%1"=="-f" goto COPY_ONLY
if /i "%1"=="--quick" goto COPY_ONLY
if /i "%1"=="-q" goto COPY_ONLY

rem ---------- Step 1: Build APK ----------
echo  [1/3] Building latest Zyphuel APK (assembleDebug)...
echo        Version: v%VERSION_NAME% (Build %VERSION_CODE%)
echo        Intezaar karein (Daemon cached build ~3-10 seconds)...
echo.

if not "%GRADLE_CMD%"=="" (
    call "%GRADLE_CMD%" assembleDebug --daemon
    if errorlevel 1 (
        echo.
        echo  [!] Gradle build me masla aya, mojooda APK check kar rahe hain...
        if not exist "%SRC_APK%" (
            echo  [X] Koi pehle se built APK nahi mili: %SRC_APK%
            pause
            exit /b 1
        )
        echo  [*] Mojooda APK mil gayi hai, isko Desktop par copy kiya ja raha hai.
    )
) else (
    echo  [!] Gradle command nahi mili, existing APK use kar rahe hain...
)

:COPY_ONLY
echo.
echo  [2/3] Checking APK file...
if not exist "%SRC_APK%" (
    echo  [X] Error: APK file mojood nahi hai:
    echo      %SRC_APK%
    echo      Pehle app build karein.
    pause
    exit /b 1
)

rem ---------- Step 2: Copy to Desktop ----------
echo.
echo  [3/3] Copying APK to Desktop...
echo        Desktop Location: %DESKTOP_DIR%

copy /y "%SRC_APK%" "%OUT_VERSIONED%" >nul
if errorlevel 1 (
    powershell -NoProfile -Command "Copy-Item -LiteralPath '%SRC_APK%' -Destination '%OUT_VERSIONED%' -Force"
)

copy /y "%SRC_APK%" "%OUT_LATEST%" >nul
if errorlevel 1 (
    powershell -NoProfile -Command "Copy-Item -LiteralPath '%SRC_APK%' -Destination '%OUT_LATEST%' -Force"
)

rem ---------- Step 3: Get File Info ----------
set "FILE_SIZE="
if exist "%OUT_VERSIONED%" (
    for /f "usebackq delims=" %%S in (`powershell -NoProfile -Command "$len = (Get-Item -LiteralPath '%OUT_VERSIONED%').Length; [string]::Format('{0:N2} MB ({1:N0} bytes)', ($len / 1MB), $len)"`) do (
        set "FILE_SIZE=%%S"
    )
)

rem ---------- Step 4: Open File in Explorer ----------
start "" explorer.exe /select,"%OUT_VERSIONED%"

rem ---------- Success Banner ----------
echo.
echo  ============================================================
echo    KAMYAB: APK FORN DESKTOP PAR DOWNLOAD HO CHUKI HAI.
echo  ============================================================
echo.
echo    [+] App Version : v%VERSION_NAME% (Build %VERSION_CODE%)
echo    [+] Desktop APK : %OUT_VERSIONED%
echo    [+] Latest Alias: %OUT_LATEST%
echo    [+] File Size   : %FILE_SIZE%
echo    [+] File Status : Desktop folder khul kar APK highlight ho chuki hai.
echo.
echo    Aap is APK ko apne phone me send karke install kar sakte
echo    hain ya direct WhatsApp / Google Drive par share kar sakte hain.
echo  ============================================================
echo.
pause
