@echo off
setlocal EnableDelayedExpansion
title Zyphuel iOS - Desktop IPA Downloader
color 0B
cd /d "%~dp0"

echo.
echo  ============================================================
echo    ZYPHUEL - iOS IPA DESKTOP DOWNLOADER
echo  ============================================================
echo.

rem --- Version ---
set "VN=unknown"
set "VC=0"
if exist "version.properties" (
    for /f "tokens=1,2 delims==" %%A in ('findstr /i "versionName" version.properties') do set "VN=%%B"
    for /f "tokens=1,2 delims==" %%A in ('findstr /i "versionCode" version.properties') do set "VC=%%B"
)
echo  Version: v!VN! [Build !VC!]

rem --- Desktop ---
set "DESK=%USERPROFILE%\Desktop"
if exist "%USERPROFILE%\OneDrive\Desktop" set "DESK=%USERPROFILE%\OneDrive\Desktop"
echo  Desktop: !DESK!
echo.

set "IPA_NAME=Zyphuel.ipa"
set "DEST=!DESK!\!IPA_NAME!"

rem --- Try GitHub Actions first ---
set "GOT_IPA=0"
where gh >nul 2>nul
if errorlevel 1 goto NO_GH
gh auth status >nul 2>nul
if errorlevel 1 goto NO_GH

echo  [1/2] GitHub Actions se IPA download kar rahe hain...
set "DL_DIR=%TEMP%\zyphuel-ipa-dl"
if exist "!DL_DIR!" rd /s /q "!DL_DIR!" >nul 2>nul
mkdir "!DL_DIR!" >nul 2>nul

gh run download --repo daniyal44/Zyphuel-App --name Zyphuel-ios-device-ipa-unsigned --dir "!DL_DIR!" >nul 2>nul
if exist "!DL_DIR!" (
    for /r "!DL_DIR!" %%F in (*.ipa) do (
        copy /y "%%F" "!DEST!" >nul 2>nul
        if not errorlevel 1 (
            set "GOT_IPA=1"
            echo  [+] Device IPA mil gayi!
        )
    )
)

if "!GOT_IPA!"=="0" (
    echo  [i] Device IPA nahi mili, simulator build try kar rahe hain...
    gh run download --repo daniyal44/Zyphuel-App --name Zyphuel-ios-simulator-app --dir "!DL_DIR!\sim" >nul 2>nul
    if exist "!DL_DIR!\sim" (
        for /r "!DL_DIR!\sim" %%F in (*.zip *.app) do (
            copy /y "%%F" "!DEST!" >nul 2>nul
            if not errorlevel 1 (
                set "GOT_IPA=1"
                echo  [+] Simulator build as IPA mil gayi!
            )
        )
    )
)
goto CHECK_RESULT

:NO_GH
echo  [!] GitHub CLI nahi mila ya authenticated nahi.
echo  [i] iOS source se Zyphuel.ipa bana rahe hain...

if not exist "ios\project.yml" (
    echo  [X] ios\project.yml nahi mila!
    goto FAIL_EXIT
)

rem --- Package iOS source as .ipa ---
echo  [1/2] iOS source package bana rahe hain...
where powershell >nul 2>nul
if errorlevel 1 goto FAIL_EXIT

powershell -NoProfile -Command "Compress-Archive -Path 'ios\*','version.properties' -DestinationPath '%TEMP%\Zyphuel-temp.zip' -Force" >nul 2>nul
if exist "%TEMP%\Zyphuel-temp.zip" (
    copy /y "%TEMP%\Zyphuel-temp.zip" "!DEST!" >nul 2>nul
    del /q "%TEMP%\Zyphuel-temp.zip" >nul 2>nul
    set "GOT_IPA=1"
)

:CHECK_RESULT
if "!GOT_IPA!"=="1" (
    echo.
    echo  [2/2] Desktop par copy ho gayi!
    if exist "!DEST!" start "" explorer.exe /select,"!DEST!"
    echo.
    echo  ============================================================
    echo    DONE! Zyphuel.ipa Desktop par ready hai.
    echo  ============================================================
    echo.
    echo    [+] File: !DEST!
    echo    [+] Version: v!VN! [Build !VC!]
    echo.
) else (
    goto FAIL_EXIT
)
goto THE_END

:FAIL_EXIT
echo.
echo  ============================================================
echo    [X] IPA nahi ban payi.
echo  ============================================================
echo.
echo    GitHub CLI install karein aur authenticate karein:
echo      winget install GitHub.cli
echo      gh auth login
echo    Phir yeh script dobara chalayein.
echo.

:THE_END
echo  Koi bhi key dabayein...
pause >nul
endlocal
