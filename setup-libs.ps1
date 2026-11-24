$ErrorActionPreference = "Stop"

$starsectorCore       = "E:\Dev\Starsector-0.98.5a-RC9\starsector-core"
$oldestStarsectorCore = "E:\Dev\Starsector\starsector-core"
$oldestGameVersion    = "0.98a-RC8"
$devMods              = "E:\Dev\Starsector\mods"
$sourceMods           = "C:\Games\Starsector\mods"
$libs                 = Join-Path $PSScriptRoot "libs"
$oldestGameLibs       = Join-Path $libs $oldestGameVersion

New-Item -ItemType Directory -Force -Path $libs | Out-Null
New-Item -ItemType Directory -Force -Path $oldestGameLibs | Out-Null

$coreJars = @(
    "starfarer.api.jar",
    "starfarer_obf.jar",
    "fs.common_obf.jar",
    "fs.sound_obf.jar",
    "lwjgl.jar",
    "lwjgl_util.jar",
    "json.jar",
    "xstream-1.4.10.jar",
    "log4j-1.2.9.jar"
)

function Copy-GameJars {
    param([string]$GameCore, [string]$Destination, [string]$SettingName)

    foreach ($jar in $coreJars) {
        $coreJar = Join-Path $GameCore $jar
        if (-not (Test-Path -LiteralPath $coreJar)) {
            throw "Missing $coreJar. Install Starsector at $GameCore (or change `$$SettingName in setup-libs.ps1) and run it again."
        }
        Copy-Item -Path $coreJar -Destination $Destination -Force
    }
}

Copy-GameJars -GameCore $starsectorCore -Destination $libs -SettingName "starsectorCore"
Copy-GameJars -GameCore $oldestStarsectorCore -Destination $oldestGameLibs -SettingName "oldestStarsectorCore"

$libJars = @{
    "LazyLib"          = @("LazyLib.jar", "LazyLib-Kotlin.jar", "internal\Kotlin-Runtime.jar")
    "MagicLib"         = @("MagicLib.jar", "MagicLib-Kotlin.jar")
    "LunaLib"          = @("LunaLib.jar", "libs\fuzzywuzzy-1.3.0.jar")
    "Console Commands" = @("lw_Console.jar")
    "GraphicsLib"      = @("Graphics.jar")
}

foreach ($modName in $libJars.Keys) {
    $srcJarsDir = Join-Path $sourceMods "$modName\jars"
    foreach ($jar in $libJars[$modName]) {
        $modJar = Join-Path $srcJarsDir $jar
        if (-not (Test-Path -LiteralPath $modJar)) {
            throw "Missing $modJar. Exiled Sector compiles against $modName, so install $modName into $sourceMods and run setup-libs.ps1 again."
        }
        Copy-Item -Path $modJar -Destination (Join-Path $libs (Split-Path $jar -Leaf)) -Force
    }
}

Write-Host "Copied compile-time jars into $libs"

foreach ($modName in @("LazyLib", "MagicLib", "LunaLib")) {
    $src = Join-Path $sourceMods $modName
    $dst = Join-Path $devMods $modName
    if (Test-Path $dst) {
        Write-Host "$modName already present in dev mods folder, skipping."
    } else {
        Copy-Item -Path $src -Destination $dst -Recurse -Force
        Write-Host "Copied $modName into $devMods"
    }
}

Write-Host "Done."
