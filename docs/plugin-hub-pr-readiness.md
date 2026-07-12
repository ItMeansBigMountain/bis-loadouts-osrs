# Plugin Hub PR readiness checklist

Use this checklist before opening or updating the RuneLite Plugin Hub PR for BIS Loadouts.

## Repository metadata

- Display name: `BIS Loadouts`
- Main plugin class: `com.itmeansbigmountain.bisloadouts.BisLoadoutsPlugin`
- Config group: `bisloadouts`
- GitHub repo: `https://github.com/ItMeansBigMountain/bis-loadouts-osrs`
- Local HeRmEz lifecycle path: `projects/osrs-plugins/pr-review-pending/BisLoadouts`

## Required local checks

Run from the plugin repo root with Java 11:

```bash
./gradlew clean test assemble --no-daemon --console=plain
```

Expected result:

```text
BUILD SUCCESSFUL
```

## Manual UI smoke test

1. Launch with `./gradlew run --no-daemon --console=plain`.
2. Confirm the plugin appears as `BIS Loadouts` and enables cleanly.
3. Open the side panel in the default RuneLite sidebar width.
4. Verify no title, search field, gear icon row, or bottom text section clips horizontally.
5. Leave boss search blank and confirm it behaves as general best-by-stats gear.
6. Select at least one known boss and confirm the defence guide orders styles weakest to strongest.
7. Cycle gear arrows and confirm strongest recommendations appear first.
8. Test ranged recommendations with a bow, crossbow, blowpipe/darts, and a no-ammo weapon path.
9. Switch 1H/2H mode and confirm offhand recommendations recalculate correctly.
10. Disable network access or simulate endpoint failure and confirm local fallbacks still render.

## Public API and privacy notes

BIS Loadouts uses public read-only metadata endpoints only. It does not send credentials, chat text, bank contents, inventory contents, or equipped items to external services.

Documented public sources:

- OSRS Wiki MediaWiki API for boss names/pages.
- OSRS Wiki real-time price mapping for item metadata sanity filtering.
- GearScape public monster/equipment/weapon stat endpoints.

If Plugin Hub review asks about data usage, point reviewers to:

- `README.md` section: `Privacy and network usage`
- `docs/wiki-gearscape-integration.md`

## Known intentional scope limits

- DPS values are simplified estimates, not exact endgame calculator parity.
- Gear recommendations are best-available according to the plugin model and current public data/fallbacks.
- Owned-only, item exclusion lists, prayers, potions, and special-case boss mechanics are future improvements.
