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

echo [i] Installing Git pre-commit hook for automated code graph updates...

(
echo #!/bin/sh
echo # Zyphuel Automated Code Graph Generator Hook
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
