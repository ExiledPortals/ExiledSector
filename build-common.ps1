function Sync-OpSpentHullModPool {
    param([string]$ProjectRoot)

    $javaFile = Join-Path $ProjectRoot "src\main\java\exiledsector\persistence\OpSpentSlotManager.java"
    $csvFile = Join-Path $ProjectRoot "data\hullmods\hull_mods.csv"

    $match = Select-String -Path $javaFile -Pattern 'SLOT_COUNT\s*=\s*(\d+)' | Select-Object -First 1
    if (-not $match) {
        throw "Could not find SLOT_COUNT in $javaFile"
    }
    $slotCount = [int]$match.Matches[0].Groups[1].Value

    $lines = Get-Content $csvFile -Encoding UTF8
    $header = $lines[0]
    $fixedRows = $lines[1..($lines.Count - 1)] | Where-Object { $_ -notmatch ',exiledSector_opSpent_\d+,' }
    $pool = 0..($slotCount - 1) | ForEach-Object {
        "Exiled Sector OP Reserve $_,exiledSector_opSpent_$_,,,,hide_in_codex,,,,TRUE,TRUE,0,0,0,0,exiledsector.effects.SkillTreeOpSpentHullMod,`"Internal marker hullmod used by Exiled Sector to reserve ordnance points spent on allocated skill tree nodes for one ship slot. Its OP cost is set programmatically. Always installed automatically; not player-visible.`",,,"
    }
    $out = @($header) + $fixedRows + $pool
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($csvFile, $out, $utf8NoBom)
    Write-Host "Synced OP-reservation hullmod pool to $slotCount slots (from SLOT_COUNT in OpSpentSlotManager.java)"
}

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

    Sync-OpSpentHullModPool -ProjectRoot $ProjectRoot
    $mvn = Get-MavenCommand
    Push-Location $ProjectRoot
    try {
        & $mvn -q clean package
        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed."
        }
    } finally {
        Pop-Location
    }
}

function Copy-ModFiles {
    param([string]$ProjectRoot, [string]$Destination)

    $excludedGraphics = @("description", "unused")
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    foreach ($file in @("mod_info.json", "ExiledSector.version", "LICENSE")) {
        Copy-Item -Path (Join-Path $ProjectRoot $file) -Destination $Destination -Force
    }
    foreach ($folder in @("jars", "data")) {
        Copy-Item -Path (Join-Path $ProjectRoot $folder) -Destination $Destination -Recurse -Force
    }
    $graphics = New-Item -ItemType Directory -Force -Path (Join-Path $Destination "graphics")
    Get-ChildItem (Join-Path $ProjectRoot "graphics") |
        Where-Object { $excludedGraphics -notcontains $_.Name } |
        ForEach-Object { Copy-Item -Path $_.FullName -Destination $graphics.FullName -Recurse -Force }
    Write-TexturePreloadList -ProjectRoot $ProjectRoot -Destination $Destination -ExcludedFolders ($excludedGraphics + "fonts")
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

    $ledgerFile = Join-Path $ProjectRoot "src\test\resources\released_skill_nodes.json"
    $before = if (Test-Path $ledgerFile) { [System.IO.File]::ReadAllText($ledgerFile).Replace("`r`n", "`n") } else { "" }
    if ($before -ne $text) {
        [System.IO.File]::WriteAllText($ledgerFile, $text, (New-Object System.Text.UTF8Encoding($false)))
        Write-Host "Recorded $($nodes.Count) released skill node ids in $ledgerFile; commit it with the release."
    }
}

function Save-ReleasedSocketables {
    param([string]$ProjectRoot, [string]$Version)

    $ledgerFile = Join-Path $ProjectRoot "src\test\resources\released_socketables.csv"
    $header = "definition,effect,firstRelease,retiredAfter"
    $rows = @{}
    if (Test-Path $ledgerFile) {
        foreach ($row in (Import-Csv $ledgerFile -Encoding UTF8)) {
            $rows["$($row.definition),$($row.effect)"] = "$($row.firstRelease),$($row.retiredAfter)"
        }
    }
    $added = 0
    foreach ($definition in (Import-Csv (Join-Path $ProjectRoot "data\config\exiledSector\socketables.csv") -Encoding UTF8)) {
        $definitionId = "$($definition.id)".Trim()
        if (-not $definitionId -or $definitionId.StartsWith("#")) {
            continue
        }
        foreach ($pool in @($definition.prefixes, $definition.suffixes)) {
            foreach ($entry in "$pool".Split(";")) {
                $effect = $entry.Split(":")[0].Trim()
                if ($effect -and -not $rows.ContainsKey("$definitionId,$effect")) {
                    $rows["$definitionId,$effect"] = "$Version,"
                    $added++
                }
            }
        }
    }
    if ($added -gt 0) {
        [string[]]$pairs = @($rows.Keys)
        [Array]::Sort($pairs, [StringComparer]::Ordinal)
        $lines = @($header) + ($pairs | ForEach-Object { "$_,$($rows[$_])" })
        [System.IO.File]::WriteAllLines($ledgerFile, [string[]]$lines, (New-Object System.Text.UTF8Encoding($false)))
        Write-Host "Recorded $added newly released socketable pool effects in $ledgerFile; commit it with the release."
    }
}
