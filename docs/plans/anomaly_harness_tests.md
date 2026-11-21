# Anomaly harness tests

Status: planned. Nothing is built yet.

The goal is a live harness scenario for every Star Chart anomaly, and one for every situation an atlas tree node creates for those anomalies. Unit tests already cover the maths. These scenarios cover what only the running game shows: placement, dialogs, timers, fleets, battles, loot and the atlas aftermath.

The harness (`tools/harness/`, code in `exiledsector-mod` under `exiledsector.harness`) runs one scenario per game launch against a copy of a save. Today it can open a free chart beside the player, pass days, despawn the boss and leave the run. It cannot apply atlas tree nodes, open from the atlas, drive anomaly dialogs or fight a campaign battle.

The scenarios are not 66 separate launches. They are cases grouped by anomaly, and the whole suite runs in one game session without restarting the game. A single anomaly's group, or a single case, can also be run on its own (see Suites and groups).

## What gets a scenario

All 22 anomalies in `chart_anomalies.csv` get a base scenario. Eleven have no atlas nodes of their own, so the base scenario is their whole test: holdouts, abyssal_storm, pre_collapse_facility, derelict_forge, abyssal_research_complex, coronal_hypershunt, abyssal_shipyard, remnant_nexus_station, domain_mothership, black_hole (apart from event_horizon) and neutron_star.

Atlas nodes split in two.

- Number nodes change a chance, count, size, quantity or roll: every "chance" small, the loot and quantity smalls, additional_site, crowded_channel, double_down, scar_collector, early_echo, garrison_pickets, stolen_charts, deep_corruption, lone_sleeper, the three sleeper swaps, supercell, lightning_rod, charged_fields, static_harvest, solar_maximum, tempered_light, golden_hour, perfect_light, mixed_manifest, hardened_command, open_warfare and the duplication smalls. Each anomaly gets one value scenario with all its number nodes on. It records what the run actually sees (`ChartRunEffects.contextOf(run).anomalyPercent(key)`) and the counts the run generated. One run per anomaly replaces dozens of single-node runs.
- Scenario nodes change what happens. There are 44, listed by anomaly below. Each gets its own scenario, or a case inside the anomaly's scenario when the cases don't interfere.

Thirteen scenario nodes need a second anomaly in the same run: bolt_hole_drop, sunstruck_convoy, torn_holds, infested_bolt_holes, settled_shafts, rude_awakening, breached_graveyard, ion_front, geomagnetic_surge, captured_front, solar_front, ionized_dust and event_horizon. Forced anomalies skip `fitsAt` and `holdsAt`, so each combination is picked deliberately (stellar_flares with dense_nebula throws "no star").

## Two blockers

### Atlas tree effects never reach a harness run

Only `ChartRunActions.openFromAtlas` passes `AtlasTree.chartLines(state)` into a run. It reaches two places: the generation request (counts, sizes, placement) and `run.recordAtlasTreeLines` (scripts, dialogs, battle plugins read it through `ChartRunEffects.contextOf`). The harness `openChart` step and `ExiledSectorChartOpen` both use the free `openBesidePlayer`, which records no lines and no atlas node.

Two paths fix this, and both get built.

- Injection, for most scenarios. Build the request as `ChartGenContext.none().withAnomalies(ids).withEffects(lines).withBiome(region).withAnomalyData(id, data)`, open with the public `openBesidePlayer(sector, entries, random, request)`, and record the same lines on the run straight away. Lines come from node type ids through `AtlasTreeData`, so scenarios name nodes, not modifiers.
- The real atlas, for what only an atlas opening does: opening-chance rolls, Relay stages and forks, the Depot lane reveal and Officer's Ledger, Faction Skirmish, the Holdouts intel line, officer runs and every atlas aftermath check.

A small production change keeps injection honest: a public `openBesidePlayer` overload that takes the atlas node, tree lines and atlas map effects in one call, as the private eight-argument method does. Without it the generation and runtime contexts can drift apart.

### No campaign battle

`defeatChartBoss` despawns the boss as destroyed by battle. That completes the objective outside battle and skips battle loot, `StarChartLootSource.recordWin` (Settling Dust stacks), `ChartObjectives.bossFellInBattle`, `ChartAnomalies.battleFor` and every chart combat plugin (`ChartCombatPlugins.isCampaignBattle` rules out the simulator).

Battles get two levels.

- Auto-resolve: `BattleAutoresolverPluginImpl` on a battle between the player and a chosen fleet. It runs `reportPlayerEngagement` and `reportBattleFinished`, so loot reaches cargo, dust stacks rise and objectives settle. It builds no combat engine, so anomaly battle creation plugins and combat plugins don't run, and it never shows the loot screen.
- Real combat: open the encounter, pick engage, get past deployment, force the outcome, then pursue or let go, the loot screen, recovery and leave. Needed for the excavation cavern, needle/widened bores reserves, pitch black sight, breach lightning, storm strikes, sunstruck CR and station fights. The test install runs Nexerelin, which replaces `FleetInteractionDialogPluginImpl`, so option ids are resolved from the live plugin class as `TitleNewGameDriver` already does.

The outcome is forced from inside combat. A win kills every enemy ship the way Console Commands' Nuke does (`Kill.killShip`: hitpoints 1, armour cell zeroed, a huge OTHER damage hit), so kills count for loot per DP and for objectives. A loss does the same to the player's ships. `engine.endCombat(delay, side)` is the fallback for a named winner with no kills.

## Suites and groups

A case is one test: a base scenario, a value scenario or a node scenario. A group holds every case for one anomaly. A suite is a list of groups. The anomaly sections below are the groups, and each line in them is a case.

### One game session

One launch loads the save once and runs every selected case in order. Between cases the game is not restarted; instead each case starts from the same baseline.

- After the save loads, the runner captures the baseline. It records the player fleet's location, position, members, CR and cargo (commodities, crafting materials and items), the atlas state (points, allocation, loadouts, anomaly records, catalysts, officers, intents, cell flags, seed), the star chart store and the sector's harness-relevant memory.
- Each case gets its own seed. The chart seed, run id random and atlas reset all come from it, so a case gives the same result whichever cases ran before it.
- After each case, teardown runs whether the case passed or failed. It closes any open chart run and waits until `ChartRuns.unclosed(runId)` is null, closes any open dialog or core UI, ends any combat, returns the player fleet to its baseline location, and restores fleet, cargo, atlas state and the chart store from the baseline.
- Then a leak check compares `ChartCensus.take()` with the baseline (`changesSince`). A leak fails that case, not the suite. Star Chart runs are meant to leave no trace in the save, so this also tests that.
- If teardown itself fails, the session can't be trusted. The runner marks the remaining cases as not run, writes results and quits, and the script relaunches the game from the save and carries on from the next case. A restart only happens after a broken teardown.
- The sector clock keeps moving across cases. Every anomaly timer counts from its run's own start, so that is safe; any case that depends on the date sets up its own.

### Grouping inside a group

- Cases in a group run cheapest first: value scenario, then state and dialog cases, then battle cases, then atlas cases.
- Cases that don't interfere can share one opened run. The value scenario and the plain state checks of an anomaly usually fit in one run; a case that changes the run (an alarm, a corruption, a collapse) gets its own.
- A cross-anomaly node case belongs to the group of the anomaly whose tree branch holds the node (bolt_hole_drop sits under Supply Convoy) and is tagged with the other anomaly too. Selecting by tag picks it up when either anomaly is tested.
- Groups whose anomaly needs a missing mod are skipped as a whole and reported as skipped.

### Running a subset

`run-harness.ps1` takes a suite file and filters:

- `-Group <anomaly>` runs one anomaly's group.
- `-Case <id>` runs one case, with its group's shared setup.
- `-Tag <anomaly>` runs every case tagged with that anomaly, across groups.
- No filter runs the whole suite.

Results go into one `results.json`, with an entry per group and per case (steps, details, recorded values, leak check, teardown). The script prints one PASS, FAIL or SKIP line per case and a total per group.

## Harness capabilities

Built in order; later ones lean on earlier ones.

### A. Foundations

1. `openChart` parameters: `anomalies`, `tier`, `region`, `layout`, `seed`, `anomalyData` and `atlasNodes` (node ids). It fails when a forced anomaly isn't in `run.anomalyIds()`, because conflicting, mod-gated and unshaped anomalies are dropped silently. A scenario can mark an anomaly as needing a mod (AotD Vaults of Knowledge for the facility, RAT for the shipyard, research complex and abyssal storm) so it skips instead of failing. Built: `OpenChartStep.Options`, through the `ChartRunActions.openBesidePlayer` overload that takes tree lines and `AtlasTree.chartLinesOf`; mods are named per group (`requiresMods`).
2. Log scan: every scenario ends by checking `starsector.log` for `ChartAnomalies` first-failure lines naming its anomalies, since `askSafely` swallows hook exceptions. Built: every suite case ends with `expectNoErrors`, and teardown calls `ChartAnomalies.forgetHookFailures` so a repeat failure in a later case is logged again.
3. Checks:
   - `expectAnomalyState` (fields of `AnomalyState.of(run, id)`, polled because staged state is committed later)
   - `expectContext` (tuning values the run sees)
   - `expectEntities` (count and memory of entities by tag or memory key)
   - `expectFleet` (fleet memory, fleet points, member count, assignments, stat mods by id, captains' AI cores)
   - `cargoSnapshot` and `expectCargo`, covering crafting materials and item details (warped, lucky, immutable, biome modifiers), not only commodities
   - `recordState`, which writes any of the above into `results.json` for the value scenarios
4. Setup steps: grant crafting materials, fully survey a planet, toggle the transponder, activate an ability (sensor burst, interdiction pulse), set anomaly state directly (Settling Dust stacks, Depot alarm through `PostScript.raiseAlarm`).
5. Fleet movement: teleport beside an entity or point (extends `HarnessCampaign.movePlayerFleetBeside`), hold still, follow a moving terrain (storm front, drifting breach), and fly to an entity to interact with it the way a click does. `passDays` gains an option to allow engagements; today `CampaignTimeSkip` sets no-engaging every iteration.
6. Dialog driver, extracted from `TitleNewGameDriver.offeredOptions`: open a dialog on an entity through the normal plugin pick, list options with enabled state and tooltips, pick by option data or visible text, re-read `dialog.getPlugin()` after every pick (hand-offs swap it), and close the loot screen. Package-private choice records (relay offers, prototype picks) are picked by index or text.
7. Auto-resolve battle against a fleet found by tag, memory key or side.
8. Suite runner, as described in Suites and groups: suite files with groups, cases and tags; baseline capture, per-case seeds, teardown and restore, the leak check, relaunch after a broken teardown, and the `-Group`, `-Case` and `-Tag` filters. Built: `HarnessSuite`, `SuiteCase`, `SuiteSteps`, `HarnessBaseline` and `PersistentSnapshot` in `exiledsector.harness`, and `run-harness.ps1` (`-Group`, `-Case`, `-Tag`, `-StartAfter`, `-MaxRelaunches`).

### B. Real battles

1. `engageFleet`: open the encounter through the plugin pick (anomaly dialogs wrap their own `AnomalyEncounters.isolated` config), pick engage or continue into battle.
2. Deployment: press the deployment dialog's button through `UiTree`, or deploy through the fleet manager and suppress the dialog.
3. `forceBattleOutcome`: win or lose as described above, then wait for the return to the campaign. The existing combat steps (autopilot, speed, damage tally, stats) already work in real combat.
4. Post-battle driver: pursue or let go, continue to loot, close the loot screen, handle recovery, leave. It records which pipeline stages ran.
5. `killBossInBattle`: the boss through a real or auto-resolved battle, for scenarios that need the in-battle boss path. Twin and station bosses take more than one engagement.
6. Combat checks: map bounds, engine custom data (`exiledSector_shaftCavern`), deployed ships by hull size, ship stat mods by id (sight radius), CR, and the existing damage tally for arcs.

### C. Atlas

1. Atlas setup: reset the atlas with a seed (`AtlasDevTools.reset`), grant points, allocate a node path (`AtlasTree.allocatePath`, which drops unconnected ids silently), grant a star chart into the store, reach a cell, insert an anomaly (with a chain stage for Relay), then `openFromAtlas` and enter. Nodes are allocated before opening, since a run snapshots its lines at open. Cells revealed earlier keep their old atlas map effects, so the reset comes first.
2. Officer board setup: an active officer at a command centre with carried items, declared push or challenge intents (`AtlasState.setDeclaredIntents`), forced turns (`OfficerTurns.take` with a forced cause).
3. Atlas aftermath checks after leaving a run: anomaly records and chain stage at cells (`RelayStages.recordAt`), territory owners, officer scars, d-mods, carried items, renown and defeats, declared and decided intents, cell flags (complete, failed, anomalies spent), revealed lanes.
4. Several runs in one scenario: open, play, leave, open the onward cell.

### D. Raids and statistics

1. Ground raid driver for `ChartRaid`: marines in cargo, the valuables option, the vanilla raid target picker (driven, or bypassed through its delegate), confirm through to the result.
2. Statistical runs for random atlas turns: many forced turns with fixed seeds, with and without the node, comparing counts.

## Scenarios by anomaly

Each section is one group, and each line is a case: what it checks and the capabilities it needs. Every group also has a value case for its number nodes, run first.

### Excavation Sites

- Base, open shaft: fully surveyed site planet, the excavation option, descend, real cavern fight, win. Site cleared, inhibitors in cargo, cavern custom data set, no oversize ships deployed. A1, A3, A4, A6, B1–B4, B6.
- Base, collapsed shaft: raid with marines. Inhibitor count against the raid share, collapsed shaft cleared. D1.
- twin_shafts: both options on one planet; state goes from both shafts to one to cleared. Base cases combined.
- deeper_bores: after the first win the dialog offers deeper and surface; the second cavern fleet carries `$exiledSector_excavationSecondCavern` and its larger size; surface closes the shaft. A6, B.
- needle_bores and widened_bores: descend disabled with its tooltip without frigates (or cruisers); guardians and deployed ships within the size limit. A6, B6.
- pitch_black: every ship in the cavern has the `exiledSector_shaftDarkness` sight mod at −70%. B6.
- infested_bolt_holes: with anomalous_debris and one Corrupted Nanoswarm, the corrupt option spends it; fleet carries `$exiledSector_excavationCorrupted`; one engram drops. A4, A6, B.
- settled_shafts: with dust stacks set, inhibitor quantity rises per stack. A4, value scenario.

### Settling Dust (dense_nebula)

- Base: dense_nebula run, N auto-resolved wins against chart fleets. Stacks equal N up to the cap, boss star chart percent and intel line follow. A7.
- long_settling: other hostile run fleets grow by 3% of start fleet points per stack after each win; the boss and held guardians don't. A3, A7.
- ionized_dust: stacks rise in a magnetic_storm or storm_front run without a nebula only with the node. A7.

### Supply Convoy

- Base, delivery: pass days until convoys deliver; destination planet opens the cache dialog; raid it. Phases advance, cache taken, cargo rises. A5, A6, D1.
- Base, attack: attack a convoy through its dialog and win. Pods appear and never expire, convoy lost, intel updates. A6, B (or A7).
- Base, interdiction: a pulse in range turns the convoy hostile and it flees. A4.
- open_season: convoys hostile from spawn; with engagements allowed they intercept. A5.
- bolt_hole_drop: with excavation_sites, convoys route to standing sites; delivery fills the planet's delivery keys and joins escorts to the guardians; no cache dialog. A3, A5.
- sunstruck_convoy: with stellar_flares, the first waypoint is the star; engaging drops convoy CR by 20% once. B6.
- supply_lines: defeating a convoy hands its feedstock to an active atlas officer. C2, C3.
- torn_holds: with spatial_breach, pods dropped while the player is in the breach carry more feedstock. A5, B.

### Holdouts

- Base: forced with anomaly data naming the holdout faction. Patrol counts and sizes, hostility to the occupier, the boss ignored, restraint below twice the strength. Through the atlas path (an `ExiledSectorAtlasClaim` cell) the intel line shows. A1, A3, C1.

### Relay Network

- Base: wake each relay (nav buoy slow and close for 0.25 days, comm relay with transponder on, sensor array during a sensor burst) and pay to restore power; pick an offer per relay; boss scars match; boss loot holds the picked groups. A3–A6.
- Chain: atlas run at stage 1 with all relays woken, leave; the record moves onward at stage 2 with the stage fleet and loot bonuses; not all woken ends the chain. C1, C3, C4.
- forked_trace: a completed stage moves on to two cells, each at the next stage. C4.
- extra_relay: a fourth relay; the chain only moves after it wakes. A3.
- shared_broadcast: each pick also goes to an active atlas officer. C2, C3.
- full_array: once every relay wakes, every other fleet is revealed. A3.

### Depot (listening_post)

- Base, unseen: transponder off, infiltrate, open, pick a prototype, loot. Lane reveal on an atlas run. A4, A6, C3.
- Base, alarm: raise the alarm; vault share purges to its floor; pickets gather and merge into one guard; engage it, or remove it and open the purged vault. A4, A5, B.
- hair_trigger: identification raises the alarm on the next check, with no report timer; an unseen vault offers five prototypes. A5.
- smash_and_grab: the vault only opens through a ground raid; one more prototype pick. D1.
- hunting_party: on alarm, pickets carry the hunting flag and intercept; picket battles drop prototypes. A5, B.
- fortified_depot: the armed station exists, the vault doesn't purge while it stands, engage targets it, destroying it resumes the purge. B.
- officers_ledger: an atlas run inside an officer's territory; the vault duplicates one of that officer's carried rewards. C1, C2.

### Faction Skirmish

Only reachable through a declared push or challenge between atlas officers, so every case uses C1 and C2.

- Attacker wins, defender wins, the player beats both (dropped), the player beats one and the rival takes the other, left undecided. Each checks the result, territory, renown and intents after leaving. Fleets found by `$exiledSector_chartSkirmishSide`; a side's loss is a despawn as destroyed by battle, a player kill needs B. C3.
- spoils_of_war: one extra item when the player beats an officer, and both officer fleets 30% larger. B.

### Spatial Breach and Event Horizon

- Base: hold the fleet on the core until it opens; radius grows, caches form and dissolve, the supply buff and fuel drain apply inside, other fleets get the charge assignment, a cache salvaged inside is warped, everything is gone after collapse. A3, A5, A6.
- heavy_breach: holds at full radius for 30 seconds; drain rises while held. A5.
- rift_magnetism: the breach moves towards an off-centre player at about 40 su per second. A5.
- hyperspace_bleed: a real battle inside the breach; arcs every 6 seconds in the damage tally. B.
- incursion: the research station stays unclickable until its guard is gone, can then be salvaged and outlasts the collapse; left guarded, both vanish. A3, A6.
- rude_awakening: with anomalous_debris, dormant fleets wake and charge the breach; a debris fleet's battle drops inside are all warped. A3, B.
- rift_duel: an officer run; the boss dies while the player is inside; two carried rewards duplicated. C1, C2, B5.
- event_horizon: black_hole run; the terrain exists at the right radius; every item salvaged from a derelict is warped. A3, A6.

### Anomalous Debris

- Base: the fleet is hidden until the player is in reveal range; corrupting through the dialog spends a nanoswarm and grows the fleet; a won battle drops engrams once. A3–A7.
- double_dose: the corrupt twice option costs two and gives two corruptions. A6.
- breached_graveyard: with spatial_breach, a debris field sits on the breach core. A3.
- restless_sleepers: fleets start woken, hostile and corrupted once; clicking goes straight to the fight. A3, A6.
- silent_choir: every fleet is corrupted twice; the dialog shows only engage and leave. A6.

### Storm Front

- Base: the front's sensor penalty applies inside; it crosses the system in about 30 days; a real battle inside shows strikes, more loot and biome modifiers on hull items. A5, B.
- eye_of_the_storm: the front travels slower while the player follows it inside. A5.
- redacted_front: interacting with a hostile fleet inside the front adds 5–10 Remnant frigates to it once. A5, A6.
- captured_front: with black_hole, the front orbits the hole. A3.
- solar_front: with stellar_flares, the front is solar and battle drops roll lucky biome modifiers. B.
- thunder_duel: an officer run; the boss dies while the player is in the front; one extra officer reward. C1, C2, B5.

### Magnetic Storm

- Base: the magnetic fields exist and the crafting item bonus is in the run context. A3.
- ion_front: with storm_front, where the front crosses a field an aurora fires towards it; battles inside the front get more crafting items. A5, B.
- geomagnetic_surge: with stellar_flares, an active corona flare facing a field sets off its aurora; drops get one more lucky modifier. A5.

### Abyssal Storm

- Base: abyssal_rift region; across more than 45 seconds of campaign time, fleets outside fog get the detected range mod during storms, fleets in fog don't. Needs RAT. A1, A3, A5.

### Stellar Flares

- Base: every corona's flare probability is 1; a hull item drop in the run carries the expected luck. Number nodes go in the value scenario, including the `FlareTempo` script for solar_maximum. A3.

### Site anomalies

Each base scenario finds the site, fights or bypasses its guards, takes the reward and checks the loot. Real guard fights need B; a guard bypass (set `$defenderFleetDefeated`, or a small package hook for the facility stage) keeps the reward check cheap.

- pre_collapse_facility (AotD Vaults of Knowledge): guarded, open, salvaged; databank drops.
- derelict_forge: programs offered, a program run through `ChartForge.run` with granted feedstock spends it and changes the item; runs left falls.
- abyssal_research_complex (RAT): the infuse program in the abyss.
- coronal_hypershunt: blue supergiant and coronal tap; Omega guards beaten; Omega weapon drops.
- abyssal_shipyard (RAT): salvaged once, then stripped.
- remnant_nexus_station: launches fleets only within 1 ly of the player; hull items from the station fleet are overcharged (B).
- domain_mothership: launches derelict patrols; losses counted.
- neutron_star: to be surveyed before its scenario is written.

### Atlas officers

- Base: an officer run dresses the boss (officer memory keys, flagship, levels); defeating it adds a scar and d-mod and drops carried rewards; losing to it takes a victorious turn once per run (B). C1, C2, C3.
- iron_grip: the defeated officer keeps all but one reward group. C3.
- trophy_refit and scrappers_fortune: statistical forced turns with and without the node. D2.

## Build order

1. A1–A8, with the suite runner and teardown built first so every later case is written as part of a group. Then every base scenario that needs no real battle and every value scenario. About 12 new steps and a suite file per anomaly. Also covers about 22 scenario nodes: twin_shafts state, the deeper_bores dialog, double_dose, silent_choir, restless_sleepers, breached_graveyard, extra_relay, full_array, hair_trigger, fortified_depot purge hold, heavy_breach, rift_magnetism, incursion, rude_awakening wake-up, eye_of_the_storm, redacted_front, captured_front, open_season, bolt_hole_drop, the sunstruck route, ion_front and geomagnetic_surge auroras, long_settling, ionized_dust and event_horizon.
2. B, then the scenarios that need real combat: cavern fights (needle_bores, widened_bores, pitch_black, infested_bolt_holes), hyperspace_bleed, storm strikes, sunstruck CR, solar_front loot, Depot engage and the armed station, hunting_party, convoy attack and pods, engram drops, the Nexus overcharge and real guard fights. The largest step and the likeliest to stall.
3. C, then the atlas scenarios: the Relay chain and forked_trace, shared_broadcast, officers_ledger, supply_lines, the Depot lane reveal, every Skirmish outcome and spoils_of_war, the officer base scenario, iron_grip, rift_duel, thunder_duel, opening-chance nodes and dense_nebula_atlas_chance.
4. D, then the raid scenarios (collapsed shaft, convoy cache, smash_and_grab, the collapsed half of twin_shafts) and the statistical officer turns.

## Risks

- Randomness: battle loot (`ItemLootListener` uses its own unseeded random), combat AI, lightning, storm strikes, auroras and the abyssal storm cycle aren't seeded. Those checks use ranges or presence; only generation output is compared exactly. An injectable random would make them exact, but it's an optional production change.
- Timing: the breach runs in campaign seconds, convoys take more than 5 days plus travel, the storm front 30 days and may never reach the planets. Fast-forward changes frame sizes, so timed checks allow slack.
- Atlas side effects: an atlas node faked onto a free run makes run close touch a cell that doesn't match the atlas, so atlas cases use the real path and start from the restored atlas baseline.
- Real combat UI: the deployment, loot and recovery screens are obfuscated widgets, and Nexerelin changes the encounter dialog.
- Isolation: anything teardown doesn't restore leaks into later cases. The baseline covers what the cases touch, and the leak check catches chart content left behind, but other state (faction relations changed by a fight, intel added, sector memory flags) needs adding to the baseline as cases start touching it. A case that passes alone but fails in the full suite points at a gap in teardown.
- Run time: one session for about 66 cases is long, so the cases in a group share runs wherever they can, and the filters keep a single anomaly's run short.

## Side findings

- `docs/plans/completed/anomalies/relay_network.md` still promises a guaranteed last-stage unique; the code has none, only early_echo's chance.
- Some modifiers have no node or data row setting them: `CHART_FORGE_RUNS`, `CHART_FORGE_COST_PERCENT`, `CHART_RELAY_COST_PERCENT`, `CHART_RELAY_CHAIN_STAGES`, `CHART_LISTENING_POST_PICKETS`, `CHART_BREACH_GROWTH_PERCENT`.
- The Event Horizon terrain only draws and shows a tooltip; breach effects ignore it, and only the warped derelicts change play.

## Open decisions

- Injection plus the `openBesidePlayer` overload for most scenarios, with the real atlas only where it's needed (recommended), or the real atlas everywhere.
- Number nodes covered by one value scenario per anomaly (recommended), or left to unit tests.
- Whether small production changes for testability are allowed: the overload, public dialog option ids, an injectable loot random.
- Where a scenario only needs loot, auto-resolve (recommended) or real combat.
