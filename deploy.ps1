$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$mainCheckout = "E:\Dev\ExiledSector"
$modTarget   = "E:\Dev\Starsector\mods\ExiledSector"

function Get-GitPath([string]$Flag) {
    $value = git -C $projectRoot rev-parse $Flag
    if ($LASTEXITCODE -ne 0 -or -not $value) { throw "git rev-parse $Flag failed in $projectRoot" }
    return [System.IO.Path]::GetFullPath([System.IO.Path]::Combine($projectRoot, $value)).TrimEnd('\')
}

$resolvedRoot = [System.IO.Path]::GetFullPath($projectRoot).TrimEnd('\')
if ($resolvedRoot -ne $mainCheckout -or (Get-GitPath "--git-dir") -ne (Get-GitPath "--git-common-dir")) {
    throw "deploy.ps1 only runs from the main checkout $mainCheckout, not from $resolvedRoot (git worktrees and other checkouts must not overwrite the deployed mod)."
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
