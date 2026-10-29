# Anomalies: Remnant Nexus and Domain-era Mothership

Two biome landmarks: a Remnant Nexus in every Remnant Expanse pocket and a Domain-era survey mothership in every Derelict Graveyard pocket. Each is a station that launches and rebuilds fleets of its faction for as long as it stands, the way a vanilla Nexus does.

Status: built 2026-10-29 apart from the in-game checks (package `exiledsector.charts.anomaly.station`); move this plan to `completed/anomalies/` once those are done. Requested 2026-10-29. See `overview.md` for the shared framework.

## Placement

Neither anomaly comes from the chance roll, a catalyst or an insertion. An anomaly with a `biomes` column in `chart_anomalies.csv` sits in exactly one system of each pocket of the listed biomes. A pocket is one biome site: special biomes never sit beside another site of their own type, so each site is its own pocket. The system is the one among the site's cells with the lowest hash of the atlas seed, the anomaly id and the cell, skipping void cells, the Gateway, special sites and infested systems (an infestation would hand the system to another faction). The same atlas always picks the same system.

The anomaly comes first in the node's list, ahead of rolled anomalies, so it wins any conflict and always fits under `MAX_PER_SYSTEM`. Like star anomalies, a failed run doesn't spend it. `insertable` is forced off, so `AtlasAnomalies.canInsert` refuses it (`NOT_INSERTABLE`), `AtlasCatalysts` skips any catalyst row naming it, and a Relay Network chain never moves it (only inserted records move).

## Bosses

The Remnant and derelict boss station rows are gone from `chart_boss_stations.csv`, and the station row of `chart_bosses.csv` lists them in its new `excluded_factions` column, so they never fall back to the shared midline stations: their boss is always a fleet.

## The stations

The station is a station-mode fleet of the anomaly's faction built by the chart engine (`ChartAnomalyFleets.station`), on a clear orbit 3000 to 11000 su out, preferring one 4000 su or more from the inner wormhole. It carries the run's tier, so its tree is chart-level, and it drops one chart fleet's items and vanilla loot. A Nexus is the damaged variant up to tier 7 and the standard one from tier 8; the mothership is always `station_derelict_survey_mothership_Standard`.

Decided: the station is a side objective with its own loot, and the fleets it launches are a hazard that drops no chart items, so an endless supply of fleets can't be farmed. Destroying the station ends the launches. It is not the chart objective.

## Fleets

Vanilla's `RemnantStationFleetManager` (a `SourceBasedFleetManager`) launches Remnant fleets from a Nexus, raises their size with each fleet lost and rebuilds them after a delay; it works inside a chart system, so `NexusFleetManager` extends it. It skips vanilla's global encounter-point listener, which would put Remnant encounters into the main sector's hyperspace, and builds each fleet with chart tier scaling: 35% of the tier's fleet size, 5% more per fleet lost, at most 60% (placeholders), patrolling under vanilla's `RemnantAssignmentAI`. A damaged Nexus keeps two fleets out and rebuilds after 25 days, a full one three after 15.

Vanilla has no mechanism that spawns drone fleets from a mothership: its derelict mothership is a salvage entity whose defenders (`SalvageGenFromSeed`) fight once and never return. `MothershipFleetManager` mirrors the Nexus on `SourceBasedFleetManager` with derelict drones up to cruisers, built like the Derelict Forge's guards (two fleets, 20 days, placeholders).

## Cleanup

The managers are system scripts that implement `ChartRunBound`. `ChartRunCloser` despawns every fleet in the system, then removes the system's run-bound scripts before it removes the entities and the system. A manager spawns nothing once its run isn't live or its station is dead. Battles here are ordinary fleet battles, not salvage defenders, so vanilla's derelict tally never moves.

## Build order

1. [x] The `biomes` column and per-biome placement. Built: `ChartAnomalies.Definition.biomes` (`perBiome`, `keptWhenSpent`), `AtlasAnomalyCandidate.biomes`, `AtlasBiomeAnomalies` and `AtlasGenerator.biomeAnomalyCell`; per-biome candidates are skipped by the chance roll. Tests: `AtlasBiomeAnomaliesTest`.
2. [x] Boss rows removed and `excluded_factions` on `chart_bosses.csv` (`ChartBosses.Definition.excludes`). Tests: `ChartBossesTest`.
3. [x] The anomalies and their managers: `LaunchingStation`, `RemnantNexusStation`, `DomainMothership`, `NexusFleetManager`, `MothershipFleetManager`, `StationFleets`, `StationRules`; system-script cleanup in `ChartRunCloser`. Tests: `LaunchingStationTest`, `ChartRunCloserTest`.
4. [x] Strings (en and zh_CN draft), `docs/docs.md` section, placeholder atlas icons: `graphics/icons/anomalies/remnant_nexus_station.png` (vanilla's Remnant crest scaled to 40 px) and `graphics/icons/anomalies/domain_mothership.png` (a copy of vanilla's `log_to_domain_derelict` intel icon).
5. [ ] In-game checks: the Nexus and mothership launch, fleets patrol and return, the station loot drops, and nothing is left after the run closes.

## Open questions

- Whether a Nexus that keeps launching breaks the overview's "no anomaly spawns waves" rule too much at high tiers (fleet counts are low placeholders for now).
- Final icons to replace the two placeholders.
