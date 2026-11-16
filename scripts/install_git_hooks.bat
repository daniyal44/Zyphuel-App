@echo off
setlocal
cd /d "%~dp0\.."

if not exist ".git" (
    echo [X] Not a git repository.
    exit /b 1
)

if not exist ".git\hooks" (
    mkdir ".git\hooks"
)

echo [i] Installing Git pre-commit hook (version auto-sync + graph update)...

(
echo #!/bin/sh
echo # Zyphuel Automated Version Sync + Code Graph Generator Hook
echo.
echo # Auto-sync version across all files on every commit
echo if command -v python ^>/dev/null 2^>^&1; then
echo     if [ -f "scripts/sync_versions.py" ]; then
echo         python scripts/sync_versions.py 2^>/dev/null
echo         git add version.properties 2^>/dev/null
echo         git add ios/project.yml 2^>/dev/null
echo         git add ios/Zyphuel/Core/Config/ZyphuelConfig.swift 2^>/dev/null
echo         git add README.md 2^>/dev/null
echo         git add FEATURES_DOCUMENTATION.md 2^>/dev/null
echo         git add docs/Side_bar.md 2^>/dev/null
echo         git add docs/structure.md 2^>/dev/null
echo     fi
echo fi
echo.
echo # Update engineering velocity graph
echo if command -v node ^>/dev/null 2^>^&1; then
echo     if [ -f "scripts/generate_github_graph.js" ]; then
echo         echo "[Git Hook] Automatically updating README code-based graph..."
echo         node scripts/generate_github_graph.js
echo         git add README.md
echo     fi
echo fi
echo exit 0
) > ".git\hooks\pre-commit"

echo [OK] Git pre-commit hook installed successfully at .git\hooks\pre-commit

rem Configure ours merge driver to permanently prevent merge conflicts on README.md
git config merge.ours.driver true
if not exist ".gitattributes" (
    echo README.md merge=ours> ".gitattributes"
)
echo [OK] Git merge driver configured: README.md merge=ours
endlocal
