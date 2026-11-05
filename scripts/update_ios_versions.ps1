param(
    [Parameter(Mandatory = $true)][string]$Root
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$iosDir = Join-Path $Root 'ios'
$projectYml = Join-Path $iosDir 'project.yml'
$reportPath = Join-Path $iosDir 'build_report.txt'
$lines = New-Object System.Collections.Generic.List[string]

function Write-Report([string]$text) {
    Write-Host $text
    $lines.Add($text) | Out-Null
}

function Get-LatestTag([string]$repo) {
    try {
        $headers = @{ 'User-Agent' = 'Zyphuel-iOS-Tool'; 'Accept' = 'application/vnd.github+json' }
        $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/releases/latest" `
            -Headers $headers -TimeoutSec 20
        if ($release.tag_name) { return $release.tag_name.TrimStart('v') }
    } catch {
        return $null
    }
    return $null
}

function Update-PackagePin {
    param(
        [string]$Content,
        [string]$Repo,
        [string]$Version
    )
    if ([string]::IsNullOrWhiteSpace($Version)) { return $Content }

    $pattern = '(?s)(https://github\.com/' + [regex]::Escape($Repo) + '\s*\r?\n\s*from:\s*")[^"]+(")'
    # Braced group references: "$1" + "13.1.1" would be parsed as group 113.
    $replacement = '${1}' + $Version + '${2}'
    $updated = [regex]::Replace($Content, $pattern, $replacement)

    # Safety guard: never write a file whose URL or version pin disappeared.
    if ($updated -notmatch [regex]::Escape($Repo)) {
        throw "Pin update would corrupt the $Repo entry"
    }
    if ($updated -notmatch 'from:\s*"') {
        throw 'Pin update would remove the version pin'
    }
    return $updated
}

Write-Report 'Zyphuel iOS toolchain report'
Write-Report ('Generated: ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
Write-Report ''

# ---------------------------------------------------------------- dependency pins
$firebaseLatest = Get-LatestTag 'firebase/firebase-ios-sdk'
$googleLatest = Get-LatestTag 'google/GoogleSignIn-iOS'

if (-not $firebaseLatest) { Write-Report '[!] Could not reach GitHub for the latest Firebase iOS SDK; keeping the existing pin.' }
if (-not $googleLatest) { Write-Report '[!] Could not reach GitHub for the latest GoogleSignIn-iOS; keeping the existing pin.' }

$content = Get-Content -Raw -Path $projectYml
$original = $content
$pinFailure = $false

try {
    $content = Update-PackagePin -Content $content -Repo 'firebase/firebase-ios-sdk' -Version $firebaseLatest
    $content = Update-PackagePin -Content $content -Repo 'google/GoogleSignIn-iOS' -Version $googleLatest
} catch {
    $pinFailure = $true
    $content = $original
    Write-Report ('[x] ' + $_.Exception.Message + ' - original project.yml kept.')
}

if (-not $pinFailure) {
    if ($content -ne $original) {
        Set-Content -Path $projectYml -Value $content -Encoding UTF8 -NoNewline
        Write-Report '[+] ios/project.yml updated'
    } else {
        Write-Report '[=] ios/project.yml already current'
    }
}

if ($firebaseLatest) { Write-Report "[+] Firebase iOS SDK: latest release $firebaseLatest" } else { Write-Report '[=] Firebase iOS SDK: pin unchanged (offline)' }
if ($googleLatest) { Write-Report "[+] GoogleSignIn-iOS: latest release $googleLatest" } else { Write-Report '[=] GoogleSignIn-iOS: pin unchanged (offline)' }
Write-Report ''

# ---------------------------------------------------------------- structure checks
Write-Report 'Structure checks'

$required = @(
    'project.yml',
    'Config/Info.plist',
    'Config/Zyphuel.entitlements',
    'Zyphuel/App/ZyphuelApp.swift',
    'Zyphuel/App/AppState.swift',
    'Zyphuel/Core/Config/ZyphuelConfig.swift',
    'Zyphuel/Core/Models/Models.swift',
    'Zyphuel/Core/Pricing/PricingEngine.swift',
    'Zyphuel/Core/Security/Security.swift',
    'Zyphuel/Core/Services/AuthService.swift',
    'Zyphuel/Core/Services/OrderService.swift',
    'Zyphuel/Features/Auth/AuthView.swift',
    'Zyphuel/Features/Home/CustomerHomeView.swift',
    'Zyphuel/Features/Order/OrderViews.swift',
    'Zyphuel/Features/Rider/RiderHomeView.swift',
    'Zyphuel/Features/Admin/AdminDashboardView.swift',
    'Zyphuel/Features/Profile/ProfileView.swift',
    'Tests/ZyphuelLogicTests.swift'
)

$missing = 0
foreach ($item in $required) {
    if (Test-Path (Join-Path $iosDir $item)) {
        Write-Report "  [ok] $item"
    } else {
        Write-Report "  [missing] $item"
        $missing++
    }
}
Write-Report ''

# ---------------------------------------------------------------- dependency pins sanity
$yml = Get-Content -Raw -Path $projectYml
foreach ($repo in @('firebase/firebase-ios-sdk', 'google/GoogleSignIn-iOS')) {
    if ($yml -match ('https://github\.com/' + [regex]::Escape($repo))) {
        Write-Report "  [ok] $repo URL intact"
    } else {
        Write-Report "  [missing] $repo URL"
        $missing++
    }
}
$pinMatches = [regex]::Matches($yml, 'from:\s*"([0-9][^"]*)"')
Write-Report ("  [ok] version pins: " + (($pinMatches | ForEach-Object { $_.Groups[1].Value }) -join ', '))
Write-Report ''

# ---------------------------------------------------------------- plist validation
foreach ($plist in @('Config/Info.plist', 'Config/Zyphuel.entitlements')) {
    $path = Join-Path $iosDir $plist
    if (Test-Path $path) {
        try {
            [xml](Get-Content -Raw -Path $path) | Out-Null
            Write-Report "  [ok] $plist is valid XML"
        } catch {
            Write-Report "  [invalid] $plist is not valid XML"
            $missing++
        }
    }
}
Write-Report ''

# ---------------------------------------------------------------- Swift sanity
$swiftFiles = Get-ChildItem -Path (Join-Path $iosDir 'Zyphuel') -Recurse -Filter *.swift
$testFiles = Get-ChildItem -Path (Join-Path $iosDir 'Tests') -Recurse -Filter *.swift -ErrorAction SilentlyContinue
$totalLines = 0
$unbalanced = 0

foreach ($file in @($swiftFiles) + @($testFiles)) {
    $text = Get-Content -Raw -Path $file.FullName
    $totalLines += ($text -split "`n").Count
    $open = ([regex]::Matches($text, '\{')).Count
    $close = ([regex]::Matches($text, '\}')).Count
    if ($open -ne $close) {
        Write-Report "  [unbalanced] $($file.Name): { = $open, } = $close"
        $unbalanced++
    }
}

$appCount = @($swiftFiles).Count
$testCount = @($testFiles).Count
Write-Report "  Swift files: $appCount app, $testCount test"
Write-Report "  Swift lines: $totalLines"
Write-Report ''

# ---------------------------------------------------------------- config presence
$googlePlist = Join-Path $iosDir 'Config/GoogleService-Info.plist'
if (Test-Path $googlePlist) {
    Write-Report '[ok] GoogleService-Info.plist present (git-ignored)'
} else {
    Write-Report '[info] GoogleService-Info.plist missing. Add it from Firebase Console before running on a device.'
}
Write-Report ''

# ---------------------------------------------------------------- summary
if ($missing -eq 0 -and $unbalanced -eq 0 -and -not $pinFailure) {
    Write-Report 'RESULT: PASS - structure, pins and plist files are valid.'
} else {
    Write-Report ("RESULT: ATTENTION - missing=$missing unbalancedSwiftFiles=$unbalanced pinFailure=$pinFailure")
}

Set-Content -Path $reportPath -Value ($lines -join [Environment]::NewLine) -Encoding UTF8
