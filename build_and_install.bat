@echo off
echo ============================================
echo  ZYPHUEL BUILD ^& INSTALL v1.0
echo  One-click: Build Debug APK ^& Install
echo ============================================
echo.

REM Step 1: Delete any stuck git lock file
if exist ".git\index.lock" (
    echo [1/4] Removing stuck .git/index.lock...
    del /f ".git\index.lock"
    echo  Done.
) else (
    echo [1/4] No .git/index.lock found. OK.
)
echo.

REM Step 2: Clean and build
echo [2/4] Building Debug APK (this may take 2-5 minutes)...
echo  Please wait...
call gradlew.bat :app:assembleDebug
if errorlevel 1 (
    echo.
    echo ============================================
    echo  BUILD FAILED! Check errors above.
    echo ============================================
    pause
    exit /b 1
)
echo.
echo  Build successful!
echo.

REM Step 3: Find the APK
set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
if not exist "%APK_PATH%" (
    echo ERROR: APK not found at %APK_PATH%
    pause
    exit /b 1
)
echo [3/4] APK ready: %APK_PATH%
echo.

REM Step 4: Install via ADB (if device connected)
echo [4/4] Installing on connected device...
adb devices 2>nul | findstr /r "device$" >nul 2>&1
if errorlevel 1 (
    echo  No device detected via ADB.
    echo  APK is ready at: %APK_PATH%
    echo  Install manually: adb install -r %APK_PATH%
) else (
    adb install -r "%APK_PATH%"
    if errorlevel 1 (
        echo  Install failed. Trying with -t flag...
        adb install -r -t "%APK_PATH%"
    )
    echo.
    echo  Starting app...
    adb shell am start -n com.aistudio.zyphuel.appv2/.MainActivity
    echo.
    echo ============================================
    echo  DONE! App should be open on your phone.
    echo ============================================
)
echo.
pause
