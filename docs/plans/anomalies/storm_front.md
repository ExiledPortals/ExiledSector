# Anomaly: Storm Front

An ion storm front drifts slowly across the system on a path you can see in advance. Any battle fought inside it is fought in the storm: lightning arcs strike ships on both sides every few seconds. Battles inside drop more loot, so where you fight becomes the decision.

Status: built 2026-10-29 apart from the in-game checks (step 2's Terrain FX pass, strike feel, visuals); move this plan to `completed/anomalies/` once those are done. Proposed 2026-10-28. See `overview.md` for the shared framework.

## How it plays

The storm is a custom campaign terrain on `BaseTerrain`, a wide band that enters at the system edge, crosses at least two orbits with POIs or planets, and leaves at the far edge over a fixed number of days (placeholder 30), then dissipates. Its path and timing show in chart intel from the start of the run, as the Supply Convoy's route does. Inside it, every fleet's sensor range is reduced (placeholder), so the storm is also cover.

The storm adds no fleets. Chart fleets, bosses, convoys and anomaly fleets can all end up fighting in it, depending on where you catch them. A fleet the storm passes over isn't pushed or damaged in the campaign layer.

## Battles in the storm

A battle is in the storm when the player fleet is inside the terrain as the engagement starts. The flag is recorded then, the way `ShaftEncounters.battleContextCreated` records the cavern battle, and a combat plugin of ours reads it.

In the battle, an arc strikes every few seconds (placeholder 6), hitting a random ship on the field weighted by hull size, from either side. Strikes are EMP arcs (`CombatEngineAPI.spawnEmpArc`) that shields can block and phased ships can't be hit by, so raising shields and phasing are how you weather them. A brief flash and drifting cloud overlay are drawn by a `CombatLayeredRenderingPlugin` of ours, GL11 only, with no `glGet` calls while drawing (Fast Rendering rules). Combat nebula tiles are never used, per the Foiler mods rules in `../atlas.md`.

## Rewards

Battles in the storm drop increased item quantity (placeholder +50%), added in `ChartFleetLoot` from the flag recorded at the start. Hull items from those battles can also roll storm-only modifiers (`item_modifiers.csv` rows with the `_STORM_FRONT` suffix, as Anomalous Debris' engram modifiers carry `_ANOMALOUS_DEBRIS`), placeholder lines with an energy and EMP theme.

## With other anomalies

The storm counts as terrain for the Supply Convoy's escorts (`ConvoyTerrain`), so they drift apart inside it like they do in nebulae and asteroid fields.

## Chart modifiers

Tuning lines are chart effects carried by an atlas source, per `overview.md`:

- `CHART_STORM_FRONT_LOOT_QUANTITY_PERCENT`: increased quantity of items dropped by battles in the storm.
- `CHART_STORM_STRIKE_FREQUENCY_PERCENT`: increased frequency of storm strikes.
- `CHART_STORM_FRONT_SIZE_PERCENT`: increased Storm Front size.

## Build order

1. [x] The anomaly class (`exiledsector.charts.anomaly.storm`) and its insertable `chart_anomalies.csv` row; the storm terrain plugin with its path, movement, sensor penalty and renderer; route and timing in chart intel. Built: `StormFront` adds one `exiledSector_chart_storm_front` terrain (`data/campaign/terrain.json`, vanilla's hyperspace storm loop) whose band is 3000 su wide and as long as the system is wide (twice the distance to the outermost world or POI plus 1500 su, at least 6000 su), on a random heading; it starts beyond one edge when the run opens and leaves past the far edge after 30 days, so it crosses every orbit (`StormPath`, numbers in `StormRules`, placeholders). `StormFrontTerrainPlugin` keeps only numbers, text and the run's start timestamp, moves its entity with the band, gives every fleet inside 50% less sensor range (a temporary multiplier, as vanilla's nebula applies its own), and draws drifting `cleaner_clouds00` puffs tinted ion blue with `starburst_glow1` flashes; the map shows the puffs. Chart intel, through `ChartAnomaly.intelText`, gives the days until it leaves and the next three worlds or POIs it reaches with their days. Textures preloaded from `StarChartsDomain.onApplicationLoad`; save alias `exiledSector.StormFrontTerrainPlugin`. Tests: `StormPathTest`, `StormFrontTest`.
2. [~] The in-storm battle flag, the strike plugin and its overlay. In-game pass with Terrain FX on. Built: `StormBattlePlugin`, registered in `data/config/settings.json` like `ChartBattlePlugin`, turns on in `init` when the player fleet is inside a front (`StormBattles.playerFront`; the campaign is paused through the whole encounter, so this reads the same state as the engagement start). Every 6 seconds (scaled by the strike line) it fires `spawnEmpArc` with a null source ship (the engine's `EmpArcEntity` builds the damage without source stats then) from 600 to 900 su away at a ship picked by `StormStrikes`: not a fighter, hulk, module or phased ship, weighted 1/2/3/4 by hull size, either side; 200 energy damage and 800 EMP, 10% more per tier above 1 (placeholders), blocked by shields like any non-piercing arc. `StormOverlay`, a `BaseCombatLayeredRenderingPlugin`, draws ten slow `cleaner_clouds00` puffs below the ships and a `starburst_glow1` plus `hit_glow` flash at each strike, with a GraphicsLib light through `CombatGlow`; Terrain FX draws nothing for our terrain. Tests: `StormRulesTest`. Not done: the in-game pass.
3. [x] The loot bonus in `ChartFleetLoot` and the storm-only hull item modifiers. Built: `StarChartLootSource.recordEngagement` records whether the player fleet is in a front at the battle's first engagement, so auto-resolved battles count too; `ChartFleetLoot.roll` then multiplies each chart fleet's share by 1.5 plus the quantity line (`StormRules.lootQuantityMult`) and passes the result through `StormLoot.touch`, which rerolls 25% of hull items around one storm modifier the way an engram does (`GrantedModifiers` in star charts `exiledsector.items`, over `AnomalousDebrisCurrency.reroll`). The two placeholder modifiers in `item_modifiers.csv`, `EMP_DAMAGE_TAKEN_PERCENT_STORM_FRONT` (prefix Stormproof, 25% reduced EMP damage taken) and `NON_BEAM_ENERGY_WEAPON_CHAIN_CHANCE_PERCENT_STORM_FRONT` (suffix Thunderhead, 8% chain chance), are reserved at item load (`StormFront.loadItems`, `StormLoot.reserveModifiers`), so no other roll draws them. Tests: `GrantedModifiersTest`.
4. [x] `ConvoyTerrain` counting the storm; chart modifier lines, strings (en and zh_CN), the atlas marker icon, the `docs/docs.md` section and tests. Built: `ConvoyTerrain.slowsConvoys` takes the front; `ChartEffect.CHART_STORM_FRONT_LOOT_QUANTITY_PERCENT`, `CHART_STORM_STRIKE_FREQUENCY_PERCENT` and `CHART_STORM_FRONT_SIZE_PERCENT` through `ChartGenContext.addAnomalyPercent`, carried by no source yet; `chart.storm.*`, atlas, effect and modifier name strings in both catalogues (zh a draft); the atlas marker `graphics/icons/anomalies/storm_front.png`, a circular crop of vanilla's `hyperspace1` background (placeholder); the `docs/docs.md` section. Tests: `ConvoyTerrainTest`, `ChartPersistenceGuardTest`.
5. [x] The Storm Terminus catalyst (`atlas_catalysts.md`): the `atlas_catalysts.csv` row (terminus family), its `special_items.csv` row and name strings (en and zh_CN); it loads once the anomaly has its icon. Built: `exiledSector_storm_terminus` in both files with the termini's shared flavour, `catalyst.exiledSector_storm_terminus.name` in both catalogues.

## Open questions

- Whether `spawnEmpArc` accepts a null source ship, or the strikes need an invisible source entity. Answered from the decompiled `EmpArcEntity`: a null source builds the damage without source stats and passes null to `applyDamage`, so no source entity is needed.
- Strike damage and EMP by tier, and whether strikes should favour the side with more ships on the field. Placeholder: 200 energy and 800 EMP, 10% more per tier, no lean toward either side.
- Whether ships near a strike should get a short boost (energy weapon damage, flux dissipation) so the storm isn't only a hazard. Not built for now.
- Whether Terrain FX should draw anything for our terrain in the battle background, or leave it to our overlay. Decided: our overlay only.
- Whether a second front should follow once the first has crossed, so a long run still has a storm. Decided for now: one front per run.
