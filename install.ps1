param([string]$ClientRoot = (Join-Path $env:APPDATA 'Polyfrost\OneClient'))
$ErrorActionPreference = 'Stop'
$manifest = Get-Content -LiteralPath "$PSScriptRoot\resources\fabric.mod.json" -Raw | ConvertFrom-Json
$artifact = Join-Path $PSScriptRoot "dist\oneclient-local-cosmetics-$($manifest.version).jar"
if (-not (Test-Path -LiteralPath $artifact)) { throw 'Build the add-on with build.ps1 before installing.' }
$clusterRoot = Join-Path $ClientRoot 'clusters'
$installed = 0
foreach ($profileName in @('1.8.9 Fabric', '26.3 Fabric')) {
    $modsPath = Join-Path (Join-Path $clusterRoot $profileName) 'mods'
    if (-not (Test-Path -LiteralPath $modsPath)) { continue }
    $destination = Join-Path $modsPath "oneclient-local-cosmetics-$($manifest.version).jar"
    if ((Test-Path -LiteralPath $destination) -and ((Get-FileHash -LiteralPath $destination).Hash -ne (Get-FileHash -LiteralPath $artifact).Hash)) {
        throw "A different add-on already exists at $destination; preserve it before replacing."
    }
    Copy-Item -LiteralPath $artifact -Destination $destination -Force
    if ((Get-FileHash -LiteralPath $destination).Hash -ne (Get-FileHash -LiteralPath $artifact).Hash) { throw "Copy verification failed: $destination" }
    $previous = Join-Path $modsPath 'oneclient-local-cosmetics-1.0.0.jar'
    if (Test-Path -LiteralPath $previous) {
        $backupDir = Join-Path $PSScriptRoot "build\backups\$profileName"
        New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
        $backupFile = Join-Path $backupDir 'oneclient-local-cosmetics-1.0.0.jar'
        if (-not (Test-Path -LiteralPath $backupFile)) { Copy-Item -LiteralPath $previous -Destination $backupFile }
        if ((Get-FileHash -LiteralPath $previous).Hash -ne (Get-FileHash -LiteralPath $backupFile).Hash) { throw "Backup mismatch: $previous" }
        Remove-Item -LiteralPath $previous
    }
    Write-Output "Installed and SHA256 verified: $destination"
    $installed++
}
if ($installed -eq 0) { throw 'No supported OneClient profiles found at the selected ClientRoot.' }
