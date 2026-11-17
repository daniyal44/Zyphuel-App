@echo off
echo ============================================
echo  ZYPHUEL CRASH FIX
echo  Clears corrupted app data and restarts
echo ============================================
echo.
echo This will clear all local app data (login, settings, cached prices).
echo Your Firestore cloud data (orders, account) is safe.
echo.
echo Make sure your phone is connected via USB with USB Debugging ON.
echo.
pause

echo.
echo [1/3] Clearing app data...
adb shell pm clear com.aistudio.zyphuel.appv2
echo.
echo [2/3] Starting app...
adb shell am start -n com.aistudio.zyphuel.appv2/.MainActivity
echo.
echo [3/3] Done! The app should open fresh now.
echo.
echo If it STILL crashes, you need to:
echo   1. Uninstall the app completely
echo   2. Rebuild: ./gradlew assembleDebug
echo   3. Install the NEW APK
echo.
pause
