@echo off
setlocal enabledelayedexpansion
title Zyphuel - Push CI Fixes
color 0A

cd /d "%~dp0"

echo ============================================================
echo   ZYPHUEL - PUSH CI FIXES (Ripple, Workflows, Security)
echo ============================================================
echo.

rem Remove stale git lock if present
if exist ".git\index.lock" (
    echo [i] Stale git lock file detected, removing...
    del /f /q ".git\index.lock"
    if exist ".git\index.lock" (
        echo [X] Cannot remove .git\index.lock - close all git programs and try again.
        goto END
    )
    echo [OK] Lock removed.
)

rem Configure git
git config user.name "daniyal44"
git config user.email "daniyal44@users.noreply.github.com"

rem Fetch and sync with remote
echo.
echo [1/5] Remote se sync kar rahe hain...
git fetch origin main >nul 2>&1
git pull --rebase origin main >nul 2>&1

rem Update engineering velocity graph
where node >nul 2>&1
if not errorlevel 1 (
    if exist "scripts\generate_github_graph.js" (
        echo [2/5] Engineering velocity graph update kar rahe hain...
        node scripts\generate_github_graph.js >nul 2>&1
    )
) else (
    echo [2/5] Node.js nahi mila, graph skip kar rahe hain...
)

rem Stage fix files one by one (avoids line-continuation issues)
echo [3/5] Fix files stage kar rahe hain...
git add .github/workflows/android-build.yml 2>nul
git add .github/workflows/ios-build.yml 2>nul
git add .github/workflows/update-graph.yml 2>nul
git add .gitignore 2>nul
git add github.bat 2>nul
git add app/src/main/java/com/example/ui/Screens.kt 2>nul
git add desktop_apk_download.bat 2>nul
git add desktop_ios_download.bat 2>nul
git add README.md 2>nul

rem Check if there are changes to commit
git diff --cached --quiet
if errorlevel 1 (
    echo [4/5] Commit bana rahe hain...
    git commit -m "fix: ripple+lifecycle imports, CI email, .gitignore, bat scripts"
) else (
    echo [4/5] Koi staged changes nahi hain...
)

echo [5/5] GitHub par push kar rahe hain...
git push origin main
if errorlevel 1 (
    echo.
    echo [!] Push reject hua, rebase kar ke dobara try...
    git pull --rebase origin main
    git push origin main
    if errorlevel 1 (
        echo [X] Push fail. Internet ya permissions check karein.
        goto END
    )
)

echo.
echo ============================================================
echo   DONE! Sab fixes push ho gayi hain.
echo.
echo   Changes pushed:
echo   - Screens.kt: rememberRipple fix + LocalLifecycleOwner fix
echo   - android-build.yml: Email Gateway notification
echo   - ios-build.yml: Secrets.plist fix + email notification
echo   - update-graph.yml: Daily cron schedule
echo   - .gitignore: SMTP/email password patterns
echo   - github.bat: Removed forced [skip ci]
echo   - desktop_apk_download.bat: Auto-detect JDK/Gradle + GH fallback
echo   - desktop_ios_download.bat: Device IPA download + version fix
echo.
echo   CI will run automatically after push!
echo   https://github.com/daniyal44/Zyphuel-App/actions
echo ============================================================

:END
echo.
echo Koi bhi key dabayein...
pause >nul
endlocal
