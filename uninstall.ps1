param([string]$ClientRoot = (Join-Path $env:APPDATA 'Polyfrost\OneClient'))
$ErrorActionPreference = 'Stop'
$clusterRoot = Join-Path $ClientRoot 'clusters'
if (-not (Test-Path -LiteralPath $clusterRoot)) { return }
foreach ($profile in (Get-ChildItem -LiteralPath $clusterRoot -Directory)) {
    $modsPath = Join-Path $profile.FullName 'mods'
    if (-not (Test-Path -LiteralPath $modsPath)) { continue }
    foreach ($modFile in @(Get-ChildItem -LiteralPath $modsPath -Filter 'oneclient-local-cosmetics-*.jar')) {
        Remove-Item -LiteralPath $modFile.FullName
        Write-Output "Removed: $($modFile.FullName)"
    }
}
