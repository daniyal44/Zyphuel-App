@echo off
setlocal
title Zyphuel Desktop - Run (debug output)
color 0D

rem ============================================================
rem   ZYPHUEL DESKTOP - run karne ka doosra tareeqa
rem
rem   Ye app ko POORE JDK par chalati hai (packaged .exe ki trimmed
rem   JRE ke bajaye). Do fayde:
rem     1) Agar .exe "failed to launch" de raha ho, ye phir bhi chalegi
rem     2) Koi bhi asli error/stack trace is window me dikhega
rem
rem   Screen par jo error aaye, wo Claude ko paste kar dein.
rem ============================================================

cd /d "D:\Games\New folder-web\Claude"

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot"
set "GRADLE=C:\Users\mdani\.gradle\wrapper\dists\gradle-9.5.0-bin\bvnork1r7n8i6kp5cnkibsc9q\gradle-9.5.0\bin\gradle.bat"

echo.
echo  ============================================================
echo    ZYPHUEL Desktop - Debug Run
echo  ============================================================
echo.
echo   App window khulni chahiye. Ye console window khuli rehne dein -
echo   isme app ke messages aur errors aate hain.
echo.
echo   Band karne ke liye: app window band karein, ya Ctrl+C dabayein.
echo  ------------------------------------------------------------
echo.

call "%GRADLE%" -p desktop run --console=plain --stacktrace

echo.
echo  ------------------------------------------------------------
echo   App band ho gayi.
echo.
echo   Agar upar koi error/exception hai, wo Claude ko paste kar dein.
echo.
echo   Ek aur kaam ki cheez: ye command batati hai ke packaged .exe ko
echo   kaun se Java modules chahiye ^(agar .exe launch na ho^):
echo.
echo      gradle -p desktop suggestRuntimeModules
echo.
pause
endlocal
