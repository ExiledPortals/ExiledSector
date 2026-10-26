# Star Charts package layout

`exiledsector.charts` holds 98 classes in one flat package, next to small subpackages (`anomaly` with 14, `atlas` with 30, `feature` with 6) and two empty leftover folders (`expansion` and `phenomenon`, in both `main` and `test`). Finding a class means already knowing its name. This plan sorts the root into subpackages by the stage of a chart's life each class belongs to. It only moves classes: no behaviour changes, no class renames.

Status: in progress on branch `charts-packages`.

## Constraints

- Saves. `ChartSaveAliases` gives XStream aliases to the persisted chart classes (`ChartRun`, `ChartRunState`, `ChartWormholePlugin`, `ChartObjectiveIntel`, `WormholeGateway` and the three Breach classes), so they can change package safely. `AtlasState` sits in sector persistent data with no alias, along with whatever it holds, so moving it would break saves. Add aliases for it and every class it stores before it moves, or leave it where it is. Run a save made before the move through `SaveFixtureTest` (exiledsector-mod) afterwards.
- Data files name these classes: `ChartBattlePlugin`, `ChartWormholePlugin`, `WormholeGateway`, `BreachCorePlugin`, `BreachTerrainPlugin` (in `settings.json`, `custom_entities.json` and `terrain.json`), plus `StarChartItemPlugin` and the console commands. Every moved class that data names gets its data entry updated in the same commit, and a test checks that each class name in `data/` resolves (extend `ChartContentDataTest` or add a small one).
- Package-private access. 20 root classes are package-private (for example `BuiltInTable`, `PrivateField`, `ChartSalvorsTally`, `ChartVanillaLoot`, `ChartEnemyDamageListener`), and many public classes have package-private members that tests and siblings use. Keep each tightly bound group in one package so the access stays package-private. Make something `public` only where a group boundary really cuts through it, and list those cases in the commit.
- Tests mirror the main packages. That includes the four `exiledsector.charts` tests in `exiledsector-mod` (`ChartContentDataTest`, `ChartDropsTest`, `ChartFleetHullItemsTest`, `ChartTierLevelsTest`), which move with the classes they test.
- Other modules import from `exiledsector.charts`: `ExiledSectorModPlugin` and some tests in `exiledsector-mod`. Their imports change. Nothing outside this plugin and those tests should need to.
- Merges. Moving whole files conflicts with any branch that has unfinished edits to them. Do the move in one sitting on `parallel-work` while no other worktree has chart work open, then tell the other worktrees to merge it before they touch charts again. Volume branches seldom touch charts, but check `git worktree list` and each branch's diff against `parallel-work` under `exiledsector-star-charts` first.

## Target layout

All under `exiledsector.charts`. Class names stay as they are.

### Root (wiring and shared ids)
`StarCharts`, `StarChartsDomain`, `StarChartSettings`, `ChartIds`, `ChartSaveAliases`, `BuiltInTable`

`BuiltInTable` stays here because anomalies, bosses, features and POIs all use it. If that forces it public, it can instead go to a `charts.table` package.

### `run`: a chart run from entering to closing
`ChartRun`, `ChartRuns`, `ChartRunState`, `ChartRunBound`, `ChartRunActions`, `ChartRunScript`, `ChartRunEffects`, `ChartBlockedAbilities`, `ChartCampaignPlugin`, `ChartContentListener`

### `run.cleanup`: making a closed run leave no trace
`ChartRunCloser`, `ChartLoadSweep`, `ChartSalvorsTally`, `ChartTopographyScans`, `ChartUiData`, `ChartUsedNames`, `PrivateField`

`ChartUsedNames` is also used by `ChartLayoutGenerator`, so its `count` and `register` methods become public.

### `run.diagnostics`: soak tests and census
`ChartCensus`, `ChartSystemContents`, `ChartSoakScript`

### `wormhole`: the gateway, the wormholes and buying charts
`WormholeGateway`, `ChartSupply`, `ChartWormholePlugin`, `ChartWormholeDialog`, `ChartWormholeBand`, `ChartWormholePlacement`, `ChartWormholeVisual`

### `generation`: building a chart system from an item
`ChartSystemFactory`, `ChartGenContext`, `ChartGenerated`, `ChartTier`, `ChartTiers`

`ChartGenContext` is used by 24 files. It goes here and becomes the main type other packages import.

### `layout`: stars, planets and terrain
`ChartLayout`, `ChartLayoutEngine`, `VanillaChartLayoutEngine`, `ChartLayoutGenerator`, `ChartLayoutRecipe`, `ChartLayoutRequest`, `ChartLook`, `ChartShape`, `ChartStarSystemGenerator`, `ChartDeepSpace`, `ChartBodyNames`

The current `charts.feature` package becomes `layout.feature`, with `ChartFeatures` moving into it.

### `layout.sketch`: the atlas preview of a system before it exists
`ChartSystemSketcher`, `ChartSystemSketch`, `ChartSketchPalette`

### `content`: fleets, sites and factions placed into the layout
`ChartContent`, `ChartContentEngine`, `VanillaChartContentEngine`, `ChartContentGenerator`, `ChartContentPlan`, `ChartContentPlanner`, `ChartFleetSpread`, `ChartSpot`, `ChartSites`, `ChartPoi`, `ChartPois`, `ChartBosses`, `ChartBossType`, `ChartThreatFleets`, `ChartFactions`, `ChartFaction`, `ModdedChartFactions`, `ChartColonyItems`

### `objective`: what finishes a chart and what it rewards
`ChartObjective`, `ChartObjectives`, `ChartObjectiveIntel`, `ChartRewards`

### `effect`: chart modifiers and how they reach fleets and combat
`ChartEffect`, `ChartEffects`, `ChartFactionEffect`, `ChartFleetStat`, `ChartFleetConditions`, `ChartFleetConditionSource`, `ChartEnemyDamageListener`, `ChartBattlePlugin`

### `loot`: drops from fleets, sites and completion
`ChartLoot`, `ChartDrops`, `DropType`, `LootContexts`, `ChartLootTotals`, `ChartVanillaLoot`, `ChartFleetLoot`, `StarChartLootSource`

### `anomaly`
This package stays, but the six Spatial Breach classes (`SpatialBreach`, `BreachCorePlugin`, `BreachFieldRenderer`, `BreachRules`, `BreachSupplyBuff`, `BreachTerrainPlugin`) move to `anomaly.breach`. `ChartAnomalies` (the registry) moves from the root into `anomaly`.

### `atlas`
30 classes, but they hang together, so they're left alone in this pass. A later pass could split generation (`AtlasGenerator`, `AtlasLayout`, `AtlasPositions`, `AtlasHash`, `AtlasBiome(s)`, `AtlasTerrain`, `AtlasTerritories`, `AtlasDecoration`, `AtlasSpecialPlacer`, `AtlasAnomalies`, `AtlasAnomalyCandidate`) from state and progress, but only after `AtlasState` has its aliases. `AtlasRunNode` moves in from the root.

With this layout, the root has 6 classes and no new package has more than 18 (`content` is the largest).

## Placement rule for new classes

Add one line to the Layout section of `CLAUDE.md`: Star Charts classes go in the `exiledsector.charts` subpackage for their stage (run, run.cleanup, wormhole, generation, layout, content, objective, effect, loot, anomaly, atlas); the root holds only wiring and shared ids.

## Build order

Each step is its own commit, and each one builds and passes `mvn -q clean package` before the next starts. Move files with `git mv` so history follows them, and fix imports with IntelliJ's Move refactoring or by editing the files. Never split files on `\n`, because of the line-ending rule.

1. [x] Delete the empty `expansion` and `phenomenon` folders in `main` and `test`. Git never tracked them (no files), so the branch has nothing to delete; they exist only on disk in older checkouts and go with `git clean -d`.
2. [x] Add XStream aliases for `AtlasState` and the classes it stores, with a save-fixture test that loads a save made before the aliases. Nothing moves in this step. Built: `ChartSaveAliases` maps `exiledSector.AtlasState`; `AtlasState` stores only JDK maps, sets and numbers, so it needs no other alias (`ChartPersistenceGuardTest` checks that). `SaveFixtureTest` loads `save_fixture_atlas_before_alias.xml`, written by plain XStream from a real `AtlasState` before the alias, and checks that it resaves under the alias. A save written before the alias still names `exiledsector.charts.atlas.AtlasState`, so a later pass that moves it must also alias that old name.
3. [ ] Add a test that checks every `exiledsector.*` class named in `data/` resolves.
4. [ ] Move the leaf groups that little else depends on: `run.diagnostics`, `run.cleanup`, `objective`, `layout.sketch`, `anomaly.breach`.
5. [ ] Move `loot` and `effect`, and update the `ChartBattlePlugin` data entry.
6. [ ] Move `layout` (with `feature` becoming `layout.feature`) and `content`.
7. [ ] Move `generation`, `wormhole` and `run`, and update the `ChartWormholePlugin` and `WormholeGateway` data entries.
8. [ ] Move `ChartAnomalies` and `AtlasRunNode` into their packages, then check that the root holds only the six classes listed above.
9. [ ] Fix the class paths in `docs/plans/atlas.md` and `docs/plans/anomalies/overview.md`, add the placement line to `CLAUDE.md`, then deploy and load an existing save with a chart open and an atlas in progress (only after the user asks for a deploy).
