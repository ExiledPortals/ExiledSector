param([int]$Port = 8791)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$toolsDir = $PSScriptRoot
$projectRoot = Split-Path -Parent $toolsDir
$editorPath = Join-Path $toolsDir "skill_tree_editor.html"
$vocabularyPath = Join-Path $toolsDir "editor_vocabulary.json"
$typesPath = Join-Path $projectRoot "data\skilltrees\skill_types.json"
$treePath = Join-Path $projectRoot "data\skilltrees\ship_skill_tree.json"
$socketablesPath = Join-Path $projectRoot "data\config\exiledSector\socketables.csv"
$socketableAffixesPath = Join-Path $projectRoot "data\config\exiledSector\socketable_affixes.csv"
$socketableNamesPath = Join-Path $projectRoot "data\config\exiledSector\socketable_names.json"
$socketableSalvagePath = Join-Path $projectRoot "data\config\exiledSector\socketable_salvage.csv"
$socketableSalvageCompatDir = Join-Path $projectRoot "data\config\exiledSector\compat\salvage"
$socketableCraftingPath = Join-Path $projectRoot "data\config\exiledSector\socketable_crafting.csv"
$commoditiesPath = Join-Path $projectRoot "data\campaign\commodities.csv"

function Resolve-SalvagePath($file) {
    if (-not $file -or $file -eq "socketable_salvage.csv") { return $socketableSalvagePath }
    if ($file -match '^compat/salvage/[A-Za-z0-9_.-]+\.csv$') {
        return Join-Path $socketableSalvageCompatDir ($file.Substring("compat/salvage/".Length))
    }
    return $null
}
$vanillaCoreDir = Join-Path (Split-Path -Parent $projectRoot) "Starsector\starsector-core"
$vanillaPrefix = [System.IO.Path]::GetFullPath($vanillaCoreDir).TrimEnd('\') + '\'
$vanillaCargoIconsDir = Join-Path $vanillaCoreDir "graphics\icons\cargo"
$staticImagesDir = Join-Path $projectRoot "graphics\backgrounds\static_images"
$graphicsDir = Join-Path $projectRoot "graphics"
$editorHost = "localhost:$Port"
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

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Write-TextFilesAtomic($writes) {
    $staged = @()
    try {
        foreach ($write in $writes) {
            $fullPath = [System.IO.Path]::GetFullPath($write.Path)
            $directory = [System.IO.Path]::GetDirectoryName($fullPath)
            $tempPath = Join-Path $directory ("." + [System.IO.Path]::GetFileName($fullPath) + "." + [Guid]::NewGuid().ToString("N") + ".tmp")
            $staged += [pscustomobject]@{ TempPath = $tempPath; TargetPath = $fullPath }
            [System.IO.File]::WriteAllText($tempPath, $write.Content, $utf8NoBom)
        }
        foreach ($file in $staged) {
            if ([System.IO.File]::Exists($file.TargetPath)) {
                [System.IO.File]::Replace($file.TempPath, $file.TargetPath, [NullString]::Value)
            } else {
                [System.IO.File]::Move($file.TempPath, $file.TargetPath)
            }
        }
    } finally {
        foreach ($file in $staged) {
            if ([System.IO.File]::Exists($file.TempPath)) { [System.IO.File]::Delete($file.TempPath) }
        }
    }
}

function Get-BytesHash($bytes) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        return [BitConverter]::ToString($sha.ComputeHash($bytes)).Replace("-", "").ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
}

function Get-FileContentHash($path) {
    if (-not [System.IO.File]::Exists($path)) { return "" }
    return Get-BytesHash ([System.IO.File]::ReadAllBytes($path))
}

function Read-DataFile($path) {
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $text = [System.Text.Encoding]::UTF8.GetString($bytes)
    if ($text.Length -gt 0 -and $text[0] -eq [char]0xFEFF) { $text = $text.Substring(1) }
    return @{ content = $text; hash = (Get-BytesHash $bytes) }
}

function Test-JsonArrayField($text, $field) {
    if ([string]::IsNullOrWhiteSpace($text)) { return "it is empty" }
    try { $parsed = $text | ConvertFrom-Json } catch { return "it did not parse as JSON" }
    if ($null -eq $parsed -or $parsed -is [System.Array] -or $null -eq $parsed.PSObject.Properties[$field]) { return "it has no $field array" }
    $values = $parsed.PSObject.Properties[$field].Value
    if ($null -eq $values -or $values -is [string] -or @($values).Count -eq 0) { return "its $field array is empty" }
    return $null
}

function Test-JsonObject($text) {
    if ([string]::IsNullOrWhiteSpace($text)) { return "it is empty" }
    try { $parsed = $text | ConvertFrom-Json } catch { return "it did not parse as JSON" }
    if ($parsed -isnot [System.Management.Automation.PSCustomObject]) { return "it is not a JSON object" }
    return $null
}

function Test-CsvHeader($text, $prefix, $columns) {
    if (-not ([string]$text).StartsWith($prefix)) { return "it must start with $columns" }
    return $null
}

$dataResources = @{
    "types" = @{ Path = $typesPath; Check = { param($text) Test-JsonArrayField $text "skillTypes" } }
    "tree" = @{ Path = $treePath; Check = { param($text) Test-JsonArrayField $text "nodes" } }
    "socketables" = @{ Path = $socketablesPath; Check = { param($text) Test-CsvHeader $text "id," "the id column" } }
    "socketable-affixes" = @{ Path = $socketableAffixesPath; Check = { param($text) Test-CsvHeader $text "effect," "the effect column" } }
    "socketable-names" = @{ Path = $socketableNamesPath; Check = { param($text) Test-JsonObject $text } }
    "socketable-crafting" = @{ Path = $socketableCraftingPath; Check = { param($text) Test-CsvHeader $text "item," "the item column" } }
    "commodities" = @{ Path = $commoditiesPath; Check = { param($text) Test-CsvHeader $text "name,id," "the name and id columns" } }
}

function Resolve-DataResource($name) {
    if (-not $name) { return $null }
    if ($dataResources.ContainsKey($name)) { return $dataResources[$name] }
    if ($name.StartsWith("salvage:")) {
        $path = Resolve-SalvagePath $name.Substring("salvage:".Length)
        if ($path) { return @{ Path = $path; Check = { param($text) Test-CsvHeader $text "site," "the site column" } } }
    }
    return $null
}

function Write-JsonResponse($response, $statusCode, $payload) {
    $response.StatusCode = $statusCode
    $response.ContentType = "application/json; charset=utf-8"
    $bytes = [System.Text.Encoding]::UTF8.GetBytes((ConvertTo-Json -InputObject $payload -Depth 6))
    $response.OutputStream.Write($bytes, 0, $bytes.Length)
}

function Write-JsonArrayResponse($response, $items) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes((ConvertTo-Json -InputObject @($items)))
    $response.ContentType = "application/json; charset=utf-8"
    $response.OutputStream.Write($bytes, 0, $bytes.Length)
}

function Write-FileResponse($response, $path, $contentType) {
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $response.ContentType = $contentType
    $response.OutputStream.Write($bytes, 0, $bytes.Length)
}

function Read-JsonBody($request) {
    $reader = New-Object System.IO.StreamReader($request.InputStream, [System.Text.Encoding]::UTF8)
    return $reader.ReadToEnd() | ConvertFrom-Json
}

function Get-ProjectRelativePath($fullPath) {
    return $fullPath.Substring($fullProjectRoot.Length + 1) -replace '\\', '/'
}

function Test-NewImageDestination($response, $destFull, $overwrite) {
    if ($overwrite -eq $true -or -not (Test-Path -LiteralPath $destFull)) { return $true }
    $destination = Get-ProjectRelativePath $destFull
    Write-JsonResponse $response 409 @{ ok = $false; exists = $true; path = $destination; message = "$destination already exists." }
    return $false
}

$serveEditor = {
    param($request, $response)
    Write-FileResponse $response $editorPath "text/html; charset=utf-8"
}

$routes = @{
    "GET /" = $serveEditor
    "GET /index.html" = $serveEditor

    "GET /data/vocabulary" = {
        param($request, $response)
        Write-FileResponse $response $vocabularyPath "application/json; charset=utf-8"
    }

    "GET /data/files" = {
        param($request, $response)
        $names = @(([string]$request.QueryString["names"]).Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ })
        $files = @{}
        foreach ($name in $names) {
            $resource = Resolve-DataResource $name
            if (-not $resource -or -not [System.IO.File]::Exists($resource.Path)) {
                Write-JsonResponse $response 404 @{ ok = $false; message = "Unknown data file: $name" }
                return
            }
            $files[$name] = Read-DataFile $resource.Path
        }
        Write-JsonResponse $response 200 @{ ok = $true; files = $files }
    }

    "POST /save/files" = {
        param($request, $response)
        $body = Read-JsonBody $request
        $entries = if ($body -and $body.files) { @($body.files.PSObject.Properties) } else { @() }
        if ($entries.Count -eq 0) {
            Write-JsonResponse $response 400 @{ ok = $false; message = "Nothing was written - the request named no files." }
            return
        }
        $writes = @()
        $problems = @()
        $changed = @()
        foreach ($entry in $entries) {
            $resource = Resolve-DataResource $entry.Name
            if (-not $resource) {
                $problems += "unknown data file $($entry.Name)"
                continue
            }
            $label = Get-ProjectRelativePath ([System.IO.Path]::GetFullPath($resource.Path))
            $content = [string]$entry.Value.content
            $reason = & $resource.Check $content
            if ($reason) {
                $problems += "$label was refused because $reason"
            } elseif ($null -eq $entry.Value.baseHash -or [string]$entry.Value.baseHash -ne (Get-FileContentHash $resource.Path)) {
                $changed += $label
            } else {
                $writes += [pscustomobject]@{ Name = $entry.Name; Path = $resource.Path; Content = $content }
            }
        }
        if ($problems.Count -gt 0) {
            Write-JsonResponse $response 400 @{ ok = $false; message = ("Nothing was written - " + ($problems -join "; ") + ".") }
            return
        }
        if ($changed.Count -gt 0) {
            $pronoun = if ($changed.Count -eq 1) { "it" } else { "them" }
            Write-JsonResponse $response 409 @{ ok = $false; conflict = $true; changed = $changed
                message = ("Nothing was written - " + ($changed -join ", ") + " changed on disk since the editor loaded $pronoun.") }
            return
        }
        Write-TextFilesAtomic $writes
        $hashes = @{}
        foreach ($write in $writes) { $hashes[$write.Name] = Get-FileContentHash $write.Path }
        Write-JsonResponse $response 200 @{ ok = $true; message = "Saved."; hashes = $hashes }
    }

    "GET /list-socketable-salvage" = {
        param($request, $response)
        $files = @("socketable_salvage.csv")
        if (Test-Path -LiteralPath $socketableSalvageCompatDir) {
            $files += Get-ChildItem -LiteralPath $socketableSalvageCompatDir -Filter "*.csv" -File | Sort-Object Name |
                ForEach-Object { "compat/salvage/" + $_.Name }
        }
        Write-JsonArrayResponse $response $files
    }

    "GET /list-images" = {
        param($request, $response)
        $files = @()
        if (Test-Path -LiteralPath $staticImagesDir) {
            $files = Get-ChildItem -LiteralPath $staticImagesDir -Filter "*.png" -File |
                Sort-Object Name |
                ForEach-Object { "graphics/backgrounds/static_images/" + $_.Name }
        }
        Write-JsonArrayResponse $response $files
    }

    "GET /list-all-images" = {
        param($request, $response)
        $files = @()
        if (Test-Path -LiteralPath $graphicsDir) {
            $files = Get-ChildItem -LiteralPath $graphicsDir -Filter "*.png" -File -Recurse |
                ForEach-Object { $_.FullName.Substring($projectRoot.Length + 1) -replace '\\', '/' } |
                Sort-Object
        }
        if (Test-Path -LiteralPath $vanillaCargoIconsDir) {
            $files = @($files) + @(Get-ChildItem -LiteralPath $vanillaCargoIconsDir -Filter "*.png" -File |
                Sort-Object Name |
                ForEach-Object { "graphics/icons/cargo/" + $_.Name })
        }
        Write-JsonArrayResponse $response $files
    }

    "POST /colorshift" = {
        param($request, $response)
        $body = Read-JsonBody $request
        $suffix = ([string]$body.suffix).Trim()
        $hueShift = 0
        try { $hueShift = [double]$body.hueShift } catch { $hueShift = 0 }
        $srcFull = Resolve-ProjectPath ([string]$body.path)
        if (-not $srcFull -or -not (Test-Path -LiteralPath $srcFull -PathType Leaf)) {
            Write-JsonResponse $response 400 @{ ok = $false; message = "Source image not found." }
            return
        }
        if (-not $suffix -or $suffix -match '[\\/:]') {
            Write-JsonResponse $response 400 @{ ok = $false; message = "Suffix is required and cannot contain path separators." }
            return
        }
        $dir = [System.IO.Path]::GetDirectoryName($srcFull)
        $baseName = [System.IO.Path]::GetFileNameWithoutExtension($srcFull)
        $destFull = Join-Path $dir ($baseName + "_" + $suffix + ".png")
        if (-not (Test-NewImageDestination $response $destFull $body.overwrite)) { return }
        try {
            HueShiftImage $srcFull $destFull $hueShift
            Write-JsonResponse $response 200 @{ ok = $true; path = (Get-ProjectRelativePath $destFull) }
        } catch {
            Write-JsonResponse $response 500 @{ ok = $false; message = $_.Exception.Message }
        }
    }

    "POST /make-circular" = {
        param($request, $response)
        $body = Read-JsonBody $request
        $suffix = ([string]$body.suffix).Trim()
        $srcFull = Resolve-ProjectPath ([string]$body.path)
        if (-not $srcFull -or -not (Test-Path -LiteralPath $srcFull -PathType Leaf)) {
            Write-JsonResponse $response 400 @{ ok = $false; message = "Source image not found." }
            return
        }
        if (-not $suffix -or $suffix -match '[\\/:]') {
            Write-JsonResponse $response 400 @{ ok = $false; message = "Suffix is required and cannot contain path separators." }
            return
        }
        $dir = Join-Path $projectRoot "graphics\unused\circular"
        if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
        $baseName = [System.IO.Path]::GetFileNameWithoutExtension($srcFull)
        $destFull = Join-Path $dir ($baseName + "_" + $suffix + ".png")
        if (-not (Test-NewImageDestination $response $destFull $body.overwrite)) { return }
        try {
            MakeCircularImage $srcFull $destFull
            Write-JsonResponse $response 200 @{ ok = $true; path = (Get-ProjectRelativePath $destFull) }
        } catch {
            Write-JsonResponse $response 500 @{ ok = $false; message = $_.Exception.Message }
        }
    }

    "POST /move-image" = {
        param($request, $response)
        $body = Read-JsonBody $request
        $srcFull = Resolve-ProjectPath ([string]$body.from)
        $destFull = Resolve-ProjectPath ([string]$body.to)
        if (-not $srcFull -or -not $destFull -or -not (Test-Path -LiteralPath $srcFull -PathType Leaf)) {
            Write-JsonResponse $response 400 @{ ok = $false; message = "Source image not found." }
            return
        }
        if (Test-Path -LiteralPath $destFull) {
            Write-JsonResponse $response 400 @{ ok = $false; message = "$(Get-ProjectRelativePath $destFull) already exists." }
            return
        }
        try {
            $destDir = [System.IO.Path]::GetDirectoryName($destFull)
            if (-not (Test-Path -LiteralPath $destDir)) { New-Item -ItemType Directory -Force -Path $destDir | Out-Null }
            Move-Item -LiteralPath $srcFull -Destination $destFull
            Write-JsonResponse $response 200 @{ ok = $true; path = (Get-ProjectRelativePath $destFull) }
        } catch {
            Write-JsonResponse $response 500 @{ ok = $false; message = $_.Exception.Message }
        }
    }

    "POST /delete-image" = {
        param($request, $response)
        $body = Read-JsonBody $request
        $srcFull = Resolve-ProjectPath ([string]$body.path)
        if (-not $srcFull -or -not (Test-Path -LiteralPath $srcFull -PathType Leaf)) {
            Write-JsonResponse $response 400 @{ ok = $false; message = "Image not found." }
            return
        }
        try {
            Remove-Item -LiteralPath $srcFull -Force
            Write-JsonResponse $response 200 @{ ok = $true }
        } catch {
            Write-JsonResponse $response 500 @{ ok = $false; message = $_.Exception.Message }
        }
    }
}

function Send-StaticFile($request, $response) {
    $relPath = [Uri]::UnescapeDataString($request.Url.LocalPath.TrimStart('/'))
    $fullFilePath = Resolve-ProjectPath $relPath
    if (-not $fullFilePath -or -not (Test-Path -LiteralPath $fullFilePath -PathType Leaf)) {
        $fullFilePath = Resolve-VanillaPath $relPath
    }
    if (-not $fullFilePath -or -not (Test-Path -LiteralPath $fullFilePath -PathType Leaf)) {
        $response.StatusCode = 404
        return
    }
    $contentType = switch ([System.IO.Path]::GetExtension($fullFilePath).ToLowerInvariant()) {
        ".png" { "image/png" }
        ".jpg" { "image/jpeg" }
        ".jpeg" { "image/jpeg" }
        default { "application/octet-stream" }
    }
    Write-FileResponse $response $fullFilePath $contentType
}

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:$Port/")
$listener.Start()
Write-Host "Skill tree editor running at http://localhost:$Port/ - open that URL in your browser."
Write-Host "Saves write directly to the skill tree, socketable and commodity data files under $fullProjectRoot"
Write-Host "Press Ctrl+C to stop."

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response
        try {
            $routeKey = $request.HttpMethod + " " + $request.Url.LocalPath
            if ($request.HttpMethod -ne "GET" -and -not (Test-EditorPost $request)) {
                Write-JsonResponse $response 403 @{ ok = $false; message = "Rejected: only the skill tree editor page may change files." }
            } elseif ($routes.ContainsKey($routeKey)) {
                & $routes[$routeKey] $request $response
            } elseif ($request.HttpMethod -eq "GET") {
                Send-StaticFile $request $response
            } else {
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
