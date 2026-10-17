param(
    [string]$ClientRoot = (Join-Path $env:APPDATA 'Polyfrost\OneClient'),
    [string]$JavaHome
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\tooling.ps1"
$tools = Get-LocalBuildTools -ClientRoot $ClientRoot -JavaHome $JavaHome
$checkRoot = Join-Path $PSScriptRoot "build\checks\$([guid]::NewGuid().ToString('N'))"
$classes = Join-Path $checkRoot 'classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath "$PSScriptRoot\checks" -Filter '*.java' -Recurse | ForEach-Object FullName)
& (Join-Path $tools.JavaBin 'javac.exe') --release 17 -proc:none -classpath $tools.CompileClasspath -d $classes "$PSScriptRoot\src\localcosmetics\LocalCosmetics.java" "$PSScriptRoot\src\localcosmetics\SelectionStore.java" "$PSScriptRoot\src\localcosmetics\CompatibilityPlugin.java" @sources
if ($LASTEXITCODE -ne 0) { throw 'Check compilation failed' }
foreach ($phase in @('write', 'read', 'corrupt', 'writeFailure')) {
    $scenario = if ($phase -eq 'read') { 'write' } else { $phase }
    $runDirectory = Join-Path $checkRoot $scenario
    New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
    Push-Location -LiteralPath $runDirectory
    try {
        & (Join-Path $tools.JavaBin 'java.exe') -ea -cp $classes localcosmetics.PersistenceCheck $phase
        if ($LASTEXITCODE -ne 0) { throw "Check failed: $phase" }
    } finally { Pop-Location }
}
$polyPlusJars = @(Get-OneClientProfiles -ClientRoot $ClientRoot | ForEach-Object PolyPlusJar)
if ($polyPlusJars.Count -gt 0) {
    & (Join-Path $tools.JavaBin 'java.exe') -ea -cp "$classes;$($tools.CompileClasspath)" localcosmetics.CompatibilityCheck @polyPlusJars
    if ($LASTEXITCODE -ne 0) { throw 'Installed PolyPlus compatibility check failed' }
} else { Write-Warning 'No installed PolyPlus profiles; bytecode compatibility checks skipped.' }
