@echo off
setlocal enabledelayedexpansion
title Zyphuel Desktop - Build and Run
color 0E

rem ============================================================
rem   ZYPHUEL DESKTOP (Operations Console) - Windows .exe
rem
rem   Bas is file par DOUBLE-CLICK karein. Ye:
rem     1) Desktop app build karti hai (Compose Desktop)
rem     2) Asli .exe banati hai
rem     3) App chala deti hai
rem
rem   PEHLI BAAR: Gradle ko Compose Desktop libraries download karni
rem   hongi (~150-250 MB), is liye pehli build 5-15 minute le sakti
rem   hai. Baad ki builds 20-30 second.
rem
rem   Ye build Android app se BILKUL ALAG hai - yahan koi masla ho
rem   to aapki mobile app par koi asar nahi padega.
rem ============================================================

cd /d "D:\Games\New folder-web\Claude"

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot"
set "GRADLE=C:\Users\mdani\.gradle\wrapper\dists\gradle-9.5.0-bin\bvnork1r7n8i6kp5cnkibsc9q\gradle-9.5.0\bin\gradle.bat"
set "LOG=D:\Games\New folder-web\Claude\desktop-build-log.txt"
set "PS=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
set "APPEXE=D:\Games\New folder-web\Claude\desktop\build\compose\binaries\main\app\ZyphuelOpsConsole\ZyphuelOpsConsole.exe"

echo.
echo  ============================================================
echo    ZYPHUEL  -  Desktop Operations Console
echo  ============================================================
echo.
echo    Ye app aapke PC par:
echo      - Saare orders live dikhayegi
echo      - Rider ko asli map par chalte hue dikhayegi
echo      - Order status change karne degi (Assigned/Delivering/...)
echo.
echo    Internet zaroori hai (Firestore + map tiles).
echo.

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo  [X] JDK nahi mila: %JAVA_HOME%
    goto END
)
if not exist "%GRADLE%" (
    echo  [X] Gradle nahi mila: %GRADLE%
    goto END
)

rem ---------- Purana chalta hua app band karein ----------
rem Agar pichli .exe abhi bhi chal rahi hai (ya crash hone ke baad JVM zinda
rem reh gaya hai), wo jar files pakde rakhti hai aur Gradle naya app image nahi
rem bana pata: "Unable to delete directory ... app". Is liye pehle band karte hain.
echo  [0/2] Purana app process aur purana build saaf kar rahe hain...
taskkill /IM ZyphuelOpsConsole.exe /F >nul 2>&1
if not errorlevel 1 echo        - chalta hua app band kar diya
timeout /t 2 /nobreak >nul 2>&1

rem Poora compose output hata dete hain, taake trimmed JRE (createRuntimeImage)
rem bhi naye Java modules ke saath dobara bane - warna wo "UP-TO-DATE" reh jati hai
rem aur app phir launch hote hi gir jati hai.
if exist "D:\Games\New folder-web\Claude\desktop\build\compose" (
    rmdir /S /Q "D:\Games\New folder-web\Claude\desktop\build\compose" >nul 2>&1
)
if exist "D:\Games\New folder-web\Claude\desktop\build\compose" (
    echo.
    echo  [X] Purana build folder delete nahi ho saka - kuch use kar raha hai.
    echo      Ye karein:
    echo        1^) Koi bhi khuli "ZyphuelOpsConsole" window band karein
    echo        2^) Windows Explorer me agar ye folder khula hai to band karein:
    echo           desktop\build\compose\binaries\main\app
    echo        3^) Phir ye script dobara chalayein
    goto END
)
echo        - purana build saaf ho gaya
echo.

echo  [1/2] Desktop app build ho rahi hai...
echo        ^(pehli baar lamba lag sakta hai - libraries download hongi^)
echo        Log: desktop-build-log.txt
echo  ------------------------------------------------------------
echo.

call "%GRADLE%" -p desktop createDistributable --console=plain 2>&1 | "%PS%" -NoProfile -Command "$input | ForEach-Object { Write-Host $_; $_ } | Out-File -FilePath '%LOG%' -Encoding ascii"

echo.
echo  ------------------------------------------------------------
findstr /C:"BUILD SUCCESSFUL" "%LOG%" >nul 2>&1
if errorlevel 1 goto BUILD_FAILED

if not exist "%APPEXE%" (
    echo  [!] Build successful magar .exe nahi mila yahan:
    echo      %APPEXE%
    echo      "desktop\build\compose\binaries\main\app" folder check karein.
    goto END
)

echo  [OK] BUILD SUCCESSFUL
echo       EXE: %APPEXE%
echo.
echo  [2/2] App chala rahe hain...
echo.
echo  NOTE: App ko yahin se chalana behtar hai - is folder me
echo        app\google-services.json hai jise app Firebase settings
echo        ke liye padhti hai.
echo.

rem CWD project root hi rehta hai, taake app google-services.json dhoond le.
start "" "%APPEXE%"

echo  ============================================================
echo    App launch kar di gayi - console window khulni chahiye.
echo.
echo    AGAR WINDOW NA KHULE ^(ya "failed to launch" aaye^):
echo       RUN-DESKTOP-DEBUG.bat chalayein.
echo       Wo app ko poore JDK par chalati hai aur asli error
echo       screen par dikhati hai.
echo.
echo    Shortcut banane ke liye: %APPEXE%
echo    ka shortcut banayein aur us shortcut ki "Start in" property
echo    ko is folder par set karein:
echo    D:\Games\New folder-web\Claude
echo  ============================================================
goto END

:BUILD_FAILED
echo  [X] DESKTOP BUILD FAIL HO GAYA.
echo.
echo  Log ka aakhri hissa ^(poora log: desktop-build-log.txt^):
echo  ------------------------------------------------------------
"%PS%" -NoProfile -Command "Get-Content '%LOG%' -Tail 45"
echo  ------------------------------------------------------------
echo.
echo  Ye errors Claude ko paste kar dein - wo foran fix kar dega.
echo  ^(Aksar pehli baar plugin/version ka chota masla hota hai.^)

:END
echo.
echo  Window band karne ke liye koi bhi key dabayein...
pause >nul
endlocal
