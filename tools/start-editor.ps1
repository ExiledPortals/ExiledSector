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
    if (-not (Test-Path (Join-Path $editorDir "node_modules"))) {
        npm install --no-audit --no-fund
    }
    npm run dev
} finally {
    Pop-Location
}
