param(
    [string]$ClientRoot = (Join-Path $env:APPDATA 'Polyfrost\OneClient'),
    [string]$JavaHome
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\tooling.ps1"
$tools = Get-LocalBuildTools -ClientRoot $ClientRoot -JavaHome $JavaHome
$jdkRoot = $tools.JavaBin
$mixinJar = $tools.MixinJar
New-Item -ItemType Directory -Force -Path "$PSScriptRoot\build\classes", "$PSScriptRoot\dist" | Out-Null
$sources = @(Get-ChildItem -LiteralPath "$PSScriptRoot\src" -Filter '*.java' -Recurse | ForEach-Object FullName)
& "$jdkRoot\javac.exe" --release 17 -proc:none -classpath $mixinJar -d "$PSScriptRoot\build\classes" @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& "$jdkRoot\jar.exe" --create --file $tools.Artifact -C "$PSScriptRoot\build\classes" . -C "$PSScriptRoot\resources" .
if ($LASTEXITCODE -ne 0) { throw 'Packaging failed' }
Get-FileHash -LiteralPath $tools.Artifact
