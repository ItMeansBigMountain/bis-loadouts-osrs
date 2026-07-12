# BIS Loadouts

BIS Loadouts is a RuneLite external plugin for boss-focused gear recommendations. It reads the local player's combat stats, lets the player choose a boss, combat style, and budget tier, then shows a compact side panel with best-available gear by slot, estimated DPS, hit chance, max hit, and simple boss defence guidance.

The plugin is intentionally lightweight for its first Plugin Hub candidate release. It is designed as an in-client gear advisor and pre-fight sanity check, not a full GearScape-equivalent DPS simulator.

## Features

- RuneLite side panel with selected boss, combat style, estimated DPS, hit chance, max hit, warnings, and gear recommendations by slot.
- Best-available setup recommendations for melee styles, ranged, and magic.
- One-handed/two-handed weapon handling with offhand recommendations when applicable.
- Ranged ammo compatibility filtering for arrows, bolts, darts, and no-ammo weapons.
- Boss search/autocomplete backed by public boss data, with blank search supported for general best-by-stats gear.
- Budget tiers: Budget, Midgame, Rich, and No limit.
- Boss defence guide ordered from weakest style to strongest/avoid style.
- DPS-per-style section for quick comparison across combat styles.
- Local fallback boss and gear data so the panel remains usable if a public data source is unavailable.
- Unit-tested recommendation and boss-data helper logic.

## Current boss/data behavior

BIS Loadouts uses local fallbacks immediately, then refreshes public read-only data in the background when available:

- OSRS Wiki MediaWiki API for boss page lookup and boss-name autocomplete support.
- OSRS Wiki real-time price mapping as an item sanity filter.
- GearScape public endpoints for machine-readable monster and equipment/weapon stat rows.

The plugin uses a descriptive User-Agent for public HTTP requests and does not require API keys.

More detail is in [`docs/wiki-gearscape-integration.md`](docs/wiki-gearscape-integration.md).

## Configuration

Open RuneLite's plugin configuration for `BIS Loadouts`:

- `Boss Profile`: fallback profile used when live boss data cannot be matched.
- `Boss Name`: optional live lookup. Leave blank for general best-by-stats gear.
- `Combat Style`: `Auto` compares available styles; otherwise forces stab, slash, crush, ranged, or magic recommendations.
- `Budget Tier`: filters expensive gear for Budget, Midgame, Rich, or No-limit recommendations.
- `Target Combat Level`: combat level treated as fully ready for the selected profile's login summary.
- `Target Prayer Level`: Prayer level treated as fully ready for the selected profile's login summary.
- `Warning Threshold`: loadout score below this value shows a caution message.
- `Show Login Summary`: enables or disables the login chat message.

## Privacy and network usage

BIS Loadouts does not send chat messages, account credentials, bank contents, inventory contents, or equipment contents to third-party services.

The current public network requests are read-only lookups for boss/item metadata. They include the requested boss/item names or public endpoint paths needed to build recommendations.

If public data requests fail, the plugin falls back to bundled/local data and continues to render.

## Local development

Requirements:

- Java 11.
- Gradle wrapper included in this repository.

From the repo root on Linux/macOS:

```bash
export JAVA_HOME=/opt/data/jdks/current-java11
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew clean test assemble --no-daemon --console=plain
```

From Windows PowerShell, after setting `JAVA_HOME` to a Java 11 JDK:

```powershell
.\gradlew.bat clean test assemble --no-daemon --console=plain
```

Launch RuneLite in developer mode:

```bash
./gradlew run --no-daemon --console=plain
```

Windows PowerShell:

```powershell
.\gradlew.bat run --no-daemon --console=plain
```

## Manual RuneLite testing checklist

Before submitting or updating a Plugin Hub PR, verify in a RuneLite developer-mode session:

1. The plugin appears as `BIS Loadouts` in the plugin list.
2. The plugin can be enabled without startup errors.
3. The configuration panel shows boss, combat style, budget, threshold, and login-summary settings with clear labels.
4. The right-side navigation button opens the BIS Loadouts side panel.
5. Leaving boss search blank does not insert `None` or break search.
6. Selecting a boss refreshes gear, boss defence guidance, and DPS-per-style output.
7. Switching combat style changes the recommended gear.
8. Switching 1H/2H weapon mode recalculates offhand/ammo behavior correctly.
9. Ranged weapons show compatible ammo when needed and no incompatible ammo when not needed.
10. Changing budget tier updates recommendations without UI cutoff in the default RuneLite side panel width.
11. Login summary appears exactly once when enabled and is suppressed when disabled.
12. Network failures do not freeze the client and local fallbacks still render.

## Plugin Hub readiness notes

See [`docs/plugin-hub-pr-readiness.md`](docs/plugin-hub-pr-readiness.md) for the full PR checklist.

- `runelite-plugin.properties` points to `com.itmeansbigmountain.bisloadouts.BisLoadoutsPlugin`.
- Source package is `com.itmeansbigmountain.bisloadouts`.
- The Gradle `run` task uses `BisLoadoutsPluginTest` as the developer-mode launcher.
- Tests pass with Java 11 using `./gradlew clean test assemble --no-daemon --console=plain`.
- Public data usage is documented in this README and in `docs/wiki-gearscape-integration.md`.
- No API keys, credentials, user bank data, inventory data, or equipment data are sent externally.

## Product direction

BIS Loadouts should remain focused on best-in-slot and best-available PvM loadout recommendations inside RuneLite.

Near-term improvements after the first PR-ready pass:

- More exact DPS formulas and special-case boss mechanics.
- Owned/excluded item controls.
- Prayer, potion, Slayer task, and wilderness assumptions.
- Upgrade-next recommendations.
- Screenshot assets for Plugin Hub PR review if requested.
