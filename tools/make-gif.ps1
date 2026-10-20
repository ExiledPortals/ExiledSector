param(
    [Parameter(Mandatory = $true)][string]$Source,
    [string]$Name,
    [string]$Output,
    [string]$Crop,
    [int]$Width = 960,
    [int]$Fps = 20,
    [double]$MaxMB = 8,
    [switch]$KeepSize
)

$ErrorActionPreference = "Stop"

$ffmpegCommand = Get-Command ffmpeg -ErrorAction SilentlyContinue
$ffmpeg = if ($ffmpegCommand) { $ffmpegCommand.Source } else {
    Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Gyan.FFmpeg*\ffmpeg-*\bin\ffmpeg.exe" -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $ffmpeg) {
    throw "Could not find ffmpeg. Install it with: winget install Gyan.FFmpeg"
}
$ffprobe = Join-Path (Split-Path $ffmpeg) "ffprobe.exe"

$sourcePath = (Resolve-Path -LiteralPath $Source).Path
if (-not $Name) {
    $Name = [IO.Path]::GetFileNameWithoutExtension($sourcePath)
}
if (-not $Output) {
    $Output = Join-Path $PSScriptRoot "..\graphics\description\$Name.gif"
}
$outputPath = [IO.Path]::GetFullPath($Output)

$sourceWidth = [int](& $ffprobe -v error -select_streams v:0 -show_entries stream=width -of csv=p=0 $sourcePath)
$frameWidth = if ($Crop) { [int]($Crop.Split(":")[0]) } else { $sourceWidth }
$attemptWidth = [Math]::Min($Width, $frameWidth)
$attemptFps = $Fps

while ($true) {
    $filters = @()
    if ($Crop) {
        $filters += "crop=$Crop"
    }
    $filters += "fps=$attemptFps"
    if ($attemptWidth -lt $frameWidth) {
        $filters += "scale=${attemptWidth}:-2:flags=lanczos"
    }
    $graph = ($filters -join ",") + ",split[a][b];[a]palettegen=stats_mode=diff[p];[b][p]paletteuse=dither=sierra2_4a:diff_mode=rectangle"
    & $ffmpeg -v error -y -i $sourcePath -filter_complex $graph -loop 0 $outputPath
    if ($LASTEXITCODE -ne 0) {
        throw "ffmpeg failed to encode the GIF."
    }

    $sizeMB = (Get-Item $outputPath).Length / 1MB
    "{0}: {1}px wide, {2} fps, {3:N2} MB" -f $outputPath, $attemptWidth, $attemptFps, $sizeMB
    if ($KeepSize -or $sizeMB -le $MaxMB) {
        break
    }
    if ($attemptFps -gt 15) {
        $attemptFps = 15
    } elseif ($attemptWidth -gt 640) {
        $attemptWidth = [Math]::Max(640, [int]($attemptWidth * 0.85))
    } else {
        Write-Warning "Still over $MaxMB MB at 640px and 15 fps. Shorten the clip, crop tighter, or pass -KeepSize."
        break
    }
    "Over $MaxMB MB, retrying smaller."
}
