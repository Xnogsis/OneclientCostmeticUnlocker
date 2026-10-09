# Changelog

## 1.0.1

- Fix the main-menu failure caused by literal selectors for Kotlin method names containing hyphens.
- Use anchored regex selectors for the affected equipment and packet hooks.
- Validate injection selectors with the installed Mixin parser against both supported PolyPlus JARs.
- Back up and remove the previous installed add-on during upgrades.

## 1.0.0

- Initial local wardrobe, equipment, and emote implementation.
- Known issue: invalid Mixin selectors prevent the main menu from loading. Use 1.0.1.
