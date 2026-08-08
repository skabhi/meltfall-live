param(
    [string]$ImagePath = "",
    [int]$DropCount = 80
)

if ([string]::IsNullOrWhiteSpace($ImagePath)) {
    $ImagePath = Join-Path $PSScriptRoot "melting_face_transparent.png"
}

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

if (-not (Test-Path -LiteralPath $ImagePath)) {
    throw "Image not found: $ImagePath"
}

function Convert-MagentaToTransparent {
    param([System.Drawing.Bitmap]$Source)

    $transparent = [System.Drawing.Bitmap]::new($Source.Width, $Source.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

    for ($y = 0; $y -lt $Source.Height; $y++) {
        for ($x = 0; $x -lt $Source.Width; $x++) {
            $pixel = $Source.GetPixel($x, $y)
            $isMagentaBackground = $pixel.R -gt 165 -and $pixel.B -gt 115 -and $pixel.G -lt 80

            if ($isMagentaBackground) {
                $transparent.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, $pixel.R, $pixel.G, $pixel.B))
            }
            else {
                $transparent.SetPixel($x, $y, $pixel)
            }
        }
    }

    return $transparent
}

function New-Drop {
    param(
        [int]$Width,
        [int]$Height,
        [Random]$Random
    )

    $sizeRoll = $Random.NextDouble()
    if ($sizeRoll -gt 0.86) {
        $size = $Random.Next(88, 142)
    }
    else {
        $size = $Random.Next(42, 88)
    }

    return [pscustomobject]@{
        X        = $Random.Next(-20, [Math]::Max(1, $Width - 20))
        Y        = -($size + $Random.Next(8, [Math]::Max(24, [int]($Height * 0.7))))
        Size     = $size
        Speed    = $Random.NextDouble() * 560 + 360
        Drift    = ($Random.NextDouble() - 0.5) * 38
        Rotation = $Random.NextDouble() * 360
        Spin     = ($Random.NextDouble() - 0.5) * 95
    }
}

$rawImage = [System.Drawing.Bitmap]::new($ImagePath)
$emoji = Convert-MagentaToTransparent -Source $rawImage
$rawImage.Dispose()

$random = [Random]::new()
$drops = [System.Collections.Generic.List[object]]::new()
$frameClock = [System.Diagnostics.Stopwatch]::StartNew()

$form = [System.Windows.Forms.Form]::new()
$form.Text = "Melting Face Rain - $DropCount emojis"
$form.Width = 980
$form.Height = 720
$form.BackColor = [System.Drawing.Color]::FromArgb(4, 8, 6)
$form.StartPosition = "CenterScreen"
$form.KeyPreview = $true

$doubleBufferedProperty = [System.Windows.Forms.Control].GetProperty(
    "DoubleBuffered",
    [System.Reflection.BindingFlags]::NonPublic -bor [System.Reflection.BindingFlags]::Instance
)
$doubleBufferedProperty.SetValue($form, $true, $null)

$DropCount = [Math]::Max(5, $DropCount)
for ($i = 0; $i -lt $DropCount; $i++) {
    $drops.Add((New-Drop -Width $form.ClientSize.Width -Height $form.ClientSize.Height -Random $random))
    $drops[$i].Y = -($drops[$i].Size + $random.Next(8, [Math]::Max(24, [int]($form.ClientSize.Height * 2.2))))
}

$form.Add_KeyDown({
    if ($_.KeyCode -eq [System.Windows.Forms.Keys]::Escape) {
        $form.Close()
    }
})

$form.Add_Paint({
    param($sender, $event)

    $graphics = $event.Graphics
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::Bilinear
    $graphics.Clear($form.BackColor)

    foreach ($drop in $drops) {
        if ($drop.Y -lt -$drop.Size -or $drop.Y -gt $form.ClientSize.Height) {
            continue
        }

        $state = $graphics.Save()
        $centerX = $drop.X + ($drop.Size / 2)
        $centerY = $drop.Y + ($drop.Size / 2)

        $graphics.TranslateTransform([float]$centerX, [float]$centerY)
        $graphics.RotateTransform([float]$drop.Rotation)

        $rect = [System.Drawing.Rectangle]::new(
            [int](-$drop.Size / 2),
            [int](-$drop.Size / 2),
            [int]$drop.Size,
            [int]$drop.Size
        )

        $graphics.DrawImage($emoji, $rect)
        $graphics.Restore($state)
    }
})

$timer = [System.Windows.Forms.Timer]::new()
$timer.Interval = 1
$fpsClock = [System.Diagnostics.Stopwatch]::StartNew()
$frameCounter = 0
$timer.Add_Tick({
    $elapsed = [Math]::Min(0.05, $frameClock.Elapsed.TotalSeconds)
    $frameClock.Restart()
    $script:frameCounter++

    for ($i = 0; $i -lt $drops.Count; $i++) {
        $drops[$i].Y += $drops[$i].Speed * $elapsed
        $drops[$i].X += $drops[$i].Drift * $elapsed
        $drops[$i].Rotation += $drops[$i].Spin * $elapsed

        if ($drops[$i].Y -gt ($form.ClientSize.Height + $drops[$i].Size)) {
            $drops[$i] = New-Drop -Width $form.ClientSize.Width -Height $form.ClientSize.Height -Random $random
        }
    }

    if ($fpsClock.ElapsedMilliseconds -ge 1000) {
        $fps = [Math]::Round(($frameCounter * 1000) / $fpsClock.ElapsedMilliseconds)
        $form.Text = "Melting Face Rain - $DropCount emojis - $fps FPS"
        $script:frameCounter = 0
        $fpsClock.Restart()
    }

    $form.Invalidate()
})

$form.Add_FormClosed({
    $timer.Stop()
    $timer.Dispose()
    $emoji.Dispose()
})

$timer.Start()
[void]$form.ShowDialog()
