@echo off
setlocal enabledelayedexpansion
title Zyphuel - 1-Click GitHub Direct Push with Code Graph Auto-Sync
color 0A

cd /d "%~dp0"

echo ============================================================
echo   ZYPHUEL - 1-CLICK DIRECT GITHUB PUSH ^& GRAPH SYNC
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

rem Configure Git merge driver to permanently prevent merge conflicts on README.md
git config merge.ours.driver true
if not exist ".gitattributes" (
    echo README.md merge=ours> ".gitattributes"
)

rem Ensure Git pre-commit hook is active
if exist ".git" (
    if not exist ".git\hooks\pre-commit" (
        if exist "scripts\install_git_hooks.bat" (
            call scripts\install_git_hooks.bat >nul 2>&1
        )
    )
)

rem Get active branch
for /f "tokens=*" %%b in ('git rev-parse --abbrev-ref HEAD 2^>nul') do set "BRANCH=%%b"
if "%BRANCH%"=="" set "BRANCH=main"
echo Branch : %BRANCH%
echo.

rem Pre-sync: Fetch remote first to prevent fast-forward push rejections
echo [0/3] Remote repository sync check kar rahe hain...
git fetch origin %BRANCH% >nul 2>&1
for /f %%c in ('git rev-list HEAD..origin/%BRANCH% --count 2^>nul') do set "BEHIND_COUNT=%%c"
if not "!BEHIND_COUNT!"=="" if not "!BEHIND_COUNT!"=="0" (
    echo [i] Remote repository par !BEHIND_COUNT! naye commit^(s^) hain. Auto-syncing...
    git status --porcelain | findstr /r "." >nul 2>&1
    if not errorlevel 1 (
        git stash push -u -m "zyphuel_autostash" >nul 2>&1
        set "DID_STASH=1"
    ) else (
        set "DID_STASH=0"
    )
    git pull --rebase -X ours origin %BRANCH% >nul 2>&1
    if errorlevel 1 (
        git checkout --theirs README.md >nul 2>&1
        git add README.md >nul 2>&1
        set "GIT_EDITOR=true"
        git rebase --continue >nul 2>&1
        if errorlevel 1 git rebase --skip >nul 2>&1
    )
    if "!DID_STASH!"=="1" (
        git stash pop >nul 2>&1
        if errorlevel 1 (
            git checkout --ours README.md >nul 2>&1
            git add README.md >nul 2>&1
        )
    )
)

rem Update engineering velocity graph if Node.js is present
where node >nul 2>&1
if not errorlevel 1 (
    if exist "scripts\generate_github_graph.js" (
        echo [i] Engineering velocity graph update kar rahe hain...
        node scripts\generate_github_graph.js
    )
)

rem Set commit message
set "COMMIT_MSG=%~1"
if "%COMMIT_MSG%"=="" (
    set "APP_VER=v2.6.4.0.0.25"
    if exist "app\build.gradle.kts" (
        for /f "tokens=2 delims==" %%i in ('findstr /i "versionName" app\build.gradle.kts 2^>nul') do (
            set "TEMP_VER=%%~i"
            set "TEMP_VER=!TEMP_VER:"=!"
            set "TEMP_VER=!TEMP_VER: =!"
            if not "!TEMP_VER!"=="" set "APP_VER=v!TEMP_VER!"
        )
    )
    set "COMMIT_MSG=chore: direct push update - %date% %time% (!APP_VER!) [skip ci]"
) else (
    echo "!COMMIT_MSG!" | findstr /i "skip ci" >nul 2>&1
    if errorlevel 1 set "COMMIT_MSG=!COMMIT_MSG! [skip ci]"
)

echo [1/3] Files stage kar rahe hain (git add -A)...
git add -A

git diff --cached --quiet
if errorlevel 1 (
    echo [2/3] Commit banaya ja raha hai:
    echo       "%COMMIT_MSG%"
    git commit -m "%COMMIT_MSG%"

    rem Refresh graph with the newly created commit and amend seamlessly
    where node >nul 2>&1
    if not errorlevel 1 (
        if exist "scripts\generate_github_graph.js" (
            node scripts\generate_github_graph.js >nul 2>&1
            git add README.md >nul 2>&1
            git diff --cached --quiet >nul 2>&1
            if errorlevel 1 (
                git commit --amend --no-edit >nul 2>&1
            )
        )
    )
) else (
    echo [2/3] Koi naye uncommitted changes nahi hain, existing commits push karenge...
)

echo.
echo [3/3] GitHub par direct push kar rahe hain (%BRANCH%)...
git push origin %BRANCH%
if errorlevel 1 (
    echo.
    echo [!] Push reject hua. Auto-resolving and syncing with remote...
    git fetch origin %BRANCH%
    git pull --rebase -X ours origin %BRANCH%
    if errorlevel 1 (
        echo [i] Resolving auto-generated graph in README.md...
        git checkout --theirs README.md >nul 2>&1
        where node >nul 2>&1
        if not errorlevel 1 (
            if exist "scripts\generate_github_graph.js" (
                node scripts\generate_github_graph.js >nul 2>&1
            )
        )
        git add README.md >nul 2>&1
        set "GIT_EDITOR=true"
        git rebase --continue >nul 2>&1
        if errorlevel 1 (
            git rebase --skip >nul 2>&1
        )
    )

    rem Refresh graph once more before final push
    where node >nul 2>&1
    if not errorlevel 1 (
        if exist "scripts\generate_github_graph.js" (
            node scripts\generate_github_graph.js >nul 2>&1
            git add README.md >nul 2>&1
            git diff --cached --quiet >nul 2>&1
            if errorlevel 1 (
                git commit --amend --no-edit >nul 2>&1
            )
        )
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
if "%~2"=="--no-pause" goto FINISH
if "%NON_INTERACTIVE%"=="1" goto FINISH
echo Window band karne ke liye koi bhi key dabayein...
pause >nul
:FINISH
endlocal