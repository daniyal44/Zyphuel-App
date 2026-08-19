@echo off
setlocal enabledelayedexpansion
title Zyphuel - Push to GitHub
color 0A

rem ============================================================
rem   ZYPHUEL - Push source code to GitHub
rem   Repo: https://github.com/daniyal44/Zyphuel-App
rem
rem   Bas is file par DOUBLE-CLICK karein.
rem   Pehli baar GitHub login ka browser window khul sakta hai -
rem   wahan sign in kar lein.
rem ============================================================

cd /d "D:\Games\New folder-web\Claude"

set "REPO=https://github.com/daniyal44/Zyphuel-App"
set "BRANCH=main"

echo.
echo  ============================================================
echo    ZYPHUEL  -  GitHub Push
echo  ============================================================
echo.
echo    Repo : %REPO%
echo    Folder: %CD%
echo.

rem ---------- git installed? ----------
where git >nul 2>&1
if errorlevel 1 (
    echo  [X] Git install nahi hai.
    echo      Yahan se install karein: https://git-scm.com/download/win
    echo      Install ke baad ye script dobara chalayein.
    goto END
)

rem ---------- Privacy notice ----------
echo  ------------------------------------------------------------
echo    DHYAN DEIN - ye push PUBLIC ho sakta hai:
echo.
echo    Ye files .gitignore me hain, upload NAHI hongi:  (safe)
echo       .env          ^(API keys^)
echo       local.properties, debug.keystore, build-log.txt
echo.
echo    Ye file UPLOAD HOGI:
echo       app\google-services.json   ^(Firebase client config^)
echo.
echo    Firebase ka ye file client-side config hai aur aam tor par
echo    repos me hota hai. Agar aap ise public nahi karna chahte to
echo    ABHI "N" dabayein - main aap ko ignore karne ka tareeqa
echo    bata dunga.
echo  ------------------------------------------------------------
echo.
set /p "OK=Push karna hai? (Y = haan / N = nahi):  "
if /i not "!OK!"=="Y" (
    echo.
    echo  Cancel kar diya. Kuch upload nahi hua.
    goto END
)
echo.

rem ---------- git identity (agar set nahi hai) ----------
set "GITNAME="
for /f "delims=" %%N in ('git config user.name 2^>nul') do set "GITNAME=%%N"
if not defined GITNAME (
    echo  [i] Git username set nahi tha - set kar rahe hain.
    git config user.name "daniyal44"
)
set "GITMAIL="
for /f "delims=" %%N in ('git config user.email 2^>nul') do set "GITMAIL=%%N"
if not defined GITMAIL (
    echo  [i] Git email set nahi tha - set kar rahe hain.
    git config user.email "daniyal44@users.noreply.github.com"
)

rem ---------- repo init ----------
if not exist ".git" (
    echo  [1/5] Git repo banate hain...
    git init
    if errorlevel 1 goto GITFAIL
) else (
    echo  [1/5] Git repo pehle se maujood hai.
)

echo  [2/5] Branch "%BRANCH%" set kar rahe hain...
git branch -M %BRANCH% 2>nul

echo  [3/5] Remote "origin" set kar rahe hain...
git remote remove origin 2>nul
git remote add origin %REPO%
if errorlevel 1 goto GITFAIL

echo  [4/5] Files add + commit kar rahe hain...
git add -A
if errorlevel 1 goto GITFAIL

git diff --cached --quiet
if not errorlevel 1 (
    echo  [i] Koi naya change nahi mila - commit skip kar rahe hain.
    goto DO_PUSH
)

git commit -m "Real-time rider GPS tracking, live map fixes, and COD payment method" -m "- Rider foreground service publishes live GPS to Firestore live_tracking/{orderId}" -m "- Customer map marker follows the real rider position with smooth interpolation" -m "- Fix UI freeze: camera follow was keyed on the per-frame glide value, restarting the animation ~60x/second" -m "- Fix live rider marker never moving: WebView update only refreshed the destination pin, never the driver marker or route" -m "- Stop live GPS updates from hijacking the map camera every 4 seconds" -m "- Fall back to the vector map unless a real (AIza...) Maps SDK key is present" -m "- Thread the selected payment method (COD / Card) through placeOrder to the saved order" -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
if errorlevel 1 goto GITFAIL

:DO_PUSH
echo.
echo  [5/5] GitHub par push kar rahe hain...
echo        ^(Login ka browser window khul sakta hai - sign in kar lein^)
echo.
git push -u origin %BRANCH%
if not errorlevel 1 goto PUSHED

echo.
echo  [!] Push reject hua - lagta hai GitHub par pehle se kuch code hai.
echo      Pehle wahan ka code laate hain, phir dobara push karte hain...
echo.
git pull --rebase origin %BRANCH%
if errorlevel 1 (
    echo.
    echo  [X] Pull/rebase me masla aya ^(conflict ho sakta hai^).
    echo      Upar ka poora message Claude ko paste kar dein.
    goto END
)
git push -u origin %BRANCH%
if errorlevel 1 (
    echo.
    echo  [X] Push phir bhi fail hua. Upar ka message Claude ko paste kar dein.
    goto END
)

:PUSHED
echo.
echo  ============================================================
echo    HO GAYA! Code GitHub par chala gaya.
echo    Dekhein: %REPO%
echo  ============================================================
goto END

:GITFAIL
echo.
echo  [X] Git command fail hui. Upar ka message Claude ko paste kar dein.

:END
echo.
echo  Window band karne ke liye koi bhi key dabayein...
pause >nul
endlocal
