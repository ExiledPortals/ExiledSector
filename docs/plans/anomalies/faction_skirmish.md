# Anomaly: Faction Skirmish

Two factions fight over a system on the border between their territories on the atlas. Before the wormhole opens, the player sides with the defender, sides with the attacker, or stays out. Siding with the attacker and winning hands the system to the attacker on the atlas; every other outcome leaves it with its owner.

Status: built 2026-10-26, its Atlas Catalyst 2026-10-29, apart from the in-game checks below; move this plan to `completed/` once those are done. Redesigned 2026-10-26 around atlas territories, replacing the allied-foothold design. See `overview.md` for the shared framework.

## Where it appears

The defender is the system's owner and the attacker is a faction that owns a system one lane away. Any two different factions that meet on a border can fight; their relationship outside the atlas never matters. Factions listed in `data/config/exiledSector/chart_skirmish_factions.csv` (`faction,note`) never take a side: remnant, derelict, threat and rat_abyssals to start.

The anomaly rolls only on the atlas, through `fitsAt`: the system's own faction must be able to take a side, at least one lane neighbour must belong to another faction that can, and infested systems, special sites and the Gateway never qualify. `AtlasTraits` gets the lane neighbours' factions for this (its territory ids ignore biome factions, so a pirate pocket inside Hegemony space would read as Hegemony). There is no chart chance line and no intel, since a skirmish needs a border and an owner. A border system without the anomaly plays as a normal chart.

The attacker is picked when the run opens and when the tooltip draws, from the lane neighbours' current owners (captures included), weighted by how many lanes lead into each faction and raced on the cell's hash, so it never changes while the map stays the same. If no neighbour of another faction remains, the skirmish drops from the system. The node tooltip names both sides.

## Choosing a side

Pressing Open Wormhole on a skirmish system opens a confirmation over the atlas: side with the defender, side with the attacker, stay out, or cancel. Each option says what winning does to the map, and that completing the system ends the fight for good. The choice is stored on `AtlasRunNode`, and `ExiledSectorChartOpen` takes `side=defender|attacker|neither` for testing.

The player's real standing with either faction never matters: per-fleet memory flags make the chosen side friendly and the other side hostile for the run, and chart fleets already carry no reputation impact. Allied fleets and an allied station switch to the run-only faction `exiledSector_skirmish_ally` (friendly to the player, hidden from the intel tab, not playable in Nexerelin), which takes the real faction's name, crest and logo for the run but keeps its own green colour.

## Fleets and strength

The defender always has a station in a skirmish system, picked from `chart_boss_stations.csv`. Factions without their own rows fall back to shared rows (an empty `factions` column), which get vanilla's midline stations by tier.

Strength is the fleet points of a side's fleets, leaving out the station. When the player picks a side, the other side keeps the chart's normal enemy budget (after Fleet Size, Strength and the other chart lines) and the allied side gets that budget divided by 1.3 (placeholder), so the enemy is always at least 30% stronger and the allies can't win alone. Any later line that strengthens allies is capped so the gap never drops below 30%. When the player stays out, both sides get the normal budget, both are hostile to the player and to each other, and the player can let them wear each other down first. Skirmish fleets may go past the chart's fleet limit.

Enemy fleets push on their own: attacker fleets go for defender fleets and the station, and defender fleets meet them. Those battles are fought between NPCs in the campaign layer and leave their damage and lost CR, and the player can join any of them.

## Winning

Every skirmish run uses the boss objective.

- Siding with the attacker: the boss is the defender's station, and it counts whoever destroys it. Completing the system records it as captured by the attacker.
- Siding with the defender: the boss is the attacker's commander, who leads a final push on the station once the attacker's other fleets have fallen. If the station falls, the objective can't be completed and the run fails when it closes.
- Staying out: the boss is the defender's station, counted whoever destroys it (decided 2026-10-26). The map never changes, even if the player destroys it.

Killing the attacker's commander always drops boss loot, even after the allied station has fallen (decided 2026-10-26).

A failed run never changes ownership.

## Captures on the atlas

`AtlasState` stores one encoded string per captured system (cell and new owner), like inserted anomalies, so the generator stays a pure function of the seed. One reader returns a system's owner with captures applied; `AtlasOpening.factionFor`, the node tooltips, `AtlasTerritoryBorders` and the atlas fleet traffic all go through it, so a captured system redraws the border as a dent into the defender's territory and its scenery fleets change colour. Captures don't add new skirmishes along the moved front; the world inserting them later is a separate plan.

## Rewards

Nothing beyond the extra fleets and their loot; allied fleets drop nothing. Later, contacts in each faction will offer missions to capture a number of atlas systems for them, built on the capture records (own plan).

## Chart modifiers

None for now. The old allied reward, stock and strength lines are dropped.

## Build order

1. [x] `chart_skirmish_factions.csv`, neighbour factions on `AtlasTraits`, the `fitsAt` rule, the attacker pick, the `chart_anomalies.csv` row and a tooltip line naming both sides. Built: `charts.anomaly.skirmish` (`FactionSkirmish`, `SkirmishFactions`), `AtlasTraits.neighbourFactionIds`, `specialId` and `infestationId`, the atlas roll honouring `fitsAt` (`AtlasData.rules`), `ChartAnomaly.holdsAt` dropping anomalies in `AtlasAnomalies.effective`, `AtlasOpening.ownerOf`, `AtlasBorders.attacker`, and the `chart.atlas.node.skirmish` line. Marker icon: `graphics/icons/anomalies/faction_skirmish.png`, a circular copy of vanilla `faction_conflict`.
2. [x] Capture records in `AtlasState`, the capture-aware owner reader used by opening, tooltips, borders and fleet traffic, and a dev console command to capture and release a system. Built: `AtlasState.capturedSystems` (packed cell to captor; capturing for the original owner releases), `AtlasOpening.ownerOf` (infestation, then capture, then the generator's faction; opening, tooltips, skirmish sides) and `territoryOwnerOf` (capture, then the generator's faction, as borders and traffic ignored infestation before; `AtlasScenery.territoryAt`, `AtlasFleetTraffic.ports`), and `ExiledSectorAtlasCapture` (`AtlasCaptureCommand`).
3. [~] The side confirmation over the atlas, the choice on `AtlasRunNode`, and `side=` on the console open command. Built: `BenchModeSupport.OpenChoice` with a stacked-button confirm (`HoloPanel.openChoiceConfirm`, `SocketWorkbenchPanel.showChoiceConfirm`), the atlas choices (`AtlasNodeText.sideChoices`, `AtlasEntry`, `SkirmishSide`), the attacker and side on `AtlasRunNode` (two new encoded fields; older nodes still read) and `ChartGenContext`, `FactionSkirmish.join` in `ChartRunActions.openFromAtlas` (no choice means staying out), and `side=` on `ExiledSectorChartOpen`, which adds the anomaly with no attacker for step 4 to pick. Not built: the in-game check of the confirm dialog.
4. [x] Two-sided fleets with the strength rule, the defender's station with shared fallback rows, and fleet assignments that attack the other side. Built: `SkirmishPlanner` (attacker fleets matching the defender's ordinary fleets plus a commander, the allied side's fleets divided by `ALLY_STRENGTH_DIVISOR`, an attacker picked for console runs), a forced station boss in `ChartContentPlanner.plan`, `ChartContentPlan.PlannedSkirmish`, and in `ChartContentGenerator` the station spawned first, `joinSkirmish` flags (hostile to the other side, allies non-hostile to the player with no chart loot), attacker fleets from a staging point far from the station (`ORBIT_AGGRESSIVE` on it, the commander holding with `SKIRMISH_COMMANDER_MEMORY_KEY`), and defender patrols on `ORBIT_AGGRESSIVE` around it. Orbit assignments are the only ones the engine ends when their target dies, so both sides fall back to patrolling once the station falls. An allied boss never holds the colony item. Allies fly as the proxy faction `exiledSector_skirmish_ally` (`data/world/factions/`, with a Nexerelin config).
5. [x] Objectives per side, the attacker commander's final push, failure when the station falls on the defender's side, and the capture on completion. Built: `ChartContentGenerator.settleSkirmish` (the station, or the commander with the allied station as `ChartContent.protectedFleetId`), `ChartRun.objectiveLost` set by `ChartObjectives.fleetGone` and `reconcile` when the protected fleet goes, checked before any target so a station falling in the same battle as the boss still loses, and the run fails on close, `ChartSkirmishes.pushCommander` (from the despawn listener and the run script's objective check), `AtlasProgress.captureForWinner`, and the `chart.intel.lost` line.
6. [x] Strings (en and zh_CN), the `docs/docs.md` section and tests. Built: the run intel line `chart.anomaly.faction_skirmish.intel`, the "Faction Skirmish" section in `docs/docs.md`, and tests for the shipped faction list and the atlas text and side choices (`AtlasNodeTextTest`).
7. [x] The Assault Orders catalyst (`atlas_catalysts.md`): the anomaly made `insertable`, fitting by its usual `fitsAt` (the owner can take a side and a lane neighbour of another such faction can attack) with the attacker picked as usual; the border check's own refusal reason in place of the generic `does_not_fit` line; the `atlas_catalysts.csv` row (orders family), its `special_items.csv` row, name strings (en and zh_CN) and tests. Built: `faction_skirmish` is `insertable`; `ChartAnomaly.insertVerdict` (by default `fitsAt` plus `holdsAt`, so a catalyst is never spent on an anomaly that would not hold) decides `AtlasAnomalies.canInsert`'s `DOES_NOT_FIT`, and `FactionSkirmish` overrides it with the current owner, capture included: special site, infested, an owner that takes no side, or no neighbour to attack (`catalyst.refused.skirmish.*`). The `exiledSector_assault_orders` rows in `atlas_catalysts.csv` and `special_items.csv` (order 931), and name and refusal strings in both catalogues. Tests: `FactionSkirmishTest`, `AtlasCatalystsTest`, keys in `StarChartUiStringKeysTest`.

## Open questions

- Settled 2026-10-26: per-fleet memory flags are enough, with no proxy faction. Fleets run `ModularFleetAI`, whose `isHostileTo` is `TacticalModule.isHostileTo`; it honours `$cfai_makeHostile_<faction>` and `$cfai_makeNonHostile_<faction>` on either fleet (`Misc.makeHostileToFaction`), and for the player `$cfai_makeHostile` and `$cfai_makeNonHostile` before any reputation. `CampaignFleet.isHostileTo` asks both fleets' AI, and `BaseLocation` starts an NPC battle and `Battle.pickSide` places joiners through it, so the flags decide who fights, who joins which side, and where the player can join.
- Still to check in game: NPC-vs-NPC battles in a chart system resolve and leave damage and lost CR as they do in the sector.
- Settled 2026-10-26: siding with a faction the player is at war with. The player fleet has no AI, so `CampaignFleet.isHostileTo` from the player's side ends in a plain faction-relation check that reads no memory flags, and `Battle.pickSide` would keep such allies out of the player's battles. Allies therefore switch to the proxy faction above (`VanillaChartContentEngine.dressAllies`, `ChartContentGenerator.joinSkirmish`), and enemy fleets are flagged hostile to it as well.
