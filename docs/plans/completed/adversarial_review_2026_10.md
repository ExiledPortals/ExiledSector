# Adversarial review plan (2026-10-31, `parallel-work` @ 04cb4f60)

Ten read-only reviewers went over the whole codebase. Three covered anomalies: one on the framework and cross-anomaly abstraction, and two hunting bugs in different anomaly groups. The other seven covered the chart run and loot pipeline, atlas/layout/content, the non-chart star-charts code, core skill effects, core items/UI/infrastructure, the skill-tree module, and hull upgrades plus duplication between modules. Each reviewer tried to refute its own bugs before reporting them. Where reviewers found the same thing, the findings are merged and cross-referenced by id.

How to use this file: delete the items you don't want, then hand the rest out. Every item has an id, an effort (S/M/L) and, for bugs, a severity and confidence. Line numbers are as of commit 04cb4f60. Nothing has been built, tested or changed.

## Decisions (2026-10-31)

- Readability for humans comes first. Domain knowledge moves out of core (6.A2, 6.O1, 8.H2) as far as needed to keep the code clean and easy to follow.
- Save compatibility doesn't matter until 1.0. The save-sensitive refactors (1.1.A7, 1.1.A8, 1.4.A1, 1.4.A2) go ahead.
- 5.A5 (backing factories for all 14 skill-effect enums): do it now.
- 1.1.A11 (split `ChartAnomaly` into role interfaces): do it.
- 5.B5: on-hit effects get hostility checks.
- 1.2.B4: kept guards recover between fights, as vanilla salvage defenders do.
- 2.B3: the completion reward uses all of the chart's lines, added together the same way as everywhere else.
- 1.3.B3: Stellar Flares must never be placed in a system without a star.
- 7.O2: drop the null-member checks.
- Delete all dead code and the tests that only cover it (4.O1, 8.O1, 1.2.O1, 7.O1).
- Dropped as intended or not needed: 5.O3 (Terrifying Presence stacking), 8.B4 (the XP-difficulty scale is a general setting), 1.3.B5 (hypershunt stale text).
- 2.A4: both boss guarantees are kept if either side has one.
- 3.A1: when vanilla loot fails, the merged path deletes the site and logs the error.
- Seeds: chart layout and loot refactors may change the random draw order; update the affected tests. Atlas generation stays bit-identical.

Notes that apply to every item:
- Save safety. Not a constraint until 1.0 (see Decisions). Persisted classes still get an alias in `ChartSaveAliases` when they move (2.B1), so that later moves stay cheap.
- Seeds. Several refactors touch seeded rolls. Atlas generation (3.A5, 3.A8) must stay bit-identical, because the atlas is rebuilt from its seed on load under saved progress. Chart layout and loot changes only affect newly opened runs. Changing their draw order is accepted (update the tests that expect exact results).
- One build at a time. Pass this on to any session that picks up an item.

## Fix first (highest-value bugs)

| Id | Bug | Severity |
|---|---|---|
| 2.B1 | Four persisted chart classes (`ListeningPostPlugin`, `PostScript`, `MothershipFleetManager`, `NexusFleetManager`) have no save alias, and the guard test can't catch it | med, confirmed |
| 1.3.B1 | Re-engaged hypershunt guards drop chart loot and vanilla drops twice (drift from the forge copy) | med, likely |
| 1.1.B1 | A convoy cache on the Pre-Collapse Facility planet locks the facility for the run | med, confirmed |
| 7.B2 | Save repair leaves stranded nodes allocated and always forgets sockets replaced by optional nodes | med, likely |
| 8.B1 | The ship summary under-reports per-S-mod affinity | med, confirmed |
| 6.B1 | `NodeRingArt.drawPortalRings` leaves texturing off and blending additive | low-med, confirmed |
| 8.B2 / 8.B3 | Hull-item restock deletes items the player sold; a switched-off domain's stock stays forever | low-med |
| 5.B2 | `HitDamageRestorer` may compound beam damage reductions when a beam changes target | med if real, possible |
| 2.B2 | Leaving a chart without a sensor line keeps the breach and relay buffs on the fleet | low, confirmed |
| 1.3.B2 | Storm Front's sensor penalty leaves the chart with the fleet (save trace) | low, confirmed |

## Suggested order

1. The small confirmed bugs that need no refactor: 2.B1, 8.B1, 6.B1, 2.B2, 1.3.B2, 1.2.B2, 6.B2, 6.B3, 5.B1, 5.B4.
2. The anomaly framework, in this order: 2.A1 then 1.1.A1 (live-run lookups), 1.1.A3 (dialog base, fixes 1.1.B1), 1.1.A4 (guards, fixes 1.3.B1), 1.1.A2 (content site), then 1.1.A5, A6 and A9. Leave the save-sensitive A7/A8 and the seam work in 1.4 until last.
3. Core shared helpers (section 9), each in one pass across modules.
4. Everything else by area.

## 1. Anomalies (priority area)

### 1.1 Framework abstractions (ordered by value)

Paths are under `exiledsector-star-charts/src/main/java/exiledsector/charts/`. Three reviewers found these independently, and where their findings overlapped they are merged here.

- [x] **1.1.A1 `AnomalyRun`: the framework decides whether an anomaly is live here** (M). Anomalies keep asking for themselves whether the current run is live and theirs:
  - `inRunSystem(run, location)` is copied in `ExcavationSites:93`, `PreCollapseFacility:83`, `DerelictForge:79` and `ListeningPost:111`.
  - About ten "current run, live, my run id, maybe my anomaly id" variants: `ChartForge:37,42`, `CoronalHypershunt:102`, `ChartRelays:39,49`, `StationFleets:35`, `StormBattles:24,29`, `PostScript:94`, `PostDialog:55`, `ConvoyScript:139,342`, `BreachTerrainPlugin:318`, `DormantFleetDialog:116`, `ChartSkirmishes:23`, `RelayStates:54`.
  - The "no run to record" precondition at the top of `generate` is copied four times (Excavation, Facility, Forge, ListeningPost).
  - Dispatchers loop over every registered anomaly whether or not the run placed it: `ChartRunScript:77`, `ChartContentListener:25`, `ChartRunEffects:70`, `ChartFleetLoot:181-213`, `StarChartLootSource:172`, `ChartObjectiveIntel:133-145`, `ChartCampaignPlugin:31,37`.

  Add `record AnomalyRun(run, system, context)` with `liveOrNull()`, `holds(entity)` and `has(anomaly)`, plus `ChartAnomalies.inRun(anomalyRun)` and `AnomalyContentSite.run()`. Run-scoped hooks take an `AnomalyRun`: `checkRun`, `fleetGone`, `dialogFor`, `holdsBackBossUnique`, `fleetItems`, `extraFleetLoot` and `intelText` (which absorbs `isActiveIn`). `clearFrom`, `battleFor` and `siteChances` stay global. Removes about 150 lines and makes hooks safe by default, which fixes 1.1.B4 and similar mistakes. Two dialogs, the Post script and `StormBattles` use no `isLive` check at all today, and the hypershunt dialog's check causes 1.3.B5. Keep the registry order, because `dialogFor` priority depends on it.
- [x] **1.1.A2 `AnomalyContentSite` owns everything `generate` creates** (M).
  - Terrain is added by hand: `SpatialBreach` (65-76) has its own try/catch rollback copied from `AnomalyContentSite` (116-122), and `AnomalousDebris` (69-77) and `StormFront` (49-56) track and tag by hand.
  - Scripts are added untracked, so they are not rolled back: `SupplyConvoy:83`, `ListeningPost:77`, `LaunchingStation:65`.
  - Run-state writes have no rollback: Excavation, Facility, Forge, ListeningPost (see 1.1.B5).
  - Planet picking: "solid else any" is copied in `ExcavationSites` (78-91) and `PreCollapseFacility` (64-77). The two classes import each other's state classes to avoid each other's planets. The convoy destinations (`SupplyConvoy` 53-58) and `StormFront.marks` (59-72) repeat the non-star filter without any claim check (the root of 1.1.B1).
  - The open-spot search is the same algorithm (24 attempts, half near POIs, then a 2000-8000 su ring, same constants) in `SpatialBreach` (79-106) and `AnomalousDebris` (80-120).
  - Banded orbit search ("prefer far from the entry, else the farthest") is written in `DerelictForge` (52-61), `LaunchingStation` (69-90) and `ListeningPost` (81-101). `RelayNetwork` (97-122) re-implements `AnomalyContentSite.clearOrbit` with an extra predicate and copies its constants. `StarAnomalies.placeResearchStation` and `CoronalHypershunt.place` step a single radius.
  - The "centre or star" lookup is repeated in about eight places.
  - The per-anomaly random derivation in `ChartContentGenerator` (788-790) duplicates `BuiltInTable.rollFor`.

  Add to the site: `addTerrain(id, params, at, tags...)`, `addScript(script)`, `recordState(key, value)` (staged and committed only after `generate` returns), `centre()`, `freePlanets(solidFirst)` and `claim(planet)`, `findSpot(...)`, and `clearOrbit(OrbitSearch, random)`. Removes about 200 lines and the Excavation/Facility cross-imports, and makes rollback complete. Seeded placements shift if the random draw order changes, so keep the order or accept that once.
- [x] **1.1.A3 `AnomalyDialog` base class** (S).
  - Nine anomaly dialogs repeat `memoryMap` and five empty overrides: ConvoyCache, ConvoyEncounter, DormantFleet, ExcavationSite, Facility, Forge, Post, Hypershunt, Relay. `GatewayUpgradeDialog`, `ChartWormholeDialog`, `AtlasAbilityDialog` and skill-tree `NpcFleetInspectPlugin` do the same.
  - All nine add the Leave option and its Escape shortcut by hand.
  - The entity image visual is set by hand in Forge, Post, Hypershunt and Relay.
  - Hand-off to the next claimant is written in Excavation, Facility and ConvoyCache, where it is broken (1.1.B1).
  - "Return here after the encounter" is written in Excavation, Facility and Post.

  Add a core `exiledsector.ui.AnomalyDialog` (core, so skill-tree can use it) with the boilerplate, `addLeaveOption()`, `showEntityVisual()`, `handOff(next)`, `continueAfter(self, visited)` and `returnHere(...)`. Continue must chain to the next claimant after this anomaly in registry order, with a visited set, so Excavation and Convoy can't ping-pong. Removes about 300 lines in total.
- [x] **1.1.A4 Shared encounter config and guard-fleet holders** (M).
  - FIDConfig builders: `FacilityDialog` (56-84) and `ShaftEncounters` (20-40) share 14 fields. `DormantEncounters` (13-33) and `PostDialog` (140-159) are subsets.
  - Engage-and-swap is written in Excavation, Facility, Post and DormantFleet. The "won outright" check is in Excavation and Facility.
  - Planet-memory guard holders: `FacilityGuards` (27-44) and `ShaftFleets` (32-48).
  - Vanilla `$defenderFleet` guards: `ForgeGuards` (32-74) and `HypershuntGuards` (50-92) have identical `arm`/`guarded`/`muster`/`seedOf` and constants. Their dialogs repeat `engageGuards`/`fillMemoryMap`/`BEAT_DEFENDERS_CONTINUE`.

  Add `AnomalyEncounters.isolated(leaveKey, onLeave, battle, straightToEngage)` (no AI retreat, no pull-ins, no rep impact), plus `engage` and `wonOutright`. Add `HeldGuards.held/forget` (always sets the containing location), a `VanillaDefenders` helper, and a `GuardedSiteDialog` for Forge and Hypershunt. Removes about 250 lines. The two guard classes have already drifted: Forge re-prepares with `lootable=false` on every muster, Hypershunt prepares once with `lootable=true`. That drift is the hypershunt rematch bug (1.3.B1). Keep the planet-memory keys for mid-run saves.
- [x] **1.1.A5 `ChartRaidObjective` base and a shared raid scale** (S). `ConvoyCacheObjective` and `ExcavationSiteObjective` share nine identical overrides and `valueOf`. `ConvoyRules.raidDifficulty/cacheDanger` (66-76) equal `ExcavationRules.raidDifficulty/siteDanger` (65-75), with the same 60/30/5/10 constants. Both `ChartRaid.Host` implementations repeat `raidLeft`/`raidFinished`, and both dialogs repeat the raid option dispatch and seeding. Add an abstract objective with `take(random)` and `describe(tooltip)`, `ChartRaid.difficulty(tier)`/`danger(tier)`, one `Host.raidClosed(dialog)`, and a `ChartRaid.Session` holding the raid and its launch counter. Removes about 140 lines. Not persisted.
- [x] **1.1.A6 Typed anomaly run state** (S/M). `ExcavationStates` with `ExcavationSite.encode/decode`, `FacilityStates`, `ForgeStates`, `RelayStates` (19-43) and `PostState.encode/decode` each hand-parse ints, enums, base-36 longs and floats with their own try/catch. `ChartRun` (318-349) re-parses the whole state string on every read, and `ExcavationStates.all` reads twice per index. Add `AnomalyState.of(run, anomalyId)` with typed getters and `put`/`remove`, a `StateFields.join/split`, and a transient parsed-map cache on `ChartRun`. Keep key names and encodings exactly. The persisted `ChartRun.anomalyStates` field is unchanged. Removes about 120 lines.
- [x] **1.1.A7 `AnomalyRunScript` base for run-bound system scripts** (M, save-sensitive).
  - `heist/PostScript` (26-124) and `convoy/ConvoyScript` (37-160) share the run id, tier and system fields, `done`, the day counter, the transient failure flag, a static `in(location)`, the `ChartRunBound` methods, "done when the run is gone", interval ticking and log-once failure handling.
  - "Message only if the player is in the system" and fleet-by-id lookups repeat in both.
  - `NexusFleetManager` and `MothershipFleetManager` (14-47 each) repeat their spawn logic. Their vanilla superclasses differ, so they need a composed `StationLaunches` rather than a base class.

  Both scripts are persisted (`ConvoyScript` is aliased in `ChartSaveAliases`). Moving fields into a superclass should still load, because XStream matches fields by name up the hierarchy, but test it with `SaveFixtureTest` on convoy and post saves. Keep `ConvoyScript`'s `daysSinceCheck = Float.MAX_VALUE` start. Removes about 120 lines.
- [x] **1.1.A8 `ChartAnomalyTerrain` base and sprite-sheet helpers** (M, save-sensitive).
  - `BreachTerrainPlugin`, `AnomalousDebrisTerrainPlugin` and `StormFrontTerrainPlugin` share `init`, `getEffectCategory`, `shouldCheckFleetsToApplyEffect`, the `advance` guard and all the tooltip boilerplate.
  - The preload loops in `StormFrontTerrainPlugin` (96-104) and `AnomalousDebrisTerrainPlugin` (80-88) are identical, and core `SpriteCache.ensureLoaded` already does this.
  - Cloud sheet slicing is copy-pasted between `StormFrontTerrainPlugin` (220-246) and `StormOverlay` (139-168); see also 4.A4.
  - Terrain finders are written four times: `breachIn`, `fieldsIn`, `frontsIn`, `slowingTerrain`.

  Add an abstract base with `nameKey()`, `tooltipLines()` and `advanceLive()`, plus `AnomalyTerrains.in(location, Class)`. All three plugins are persisted and aliased, so this carries the same XStream caveat as 1.1.A7. Removes about 150 lines.
- [x] **1.1.A9 (partly done)** **Data-driven anomaly loot spec** (M). `ExcavationLoot`, `heist/VaultLoot`, `FacilityLoot.roll` and `ConvoyCargo.roll` share the context, quantity multiplier and roll shape. `effectFor(DropType)` is identical in two of them. `ExcavationRules`, `HeistRules` and `FacilityRules` repeat the loot-step constants. Add `record AnomalyLoot(scope, siteId, baseSteps, stepsPerTier, itemMult, quantityKey, leans, leanPercent)` with `roll`, `items` and `addVanillaDrops`, plus `ChartEffect.forDropType`. Optionally make the numbers columns in the anomaly's `chart_loot.csv` scope. Removes about 100 lines.
- [x] **1.1.A10 Atlas and run-node hooks** (M/L). See 1.4.A2. The framework reviewer adds more sites that special-case skirmish and relay by name: `ui/atlas/AtlasNodeText` (99-106, 141), `console/ChartOpenCommand` (251-256), and `AtlasRunNode.guaranteedBossUnique`. It also proposes a `default List<AtlasLine> atlasLines(...)` hook and a per-anomaly data map on `AtlasRunNode` in place of the skirmish fields (string-encoded inside the persisted `ChartRun`, so it needs a decode fallback). Keep the join order: skirmish before relay.
- [x] **1.1.A11 Split the fat `ChartAnomaly` interface into role interfaces** (M). It has 27 defaults across six concerns: atlas, star/layout, content, run, loot and load. Every caller iterates all 20 anomalies for every hook. Split it into `StarRule`, `LayoutShaper`, `ContentPlacer`, `RunParticipant`, `LootShaper` and `AtlasRule`, with `ChartAnomalies.with(Class<R>)` returning cached typed lists. This pairs with 1.1.A1. Decided: do it.
- [x] **1.1.A12 (partly done)** **Small bypassed or missing helpers** (S each).
  - `ChartEffect` (93-134): 23 anomaly tuning constants use the same lambda, and four copy-pasted `description` overrides. An `anomalyLine(tuningKey, integer, wording)` constructor removes about 30 lines and 8 anomaly imports.
  - Revealing a fleet to the player (the ×1000 detected-range multiplier plus extended range) is in three places with duplicated constants: `ChartContentGenerator:276-281`, `ChartRelays:183-186` and `Pickets:69-71`. Add `ChartFleetVisibility.reveal(fleet, modId)`.
  - 18 raw `getPlayerFleet()` lookups in `anomaly/` bypass core `PlayerFleet.orNull`. Add `PlayerFleet.isIn(location)` for the 8 "player in this system" checks.
  - `ForgePrograms.register` re-implements `BuiltInTable.rows`; it needs parser-returns-null-to-skip support.
  - `AtlasAnomalyCandidate` copies the fields of `Definition` (`AtlasData` 87-92).
  - `ChartAnomalies.Definition.replacesStar` duplicates `ChartAnomaly.replacesStar()`. The CSV `intel` column is still unused.
  - `ConvoyTerrain` imports `StormFrontTerrainPlugin` to know that storms slow convoys. Use a `SlowsFleets` marker interface instead.
  - `AnomalousDebris` (75-76) and `StormFront` (54-55) add tags that `ChartSystemFactory.tagEveryEntity` already adds to every entity.
  - `AnomalyLayoutSite` shares the single layout `Random` across anomalies, so adding or removing an anomaly reshuffles later layout steps such as the abyss dressing. Give it `random(salt)` like the content site.
- [x] **1.1.A13 Breach discs through `HoloShapes.disc`** (S). `BreachCorePlugin.drawHole` (79-92) and `BreachFieldRenderer.drawVoid` (47-60) each hand-write a black triangle fan. Add a segment-count overload to `HoloShapes.disc` and call it from both.

### Framework bugs

- [x] **1.1.B1 A convoy cache locks the Pre-Collapse Facility on the same planet** (med, confirmed by two reviewers). A convoy destination can be any non-star planet, including the facility's, and `SupplyConvoy` comes before `PreCollapseFacility` in `BUILT_IN`. After delivery the planet opens `ConvoyCacheDialog`, whose Continue (90-93) goes straight to `RuleBasedInteractionDialogPluginImpl`. `FacilityDialog` can't be reached until the cache is raided, and with no marines the raid is disabled, so the facility is locked for the whole run. Fix: convoy destinations skip claimed planets (1.1.A2), and Continue chains to the next claimant with a visited set (1.1.A3).
- [x] **1.1.B2 One anomaly's `shape()` throwing discards the whole layout** (low, confirmed path). `ChartStarSystemGenerator` (284-287) runs `shape` with no per-anomaly isolation. Any exception scraps every anomaly's terrain and the feature and leaves a fallback star. `generate` isolates each anomaly, so the two stages are inconsistent. Wrap each `shape` in its own try/catch and drop only that anomaly. 3.B1 covers what happens to the anomaly list after the fallback.
- [x] **1.1.B3 Hook dispatch has no per-anomaly isolation** (low, possible). `ChartRunScript` (77-79) runs every anomaly's `checkRun` inside one log-once catch, so one anomaly throwing every second silently stops every later one. Skirmish comes before relay, so a skirmish failure would stop relay syncing. `ChartContentListener` and `ChartCampaignPlugin.dialogFor` have no catch at all, and `dialogFor` runs for every interaction in the sector. Put a try/catch per anomaly inside the `ChartAnomalies` dispatch, logged once per anomaly id.
- [x] **1.1.B4 `RelayNetwork.holdsBackBossUnique` withholds the guaranteed unique in runs without a relay** (low, latent). `allWoken` returns false when the run has no network, so the hook says "hold back" for every other run (`RelayNetwork` 55-57, `RelayStates` 54-55). Only relay sets `guaranteedBossUnique` today, so nothing is visible yet. Check that the run has the relay anomaly first, or dispatch only to anomalies in the run (1.1.A1).
- [x] **1.1.B5 Anomaly run state survives a failed content build** (low, possible). If `settle()` or anything after `generateAnomalies` throws, the content becomes `ChartContent.none` and `run.anomalyIds` is empty. The excavation, facility, forge and post state already written to `ChartRun.anomalyStates` stays (`ChartContentGenerator` 134-138, 794-797). The Excavation and Facility dialogs check only that state and the run system, so planets still offer sites in a run that "has no anomalies". Fix with staged state (1.1.A2), or clear anomaly states in `removeEverything`.

### 1.2 Per-anomaly bugs: Breach, Convoy, Debris, Excavation, Facility, Forge

This reviewer's abstraction findings are merged into 1.1: dialog base into A3, raid objective into A5, guards and encounters into A4, spot picker into A2, live-run check into A1, breach discs into A13. Its convoy/facility Continue bug is 1.1.B1.

- [x] **1.2.B1 Escorts turned hostile after delivery patrol forever** (low, likely). After the freighters despawn at the exit, the escorts are sent out with `leave()`. If the player pulses or attacks one before it despawns, `convoy/ConvoyScript.turnHostile` (246-270) calls `guard(escort, null)` (349-358), which clears its exit assignment and patrols. The phase is DELIVERED and `freighters` is null, so neither `leave` nor `flee` runs again, and the escorts stay hostile in the system for the rest of the run. Fix: in `turnHostile`, call `leave(convoy)` when `freighters == null` or the phase is DELIVERED or LOST.
- [x] **1.2.B2 Derelict Forge dialog text uses the UI catalogue** (low, confirmed). `forge/ForgeDialog` (113-131) calls `Translation.msg(...).text()` and `ForgeTexts.programName/costText` outside `I18n.forGameText`. When the UI and game languages differ, the runs-left line, program lines, stock line and Run options show in the UI language. Wrap them.
- [x] **1.2.B3 Static `expectedOpponent` in `ShaftEncounters` can outlive a battle** (low, possible). It is set in battleContextCreated and cleared only in notifyLeave (15, 42-67). If the FID ends without notifyLeave (total defeat, then respawn), it keeps a reference to the old fleet graph, across a save load, until the next shaft fight. This is a memory leak only. Clear it in `config()` and `afterDefinitionLoad`, or use a memory flag.
- [x] **1.2.B4 Kept facility and shaft guards never recover** (low, likely; design call). After a fight the player didn't win outright, the guard fleet sits in planet memory outside any location and nothing advances it (`FacilityGuards` 27-40, `ShaftFleets` 32-44). Enemy CR and hull damage stay frozen while the player rests and re-engages. Vanilla's `SalvageDefenderInteraction` attaches a `FleetAdvanceScript` for this. Decided: guards should recover, so add one. `ShaftFleets` also never calls `setContainingLocation`.
- [x] **1.2.O1** `ConvoyRole.parse`, `ConvoyStep.kind()` and reads of `SupplyConvoy.ROLE_MEMORY_KEY` exist only in tests. Main code writes the role key and never reads it.
- [x] **1.2.O2** `ForgeGuards.arm` sets `$hasDefenders` permanently, then `muster` overwrites it with a 0-day expiry. Nothing reads it, so it is a dead write.
- [x] **1.2.O3** `BreachFieldRenderer.render:36` allocates a `RadialBand` every frame. Cache it on the terrain.

The reviewer checked these and found no problem: zero save trace (the run closer removes run-bound scripts, listeners, intel, fleets and entities; planet-memory guards go with the system), double rewards, seeding, and timer units.

### 1.3 Per-anomaly bugs: Heist, Hypershunt, Relay, Skirmish, Station, Storm, single-class anomalies

This reviewer's abstraction findings are merged into 1.1: salvage guards into A4, far-orbit search, the centre fallback and relay `spreadOrbit` into A2, fleet reveal into A12, live-run lookups into A1 and 2.A1, storm sheet slicing into A8 and 4.A4. Its relay boss-unique veto is 1.1.B4.

- [x] **1.3.B1 Re-engaged hypershunt guards drop chart loot and vanilla drops twice** (med, likely; I confirmed the drift in code). `hypershunt/HypershuntGuards.muster` (60-82) calls `ChartAnomalyFleets.prepare(..., lootable=true)` only when it creates the fleet. Vanilla `SalvageDefenderInteraction.notifyLeave` clears the defender fleet's memory and keeps a damaged fleet for 10 days, so after a fight the player didn't win outright, the chart ledger (START_DP, LOOT_DP_PAID, run key, hostility flags) is gone. On re-engagement, `ChartFleetLoot.adoptDefender` (73-95) re-adopts it with START_DP set to the remaining DP and nothing paid. The remaining ships then drop a full share again, and `ChartVanillaLoot.addToFleet` runs a second time. The result is up to about 2x chart items and double vanilla drops. `ForgeGuards.muster` avoids this by preparing with `lootable=false` on every muster. Fix: prepare on every muster as Forge does, and let `adoptDefender` own the ledger and the one-time loot (1.1.A4 makes this one shared helper).
- [x] **1.3.B2 Storm Front's sensor penalty and terrain key are not lifted when leaving** (low, confirmed). `storm/StormFrontTerrainPlugin` (148-153) applies a temporary mod `exiledSector_chart_storm_front_stat_mod` and BaseTerrain's `$terrain_...` key. `StormFront` has no `clearFrom` (Breach has one), and `ChartRunEffects.removeFrom` doesn't strip it. A Transverse Jump or an exit from inside the band carries -50% sensor range into the normal sector for up to 0.1 days, and a save in that window keeps the mod and the key. Add `StormFront.clearFrom` that removes both.
- [x] **1.3.B3 Stellar Flares in a starless system does nothing but still grants its loot line** (low, likely). `StellarFlares` (22-26) iterates `site.stars()`. `dense_nebula` removes its star (`ChartStarSystemGenerator.hideNebulaCentre`, 329-344), and deep-space and abyssal centres have none. `chart_anomalies.csv` has no `excludes` against them, and a Flare Terminus catalyst can insert it there. The system shows the icon and gets `CHART_FEEDSTOCK_PERCENT:50` with no flares. Decided: Stellar Flares must never be placeable in a system without a star, whether by rolling or by catalyst. Use a placement rule that checks for a star, not a list of excluded types, so new starless types are covered automatically. A related possible case: `MagneticStorm.shape` (33) uses `site.centre()`, which in a nebula may be the removed hidden star, so the field could orbit an entity that is not in the location.
- [x] **1.3.B4 `RelayChain.settle` is not idempotent** (low, possible). `relay/RelayChain` (97-115) runs from `AtlasProgress.closed`, which runs at the start of every `ChartRunCloser.close` and in `ChartLoadSweep:85`. If closing runs twice (a failed record delete, or the sweep closing again), `recordAt(cell)` is null, `chainAt` rolls a fresh stage-1 chain, and `completed && allWoken` inserts a second network onward. Record a `relay_network.settled` state on the run and return early when it is set.
- [x] **1.3.O1** `StormBattlePlugin` keeps striking after the battle is decided: there is no `engine.isCombatOver()` check. Cosmetic.
- [x] **1.3.O2** `CoronalHypershunt:86`: the fallback orbit uses `radius / 20` days, while `clearOrbit` uses `radius / 15`, so the orbit period depends on which path ran. Trivial; goes away with 1.1.A2.

The reviewer checked these and found no problem: scripts are added last in `generate` (no orphan on throw), terrain params are copied into plain fields, Post and Relay state are string-encoded, and `NexusFleetManager` suppresses vanilla's encounter-point listener correctly.

### 1.4 Anomaly knowledge leaking into atlas, layout, content and run (atlas/layout/content reviewer)

The anomaly-to-layout/content direction is clean: anomalies go through `AnomalyLayoutSite`, `AnomalyContentSite`, `ChartSites` and `ChartAnomalyFleets`. The reverse direction leaks.

- [x] **1.4.A1 Move Faction Skirmish out of content** (L).
  - `content/ChartContentPlanner` (74-87) hard-codes `FactionSkirmish.ANOMALY_ID`: it forces a station boss and runs `SkirmishPlanner`.
  - `ChartContentGenerator.Build` holds about 170 skirmish-only lines: fields (382-387), `joinSkirmish` (283-300), `spawnAttackerFleet` (533-574), `dressAllies` (576-587), `stagingPoint` (589-608), `assignAttacker` (610-622) and `settleSkirmish` (815-829).

  Add a `ChartAnomaly.adjustPlan(plan, context)` default hook and move skirmish fleet spawning into `anomaly.skirmish` behind `adjustPlan` + `generate`.
- [x] **1.4.A2 Run lifecycle hooks on `ChartAnomaly`** (M). `run/ChartRunActions` (92-93) hard-codes `FactionSkirmish.join` and `RelayChain.join`. `atlas/AtlasProgress` calls `RelayChain.closed` (92-99), and its `captureForWinner` (26-28) is skirmish-only. `AtlasRunNode` carries skirmish fields. Add default hooks `joinRunNode(node, generator, state, cell, choice)`, `runCompleted(sector, run)` and `runClosed(sector, run)`. `AtlasRunNode`'s encoding must stay readable.
- [x] **1.4.A3 One `RunFactionDress` helper** (S). The "overrides plus a neutral-to-all loop" faction dressing is written three times:
  - `content/ChartContentEngine` (38-40, `dressAllies`/`dressConvoy`)
  - `VanillaChartContentEngine` (516-552), which imports `ConvoyRules` and `SkirmishFactions`
  - `debris/DormantFleets.dressFaction` (82-95)

  Add `RunFactionDress.apply(runFaction, name, article, isOrAre, towardsPlayer, neutralToAll)` in the anomaly root and take it out of `ChartContentEngine`. `ModdedChartFactions:24` also imports the `DormantFleets` and `SkirmishFactions` ids.
- [x] **1.4.A4 (partly done)** **Star anomaly ids in layout** (S). `layout/ChartLayoutRecipe` (7-8) hard-codes the black hole, neutron star and magfield ids from `StarAnomalies`. Derive `ANOMALY_STARS` from the registered star anomalies' `primaryStar()`. `ChartStarSystemGenerator` (433-450) holds neutron-star pulsar logic that could move to the neutron star anomaly.
- [x] **1.4.O1 Misplaced classes**. `atlas/AtlasBorders` is used only by `FactionSkirmish:83`; move it to `charts.anomaly.skirmish`. `ChartPois.modRequirementMet` gates atlas data, catalysts and the anomaly framework; move it to the charts root or `compat`.

## 2. Charts run pipeline (root, run, cleanup, wormhole, generation, objective, effect, loot)

### Abstraction

- [x] **2.A1 One set of live-run lookups in `ChartRuns`** (S). Pairs with 1.1.A1: these helpers are what `AnomalyRun.liveOrNull()` would build on. The "current run, if live and matching X" lookup is re-implemented in:
  - `loot/ChartDrops.liveSystemRun` (329-338) and `loot/ChartFleetLoot.liveRunOf` (133-140)
  - `objective/ChartObjectives.trackedRun` (26-32) and `run/ChartTransverseJumpAbility.chartRunAround` (236-244)
  - `wormhole/ChartWormholeDialog.runFor` (115-121) and `objective/ChartObjectiveIntel.liveRun` (57-60). Both treat a run as live while it is not CLOSED; everything else uses `isLive`.
  - every anomaly site listed in 1.1.A1

  Add `ChartRuns.live()`, `live(runId)`, `liveIn(location)`, `liveOwning(entity)`, `liveWith(anomalyId)`, and `open(runId)` for the "not CLOSED" variant. The names then make that difference explicit. Removes 60-80 lines.
- [x] **2.A2 One sweep of run-bound objects** (S). `run/cleanup/ChartRunCloser` (233-264) has five near-identical loops over sector scripts, transient scripts, system scripts, listeners and the ListenerManager. `ChartLoadSweep.removeOrphanIntel` (186-191) handles only `ChartObjectiveIntel`. `ChartContentGenerator.removeEverything` (860-864) handles only system scripts. `ChartLoadSweep` (82-87) re-types `ChartRunCloser.close(sector, run)` without the closer's try around `AtlasProgress`. Add `ChartRunCloser.removeBound(sector, system, Predicate<ChartRunBound> doomed)` covering all five collections plus intel, and use it from all three places; the repair path calls `close(sector, run)`. Fixes 2.B4. Removes about 35 lines.
- [x] **2.A3 `ChartRunEffects.apply` and `lift`** (S). `ChartRunScript` (47-49, 154-177), `ChartRunCloser:43`, `ChartLoadSweep` (49-61, which clears, then calls `removeFrom`, which clears again) and `ChartRunEffects` (68-87) each apply or lift part of the run effects. Add `apply(fleet, run)` for sensor range, conditions and battle size, and `lift(fleet)` for conditions, every `clearFrom`, and the temporary and permanent mods. The script keeps one `effectsApplied` flag. Fixes 2.B2.
- [x] **2.A4 One copy path in `ChartGenContext`** (S). `withAtlasNode` (244-258), `withAnomalies` (260-272), `copyWith` (292-302) and `withSkirmishSide` (324-335) each copy `forcedAnomalies`, both boss guarantees and the skirmish fields by hand. Add a private `derive(effects, factionId)`. `withAtlasNode` replaces `guaranteedBossTierUp` but ORs `guaranteedBossUnique`. Decided: OR both.
- [x] **2.A5 Chart loot plumbing** (S). `ChartFleetLoot.roll` (225-239), `ChartCargoLoot.rollItems` (283-292) and `StarChartLootSource:161` each compute scaled item chances and the feedstock and currency multipliers, then call `ChartDrops.roll(..., Mults.of(context, m))`. `ChartDrops:274` copies `ChartGenContext.multiplier` inline. Add `ChartGenContext.lootModifiers(scale)` (the core `LootModifiers` record already exists) and a `ChartCargoLoot.rollWithCharts(...)`. This is where 2.B3 gets fixed.
- [x] **2.A6 `DropData` from `ChartLoot.Entry`** (S). `ChartVanillaLoot.addToFleet` (337-399) repeats the entry-to-`DropData` conversion that `drops()` (280-290) already does, and builds value drops twice. Have `addToFleet` call `drops(...)` and add one `valueDrop(entry, mult)` factory.
- [x] **2.A7 Anomaly knowledge in generic chart tables** (S/M). This and 1.1.A12 are the same `ChartEffect` change; do them together. `effect/ChartEffect` (93-133) imports 8 anomaly classes and has four copy-pasted `.reduced`/`.increased` description overrides. Replace them with an enum constructor flag. `loot/ChartLoot` (94-97, 139-142) hard-codes four anomaly loot scopes; collect them from a `ChartAnomaly.lootScopes()` hook instead. Keep the effect constant names, because rolls persist them.

(Enum parse-by-text in `ChartObjective`, `ChartLoot.Mode`, `ChartLoot.vanillaType`, `ChartDrops.Source.byId` and `ChartEffect.byNameOrNull` belongs to 3.A9.)

### Bugs

- [x] **2.B1 Four persisted chart classes have no XStream alias** (med, confirmed). `ChartSaveAliases` (290-314) has no alias for these, all of which a mid-run save stores under their full class names:
  - `anomaly.heist.ListeningPostPlugin` (the custom entity plugin named in `custom_entities.json`)
  - `anomaly.heist.PostScript` (a system script added at `ListeningPost:77`)
  - `anomaly.station.MothershipFleetManager` and `NexusFleetManager` (system scripts added at `LaunchingStation:65`)

  The next package move or rename makes every save taken inside such a chart fail to load. `ChartPersistenceGuardTest` (64-78) checks a hand-written list, so it doesn't notice. Add `exiledSector.*` aliases for all four, and make the guard test scan for chart classes implementing `EveryFrameScript`, `CustomCampaignEntityPlugin`, `CampaignTerrainPlugin`, `Buff` or `IntelInfoPlugin`.
- [x] **2.B2 Leaving a chart lifts anomaly and relay effects only when it has a sensor-range line** (low, confirmed). In `ChartRunScript.liftRunEffects` (171-177), `ChartRunEffects.removeFrom` runs only `if (sensorRangeApplied)`. Take a chart with no sensor line that holds a Spatial Breach or relays: when the player leaves with entries left, the run goes back to OPEN, and the `BreachSupplyBuff` and the relay `nav_buoy`/`sensor_array` mods stay on the fleet outside the chart until they expire or the run closes. Fixed by 2.A3.
- [x] **2.B3 The completion reward ignores the Star Chart % and Currency % lines** (low, possible; intent unclear). `ChartDrops` (270-276), called from `ChartRewards` (236, 256), rolls the reward with `rewardMult` only, while every other in-run source uses `Mults.of(context, …)`. Catalyst % is applied here, but multiplied as `(1 + catalyst%)` where everywhere else it is added. Decided: the reward uses all of the chart's lines, with Catalyst % added like the others.
- [x] **2.B4 The load sweep removes only orphaned `ChartObjectiveIntel`** (low, possible). If a close step fails or a run record is lost, `ConvoyIntel`, the `ConvoyScript`/`PostScript` scripts and listeners, and ListenerManager entries of the dead run survive every later load. Fixed by 2.A2.

### Other

- [x] **2.O1** `ChartWormholePlacement.outermost` (41-56) repeats the band building in `blockedBands` (107-120).
- [x] **2.O2** `ChartRunActions.collapseOuter` (299-301) only passes the call through to `run.collapseOuter()`.
- [x] **2.O3 (partly done)** Two lifecycle cases are only fixed by the next load. A `LEAVE_FOR_NOW` orphan system (`ChartLoadSweep:126`) is never revisited after the player leaves it. A run whose closer keeps failing is re-closed every two frames by `ChartRunScript.closeWhenClear`, with no give-up.

The reviewer checked these and found no problem: close ordering, the Transverse Jump swap before saves, the respawn and death path, `removeStarSystemNextFrame` leftovers, topography scans, procgen name release, the salvage-defender memory snapshot, single delivery of battle loot, the `ChartFleetConditions` cache, and alias registration order.

## 3. Charts atlas, layout and content

The atlas is rebuilt from its seed on every load, and completed cells are saved by coordinate. Any change to the output of `AtlasTerrain`, `AtlasHash`, `AtlasBiomes` or `AtlasTerritories` moves the map under saved progress, and `AtlasState.generatorVersion` is stored but nothing branches on it. Every refactor of atlas generation code below must therefore produce bit-identical output.

### Abstraction

- [x] **3.A1 One POI site spawn path** (S). `content/ChartContentGenerator.Build.placePoi` and `addVanillaLoot` (418-463) duplicate `content/ChartSites.add` (50-73), which every anomaly uses through `AnomalyContentSite.addSite`. Make `ChartSites.spawn(...)` the single path: entity choice, positioning, salvage-spec override, `tagContent`, vanilla loot and cleanup on failure. Keep the random draw order. The two copies disagree when loot fails (`placePoi` keeps the site, `ChartSites` removes it and rethrows). Decided: remove the site and log the error. Removes about 35 lines.
- [x] **3.A2 Fleet launch and run-fleet marking** (M).
  - `ChartContentGenerator.spawnFleet` (624-677) and `spawnAttackerFleet` (533-574) share the whole create, prepare, sensor/loot, add, assign and track sequence.
  - `content/ChartAnomalyFleets.prepare` (87-96) repeats the sensor and loot setup.
  - `anomaly/debris/DormantFleets` (169-193) re-types `markContent` and the three tags of `tagContent` (327-331).

  Add a `Build.launch(...)` and a public `ChartContentGenerator.markRunFleet(fleet, runId, tier, seed)`, and optionally `ChartIds.writeSeed`. Keep the `nextLong()` order. Removes about 60 lines.
- [x] **3.A3 `ChartSites.pointAround(centre, degrees, radius)`** (S). The polar offset is written by hand about 14 times:
  - charts side: `ChartContentGenerator` (555, 739, 749), `ChartSites.Orbit.position`, `ChartAbyss:102`, `ChartWormholePlacement:79`
  - anomaly side: `BreachRules:107`, `SpatialBreach:105`, `ConvoyFleets:63`, `ConvoyRoute:71`, `AnomalousDebris:119`, `AnomalousDebrisTerrainPlugin:273`, `PicketStation:42`, `StormStrikes:50`

  Use `Math.cos` rather than LazyLib's `getPointOnCircumference`, which may use approximate trig and shift exact positions.
- [x] **3.A4 `WeightedPick.pick(Map<K,Float>, Random)`** (S). Hand-written weighted picks sit in `layout/ChartDeepSpace` (40-55), `ChartLayoutGenerator.pickType` (94-110), `layout/abyss/ChartAbyss` (43-64), `layout/sketch/ChartSystemSketcher` (228-241), `ui/atlas/AtlasDecorations` (~174-185) and `anomaly/storm/StormStrikes` (~32), while content already uses core `items.WeightedPick`. Its scheme matches `pickType` and `ChartDeepSpace` bit for bit. `ChartAbyss` needs non-positive weights skipped. The sketcher uses `nextDouble`, which only affects previews. Removes about 70 lines.
- [x] **3.A5 Shared seed mixer and value noise** (S, must stay bit-identical).
  - The SplitMix64 mixer is copied in `atlas/AtlasHash` (56-61), `ChartLayoutGenerator.derivedRandom` (112-117), core `ui/decoration/HyperspaceClouds` (~413), `ui/atlas/AtlasDecorations` (~302) and `ui/atlas/AtlasTerritoryBorders` (~384).
  - Two-octave value noise is copied in `atlas/AtlasTerrain` (316-340) and `AtlasTerritoryBorders` (369-401).

  Add a core `SeedHash` (`mix`, `derived`, `unit`) and a `charts.atlas.AtlasNoise`. Only `AtlasTerritoryBorders` (UI) may change output. Removes about 60 lines.
- [x] **3.A6 Special-site behaviour on the `AtlasSpecial.Kind` enum** (M). Per-kind if/else is spread across `AtlasProgress` (48-60, 114-119), `AtlasReach` (73-81), `AtlasOpening` (55-61, 82-84) and `AtlasDevTools` (38-39). Move it onto the enum: `revealLanes`, `onCompleted`, `oneAttempt`, `openBlocker` and `nodeEffectsId`. The enum is not saved.
- [x] **3.A7 (partly done)** **Catalyst and insert eligibility** (S).
  - The fog check is duplicated in `AnomalyCatalystUse` (20-22) and `RedeploymentCatalystUse` (28-30).
  - `RedeploymentCatalystUse` (31-39) re-types the origin/void, completed and open-run checks from `AtlasAnomalies` (92-100).
  - The star and non-star pairs `canInsert`/`canInsertStar` and `insert`/`insertStar` (70-129), plus the `forcesStar()` ternaries, collapse into one method each with a `forcingStar` flag.

  Add an `AtlasAnomalies.systemCheck(...)` returning `InsertCheck`.
- [x] **3.A8 `JitteredGrid` for biomes and territories** (M, must stay bit-identical). `AtlasBiomes` (105-266, five window loops plus `site`) and `AtlasTerritories.territoryPoint` (94-116) use the same jittered point formula, and `AXIS_SALT = 0x59` is copied between them. Add a package-private `JitteredGrid(seed, pointSalt, spacing)` with `point` and `forEachIn`.
- [x] **3.A9 Core `EnumKeys.parse/key`** (S). About 12 hand-written parse-by-key methods:
  - `AtlasSpecial.Kind.parse`, `AtlasCatalysts.Family.byKey`, `AtlasCatalysts.Effect.byKey`, `AtlasAnomalyRecord.Source.parse`
  - `ChartBossType`, `ChartPoi.Kind`, `ChartLayoutGenerator.parse`, the `AtlasData` placement `valueOf`
  - outside this scope: `SkirmishSide`, `ChartObjective`, `ChartLoot.Mode`, `ExcavationSite.Access`

  `Source.parse` reads saved strings, so keep upper-case names working. Removes about 80 lines.
- [x] **3.A10 (partly done)** **Small duplicates** (S).
  - The objective-to-targets switch is written twice in `ChartContentGenerator` (808-811, 818-821).
  - The corona outer edge plus flare reach is computed three times: `ChartContentGenerator.hazards`, `ChartWormholePlacement.ringTerrainBand` and `ChartStarSystemGenerator.farStarReach`.
  - `FeatureSite.between/angle` duplicates `AnomalyLayoutSite.between/angle`, and every feature computes `outerRadius() + GAP + between(...)`, which wants a `FeatureSite.orbitBeyond(gap, spread)`.

(The anomaly seam findings from this reviewer are in 1.4.)

### Bugs

- [x] **3.B1 A failed layout still hands every planned anomaly to content** (low, confirmed). When the vanilla generator throws, `ChartLayoutGenerator.generate` (136-161) scrubs the system and adds a fallback star, which removes every anomaly's shape terrain. It still returns the full `anomalyIds`. Content then generates Black Hole derelicts around an ordinary star, Neutron Star lines with no neutron star, Asteroid Storm caches with no belts, and so on. Return an empty list (or only the anomalies whose shape survived) when `!built.generated()`.
- [x] **3.B2 Possible region-cache thrash** (low, possible). With neighbour rules on (`frozen_fringe`, `furnace`), a cache miss in `AtlasBiomes.pickRegion` (189-266) recursively decides about 70 outranking sites, and each of those can recurse again. If one decision needs more than the 4096-entry LRU, entries are evicted mid-recursion and recomputed. Results stay correct. Unmeasured. Use a larger or unbounded per-generator cache.

### Other

- [x] **3.O1** `AtlasRuntime.generator` (17-19) rebuilds `AtlasData.rules()` (every anomaly candidate with new lambdas, plus map copies) on every call, before the cache check. Compute it only on a miss.
- [x] **3.O2** `AtlasDevTools.complete` (94-97) calls `spendInsertedAnomalies`, while real play (`AtlasProgress.complete`) calls `spendUnchainedAnomalies`. The console Complete therefore kills Relay chains that real play keeps, so testing chains from the console differs from real play.
- [x] **3.O3** `AtlasGenerator.THREAT_ONLY = Set.of("threat")` duplicates `ChartThreatFleets.FACTION_ID`.

The reviewer checked these and found no problem: atlas determinism (the only unseeded randomness is the master seed, an unreachable fallback and a cosmetic portrait), termination of every loop and retry, no re-entrant `computeIfAbsent`, lane symmetry, `AtlasState` save shape, and the foiler-mod terrain rules.

## 4. Star Charts UI, items and console (non-`charts` code in `exiledsector-star-charts`)

### Abstraction

- [x] **4.A1 Shared disassembly mode for storage panels** (M). `ui/socket/StarChartStoragePanel` copies the disassembly mode of core `ui/socket/ItemStoragePanel` almost line for line: marking, the confirm dialog, the rarity breakdown, the parts tally and notice, the mode button and the cell footer. `addRarityBreakdown`, `partsText` and the tally loop are identical. Add a core `ui/socket/DisassemblySelection` that holds the marked set and the mode state, writes the confirm text, adds the mode button, renders the cell line and the hint, and runs `disassemble(items, materials, disassembler)` returning a result with its notice. Each panel keeps its own filtering and rebuild calls; the unique-item warning becomes a flag. Removes about 210 lines. UI only.
- [x] **4.A2 Sectioned tile grid and one tile class** (M). `AtlasCatalystPanel` copies `BenchMaterialsPanel`'s height calculation, section loop and count-label rows (same `COUNT_HEIGHT`/`COUNT_INSET`). `CatalystTile` is `WorkbenchTile` with different constants. The fill + inner glow + stroke frame also appears in `IconTileCell` and `ItemCell`. Add a core `SectionedTileGrid` (`preferredHeight`, `build` with a tile factory) and a `TileFrame.draw(...)` in `ui/util`. Give `WorkbenchTile` separate `available` and `toggledOn` suppliers, then delete `CatalystTile`. Removes about 160 lines. Keep the differing border and glow values as parameters.
- [x] **4.A3 Planet sphere drawing helper** (S). `AtlasSystemPreview` (319-340) and skill-tree `SkillTreeStarRenderer` (128-148) run the same GL sequence: cull face, front face, tilt/pitch/spin rotations, `sphere.draw`, rim passes. Both also build a spec-by-type map (`ChartStarSystemGenerator` loops over the planet specs too). Add a core `PlanetSphere.draw(...)` and a cached `PlanetSpecs.byType(type)`. Removes about 40 lines.
- [x] **4.A4 Sprite-sheet cell helper** (S). Five places set texX/texY/texWidth/texHeight by hand before `renderAtCenter`: `AtlasSystemArt.drawCell` and `drawStorm`, `StormOverlay:153`, `StormFrontTerrainPlugin:233`, `AnomalousDebrisTerrainPlugin:254`, plus a raw GL version in `CatalystIconFx`. Add `SpriteDraw.drawCellAtCenter(sheet, cols, rows, cell, x, y, w, h, angle, tint, alpha, additive)` in core `ui/util`. Removes about 40 lines; puts the half-texel rule in one place.
- [x] **4.A5 Console argument helpers** (S). Six commands tokenise arguments by hand with a redundant null/blank check that `CampaignCommand` already does (`AtlasGrantCommand`, `GrantStarChartsCommand`, `GrantAtlasCatalystsCommand`, `AtlasTargetCommand`, `ChartOpenCommand` twice, hull-upgrades `GrantHullItemsCommand`). Five atlas commands build the same "Can't use ... Usage:" message. Add `ConsoleArgs.tokens(String)` in core and `badSyntax(rest)` on `AtlasTargetCommand`.
- [x] **4.A6 One source for open-failure text and checks** (S). `ChartOpenCommand` and `ChartOpenStoredCommand` switch on `OpenResult` with identical strings. `ChartSoakCommand` and `AtlasResetCommand` each word "a run is already open" differently. `ui/chart/WormholeOpenCheck` re-checks the no-fleet, in-transition and in-system preconditions that `ChartRunActions` (170-178) already returns as `OpenResult`, so the UI and console checks can drift apart. Add `ChartConsoleText.openFailure(OpenResult, ...)` and have `WormholeOpenCheck` reuse a shared `ChartRunActions.fleetBlocker(fleet)`.
- [x] **4.A7 Small data-rule duplicates** (S).
  - `AnomalousDebrisCurrency.canUse` repeats `GrantedModifiers.canGrant`.
  - `AnomalousDebrisCurrencies` parses item_types twice (`parse` and `checkRows`).
  - `StarChartDefinitions`, `StarChartImplicits` and `StarChartDataRules` each build the same "system type"/"minimum tier" error text; add `systemTypeProblem` and `minTierProblem` helpers.
  - `StarChartImplicits:25` declares its own `ENTRY_SEPARATOR` instead of using `ItemDataIntegrity.ENTRY_SEPARATOR`.
- [x] **4.A8 (partly done)** **Special-item localise and register** (S). `AnomalousDebrisCurrencies`, `AtlasCatalystItems` and `WormholeTechItems` each run a localise loop shaped like core `MaterialItems.localise`, which hard-codes a `material.` prefix. `WormholeTechItems` also repeats `MaterialItems.data/add/displayName`. Give `MaterialItems.localise` a key-prefix parameter and call it from all three.
- [x] **4.A9 `ItemStore.ownedStarCharts()`** (S). The "owned Star Charts or empty" expression appears in five places: `ChartListCommand`, `ChartOpenStoredCommand`, `StarChartBench`, `StarChartBrowser`, `StarChartStoragePanel`.
- [x] **4.A10 Atlas line styling and faction names** (S). Tone-to-`StyledText` conversion is written in `AtlasScreen` (925-934) and twice in `AtlasNodeText`. Add `Line.styled()`. `AtlasScenery.factionName` duplicates `AtlasNodeText.factionName`.
- [x] **4.A11 Catalyst tooltip written twice** (S). `AtlasCatalystItemPlugin` and `AtlasCatalystPanel` write the same title, category, adds-line and flavour. Add `AtlasCatalystItems.writeTooltip(tooltip, catalyst, pad)`; each caller adds its own footer.
- [x] **4.A12 Overlay fade lifecycle** (S, low value). `AtlasScreen`, `ModificationBenchScreen` and `HullUpgradesScreen` each repeat the open-level, closing and close-requested fade logic. A small core `OverlayFade` could hold it.

### Bugs

- [x] **4.B1 Per-frame lookups and allocations in atlas rendering** (low, confirmed). With an overlay shown, `AtlasTerritoryBorders.render` (145, 155) resolves each region's colour every frame through `FactionStarColours` and `getFaction`, and allocates a `Color` per region. `AtlasMapCanvas:490` allocates a `NotableLook` per visible node per frame. Resolve colours once in `attach()`, and cache `NotableLook` by state.
- [x] **4.B2 Catalyst anomaly name has no fallback** (low, likely). `AtlasCatalystItems:65` calls `Translation.text("chart.atlas.anomaly." + id)` directly. `AtlasNodeText.name` checks `Translation.has` and falls back to a tidied id. If an id is missing from the strings, the catalyst tooltip shows the raw key. Move `name`/`tidy` into a charts-level text helper and use it here.

### Other

- [x] **4.O1 Dead code** (only tests call these): `StarChartOpening.open`, `WormholeOpenCheck.current`, `StarChartBrowser.storedCharts/inSelectedTierCount/toggle`, `AtlasAbyssShade.hasAbyss`, `StarChartItems.register/clear`, `StarChartCurrency.unregisterAll`. `StarChartImplicits.clear()` also leaves `SHARED_CHOICE` and `UNLISTED_CHOICE` set.
- [x] **4.O2 Package placement**. `ui/chart/WormholeOpenCheck`, `StarChartOpening` and `StarChartCrafting` contain no UI. Move them to `charts.wormhole`, `charts.run` or `items`. Check first that none of them is persisted. Leave `AtlasAbility` where it is, because ability plugins are saved.
- [x] **4.O3 `items` depends on `ui`**. `items.AtlasCatalystItems` imports `ui.cargo.CatalystLook` and `CargoIconLook`. Move `look()` into `ui/cargo`.

The reviewer checked GL state in `ui/atlas`, `ui/cargo` and `ui/socket` and found no problem: everything runs inside `GlScope`, is GL11 only and uses no `glGet`. It also found no problem with the queued-action list or with combining and disassembling the loaded chart.

## 5. Core skill effects (`skills/`, `skilleffect/*`, `effects/`)

These are already well factored and need nothing new: listener registration (`SkillEffectSupport.ensureListener`, `ListenerEffect`, `FighterListenerEffect`, `LethalHitArbiter.addSaver`, `AuraField.register`; no listener can be added twice on refit), range queries (`CombatQueries`) and drones (`WeaponDroneShell` and friends).

### Abstraction

- [x] **5.A1 `TimedTargetDebuff`** (S). `onhit/MissileSnareListener.Snare` (36-63) and `onhit/BeamShieldDisruptionListener.ShieldDisruption` (35-62) are the same code apart from the stat getter, the multiplier formula and the duration. Add one listener with a map from `Spec(modId, seconds, stat, multFor)` to state, entered through `refresh(target, spec, percent)`, which removes itself when the map is empty. Removes about 50 lines, and future on-hit debuffs become one line.
- [x] **5.A2 (partly done)** **Use the existing backings for "write the magnitude into one dynamic mod" bodies** (S).
  - `WeaponSkillEffect`: 83, 99, 151, 157, 180.
  - `LogisticsSkillEffect`: 70, 169, 185.
  - `DefenseSkillEffect`: 39, 243.
  - `FighterSkillEffect`: 95.
  - `FluxSkillEffect`: 37, 43.
  - `PhaseSkillEffect`: 56.

  Add a `Flag(key)` backing for the four `*_COUNTS`/`PD_*` flags. Otherwise use `SimpleStatEffect(FLAT, dynamicMod(KEY), ...)`, `StatTarget.all(...)` and `BonusPercentagePoints`. `appliesToNpcShips` needs a hook on `EffectBacking`, or those two constants keep a small body. Removes about 90 lines. Description text must stay identical; check it against the description golden file.
- [x] **5.A3 (partly done)** **`PerCountEffect` and a generalised `PerDModMultEffect`** (S/M).
  - Per-count bodies: `FluxSkillEffect` 51 and 64; `LogisticsSkillEffect` 31, 80, 97 (cap -80) and 147.
  - `DefenseSkillEffect` 114, 136 and 158 repeat `PerDModMultEffect` and differ only in which `apply*DModEffectMult` they call.

  Add `record PerCountEffect(count, target, mode, minTotal, statKey, lowerIsBetter)`, and let `PerDModMultEffect` take a `MultApplier` in place of a `StatTarget`. Keep or drop the modId suffixes deliberately. Removes about 100 lines.
- [x] **5.A4 `DerivedStatEffect`** (S). Some effects apply after other effects and then again after ship creation: `WeaponSkillEffect` 31, 52 and 117, plus `LogisticsSkillEffect:147` (after others only). Add `DerivedStatEffect(applier, mode, reapplyAfterCreation)` and `EffectBacking.appliesAfterOtherEffects()`. Removes about 40 lines.
- [x] **5.A5 `Backings` factories** (S, mechanical). All 14 part enums repeat the same four constructors:
  - `AuraSkillEffect`, `BattleSkillEffect`, `DefenseSkillEffect`, `FighterSkillEffect`, `FluxSkillEffect`
  - `LogisticsSkillEffect`, `MovementSkillEffect`, `OnHitSkillEffect`, `PhaseSkillEffect`, `ReadinessSkillEffect`
  - `ShieldSkillEffect`, `ShipSystemSkillEffect`, `SpecialSkillEffect`, `WeaponSkillEffect`

  Add static `Backings.stat(...)` and `Backings.listener(...)`, and keep only the `(EffectBacking)` and no-arg constructors. Removes about 90 lines. It touches every enum, so expect conflicts with volume branches. Decided: do it now.
- [x] **5.A6 (skipped)** **`PendingHit` and `ReentryGuard`** (S). The pending hit checked against `getParamAboutToApplyDamage()` is written in `shield/SharedShieldDamageListener` (41-91) and `fighter/WingSharedFateListener` (70-104). Static re-entry flags appear in `WingSharedFateListener.applyingSharedDamage` and `BeamSplitListener.applyingSimulatedHit`. Removes about 30 lines and puts the "same hit" rule in one place.
- [x] **5.A7 (partly done)** **Bypassed helpers** (S; fold into whichever change touches the file).
  - `weaponOf(param)` is written twice (`DamageConversionListener` 57-62, `DisintegrationListener` 54-59). Add `CombatQueries.weaponBehind`.
  - Hand-written radius filters on `shipsNear` appear in `MalfunctionAuraField`, `TerrifyingPresenceField` and `RepairCoordinatorField`. `DeathExplosionListener` (50-51) scans every engine ship instead of the grid. Add `CombatQueries.shipsWithin(loc, radius, filter)`. Don't use `hostilesNear` here: it also drops phased ships.
  - `EffectText.templated` is written out by hand in `FighterSkillEffect:202`, `ShipSystemSkillEffect:36` and `RadiusGate:18`.
  - `SolarisOverdriveListener` (94-104) re-implements `PlayerStatus.showIfPlayer`.
- [x] **5.A8 `effects/` duplicates** (S). `ItemLootListener` (81-86) rebuilds the enemy-losses list that `PlayerEngagement.lostBy` (31-41) builds, with a small divergence in how it finds the player's side; expose `enemyLosses` instead. `FighterBayOverflow` (52-66) and `UnfitWeapons` (45-58) end with the same update-and-announce block; extract a `ReturnedToCargo.announce(...)`.

### Bugs

- [x] **5.B1 Per-frame lambda allocation on every ship with Flux-Scaled Volatility** (low, confirmed). `flux/FluxScaledVolatilityListener` (71, 90-93) builds a capturing lambda every frame for every ship with the effect, AI ships included, before `showIfPlayer` checks whether this is the player ship. Return early unless the owner is the player ship.
- [x] **5.B2 (skipped)** **`HitDamageRestorer.track` keeps a stale entry when the same beam damage object moves to a new target** (med if it triggers, possible). Beam `DamageAPI` objects persist across frames. Say an entry for D on target A is never restored, and D then hits B. In `HitDamageRestorer` (43-61):
  - The A entry is skipped, because its target differs (48).
  - The new entry records the already-reduced base damage as the original.
  - The reductions then compound.
  - A later hit on A writes the old base back onto D mid-use.

  It is unverified whether the engine ever skips `reportDamageApplied` after `modifyDamageDealt`, for example for a 0-damage EMP-converted beam tick. Fix: match on the damage object first, then restore and re-track if the target changed.
- [x] **5.B3 Each fighter launch resolves the whole tree again** (low, confirmed). `SkillTreeHullMod.applyTreeToFighter` (197-198, `treeFor` at 271) rebuilds `ShipEffectContext` and `StatBaseline` and queries every source before the `ResolvedTree` cache hits. That happens for every replacement fighter in long fights. Reuse the carrier's tree from its `ShipCombatPlan` custom data.
- [x] **5.B4 `applyAlwaysCountingSensorStrength` does full work on every fleet's sync** (low, confirmed). `logistics/FleetWideEffects` (49-73), called from `SkillTreeHullMod:222`, copies the member list, allocates two arrays and unmodifies a stat on every `onFleetSync` for every fleet in the sector, even when no member has the effect. Return before allocating when no member matches.
- [x] **5.B5 On-hit side effects with no hostility check hit allies on friendly fire** (low, possible; design call). These run on allied targets too:
  - `onhit/DisintegrationListener` (33-46) strips armour.
  - `BeamShieldDisruptionListener` (24-33) raises shield damage taken.
  - `BeamHardFluxListener` (19-24) turns soft flux hard.

  `MissileSnare`, `IonArc`, `ProjectileEcho`, `KillFluxVent` and `Furor` all check `isHostile`. Decided: add `isHostile` checks to all three.

### Other

- [x] **5.O1 (partly done)** Small per-frame allocations. `RepairCoordinatorField:67` wraps an already fresh list in another `ArrayList` every 0.25 s per coordinator. `RepairDroneRenderer:62` allocates a new `EnumSet` on every `getActiveLayers()` call; make it a constant.
- [x] **5.O2** `ThermoAcceleratorListener` (25-30) applies a fixed flux-cost stat from a listener constructor. Make that a `SimpleStatEffect` on the same node and keep the listener only for the hardpoint block.

The reviewer checked these and found no problem: `computeFluxDealt` used as EMP, `setDamage` plus restore with the beam type restored each tick, no modId collisions, `getWingMembers()` returning a copy, and the phase-field statics reset on load.

## 6. Core items, UI and infrastructure

### Abstraction

- [x] **6.A1 The hull bench as a `BenchModeSupport`** (M). The hull bench is the `null` fallback in `BenchModes` (29-32, `modeFor` `orElse(HULL_ITEMS)`). `SocketWorkbenchPanel` therefore branches on `domainSupport() != null` about 15 times, with parallel `usable`/`domainUsable` and `applyUse`/`applyDomainUse` pairs (504-731, 936-1230). `ModificationBay` (153-319) handles a storage panel and a mode picker separately. Add a core `HullBench implements BenchModeSupport`: HULL_ITEM plus SUBROUTINE, `refusal` from `ItemCrafting.verdict`, `use` from today's `applyUse`, `currencies()` from `hullBenchCurrencies`, and a `pickerSession()` wrapping `ItemStoragePanel.openPicker`. `BenchModes.of` then falls back to `HullBench.INSTANCE`. Removes 120-150 lines and fixes 6.B6. UI only. Keep the hull-path detail header without the description.
- [x] **6.A2 (partly done)** **Move domain type switches in core onto `ItemTypeSupport` hooks** (M). Core names star-chart and anchor types directly:
  - `ItemCrafting.takesCurrency` (84-86, hard-coded WORMHOLE_ANCHOR, also used by `ForgeOperation.verdict:84`), `corruptionOutcomes` (374-384, switches on SUBROUTINE, STAR_CHART and HULL_ITEM) and 425-428
  - `ItemData` (27, 35, 77, 130) and `ItemRoller` (52, 62, 75): STAR_CHART "rolls at tier, no rarity"
  - `AffixLayout:27`: STAR_CHART_LIMITS is hard-coded although `AffixLayout.register` exists
  - `ItemNames` (98, 111) and `ItemStoragePanel` (594 `listedKind`, 822)
  - the "applies to HULL_ITEM or SUBROUTINE" test in `ItemCurrency:50`, `MaterialItemPlugin:43` and `SocketWorkbenchPanel:725`

  Add `ItemTypeSupport` defaults `takesCurrency()`, `corruptionOutcomes(item)`, `tiered()`, `affixLimits()` and `listedKind(item)`, plus `ItemCurrency.appliesToShipItems()`. Corruption outcome lists must keep their order, because outcomes are picked by index. The core `BenchMode` enum also hard-codes the STAR_CHARTS and FORGE modes with their art, and `ItemStore` carries a star-chart prefix and `holdsStarCharts`. These are the same pattern.
- [x] **6.A3 (partly done)** **One item text writer for the tooltip and the bench details** (S-M). `SocketWorkbenchPanel.rebuildDetails` (1209-1243) repeats `ItemTooltip.writeBody` (34-55): the same title, base name, header, implicit and primary sections, centred. Its 1221-1228 re-implements `Item.headerLines` (320-325). Add `Item.statusLines()` (mirrored, corrupted and kind lines) and `ItemTooltip.writeSections(tooltip, item, hullSize, rollRanges, ItemTextLayout)`. Removes about 50 lines and stops the two views drifting apart.
- [x] **6.A4 Shared explicit add, remove and reroll** (S).
  - Weighted side pick then roll: `ItemCrafting.augment` (171-173), `substitute` (286-287), `ItemForging.graft` (81-82), `anchorReroll` (162-163).
  - Insert and refreeze if rarity changed: the same four (174-180, 288-292, 85-91, 164-168).
  - Remove and refreeze: `annul` (310-313), `excise` (113-116).
  - Per-line reroll: `ItemCrafting.reroll` (217-222), `stretchedReroll` (477-482), `Item.rerolledImplicit` (391-398), `ItemForging.perfected` (158-164).

  Add `AffixLayout.rollFrom(sides, random)`, `Item.addExplicit` and `removeExplicit`, and `RolledModifier.mapLines(...)`. Keep the Random call order where tests seed it. Removes about 50 lines.
- [x] **6.A5 `DomainContributions.first(mapper)` / `firstOr`** (S). "First non-null contribution" loops appear in `LootSources` (five times: 28-35, 37-45, 55-62, 79-87, 89-97), `CargoIconLooks` (56-61, 81-87), `ShipRecords.resolve` (21-26) and `BenchModes.of/modeFor`. Removes about 40 lines.
- [x] **6.A6 `CraftingCurrency` constant-specific bodies** (S). `canUse`, `use`, `wouldChange`, `respectsWards`, `createsCopy` and `mayChangeDefinition` each switch on `this` (79-136). `ForgeOperation` already uses constant bodies. Make `canUse` and `use` abstract per constant and turn the three booleans into constructor flags.
- [x] **6.A7 Tile chrome, icon drawing and input adapters** (S). `WorkbenchTile` (62-82), `IconTileCell` (49-63) and `ItemCell` (101-122) share the same background colour, border width and fill, inner glow, stroke sequence. `ItemCell` and `WorkbenchTile` repeat the `CargoIconFx` versus `AnimatedItemIcons` choice. Three anonymous `TileInput.Handler` adapters exist: `ItemCell` (73-93), `TileInput.forHooks` (30-50) and `SocketWorkbenchPanel.tileHandler` (1065-1096). Add `TileChrome.draw`, `TileChrome.drawIcon` and a `TileInput.Handler.of(...)` factory. This is the same helper as 4.A2's `TileFrame`, so build it once. Removes about 60 lines.
- [x] **6.A8 Smaller ones** (S each).
  - `SectorMarkets.forEach(sector, consumer)`: `SocketCustody.ownedShips` (185-218) and `MaterialMigration.savedCargoes` (52-85) both walk the markets, and they disagree on which locations to visit and on `getCargo` versus `getCargoNullOk` (6.B2).
  - `StackAbsorber.counting(predicate, adder)`: `RefitSchematic.absorbInto` and `MasterSchematic.absorbInto` are the same apart from the match and the add.
  - `Modifier.byId(list, id)` and `Modifier.lineIn(list, effect)`: the lookups are repeated in `ItemImplicits` (196-213), `CorruptionImplicitPool` (50-67), `Item.implicitModifier`, `StarChart` (178-182) and `StarChartImplicits` (102, 139, 152).
  - One settings listener with `ModSettingsListener.onReload(Runnable)`, replacing the one-method listener classes in `ItemRoller`, `ItemAnimationSetting`, `DomainSettingsListener`, `DiminishingReturns` and `NpcBonusScale`, and the paired `ensureRegistered` + `reloadSettings()` calls in `ExiledSectorSettings` (57-63).
  - `ItemForging.excisable` (107-113) and `anchors` (147-153) build the same unlocked list.
  - `ItemStoragePanel.openSectionItems`, `openSubroutines` and `openPicker` (127-159) repeat the absorb, settle and attach sequence; extract an `attachFresh(placement)`.

### Bugs

- [x] **6.B1 `NodeRingArt.drawPortalRings` leaks GL state** (low-med, confirmed). Lines 128-136 enable blend, disable `TEXTURE_2D` and set an additive blend func, but restore only blend. The caller path (`AtlasMapCanvas.drawNodes` from `AtlasScreen.renderBelow`) is wrapped only in a scissor-bit `GlScope`, so texturing stays off and blending stays additive for the rest of the atlas frame and for vanilla UI drawn after it. Use `try (GlScope scope = GlScope.flat().additive())` instead of the raw calls.
- [x] **6.B2 `MaterialMigration` creates cargo for every submarket on every load** (low, likely). `savedCargoes` (79-84) calls `submarket.getCargo()`, which lazily creates and persists cargo and mothballed fleet data for every never-opened submarket (about 150 or more) on every `onGameLoad`. Use `getCargoNullOk()` as `SocketCustody` does.
- [x] **6.B3 (skipped)** **`CampaignMessages.plain` does not escape `%`** (low, likely, latent). Lines 18-23 call `addMessage(String, Color)`, which goes through `MessageIntel` and then `addPara`, a format string. A `%` in another mod's weapon or wing name, an item name or a future zh string crashes when the intel row renders. `highlighted` (29) already escapes. Use `VanillaText.escapeFormat(message)`.
- [x] **6.B4 (partly done)** **`ItemData` round trip loses state for anything that isn't a chart** (low, confirmed, latent). `Item.traits()` (90-92) returns null and `applyTraits` (78-85) ignores implicits, and only `StarChart` overrides both. Encoding a hull item or subroutine drops corruption, mirrored, replaced or added implicits and `HullItem.targetSectionId`, while decoding does read corruption and mirrored. Moving such an item to cargo and back would un-corrupt it and reroll its implicits. Only charts use this path today (`ChartRunActions`). Make the base `traits()` and `applyTraits` handle those fields.
- [x] **6.B5 `ItemCell` recomputes frame colour and icon path every frame** (low, confirmed). `item.frameColor()` (108) and `item.iconPath()` (129, 134) run rarity, `explicits()` (which allocates), definition and alias lookups per visible cell per frame. Both values are constant for a cell's lifetime. Compute them once in the constructor, as `AnimatedCargoItemPlugin` does.
- [x] **6.B6 The workbench bypasses the currency hooks** (low, confirmed, latent). `SocketWorkbenchPanel` hard-codes `REPLICATION` (637) and `TRANSPOSITION || CORRUPTION` (644) instead of calling `currency.createsCopy()` and `mayChangeDefinition()`. A domain hull-bench currency that sets those flags would be handled wrong. None does today. Fixed by 6.A1.

### Other

- [x] **6.O1** `BenchModes.of`, `modeFor` and `detailLines` (12, 20-27, 38-42) read `EVERY_SUPPORT`, including switched-off domains, although CLAUDE.md lists bench modes as content hooks. There is no harm today.
- [x] **6.O2** `ItemDrops.hasUniqueRoll` (101-104) reads `RULES` directly instead of `activeRule`, so other-mod rules count while their drops are switched off. Only `UniqueSourceCheck`'s startup log uses it.

## 7. Skill tree module

### Abstraction

- [x] **7.A1 `ShipTrees.release` helper: release nodes, then whatever they strand, and collect the refunds** (M). Each of these snapshots `reachableAllocatedNodeIds`, deallocates, calls `releaseNewlyStranded`, gathers `ItemChargeLedger` charges and refunds them to cargo:
  - `NodeAllocator` (300-331, 409-413)
  - `LockAreasCommand` (80-104)
  - `DoctrineRevertRelease` (62-85)
  - `PhantomConflictWatch` (166-177)
  - `ShipTreeSync.dropNpcAnchors` (255-258)

  Only one of them is anchor-exit aware. Add `ReleasedNodes ShipTrees.release(data, topology, rootId, nodes)` with `ReleasedNodes.refundTo(cargo)`. Removes about 60 lines, and 7.B2 can use it. Tests exist for each caller.
- [x] **7.A2 `AllocationGate.forShip(data, facts, budget, heldItems)`** (S). Five places assemble ShipFacts, the eligibility context, a Budget and the gate: `NodeAllocator.snapshot/planningSnapshot` (229-256), `TemplateLevelUp` (57-77), `DormantNodeWake` (61-66), `DoctrineRevertRelease` (70-72) and `NpcSkillTreeBuilder` (445-468). Only `NodeAllocator` calls `DoctrineOverrides.bindShipFacts` first, so the doctrine layer in `TemplateLevelUp` and `DormantNodeWake` can run against stale facts. The factory should always bind. Removes about 40 lines and fixes that inconsistency.
- [x] **7.A3 One template auto-allocation** (S). `TemplateLevelUp` (53-78) loops `firstAllocatable` with a fresh gate per step. `ui/node/TemplateStepExecutor` with `skills/template/AutoAllocateRun` does deferred-retry passes. The retry rules differ, so a level-up and the "Auto allocate" button can pick different nodes from the same template. Have `TemplateLevelUp` call `AutoAllocateRun.run(...)`.
- [x] **7.A4 One `node[=option]` entry codec** (S). `NpcTreeTag` (38-52, 109-160) and `TemplateCodec` (21-25, 72-91) both split on `=`, apply `NodeReplacements.current` and `AffinityOptions.currentOptionId`, and format the same way. Add `record NodeEntry(nodeId, optionId)` with `format()` and `parse(raw)` that apply the migrations. This also fixes the root migration in 7.B3.
- [x] **7.A5 Small duplicates** (S each).
  - `RootConduits.connectorKey` and an inline copy in `HyperspaceRoute:27` duplicate `SkillTree.curveKey`.
  - `SkillTreeLoader.parseNodes` (114-127) repeats the skip-and-log loop of `forEachEntry` (57-70).
  - `HyperspaceGhostFlights` (38-48) and `node/SkillTreeWormholeGhostFlights` (61-75) each hand-roll a random-interval route launch scheduler. Add a `RouteFlightScheduler` inside `GhostFlightPool`.
- [x] **7.A6 (partly done)** **Fleet renderers as tree decorations** (S, after 8.A2). `FactionFleetRenderer` and `SkillTreeFleetRenderer` sit outside the `DecorationRenderer`/`TreeDecorations` framework, and `SkillTreeCanvasPlugin` advances and renders them one by one. `SkillTreeFleetRenderer` (113-131) repeats the render loop again for one fleet.

(The lane fleet traffic copy is in 8.A2, the planet sphere and spec lookup in 4.A3, and the icon FX helpers in 8.A7. This reviewer adds the `CacheIconFx`/`DoctrineIconFx` still-image fallback (`drawStill`) and the cross-class borrowing, where Cache calls `Doctrine.flash` and `Anchor.smooth`; both go into the same `IconFxKit`.)

### Bugs

- [x] **7.B1 Phantom hull mods of gated nodes swap between the two sync paths** (low, likely). Inside a Phoenix Ash `GATES_RADIUS_*` radius, the stats path (`SkillTreeEffectSource` 79-85, 134-167) drops gated contributions, so it leaves the node's phantom hull mods out of the wanted set. The public `ShipTreeSync.syncVariant` (114-122) filters only hollow nodes, so it wants them. Each memberChanged or refit edit adds the tagged perma mod, and the next `updateStats` removes it. `hasHullMod` checks and the refit list flicker. Fix: one helper for placing nodes (allocated, not hollow, not gated) used by both `syncVariant` overloads.
- [x] **7.B2 Save repair leaves stranded nodes allocated, and always forgets sockets replaced by optional nodes** (med, likely).
  - `ShipTrees.replaceNode` (446-449) always drops `optionalSelections[oldId]`. `node_replacements.csv` maps `modular_hull_socket_*` onto `small_flux_optional_*` nodes, so after the replacement `forgetUnknownNodes` (457-489) sees no option and forgets the node.
  - Forgetting, or removing an unknown mid-path node, only calls `release(nodeId)`, and nothing calls `releaseUnreachable`. The nodes behind it stay allocated and disconnected: they keep applying effects and counting OP.

  Fix: run `releaseUnreachable` and refund after forgetting. In `replaceNode`, keep the old option if the new type offers it, otherwise use `firstOption`.
- [x] **7.B3 NPC tree decode can leave disconnected nodes, and ignores renamed roots** (low, likely). `NpcTreeTag.restore` (132-160) skips an optional node with no option but still allocates every later entry, with no connectivity check. Recovering the ship hands those orphans to the player through `adoptNpcTree`. `decode:96` resolves the root without `NodeReplacements.current`, so a renamed root decodes to an empty tree. Fix: migrate the root and run `releaseUnreachable` at the end of `decode` (see 7.A4).

### Other

- [x] **7.O1** `NpcTreeBuild.strippedHullModIds` is built but production never reads it; stripping runs from `exclusiveHullModIds`. Decided: delete it as dead code.
- [x] **7.O2** `SkillTreeCanvasPlugin` (80-101) null-checks `member`, but `TreeAllocationSession:81`, `NodeAllocator` and `SkillTreeNodeDrawer:82` dereference it unguarded. Decided: drop the null checks.

The reviewer checked these and found no problem: NPC generation determinism, AllocationGate and snapshot caching (no per-frame rebuilds), `PhantomInstallSync` add and remove, the `DoctrineOverrides` weak maps, and the `RespecPlan` order.

## 8. Hull Upgrades, cross-module duplication and domain hook wiring

### Abstraction

- [x] **8.A1 Hull-upgrade loot through a `LootSource` site hook** (M). Core and charts both roll hull-upgrade loot directly. Core `items/ItemDrops` (212-385) holds `SectionItemLoot`, `rollSite(sectionsOpen)`, `rollMasterSchematic`, `rollSectionItemLoot`/`rollTargeted` (which also rolls RefitSchematic) and `pickSectionItemBasic`. `ItemSalvageListener:45`, `charts/objective/ChartRewards` (85-93), `charts/loot/ChartCargoLoot` and `ChartFleetLoot:232` call them. The rule "if sections are open roll section loot, always roll Master Schematic" is written three times: `HullUpgradeLootSource:108-112`, `ChartRewards:85-93` and `ItemDrops:263-268`. Do this:
  - Generalise `TechMiningItemListener.Find` into a core `loot.SiteFind(sector, dropSite, random, chances, items, upgrades)` and add `LootSource.addSiteFinds(SiteFind)`.
  - Move the hull-only rolls into a hull-upgrades `HullItemDrops`.
  - Drop the `sectionsOpen` parameter from `ItemDrops.rollSite`.
  - Have charts call `LootSources.all().forEach(s -> s.addSiteFinds(find))`.

  About 150 lines leave core. Risk: seeded rolls change order. Saves are safe, because the classes stay in package `exiledsector.items`.
- [x] **8.A2 `FleetTraffic` base for lane fleets** (M). Skill-tree `ui/decoration/FactionFleetRenderer` re-implements core `ui/decoration/LaneFleetTraffic`: `advance()` and `render()` are line for line identical, spawn and `startBattle` are near identical. Extract a core `FleetTraffic` holding the sprite cache, fleets, battles, respawns and random, with `advance`, `render`, `spawnFleet` and `startBattle(fleet, eligibleEnemy)`. It has `respawn`/`arrive` hooks, and the tree's faction lookups reuse `LaneFleetTraffic.Factions`. This also fixes a drift: the tree counts Nexerelin alliances as allied, the atlas lanes do not. Removes about 90 lines. UI only.
- [x] **8.A3 `MarketStock.replace`** (S). `HullUpgradeLootSource` (122-134) with `MarketHullItems.isStock`, and `SkillTreeLootSource` (168-183), each purge and re-add their market stock. Add a core `MarketStock.replace(cargo, ownStock, fresh)` that always lists with `ItemData.toMarketListing()`, and split purge (a state step that runs for every domain) from adding (a content hook). This fixes 8.B2 and 8.B3.
- [x] **8.A4 (partly done)** **Refit full-screen button template** (S).
  - `HullUpgradesRefitButton`, `ModificationBenchRefitButton` and `SkillTreeRefitButton` share one open/init shape, and two of them write their label key twice.
  - Add `createScreen(member, variant, button)` to `FullScreenRefitButton` and give it a default `initPanel`.
  - Add a static `openScreen(Class, event)` and a `columnButton(id, hotkey, press)` built from `buttonKey()`.

  Removes about 30 lines.
- [x] **8.A5 `RandomPlayerFleetCommand` and `ConsoleArgs.copiesOrComplain`** (S). Eight console commands repeat the `this(new Random())` constructor plus a test constructor: `GrantHullItems`, `GrantUniqueHullItems`, `GrantDoctrineArchives`, `GrantModspecCache`, `GrantWormholeAnchors`, `GrantStarCharts`, `ChartOpen`, `ChartOpenStored`. `GrantMasterSchematicsCommand` and `GrantRefitSchematicsCommand` both repeat the copies-count parse and its error message. Merge with 4.A5 (`ConsoleArgs.tokens`). Removes about 50 lines.
- [x] **8.A6 `HullUpgradePanel` currency tile** (S). `schematicCell` (296-309) and the Master Schematic cell (316-324) are near identical, and so are the two confirm openers (189-209). Add `currencyCell(...)` and `openCurrencyConfirm(...)`.
- [x] **8.A7 Icon FX timing and colour helpers** (S). `cycleOf` is copied in `AnchorIconFx`, `CacheIconFx`, `DoctrineIconFx` and `CatalystIconFx`; `smooth` in `AnchorIconFx`, `CatalystIconFx` and `HyperspaceTransition`; `lighten`/`mix` in four classes. Add `Fade.cycle(seconds, period)` and `Fade.window(from, to, value)` to core `render.Fade`, and use `Misc.interpolateColor` for the colour helpers. The skill-tree reviewer also proposes an `IconFxKit` holding `drawStill` (the still-image fallback in Cache and Doctrine) and `flash`/`wave`/`drawFramedBorder`, so `CacheIconFx` stops borrowing from `DoctrineIconFx` and `AnchorIconFx`. Removes about 30 to 60 lines.

(The planet sphere duplication this reviewer also found is merged into 4.A3.)

### Bugs

- [x] **8.B1 Ship summary under-reports per-S-mod affinity** (med, confirmed). `ui/section/SectionShipSummary:28` calls `HullUpgradeEffectSource.contributions(sections, hullSize)`, which passes `sModCount` 0. A hull item with `HIGHEST_AFFINITY_FLAT_PER_S_MOD` therefore adds nothing in `ShipTreeSummaryRenderer.partContributions`, so the summary tooltip's affinity totals and steps are lower than the ship really has. The real stats use `variant.getSMods().size()`. Pass the variant's S-mod count (or go through `contributions(ShipEffectContext)`) and drop the two-argument overload.
- [x] **8.B2 Restock deletes hull items the player sold to a submarket** (low-med, likely). `HullUpgradeLootSource:123-132` adds stock as plain hull-item specials. On each restock it removes every non-unique `HULL_ITEM` special in that cargo, including items the player sold there. Wormhole anchors avoid this with `toMarketListing`. Fixed by 8.A3.
- [x] **8.B3 A switched-off domain's market stock stays for sale forever** (low, confirmed). The stock purge lives inside the content hook (`HullUpgradeLootSource:116-134`, `SkillTreeLootSource:168-183`, called from `MarketItemStock:50` for enabled domains only). Once Hull Upgrades or the Skill Tree is switched off, its last batch stays buyable. This breaks the CLAUDE.md rule that cleanup after a switch-off belongs to state hooks. Fixed by 8.A3.

### Domain hook wiring

All content hooks use the enabled-only path. All state hooks run for every domain, and spot checks show they check the switch inside. The places where core or charts reach into a domain directly:

- [x] **8.H1** Hull-upgrade loot is rolled by core and charts (8.A1).
- [x] **8.H2** `ExiledSector.install` (core `ExiledSector:36-37`) hard-wires the RefitSchematic and MasterSchematic stack absorbers. Register them from `HullUpgradesDomain` instead, still unconditionally.
- [x] **8.H3** The market purge sits in a content hook (8.B3).

### Other

- [x] **8.O1 Dead code** (only tests call these): `SectionLayout.Arrangement.BOTH_SIDES`/`placeBothSides`, `Placement.leftSide()`, `SectionsDisplay.SectionBounds.UNBOUNDED`, `SectionsDisplay.forgetRenderedSections()`, `HullUpgradePanel.Sizing.FIT_HEIGHT`/`tileSizes(...)`, and the `BattleItemDrops.roll/plan` overloads without `sectionsOpen`.
- [x] **8.O2** `GrantUniqueHullItemsCommand` grants every unique item, subroutines included, using only core classes. Move it to core `console`.
- [x] **8.O3 Stale doc**. docs.md "Hull item mechanics" names `Item.rerollTarget`; the method is now `Item.retuneTrait`.
- [x] **8.O4** `HullUpgradePoints.chartsEnabled()` finds the charts domain by scanning item-type supports. Add a core `ExiledSector.domainEnabledFor(ItemType)`.
- [x] **8.O5** `BattleItemDrops.DATA_PATH` and `MarketHullItems.DATA_PATH` hard-code `data/config/exiledSector/...` instead of using `ExiledSector.configPath(...)`.

## 9. Shared core helpers proposed by more than one reviewer

Build each of these once in core and switch every site over in one pass. The items they come from are listed after each one.

- [x] **9.1 `PlanetSphere.draw` and `PlanetSpecs.byType`**. Used by the atlas preview, the skill-tree star renderer and the chart generator (4.A3).
- [x] **9.2 `SpriteDraw.drawCellAtCenter` / sprite-sheet cells**. Used by the atlas art, the storm overlay and terrain, the debris terrain and the catalyst icon (4.A4, 1.1.A8).
- [x] **9.3 `TileFrame` / `TileChrome`, `SectionedTileGrid`, `TileInput.Handler.of`**. Used by the bench, storage, catalyst and icon tiles (4.A2, 6.A7).
- [x] **9.4 `ConsoleArgs` (`tokens`, `copiesOrComplain`), `RandomPlayerFleetCommand`, `ChartConsoleText.openFailure`**. Used by every module's console package (4.A5, 4.A6, 8.A5).
- [x] **9.5 `EnumKeys.parse/key`**. About 17 parse-by-key methods across charts, atlas and anomalies (3.A9, 2.A7).
- [x] **9.6 `WeightedPick.pick(Map, Random)`**. Layout, abyss, sketcher, atlas decorations and storm strikes (3.A4).
- [x] **9.7 `SeedHash` and `AtlasNoise`**. Atlas, layout and decorations; must stay bit-identical (3.A5).
- [x] **9.8 `FleetTraffic`**. Skill-tree lanes and the atlas lanes (8.A2, 7.A6).
- [x] **9.9 `Fade.cycle/window` and `IconFxKit`**. The anchor, cache, doctrine and catalyst icon FX (8.A7).
- [x] **9.10 `DomainContributions.first/firstOr`**. Loot sources, icon looks, ship records and bench modes (6.A5).
- [x] **9.11 `ChartRuns.live*`**. Charts and every anomaly (2.A1, 1.1.A1).
- [x] **9.12 (partly done)** **`MaterialItems.localise(itemId, keyPrefix, ...)` and `ItemStore.ownedStarCharts()`** (4.A8, 4.A9).
- [x] **9.13 `MarketStock.replace`**. Hull upgrades and skill-tree market stock (8.A3).
- [x] **9.14 `AnomalyDialog`**. Also used by `GatewayUpgradeDialog`, `ChartWormholeDialog`, `AtlasAbilityDialog` and skill-tree `NpcFleetInspectPlugin` (1.1.A3).

