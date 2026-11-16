# Rarity drop bands

Crafting item drops follow Path of Exile 1's drop pool model. How many items drop depends on how much you destroy
(deployment points, then Item Quantity). Which items they are depends on a band roll per item, skewed by Item Rarity,
then a weighted pick within the band. This replaces three separate weight tables (the crafting rows of
`battle_loot.csv`, `chart_materials.csv` with its per-tier targets and fleet-point shares, and each family's
`dropWeight` used by salvage sites) with one count rule and one weight table.

Today rarity (`ItemChances.rarity`, fed only by the chart suffix `CHART_ITEM_RARITY_PERCENT`, Treasure) weights
subroutine and salvage modifier counts and scales unique hull item chances. Crafting items ignore it. Those jobs
stay as they are.

## Rules

1. The count from a fleet is 1.13 crafting items per 100 DP lost (a tier 15 chart's fleets and boss drop 50) ×
   the fleet's share × Item Quantity × the fleet's loot dial. The loot dial is an anomaly's `fleet_loot` or the
   boss's spoils. Crafting items ignore battle difficulty and the per-battle diminishing curve, as chart crafting
   already does. The fleet ledger still pays each chart fleet once, by the share of its starting DP destroyed, so
   rebuilt defenders and station launches add nothing.
2. Chart fleets have a share of 1. Fleets outside charts share one level-scaled factor for every `battle_loot.csv`
   row, replacing the per-row `regular_share`: 0.5 + 0.5 × (player level - 1) / (level cap - 1), read against the
   game's level cap rather than a fixed 15. At the level cap a fleet outside a chart drops per DP exactly what a
   tier 1 chart fleet does: the same crafting count at band shift 1x, with the chart-only items' share going to the
   others, and the same hull items, uniques and schematics. Subroutines stay chart-only.
3. Every crafting item has one weight in `crafting_drops.csv`, along with whether it drops only inside charts
   (ciphers and tag inhibitors). Each source sets only the count and which items it may give. Fleets, salvage
   sites, chart salvage, Atlas officer rewards and excavation tag inhibitors all pick from that one table. Engrams
   keep their own officer reward weights.
4. A crafting item's band is its material tier (`MaterialTier`: common, rare, unique, mythic), so the bands always
   match the tiers shown in game.
5. Each drop first rolls a band, weighted by the summed weights of the items the source may give in that band.
   The rare, unique and mythic weights are multiplied by the band shift, and common stays at 1. It then picks an
   item within the band by weight. Commons never reach zero, so no floor is needed.
6. The band shift is the chart tier's base shift × (1 + increased Item Rarity). The tier's base shift sits in a
   `band_shift` column of `star_chart_tiers.csv`, set per tier so each tier keeps today's rare-or-better share
   (1x at tier 1, 1.54x at tier 3, 1.77x at tier 5, 1.95x at tier 8, 2.01x at tier 10, 2.11x at tier 15). It applies
   to crafting bands only, so unique hull items and modifier counts don't change with tier. Treasure and later
   rarity sources multiply on top, so +30% is equally strong at every tier.
7. Hull items found anywhere in a chart (caches, sites, derelicts, officer rewards) use the chart's rarity, not
   only battle drops.

## Model results

From `docs/analysis/chart_loot/rarity_band_model.js` (writes `rarity_band_tiers.csv`, `rarity_band_items.csv` and
`rarity_band_outside.csv`). The model covers an unmodified chart, fleets plus boss only; sites and the objective
reward drop on top. The item weights are today's tier 15 rates per 100 DP.

A chart holds about 400 DP at tier 1 and 4400 at tier 15. Today's targets are linear in tier, but chart DP grows
faster than linear, so today's middle tiers drop more per DP than tier 15. One rate takes that extra away:

| Tier | Chart DP | Today | Plan | Change |
|---|---|---|---|---|
| 1 | 401 | 7.6 | 4.5 | -40% |
| 3 | 632 | 14.2 | 7.1 | -50% |
| 5 | 985 | 20.7 | 11.1 | -46% |
| 8 | 1664 | 30.5 | 18.8 | -38% |
| 10 | 2327 | 37.1 | 26.3 | -29% |
| 12 | 3017 | 43.6 | 34.1 | -22% |
| 15 | 4418 | 53.4 | 50.0 | -6% |

The per-tier band shift keeps every tier's mix as it is today, so each band falls by the same percentage as the
total. For example, Corrupted at tier 8 goes from 5.2 to 3.2 and the unique band from 2.45 to 1.47, both -38%.

Common share of a chart's crafting items: 80% at tier 1, 68% at tier 8 and 66% at tier 15. With Treasure +30%
those become 76%, 62% and 60%.

Augmentation stays far more common than Immutable at every tier. At tier 15 a chart drops 8.6 Augmentations to
0.73 Immutables (7.8 to 0.86 with Treasure +30%). Immutables would only outdrop Augmentations at a total band
shift of 24.7x, which is 11.7x on top of tier 15's base.

Within the bands, fixed weights move a few items against today's targets. Fundamental is 0.14 at tier 1 instead
of 0.075, and Annulment and Tuning drop to a third of today at tier 1 (0.05 instead of 0.15) because today's tier 1
targets give them more than their tier 15 weight would.

Outside charts, a 300 DP fight drops 1.7 crafting items at level 1 and 3.4 at the level cap, against 0.99 today.
The mix is a tier 1 chart's, so rare and higher items rise much more than the total:

| Item | Today | Level 1 | Level cap |
|---|---|---|---|
| Augmentation | 0.45 | 0.57 | 1.13 |
| Fundamental | 0.30 | 0.08 | 0.17 |
| Reshaping | 0.15 | 0.57 | 1.13 |
| Corrupted | 0.045 | 0.29 | 0.57 |
| Unique band | 0.036 | 0.13 | 0.26 |

The other rows against today's `regular_share`:

| Row | Today | Level 1 | Level cap |
|---|---|---|---|
| Hull items | 0.68 | 0.5 (-27%) | 1 (+47%) |
| Unique hull items, Refit, Logistics and Master Schematics | 0.375 | 0.5 (+33%) | 1 (+167%) |
| Deintegration Schematics | 0.125 | 0.5 (x4) | 1 (x8) |

Hull items, unique hull items and the schematics only drop from the hull items drop level setting (15 by default),
so at default settings they always drop at the level cap factor. Hull items would only fall below today under level
6 with that setting lowered.

## Open questions

Each is built with one answer for now: a tier 1 chart's mix outside charts, Fundamental's derived weight, the
curve kept for the other rows, one shift for every band above common and vanilla finds unchanged.

1. Whether fights outside charts should keep a tier 1 chart's mix, or keep today's common-heavy mix with a band
   shift below 1x (about 0.3x keeps today's rare share).
2. What Fundamental's single weight should be; today it is 0.8 per 100 DP outside charts and 0.03 to 0.07 inside.
3. Whether hull items, subroutines and schematics should also drop the per-battle diminishing curve.
4. Whether higher bands should shift faster than rare (for example the shift squared for unique and mythic).
5. Whether vanilla finds in a chart (weapons, blueprints, AI cores) should respond to rarity at all; they have no
   tier today.

## Steps

1. Done: `crafting_drops.csv` (item, weight, chart only) and the core helper `CraftingDrops` (`exiledsector-core`,
   `exiledsector.items`). One weighted pick by weight × band shift (common at 1x) gives the same odds as a band roll
   then an in-band pick, so the helper does that in one step. Tests in `CraftingDropsTest`.
2. Done: `BattleMaterials` uses the one `crafting_item` row (1.132 per 100 DP) in `battle_loot.csv` and the helper;
   the per-item crafting rows and the `regular_share` column are gone. `FleetLootRules.share` gives fleets outside
   charts `OutsideChartShare` (`exiledsector.loot`) for every row, read against the game's level cap, on top of the
   hull items drop level gate. Tests in `BattleMaterialsTest`, `BattleLootRatesTest`, `OutsideChartShareTest` and
   `BattleItemDropsTest`.
3. Done: `ChartFleetLoot.craftingItems` counts from claimed DP at the same rate; `ChartMaterials`,
   `ChartMaterialsTest` and `chart_materials.csv` are gone; `band_shift` in `star_chart_tiers.csv`, read by
   `ChartTiers.bandShift` and `ChartGenContext.craftingBandShift`; `OfficerRewardPool` weights crafting items by
   `CraftingDrops.shiftedWeight` at the run's shift (`RewardContext.craftingBandShift`). Tests in
   `ChartFleetLootTest`, `ChartTiersTest` and `OfficerRewardPoolTest`.
4. Done: `ItemDrops` picks site crafting items through the helper (outside-chart items, the site's band shift);
   `ChartDrops` picks chart salvage and run reward ciphers through it. `dropWeight` is gone from the crafting
   families, `StarChartCipher` and `Engram`. Tests in `ItemDropsTest`, `ChartDropsTest` and `StarChartCipherTest`.
5. Done: `ExcavationLoot.tagInhibitors` picks through the helper, inhibitors that inhibit a tag only, at the run's
   band shift. Tests in `ExcavationRulesTest`.
6. Done: caches, sites, black hole derelicts and spatial breach already took `ChartGenContext.lootMultipliers`. The
   boss guarantee in `ChartFleetLoot`, non-unique officer rewards (`OfficerRewardDrop`, through
   `ItemData.rolledAtTierWithRarity`) and the objective reward (`ChartRewards`) now take the chart's rarity too.
7. Done: Treasure in `star_chart_modifiers.csv` at low 10-15 / mid 15-22 / high 22-30 with the `crafting_item` tag, and
   its line and totals text in en and zh_CN name hull items, subroutines and crafting items (star charts and vanilla
   finds don't respond to rarity).
8. Done: `chart_loot_model.js` reads `crafting_drops.csv`, the `crafting_item` rate and `band_shift`;
   `rarity_band_model.js` keeps the old amounts in the script. `docs/docs.md` has a Rarity section under Star Charts
   and the outside-chart share in Drops.
