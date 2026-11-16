@echo off
echo ============================================
echo  ZYPHUEL APP CRASH DEBUGGER
echo  This captures the crash log from your phone
echo ============================================
echo.
echo Make sure:
echo  1. Your phone is connected via USB
echo  2. USB Debugging is enabled
echo  3. ADB is installed (comes with Android Studio)
echo.
echo Press any key to start capturing...
pause >nul

echo.
echo [Step 1] Clearing old logs...
adb logcat -c

echo [Step 2] Now OPEN the Zyphuel app on your phone.
echo          Waiting for crash...
echo          (Press Ctrl+C when the app crashes)
echo.

adb logcat *:E -v time | findstr /i "FATAL AndroidRuntime zyphuel example crash MainViewModel" > crash_output.txt 2>&1

echo.
echo ============================================
echo  Crash log saved to: crash_output.txt
echo  Also checking for app's internal crash log...
echo ============================================

adb shell run-as com.aistudio.zyphuel.appv2 cat files/crash_log.txt > app_crash_log.txt 2>&1
if exist app_crash_log.txt (
    echo  App crash log saved to: app_crash_log.txt
) else (
    echo  No internal crash log found
)

echo.
echo  SHARE BOTH FILES WITH CLAUDE:
echo   - crash_output.txt
echo   - app_crash_log.txt
echo ============================================
pause
