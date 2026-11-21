param(
    [Parameter(Mandatory = $true)][string]$Scenario,
    [string]$Save,
    [string]$Resolution = "1920x1080",
    [int]$TimeoutSeconds = 600,
    [string]$GameDir = "E:\Dev\Starsector",
    [string]$Group,
    [string]$Case,
    [string]$Tag,
    [string]$StartAfter,
    [int]$MaxRelaunches = 10,
    [switch]$Record,
    [int]$RecordFps = 60,
    [switch]$RebuildFixture
)

$ErrorActionPreference = "Stop"
if ($Record) { . (Join-Path $PSScriptRoot "recording.ps1") }

$maxWidth = 1920
$maxHeight = 1080
if ($Resolution -notmatch '^(\d+)x(\d+)$') { throw "Resolution must look like 1600x900, not $Resolution" }
if ([int]$Matches[1] -gt $maxWidth -or [int]$Matches[2] -gt $maxHeight) {
    throw "Resolution $Resolution is above ${maxWidth}x${maxHeight}; the harness runs windowed at most that size so screenshots work."
}

$scenarioFile = Join-Path (Get-Location) $Scenario
if ([System.IO.Path]::IsPathRooted($Scenario)) { $scenarioFile = $Scenario }
if (-not (Test-Path -LiteralPath $scenarioFile)) { throw "No scenario at $scenarioFile" }
$scenarioText = [System.IO.File]::ReadAllText($scenarioFile)
$scenarioJson = $scenarioText | ConvertFrom-Json
$scenarioFields = @($scenarioJson.PSObject.Properties.Name)
$newGame = $scenarioFields -contains "newGame"
$writeFixture = if ($scenarioFields -contains "writeFixture") { [string]$scenarioJson.writeFixture } else { $null }

$fixtureName = "harness_fixture"
$fixtureScenario = Join-Path $PSScriptRoot "fixture.json"
$gameSaves = Join-Path $GameDir "saves"

function Build-Fixture {
    Write-Host "Building $fixtureName from $fixtureScenario"
    & $PSCommandPath -Scenario $fixtureScenario -Resolution $Resolution -TimeoutSeconds $TimeoutSeconds -GameDir $GameDir
    if ($LASTEXITCODE -ne 0) { throw "Building $fixtureName failed with exit code $LASTEXITCODE" }
}

$saveSource = $null
$usesFixture = $false
$fixtureRebuilt = $false
if (-not $newGame) {
    if (-not $Save -and $scenarioFields -contains "save") { $Save = $scenarioJson.save }
    if (-not $Save) { $Save = $fixtureName }
    $saveSource = if ([System.IO.Path]::IsPathRooted($Save)) { $Save } else { Join-Path $gameSaves $Save }
    $usesFixture = (Split-Path $saveSource -Leaf) -eq $fixtureName
    if ($usesFixture) {
        $fixtureCampaign = Join-Path $saveSource "campaign.xml"
        $stale = -not (Test-Path -LiteralPath $fixtureCampaign) -or
            (Get-Item -LiteralPath $fixtureScenario).LastWriteTime -gt (Get-Item -LiteralPath $fixtureCampaign).LastWriteTime
        if ($RebuildFixture -or $stale) {
            Build-Fixture
            $fixtureRebuilt = $true
        }
    }
    if (-not (Test-Path -LiteralPath (Join-Path $saveSource "campaign.xml"))) { throw "$saveSource is not a save folder" }
    $saveName = Split-Path $saveSource -Leaf
    $oldestLoadableVersion = [version]"0.10.0"
    $descriptor = [System.IO.File]::ReadAllText((Join-Path $saveSource "descriptor.xml"))
    if ($descriptor -match '(?s)<id>exiledSector</id>.*?<versionInfo[^>]*>\s*<major>(\d+)</major>\s*<minor>(\d+)</minor>\s*<patch>(\d+)</patch>') {
        $savedWith = [version]"$($Matches[1]).$($Matches[2]).$($Matches[3])"
        if ($savedWith -lt $oldestLoadableVersion) {
            Write-Warning "$saveName was saved with Exiled Sector $savedWith; saves older than $oldestLoadableVersion no longer load."
        }
    }
}

$runDir = Join-Path $GameDir ("harness-runs\" + (Get-Date -Format "yyyyMMdd-HHmmss"))
$runSaves = Join-Path $runDir "saves"
$runCommon = Join-Path $runSaves "common"
$runScreenshots = Join-Path $runDir "screenshots"
New-Item -ItemType Directory -Force -Path $runSaves, $runScreenshots | Out-Null
$gameCommon = Join-Path $gameSaves "common"
if (Test-Path -LiteralPath $gameCommon) {
    Copy-Item -LiteralPath $gameCommon -Destination $runCommon -Recurse
} else {
    New-Item -ItemType Directory -Force -Path $runCommon | Out-Null
}
Get-ChildItem -LiteralPath $runCommon -Filter "exiledsector_harness_*" | Remove-Item -Force
[System.IO.File]::WriteAllText((Join-Path $runCommon "exiledsector_harness_scenario.data"), $scenarioText)
Copy-Item -LiteralPath $scenarioFile -Destination (Join-Path $runDir "scenario.json")
if ($newGame) {
    $startOption = "-Dexiledsector.harness.newGame=true"
} else {
    Copy-Item -LiteralPath $saveSource -Destination (Join-Path $runSaves $saveName) -Recurse
    $startOption = "-Dexiledsector.harness.save=" + (Join-Path $runSaves $saveName)
}

$coreDir = Join-Path $GameDir "starsector-core"
$javaExe = Join-Path $GameDir "jre\bin\java.exe"
$fastRenderingParams = Join-Path $coreDir "fr.vmparams"
if (Test-Path -LiteralPath $fastRenderingParams) {
    $vmTokens = @(Get-Content -LiteralPath $fastRenderingParams | ForEach-Object { $_.Trim() } |
        Where-Object { $_ -and -not $_.StartsWith("#") } | ForEach-Object { $_ -split '\s+' } | ForEach-Object { $_.Replace('\\', '\') })
    if (Test-Path -LiteralPath (Join-Path $coreDir "PatchLibAgent.jar")) { $vmTokens = @("-javaagent:PatchLibAgent.jar") + $vmTokens }
    Write-Host "Launching like fr.bat, with fr.vmparams"
} else {
    $vmTokens = ([System.IO.File]::ReadAllText((Join-Path $GameDir "vmparams")).Trim() -split '\s+') | Select-Object -Skip 1
}
$classpathIndex = [Array]::IndexOf($vmTokens, "-classpath")
if ($classpathIndex -lt 0) { throw "vmparams has no -classpath" }
$quote = { param($text) if ($text -match '\s') { '"' + $text + '"' } else { $text } }
$jvmOptions = $vmTokens[0..($classpathIndex - 1)] | Where-Object {
    $_ -notlike "-Dcom.fs.starfarer.settings.paths.saves=*" -and
    $_ -notlike "-Dcom.fs.starfarer.settings.paths.logs=*" -and
    $_ -notlike "-Dcom.fs.starfarer.settings.paths.screenshots=*" -and
    $_ -ne "-XX:+ShowMessageBoxOnError"
}
$harnessOptions = @(
    "-Dcom.fs.starfarer.settings.paths.saves=$runSaves",
    "-Dcom.fs.starfarer.settings.paths.logs=$runDir",
    "-Dcom.fs.starfarer.settings.paths.screenshots=$runScreenshots",
    "-DlaunchDirect=true",
    "-DstartRes=$Resolution",
    "-DstartFS=false",
    "-DstartSound=false",
    $startOption
)
$suiteFilters = [ordered]@{ group = $Group; case = $Case; tag = $Tag; startAfter = $StartAfter }
foreach ($suiteFilter in $suiteFilters.GetEnumerator()) {
    if ($suiteFilter.Value) { $harnessOptions += "-Dexiledsector.harness.$($suiteFilter.Key)=$($suiteFilter.Value)" }
}
$arguments = (@($jvmOptions) + $harnessOptions + $vmTokens[$classpathIndex..($vmTokens.Length - 1)] | ForEach-Object { & $quote $_ }) -join ' '

$deployedJar = Get-ChildItem -LiteralPath (Join-Path $GameDir "mods\ExiledSector\jars") -Filter "*.jar" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($deployedJar) { Write-Host "Deployed mod jars last written $($deployedJar.LastWriteTime)" }
Write-Host "Run folder $runDir"
if ($newGame) { Write-Host "Starting a new game" } else { Write-Host "Loading $saveName" }

$gameProcess = Start-Process -FilePath $javaExe -ArgumentList $arguments -WorkingDirectory $coreDir -PassThru `
    -RedirectStandardOutput (Join-Path $runDir "console.log") -RedirectStandardError (Join-Path $runDir "console-errors.log")
$recording = $null
if ($Record) { $recording = Start-HarnessRecording -GameProcess $gameProcess -Resolution $Resolution -RunDir $runDir -Fps $RecordFps }
$timedOut = -not $gameProcess.WaitForExit($TimeoutSeconds * 1000)
if ($timedOut) {
    Write-Warning "The game was still running after $TimeoutSeconds s; stopping it."
    $gameProcess.Kill()
    $gameProcess.WaitForExit()
}
if ($recording) { Stop-HarnessRecording $recording | Out-Null }

$shotParts = Get-ChildItem -LiteralPath $runCommon -Filter "exiledsector_harness_shot_*.data" | ForEach-Object {
    if ($_.BaseName -match '^exiledsector_harness_shot_(.+)_(\d+)$') {
        [pscustomobject]@{ Shot = $Matches[1]; Part = [int]$Matches[2]; File = $_.FullName }
    }
}
foreach ($shot in ($shotParts | Group-Object Shot)) {
    $base64 = ($shot.Group | Sort-Object Part | ForEach-Object { [System.IO.File]::ReadAllText($_.File).Trim() }) -join ''
    $pngFile = Join-Path $runScreenshots ($shot.Name + ".png")
    [System.IO.File]::WriteAllBytes($pngFile, [Convert]::FromBase64String($base64))
    Write-Host "Screenshot $pngFile"
}

$resultsSource = Join-Path $runCommon "exiledsector_harness_results.data"
$resultsFile = Join-Path $runDir "results.json"
$logFile = Join-Path $runDir "starsector.log"
$logErrors = 0
if (Test-Path -LiteralPath $logFile) {
    $logErrors = @(Select-String -LiteralPath $logFile -Pattern '^\d+\s+\[[^\]]*\]\s+ERROR' -ErrorAction SilentlyContinue).Count
}

$results = $null
if (Test-Path -LiteralPath $resultsSource) {
    Copy-Item -LiteralPath $resultsSource -Destination $resultsFile
    $results = [System.IO.File]::ReadAllText($resultsFile) | ConvertFrom-Json
}
$loadFailed = -not $timedOut -and ((-not $results) -or ([string]$results.runFailure -like "Could not load the save*"))
if ($loadFailed -and $usesFixture -and -not $fixtureRebuilt) {
    Write-Host "$fixtureName did not load with this build; rebuilding it and running again."
    & $PSCommandPath -Scenario $scenarioFile -Save $Save -Resolution $Resolution -TimeoutSeconds $TimeoutSeconds -GameDir $GameDir `
        -Group $Group -Case $Case -Tag $Tag -StartAfter $StartAfter -MaxRelaunches $MaxRelaunches -Record:$Record -RecordFps $RecordFps -RebuildFixture
    exit $LASTEXITCODE
}

if (-not $results) {
    Write-Host "No results were written. The game log is $logFile ($logErrors ERROR lines)."
    exit 2
}
if ($recording -and (Test-Path -LiteralPath $recording.File)) { Export-HarnessClips $recording $results.clips $runDir }
foreach ($step in $results.steps) {
    $verdict = if ($step.passed) { "PASS" } else { "FAIL" }
    Write-Host ("{0} {1,2} {2} ({3} ms): {4}" -f $verdict, $step.index, $step.step, $step.millis, $step.detail)
}
if ($results.runFailure) { Write-Host "Run failure: $($results.runFailure)" }
$suiteCases = @($results.cases | Where-Object { $_ })
foreach ($suiteCase in $suiteCases) {
    if ($suiteCase.passed) {
        Write-Host ("CASE PASS {0} ({1} steps)" -f $suiteCase.id, $suiteCase.steps)
    } else {
        Write-Host ("CASE FAIL {0} at {1}: {2}" -f $suiteCase.id, $suiteCase.failedStep, $suiteCase.failure)
    }
}
foreach ($skippedCase in @($results.skippedCases | Where-Object { $_ })) { Write-Host "CASE SKIP $skippedCase" }
foreach ($notRunCase in @($results.notRunCases | Where-Object { $_ })) { Write-Host "CASE NOT RUN $notRunCase" }
foreach ($caseGroup in ($suiteCases | Group-Object { ($_.id -split '/')[0] })) {
    $groupPassed = @($caseGroup.Group | Where-Object { $_.passed }).Count
    Write-Host ("Group {0}: {1} of {2} cases passed" -f $caseGroup.Name, $groupPassed, $caseGroup.Count)
}
if ($results.data) {
    $dataNames = @($results.data.PSObject.Properties | ForEach-Object { $_.Name })
    if ($dataNames.Count -gt 0) { Write-Host ("Recorded data: " + ($dataNames -join ", ")) }
}
Write-Host "Results $resultsFile"
Write-Host "Game log $logFile ($logErrors ERROR lines, $($results.errorsLogged) while the harness listened)"

if ($timedOut) { exit 3 }
if ($results.abortedAfterCase) {
    $notRunCount = @($results.notRunCases | Where-Object { $_ }).Count
    if ($notRunCount -eq 0) { exit 1 }
    if ($MaxRelaunches -le 0) {
        Write-Host "Teardown of $($results.abortedAfterCase) failed and no relaunches are left; $notRunCount cases did not run."
        exit 1
    }
    Write-Host "Teardown of $($results.abortedAfterCase) failed; relaunching the game for the $notRunCount cases after it."
    & $PSCommandPath -Scenario $scenarioFile -Save $Save -Resolution $Resolution -TimeoutSeconds $TimeoutSeconds -GameDir $GameDir `
        -Group $Group -Case $Case -Tag $Tag -StartAfter $results.abortedAfterCase -MaxRelaunches ($MaxRelaunches - 1) -Record:$Record -RecordFps $RecordFps
    exit 1
}
if (-not $results.finished -or -not $results.passed) { exit 1 }

if ($writeFixture) {
    $savedGame = Get-ChildItem -LiteralPath $runSaves -Directory | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "campaign.xml") } |
        Sort-Object { (Get-Item -LiteralPath (Join-Path $_.FullName "campaign.xml")).LastWriteTime } -Descending | Select-Object -First 1
    if (-not $savedGame) {
        Write-Host "The scenario asks to write $writeFixture, but it saved no game; add a saveGame step."
        exit 1
    }
    $fixtureTarget = Join-Path $gameSaves $writeFixture
    if (Test-Path -LiteralPath $fixtureTarget) { Remove-Item -LiteralPath $fixtureTarget -Recurse -Force }
    Copy-Item -LiteralPath $savedGame.FullName -Destination $fixtureTarget -Recurse
    Write-Host "Saved $($savedGame.Name) as $fixtureTarget"
}
exit 0
