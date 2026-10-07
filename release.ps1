$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$modFolderName = "ExiledSector"
$zipName = "ExiledSector.zip"
$releaseDir = Join-Path $projectRoot "target\release"
$stageDir = Join-Path $releaseDir $modFolderName
$zipPath = Join-Path $releaseDir $zipName

. (Join-Path $projectRoot "build-common.ps1")

function Get-VersionString {
    param($Version)
    return "$($Version.major).$($Version.minor).$($Version.patch)"
}

$modInfo = Get-Content (Join-Path $projectRoot "mod_info.json") -Raw | ConvertFrom-Json
$versionFile = Get-Content (Join-Path $projectRoot "ExiledSector.version") -Raw | ConvertFrom-Json
$version = Get-VersionString $modInfo.version
$versionFileVersion = Get-VersionString $versionFile.modVersion
if ($version -ne $versionFileVersion) {
    throw "Version mismatch: mod_info.json is $version but ExiledSector.version is $versionFileVersion."
}

$dirty = git -C $projectRoot status --porcelain
if ($dirty) {
    Write-Warning "The working tree has uncommitted changes; the release will include them."
}

Invoke-ModBuild -ProjectRoot $projectRoot

Get-ChildItem (Join-Path $projectRoot "target\i18n") -Filter "missing-*.txt" -ErrorAction SilentlyContinue | ForEach-Object {
    $missing = @(Get-Content $_.FullName | Where-Object { $_ })
    if ($missing.Count -gt 0) {
        $locale = $_.BaseName -replace '^missing-', ''
        Write-Warning "$($missing.Count) strings have no $locale translation and will show in English; see $($_.FullName)."
    }
}

if (Test-Path $releaseDir) {
    Remove-Item -Recurse -Force $releaseDir
}
Copy-ModFiles -ProjectRoot $projectRoot -Destination $stageDir

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($zipPath, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    Get-ChildItem $stageDir -Recurse -File | ForEach-Object {
        $relative = $_.FullName.Substring($releaseDir.Length + 1).Replace('\', '/')
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $_.FullName, $relative,
            [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally {
    $zip.Dispose()
}

$archive = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
try {
    $entries = $archive.Entries | ForEach-Object { $_.FullName }
} finally {
    $archive.Dispose()
}
$required = @("mod_info.json", "ExiledSector.version", "LICENSE","jars/ExiledSector.jar", "data/config/version/version_files.csv")
foreach ($path in $required) {
    if ($entries -notcontains "$modFolderName/$path") {
        throw "Release zip is missing $modFolderName/$path"
    }
}
$outsideModFolder = $entries | Where-Object { -not $_.StartsWith("$modFolderName/") }
if ($outsideModFolder) {
    throw "Release zip has entries outside $modFolderName/: $($outsideModFolder -join ', ')"
}

Save-ReleasedSkillNodes -ProjectRoot $projectRoot
Save-ReleasedSocketables -ProjectRoot $projectRoot -Version $version

$sizeMb = [math]::Round((Get-Item $zipPath).Length / 1MB, 1)
Write-Host "Built $zipPath ($sizeMb MB, version $version)"
Write-Host "Next: tag v$version, create a GitHub release, attach $zipName (keep that exact name), then push ExiledSector.version to main."
