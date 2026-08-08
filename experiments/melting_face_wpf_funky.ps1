param(
    [string]$ImagePath = "",
    [int]$DropCount = 0,
    [ValidateSet("Default", "PowerSaving", "HighPerformance")]
    [string]$GpuPreference = "Default",
    [switch]$SkipGpuPreferenceApply,
    [switch]$HighlightRareBigDrop,
    [ValidateRange(0.0, 1.0)]
    [double]$RareBigEmojiChance = 0.012,
    [switch]$RegenerateFunkyAssets
)

if ([string]::IsNullOrWhiteSpace($ImagePath)) {
    $ImagePath = Join-Path $PSScriptRoot "melting_face_transparent.png"
}

function Set-WindowsGpuPreferenceAndRestart {
    param(
        [string]$Preference,
        [string]$ScriptPath,
        [string]$ImagePath,
        [int]$DropCount,
        [double]$RareBigEmojiChance,
        [bool]$RegenerateFunkyAssets,
        [bool]$HighlightRareBigDrop
    )

    $hostExe = [System.Diagnostics.Process]::GetCurrentProcess().MainModule.FileName
    $gpuPreferenceKey = "HKCU:\Software\Microsoft\DirectX\UserGpuPreferences"

    if ($Preference -eq "Default") {
        if (Test-Path -LiteralPath $gpuPreferenceKey) {
            Remove-ItemProperty -Path $gpuPreferenceKey -Name $hostExe -ErrorAction SilentlyContinue
        }
        return
    }

    $preferenceValue = if ($Preference -eq "HighPerformance") { "GpuPreference=2;" } else { "GpuPreference=1;" }

    if (-not (Test-Path -LiteralPath $gpuPreferenceKey)) {
        New-Item -Path $gpuPreferenceKey -Force | Out-Null
    }

    New-ItemProperty -Path $gpuPreferenceKey -Name $hostExe -Value $preferenceValue -PropertyType String -Force | Out-Null

    $argumentList = @(
        "-NoExit",
        "-STA",
        "-ExecutionPolicy", "Bypass",
        "-File", "`"$ScriptPath`"",
        "-ImagePath", "`"$ImagePath`"",
        "-DropCount", "$DropCount",
        "-RareBigEmojiChance", "$RareBigEmojiChance",
        "-GpuPreference", $Preference,
        "-SkipGpuPreferenceApply"
    )

    if ($HighlightRareBigDrop) {
        $argumentList += "-HighlightRareBigDrop"
    }

    if ($RegenerateFunkyAssets) {
        $argumentList += "-RegenerateFunkyAssets"
    }

    Start-Process -FilePath $hostExe -ArgumentList $argumentList

    exit
}

if (-not $SkipGpuPreferenceApply) {
    Set-WindowsGpuPreferenceAndRestart -Preference $GpuPreference -ScriptPath $PSCommandPath -ImagePath $ImagePath -DropCount $DropCount -RareBigEmojiChance $RareBigEmojiChance -RegenerateFunkyAssets $RegenerateFunkyAssets.IsPresent -HighlightRareBigDrop $HighlightRareBigDrop.IsPresent
}

Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName PresentationFramework
Add-Type -AssemblyName WindowsBase
Add-Type -AssemblyName System.Drawing

if (-not (Test-Path -LiteralPath $ImagePath)) {
    throw "Image not found: $ImagePath"
}

$random = [Random]::new()
$BaseDropDensity = if ($DropCount -gt 0) { $DropCount / (980.0 * 720.0) } else { 240.0 / (980.0 * 720.0) }
$RareBigDropChance = $RareBigEmojiChance
$RareBigSizeMultiplier = 1.85
$RareBigSpeedMultiplier = 0.62
$RareBigDropActive = $false
$assetDir = Join-Path $PSScriptRoot "funky_emoji_assets"
$rareBigAssetPath = Join-Path $PSScriptRoot "melting_face_rare_big_highlight.png"

if (-not (Test-Path -LiteralPath $assetDir)) {
    New-Item -ItemType Directory -Path $assetDir | Out-Null
}

function New-TintedAssets {
    param(
        [string]$SourcePath,
        [string]$OutputDir,
        [bool]$Force = $false
    )

    $palette = @(
        @{ Name = "acid";    R = 0.45; G = 1.35; B = 0.55 },
        @{ Name = "cyan";    R = 0.45; G = 1.05; B = 1.45 },
        @{ Name = "pink";    R = 1.35; G = 0.55; B = 1.25 },
        @{ Name = "orange";  R = 1.45; G = 0.88; B = 0.42 },
        @{ Name = "violet";  R = 0.92; G = 0.62; B = 1.55 },
        @{ Name = "lime";    R = 0.82; G = 1.55; B = 0.42 },
        @{ Name = "hotred";  R = 1.55; G = 0.46; B = 0.42 },
        @{ Name = "yellow";  R = 1.35; G = 1.25; B = 0.35 }
    )

    $sourceInfo = Get-Item -LiteralPath $SourcePath
    $cachedPaths = @($palette | ForEach-Object { Join-Path $OutputDir ("melting_face_{0}.png" -f $_.Name) })
    $cacheReady = -not $Force

    foreach ($path in $cachedPaths) {
        if (-not (Test-Path -LiteralPath $path)) {
            $cacheReady = $false
            break
        }

        if ((Get-Item -LiteralPath $path).LastWriteTime -lt $sourceInfo.LastWriteTime) {
            $cacheReady = $false
            break
        }
    }

    if ($cacheReady) {
        return $cachedPaths
    }

    $source = [System.Drawing.Bitmap]::new($SourcePath)
    $paths = [System.Collections.Generic.List[string]]::new()

    foreach ($tone in $palette) {
        $path = Join-Path $OutputDir ("melting_face_{0}.png" -f $tone.Name)
        $target = [System.Drawing.Bitmap]::new($source.Width, $source.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                $pixel = $source.GetPixel($x, $y)
                $isMagentaBackground = $pixel.R -gt 165 -and $pixel.B -gt 115 -and $pixel.G -lt 80

                if ($isMagentaBackground) {
                    $target.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
                    continue
                }

                $r = [Math]::Min(255, [int]($pixel.R * $tone.R))
                $g = [Math]::Min(255, [int]($pixel.G * $tone.G))
                $b = [Math]::Min(255, [int]($pixel.B * $tone.B))
                $target.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, $r, $g, $b))
            }
        }

        $target.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
        $target.Dispose()
        $paths.Add($path)
    }

    $source.Dispose()
    return $paths
}

function Load-Bitmap {
    param([string]$Path)

    $bitmap = [System.Windows.Media.Imaging.BitmapImage]::new()
    $bitmap.BeginInit()
    $bitmap.CacheOption = [System.Windows.Media.Imaging.BitmapCacheOption]::OnLoad
    $bitmap.UriSource = [Uri]::new($Path)
    $bitmap.EndInit()
    $bitmap.Freeze()
    return $bitmap
}

function Ensure-HighlightedRareBigAsset {
    param(
        [string]$SourcePath,
        [string]$OutputPath,
        [bool]$Force = $false
    )

    if ((-not $Force) -and (Test-Path -LiteralPath $OutputPath)) {
        $sourceInfo = Get-Item -LiteralPath $SourcePath
        $outputInfo = Get-Item -LiteralPath $OutputPath

        if ($outputInfo.LastWriteTime -ge $sourceInfo.LastWriteTime) {
            return
        }
    }

    $source = [System.Drawing.Bitmap]::new($SourcePath)
    $target = [System.Drawing.Bitmap]::new($source.Width, $source.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $red = [System.Drawing.Color]::FromArgb(255, 230, 0, 20)
    $white = [System.Drawing.Color]::FromArgb(255, 255, 255, 255)

    for ($y = 0; $y -lt $source.Height; $y++) {
        for ($x = 0; $x -lt $source.Width; $x++) {
            $pixel = $source.GetPixel($x, $y)
            $isMagentaBackground = $pixel.R -gt 165 -and $pixel.B -gt 115 -and $pixel.G -lt 80

            if ($pixel.A -gt 20 -and -not $isMagentaBackground) {
                $target.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, $white.R, $white.G, $white.B))
                continue
            }

            $nearOpaquePixel = $false
            for ($outlineY = [Math]::Max(0, $y - 2); $outlineY -le [Math]::Min($source.Height - 1, $y + 2); $outlineY++) {
                for ($outlineX = [Math]::Max(0, $x - 2); $outlineX -le [Math]::Min($source.Width - 1, $x + 2); $outlineX++) {
                    $outlinePixel = $source.GetPixel($outlineX, $outlineY)
                    $outlineIsMagenta = $outlinePixel.R -gt 165 -and $outlinePixel.B -gt 115 -and $outlinePixel.G -lt 80

                    if ($outlinePixel.A -gt 20 -and -not $outlineIsMagenta) {
                        $nearOpaquePixel = $true
                        break
                    }
                }

                if ($nearOpaquePixel) {
                    break
                }
            }

            if ($nearOpaquePixel) {
                $target.SetPixel($x, $y, $red)
            }
            else {
                $target.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
            }
        }
    }

    $target.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $source.Dispose()
    $target.Dispose()
}

$assetPaths = New-TintedAssets -SourcePath $ImagePath -OutputDir $assetDir -Force $RegenerateFunkyAssets.IsPresent
$bitmaps = @($assetPaths | ForEach-Object { Load-Bitmap -Path $_ })
$rareBigBitmap = $null
if ($HighlightRareBigDrop) {
    Ensure-HighlightedRareBigAsset -SourcePath $ImagePath -OutputPath $rareBigAssetPath -Force $RegenerateFunkyAssets.IsPresent
    $rareBigBitmap = Load-Bitmap -Path $rareBigAssetPath
}

$window = [System.Windows.Window]::new()
$window.Title = "Melting Face Funky WPF Rain - GPU $GpuPreference"
$window.Width = 980
$window.Height = 720
$window.WindowStartupLocation = [System.Windows.WindowStartupLocation]::CenterScreen
$window.Background = [System.Windows.Media.SolidColorBrush]::new([System.Windows.Media.Color]::FromRgb(4, 6, 10))

$root = [System.Windows.Controls.Grid]::new()
$canvas = [System.Windows.Controls.Canvas]::new()
$canvas.ClipToBounds = $true
$canvas.Background = $window.Background
[void]$root.Children.Add($canvas)

$shortcutOverlay = [System.Windows.Controls.TextBlock]::new()
$shortcutOverlay.Text = "F: fullscreen   Q/Esc: quit"
$shortcutOverlay.Foreground = [System.Windows.Media.SolidColorBrush]::new([System.Windows.Media.Color]::FromRgb(225, 255, 245))
$shortcutOverlay.Background = [System.Windows.Media.SolidColorBrush]::new([System.Windows.Media.Color]::FromArgb(150, 0, 0, 0))
$shortcutOverlay.FontFamily = [System.Windows.Media.FontFamily]::new("Consolas")
$shortcutOverlay.FontSize = 16
$shortcutOverlay.Padding = [System.Windows.Thickness]::new(12, 7, 12, 7)
$shortcutOverlay.Margin = [System.Windows.Thickness]::new(12)
$shortcutOverlay.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Left
$shortcutOverlay.VerticalAlignment = [System.Windows.VerticalAlignment]::Top
$shortcutOverlay.IsHitTestVisible = $false
[System.Windows.Controls.Panel]::SetZIndex($shortcutOverlay, 10000)
[void]$root.Children.Add($shortcutOverlay)

$window.Content = $root

[System.Windows.Media.RenderOptions]::SetBitmapScalingMode($canvas, [System.Windows.Media.BitmapScalingMode]::LowQuality)
[System.Windows.Media.RenderOptions]::SetEdgeMode($canvas, [System.Windows.Media.EdgeMode]::Unspecified)

function Get-StageWidth {
    [Math]::Max(1, $canvas.ActualWidth)
}

function Get-StageHeight {
    [Math]::Max(1, $canvas.ActualHeight)
}

function Reset-Drop {
    param(
        [pscustomobject]$Drop,
        [bool]$Initial = $false
    )

    $stageWidth = Get-StageWidth
    $stageHeight = Get-StageHeight
    $z = $random.NextDouble()
    $depth = [Math]::Pow($z, 1.35)
    $depthScale = 0.23 + ($depth * 2.17)
    $wasRareBig = $Drop.PSObject.Properties.Name -contains "IsRareBig" -and $Drop.IsRareBig

    if ($wasRareBig) {
        $script:RareBigDropActive = $false
    }

    $isRareBig = (-not $script:RareBigDropActive) -and ($random.NextDouble() -lt $RareBigDropChance)

    $size = [int](($random.Next(42, 86)) * $depthScale)
    if ($isRareBig) {
        $size = [int]($size * $RareBigSizeMultiplier)
        $script:RareBigDropActive = $true
    }
    $size = [Math]::Max(12, [Math]::Min(360, $size))

    $Drop.Size = $size
    $Drop.IsRareBig = $isRareBig
    $Drop.Z = $z
    $Drop.X = $random.Next(-$size, [Math]::Max(1, [int]$stageWidth))
    $Drop.Y = -($size + $random.Next(12, [Math]::Max(24, [int]($stageHeight * $(if ($Initial) { 2.2 } else { 0.7 })))))
    $Drop.Speed = ($random.NextDouble() * 280 + 180) * (0.32 + $depth * 2.55)
    if ($isRareBig) {
        $Drop.Speed *= $RareBigSpeedMultiplier
    }
    $Drop.Drift = ($random.NextDouble() - 0.5) * (5 + $depth * 62)
    $Drop.Rotation = $random.NextDouble() * 360
    $Drop.Spin = ($random.NextDouble() - 0.5) * (6 + $depth * 118)

    if ($HighlightRareBigDrop -and $isRareBig) {
        $Drop.Image.Source = $rareBigBitmap
    }
    else {
        $Drop.Image.Source = $bitmaps[$random.Next(0, $bitmaps.Count)]
    }
    $Drop.Image.Width = $size
    $Drop.Image.Height = $size
    $Drop.Image.Opacity = 0.18 + ($depth * 0.82)
    $Drop.Rotate.Angle = $Drop.Rotation
    $Drop.Translate.X = $Drop.X
    $Drop.Translate.Y = $Drop.Y
    [System.Windows.Controls.Panel]::SetZIndex($Drop.Image, [int]($depth * 1000))
}

$drops = [System.Collections.Generic.List[object]]::new()

function New-DropSprite {
    $image = [System.Windows.Controls.Image]::new()
    $image.Stretch = [System.Windows.Media.Stretch]::Fill
    $image.SnapsToDevicePixels = $false
    [System.Windows.Media.RenderOptions]::SetBitmapScalingMode($image, [System.Windows.Media.BitmapScalingMode]::LowQuality)

    $rotate = [System.Windows.Media.RotateTransform]::new()
    $translate = [System.Windows.Media.TranslateTransform]::new()
    $transform = [System.Windows.Media.TransformGroup]::new()
    [void]$transform.Children.Add($rotate)
    [void]$transform.Children.Add($translate)
    $image.RenderTransform = $transform
    $image.RenderTransformOrigin = [System.Windows.Point]::new(0.5, 0.5)
    [System.Windows.Controls.Canvas]::SetLeft($image, 0)
    [System.Windows.Controls.Canvas]::SetTop($image, 0)

    [void]$canvas.Children.Add($image)

    $drop = [pscustomobject]@{
        Image    = $image
        Rotate   = $rotate
        Translate = $translate
        X        = 0.0
        Y        = 0.0
        Size     = 64.0
        Speed    = 500.0
        Drift    = 0.0
        IsRareBig = $false
        Z        = 1.0
        Rotation = 0.0
        Spin     = 0.0
    }

    $drops.Add($drop)
    Reset-Drop -Drop $drop -Initial $true
}

function Update-DropCountForArea {
    $targetCount = [Math]::Max(12, [int]([Math]::Round((Get-StageWidth) * (Get-StageHeight) * $BaseDropDensity)))

    while ($drops.Count -lt $targetCount) {
        New-DropSprite
    }

    while ($drops.Count -gt $targetCount) {
        $lastIndex = $drops.Count - 1
        $drop = $drops[$lastIndex]
        $canvas.Children.Remove($drop.Image)
        $drops.RemoveAt($lastIndex)
    }
}

$window.Add_Loaded({
    Update-DropCountForArea

    $shortcutTimer = [System.Windows.Threading.DispatcherTimer]::new()
    $shortcutTimer.Interval = [TimeSpan]::FromSeconds(3)
    $shortcutTimer.Add_Tick({
        param($timerSender, $eventArgs)

        $timerSender.Stop()
        $fade = [System.Windows.Media.Animation.DoubleAnimation]::new(0, [TimeSpan]::FromMilliseconds(900))
        $shortcutOverlay.BeginAnimation([System.Windows.UIElement]::OpacityProperty, $fade)
    })
    $shortcutTimer.Start()
})

$window.Add_SizeChanged({
    Update-DropCountForArea
})

$window.Add_KeyDown({
    if ($_.Key -eq [System.Windows.Input.Key]::Escape -or $_.Key -eq [System.Windows.Input.Key]::Q) {
        $window.Close()
    }
    elseif ($_.Key -eq [System.Windows.Input.Key]::F) {
        if ($window.WindowStyle -eq [System.Windows.WindowStyle]::None) {
            $window.WindowStyle = [System.Windows.WindowStyle]::SingleBorderWindow
            $window.ResizeMode = [System.Windows.ResizeMode]::CanResize
            $window.WindowState = [System.Windows.WindowState]::Normal
        }
        else {
            $window.WindowStyle = [System.Windows.WindowStyle]::None
            $window.ResizeMode = [System.Windows.ResizeMode]::NoResize
            $window.WindowState = [System.Windows.WindowState]::Maximized
        }
    }
})

$clock = [System.Diagnostics.Stopwatch]::StartNew()
$fpsClock = [System.Diagnostics.Stopwatch]::StartNew()
$frames = 0

$renderHandler = [System.EventHandler]{
    $elapsed = [Math]::Min(0.05, $clock.Elapsed.TotalSeconds)
    $clock.Restart()

    $stageHeight = Get-StageHeight

    foreach ($drop in $drops) {
        $drop.Y += $drop.Speed * $elapsed
        $drop.X += $drop.Drift * $elapsed
        $drop.Rotation += $drop.Spin * $elapsed

        if ($drop.Y -gt ($stageHeight + $drop.Size)) {
            Reset-Drop -Drop $drop
            continue
        }

        $drop.Translate.X = $drop.X
        $drop.Translate.Y = $drop.Y
        $drop.Rotate.Angle = $drop.Rotation
    }

    $script:frames++
    if ($fpsClock.ElapsedMilliseconds -ge 1000) {
        $fps = [Math]::Round(($frames * 1000) / $fpsClock.ElapsedMilliseconds)
        $window.Title = "Melting Face Funky WPF Rain - $($drops.Count) emojis - $fps FPS - GPU $GpuPreference"
        $script:frames = 0
        $fpsClock.Restart()
    }
}

[System.Windows.Media.CompositionTarget]::add_Rendering($renderHandler)

$window.Add_Closed({
    [System.Windows.Media.CompositionTarget]::remove_Rendering($renderHandler)
})

[void]$window.ShowDialog()
