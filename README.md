# OneClient Cosmetics Unlocker

Simple and lightweight Client-side cosmetic unlocker for OneClient.

Equipment selections save automatically to `config/oneclient-local-cosmetics.properties`
inside the game profile's working directory. They restore after restarts and PolyPlus
resets, including slots you explicitly unequip. Selections are shared by accounts
using the same profile and remain visible only on your client.

Saves use a temporary file and atomic replacement where supported. If the save file
cannot be read or contains an invalid ID, the mod logs the error and keeps new
selections in memory without overwriting the file. Fix or delete the file and restart
to enable saving again. Unknown slot names are ignored, so a changed slot does not
prevent other selections from loading. To clear saved choices, close the game and
delete the file. Uninstalling leaves this file intact.

## Install

Download newest release from Releases, place it in your profile's `mods` folder, and restart the game.

Requires Fabric Loader 0.19.5+, Java 17+, and a compatible PolyPlus build. Tested with PolyPlus 1.2.57 on 1.8.9 and 26.3 through bytecode and adapter checks.

## Build

With OneClient installed, run in PowerShell 7:

```powershell
./build.ps1
```

Output: `dist/`. Use `./install.ps1` to install or `./uninstall.ps1` to remove.

Run `./check.ps1` for persistence and adapter checks without launching Minecraft.
