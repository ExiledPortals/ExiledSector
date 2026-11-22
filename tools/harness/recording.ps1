$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms
if (-not ("HarnessGameWindow" -as [type])) {
    Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class HarnessGameWindow {
    [StructLayout(LayoutKind.Sequential)] public struct Rect { public int Left, Top, Right, Bottom; }
    [StructLayout(LayoutKind.Sequential)] public struct Point { public int X, Y; }

    [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr window, out Rect rect);
    [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr window, out Rect rect);
    [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr window, ref Point point);
    [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr window, IntPtr insertAfter, int x, int y, int width, int height, uint flags);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr window);
}
"@
}

$script:topmostWindow = [IntPtr](-1)
$script:noSizeAndShow = [uint32]0x41

function Find-Ffmpeg {
    $ffmpegCommand = Get-Command ffmpeg -ErrorAction SilentlyContinue
    if ($ffmpegCommand) { return $ffmpegCommand.Source }
    $installed = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Gyan.FFmpeg*\ffmpeg-*\bin\ffmpeg.exe" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $installed) { throw "Recording needs ffmpeg. Install it with: winget install Gyan.FFmpeg" }
    return $installed.FullName
}

function Get-ClientArea([IntPtr]$window) {
    $client = New-Object HarnessGameWindow+Rect
    [HarnessGameWindow]::GetClientRect($window, [ref]$client) | Out-Null
    $origin = New-Object HarnessGameWindow+Point
    [HarnessGameWindow]::ClientToScreen($window, [ref]$origin) | Out-Null
    [pscustomobject]@{ X = $origin.X; Y = $origin.Y; Width = $client.Right - $client.Left; Height = $client.Bottom - $client.Top }
}

function Wait-GameWindow([System.Diagnostics.Process]$GameProcess, [int]$TimeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline -and -not $GameProcess.HasExited) {
        $GameProcess.Refresh()
        $window = $GameProcess.MainWindowHandle
        if ($window -ne [IntPtr]::Zero -and (Get-ClientArea $window).Width -gt 0) { return $window }
        Start-Sleep -Milliseconds 250
    }
    return [IntPtr]::Zero
}

function Move-GameWindowOntoPrimary([IntPtr]$window) {
    $screen = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    $frame = New-Object HarnessGameWindow+Rect
    [HarnessGameWindow]::GetWindowRect($window, [ref]$frame) | Out-Null
    $frameWidth = $frame.Right - $frame.Left
    $frameHeight = $frame.Bottom - $frame.Top
    $left = $screen.X + [Math]::Max(0, [int](($screen.Width - $frameWidth) / 2))
    $top = $screen.Y + [Math]::Max(0, [int](($screen.Height - $frameHeight) / 2))
    [HarnessGameWindow]::SetWindowPos($window, $script:topmostWindow, $left, $top, 0, 0, $script:noSizeAndShow) | Out-Null
    [HarnessGameWindow]::SetForegroundWindow($window) | Out-Null
    $area = Get-ClientArea $window
    if ($area.X -lt $screen.X -or $area.Y -lt $screen.Y -or $area.X + $area.Width -gt $screen.Right -or $area.Y + $area.Height -gt $screen.Bottom) {
        throw "The game window ($($area.Width)x$($area.Height)) does not fit on the primary monitor ($($screen.Width)x$($screen.Height))."
    }
    [pscustomobject]@{ X = $area.X - $screen.X; Y = $area.Y - $screen.Y; Width = $area.Width - ($area.Width % 2); Height = $area.Height - ($area.Height % 2) }
}

function Start-FfmpegProcess([string]$Ffmpeg, [string[]]$Arguments) {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $Ffmpeg
    $startInfo.Arguments = ($Arguments | ForEach-Object { if ($_ -match '[\s;]') { '"' + $_ + '"' } else { $_ } }) -join ' '
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardInput = $true
    $startInfo.CreateNoWindow = $true
    [System.Diagnostics.Process]::Start($startInfo)
}

function Start-HarnessRecording {
    param([System.Diagnostics.Process]$GameProcess, [string]$Resolution, [string]$RunDir, [int]$Fps, [switch]$ShowCursor, [int]$WindowTimeoutSeconds = 180)

    [HarnessGameWindow]::SetProcessDPIAware() | Out-Null
    $window = Wait-GameWindow $GameProcess $WindowTimeoutSeconds
    if ($window -eq [IntPtr]::Zero) {
        Write-Warning "The game window never appeared, so nothing is recorded."
        return $null
    }
    $area = Move-GameWindowOntoPrimary $window
    if ("$($area.Width)x$($area.Height)" -ne $Resolution) {
        Write-Warning "The game draws $($area.Width)x$($area.Height) on screen, not $Resolution; Windows display scaling may be stretching it. Recording what is on screen."
    }
    $ffmpeg = Find-Ffmpeg
    $file = Join-Path $RunDir "recording.mkv"
    $grab = "ddagrab=output_idx=0:framerate=${Fps}:offset_x=$($area.X):offset_y=$($area.Y):video_size=$($area.Width)x$($area.Height):draw_mouse=$([int][bool]$ShowCursor)"
    $encoders = @(
        @("-c:v", "h264_nvenc", "-preset", "p5", "-cq", "18"),
        @("-vf", "hwdownload,format=bgra", "-c:v", "libx264", "-preset", "veryfast", "-crf", "18", "-pix_fmt", "yuv420p")
    )
    foreach ($encoder in $encoders) {
        $startMillis = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        $process = Start-FfmpegProcess $ffmpeg (@("-hide_banner", "-loglevel", "error", "-nostats", "-y", "-f", "lavfi", "-i", $grab) + $encoder + @("-fps_mode", "cfr", "-r", "$Fps", "-g", "$Fps", $file))
        if (-not $process.WaitForExit(2000)) {
            Write-Host "Recording the game window ($($area.Width)x$($area.Height) at $Fps fps, $($encoder[1]))"
            return [pscustomobject]@{ Process = $process; Ffmpeg = $ffmpeg; File = $file; StartMillis = $startMillis; StopMillis = 0L; ZeroMillis = $startMillis; Fps = $Fps }
        }
    }
    Write-Warning "ffmpeg could not record the game window, so nothing is recorded."
    return $null
}

function Stop-HarnessRecording($Recording) {
    $Recording.StopMillis = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    if (-not $Recording.Process.HasExited) {
        $Recording.Process.StandardInput.Write("q")
        $Recording.Process.StandardInput.Close()
        if (-not $Recording.Process.WaitForExit(30000)) { $Recording.Process.Kill() }
    }
    if (-not (Test-Path -LiteralPath $Recording.File)) { return $null }
    $ffprobe = Join-Path (Split-Path $Recording.Ffmpeg) "ffprobe.exe"
    $seconds = 0.0
    $durationText = & $ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 $Recording.File
    if ([double]::TryParse(("$durationText").Trim(), [System.Globalization.NumberStyles]::Float, [System.Globalization.CultureInfo]::InvariantCulture, [ref]$seconds) -and $seconds -gt 0) {
        $Recording.ZeroMillis = $Recording.StopMillis - [long]($seconds * 1000)
    }
    $mp4 = [System.IO.Path]::ChangeExtension($Recording.File, ".mp4")
    & $Recording.Ffmpeg -hide_banner -loglevel error -y -i $Recording.File -c copy -movflags +faststart $mp4
    if ($LASTEXITCODE -eq 0) {
        Remove-Item -LiteralPath $Recording.File -Force
        $Recording.File = $mp4
    }
    Write-Host "Recording $($Recording.File) ($([Math]::Round($seconds)) s)"
    return $Recording.File
}

function Export-HarnessClips($Recording, $Clips, [string]$RunDir, [double]$PadSeconds = 0.5) {
    $clipList = @($Clips | Where-Object { $_ })
    if ($clipList.Count -eq 0) { return }
    $clipDir = Join-Path $RunDir "clips"
    New-Item -ItemType Directory -Force -Path $clipDir | Out-Null
    $number = 0
    foreach ($clip in $clipList) {
        $number++
        $startSeconds = [Math]::Max(0.0, ([long]$clip.startMillis - $Recording.ZeroMillis) / 1000.0 - $PadSeconds)
        $lengthSeconds = ([long]$clip.endMillis - [long]$clip.startMillis) / 1000.0 + 2 * $PadSeconds
        $safeName = ($clip.name -replace '[^A-Za-z0-9_-]', '_')
        $clipFile = Join-Path $clipDir ("{0:D2}_{1}.mp4" -f $number, $safeName)
        $start = $startSeconds.ToString("0.###", [System.Globalization.CultureInfo]::InvariantCulture)
        $length = $lengthSeconds.ToString("0.###", [System.Globalization.CultureInfo]::InvariantCulture)
        & $Recording.Ffmpeg -hide_banner -loglevel error -y -ss $start -i $Recording.File -t $length -an -fps_mode cfr -r $Recording.Fps -c:v h264_nvenc -preset p5 -cq 18 -movflags +faststart $clipFile
        if ($LASTEXITCODE -ne 0) {
            & $Recording.Ffmpeg -hide_banner -loglevel error -y -ss $start -i $Recording.File -t $length -an -fps_mode cfr -r $Recording.Fps -c:v libx264 -preset medium -crf 18 -pix_fmt yuv420p -movflags +faststart $clipFile
        }
        Write-Host ("Clip {0} ({1} s)" -f $clipFile, [Math]::Round($lengthSeconds, 1))
    }
}
