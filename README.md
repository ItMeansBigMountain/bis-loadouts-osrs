# BIS Loadouts

BIS Loadouts is a RuneLite external plugin that gives a quick, in-client loadout estimate before bossing. On login, it reads the local player's combat stats, compares them to the selected boss profile, and opens a RuneLite side panel with a 0-100 loadout score plus GearScape-inspired best-available gear recommendations by slot.

The score and gear engine are intentionally lightweight for the first testable release: it is a practical pre-fight sanity check with boss/style/budget-aware recommendations, not a full exact DPS simulator yet.

## Features

- Login chat summary for the configured boss/PvM profile.
- RuneLite side panel with loadout score, selected combat style, estimated DPS, hit chance, max hit, warnings, and gear by slot.
- Boss dropdown: General PvM, Scurrius, Giant Mole, Barrows, Vorkath, Zulrah, and Fight Caves as offline fallbacks.
- Free-text `Boss Name` lookup and a side-panel autocomplete selector backed by both GearScape's boss index and the OSRS Wiki `Category:Bosses` list, with OSRS Wiki page links resolved through the public Wiki API.
- Combat style dropdown with Auto, stab, slash, crush, ranged, and magic.
- Budget tiers: Budget, Midgame, Rich, and No limit.
- Live equipment/weapon stat loading from GearScape with local fallback gear if the network source is unavailable.
- Gear recommendation helper methods covered by unit tests, so the scoring/recommendation behavior can be checked without launching a live RuneLite client.

## Scoring model

The current score is computed from local client stats only:

- Combat level: 55%
- Hitpoints level: 20%
- Prayer level: 15%
- Defence level: 10%

Scores are capped at 100. If the score is below the configured warning threshold, the plugin prints a caution recommendation. If the score meets or exceeds the threshold, it reports that the account is ready for manual gear checks.

## Configuration

Open RuneLite's plugin configuration for `BIS Loadouts`:

- `Boss Profile`: offline fallback profile used when `Boss Name` is blank or live data cannot be matched.
- `Boss Name`: optional live lookup; type any boss name such as `Vorkath`, `The Leviathan`, `Zulrah`, or a new boss after the public data sources update.
- `Combat Style`: `Auto` compares available styles; otherwise forces stab/slash/crush/ranged/magic recommendations.
- `Budget Tier`: filters expensive gear for Budget, Midgame, Rich, or No-limit recommendations.
- `Target Combat Level`: combat level treated as fully ready for the selected profile's login summary.
- `Target Prayer Level`: Prayer level treated as fully ready for the selected profile's login summary.
- `Warning Threshold`: loadout score below this value shows a caution message.
- `Show Login Summary`: enables/disables the login chat message.

## API usage and privacy

No API key is required for the public OSRS Wiki API calls currently used by this plugin. The plugin uses a descriptive User-Agent and read-only public endpoints.

Runtime data flow:

- OSRS Wiki MediaWiki API resolves canonical boss pages/links for the side panel.
- GearScape's public monster/equipment/weapon endpoints provide machine-readable boss and item stats for dynamic recommendations.
- Local fallback presets remain available if either service is offline.

The live data integration is documented in [`docs/wiki-gearscape-integration.md`](docs/wiki-gearscape-integration.md).

## Local development

Requirements:

- Java 11. In this workspace, use `JAVA_HOME=/opt/data/jdks/current-java11`.
- Gradle wrapper included in this repository.

Run tests:

```bash
JAVA_HOME=/opt/data/jdks/current-java11 ./gradlew test --no-daemon -q
```

Build the plugin:

```bash
JAVA_HOME=/opt/data/jdks/current-java11 ./gradlew assemble --no-daemon -q
```

Launch RuneLite in developer mode with this external plugin loaded:

```bash
JAVA_HOME=/opt/data/jdks/current-java11 ./gradlew run --no-daemon
```

## Manual RuneLite testing checklist

Before plugin-hub prep, manually verify in a RuneLite developer-mode session:

1. The plugin appears as `BIS Loadouts` in the plugin list.
2. The configuration panel shows boss, combat style, budget, thresholds, and login-summary settings with clear labels.
3. The right-side navigation button opens a panel with loadout score, gear slots, warnings, and alternative style DPS.
4. Logging into a world prints exactly one readable game-message summary when `Show Login Summary` is enabled.
5. Changing boss/style/budget while logged in refreshes the panel.
6. Lowering `Warning Threshold` changes the message from caution to ready as expected.
7. Disabling `Show Login Summary` suppresses the login chat message while the side panel still works.

## Plugin-hub prep notes

- `runelite-plugin.properties` points to `com.itmeansbigmountain.bisloadouts.BisLoadoutsPlugin`.
- Source package is `com.itmeansbigmountain.bisloadouts`.
- The Gradle `run` task uses `BisLoadoutsPluginTest` as the developer-mode launcher.
- No screenshots are included yet; add one during manual RuneLite testing if the plugin-hub submission needs visual evidence.

## Product direction update

BIS Loadouts should include best-in-slot / best-available gear recommendations, inspired by GearScape but easier to use inside RuneLite.

Target UX:

- User selects a boss/PvM target and optionally budget, owned/unowned item exclusions, combat style, and risk level.
- Panel shows loadout score plus gear recommendations by slot, prayer/supply notes, missing prerequisites, and simpler upgrade priorities.
- Use OSRS Wiki-derived item/monster data where possible; GearScape is inspiration for preference-based setup selection and DPS-style comparison, not a private API dependency.
- Present beginner-friendly defaults before advanced calculator details.
- GearScape research has been captured in [`docs/gearscape-bis-research.md`](docs/gearscape-bis-research.md), including observed data endpoints, worker payload/result shapes, and a simplified RuneLite implementation model.
