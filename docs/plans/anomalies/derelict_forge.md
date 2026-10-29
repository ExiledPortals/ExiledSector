# Anomaly: Derelict Forge

A Domain nanoforge the lockdown missed is still running on a derelict station in the system. It can do precise work the crew's nanoswarms can't: add a modifier of a chosen kind, remove a chosen modifier, perfect one. Each forge offers a few programs, and running one costs metals, rare metals and Nanite Feedstock, which the system itself helps you gather. Like a vanilla cryosleeper, it is guarded by Domain-era automated defenses led by a Guardian, which the player has to beat first.

Status: built 2026-10-29 apart from the in-game checks (package `exiledsector.charts.anomaly.forge`, programs in core `ItemForging`); move this plan to `completed/anomalies/` once those are done. Proposed 2026-10-28. See `overview.md` for the shared framework.

## Lore

The crews reprogram nanoswarms because the Domain's licensed nanoforges are locked down. This one never got the lockdown signal. It still takes raw stock and runs its programs, and a crew that feeds it can direct a single, exact change to a hull item.

## How it plays

The chart places the forge station on a clear orbit (`AnomalyContentSite.clearOrbit`). Until its guards are beaten, interacting with it starts vanilla's automated defenses encounter; once they are beaten, interacting with it opens a dialog of ours listing the forge's programs, each with its cost. Choosing one opens a Forge mode on the Modification Bench, drawn over its own forge chamber art, to pick a hull item, and the modifier or ship part where the program needs one. Subroutines, Star Charts and corrupted hull items can't be forged (decided 2026-10-29). The forge runs one program per run (placeholder), after which it goes dark.

While the forge is in the system, the chart's sites lean toward resources (`DropType.RESOURCE`, placeholder +100%), so a player who sweeps the system can usually fuel it from what they find.

## Guards

The guards work like a cryosleeper's: vanilla puts one alpha-core Guardian (`guardian_Standard`, the derelict faction's only capital) in the cryosleeper's `$defenderFleet` (`SalvageGenFromSeed`, derelict_cryosleeper row) and runs the fight through `SalvageDefenderInteraction`, which sets `$defenderFleetDefeated` on a win. The forge keeps exactly one such Guardian and adds derelict drones (placeholder: half the tier's fleet size, cruisers at most, tier quality and S-mods), and the fleet carries the chart tier so its ships get chart-level skill trees. Retreats, damaged guards persisting for 10 days, AI core drops and the battle rules are vanilla's; chart fleet loot comes through the usual defended-site adoption.

## Programs

Each forge rolls three programs (placeholder) from `data/config/exiledSector/chart_forge_programs.csv` (`id,weight,min_tier,item_types,metals,rare_metals,feedstock,note`) on the chart seed. Starting rows, all placeholders:

| Program | What it does |
|---|---|
| Graft | Adds a modifier whose effect belongs to a chosen ship part (the subpackage of `exiledsector.skills.skilleffect` the line's effect lives in: shield, flux, weapon and so on) to an item with room for one. |
| Excise | Removes a chosen modifier. |
| Perfect | Sets a chosen modifier's values to the best end of its range for the player: the highest value, or the least harmful for a negative effect, so a "-5% to -10% damage dealt" line perfects to -5%. |
| Anchor reroll | Rerolls every other modifier into new ones, keeping the chosen one and the modifier count. |
| Retemper | Rerolls a hull item's implicit values, keeping the better roll of the old and the new for each line. |

Each program costs metals, rare metals and 20 to 50 Nanite Feedstock, all scaling with tier (placeholders). Graft, Excise and Anchor reroll refuse unique items; Perfect and Retemper accept them. Items the currency can't touch are refused in the same way (`ItemCrafting.isLockedFromCurrency`, corrupted and mirrored items), and an immutable modifier can't be excised, rerolled or replaced. Prefix and Suffix Inhibitors don't apply, because every program picks exactly what it touches.

## Rewards

The program is the reward. The forge drops no loot of its own; its guards drop what any defended chart site's guards drop.

## Chart modifiers

Tuning lines are chart effects carried by an atlas source, per `overview.md`:

- `CHART_FORGE_PROGRAMS`: +N programs offered by the Derelict Forge.
- `CHART_FORGE_RUNS`: +N programs the Derelict Forge can run.
- `CHART_FORGE_COST_PERCENT`: reduced Derelict Forge program costs.

## Build order

1. [x] The anomaly class (`exiledsector.charts.anomaly.forge`) and its insertable `chart_anomalies.csv` row; the forge station on a clear orbit, its rolled programs and runs left in `ChartRunState`; the resource leaning on the system's sites. Built: `DerelictForge` (a neutral station entity, now `exiledSector_chart_derelict_forge_station`, tagged `FORGE_TAG` on the first clear orbit in 2000 su bands from 2500 su out), `ForgeStates` (offered programs and runs left in the run's anomaly state), `ForgeRules`; the leaning is `CHART_RESOURCE_PERCENT:100` in the row's `effects`. Tests: `DerelictForgeTest`.
2. [x] `chart_forge_programs.csv` and its loader; each program as an operation on `Item` next to `ItemCrafting`, sharing its refusal rules, with tests per program. Built: `ForgePrograms` (loaded after `AtlasCatalysts`), and in core `ForgeOperation`, `ForgeChoice` and `ItemForging`, sharing the mirrored, corrupted and locked rules of `ItemCrafting`; Graft reads each line's part from its effect's `skilleffect` subpackage (`ItemForging.partOf`), so a hybrid counts for every part it touches; Perfect and Retemper use `ModifierLine.bestRoll` and `isBetter` with the effect's `invertsDescriptionColours`. Tests: `ItemForgingTest` (hull-upgrades), `ForgeProgramsTest`.
3. [x] The forge dialog through `ChartCampaignPlugin.pickInteractionDialogPlugin`, and the Forge bench mode registered through `StarChartsDomain.benchModes` (item picker, modifier picker, cost and a confirmation). Built: `ForgeDialog` (programs, costs and stock; opens the bench with `ModificationBenchCargoLauncher.openMode` and reports the result), `ForgeBench` (`BenchMode.FORGE`, art `graphics/ui/exiledSector/derelict_forge.png`, hull item storage as the picker, no nanoswarm or Inhibitor tiles, a Run Program button whose confirmation names the program, a prompt and the cost and offers one button per modifier, "Keep ..." buttons for Anchor reroll, or per ship part), `ChartForge` (verdict and run: the cost and the run are taken only after the program changed the item). Core bench hooks: `BenchModeSupport.accepts`, `usesInhibitors`, `confirmLines`, `openChoices`, `playOpened`. Tests: `ChartForgeTest` (mod).
4. [x] Chart modifier lines, strings (en and zh_CN), the atlas marker icon, the `docs/docs.md` section and tests. Built: `ChartEffect.CHART_FORGE_PROGRAMS`, `CHART_FORGE_RUNS` and `CHART_FORGE_COST_PERCENT` (reads "reduced" when negative), carried by no source yet; `chart.forge.*` strings and `ui.forge.refused.*` in core (zh draft); docs section "Derelict Forge"; atlas icon `graphics/icons/anomalies/derelict_forge.png`, a circular crop of vanilla's pristine nanoforge item icon.
5. [x] The Forge Survey catalyst (`atlas_catalysts.md`): the `atlas_catalysts.csv` row (survey family), its `special_items.csv` row and name strings (en and zh_CN); it loads once the anomaly has its icon. Built: row `exiledSector_forge_survey` (survey family), special item row (order 935), names in both catalogues. Tests: `AtlasCatalystsTest`.

6. [x] Guards: a derelict defender fleet with one Guardian, fought through vanilla's defender interaction before the forge opens. Built: `ForgeGuards` (seed and `$hasDefenders` set at placement, the fleet built on first visit into `$defenderFleet` and re-marked with the chart tier on each visit, and the vanilla derelict tally restored after a win), `ForgeDialog.engageGuards`, engine `createEmptyFleet` and `addCoreFlagship`, custom entity `exiledSector_chart_derelict_forge_station` (a copy of `station_mining` named "derelict forge" for vanilla's after-battle line). Tests: `ForgeGuardsTest`, `DerelictForgeTest`.

## Open questions

- Whether the guards are too much at tier 1, where the Guardian alone outweighs the drones (left to balance; the Guardian is fixed and only the drones scale).
- Whether programs that pick a modifier are too strong against Reshaping and Tuning Nanoswarms, and need a higher cost or fewer runs at low tiers (left to balance for now).

## Resolved questions

- Graft finds a modifier's ship part automatically from its effect's package in `exiledsector.skills.skilleffect`, with no csv column; a hybrid counts under every part any of its lines belongs to.
- The forge takes uncorrupted hull items only: no subroutines and no Star Charts.
- Every program also costs Nanite Feedstock, 20 to 50 per run before tier scaling.
- Anchor reroll rerolls the other modifiers themselves, not just their values.
- Graft, Excise and Anchor reroll refuse unique hull items; Perfect and Retemper accept them (decided 2026-10-29).
