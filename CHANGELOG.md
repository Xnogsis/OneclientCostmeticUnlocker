# Changelog

## 1.2.0

- Save equipment overrides per profile, including explicitly unequipped slots.
- Restore saved selections after game restarts and PolyPlus resets.
- Write saves atomically where supported and preserve unreadable or malformed files.
- Ignore unknown saved slot names and handle missing player UUIDs safely.
- Check that PolyPlus body slots remain enums before applying hooks.
- Exclude the legacy ASM bundle from the build classpath to prevent conflicts with modern ASM.

## 1.1.0

- Ship one Minecraft-independent JAR without an exact PolyPlus version requirement.
- Match Kotlin-mangled method names across compiler suffix changes.
- Check the cosmetic API before applying hooks; disable the entire add-on for incompatible builds.
- Discover installed PolyPlus profiles automatically for installation and verification.
- Remove older add-on versions during upgrades and simplify the README.

## 1.0.1

- Fix the main-menu failure caused by literal selectors for Kotlin method names containing hyphens.
- Use anchored regex selectors for the affected equipment and packet hooks.
- Validate injection selectors with the installed Mixin parser against both supported PolyPlus JARs.
- Back up and remove the previous installed add-on during upgrades.

## 1.0.0

- Initial local wardrobe, equipment, and emote implementation.
- Known issue: invalid Mixin selectors prevent the main menu from loading. Use 1.0.1.
