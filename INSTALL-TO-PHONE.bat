@echo off
setlocal enabledelayedexpansion
title Zyphuel - Build and Install to Phone
color 0B

rem ============================================================
rem   ZYPHUEL - One-click build + install to Android phone
rem   Bas is file par DOUBLE-CLICK karein. Kuch type karne ki
rem   zaroorat nahi. Ye script khud:
rem     1) APK build karti hai (cached Gradle 9.5 + JDK 25)
rem     2) Connected phone/emulator dhoondti hai
rem     3) App install karke launch kar deti hai
rem   Poora build log "build-log.txt" me save hota hai.
rem ============================================================

cd /d "D:\Games\New folder-web\Claude"

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot"
set "GRADLE=C:\Users\mdani\.gradle\wrapper\dists\gradle-9.5.0-bin\bvnork1r7n8i6kp5cnkibsc9q\gradle-9.5.0\bin\gradle.bat"
set "ADB=C:\Users\mdani\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set "APK=D:\Games\New folder-web\Claude\app\build\outputs\apk\debug\app-debug.apk"
set "PKG=com.aistudio.zyphuel.appv2"
set "LOG=D:\Games\New folder-web\Claude\build-log.txt"
set "PS=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"

echo.
echo  ============================================================
echo    ZYPHUEL  -  Build ^& Install
echo  ============================================================
echo.
echo   Phone checklist (install ke liye zaroori):
echo     - Phone USB cable se laptop ke saath juda ho
echo     - Settings ^> Developer options ^> USB debugging = ON
echo     - Phone par "Allow USB debugging?" popup aaye to ALLOW dabayein
echo.
echo   (Agar phone connect nahi hai to bhi koi masla nahi -
echo    APK ban jayegi, use manually bhej kar install kar sakte hain.)
echo.

rem ---------- Sanity checks ----------
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo  [X] JDK nahi mila yahan:
    echo      %JAVA_HOME%
    echo      Java install karein ya is script me JAVA_HOME theek karein.
    goto END
)
if not exist "%GRADLE%" (
    echo  [X] Gradle nahi mila yahan:
    echo      %GRADLE%
    goto END
)

rem ---------- STEP 1: BUILD ----------
echo  [1/3] APK build ho rahi hai... ^(2-5 minute lag sakte hain, intezaar karein^)
echo        Live output niche aa raha hai, log: build-log.txt
echo  ------------------------------------------------------------
echo.

call "%GRADLE%" assembleDebug --console=plain 2>&1 | "%PS%" -NoProfile -Command "$input | ForEach-Object { Write-Host $_; $_ } | Out-File -FilePath '%LOG%' -Encoding ascii"

echo.
echo  ------------------------------------------------------------
findstr /C:"BUILD SUCCESSFUL" "%LOG%" >nul 2>&1
if errorlevel 1 goto BUILD_FAILED

echo  [OK] BUILD SUCCESSFUL
if exist "%APK%" (
    for %%F in ("%APK%") do echo       APK: %%~fF  ^(%%~zF bytes^)
) else (
    echo  [!] Build successful magar APK file nahi mili:
    echo      %APK%
    goto END
)
echo.

rem ---------- STEP 2: FIND DEVICES ----------
echo  [2/3] Connected devices check kar rahe hain...
echo.
if not exist "%ADB%" (
    echo  [!] adb nahi mila yahan:
    echo      %ADB%
    goto MANUAL_INSTALL
)

"%ADB%" start-server >nul 2>&1
"%ADB%" devices

set "FOUND=0"
for /f "skip=1 tokens=1,2" %%A in ('""%ADB%" devices"') do (
    if "%%B"=="device" (
        set "FOUND=1"
        echo.
        echo  [3/3] Install kar rahe hain -^> %%A
        "%ADB%" -s %%A install -r "%APK%"
        if errorlevel 1 (
            echo.
            echo  [!] Install fail hua ^(shayad purana signature^). Purani app
            echo      hata kar dobara koshish kar rahe hain...
            "%ADB%" -s %%A uninstall %PKG%
            "%ADB%" -s %%A install -r "%APK%"
            if errorlevel 1 (
                echo  [X] %%A par install phir bhi fail hua. Upar ka message dekhein.
            ) else (
                echo  [OK] %%A par install ho gayi.
                "%ADB%" -s %%A shell monkey -p %PKG% -c android.intent.category.LAUNCHER 1 >nul 2>&1
                echo  [OK] App launch kar di gayi.
            )
        ) else (
            echo  [OK] %%A par install ho gayi.
            "%ADB%" -s %%A shell monkey -p %PKG% -c android.intent.category.LAUNCHER 1 >nul 2>&1
            echo  [OK] App launch kar di gayi.
        )
    )
    if "%%B"=="unauthorized" (
        echo.
        echo  [!] Device %%A "unauthorized" hai.
        echo      Phone ki screen dekhein - "Allow USB debugging?" popup par
        echo      ALLOW dabayein, phir ye script dobara chalayein.
    )
)

if "!FOUND!"=="0" goto MANUAL_INSTALL

echo.
echo  ============================================================
echo    HO GAYA! App aapke phone par install ho chuki hai.
echo  ============================================================
goto END

rem ---------- Manual fallback ----------
:MANUAL_INSTALL
echo.
echo  ============================================================
echo    Koi phone connected nahi mila - lekin APK tayyar hai:
echo.
echo    %APK%
echo.
echo    Phone par daalne ke 2 tareeke:
echo      1^) USB debugging ON karke phone jodein, phir ye script
echo         dobara double-click karein ^(sab khud ho jayega^).
echo      2^) Ye app-debug.apk file phone par bhej dein
echo         ^(USB / WhatsApp / Google Drive^), phone me us file par
echo         tap karein. Agar poochhe to "Unknown sources /
echo         Install unknown apps" ko Allow karein.
echo  ============================================================
goto END

rem ---------- Build failure ----------
:BUILD_FAILED
echo  [X] BUILD FAIL HO GAYA.
echo.
echo  Kotlin/Java errors:
echo  ------------------------------------------------------------
findstr /R /C:"^e: " /C:"error:" "%LOG%"
echo  ------------------------------------------------------------
echo.
echo  Log ka aakhri hissa ^(poora log: build-log.txt^):
echo  ------------------------------------------------------------
"%PS%" -NoProfile -Command "Get-Content '%LOG%' -Tail 40"
echo  ------------------------------------------------------------
echo.
echo  Ye errors ^(ya build-log.txt^) Claude ko paste kar dein -
echo  wo foran fix kar dega.
goto END

:END
echo.
echo  Window band karne ke liye koi bhi key dabayein...
pause >nul
endlocal
