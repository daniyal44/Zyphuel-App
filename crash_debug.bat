@echo off
echo ============================================
echo  ZYPHUEL APP CRASH DEBUGGER v2.0
echo  Diagnoses instant-close crash on launch
echo ============================================
echo.
echo Make sure:
echo  1. Your phone is connected via USB
echo  2. USB Debugging is enabled
echo  3. ADB is installed (comes with Android Studio)
echo.

REM Check ADB is available
adb devices >nul 2>&1
if errorlevel 1 (
    echo ERROR: ADB not found! Add Android SDK platform-tools to your PATH.
    pause
    exit /b 1
)

echo Connected devices:
adb devices
echo.

echo ============================================
echo  OPTION 1: Capture crash log
echo  OPTION 2: Clear app data (fixes corrupt DB)
echo  OPTION 3: Both (clear + capture fresh crash)
echo ============================================
echo.
set /p CHOICE="Enter choice (1/2/3): "

if "%CHOICE%"=="2" goto CLEAR_DATA
if "%CHOICE%"=="3" goto CLEAR_AND_CAPTURE

:CAPTURE
echo.
echo [Step 1] Pulling internal crash_log.txt from app...
adb shell run-as com.aistudio.zyphuel.appv2 cat files/crash_log.txt > app_crash_log.txt 2>&1
if exist app_crash_log.txt (
    echo  Internal crash log saved to: app_crash_log.txt
    echo  --- Content Preview ---
    type app_crash_log.txt
    echo  --- End Preview ---
) else (
    echo  No internal crash log found.
)

echo.
echo [Step 2] Clearing logcat buffer...
adb logcat -c

echo [Step 3] Starting logcat capture (will auto-stop in 30 seconds)...
echo          NOW OPEN the Zyphuel app on your phone!
echo.

REM Capture logcat for 30 seconds focusing on crash-related output
start /b cmd /c "timeout /t 30 >nul & taskkill /f /im adb.exe >nul 2>&1"
adb logcat *:E -v threadtime 2>nul | findstr /i "FATAL AndroidRuntime zyphuel example crash MainViewModel Room Database Migration Firebase" > crash_output.txt 2>&1

echo.
echo ============================================
echo  Crash output saved to: crash_output.txt
echo  Internal log saved to: app_crash_log.txt
echo.
echo  SHARE BOTH FILES WITH CLAUDE!
echo ============================================
goto END

:CLEAR_DATA
echo.
echo [Clearing all app data...]
adb shell pm clear com.aistudio.zyphuel.appv2
echo  App data cleared! The app should start fresh now.
echo  Open the app and check if it works.
if "%CHOICE%"=="3" goto CAPTURE
goto END

:CLEAR_AND_CAPTURE
goto CLEAR_DATA

:END
echo.
pause
