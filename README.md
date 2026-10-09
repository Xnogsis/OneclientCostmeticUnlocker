# OneClient Local Cosmetics

A client-side Fabric add-on for the PolyPlus cosmetic wardrobe in OneClient. Version **1.0.1** targets **PolyPlus 1.2.57**, with bytecode checks for the **1.8.9 Fabric** and **26.3 Fabric** profiles.

The add-on exposes loaded catalog cosmetics and emotes in the wardrobe, keeps equipment selections in memory, and suppresses cosmetic and emote broadcasts. Other players continue to see the server's equipment state. Items still depend on definitions and assets the client can load. Selections reset when the catalog resets or the game exits.

## Requirements

- Windows and PowerShell 7.
- OneClient with both supported profiles installed and their dependencies downloaded for the full verification suite.
- A JDK compatible with the installed PolyPlus release (the tested installation uses JDK 25).

Build scripts use JAVA_HOME when set, otherwise they find a JDK bundled with OneClient. Mixin and verification dependencies come from the local OneClient installation. No game binaries, cosmetic assets, or dependency JARs are included in this repository.

## Build and verify

From the repository folder:

```powershell
./build.ps1
./verify.ps1 -Adapter
```

The artifact is `dist/oneclient-local-cosmetics-1.0.1.jar`. The scripts accept `-ClientRoot` for a nonstandard OneClient location; build and verification also accept `-JavaHome`:

```powershell
./build.ps1 -ClientRoot 'D:\OneClient' -JavaHome 'C:\Java\jdk-25'
./verify.ps1 -ClientRoot 'D:\OneClient' -JavaHome 'C:\Java\jdk-25' -Adapter
```

The verifier checks selectors using Mixin's actual parser and verifies 11 injection targets, callback types, argument coercions, and reflection adapter members in both installed PolyPlus JARs. The Adapter switch additionally extracts local test dependencies into the ignored build folder and exercises actual PolyPlus classes for ownership exposure, equipment changes, refresh retention, and packet filtering. These checks issue no network requests.

## Install and remove

Close the game, then run:

```powershell
./install.ps1
```

This copies the add-on into each supported installed profile, verifies its SHA256, and backs up/removes version 1.0.0 if present. Launch the game and open the PolyPlus cosmetics wardrobe. The original PolyPlus JARs are unchanged.

To remove the add-on while the game is closed:

```powershell
./uninstall.ps1
```

Both scripts accept `-ClientRoot`.

## Status and troubleshooting

Compilation, selector checks, and adapter tests passed against the tested installation. In-game Mixin application, wardrobe interactions, downloads, and visual rendering remain unverified after the 1.0.1 fix.

Version 1.0.0 caused the main menu to fail because Mixin rejected Kotlin method names containing hyphens. Version 1.0.1 uses anchored regex selectors and adds parser validation. See [CHANGELOG.md](CHANGELOG.md).

If startup fails, run the uninstall script and inspect the affected profile's logs/latest.log. Redact account identifiers, tokens, local paths, and server addresses before sharing logs.

## Source layout

- `src/`: adapter and Mixin hooks.
- `resources/`: Fabric manifest and Mixin configuration.
- `verification/`: bytecode and adapter checks.
- `tooling.ps1`: shared local tool discovery.
- `build.ps1`, `verify.ps1`, `install.ps1`, `uninstall.ps1`: development and installation commands.

The analysis, build, and dist directories are ignored. Keep logs, decompiled third-party code, local backups, and downloaded dependencies out of commits. Build artifacts can be attached separately to a GitHub release.
