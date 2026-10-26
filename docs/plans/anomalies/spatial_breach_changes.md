# Anomaly: Spatial Breach changes

There are three changes to the Spatial Breach (built in `../completed/anomalies/spatial_breach.md`):

1. The breach core no longer gives loot and can't be interacted with. The caches are the breach's only loot. The breach runs its course and closes by itself, with nothing at the end.
2. The core item modifier line becomes a cache item line.
3. Star Charts no longer roll any anomaly content, whether breach, storm or flare. Anomalies only come from the atlas: a cell rolls one, or has one inserted.

Status: planned, nothing built.

## Part 1: no core interaction, no core loot

### Today

- The breach starts closed. It opens when the player fleet flies over its core, grows to full size over 60 seconds, then collapses over 60 seconds. Caches form as the edge grows past them and vanish as it collapses back past them. At the end, `BreachTerrainPlugin.collapse` removes the field and the core. This automatic close stays.
- The core can also be clicked. `ChartCampaignPlugin` opens `BreachCoreDialog` for it, and "Close the breach" ends the breach early (`BreachTerrainPlugin.close`), removes every cache still inside, and rolls loot on the spot. That roll is `ChartSites.siteSalvage` with an item multiplier of `0.5 + 1.5 × grown fraction` times `CHART_BREACH_CORE_ITEM_PERCENT`, a rarity bonus of 50-100%, +20% per tier above 1 (`BreachRules.coreItemMult`, `coreRarityPercent`, `coreSalvageMult`), and the `exiledSector_chart_breach_core` row in `item_salvage.csv`. All of this goes.

### Changes

- The core stays as the breach's centre: it's what the fleet flies over to open the breach, and `BreachCorePlugin` draws it. It gets vanilla's `Tags.NON_CLICKABLE` when it's created in `SpatialBreach.generate`, so it can't be clicked or interacted with. Cores in saves made mid-run don't have the tag. `BreachTerrainPlugin` adds it on its first `advance` if it's missing.
- Remove `BreachCoreDialog`, its branch in `ChartCampaignPlugin`, and the dialog's tests.
- Remove the core loot: `BreachRules.coreItemMult`, `coreRarityPercent`, `coreSalvageMult` and their constants, `SpatialBreach.ITEM_MULT_KEY` and `RARITY_KEY`, and the `exiledSector_chart_breach_core` row in `item_salvage.csv`. Check the salvage loader doesn't report the missing row as a problem anywhere else.
- `BreachTerrainPlugin.close` stays, but only for `collapse` and run cleanup, so it becomes package-private. It no longer has to return the grown fraction. If nothing else reads `peakRadius` and `grownFraction` once the core loot is gone, remove them too.
- Caches stay as they are: they form as the breach grows and vanish when it collapses past them. Whatever the player hasn't salvaged by then is lost.
- The core keeps its name, "Breach Core", and its map icon, because it's still the thing to fly over.

## Part 2: core items become cache items

`CHART_BREACH_CORE_ITEM_PERCENT` becomes `CHART_BREACH_CACHE_ITEM_PERCENT`, with the line "<good>{value}%</good> increased quantity of items found in breach caches". It feeds the breach cache salvage roll, replacing the core roll. `SpatialBreach.CORE_ITEM_KEY` becomes `CACHE_ITEM_KEY`, and `BreachTerrainPlugin.Params.coreItemMult` becomes `cacheItemMult`. An old save's field is just ignored, and a missing value counts as 1.

After Part 3, no chart rolls this line or the other breach tuning lines (`CHART_BREACH_GROWTH_PERCENT`, `CHART_BREACH_CACHE_PERCENT`). The three tuning effects stay in `ChartEffect` so that an atlas source can carry them later, through an anomaly's `effects` column in `chart_anomalies.csv` or an atlas node's effects.

## Part 3: no anomaly rolls on Star Charts

Star Charts roll no anomaly content at all: no Spatial Breach, Asteroid Storm, Magnetic Storm or Stellar Flares lines. Every anomaly comes from the atlas.

- Delete the anomaly modifiers from `star_chart_modifiers.csv` (`CHART_BREACH_CHANCE`, `HYBRID_CHART_BREACH_GROWTH`, `HYBRID_CHART_BREACH_CACHES`, `HYBRID_CHART_BREACH_CORE`, `CHART_ASTEROID_STORM_CHANCE`, `CHART_MAGNETIC_STORM_CHANCE`, `CHART_STELLAR_FLARES_CHANCE`), along with their seven `@` weight entries in `star_charts.csv`.
- Delete the chance effects `CHART_BREACH_CHANCE`, `CHART_ASTEROID_STORM_CHANCE`, `CHART_MAGNETIC_STORM_CHANCE` and `CHART_STELLAR_FLARES_CHANCE`, with their `chart.effect.*` and `.chance` strings. They're the only chance effects, so remove the chance machinery too: the `Chance` record and `isChance()` in `ChartEffect`, `anomalyChances`, `addAnomalyChance` and `anomalyChance` in `ChartGenContext` (and its debug log line), the roll loop and `ROLL_SALT` in `ChartAnomalies.planned` (which keeps only the forced, atlas-given anomalies), and the "an anomaly can't add anomalies" check in `ChartAnomalies.parse`.
- Charts that already exist carry these lines. Only our own saves matter before 1.0, so they just lose them. Check how the item modifier loader handles a modifier id or effect name that no longer exists. If it fails, make it drop the line with a single log message, so old charts still load and open.
- Check the item modifier integrity checker and any tests that list the chart modifiers (`StarChartSiteModifiersTest`, `ChartContentDataTest` and others), and remove the deleted ids from them.

Without chart rolls, the atlas is the only source of anomalies. `ChartGenContext.forceAnomalies` lists the anomaly once, so a system gets at most one breach. Two breaches in a system, which would need indexed cores, per-breach cache ownership, spacing and a count line, isn't needed any more. If the atlas later allows several inserted breaches in one system, that work comes back in its own plan. Note for that plan: `BreachTerrainPlugin.close` removes every `CACHE_TAG` entity in the system, so it would need fixing first.

## Text

All text goes through `exiledlib.i18n`, in `en.json` and `zh_CN.json`, following `docs/tooltip_wording.md`.

- `chart.breach.core.description`: the core is the knot at the tear's centre that sets the breach off. Nothing about closing it or what it leaves.
- `chart.breach.description` and `chart.breach.effect.depth`: drop "Closing its core ends it and removes the caches still inside".
- `chart.anomaly.spatial_breach.intel` and `chart.atlas.anomaly.spatial_breach.description`: drop "closing its core ends it".
- The description column for `spatial_breach` in `chart_anomalies.csv`: drop "a core that closes it".
- `chart.effect.CHART_BREACH_CORE_ITEM_PERCENT` becomes `chart.effect.CHART_BREACH_CACHE_ITEM_PERCENT`, with the cache wording above.
- Remove `chart.breach.core.inert`, `.option.close`, `.confirm`, `.confirm.yes`, `.confirm.no`, `.closed` and `.salvage`.

## Build order

1. [ ] Make the core non-clickable, including on old cores. Remove `BreachCoreDialog`, its `ChartCampaignPlugin` branch, the core loot code and constants, and the `item_salvage.csv` row. Shrink `close` to the collapse path. Tests: the core can't be interacted with, and collapsing removes the field, the core and the remaining caches with no loot.
2. [ ] Rename the core item effect to the cache item effect and apply it in the cache salvage roll. Test that a higher value gives more items per cache.
3. [ ] Delete the seven anomaly modifiers and their weights, the four chance effects and their strings, and the chance machinery in `ChartEffect`, `ChartGenContext` and `ChartAnomalies`. Make old charts drop unknown lines when they load. Tests: no chart can roll an anomaly line, `planned` returns exactly the atlas-given anomalies, and a chart carrying a removed line still loads and opens.
4. [ ] Text in en and zh_CN.
5. [ ] Update `docs/docs.md`: the Spatial Breach section (automatic close, caches as the only loot) and the chart modifier text (anomalies come only from the atlas). Mark the core reward steps and the chart modifier section in `../completed/anomalies/spatial_breach.md` as replaced by this plan, fix any mention of chart chance lines in `../atlas.md` and `overview.md`, then move this plan to `../completed/anomalies/`.
