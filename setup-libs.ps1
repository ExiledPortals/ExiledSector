$ErrorActionPreference = "Stop"

$starsectorCore = "E:\Dev\Starsector\starsector-core"
$devMods        = "E:\Dev\Starsector\mods"
$sourceMods     = "C:\Games\Starsector\mods"
$libs           = Join-Path $PSScriptRoot "libs"

New-Item -ItemType Directory -Force -Path $libs | Out-Null

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

foreach ($jar in $coreJars) {
    Copy-Item -Path (Join-Path $starsectorCore $jar) -Destination $libs -Force
}

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
        Copy-Item -Path (Join-Path $srcJarsDir $jar) -Destination (Join-Path $libs (Split-Path $jar -Leaf)) -Force
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
