param([string]$ClientRoot = (Join-Path $env:APPDATA 'Polyfrost\OneClient'))
$ErrorActionPreference = 'Stop'
$clusterRoot = Join-Path $ClientRoot 'clusters'
foreach ($profileName in @('1.8.9 Fabric', '26.3 Fabric')) {
    foreach ($version in @('1.0.0', '1.0.1')) {
        $modFile = Join-Path (Join-Path (Join-Path $clusterRoot $profileName) 'mods') "oneclient-local-cosmetics-$version.jar"
        if (Test-Path -LiteralPath $modFile) {
            Remove-Item -LiteralPath $modFile
            Write-Output "Removed: $modFile"
        }
    }
}
