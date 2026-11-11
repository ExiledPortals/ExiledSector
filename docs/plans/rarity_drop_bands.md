# Rarity drop bands

Item Rarity should change what kind of loot drops; Item Quantity changes how much. Today rarity (`ItemChances.rarity`,
fed by the chart suffix `CHART_ITEM_RARITY_PERCENT` and Spatial Breach depth) only weights hull item modifier counts
above the common maximum (`AffixLayout.rolledModifierCount`) and scales unique hull item chances (`ItemDrops`,
`BattleItemDrops.Drop.rare`). Every other row of `battle_loot.csv` ignores it, so a high-rarity boss drops the same
crafting items as a pirate patrol.

This follows Path of Exile 1's drop pool change: there, the rolled rarity gates which drop pools can be picked, so
common currency drops only on a normal roll and valuable currency only on a magic or rare one. We keep our per-row
expected counts and get the same effect by shifting the expected count between bands instead of rolling a rarity
first.

## Rules

1. Each `battle_loot.csv` row gets a `rarity_band`: `common`, `rare` or `unique`. Rows that already respond to rarity
   another way (`hull_item`, `unique_hull_item`) keep their current handling and take `none`.
2. At rarity 1x nothing changes, so fleets with no rarity source (everything outside Star Charts) drop as now.
3. Above 1x, each fleet's expected count for every `rare` and `unique` row is multiplied by the rarity multiplier, and
   the expected count this adds is taken off the `common` rows in proportion to their share, floored at zero. The
   battle's total expected count stays the same; only the mix improves.
4. Below 1x (if a drawback ever lowers rarity) the shift runs the other way.
5. The shift applies per enemy fleet after the curve and difficulty, before Item Quantity and the boss's spoils
   multiplier, so quantity scales the reshaped mix.

## Bands

The current rates already sort the crafting items. Common: augmentation, fundamental and reshaping nanoswarms. Rare:
corrupted, metamorph, annulment, tuning and immutable nanoswarms, and both inhibitors. Unique: the replicator nanoseed.
Subroutines and the schematics (refit, logistics, deintegration, master) are still to be placed; master schematics are
gated by `MasterSchematic.required()` and may be better left at `none`.

## Open questions

1. Whether the band shift should also cover chart cargo and site finds, or only battle drops.
2. Whether common crafting items still come in fast enough from regular fleets and markets once high-rarity charts
   drop fewer of them.
3. The Treasure suffix range (+15-60%) was set for hull items only; it needs a fresh placeholder once rarity also
   reshapes crafting drops (`docs/analysis/balance.csv`).

## Steps

1. `rarity_band` column in `battle_loot.csv`, read into `BattleLootRates.Rate`; missing means `none`.
2. One core helper that takes a fleet's expected counts per row and its rarity multiplier and returns the shifted
   counts, used by `BattleMaterials`, `ChartMaterials`, `ChartFleetLoot` and `SkillTreeSchematicDrops` so the bands
   are applied in one place.
3. Tests for the helper (identity at 1x, total preserved, common floored at zero) and for each caller.
4. `docs/docs.md` (Battle loot): one paragraph on what rarity changes.
