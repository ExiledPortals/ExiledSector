param(
    [string]$SavesDir = "E:\Dev\Starsector\saves",
    [string]$SaveName = ""
)

$ErrorActionPreference = "Stop"
$marker = "exiledSector_chart_"

if ($SaveName) {
    $campaignFile = Get-Item (Join-Path (Join-Path $SavesDir $SaveName) "campaign.xml")
} else {
    $campaignFile = Get-ChildItem -Path $SavesDir -Directory |
        ForEach-Object { Get-Item -LiteralPath (Join-Path $_.FullName "campaign.xml") -ErrorAction SilentlyContinue } |
        Where-Object { $_ } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $campaignFile) {
        throw "No campaign.xml found under $SavesDir"
    }
}

$saveText = [System.IO.File]::ReadAllText($campaignFile.FullName)
$markerMatches = [regex]::Matches($saveText, "$([regex]::Escape($marker))[A-Za-z0-9_]*")
$chartedSpaceCount = [regex]::Matches($saveText, [regex]::Escape("Charted Space")).Count

Write-Host "Save:            $($campaignFile.Directory.Name)"
Write-Host "Saved at:        $($campaignFile.LastWriteTime)"
Write-Host ("campaign.xml:    {0:N0} bytes ({1:N1} KB)" -f $campaignFile.Length, ($campaignFile.Length / 1KB))
Write-Host "$marker occurrences: $($markerMatches.Count)"
Write-Host "'Charted Space' occurrences: $chartedSpaceCount"

if ($markerMatches.Count -gt 0) {
    Write-Host "Most common chart ids and tags:"
    $markerMatches | Group-Object Value | Sort-Object Count -Descending | Select-Object -First 20 |
        ForEach-Object { Write-Host ("  {0,6}  {1}" -f $_.Count, $_.Name) }
    exit 1
}
exit 0
