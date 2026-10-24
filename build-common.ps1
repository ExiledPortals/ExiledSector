$ModRootFiles = @("mod_info.json", "ExiledSector.version", "LICENSE", "CREDITS.md")
$LibraryModFolder = "exiled-lib\mod"
$LibraryModRootFiles = @("mod_info.json", "ExiledLib.version")
$BuildJvmOptions = "-XX:ActiveProcessorCount=4 -XX:CICompilerCount=2 -XX:TieredStopAtLevel=1"

function Get-MavenCommand {
    $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCmd) {
        return $mvnCmd.Source
    }
    $mvn = Get-ChildItem "C:\Program Files\JetBrains\IntelliJ IDEA*\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" |
        Select-Object -First 1 -ExpandProperty FullName
    if (-not $mvn) {
        throw "Could not find mvn on PATH or bundled with IntelliJ IDEA."
    }
    if (-not $env:JAVA_HOME) {
        $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
    }
    return $mvn
}

function Invoke-ModBuild {
    param([string]$ProjectRoot)

    $mvn = Get-MavenCommand
    $buildLock = Join-Path $ProjectRoot ".maven-build.lock"
    Set-Content -LiteralPath $buildLock -Value $PID
    $previousJvmOptions = $env:JAVA_TOOL_OPTIONS
    $env:JAVA_TOOL_OPTIONS = $BuildJvmOptions
    Push-Location $ProjectRoot
    try {
        & $mvn -q clean package
        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed."
        }
    } finally {
        Pop-Location
        $env:JAVA_TOOL_OPTIONS = $previousJvmOptions
        Remove-Item -LiteralPath $buildLock -Force -ErrorAction SilentlyContinue
    }
}

function Copy-ModFiles {
    param([string]$ProjectRoot, [string]$Destination)

    $excludedGraphics = @("description", "unused")
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    foreach ($file in $ModRootFiles) {
        Copy-Item -Path (Join-Path $ProjectRoot $file) -Destination $Destination -Force
    }
    foreach ($folder in @("jars", "data", "sounds")) {
        Copy-Item -Path (Join-Path $ProjectRoot $folder) -Destination $Destination -Recurse -Force
    }
    $graphics = New-Item -ItemType Directory -Force -Path (Join-Path $Destination "graphics")
    Get-ChildItem (Join-Path $ProjectRoot "graphics") |
        Where-Object { $excludedGraphics -notcontains $_.Name } |
        ForEach-Object { Copy-Item -Path $_.FullName -Destination $graphics.FullName -Recurse -Force }
    Get-ChildItem -LiteralPath $Destination -Recurse -File -Force -Filter ".*.tmp" | ForEach-Object { $_.Delete() }
    Write-TexturePreloadList -ProjectRoot $ProjectRoot -Destination $Destination -ExcludedFolders ($excludedGraphics + "fonts")
}

function Copy-LibraryModFiles {
    param([string]$ProjectRoot, [string]$Destination)

    $libraryMod = Join-Path $ProjectRoot $LibraryModFolder
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    foreach ($file in $LibraryModRootFiles) {
        Copy-Item -Path (Join-Path $libraryMod $file) -Destination $Destination -Force
    }
    Copy-Item -Path (Join-Path $ProjectRoot "LICENSE") -Destination $Destination -Force
    foreach ($folder in @("jars", "data")) {
        Copy-Item -Path (Join-Path $libraryMod $folder) -Destination $Destination -Recurse -Force
    }
}

function Sync-FolderMirror {
    param([string]$Source, [string]$Destination)

    & robocopy $Source $Destination /MIR /NJH /NJS /NP /NFL /NDL | Out-Null
    if ($LASTEXITCODE -ge 8) {
        throw "robocopy failed to mirror $Source into $Destination (exit code $LASTEXITCODE)."
    }
    $global:LASTEXITCODE = 0
}

function Write-TexturePreloadList {
    param([string]$ProjectRoot, [string]$Destination, [string[]]$ExcludedFolders)

    $graphicsRoot = (Resolve-Path (Join-Path $ProjectRoot "graphics")).Path
    $paths = Get-ChildItem $graphicsRoot -Directory |
        Where-Object { $ExcludedFolders -notcontains $_.Name } |
        ForEach-Object { Get-ChildItem $_.FullName -Recurse -File -Filter "*.png" } |
        ForEach-Object { "graphics/" + $_.FullName.Substring($graphicsRoot.Length + 1).Replace("\", "/") } |
        Sort-Object
    $listFile = Join-Path $Destination "data\config\exiledSector\texture_preload.csv"
    [System.IO.File]::WriteAllLines($listFile, [string[]](@("path") + $paths))
}

function Save-ReleasedSkillNodes {
    param([string]$ProjectRoot)

    $tree = Get-Content (Join-Path $ProjectRoot "data\skilltrees\ship_skill_tree.json") -Raw -Encoding UTF8 | ConvertFrom-Json
    $types = Get-Content (Join-Path $ProjectRoot "data\skilltrees\skill_types.json") -Raw -Encoding UTF8 | ConvertFrom-Json
    $rootTypes = @($types.skillTypes | Where-Object { $_.tier -ceq "ROOT" } | ForEach-Object { $_.id })
    [string[]]$nodes = @($tree.nodes | ForEach-Object { $_.id })
    [string[]]$roots = @($tree.nodes | Where-Object { $rootTypes -ccontains $_.type } | ForEach-Object { $_.id })
    [Array]::Sort($nodes, [StringComparer]::Ordinal)
    [Array]::Sort($roots, [StringComparer]::Ordinal)
    $nodeLines = ($nodes | ForEach-Object { "    `"$_`"" }) -join ",`n"
    $rootLines = ($roots | ForEach-Object { "    `"$_`"" }) -join ",`n"
    $text = "{`n  `"nodes`": [`n$nodeLines`n  ],`n  `"roots`": [`n$rootLines`n  ]`n}`n"

    $ledgerFile = Join-Path $ProjectRoot "exiledsector-skill-tree\src\test\resources\released_skill_nodes.json"
    $before = if (Test-Path $ledgerFile) { [System.IO.File]::ReadAllText($ledgerFile).Replace("`r`n", "`n") } else { "" }
    if ($before -ne $text) {
        [System.IO.File]::WriteAllText($ledgerFile, $text, (New-Object System.Text.UTF8Encoding($false)))
        Write-Host "Recorded $($nodes.Count) released skill node ids in $ledgerFile; commit it with the release."
    }
}

function Get-PoolEntryEffects {
    param([string]$Entry, [hashtable]$EffectsByModifier)

    $trimmed = $Entry.Trim()
    if (-not $trimmed) {
        return @()
    }
    if ($trimmed.StartsWith("@")) {
        $modifierId = $trimmed.Substring(1).Split(":")[0].Trim()
        if ($EffectsByModifier.ContainsKey($modifierId)) {
            return $EffectsByModifier[$modifierId]
        }
        throw "Pool entry $trimmed references a modifier that is not in item_modifiers.csv"
    }
    return @($trimmed.Split(":")[0].Trim())
}

function Save-ReleasedItems {
    param([string]$ProjectRoot, [string]$Version)

    $configDir = Join-Path $ProjectRoot "data\config\exiledSector"
    $ledgerFile = Join-Path $ProjectRoot "exiledsector-mod\src\test\resources\released_items.csv"
    $header = "definition,effect,firstRelease,retiredAfter"
    $rows = @{}
    if (Test-Path $ledgerFile) {
        foreach ($row in (Import-Csv $ledgerFile -Encoding UTF8)) {
            $rows["$($row.definition),$($row.effect)"] = "$($row.firstRelease),$($row.retiredAfter)"
        }
    }
    $effectsByModifier = @{}
    foreach ($modifier in (Import-Csv (Join-Path $configDir "item_modifiers.csv") -Encoding UTF8)) {
        $effectsByModifier["$($modifier.id)".Trim()] = @("$($modifier.lines)".Split(";") | ForEach-Object { $_.Split(":")[0].Trim() } | Where-Object { $_ })
    }
    $definitions = @()
    foreach ($definitionFile in @("subroutines.csv", "hull_items.csv")) {
        $definitions += @(Import-Csv (Join-Path $configDir $definitionFile) -Encoding UTF8)
    }
    $added = 0
    foreach ($definition in $definitions) {
        $definitionId = "$($definition.id)".Trim()
        if (-not $definitionId -or $definitionId.StartsWith("#")) {
            continue
        }
        foreach ($pool in @($definition.prefixes, $definition.suffixes)) {
            foreach ($entry in "$pool".Split(";")) {
                foreach ($effect in (Get-PoolEntryEffects -Entry $entry -EffectsByModifier $effectsByModifier)) {
                    if ($effect -and -not $rows.ContainsKey("$definitionId,$effect")) {
                        $rows["$definitionId,$effect"] = "$Version,"
                        $added++
                    }
                }
            }
        }
    }
    if ($added -gt 0) {
        [string[]]$pairs = @($rows.Keys)
        [Array]::Sort($pairs, [StringComparer]::Ordinal)
        $lines = @($header) + ($pairs | ForEach-Object { "$_,$($rows[$_])" })
        [System.IO.File]::WriteAllLines($ledgerFile, [string[]]$lines, (New-Object System.Text.UTF8Encoding($false)))
        Write-Host "Recorded $added newly released item pool effects in $ledgerFile; commit it with the release."
    }
}
