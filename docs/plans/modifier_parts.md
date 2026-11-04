# Modifier parts

Status: planned, nothing built.

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
5. A data integrity test in exiledsector-mod, which sees every domain, fails when a hull item modifier in `item_modifiers.csv` has no part in the index. Orphans like the 14 above can't come back unnoticed. The same test checks that pool entries in a generic row's prefix column are prefixes and those in its suffix column are suffixes, unless `ItemDataIntegrity` already checks that.

Why not a `parts` column on `item_modifiers.csv`? It would repeat what the generic pools already say, along with their weights and tiers. It would also make core own biome placement, which belongs to the Star Charts domain. The index gives a single source for each fact and keeps the domain split.

## Orphans

Delete these 12 rows from `item_modifiers.csv`. Each has a pooled version of the same stat, named in the brackets. Delete their `socketable.modifier.<id>` keys from `zh_CN.json` too (all except `WEAPON_DAMAGE_PERCENT`, which has none).
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

Still to decide: `ENERGY_WEAPON_FLUX_COST_PERCENT` and `WEAPON_TURN_RATE_PERCENT_BRIDGE` have no pooled version, so deleting them would take the stat off hull items entirely. The obvious homes are the Weapon Mount or Reactor pool for the first and the Bridge pool for the second. The integrity test in step 5 can't land until both have an answer.

## Steps

1. Rename `ReservedModifiers` to `ModifierSources` and add the parts argument, updating `BiomeModifiers`, `Engrams` and `StormLoot`.
2. `ModifierParts` with tests in core for each source, the corruption implicits and the rebuild on reserve and release.
3. Switch `ItemStatPool` to `ModifierParts` and drop the every-part fallback, updating `ItemStatPoolTest`.
4. Delete the 12 orphan rows and their zh_CN name keys, and handle the other two as decided. Run the full build to confirm no shipped-data test relied on them.
5. The integrity test in exiledsector-mod.
6. Check whether `tools/editor` serialises any file this changes. As planned, no data file gains a column, so this should be a no-op.
7. Update the Advanced search section of `docs/docs.md` if it mentions the every-part fallback.
