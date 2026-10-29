$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$deployCheckouts = @("E:\Dev\ExiledSector-parallel-work", "E:\Dev\ExiledSector-new-faction-areas", "E:\Dev\ExiledSector")
$modTarget   = "E:\Dev\Starsector\mods\ExiledSector"
$retiredLibraryTarget = "E:\Dev\Starsector\mods\ExiledLib"

$resolvedRoot = [System.IO.Path]::GetFullPath($projectRoot).TrimEnd('\')
if ($deployCheckouts -notcontains $resolvedRoot) {
    throw "deploy.ps1 only runs from $($deployCheckouts -join ' or '), not from $resolvedRoot (other worktrees and checkouts must not overwrite the deployed mod)."
}

. (Join-Path $projectRoot "build-common.ps1")

Invoke-ModBuild -ProjectRoot $projectRoot

$stageDir = Join-Path ([System.IO.Path]::GetTempPath()) ("ExiledSector-deploy-" + [Guid]::NewGuid().ToString("N"))
try {
    Copy-ModFiles -ProjectRoot $projectRoot -Destination (Join-Path $stageDir "ExiledSector")
    Sync-FolderMirror -Source (Join-Path $stageDir "ExiledSector") -Destination $modTarget
} finally {
    if (Test-Path -LiteralPath $stageDir) {
        [System.IO.Directory]::Delete($stageDir, $true)
    }
}

if (Test-Path -LiteralPath (Join-Path $retiredLibraryTarget "jars\ExiledLib.jar")) {
    [System.IO.Directory]::Delete($retiredLibraryTarget, $true)
    Write-Host "Removed the old ExiledLib mod folder; its code is now part of ExiledSector."
}

Write-Host "Deployed ExiledSector to $modTarget"
