function Get-LocalBuildTools {
    param([string]$ClientRoot, [string]$JavaHome)
    $metadata = Join-Path $ClientRoot 'metadata'
    if (-not $JavaHome) { $JavaHome = $env:JAVA_HOME }
    if (-not $JavaHome) {
        $javaRoot = Join-Path $metadata 'java'
        $candidate = Get-ChildItem -LiteralPath $javaRoot -Filter 'javac.exe' -Recurse -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($candidate) { $JavaHome = Split-Path (Split-Path $candidate.FullName -Parent) -Parent }
    }
    if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\javac.exe'))) {
        throw 'JDK not found. Set JAVA_HOME or pass -JavaHome pointing to a JDK installation.'
    }
    $mixinJar = Get-ChildItem -LiteralPath (Join-Path $metadata 'libraries\net\fabricmc\sponge-mixin') -Filter '*.jar' -Recurse -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $mixinJar) { throw 'Mixin library not found. Launch an installed OneClient profile to download its dependencies first.' }
    $manifest = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'resources\fabric.mod.json') -Raw | ConvertFrom-Json
    return [pscustomobject]@{
        Metadata = $metadata
        JavaBin = Join-Path $JavaHome 'bin'
        MixinJar = $mixinJar.FullName
        Artifact = Join-Path $PSScriptRoot "dist\oneclient-local-cosmetics-$($manifest.version).jar"
        Version = $manifest.version
    }
}
