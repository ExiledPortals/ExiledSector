$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$deployCheckouts = @("E:\Dev\ExiledSector-parallel-work", "E:\Dev\ExiledSector")
$modTarget   = "E:\Dev\Starsector\mods\ExiledSector"

$resolvedRoot = [System.IO.Path]::GetFullPath($projectRoot).TrimEnd('\')
if ($deployCheckouts -notcontains $resolvedRoot) {
    throw "deploy.ps1 only runs from $($deployCheckouts -join ' or '), not from $resolvedRoot (other worktrees and checkouts must not overwrite the deployed mod)."
}

. (Join-Path $projectRoot "build-common.ps1")

Invoke-ModBuild -ProjectRoot $projectRoot

$stageDir = Join-Path ([System.IO.Path]::GetTempPath()) ("ExiledSector-deploy-" + [Guid]::NewGuid().ToString("N"))
try {
    Copy-ModFiles -ProjectRoot $projectRoot -Destination $stageDir
    Sync-FolderMirror -Source $stageDir -Destination $modTarget
} finally {
    if (Test-Path -LiteralPath $stageDir) {
        [System.IO.Directory]::Delete($stageDir, $true)
    }
}

Write-Host "Deployed ExiledSector to $modTarget"
