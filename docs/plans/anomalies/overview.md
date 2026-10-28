# Anomalies: overview

An anomaly is anything a star chart system holds beyond its ordinary layout, fleets and POIs: a black hole, a magnetic storm, a Spatial Breach, a supply convoy. Anomalies merged the earlier chart expansions (events and mechanics on top of the fleets and POIs, like league content in Path of Exile maps) and phenomena (gameplay-changing terrain) into one concept (decided 2026-10-26). Each anomaly with its own design has a plan in this folder:

| Anomaly | Plan | Fights it adds |
|---|---|---|
| Relay Network | `relay_network.md` | none, escalates the systems it leads to |
| Excavation Sites (built) | `../completed/anomalies/excavation_sites.md` | one optional per open shaft, frigates and destroyers only; collapsed shafts are a marine raid |
| Faction Skirmish | `faction_skirmish.md` | two sides' fleets; you join one or fight both |
| Listening Post (built) | `../completed/anomalies/listening_post.md` | zero if you get in unseen, one if the alarm goes up |
| Derelict Forge | `derelict_forge.md` | none |
| Storm Front | `storm_front.md` | none, changes the fights the chart already has |
| Spatial Breach (built) | `../completed/anomalies/spatial_breach.md` | none |
| Supply Convoy (built) | `../completed/anomalies/supply_convoy.md` | zero or one, sized by where you strike; a raid if it delivers |
| Anomalous Debris (built) | `../completed/anomalies/anomalous_debris.md` | optional, one per dormant fleet |

Black Hole, Neutron Star, Dense Nebula, Asteroid Storm, Magnetic Storm and Stellar Flares are built and described in `../atlas.md` (Regions, layouts and anomalies).

## Fights

Combat readiness makes back-to-back fleet battles miserable, so each anomaly keeps its own fights small: it adds pressure through the campaign layer (days, fuel, supplies, sensor profile, terrain, decisions), replaces a fight, or raises the stakes of a fight the chart already has. No anomaly spawns waves. A system can hold any number of anomalies, and fleets an anomaly adds may go past the chart's fleet limit, so stacking anomalies is how a system gets harder.

## How an anomaly plugs in

Each anomaly is one class behind `ChartAnomaly` (package `exiledsector.charts.anomaly`, with the `ChartAnomalies` registry; an anomaly with several classes gets a subpackage, such as `anomaly.breach` and `anomaly.skirmish`) and one row in `data/config/exiledSector/chart_anomalies.csv` (`id,weight,min_tier,max_tier,requires_mod,icon,intel,insertable,excludes,effects,note`). The interface has optional hooks for three stages, and an anomaly implements only the ones it needs:

1. Star: `primaryStar()` or `systemType()` replaces the star or the system type before layout (Black Hole, Neutron Star, Dense Nebula). These are locked to the atlas cell and can never be inserted.
2. Shape: `shape(AnomalyLayoutSite)` adds terrain right after layout (Asteroid Storm, Magnetic Storm, Stellar Flares).
3. Content: `generate(AnomalyContentSite)` places entities and fleets after the sites and fleets (Spatial Breach, the Black Hole's derelicts, the Neutron Star's research station, the Asteroid Storm's belt caches). `AnomalyContentSite.addSite` places a `chart_poi.csv` site on a clear orbit with chart loot. Run state lives in `ChartRunState`, and `ChartRunCloser` removes it with the run tag, so the chart leaves no save trace.

Atlas hooks have defaults: `atlasWeight` leans the node roll from the node's traits, and `fitsAt` says whether the anomaly can exist at a node at all (Faction Skirmish needs a border between two factions). Rewards go through the loot framework (`ChartLoot`, `ChartRewards`) so chart loot modifiers apply to them.

`effects` lists chart lines the anomaly adds to the run, such as a loot quantity bonus.

`excludes` lists anomalies that must never share a system with this one, for pairs that would interact badly. It works in both directions, and two star anomalies always exclude each other. Wherever two conflicting anomalies meet, the one already there wins: the node's own roll before inserted ones, and within one roll the lowest hash.

Every anomaly follows the Foiler mods compatibility rules in `../atlas.md`: no combat nebula tiles, custom terrain on `BaseTerrain`, textures preloaded.

Star Charts roll no anomaly lines (since 2026-10-26, `../completed/anomalies/spatial_breach_changes.md`): every anomaly comes from the atlas, rolled by a cell or inserted, so the `CHART_<ANOMALY>_CHANCE` lines in the unbuilt plans are dropped. An anomaly's own tuning lines are still chart effects, carried by an atlas source (an anomaly's `effects` column or an atlas node's effects), and follow `docs/tooltip_wording.md`.

Players favour one anomaly over the others with intel and atlas passives, and can insert anomalies into atlas systems (`../atlas.md`, Anomalies) with Atlas Catalysts (`atlas_catalysts.md`).

## Build order

The atlas comes first. These steps sit inside `../atlas.md`'s build order (its steps 2, 3c, 12, 13 and 16).

1. [~] Anomaly framework: `ChartAnomaly`, `chart_anomalies.csv`, the per-node roll, run requests, tooltip line, run state and cleanup, with one test anomaly that places nothing (`ChartAnomaliesTest.PlacesNothing`), plus inserted anomalies (`../atlas.md` step 17). Not built: icon and intel columns' use, which wait for the marker icons and the intel item.
2. [~] Anomalies one at a time, each to its own plan's build order. Built apart from in-game checks: Faction Skirmish, Relay Network, Supply Convoy, Anomalous Debris, Excavation Sites and Listening Post. Suggested order for the rest: Storm Front, Derelict Forge.
