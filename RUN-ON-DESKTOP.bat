@echo off
setlocal enabledelayedexpansion
title Zyphuel - Run on Desktop Emulator
color 0A

rem ============================================================
rem   ZYPHUEL - One-click Run on Desktop (Android Emulator)
rem   Is file par DOUBLE-CLICK karein:
rem     1) Android Emulator (AVD: zyphuel) start karega
rem     2) Emulator boot hone ka intezaar karega
rem     3) Zyphuel APK install karega
rem     4) App ko desktop screen par launch kar dega
rem ============================================================

cd /d "D:\Games\New folder-web\Claude"

set "SDK=C:\Users\mdani\AppData\Local\Android\Sdk"
set "EMULATOR=%SDK%\emulator\emulator.exe"
set "ADB=%SDK%\platform-tools\adb.exe"
set "APK=D:\Games\New folder-web\Claude\app\build\outputs\apk\debug\app-debug.apk"
set "PKG=com.aistudio.zyphuel.appv2"
set "AVD_NAME=zyphuel"

echo.
echo  ============================================================
echo    ZYPHUEL - Launching App on Desktop Screen
echo  ============================================================
echo.

rem ---------- Sanity checks ----------
if not exist "%EMULATOR%" (
    echo  [X] Android Emulator nahi mila yahan:
    echo      %EMULATOR%
    pause
    exit /b 1
)

if not exist "%ADB%" (
    echo  [X] ADB nahi mila yahan:
    echo      %ADB%
    pause
    exit /b 1
)

if not exist "%APK%" (
    echo  [!] APK file mojood nahi hai:
    echo      %APK%
    echo      Pehle "INSTALL-TO-PHONE.bat" chala kar build kar lein.
    pause
    exit /b 1
)

rem ---------- Step 1: Check if Emulator is already running ----------
echo  [1/4] Checking active emulator...
"%ADB%" start-server >nul 2>&1

set "EMU_RUNNING=0"
for /f "tokens=1,2" %%A in ('"%ADB%" devices') do (
    echo %%A | findstr /i "emulator" >nul
    if not errorlevel 1 (
        if "%%B"=="device" (
            set "EMU_RUNNING=1"
            set "TARGET_DEVICE=%%A"
        )
    )
)

if "%EMU_RUNNING%"=="1" (
    echo  [OK] Emulator pehle se chal raha hai: !TARGET_DEVICE!
    goto INSTALL_APP
)

rem ---------- Step 2: Launch Emulator ----------
echo  [2/4] Starting Android Emulator (%AVD_NAME%)...
echo        (Desktop par phone window open ho rahi hai, intezaar karein...)
start "" "%EMULATOR%" -avd %AVD_NAME% -gpu host

rem ---------- Step 3: Wait for boot ----------
echo  [3/4] Emulator ke boot hone ka intezaar ho raha hai...
"%ADB%" wait-for-device

:WAIT_BOOT
timeout /t 3 >nul
for /f "tokens=*" %%A in ('"%ADB%" shell getprop sys.boot_completed 2^>nul') do set "BOOT_STATUS=%%A"
set "BOOT_STATUS=!BOOT_STATUS:~0,1!"
if not "!BOOT_STATUS!"=="1" (
    echo        Booting in progress...
    goto WAIT_BOOT
)

echo  [OK] Emulator mukammal boot ho chuka hai!

:INSTALL_APP
rem ---------- Step 4: Install and Launch APK ----------
echo  [4/4] Zyphuel APK install ho rahi hai...
"%ADB%" install -r -d -t "%APK%"
if errorlevel 1 (
    echo  [!] Direct install fail hua, fresh reinstall kar rahe hain...
    "%ADB%" uninstall %PKG% >nul 2>&1
    "%ADB%" install -r -d -t "%APK%"
)

echo  [OK] Launching Zyphuel App on Desktop...
"%ADB%" shell am start -n %PKG%/com.example.MainActivity >nul 2>&1

echo.
echo  ============================================================
echo    Kamyabi! Zyphuel App aapke desktop par chal rahi hai.
echo  ============================================================
echo.
pause
