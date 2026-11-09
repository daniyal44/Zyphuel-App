@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Zyphuel iOS - Real Desktop Build Deliverables
color 0B

rem ============================================================
rem   ZYPHUEL - iOS DESKTOP DELIVERABLE BUILDER (honest artifacts)
rem   Is file par DOUBLE-CLICK karein:
rem     1) iOS structure, SPM pins aur version sync verify karega
rem     2) Asli .ipa / Simulator .app dhoondega aur VALIDATE karega
rem     3) Agar asli signed IPA mil jaye to Desktop par copy karega,
rem        warna GitHub Actions se real Simulator build laayega,
rem        aur agar woh bhi na mile to SOURCE ZIP (naam me -Source)
rem        deliver karega - source ko .ipa naam se kabhi nahi badlega.
rem
rem   NOTE: Windows par iOS app compile nahi hoti (Xcode macOS-only).
rem         Isliye 84 KB "ipa" ho hi nahi sakti - woh sirf source code
rem         ki zip hoti hai. Yeh script ab woh ghalti nahi karti.
rem ============================================================

cd /d "%~dp0"

set "LOG=ios_build.log"
set "OUT_ROOT=ios-builds"
set "REPO=daniyal44/Zyphuel-App"
set "FAIL=0"
set "MIN_IPA_BYTES=1048576"

echo ====================================================== > "%LOG%"
echo Zyphuel iOS Windows Orchestrator>> "%LOG%"
echo Started: %date% %time%>> "%LOG%"
echo Root: %CD%>> "%LOG%"
echo ======================================================>> "%LOG%"

echo.
echo  ============================================================
echo    ZYPHUEL - iOS DESKTOP DELIVERABLE BUILDER
echo  ============================================================
echo.
echo  Root Folder: %CD%
echo  Log File   : %LOG%
echo.

rem ---------- Timestamp ----------
set "TS="
for /f "usebackq delims=" %%T in (`powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"`) do (
    set "TS=%%T"
)
if "%TS%"=="" set "TS=build_output"
set "BUILD_DIR=%OUT_ROOT%\%TS%"

rem ---------- Desktop path ----------
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

rem ---------- Version ----------
set "VERSION_NAME=unknown"
set "VERSION_CODE=0"

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
rem   STEP 1: PRE-FLIGHT
rem ============================================================
echo  [1/4] Checking Windows tools...
echo [%date% %time%] PRE-FLIGHT>>"%LOG%"

where git >nul 2>nul
if errorlevel 1 (
    echo  [!] git nahi mila - source restore skip ho sakta hai.
    echo [!] git not found>>"%LOG%"
) else (
    echo  [+] git detected.
)

where python >nul 2>nul
if errorlevel 1 (
    echo  [!] python nahi mila - version sync skip.
) else (
    echo  [+] Python 3 detected.
)

where powershell >nul 2>nul
if errorlevel 1 (
    echo  [X] PowerShell missing - IPA validation possible nahi.
    echo [x] PowerShell missing>>"%LOG%"
    set FAIL=1
) else (
    echo  [+] PowerShell detected.
)

set "HAS_GH=0"
where gh >nul 2>nul
if not errorlevel 1 (
    gh auth status >nul 2>nul
    if not errorlevel 1 set "HAS_GH=1"
)
if "%HAS_GH%"=="1" (
    echo  [+] GitHub CLI authenticated - Actions artifact download ON.
) else (
    echo  [i] GitHub CLI nahi mila/authenticated nahi. Actions artifact skip.
    echo [info] gh CLI unavailable or not authenticated>>"%LOG%"
)

set "MAC_BUILD=0"
where xcodebuild >nul 2>nul
if not errorlevel 1 set "MAC_BUILD=1"
if "%MAC_BUILD%"=="0" (
    echo  [i] Xcode Windows par nahi hai - yahan IPA ban nahi sakti (expected).
    echo [info] xcodebuild unavailable on this host>>"%LOG%"
)

rem ============================================================
rem   STEP 2: SOURCE + VALIDATION
rem ============================================================
echo.
echo  [2/4] Verifying iOS source and version sync...
echo [%date% %time%] SOURCE and DEPENDENCIES>>"%LOG%"

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

where python >nul 2>nul
if not errorlevel 1 (
    if exist "scripts\sync_versions.py" (
        echo  [*] Syncing versions (Android + iOS + docs)...
        python scripts\sync_versions.py >>"%LOG%" 2>&1
    )
)

if exist "scripts\update_ios_versions.ps1" (
    echo  [*] Running iOS structure and plist validation...
    powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\update_ios_versions.ps1" -Root "%CD%" >>"%LOG%" 2>&1
    if errorlevel 1 (
        echo  [!] Validation warnings - see %LOG%.
    ) else (
        echo  [+] iOS structure, pins and plist validation passed.
    )
)

rem ---------- Icon / privacy manifest presence (App Store readiness) ----------
if exist "ios\Zyphuel\Resources\Assets.xcassets\AppIcon.appiconset\AppIcon-1024.png" (
    echo  [+] App Store icon present (1024x1024).
) else (
    echo  [!] App icon missing - run scripts\generate_ios_app_icon.ps1
    echo [!] AppIcon-1024.png missing>>"%LOG%"
)
if exist "ios\Config\PrivacyInfo.xcprivacy" (
    echo  [+] Privacy manifest present (PrivacyInfo.xcprivacy).
) else (
    echo  [!] PrivacyInfo.xcprivacy missing - App Store upload will fail.
    echo [!] PrivacyInfo.xcprivacy missing>>"%LOG%"
)

rem ============================================================
rem   STEP 3: FIND A REAL BUILD (never a renamed source tree)
rem ============================================================
echo.
echo  [3/4] Locating a real installable build...
echo [%date% %time%] STAGING>>"%LOG%"

if not exist "%OUT_ROOT%" mkdir "%OUT_ROOT%"
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

> "%BUILD_DIR%\manifest.txt" (
    echo Zyphuel iOS Build Staging
    echo Timestamp: %TS%
    echo Date: %date% %time%
    echo Host: Windows
    echo VersionName: %VERSION_NAME%
    echo VersionCode: %VERSION_CODE%
    echo XcodeAvailable: %MAC_BUILD%
    echo GhCliReady: %HAS_GH%
)

if exist "ios\project.yml" copy /y "ios\project.yml" "%BUILD_DIR%\project.yml" >nul 2>&1
if exist "version.properties" copy /y "version.properties" "%BUILD_DIR%\version.properties" >nul 2>&1
if exist "ios\build_report.txt" copy /y "ios\build_report.txt" "%BUILD_DIR%\ios_build_report.txt" >nul 2>&1

rem ---- 3a. Pull device IPA and Simulator app artifacts from GitHub Actions
set "SIM_APP_ZIP="
set "DEVICE_IPA="
if "%FAIL%"=="0" if "%HAS_GH%"=="1" (
    echo  [*] GitHub Actions se latest builds download kar rahe hain...

    rem Try device IPA first (preferred - installable on real devices)
    gh run download --repo "%REPO%" --name Zyphuel-ios-device-ipa-unsigned --dir "%BUILD_DIR%\actions-device" >>"%LOG%" 2>&1
    if exist "%BUILD_DIR%\actions-device" (
        for /r "%BUILD_DIR%\actions-device" %%F in (*.ipa) do (
            if exist "%%F" set "DEVICE_IPA=%%F"
        )
    )
    if not "!DEVICE_IPA!"=="" (
        echo  [+] Device IPA mil gayi: !DEVICE_IPA!
    ) else (
        echo  [i] Device IPA artifact nahi mila.
    )

    rem Also try Simulator build
    gh run download --repo "%REPO%" --name Zyphuel-ios-simulator-app --dir "%BUILD_DIR%\actions" >>"%LOG%" 2>&1
    if exist "%BUILD_DIR%\actions" (
        for /r "%BUILD_DIR%\actions" %%F in (*.zip) do (
            if exist "%%F" set "SIM_APP_ZIP=%%F"
        )
    )
    if not "!SIM_APP_ZIP!"=="" (
        echo  [+] Simulator build mil gaya: !SIM_APP_ZIP!
    ) else (
        echo  [i] Koi fresh Simulator artifact nahi mila.
    )
)

rem ---- 3b. Collect IPA candidates anywhere in the workspace
set "FOUND_IPA="
set "REJECTED_IPA="
for /r "ios" %%F in (*.ipa) do call :CHECK_IPA "%%F"
if exist "%OUT_ROOT%" (
    for /r "%OUT_ROOT%" %%F in (*.ipa) do call :CHECK_IPA "%%F"
)

echo  [*] IPA search complete.
if not "%FOUND_IPA%"=="" echo  [+] Valid IPA: %FOUND_IPA%
if not "%FOUND_IPA%"=="" goto COPY_FOUND_IPA

rem Prefer device IPA from Actions over simulator build
if not "%DEVICE_IPA%"=="" goto COPY_DEVICE_IPA

if not "%SIM_APP_ZIP%"=="" goto COPY_SIM_APP
goto STAGE_SOURCE_ZIP

rem ------------------------------------------------------------------
:CHECK_IPA
set "CAND=%~1"
if "%CAND%"=="" goto :eof
set "CAND_SIZE=0"
for %%A in ("%CAND%") do set "CAND_SIZE=%%~zA"
if %CAND_SIZE% LSS %MIN_IPA_BYTES% (
    echo  [!] Chhoti file reject ki ^(source zip, asli app nahi^): %CAND%
    echo [!] Rejected undersized ipa: %CAND% (%CAND_SIZE% bytes)>>"%LOG%"
    set "REJECTED_IPA=%CAND%"
    goto :eof
)
set "VERDICT=NO"
for /f "usebackq delims=" %%R in (`powershell -NoProfile -Command "try { Add-Type -AssemblyName System.IO.Compression.FileSystem; $z=[System.IO.Compression.ZipFile]::OpenRead('!CAND!'); $hit=$false; foreach ($e in $z.Entries) { if ($e.FullName -match '^Payload/[^/]+\.app/Info\.plist$') { $hit=$true } }; $z.Dispose(); if ($hit) { 'YES' } else { 'NO' } } catch { 'NO' }"`) do set "VERDICT=%%R"
if /i not "%VERDICT%"=="YES" (
    echo  [!] Reject: Payload/*.app structure nahi hai - %CAND%
    echo [!] Rejected non-app archive: %CAND%>>"%LOG%"
    set "REJECTED_IPA=%CAND%"
    goto :eof
)
set "FOUND_IPA=%CAND%"
goto :eof

rem ------------------------------------------------------------------
:COPY_DEVICE_IPA
set "DESKTOP_DIPA=%DESKTOP_DIR%\Zyphuel-v%VERSION_NAME%-Build%VERSION_CODE%-Device-unsigned.ipa"
copy /y "%DEVICE_IPA%" "%DESKTOP_DIPA%" >nul
set "TARGET_DESKTOP_ITEM=%DESKTOP_DIPA%"
set "OUTPUT_KIND=Device IPA (unsigned) from GitHub Actions - sign with Xcode/AltStore"
set "EXTRA_INFO=Sign karne ke liye: Xcode re-sign ya AltStore use karein."
echo [ok] Copied device IPA to Desktop: %DESKTOP_DIPA%>>"%LOG%"
goto STAGE_DONE

rem ------------------------------------------------------------------
:COPY_FOUND_IPA
set "DESKTOP_IPA=%DESKTOP_DIR%\Zyphuel-v%VERSION_NAME%-Build%VERSION_CODE%.ipa"
copy /y "%FOUND_IPA%" "%DESKTOP_IPA%" >nul
set "TARGET_DESKTOP_ITEM=%DESKTOP_IPA%"
set "OUTPUT_KIND=Signed/real iOS IPA"
set "EXTRA_INFO="
echo [ok] Copied IPA to Desktop: %DESKTOP_IPA%>>"%LOG%"
goto STAGE_DONE

rem ------------------------------------------------------------------
:COPY_SIM_APP
set "DESKTOP_SIM=%DESKTOP_DIR%\Zyphuel-v%VERSION_NAME%-Build%VERSION_CODE%-Simulator.app.zip"
copy /y "%SIM_APP_ZIP%" "%DESKTOP_SIM%" >nul
set "TARGET_DESKTOP_ITEM=%DESKTOP_SIM%"
set "OUTPUT_KIND=Real Simulator build (.app.zip) - installs in iOS Simulator"
set "EXTRA_INFO=Simulator install: xcrun simctl install booted Zyphuel.app"
echo [ok] Copied Simulator build to Desktop: %DESKTOP_SIM%>>"%LOG%"
goto STAGE_DONE

rem ------------------------------------------------------------------
:STAGE_SOURCE_ZIP
echo  [*] Koi real build nahi mila. Source package bana rahe hain ^(naam me -Source^)...
set "SOURCE_ZIP=%DESKTOP_DIR%\Zyphuel-v%VERSION_NAME%-Build%VERSION_CODE%-iOS-Source.zip"
set "TEMP_ZIP=%TEMP%\Zyphuel-iOS-src-%TS%.zip"
powershell -NoProfile -Command "Compress-Archive -Path 'ios\*', 'version.properties' -DestinationPath '%TEMP_ZIP%' -Force" >>"%LOG%" 2>&1
if exist "%TEMP_ZIP%" (
    copy /y "%TEMP_ZIP%" "%SOURCE_ZIP%" >nul 2>&1
    del /q "%TEMP_ZIP%" >nul 2>&1
)
set "TARGET_DESKTOP_ITEM=%SOURCE_ZIP%"
set "OUTPUT_KIND=SOURCE CODE package - NOT an installable app"
set "EXTRA_INFO=Yeh .ipa nahi hai. Build ke liye instructions file parhein."

> "%DESKTOP_DIR%\Zyphuel-iOS-Build-Instructions.txt" (
    echo ============================================================
    echo   ZYPHUEL iOS - HOW TO GET A REAL, INSTALLABLE APP
    echo ============================================================
    echo App Version : v%VERSION_NAME% [Build %VERSION_CODE%]
    echo Generated   : %date% %time%
    echo.
    echo IMPORTANT: Windows par Xcode nahi hota, isliye yahan real
    echo .ipa file ban hi nahi sakti. Jo 84 KB ki "ipa" mil rahi thi
    echo woh sirf source code ki zip thi - ab usko -Source.zip naam
    echo diya jata hai taake confusion khatam ho.
    echo.
    echo OPTION A - GitHub Actions ^(recommended, no Mac needed^)
    echo ------------------------------------------------------------
    echo 1. Change push karein: github.bat
    echo 2. GitHub -^> Actions -^> "iOS Build" workflow khatam hone dein.
    echo 3. Artifact download karein: "Zyphuel-ios-simulator-app"
    echo    ^(Simulator par chalane ke liye^).
    echo 4. App Store / TestFlight ke liye signed build zaroori hai:
    echo    Xcode -^> Product -^> Archive -^> Distribute App.
    echo.
    echo OPTION B - macOS / MacBook
    echo ------------------------------------------------------------
    echo 1. Is source ZIP ko Mac par extract karein.
    echo 2. Terminal:
    echo      brew install xcodegen
    echo      cd ios ^&^& xcodegen generate
    echo      open Zyphuel.xcodeproj
    echo 3. Signing Team select karein, phir Product -^> Archive.
    echo.
    echo CONFIG REMINDER
    echo    ios/Config/GoogleService-Info.plist Firebase Console se
    echo    add karein - warna app setup mode me khulti hai.
    echo ============================================================
)

:STAGE_DONE

rem ============================================================
rem   STEP 4: SUMMARY
rem ============================================================
echo.
echo  [4/4] Opening Desktop folder...
if exist "%TARGET_DESKTOP_ITEM%" (
    start "" explorer.exe /select,"%TARGET_DESKTOP_ITEM%"
)

:SUMMARY
echo.
echo  ============================================================
if not "%FAIL%"=="0" goto ERROR_SUMMARY

echo    DONE: iOS deliverable Desktop par ready hai.
echo  ============================================================
echo.
echo    [+] App Version : v%VERSION_NAME% [Build %VERSION_CODE%]
echo    [+] Output Type : %OUTPUT_KIND%
echo    [+] Desktop File: %TARGET_DESKTOP_ITEM%
if defined EXTRA_INFO echo    [+] Note        : %EXTRA_INFO%
if not "%REJECTED_IPA%"=="" (
    echo.
    echo    [!] Purani chhoti/fake ".ipa" file mili aur reject ki gayi:
    echo        %REJECTED_IPA%
    echo        Woh ek source-code archive hai, app nahi. Aap usko
    echo        Desktop se delete kar sakte hain.
)
echo    [+] Staged Dir  : %CD%\%BUILD_DIR%
goto END_REPORT

:ERROR_SUMMARY
echo    ERROR: Setup check fail hua - %LOG% dekhein.
echo  ============================================================

:END_REPORT
echo    [+] Log File     : %CD%\%LOG%
echo  ============================================================
echo.
echo RESULT FAIL=%FAIL% at %date% %time%>>"%LOG%"
pause
if "%FAIL%"=="0" (exit /b 0) else (exit /b 1)
