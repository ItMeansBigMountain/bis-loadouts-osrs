# OSRS Wiki + GearScape live data integration

## API key status

No API key is required for the OSRS Wiki MediaWiki API endpoints used by this plugin.

The plugin sets a descriptive `User-Agent` and uses public read-only GET requests:

- OSRS Wiki page lookup: `https://oldschool.runescape.wiki/api.php?action=opensearch&format=json&limit=1&namespace=0&search=<boss>`
- GearScape boss index: `https://api.gearscape.net/api/monster`
- GearScape boss detail: `https://api.gearscape.net/api/monster/id/<npc_id>`
- GearScape equipment data: `https://api.gearscape.net/api/equipment/all`
- GearScape weapon data: `https://api.gearscape.net/api/weapon/all`

## Why both sources are used

The OSRS Wiki is the canonical public documentation/source-of-truth for pages, names, and user-facing links. Its standard MediaWiki API is stable and does not require auth.

GearScape exposes machine-readable monster and equipment stat endpoints that are directly useful for automated gear recommendations. The plugin treats GearScape as a live data backend/sanity source and falls back to local presets when network calls fail.

## No manual update workflow

At startup, the plugin refreshes a live boss index and equipment/weapon list in a background executor. That means newly released bosses/items can show up once those public data sources update, without shipping a new plugin version for every boss/item.

The built-in enum boss profiles remain only as fallbacks and quick presets. The new `Boss Name` config field accepts any boss name and resolves it against the live boss index.

## Runtime behavior

1. Start with local presets so the panel still works offline.
2. In the background, load the live boss list and live equipment/weapon stats.
3. Resolve the configured boss name to the closest live boss match.
4. Fetch detailed monster stats for the selected boss.
5. Use OSRS Wiki API search to attach the canonical wiki page URL.
6. Run the recommendation engine against live equipment where available; otherwise use the local fallback set.

## Current caveat

This is still a simplified DPS/readiness model. It uses live item and monster stats, but it is not a full GearScape-equivalent permutation calculator yet. GearScape's web app does much deeper worker-based setup search with prayers, potions, specs, ammunition compatibility, and special mechanics.
