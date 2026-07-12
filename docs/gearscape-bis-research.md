# GearScape BiS / best-available setup research

Research date: 2026-05-31

Purpose: understand GearScape enough to design an automatic RuneLite side-panel recommendation flow for `BIS Loadouts`: player stats + selected boss + selected combat style -> recommended gear by slot, DPS/loadout fit notes, and beginner-friendly upgrades.

## What GearScape is doing

GearScape is a Nuxt/Vue web app. Most of the heavy best-setup logic runs client-side in web workers, not through a single public `best setup` API endpoint.

Observed public data endpoints:

- `https://api.gearscape.net/api/monster`
- `https://api.gearscape.net/api/spell`
- `https://api.gearscape.net/api/equipment/all`
- `https://api.gearscape.net/api/weapon/all`
- `https://api.gearscape.net/api/equipment/ammunition`
- `https://api.gearscape.net/api/equipment/alias`
- `https://api.gearscape.net/api/prayer/overview`
- `https://api.gearscape.net/api/potion/overview`
- `https://api.gearscape.net/api/trailblazer/regions/all`
- item lookup example: `https://api.gearscape.net/api/item/id/24187`

Observed app assets:

- Best setup worker: `https://gearscape.net/_nuxt/776157e.worker.js`
- Overkill worker: `https://gearscape.net/_nuxt/b1f90fe.worker.js`
- Other worker: `https://gearscape.net/_nuxt/56ab90e.worker.js`

Important: use GearScape as product/algorithm inspiration, not as a hard dependency, unless the GearScape maintainer explicitly documents/stabilizes these endpoints for third-party use. For production RuneLite, prefer OSRS Wiki data + local calculations/caches.

## GearScape setup workflow

The Best Setup page builds a setup by:

1. Loading monster data, spells, equipment, weapons, ammunition, aliases, prayers, potions, and region/league metadata.
2. Collecting player stats: Attack, Strength, Defence, Hitpoints, Magic, Ranged, Prayer, Mining.
3. Collecting target selection: monster, monster variant, current defensive reductions, wilderness/task/aoe flags, distance filters, specials, and extra mechanics.
4. Collecting setup constraints:
   - budget / expensive-item cap / risk ammunition count
   - include / exclude / include-only item lists
   - locked slots
   - owned/custom items
   - selected potions and prayers
   - selected ranking mode: DPS, TTK, accuracy, max hit, average hit, overkill, etc.
   - search depth and secondary fill mode
5. Spawning the best-setup worker once per style: stab, slash, crush, ranged, magic, and sometimes atlatl.
6. Worker returns one result per style, sorted by `compareMetric` descending.
7. UI auto-selects the best style/result and fills the panel with gear, DPS, hit chance, max hit, and warnings.

Observed worker payload keys include:

```text
style, monster, allStyleWeaponsUnfiltered, allStyleWeapons, styleWeaponCounts,
allStyleEquipment, allEquipment, allAmmunition, ammunitionList, equipmentSlots,
equipmentSets, equipmentLocks, extra, aoe, tasked, wilderness, specials,
experienceSettings, experiencePreference, include, exclude, includeOnly,
includeOnlySpells, initialBudget, riskAmmunitionCount, expensiveItemCap,
stats, effectiveAttack, effectiveStrength, potions, prayers, intensifyPrayers,
airPactPrayers, allSpells, trailblazerRegions, trailblazerRegionData,
bestMode, equipmentSecondaryFill, searchDepth, forceOnehanded
```

Observed worker result keys include:

```text
dps, maxHit, averageDamage, hitChance, compareMetric, effectiveStyle,
boostKey, prayerKey, warning, equipment
```

## Example observed output

Test input: Best Setup for `Zulrah (Serpentine)`, 99 combat stats, default/unlimited-ish budget, ranking mode `Dps`, search depth `1`.

Top GearScape result was magic:

- style: magic
- DPS: ~10.8641
- max hit: 42
- hit chance: ~93.02%
- selected gear:
  - weapon: eye of ayak
  - neck: occult necklace
  - shield: elidinis' ward (f)
  - cape: imbued guthix cape
  - body: ancestral robe top
  - hands: confliction gauntlets
  - legs: ancestral robe bottom
  - head: ancestral hat
  - feet: avernic treads (max)
  - ring: magus ring

Other results from the same run:

- ranged: ~7.9181 DPS, twisted bow + fortified masori + dizana's quiver + dragon arrows
- slash: ~7.7808 DPS, noxious halberd + rancour + oathplate/ferocious/bellator-style setup
- stab: ~6.7213 DPS, noxious halberd stab setup
- crush: 0 DPS with warning: `No weapons could be found that match the distance filter of 1 tiles.`

This confirms the UI is not just a static BiS table. It ranks candidate equipment against the target's actual defensive stats/mechanics and the player's stats/options.

## Algorithm model to implement in BIS Loadouts

We should build a simpler, RuneLite-friendly version instead of cloning the whole GearScape calculator.

### Phase 1: practical MVP

Inputs:

- Player local stats from RuneLite client.
- Selected boss / target.
- Selected combat style: melee stab/slash/crush, ranged, magic.
- Optional budget tier: `no limit`, `rich`, `midgame`, `budget`, `iron/owned only`.
- Optional owned/excluded item list.
- Optional assumptions: potion, prayer, on-task, wilderness, special-attack defence reductions.

Data sources:

- OSRS Wiki static/semantic data for item equipment stats, requirements, prices, monster stats/attributes, and icons.
- RuneLite item manager/client data for IDs, names, icons, and local inventory/bank ownership where available.
- Optional public GearScape endpoints only for research/prototyping or fallback data exploration, not as required runtime infra.

Ranking:

1. Filter items by slot, combat style relevance, member/tradeable/requirements, budget/owned/excluded status.
2. Filter weapons by chosen style and attack distance constraints.
3. Generate candidate setups with a bounded search, not brute force every item combination.
4. Score each candidate with a DPS-ish metric:
   - effective attack roll vs target defence roll
   - effective strength/max-hit estimate
   - weapon attack speed
   - target weakness/resistance where known
   - style-specific bonuses and damage multipliers
5. Return top setup plus alternatives and upgrade deltas.

MVP search strategy:

- Pick top N weapons for the selected style after requirements/budget filters.
- For each weapon, greedily select top items per slot by marginal DPS contribution.
- Re-score full setup after each slot fill.
- Keep the top K partial setups per step (beam search) to avoid combinatorial explosion.
- Use slot locks/owned-only/exclude lists as hard constraints.

Recommended defaults:

- `searchDepth = 1` or `2` equivalent for fast in-client response.
- Show `best available for your current stats` before showing theoretical max BiS.
- Show why an item was skipped: missing level, over budget, excluded, unavailable, wrong style, two-handed conflict, ammo mismatch.

### Phase 2: richer boss loadout fit

Add GearScape-inspired details without making the UI overwhelming:

- Best style for this target, with DPS estimates for the other styles.
- Slot-by-slot recommendation cards with item icon, name, requirement blockers, and cost.
- `Upgrade next` row: cheapest/highest-impact upgrades from current gear/bank.
- Boss-specific warnings: target has high magic defence, crush weakness, requires dragonfire shield, poison/venom, Salve, Slayer task, wilderness risk, etc.
- Beginner mode: gear tiers only.
- Advanced mode: DPS, hit chance, max hit, prayer/potion assumptions, special-attack assumptions.

## RuneLite side-panel UX

Panel flow:

1. Boss dropdown.
2. Combat style dropdown with `Auto` option.
3. Budget/owned-only toggle.
4. Assumptions chips: potions, prayers, Slayer task, wilderness, spec reductions.
5. Loadout score.
6. Recommended setup by slot.
7. Alternatives/upgrades list.
8. Warnings and missing prerequisites.

Use icons inline, but keep hover tooltips for details:

- item requirement tooltip
- cost tooltip
- reason selected tooltip
- reason skipped tooltip
- expected DPS/hit chance tooltip

## Implementation notes

Data model shape for Java:

```java
record BossTarget(String name, int npcId, int hitpoints, int defence, int magic,
                  int stabDef, int slashDef, int crushDef, int rangedDef, int magicDef,
                  Set<String> attributes) {}

record GearItem(int itemId, String name, EquipmentSlot slot,
                int attackReq, int strengthReq, int defenceReq, int rangedReq,
                int magicReq, int prayerReq, int hitpointsReq, int slayerReq,
                int stabBonus, int slashBonus, int crushBonus, int rangedBonus, int magicBonus,
                int meleeStrength, int rangedStrength, int magicStrength,
                int prayerBonus, long price, boolean members) {}

record WeaponOption(GearItem item, CombatStyle style, int attackSpeed,
                    boolean twoHanded, String ammoType) {}

record SetupScore(Map<EquipmentSlot, GearItem> items, CombatStyle style,
                  double dps, double hitChance, int maxHit,
                  List<String> warnings) {}
```

Keep the calculator service pure and unit-tested. The RuneLite plugin layer should only collect client stats/config and render panel rows.

## Open questions before implementation

- Which source should be canonical for monster/item data in the plugin bundle: checked-in generated JSON from OSRS Wiki, or runtime HTTP cache?
- Should the plugin read local bank/equipment to support `owned only`, or start with manual include/exclude lists?
- What boss list should ship first? Suggested MVP: Zulrah, Vorkath, Barrows, Jad, Giant Mole, Scurrius, Fight Caves/Inferno placeholder, and common Slayer bosses.
- How far should we go with exact DPS formulas vs simple rankings for the first usable release?
