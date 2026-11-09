# Modifier parts

Status: built on branch `modifier-parts` (2026-11-04), not yet merged, except design point 4 (see its note). Two orphan modifiers still await a decision.

The advanced search's stat picker narrows its list to the stats a chosen ship part can roll. Core can only see part data for the base drop pools, so every biome, anomaly and corruption stat lands on every part's list. This plan gives core one index of which parts each hull item modifier can appear on, whatever its source.

## What exists today

A modifier's prefix or suffix slot is its own: the `affix` column of `item_modifiers.csv`. Its `scopes` column names item types (`hull_item`), not parts.

Parts come from the other direction, per item definition:
- The nine generic rows of `hull_items.csv`, one per part, list that part's random drop pool as `@id:weight` entries, with optional tier ranges. The same modifier can sit in several parts' pools with a different weight in each, so this is a real modifier-and-part table, not just membership. It stays as it is.
- The 53 unique rows list their fixed modifiers.
- `hull_item_implicits.csv` gives each part its implicits.

Modifiers outside those lists get their part from somewhere core can't read:
- 80 biome modifiers: the `item` column of `biome_modifiers.csv`, read by `BiomeModifiers` in exiledsector-star-charts.
- 7 engram modifiers in `engrams.csv` and 2 Storm Front modifiers hard-coded in `StormLoot`. Both are granted to any part.
- The corruption implicits in `hull_item_corruption_implicits.csv` can land on any part. Core reads this file, but it has no part column.

These sources already register their modifiers with core through `ReservedModifiers.reserve(modifierId, ownerId)`, which keeps them out of the base rolls. That call has the owner but not the parts.

14 hull item modifiers have no source at all: no pool, no unique, no biome, anomaly or corruption list. Nothing can roll them. `ARMOR_PERCENT`, `BURN_LEVEL_FLAT`, `DAMAGE_TO_DESTROYER_PERCENT`, `DAMAGE_TO_FRIGATE_PERCENT`, `ELECTRONIC_WARFARE`, `ENERGY_WEAPON_FLUX_COST_PERCENT`, `FIGHTER_REFIT_TIME_PERCENT`, `FIGHTER_WEAPON_DAMAGE_PERCENT`, `NON_BEAM_ENERGY_WEAPON_RANGE_PERCENT`, `PD_DAMAGE_TO_MISSILES_PERCENT`, `SENSOR_STRENGTH_FLAT_CREW_QUARTERS`, `SHIELD_UPKEEP_MULT`, `WEAPON_DAMAGE_PERCENT` and `WEAPON_TURN_RATE_PERCENT_BRIDGE`. Some look like leftovers from part-suffixed replacements (`ARMOR_PERCENT` beside `ARMOR_PERCENT_ARMOR_PLATING`), and two carry a part in their id but sit in no pool.

## Design

Each fact stays where it lives now, with the parts added where they're missing. Core then joins them into one index.

1. `ReservedModifiers` becomes `ModifierSources` and records the parts as well as the owner: `reserve(modifierId, ownerId, parts)`. It's a code-only rename. A source that grants to any part passes every hull item part.
   - `BiomeModifiers` passes the row's `item` part. A modifier listed for more than one biome, or the same biome on more than one part, adds each part.
   - `Engrams` and `StormLoot` pass every part, which matches what they grant today.
2. A new `ModifierParts` in `exiledsector.items` (core) maps each hull item modifier id to the parts it can appear on. It joins:
   - the generic pools and unique definitions, each adding its own part
   - `hull_item_implicits.csv`, each implicit adding its part
   - the corruption implicits, adding every part
   - `ModifierSources`, adding whatever parts each source registered

   It rebuilds when item data reloads or a source reserves or releases. Lookups never scan the catalogue.
3. `ItemStatPool` builds its per-part stat lists from `ModifierParts`. The fallback that puts unpooled stats on every part goes away.
4. A source switched off with its domain releases its reservation (`BiomeModifiers.unregisterAll` already does). Its modifiers then leave the index and the stat picker, the same way other content hooks skip switched-off domains.
   - Not built. Every domain's `loadItems` runs whatever its switch, and nothing calls `unregisterAll` on a switch-off, so the sources stay reserved and stay in the index. Releasing them would also stop `Item.explicitModifier` finding the reserved modifier on items the player already holds, since a reservation is what keeps those modifiers on an item. Leaving them listed lets the search still find those items.
5. A data integrity test in exiledsector-mod, which sees every domain, fails when a hull item modifier in `item_modifiers.csv` has no part in the index. Orphans like the 14 above can't come back unnoticed. The same test checks that pool entries in a generic row's prefix column are prefixes and those in its suffix column are suffixes, unless `ItemDataIntegrity` already checks that.

Why not a `parts` column on `item_modifiers.csv`? It would repeat what the generic pools already say, along with their weights and tiers. It would also make core own biome placement, which belongs to the Star Charts domain. The index gives a single source for each fact and keeps the domain split.

## Orphans

Delete all 14 rows from `item_modifiers.csv`, with their `socketable.modifier.<id>` keys in `zh_CN.json` (all except `WEAPON_DAMAGE_PERCENT`, which has none). These 12 have a pooled version of the same stat, named in the brackets:
- `ARMOR_PERCENT` (`ARMOR_PERCENT_ARMOR_PLATING`)
- `BURN_LEVEL_FLAT` (`BURN_LEVEL_FLAT_ENGINE_ROOM`)
- `DAMAGE_TO_DESTROYER_PERCENT` (`DAMAGE_TO_DESTROYER_PERCENT_BRIDGE`)
- `DAMAGE_TO_FRIGATE_PERCENT` (`DAMAGE_TO_FRIGATE_PERCENT_BRIDGE`)
- `ELECTRONIC_WARFARE` (`ELECTRONIC_WARFARE_BRIDGE`)
- `FIGHTER_REFIT_TIME_PERCENT` (`FIGHTER_REFIT_TIME_PERCENT_FLIGHT_DECK`)
- `FIGHTER_WEAPON_DAMAGE_PERCENT` (`FIGHTER_WEAPON_DAMAGE_PERCENT_FLIGHT_DECK`)
- `NON_BEAM_ENERGY_WEAPON_RANGE_PERCENT` (`NON_BEAM_ENERGY_WEAPON_RANGE_PERCENT_WEAPON_MOUNT`)
- `PD_DAMAGE_TO_MISSILES_PERCENT` (`PD_DAMAGE_TO_MISSILES_PERCENT_WEAPON_MOUNT`)
- `SENSOR_STRENGTH_FLAT_CREW_QUARTERS` (`SENSOR_STRENGTH_FLAT_BRIDGE`)
- `SHIELD_UPKEEP_MULT` (`SHIELD_UPKEEP_MULT_SHIELD_GENERATOR`)
- `WEAPON_DAMAGE_PERCENT` (`WEAPON_DAMAGE_PERCENT_WEAPON_MOUNT`)

None is in a drop pool in any release since 0.10.0, and no code or shipped data reads them by id. The only use left is the naming fallback in `ItemModifiers.namedForEffect`, for items saved before 0.10.0. Those stored a bare stat name, which loads as a modifier id equal to that stat. For nearly all of them the part's pool resolves the roll first, and the pooled versions share the names. A pre-0.10 item with an `ELECTRONIC_WARFARE` suffix would lose its "Interference" name word, since the Bridge version is a prefix. Saves before 1.0 don't constrain this.

`ENERGY_WEAPON_FLUX_COST_PERCENT` and `WEAPON_TURN_RATE_PERCENT_BRIDGE` have no pooled version and were last in a pool in 0.8.0, so they go too. Weapon turn rate stays on hull items as the Weapon Mount implicit; energy weapon flux cost leaves hull items and stays in skill trees and the Sindrian Diktat doctrine archive. Items from 0.8.0 lose "Traverse" and "Conservation" from their names, so `ReleasedItemsTest` now keeps names only for stats retired in 0.9.0 or later.

## Steps

1. [x] Rename `ReservedModifiers` to `ModifierSources` and add the parts argument, updating `BiomeModifiers`, `Engrams` and `StormLoot`. Built: core `ModifierSources` keeps an owner and its parts per modifier, and the same owner reserving again adds parts (another owner is still refused). `BiomeModifiers` passes the row's part, `Engrams` every part of the item types it takes, `StormLoot` every hull item part. Nothing persisted names the class, so it has no alias. Tests: `ModifierSourcesTest` (core), `BiomeModifiersTest`, `EngramTest`, `GrantedModifiersTest` (star-charts).
2. [x] `ModifierParts` with tests in core for each source, the corruption implicits and the rebuild on reserve and release. Built: core `ModifierParts.index()` joins hull item pools (generic and unique), each part's implicits, the corruption pool and `ModifierSources`, and rebuilds only when one of those changes. The corruption pool is `ItemImplicits.corruptionPool()`, which holds every part's implicits as well as the corruption-only ones, since corruption can put any of them on any part, so every hull item implicit lists every part. Tests: `ModifierPartsTest`.
3. [x] Switch `ItemStatPool` to `ModifierParts` and drop the every-part fallback, updating `ItemStatPoolTest`. Built: `ItemStatPool` reads each part's modifiers from `ModifierParts` and caches on the index. Tests: `ItemStatPoolTest` (exiledsector-mod, loading every shipped source through `ShippedModifierParts`) checks each part lists what it can carry and that a biome stat stays off parts nothing else gives it.
4. [x] Delete the 12 orphan rows and their zh_CN name keys, and handle the other two as decided. Run the full build to confirm no shipped-data test relied on them. Built: all 14 rows are gone from `item_modifiers.csv` and their 13 keys from `data/strings/exiledSector/core/zh_CN.json`; the full build passes.
5. [x] The integrity test in exiledsector-mod. Built: `ModifierPartsDataTest` loads every shipped source and fails on a hull item modifier in `item_modifiers.csv` with no part. The prefix and suffix column check already lives in `ItemDataIntegrity.referencedModifier`, run on shipped data by `ItemDataIntegrityTest`, so it isn't repeated.
6. [x] Check whether `tools/editor` serialises any file this changes. As planned, no data file gains a column, so this should be a no-op. Checked: the editor reads and writes `item_modifiers.csv` but only lost rows, and its round-trip and modifier tests pass; it doesn't touch `zh_CN.json`. No change.
7. [x] Update the Advanced search section of `docs/docs.md` if it mentions the every-part fallback. Done: the section now says the list comes from `ModifierParts`.
