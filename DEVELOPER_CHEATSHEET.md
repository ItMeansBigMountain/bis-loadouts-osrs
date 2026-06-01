# BossReadinessScore Developer Cheat Sheet

Quick commands for testing this RuneLite external plugin locally.

## 0. Where to run commands

Run every command from this repo root:

```bash
cd BossReadinessScore
```

If you cloned the whole HeRmEz workspace with submodules, the path is usually:

```bash
cd HeRmEz/projects/osrs-plugins/BossReadinessScore
```

## 1. Java requirement

RuneLite external plugin development expects **Java 11**.

Check your Java:

```bash
java -version
```

Good output starts with something like:

```text
openjdk version "11.x"
```

If you have multiple JDKs, point `JAVA_HOME` at Java 11 before running Gradle.

macOS:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
```

Windows PowerShell example:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-11"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Linux example:

```bash
export JAVA_HOME=/path/to/jdk-11
export PATH="$JAVA_HOME/bin:$PATH"
```

## 2. First sanity check

Linux/macOS:

```bash
chmod +x ./gradlew
./gradlew test --no-daemon
```

Windows PowerShell:

```powershell
.\gradlew.bat test --no-daemon
```

## 3. Build only

Linux/macOS:

```bash
./gradlew assemble --no-daemon
```

Windows PowerShell:

```powershell
.\gradlew.bat assemble --no-daemon
```

## 4. Launch RuneLite with this plugin loaded

Linux/macOS:

```bash
./gradlew run --no-daemon
```

Windows PowerShell:

```powershell
.\gradlew.bat run --no-daemon
```

This starts RuneLite in developer mode using the repo's Gradle `run` task.

## 5. In-client manual test flow

1. RuneLite opens.
2. Log in or reach the login screen.
3. Open the RuneLite plugin list.
4. Search for this plugin's display name.
5. Enable it.
6. Open the plugin configuration panel.
7. Change a setting and confirm the UI updates without errors.
8. If there is a sidebar/panel/overlay, open it and verify it renders.
9. Hop worlds or log in/out if the plugin listens for login/game-state events.
10. Watch for red error notifications, frozen UI, spammy chat messages, or repeated API calls.

## 6. Useful Gradle commands

List available tasks:

```bash
./gradlew tasks --no-daemon
```

Clean rebuild:

```bash
./gradlew clean test assemble --no-daemon
```

Run with extra logging:

```bash
./gradlew run --no-daemon --stacktrace --info
```

If Gradle acts weird, stop daemons:

```bash
./gradlew --stop
```

## 7. Files you usually edit

```text
build.gradle
runelite-plugin.properties
src/main/java/**/**Plugin.java
src/main/java/**/**Config.java
src/main/java/**/**Panel.java
src/main/java/**/**Overlay.java
src/test/java/**
README.md
```

## 8. Common fixes

### `Unsupported class file major version` or Java mismatch

Use Java 11, then rerun:

```bash
java -version
./gradlew clean test --no-daemon
```

### `Permission denied: ./gradlew`

```bash
chmod +x ./gradlew
```

### Plugin does not show in RuneLite

Check:

```bash
cat runelite-plugin.properties
```

The `pluginClass` must point to the real main plugin class under `src/main/java`.

Also verify the Gradle `run` task points at the repo's `*PluginTest` launcher.

### RuneLite opens but plugin errors immediately

Run with stacktrace:

```bash
./gradlew run --no-daemon --stacktrace
```

Then inspect the first Java exception, not the last repeated line.

### API/network features fail

Confirm whether the plugin has offline fallbacks. For API-backed plugins, failures should not freeze the client thread. Network calls should run in background executors and fail gracefully.

## 9. Before telling someone it works

Run:

```bash
./gradlew clean test assemble --no-daemon
```

Then launch:

```bash
./gradlew run --no-daemon
```

And manually verify the plugin appears, can be enabled, and does its core behavior in RuneLite.

## 10. Git workflow

```bash
git status --short
git add .
git commit -m "fix: describe what changed"
git push origin main
```

If this repo is checked out as a HeRmEz submodule, update the parent repo after pushing:

```bash
cd ../../..
git add projects/osrs-plugins/BossReadinessScore
git commit -m "chore: update BossReadinessScore submodule"
git push origin main
```
