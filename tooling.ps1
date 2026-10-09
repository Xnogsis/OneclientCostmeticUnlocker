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
    $asmJars = @(Get-ChildItem -LiteralPath (Join-Path $metadata 'libraries\org\ow2\asm') -Filter '*.jar' -Recurse | ForEach-Object FullName)
    $manifest = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'resources\fabric.mod.json') -Raw | ConvertFrom-Json
    return [pscustomobject]@{
        Metadata = $metadata
        JavaBin = Join-Path $JavaHome 'bin'
        MixinJar = $mixinJar.FullName
        CompileClasspath = (@($mixinJar.FullName) + $asmJars) -join ';'
        Artifact = Join-Path $PSScriptRoot "dist\oneclient-local-cosmetics-$($manifest.version).jar"
        Version = $manifest.version
    }
}

function Get-OneClientProfiles {
    param([string]$ClientRoot)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $clusters = Join-Path $ClientRoot 'clusters'
    if (-not (Test-Path -LiteralPath $clusters)) { return }
    foreach ($profile in (Get-ChildItem -LiteralPath $clusters -Directory)) {
        $modsPath = Join-Path $profile.FullName 'mods'
        if (-not (Test-Path -LiteralPath $modsPath)) { continue }
        $matches = @()
        foreach ($jar in (Get-ChildItem -LiteralPath $modsPath -Filter '*.jar')) {
            $zip = $null
            try {
                $zip = [IO.Compression.ZipFile]::OpenRead($jar.FullName)
                $entry = $zip.GetEntry('fabric.mod.json')
                if (-not $entry) { continue }
                $reader = [IO.StreamReader]::new($entry.Open())
                try { $mod = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
                if ($mod.id -eq 'polyplus') { $matches += $jar.FullName }
            } catch { Write-Warning "Could not inspect $($jar.Name): $($_.Exception.Message)" }
            finally { if ($zip) { $zip.Dispose() } }
        }
        if ($matches.Count -gt 1) { throw "Multiple PolyPlus JARs found in $($profile.Name). Resolve duplicates first." }
        if ($matches.Count -eq 1) { [pscustomobject]@{ Name=$profile.Name; ModsPath=$modsPath; PolyPlusJar=$matches[0] } }
    }
}
