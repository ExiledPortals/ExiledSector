# Anomalies: overview

An anomaly is anything a star chart system holds beyond its ordinary layout, fleets and POIs: a black hole, a magnetic storm, a Spatial Breach, a supply convoy. Anomalies merged the earlier chart expansions (events and mechanics on top of the fleets and POIs, like league content in Path of Exile maps) and phenomena (gameplay-changing terrain) into one concept (decided 2026-10-26). Each anomaly with its own design has a plan in this folder:

| Anomaly | Plan | Fights it adds |
|---|---|---|
| Relay Network | `relay_network.md` | none, changes every chart battle |
| Excavation Sites | `excavation_sites.md` | none, scales the boss |
| Faction Skirmish | `faction_skirmish.md` | none of your own; allies share the load |
| The Hunter | `the_hunter.md` | one, optional, on your terms |
| Spatial Breach (built) | `../completed/anomalies/spatial_breach.md` | none |
| Supply Convoy | `supply_convoy.md` | zero or one, sized by where you strike |
| Stasis Vaults | `stasis_vaults.md` | optional, one per vault |
| Asteroid Stream (Industrial Evolution only) | `asteroid_stream.md` | none |

Black Hole, Neutron Star, Dense Nebula, Asteroid Storm, Magnetic Storm and Stellar Flares are built and described in `../atlas.md` (Regions, layouts and anomalies).

## Fights

Combat readiness makes back-to-back fleet battles miserable, so each anomaly keeps its own fights small: it adds pressure through the campaign layer (days, fuel, supplies, sensor profile, terrain, decisions), replaces a fight, or raises the stakes of a fight the chart already has. No anomaly spawns waves. A system can hold any number of anomalies, and fleets an anomaly adds may go past the chart's fleet limit, so stacking anomalies is how a system gets harder.

## How an anomaly plugs in

Each anomaly is one class behind `ChartAnomaly` (package `exiledsector.charts.anomaly`) and one row in `data/config/exiledSector/chart_anomalies.csv` (`id,weight,min_tier,max_tier,requires_mod,icon,intel,insertable,excludes,note`). The interface has optional hooks for three stages, and an anomaly implements only the ones it needs:

1. Star: `primaryStar()` or `systemType()` replaces the star or the system type before layout (Black Hole, Neutron Star, Dense Nebula). These are locked to the atlas cell, never come from chart lines and can never be inserted.
2. Shape: `shape(AnomalyLayoutSite)` adds terrain right after layout (Asteroid Storm, Magnetic Storm, Stellar Flares).
3. Content: `generate(AnomalyContentSite)` places entities and fleets after the sites and fleets (Spatial Breach). Run state lives in `ChartRunState`, and `ChartRunCloser` removes it with the run tag, so the chart leaves no save trace.

Atlas hooks have defaults: `atlasWeight` leans the node roll from the node's traits (Faction Skirmish prefers hostile borders), and `fitsAt` says whether the anomaly can exist at a node at all (Asteroid Stream needs Industrial Evolution). Rewards go through the loot framework (`ChartLoot`, `ChartRewards`) so chart loot modifiers apply to them.

`excludes` lists anomalies that must never share a system with this one, for pairs that would interact badly. It works in both directions, and two star anomalies always exclude each other. Wherever two conflicting anomalies meet, the one already there wins: the node's own roll before inserted ones, the atlas before chart lines, and within one roll the lowest hash.

Every anomaly follows the Foiler mods compatibility rules in `../atlas.md`: no combat nebula tiles, custom terrain on `BaseTerrain`, textures preloaded.

Chart modifiers get two kinds of line per anomaly: `CHART_<ANOMALY>_CHANCE` ("Area contains a Relay Network") and the anomaly's own tuning lines, listed in each plan. They go in `star_chart_modifiers.csv` and the `prefixes`/`suffixes` pools of `star_charts.csv` like every other chart line, and follow `docs/tooltip_wording.md`.

Players favour one anomaly over the others with intel and atlas passives, and can insert anomalies into atlas systems (`../atlas.md`, Anomalies).

## Build order

The atlas comes first. These steps sit inside `../atlas.md`'s build order (its steps 2, 3c, 12, 13 and 16).

1. [~] Anomaly framework: `ChartAnomaly`, `chart_anomalies.csv`, the per-node roll, run requests, tooltip line, run state and cleanup, with one test anomaly that places nothing (`ChartAnomaliesTest.PlacesNothing`), plus inserted anomalies (`../atlas.md` step 17). Not built: icon and intel columns' use, which wait for the marker icons and the intel item.
2. [ ] Anomalies one at a time, each to its own plan's build order. Suggested order: Relay Network, Faction Skirmish, Excavation Sites, The Hunter, Supply Convoy, Stasis Vaults, Asteroid Stream.
