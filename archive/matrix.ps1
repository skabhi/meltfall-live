$ErrorActionPreference = "SilentlyContinue"

$chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%^&*()[]{}<>?/|\-=+;:.,"
$accentColors = @(
    [ConsoleColor]::Cyan,
    [ConsoleColor]::Yellow,
    [ConsoleColor]::Magenta,
    [ConsoleColor]::Blue,
    [ConsoleColor]::Red
)

$random = [Random]::new()
$columns = [Console]::WindowWidth
$height = [Console]::WindowHeight
$drops = New-Object int[] $columns
$lengths = New-Object int[] $columns
$speeds = New-Object int[] $columns

function Reset-Drop {
    param([int]$Index, [int]$Height)

    $script:drops[$Index] = -$script:random.Next(0, [Math]::Max(2, $Height))
    $script:lengths[$Index] = $script:random.Next(5, 18)
    $script:speeds[$Index] = $script:random.Next(1, 4)
}

function Resize-Rain {
    param([int]$Columns, [int]$Height)

    $oldDrops = $script:drops
    $oldLengths = $script:lengths
    $oldSpeeds = $script:speeds

    $script:drops = New-Object int[] $Columns
    $script:lengths = New-Object int[] $Columns
    $script:speeds = New-Object int[] $Columns

    for ($i = 0; $i -lt $Columns; $i++) {
        if ($i -lt $oldDrops.Length) {
            $script:drops[$i] = $oldDrops[$i]
            $script:lengths[$i] = $oldLengths[$i]
            $script:speeds[$i] = $oldSpeeds[$i]
        }
        else {
            Reset-Drop -Index $i -Height $Height
        }
    }
}

for ($i = 0; $i -lt $columns; $i++) {
    Reset-Drop -Index $i -Height $height
}

[Console]::CursorVisible = $false
Clear-Host

try {
    while ($true) {
        $columns = [Console]::WindowWidth
        $height = [Console]::WindowHeight

        if ($drops.Length -ne $columns) {
            Resize-Rain -Columns $columns -Height $height
            Clear-Host
        }

        for ($x = 0; $x -lt $columns; $x++) {
            if (($random.Next(0, 100) -gt 72) -and ($drops[$x] -lt 0)) {
                continue
            }

            for ($step = 0; $step -lt $speeds[$x]; $step++) {
                $headY = $drops[$x]
                $tailY = $headY - $lengths[$x]

                if ($headY -ge 0 -and $headY -lt $height) {
                    $char = $chars[$random.Next(0, $chars.Length)]
                    [Console]::SetCursorPosition($x, $headY)

                    if ($random.NextDouble() -lt 0.05) {
                        [Console]::ForegroundColor = $accentColors[$random.Next(0, $accentColors.Length)]
                    }
                    else {
                        [Console]::ForegroundColor = [ConsoleColor]::White
                    }

                    [Console]::Write($char)
                }

                $midY = $headY - 1
                if ($midY -ge 0 -and $midY -lt $height) {
                    $char = $chars[$random.Next(0, $chars.Length)]
                    [Console]::SetCursorPosition($x, $midY)
                    [Console]::ForegroundColor = [ConsoleColor]::Green
                    [Console]::Write($char)
                }

                $dimY = $headY - 4
                if ($dimY -ge 0 -and $dimY -lt $height) {
                    $char = $chars[$random.Next(0, $chars.Length)]
                    [Console]::SetCursorPosition($x, $dimY)
                    [Console]::ForegroundColor = [ConsoleColor]::DarkGreen
                    [Console]::Write($char)
                }

                if ($tailY -ge 0 -and $tailY -lt $height) {
                    [Console]::SetCursorPosition($x, $tailY)
                    [Console]::Write(" ")
                }

                $drops[$x]++

                if ($drops[$x] -gt ($height + $lengths[$x]) -or $random.NextDouble() -gt 0.997) {
                    Reset-Drop -Index $x -Height $height
                    break
                }
            }
        }

        Start-Sleep -Milliseconds 35
    }
}
finally {
    [Console]::ResetColor()
    [Console]::CursorVisible = $true
    Clear-Host
}
