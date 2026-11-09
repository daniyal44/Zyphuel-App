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

rem ---------- Java & Environment Setup (auto-detect any installed JDK) ----------
if not defined JAVA_HOME (
    for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk-*-hotspot") do (
        if exist "%%J\bin\java.exe" set "JAVA_HOME=%%J"
    )
)
if not defined JAVA_HOME (
    for /d %%J in ("C:\Program Files\Java\jdk-*") do (
        if exist "%%J\bin\java.exe" set "JAVA_HOME=%%J"
    )
)
if not defined JAVA_HOME (
    for /d %%J in ("C:\Program Files\Microsoft\jdk-*") do (
        if exist "%%J\bin\java.exe" set "JAVA_HOME=%%J"
    )
)
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin" (
        set "PATH=%JAVA_HOME%\bin;%PATH%"
        echo  [+] JDK: %JAVA_HOME%
    )
) else (
    echo  [i] JAVA_HOME set nahi hai - Gradle wrapper apna JDK dhundh lega.
)

rem ---------- Gradle Setup (always prefer the project wrapper) ----------
set "GRADLE_CMD="
if exist "%~dp0gradlew.bat" (
    set "GRADLE_CMD=%~dp0gradlew.bat"
    echo  [+] Gradle: project wrapper (gradlew.bat)
) else (
    rem Search for any installed Gradle version dynamically
    for /d %%G in ("%USERPROFILE%\.gradle\wrapper\dists\gradle-*-bin\*\gradle-*") do (
        if exist "%%G\bin\gradle.bat" (
            set "GRADLE_CMD=%%G\bin\gradle.bat"
        )
    )
    if defined GRADLE_CMD (
        echo  [+] Gradle: system install (!GRADLE_CMD!)
    ) else (
        echo  [!] Gradle nahi mili - build skip hoga, existing APK use karega.
    )
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

rem ---------- Read Version from version.properties (shared with iOS) ----------
rem Fallback values are only used if version.properties is missing.
set "VERSION_NAME=unknown"
set "VERSION_CODE=0"

if exist "version.properties" (
    for /f "tokens=1,2 delims==" %%A in ('findstr /i "versionCode versionName" version.properties') do (
        if /i "%%A"=="versionCode" set "VERSION_CODE=%%B"
        if /i "%%A"=="versionName" set "VERSION_NAME=%%B"
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
    call "%GRADLE_CMD%" assembleDebug --daemon --console=plain
    if errorlevel 1 (
        echo.
        echo  [!] Gradle build me masla aya, mojooda APK check kar rahe hain...
        if not exist "%SRC_APK%" (
            echo  [!] Local APK nahi mili, GitHub Actions se download try...
            goto GH_FALLBACK
        )
        echo  [*] Mojooda APK mil gayi hai, isko Desktop par copy kiya ja raha hai.
    )
) else (
    echo  [!] Gradle command nahi mili, existing APK ya GitHub Actions try...
    if not exist "%SRC_APK%" goto GH_FALLBACK
)
goto COPY_ONLY

:GH_FALLBACK
rem Try downloading latest APK from GitHub Actions
where gh >nul 2>nul
if errorlevel 1 goto NO_APK
gh auth status >nul 2>nul
if errorlevel 1 goto NO_APK
echo  [*] GitHub Actions se latest APK download kar rahe hain...
set "GH_DL_DIR=%TEMP%\zyphuel-apk-dl"
if exist "%GH_DL_DIR%" rd /s /q "%GH_DL_DIR%"
gh run download --repo daniyal44/Zyphuel-App --name Zyphuel-android-debug-apk --dir "%GH_DL_DIR%" >nul 2>&1
if exist "%GH_DL_DIR%" (
    for /r "%GH_DL_DIR%" %%F in (*.apk) do (
        if exist "%%F" (
            set "SRC_APK=%%F"
            echo  [+] GitHub Actions APK mil gayi: %%F
            goto COPY_ONLY
        )
    )
)
:NO_APK
echo  [X] Koi APK nahi mili - pehle build karein ya GitHub Actions run karein.
pause
exit /b 1

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
