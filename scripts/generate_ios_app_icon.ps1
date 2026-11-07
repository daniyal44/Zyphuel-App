# Generates the Zyphuel iOS App Store icon (1024x1024, opaque PNG, no alpha channel).
# Run from the repository root:
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\generate_ios_app_icon.ps1
#
# The icon is committed to the repository, so this script only needs to run when the
# brand mark changes. Apple rejects icons that carry an alpha channel, therefore the
# bitmap is rendered as 24bpp RGB.

param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$target = Join-Path $Root 'ios\Zyphuel\Resources\Assets.xcassets\AppIcon.appiconset'
if (-not (Test-Path $target)) { New-Item -ItemType Directory -Path $target -Force | Out-Null }

$size = 1024
$bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

# --- background: navy -> ocean diagonal, same palette as the app theme
$rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)
$bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    $rect,
    [System.Drawing.Color]::FromArgb(15, 23, 42),
    [System.Drawing.Color]::FromArgb(3, 105, 161),
    50.0
)
$g.FillRectangle($bg, $rect)
$bg.Dispose()

# --- soft light bloom behind the mark
$bloom = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(26, 255, 255, 255))
$g.FillEllipse($bloom, -180, -260, 900, 900)
$bloom.Dispose()

# --- brand blueprint grid lines (very subtle)
$grid = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(16, 255, 255, 255), 2)
for ($i = 128; $i -lt $size; $i += 128) {
    $g.DrawLine($grid, $i, 0, $i, $size)
    $g.DrawLine($grid, 0, $i, $size, $i)
}
$grid.Dispose()

# --- bold white "Z" mark
$z = New-Object System.Drawing.Drawing2D.GraphicsPath
$z.AddPolygon(@(
    (New-Object System.Drawing.Point(292, 286)),
    (New-Object System.Drawing.Point(732, 286)),
    (New-Object System.Drawing.Point(732, 386)),
    (New-Object System.Drawing.Point(486, 638)),
    (New-Object System.Drawing.Point(732, 638)),
    (New-Object System.Drawing.Point(732, 738)),
    (New-Object System.Drawing.Point(292, 738)),
    (New-Object System.Drawing.Point(292, 638)),
    (New-Object System.Drawing.Point(538, 386)),
    (New-Object System.Drawing.Point(292, 386))
))
$shadow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(60, 2, 6, 23))
$g.TranslateTransform(0, 10)
$g.FillPath($shadow, $z)
$g.ResetTransform()
$shadow.Dispose()

$white = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
$g.FillPath($white, $z)
$white.Dispose()
$z.Dispose()

# --- amber brand accent bar (fuel / energy cue)
$accent = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(245, 158, 11))
$g.FillRectangle($accent, 292, 790, 440, 32)
$accent.Dispose()

$g.Dispose()

$outFile = Join-Path $target 'AppIcon-1024.png'
$bmp.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

$info = Get-Item $outFile
Write-Host ("[ok] {0} ({1:N0} bytes, {2}x{3})" -f $info.FullName, $info.Length, $size, $size)
