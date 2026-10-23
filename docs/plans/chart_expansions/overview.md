# Chart expansions: overview

Chart expansions are events and mechanics that a star chart can roll on top of its fleets and POIs, the way league content sits inside Path of Exile maps. Each expansion has its own plan in this folder:

| Expansion | Plan | Fights it adds |
|---|---|---|
| Relay Network | `relay_network.md` | none, changes every chart battle |
| Excavation Sites | `excavation_sites.md` | none, scales the boss |
| Faction Skirmish | `faction_skirmish.md` | none of your own; allies share the load |
| The Hunter | `the_hunter.md` | one, optional, on your terms |
| Spatial Breach | `spatial_breach.md` | none |
| Supply Convoy | `supply_convoy.md` | zero or one, sized by where you strike |
| Stasis Vaults | `stasis_vaults.md` | optional, one per vault |
| Asteroid Stream (Industrial Evolution only) | `asteroid_stream.md` | none |

Status: planned, nothing built.

## The fight budget

Combat readiness makes back-to-back fleet battles miserable, so a chart is built around roughly one big fight plus at most one optional one. An expansion adds pressure through the campaign layer (days, fuel, supplies, sensor profile, terrain, decisions), replaces a fight, or raises the stakes of a fight the chart already has. No expansion spawns waves.

## How an expansion plugs in

`ChartContentPlan` gains a list of planned expansions next to its POIs and fleets. Each chart rolls at most one expansion by default, chosen by weight and tier from a new `data/config/exiledSector/chart_expansions.csv` (`id,weight,min_tier,max_tier,requires_mod,note`). The roll is frozen at chart creation like the rest of the chart and shown in its tooltip.

Each expansion is one class behind a shared interface (`ChartExpansion` in `exiledsector-star-charts`, package `exiledsector.charts.expansion`) with three parts: plan (reads the chart's budget, decides what to place), generate (places entities and fleets in `ChartSystemFactory`), and run (its state in `ChartRunState`, its scripts removed by `ChartRunCloser` so the chart leaves no save trace). Rewards go through the existing loot framework (`ChartLoot`, `ChartRewards`) so chart loot modifiers apply to them.

Chart modifiers get two new kinds of line per expansion: `CHART_<EXPANSION>_CHANCE` ("Area contains a Relay Network") and the expansion's own tuning lines, listed in each plan. They go in `star_chart_modifiers.csv` and the `prefixes`/`suffixes` pools of `star_charts.csv` like every other chart line, and follow `docs/tooltip_wording.md`.

How players favour one expansion over the others is left to the atlas plan.

## Build order

1. [ ] Expansion framework: `ChartExpansion`, `chart_expansions.csv`, planner roll, tooltip line, run state and cleanup, with one test expansion that places nothing.
2. [ ] Expansions one at a time, each to its own plan's build order. Suggested order: Relay Network, Faction Skirmish, Excavation Sites, The Hunter, Supply Convoy, Stasis Vaults, Spatial Breach, Asteroid Stream.
