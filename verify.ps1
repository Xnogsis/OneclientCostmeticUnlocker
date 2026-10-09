param(
    [string]$ClientRoot = (Join-Path $env:APPDATA 'Polyfrost\OneClient'),
    [string]$JavaHome,
    [switch]$Adapter
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\tooling.ps1"
$tools = Get-LocalBuildTools -ClientRoot $ClientRoot -JavaHome $JavaHome
$metadata = $tools.Metadata
$jdk = $tools.JavaBin
New-Item -ItemType Directory -Force -Path "$PSScriptRoot\build\verify" | Out-Null
$libraries = @(Get-ChildItem -LiteralPath (Join-Path $metadata 'libraries') -Filter '*.jar' -Recurse | Where-Object { $_.FullName -match '\\org\\ow2\\asm\\|\\sponge-mixin\\|\\guava\\guava\\|\\code\\gson\\gson\\' } | ForEach-Object FullName)
$classpath = ($libraries + @("$PSScriptRoot\build\verify")) -join ';'
& "$jdk\javac.exe" -classpath $classpath -d "$PSScriptRoot\build\verify" "$PSScriptRoot\verification\VerifyHooks.java"
if ($LASTEXITCODE -ne 0) { throw 'Hook verifier compilation failed' }
$artifact = $tools.Artifact
$targets = @((Join-Path $clientRoot 'clusters\1.8.9 Fabric\mods\polyplus-1.2.57+1.8.9.jar'), (Join-Path $clientRoot 'clusters\26.3 Fabric\mods\polyplus-1.2.57+26.3.jar'))
& "$jdk\java.exe" -classpath $classpath VerifyHooks $artifact @targets
if ($LASTEXITCODE -ne 0) { throw 'Hook verification failed' }
if ($Adapter) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $dependencyDir = Join-Path $PSScriptRoot 'build\test-libs'
    New-Item -ItemType Directory -Force -Path $dependencyDir | Out-Null
    $modsPath = Join-Path $ClientRoot 'clusters\1.8.9 Fabric\mods'
    $parents = @(Get-ChildItem -LiteralPath $modsPath -Filter '*.jar' | Where-Object { $_.Name -match '^polyplus|^fabric-language-kotlin' })
    foreach ($parent in $parents) {
        $zip = [IO.Compression.ZipFile]::OpenRead($parent.FullName)
        try {
            foreach ($entry in $zip.Entries) {
                if ($entry.FullName -match '^META-INF/jars/[^/]+\.jar$') {
                    [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, (Join-Path $dependencyDir $entry.Name), $true)
                }
            }
        } finally { $zip.Dispose() }
    }
    & "$jdk\javac.exe" -classpath $artifact -d "$PSScriptRoot\build\verify" "$PSScriptRoot\verification\VerifyAdapter.java"
    if ($LASTEXITCODE -ne 0) { throw 'Adapter verifier compilation failed' }
    $loggingJars = @(Get-ChildItem -LiteralPath (Join-Path $metadata 'libraries\org\apache\logging\log4j') -Filter '*.jar' -Recurse |
        Where-Object { $_.FullName -match '2.26.0' } | ForEach-Object FullName)
    foreach ($target in $targets) {
        $adapterClasspath = (@($artifact, "$PSScriptRoot\build\verify", "$dependencyDir\*", $target) + $loggingJars) -join ';'
        & "$jdk\java.exe" -classpath $adapterClasspath VerifyAdapter
        if ($LASTEXITCODE -ne 0) { throw "Adapter verification failed: $target" }
    }
}
