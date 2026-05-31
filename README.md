# Boss Readiness Score

Boss Readiness Score is a RuneLite external plugin that gives a quick, in-client readiness estimate before bossing. On login, it reads the local player's combat level plus Hitpoints, Prayer, and Defence levels, compares them to configurable targets, and prints a 0-100 readiness score to the game chat.

The score is intentionally lightweight: it is a pre-fight sanity check, not a replacement for boss-specific gear, supplies, quest, diary, or mechanic knowledge.

## Features

- Login chat summary for the configured boss/PvM profile.
- Configurable boss profile label.
- Configurable target combat level and Prayer level.
- Configurable warning threshold for caution messaging.
- Pure scoring helper methods covered by unit tests, so the score behavior can be checked without launching a live RuneLite client.

## Scoring model

The current score is computed from local client stats only:

- Combat level: 55%
- Hitpoints level: 20%
- Prayer level: 15%
- Defence level: 10%

Scores are capped at 100. If the score is below the configured warning threshold, the plugin prints a caution recommendation. If the score meets or exceeds the threshold, it reports that the account is ready for manual gear checks.

## Configuration

Open RuneLite's plugin configuration for `Boss Readiness Score`:

- `Boss Profile`: free-text label shown in the chat message, e.g. `Vorkath`, `Zulrah`, or `General PvM`.
- `Target Combat Level`: combat level treated as fully ready for the selected profile.
- `Target Prayer Level`: Prayer level treated as fully ready for the selected profile.
- `Warning Threshold`: readiness score below this value shows a caution message.
- `Show Login Summary`: enables/disables the login chat message.

## API usage and privacy

This plugin does not call external APIs. It uses RuneLite client state for local player combat/skill levels only.

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

1. The plugin appears as `Boss Readiness Score` in the plugin list.
2. The configuration panel shows all settings with clear labels.
3. Logging into a world prints exactly one readable game-message summary when `Show Login Summary` is enabled.
4. Lowering `Warning Threshold` changes the message from caution to ready as expected.
5. Disabling `Show Login Summary` suppresses the login chat message.

## Plugin-hub prep notes

- `runelite-plugin.properties` points to `com.itmeansbigmountain.bossreadinessscore.BossReadinessScorePlugin`.
- Source package is `com.itmeansbigmountain.bossreadinessscore`.
- The Gradle `run` task uses `BossReadinessScorePluginTest` as the developer-mode launcher.
- No screenshots are included yet; add one during manual RuneLite testing if the plugin-hub submission needs visual evidence.

## Product direction update

Boss Readiness Score should include best-in-slot / best-available gear recommendations, inspired by GearScape but easier to use inside RuneLite.

Target UX:

- User selects a boss/PvM target and optionally budget, owned/unowned item exclusions, combat style, and risk level.
- Panel shows readiness score plus gear recommendations by slot, prayer/supply notes, missing prerequisites, and simpler upgrade priorities.
- Use OSRS Wiki-derived item/monster data where possible; GearScape is inspiration for preference-based setup selection and DPS-style comparison, not a private API dependency.
- Present beginner-friendly defaults before advanced calculator details.
