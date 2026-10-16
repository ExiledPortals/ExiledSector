$ErrorActionPreference = "Stop"
$editorDir = Join-Path $PSScriptRoot "editor"
$nodeDir = "C:\Program Files\nodejs"
if ((Test-Path (Join-Path $nodeDir "node.exe")) -and -not (Get-Command node -ErrorAction SilentlyContinue)) {
    $env:Path = "$nodeDir;$env:Path"
}
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    throw "Node.js is not installed. Install it with: winget install OpenJS.NodeJS.LTS"
}
Push-Location $editorDir
try {
    $lockFile = Get-Item (Join-Path $editorDir "package-lock.json")
    $installedLockPath = Join-Path $editorDir "node_modules\.package-lock.json"
    if (-not (Test-Path $installedLockPath) -or (Get-Item $installedLockPath).LastWriteTime -lt $lockFile.LastWriteTime) {
        npm ci --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) {
            throw "npm ci failed with exit code $LASTEXITCODE"
        }
    }
    npm run dev
} finally {
    Pop-Location
}
