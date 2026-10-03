@echo off
setlocal enabledelayedexpansion
title Zyphuel - 1-Click GitHub Direct Push
color 0A

cd /d "%~dp0"

echo ============================================================
echo   ZYPHUEL - 1-CLICK DIRECT GITHUB PUSH
echo ============================================================
echo.
echo Folder : %CD%
echo Remote : https://github.com/daniyal44/Zyphuel-App
echo.

rem Check git
where git >nul 2>&1
if errorlevel 1 (
    echo [X] Git install nahi hai ya PATH me shamil nahi hai!
    echo     Download karein: https://git-scm.com/download/win
    goto END
)

rem Get active branch
for /f "tokens=*" %%b in ('git rev-parse --abbrev-ref HEAD 2^>nul') do set "BRANCH=%%b"
if "%BRANCH%"=="" set "BRANCH=main"
echo Branch : %BRANCH%
echo.

rem Update engineering velocity graph if Node.js is present
where node >nul 2>&1
if not errorlevel 1 (
    if exist "scripts\generate_github_graph.js" (
        echo [i] Engineering velocity graph update kar rahe hain...
        node scripts\generate_github_graph.js >nul 2>&1
    )
)

rem Set commit message
set "COMMIT_MSG=%~1"
if "%COMMIT_MSG%"=="" (
    set "COMMIT_MSG=chore: direct push update - %date% %time% (v2.6.4.0.0.21)"
)

echo [1/3] Files stage kar rahe hain (git add -A)...
git add -A

git diff --cached --quiet
if errorlevel 1 (
    echo [2/3] Commit banaya ja raha hai:
    echo       "%COMMIT_MSG%"
    git commit -m "%COMMIT_MSG%"
) else (
    echo [2/3] Koi naye uncommitted changes nahi hain, existing commits push karenge...
)

echo.
echo [3/3] GitHub par direct push kar rahe hain (%BRANCH%)...
git push origin %BRANCH%
if errorlevel 1 (
    echo.
    echo [!] Direct push reject hua. Automatic pull --rebase kar rahe hain...
    git pull --rebase origin %BRANCH%
    if errorlevel 1 (
        echo [X] Rebase me conflict aya.
        goto END
    )
    echo Dobara push kar rahe hain...
    git push origin %BRANCH%
    if errorlevel 1 (
        echo [X] Push fail ho gaya. Internet connection ya permissions check karein.
        goto END
    )
)

echo.
echo ============================================================
echo   SUCCESS! Code kamyabi se GitHub par upload ho gaya hai.
echo   Repository: https://github.com/daniyal44/Zyphuel-App
echo   Branch: %BRANCH%
echo ============================================================

:END
echo.
echo Window band karne ke liye koi bhi key dabayein...
pause >nul
endlocal