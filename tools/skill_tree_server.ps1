$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$port = 8791
$toolsDir = $PSScriptRoot
$projectRoot = Split-Path -Parent $toolsDir
$editorPath = Join-Path $toolsDir "skill_tree_editor.html"
$typesPath = Join-Path $projectRoot "data\skilltrees\skill_types.json"
$treePath = Join-Path $projectRoot "data\skilltrees\ship_skill_tree.json"
$socketablesPath = Join-Path $projectRoot "data\config\exiledSector\socketables.csv"
$socketableAffixesPath = Join-Path $projectRoot "data\config\exiledSector\socketable_affixes.csv"
$socketableNamesPath = Join-Path $projectRoot "data\config\exiledSector\socketable_names.json"
$socketableSalvagePath = Join-Path $projectRoot "data\config\exiledSector\socketable_salvage.csv"
$vanillaCoreDir = Join-Path (Split-Path -Parent $projectRoot) "Starsector\starsector-core"
$vanillaPrefix = [System.IO.Path]::GetFullPath($vanillaCoreDir).TrimEnd('\') + '\'
$vanillaCargoIconsDir = Join-Path $vanillaCoreDir "graphics\icons\cargo"
$staticImagesDir = Join-Path $projectRoot "graphics\backgrounds\static_images"
$graphicsDir = Join-Path $projectRoot "graphics"
$editorHost = "localhost:$port"
$editorOrigin = "http://$editorHost"
$fullProjectRoot = [System.IO.Path]::GetFullPath($projectRoot)
$projectPrefix = $fullProjectRoot.TrimEnd('\') + '\'

function Resolve-ProjectPath($relPath) {
    if (-not $relPath) { return $null }
    $full = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $relPath))
    if ($full.StartsWith($projectPrefix, [StringComparison]::OrdinalIgnoreCase)) { return $full }
    return $null
}

function Resolve-VanillaPath($relPath) {
    if (-not $relPath -or -not (Test-Path $vanillaCoreDir)) { return $null }
    $full = [System.IO.Path]::GetFullPath((Join-Path $vanillaCoreDir $relPath))
    if ($full.StartsWith($vanillaPrefix, [StringComparison]::OrdinalIgnoreCase)) { return $full }
    return $null
}

function Test-EditorPost($request) {
    if ($request.Headers["X-Skill-Tree-Editor"] -ne "1" -or $request.UserHostName -ne $editorHost) { return $false }
    $origin = $request.Headers["Origin"]
    return (-not $origin) -or $origin -eq $editorOrigin
}

function HueToRgbChannel($p, $q, $t) {
    if ($t -lt 0) { $t += 1 }
    if ($t -gt 1) { $t -= 1 }
    if ($t -lt (1.0/6)) { return $p + ($q - $p) * 6 * $t }
    if ($t -lt 0.5) { return $q }
    if ($t -lt (2.0/3)) { return $p + ($q - $p) * (2.0/3 - $t) * 6 }
    return $p
}

function ColorFromAhsl($a, $h, $s, $l) {
    if ($s -le 0) {
        $v = [int]([math]::Round($l * 255))
        return [System.Drawing.Color]::FromArgb($a, $v, $v, $v)
    }
    $q = if ($l -lt 0.5) { $l * (1 + $s) } else { $l + $s - $l * $s }
    $p = 2 * $l - $q
    $hk = ((($h % 360) + 360) % 360) / 360.0
    $r = HueToRgbChannel $p $q ($hk + 1.0/3)
    $g = HueToRgbChannel $p $q $hk
    $b = HueToRgbChannel $p $q ($hk - 1.0/3)
    return [System.Drawing.Color]::FromArgb($a, [int]([math]::Round($r*255)), [int]([math]::Round($g*255)), [int]([math]::Round($b*255)))
}

function HueShiftImage($srcPath, $destPath, $hueShift) {
    $src = New-Object System.Drawing.Bitmap($srcPath)
    try {
        $out = New-Object System.Drawing.Bitmap($src.Width, $src.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        for ($y = 0; $y -lt $src.Height; $y++) {
            for ($x = 0; $x -lt $src.Width; $x++) {
                $c = $src.GetPixel($x, $y)
                if ($c.A -eq 0) {
                    $out.SetPixel($x, $y, $c)
                    continue
                }
                $h = $c.GetHue() + $hueShift
                $out.SetPixel($x, $y, (ColorFromAhsl $c.A $h $c.GetSaturation() $c.GetBrightness()))
            }
        }
        $out.Save($destPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $out.Dispose()
    } finally {
        $src.Dispose()
    }
}

function MakeCircularImage($srcPath, $destPath) {
    $src = New-Object System.Drawing.Bitmap($srcPath)
    try {
        $out = New-Object System.Drawing.Bitmap($src.Width, $src.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($out)
        try {
            $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
            $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
            $brush = New-Object System.Drawing.TextureBrush($src, [System.Drawing.Drawing2D.WrapMode]::Clamp)
            try {
                $g.FillEllipse($brush, 0, 0, $src.Width, $src.Height)
            } finally {
                $brush.Dispose()
            }
        } finally {
            $g.Dispose()
        }
        $out.Save($destPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $out.Dispose()
    } finally {
        $src.Dispose()
    }
}


function Write-JsonResponse($response, $statusCode, $payload) {
    $response.StatusCode = $statusCode
    $response.ContentType = "application/json; charset=utf-8"
    $bytes = [System.Text.Encoding]::UTF8.GetBytes(($payload | ConvertTo-Json))
    $response.OutputStream.Write($bytes, 0, $bytes.Length)
}

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:$port/")
$listener.Start()
Write-Host "Skill tree editor running at http://localhost:$port/ - open that URL in your browser."
Write-Host "Saves write directly to:"
Write-Host "  $typesPath"
Write-Host "  $treePath"
Write-Host "Press Ctrl+C to stop."

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response
        try {
            if ($request.HttpMethod -ne "GET" -and -not (Test-EditorPost $request)) {
                Write-JsonResponse $response 403 @{ ok = $false; message = "Rejected: only the skill tree editor page may change files." }
            }
            elseif ($request.HttpMethod -eq "GET" -and ($request.Url.LocalPath -eq "/" -or $request.Url.LocalPath -eq "/index.html")) {
                $bytes = [System.IO.File]::ReadAllBytes($editorPath)
                $response.ContentType = "text/html; charset=utf-8"
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
            }
            elseif ($request.HttpMethod -eq "GET" -and $request.Url.LocalPath -eq "/data/types") {
                $bytes = [System.IO.File]::ReadAllBytes($typesPath)
                $response.ContentType = "application/json; charset=utf-8"
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
            }
            elseif ($request.HttpMethod -eq "GET" -and $request.Url.LocalPath -eq "/data/tree") {
                $bytes = [System.IO.File]::ReadAllBytes($treePath)
                $response.ContentType = "application/json; charset=utf-8"
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
            }
            elseif ($request.HttpMethod -eq "POST" -and $request.Url.LocalPath -eq "/save") {
                $reader = New-Object System.IO.StreamReader($request.InputStream, [System.Text.Encoding]::UTF8)
                $bodyText = $reader.ReadToEnd()
                $body = $bodyText | ConvertFrom-Json

                $typesOk = $true
                $treeOk = $true
                try { $null = $body.types | ConvertFrom-Json } catch { $typesOk = $false }
                try { $null = $body.tree | ConvertFrom-Json } catch { $treeOk = $false }

                if (-not $typesOk -or -not $treeOk) {
                    $parts = @()
                    if (-not $typesOk) { $parts += "skill_types.json body did not parse as JSON" }
                    if (-not $treeOk) { $parts += "ship_skill_tree.json body did not parse as JSON" }
                    Write-JsonResponse $response 400 @{ ok = $false; message = ("Nothing was written - " + ($parts -join "; ") + ".") }
                } else {
                    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
                    [System.IO.File]::WriteAllText($typesPath, $body.types, $utf8NoBom)
                    [System.IO.File]::WriteAllText($treePath, $body.tree, $utf8NoBom)
                    Write-JsonResponse $response 200 @{ ok = $true; message = "Saved." }
                }
            }
            elseif ($request.HttpMethod -eq "GET" -and $request.Url.LocalPath -eq "/data/socketables") {
                $bytes = [System.IO.File]::ReadAllBytes($socketablesPath)
                $response.ContentType = "text/csv; charset=utf-8"
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
            }
            elseif ($request.HttpMethod -eq "POST" -and $request.Url.LocalPath -eq "/save-socketables") {
                $reader = New-Object System.IO.StreamReader($request.InputStream, [System.Text.Encoding]::UTF8)
                $body = $reader.ReadToEnd() | ConvertFrom-Json
                $csv = [string]$body.csv
                if (-not $csv.StartsWith("id,")) {
                    Write-JsonResponse $response 400 @{ ok = $false; message = "Nothing was written - the CSV must start with the id column." }
                } else {
                    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
                    [System.IO.File]::WriteAllText($socketablesPath, $csv, $utf8NoBom)
                    Write-JsonResponse $response 200 @{ ok = $true; message = "Saved." }
                }
            }
            elseif ($request.HttpMethod -eq "GET" -and $request.Url.LocalPath -eq "/data/socketable-names") {
                $payload = @{
                    affixes = [System.IO.File]::ReadAllText($socketableAffixesPath, [System.Text.Encoding]::UTF8)
                    names = [System.IO.File]::ReadAllText($socketableNamesPath, [System.Text.Encoding]::UTF8)
                }
                Write-JsonResponse $response 200 $payload
            }
            elseif ($request.HttpMethod -eq "POST" -and $request.Url.LocalPath -eq "/save-socketable-names") {
                $reader = New-Object System.IO.StreamReader($request.InputStream, [System.Text.Encoding]::UTF8)
                $body = $reader.ReadToEnd() | ConvertFrom-Json
                $affixes = [string]$body.affixes
                $names = [string]$body.names
                $namesOk = $true
                try { $null = $names | ConvertFrom-Json } catch { $namesOk = $false }
                if (-not $affixes.StartsWith("effect,")) {
                    Write-JsonResponse $response 400 @{ ok = $false; message = "Nothing was written - the affix CSV must start with the effect column." }
                } elseif (-not $namesOk) {
                    Write-JsonResponse $response 400 @{ ok = $false; message = "Nothing was written - socketable_names.json did not parse as JSON." }
                } else {
                    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
                    [System.IO.File]::WriteAllText($socketableAffixesPath, $affixes, $utf8NoBom)
                    [System.IO.File]::WriteAllText($socketableNamesPath, $names, $utf8NoBom)
                    Write-JsonResponse $response 200 @{ ok = $true; message = "Saved." }
                }
            }
            elseif ($request.HttpMethod -eq "GET" -and $request.Url.LocalPath -eq "/data/socketable-salvage") {
                $bytes = [System.IO.File]::ReadAllBytes($socketableSalvagePath)
                $response.ContentType = "text/csv; charset=utf-8"
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
            }
            elseif ($request.HttpMethod -eq "POST" -and $request.Url.LocalPath -eq "/save-socketable-salvage") {
                $reader = New-Object System.IO.StreamReader($request.InputStream, [System.Text.Encoding]::UTF8)
                $body = $reader.ReadToEnd() | ConvertFrom-Json
                $csv = [string]$body.csv
                if (-not $csv.StartsWith("site,")) {
                    Write-JsonResponse $response 400 @{ ok = $false; message = "Nothing was written - the CSV must start with the site column." }
                } else {
                    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
                    [System.IO.File]::WriteAllText($socketableSalvagePath, $csv, $utf8NoBom)
                    Write-JsonResponse $response 200 @{ ok = $true; message = "Saved." }
                }
            }
            elseif ($request.HttpMethod -eq "GET" -and $request.Url.LocalPath -eq "/list-images") {
                $files = @()
                if (Test-Path $staticImagesDir) {
                    $files = Get-ChildItem -Path $staticImagesDir -Filter "*.png" -File |
                        Sort-Object Name |
                        ForEach-Object { "graphics/backgrounds/static_images/" + $_.Name }
                }
                $json = ConvertTo-Json -InputObject @($files)
                $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
                $response.ContentType = "application/json; charset=utf-8"
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
            }
            elseif ($request.HttpMethod -eq "GET" -and $request.Url.LocalPath -eq "/list-all-images") {
                $files = @()
                if (Test-Path $graphicsDir) {
                    $files = Get-ChildItem -Path $graphicsDir -Filter "*.png" -File -Recurse |
                        ForEach-Object {
                            $rel = $_.FullName.Substring($projectRoot.Length + 1) -replace '\\', '/'
                            $rel
                        } | Sort-Object
                }
                if (Test-Path $vanillaCargoIconsDir) {
                    $files = @($files) + @(Get-ChildItem -Path $vanillaCargoIconsDir -Filter "*.png" -File |
                        Sort-Object Name |
                        ForEach-Object { "graphics/icons/cargo/" + $_.Name })
                }
                $json = ConvertTo-Json -InputObject @($files)
                $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
                $response.ContentType = "application/json; charset=utf-8"
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
            }
            elseif ($request.HttpMethod -eq "POST" -and $request.Url.LocalPath -eq "/colorshift") {
                $reader = New-Object System.IO.StreamReader($request.InputStream, [System.Text.Encoding]::UTF8)
                $bodyText = $reader.ReadToEnd()
                $body = $bodyText | ConvertFrom-Json

                $relPath = [string]$body.path
                $suffix = ([string]$body.suffix).Trim()
                $hueShift = 0
                try { $hueShift = [double]$body.hueShift } catch { $hueShift = 0 }

                $srcFull = Resolve-ProjectPath $relPath

                if (-not $srcFull -or -not (Test-Path $srcFull -PathType Leaf)) {
                    Write-JsonResponse $response 400 @{ ok = $false; message = "Source image not found." }
                } elseif (-not $suffix -or $suffix -match '[\\/:]') {
                    Write-JsonResponse $response 400 @{ ok = $false; message = "Suffix is required and cannot contain path separators." }
                } else {
                    $dir = [System.IO.Path]::GetDirectoryName($srcFull)
                    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($srcFull)
                    $destFull = Join-Path $dir ($baseName + "_" + $suffix + ".png")
                    try {
                        HueShiftImage $srcFull $destFull $hueShift
                        $destRel = $destFull.Substring($fullProjectRoot.Length + 1) -replace '\\', '/'
                        Write-JsonResponse $response 200 @{ ok = $true; path = $destRel }
                    } catch {
                        Write-JsonResponse $response 500 @{ ok = $false; message = $_.Exception.Message }
                    }
                }
            }
            elseif ($request.HttpMethod -eq "POST" -and $request.Url.LocalPath -eq "/make-circular") {
                $reader = New-Object System.IO.StreamReader($request.InputStream, [System.Text.Encoding]::UTF8)
                $bodyText = $reader.ReadToEnd()
                $body = $bodyText | ConvertFrom-Json

                $relPath = [string]$body.path
                $suffix = ([string]$body.suffix).Trim()

                $srcFull = Resolve-ProjectPath $relPath

                if (-not $srcFull -or -not (Test-Path $srcFull -PathType Leaf)) {
                    Write-JsonResponse $response 400 @{ ok = $false; message = "Source image not found." }
                } elseif (-not $suffix -or $suffix -match '[\\/:]') {
                    Write-JsonResponse $response 400 @{ ok = $false; message = "Suffix is required and cannot contain path separators." }
                } else {
                    $dir = Join-Path $projectRoot "graphics\unused\circular"
                    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
                    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($srcFull)
                    $destFull = Join-Path $dir ($baseName + "_" + $suffix + ".png")
                    try {
                        MakeCircularImage $srcFull $destFull
                        $destRel = $destFull.Substring($fullProjectRoot.Length + 1) -replace '\\', '/'
                        Write-JsonResponse $response 200 @{ ok = $true; path = $destRel }
                    } catch {
                        Write-JsonResponse $response 500 @{ ok = $false; message = $_.Exception.Message }
                    }
                }
            }
            elseif ($request.HttpMethod -eq "POST" -and $request.Url.LocalPath -eq "/move-image") {
                $reader = New-Object System.IO.StreamReader($request.InputStream, [System.Text.Encoding]::UTF8)
                $bodyText = $reader.ReadToEnd()
                $body = $bodyText | ConvertFrom-Json

                $fromRel = [string]$body.from
                $toRel = [string]$body.to
                $srcFull = Resolve-ProjectPath $fromRel
                $destFull = Resolve-ProjectPath $toRel

                if (-not $srcFull -or -not $destFull -or -not (Test-Path $srcFull -PathType Leaf)) {
                    Write-JsonResponse $response 400 @{ ok = $false; message = "Source image not found." }
                } elseif (Test-Path $destFull) {
                    Write-JsonResponse $response 400 @{ ok = $false; message = "Destination already exists." }
                } else {
                    try {
                        $destDir = [System.IO.Path]::GetDirectoryName($destFull)
                        if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Force -Path $destDir | Out-Null }
                        Move-Item -Path $srcFull -Destination $destFull
                        $destRel = $destFull.Substring($fullProjectRoot.Length + 1) -replace '\\', '/'
                        Write-JsonResponse $response 200 @{ ok = $true; path = $destRel }
                    } catch {
                        Write-JsonResponse $response 500 @{ ok = $false; message = $_.Exception.Message }
                    }
                }
            }
            elseif ($request.HttpMethod -eq "POST" -and $request.Url.LocalPath -eq "/delete-image") {
                $reader = New-Object System.IO.StreamReader($request.InputStream, [System.Text.Encoding]::UTF8)
                $bodyText = $reader.ReadToEnd()
                $body = $bodyText | ConvertFrom-Json

                $relPath = [string]$body.path
                $srcFull = Resolve-ProjectPath $relPath

                if (-not $srcFull -or -not (Test-Path $srcFull -PathType Leaf)) {
                    Write-JsonResponse $response 400 @{ ok = $false; message = "Image not found." }
                } else {
                    try {
                        Remove-Item -Path $srcFull -Force
                        Write-JsonResponse $response 200 @{ ok = $true }
                    } catch {
                        Write-JsonResponse $response 500 @{ ok = $false; message = $_.Exception.Message }
                    }
                }
            }
            elseif ($request.HttpMethod -eq "GET") {
                $relPath = [Uri]::UnescapeDataString($request.Url.LocalPath.TrimStart('/'))
                $fullFilePath = Resolve-ProjectPath $relPath
                if (-not $fullFilePath -or -not (Test-Path $fullFilePath -PathType Leaf)) {
                    $fullFilePath = Resolve-VanillaPath $relPath
                }
                if ($fullFilePath -and (Test-Path $fullFilePath -PathType Leaf)) {
                    $ext = [System.IO.Path]::GetExtension($fullFilePath).ToLowerInvariant()
                    $contentType = switch ($ext) {
                        ".png" { "image/png" }
                        ".jpg" { "image/jpeg" }
                        ".jpeg" { "image/jpeg" }
                        default { "application/octet-stream" }
                    }
                    $bytes = [System.IO.File]::ReadAllBytes($fullFilePath)
                    $response.ContentType = $contentType
                    $response.OutputStream.Write($bytes, 0, $bytes.Length)
                } else {
                    $response.StatusCode = 404
                }
            }
            else {
                $response.StatusCode = 404
            }
        } catch {
            try { Write-JsonResponse $response 500 @{ ok = $false; message = $_.Exception.Message } } catch {}
        } finally {
            $response.OutputStream.Close()
        }
    }
} finally {
    $listener.Stop()
}
