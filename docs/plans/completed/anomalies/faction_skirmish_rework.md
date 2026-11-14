# Faction Skirmish rework: officer battles

Faction Skirmish stops being a side-picking anomaly and becomes the battlefield of the officer fights the atlas turns already declare. Every push or challenge an officer declares is fought in a system of the defender's territory, both officers bring their own flagship fleets, and the player meets both as enemies. There is no reward of its own: what the player gains comes from beating the officers.

Status: built 2026-11-14, the two proposals as written. Numbers are placeholders. It replaces the Faction Skirmish officer training entry of `anomaly_rewards.md` and the out-of-date first Faction Skirmish plan, since deleted.

## What exists already

Skirmishes no longer roll on the atlas (`FactionSkirmish.atlasWeight` is 0). After every officer turn, `SkirmishRecords` puts a world record on each declared fight's cell: a push's target cell, or for a challenge the defender's free claim nearest the challenger (`SkirmishContests.challengeCell`). A finished run settles the fight through `ContestOutcomes`, and an unfought fight is rolled at the next turn (`OfficerTurn.resolveDeclared`). This rework keeps that skeleton.

## The battle

- Location: unchanged for pushes and different-faction challenges. Same-faction challenges get a cell the same way (the defender's free claim nearest the challenger) instead of always rolling.
- The defending officer's flagship fleet, built as their command centre boss fleet is (`OfficerFleets.dress`, the rank's boss size and Fleet Strength), holds around the system's station with the system's patrols. The station stays, as part of the defence.
- The attacking officer's flagship fleet arrives built the same way at the attacker's own rank, with escorts at that rank's size; it no longer copies the defender's fleets. There is no larger or smaller side: each is sized by its officer's rank, and `SkirmishPlanner.ALLY_COMBAT_POINTS_DIVISOR` goes.
- Both sides fly run-only copies of their factions, one for the attacker and one for the defender, hostile to each other and to the player, carrying the real faction's name and crest, and never listed as factions of their own in intel, the faction screen or Nexerelin. Same-faction challenges therefore fight like any other. The copies reuse the run faction dress the convoy and the old ally side use (`RunFactionDress`, `exiledSector_convoy`).
- Both officers' flagships are always marked: revealed on the system map and radar for the whole run, and both officers named in the atlas tooltip and run intel.
- The player never picks a side. Fighting either side with the transponder on costs reputation with the real faction behind the copy, as fighting any non-hostile chart fleet does (`ChartTransponderReputation`, mapping a copy back to its real faction).

## Finishing the run and the fight

- The run completes when both officers' flagships are destroyed.
- A flagship the player destroys counts as beating that officer, with everything that brings today: the scar, their carried items, the three reward group offers and the turn it triggers (`OfficerDefeat`, `OfficerTurns`). The turn engine handles two officers beaten in one run.
- Proposal: a flagship destroyed by the other officer's side still counts toward completing the run but is not beaten by the player; it loses the fight to the other officer as an abstract loss does (`ChallengeLossScars`, renown and rank rules).
- Proposal, the fight's result: the attacker's flagship dying, to anyone, means the defender wins; only the defender's dying means the attacker wins; the player beating both drops the fight; leaving with both alive leaves it to the next turn's roll, as today.
- Both officers sit out the turn's other actions while the run is open (`TurnSetup.canAct` covers the officer fighting in the open run today; it covers both).
- A won attack actually moves the cell: today `AtlasProgress.complete` vacates the cell before `runCompleted`, and `ContestOutcomes.canTake` refuses completed cells, so attackers only ever gain renown. Settle the fight before the cell is completed.

## Removed

- The side choice: the three entry choices (`FactionSkirmish.entryChoices`), `SkirmishSide`, `SkirmishJoin`, `joinRunNode`, the defender-side protected station and run failure, the joined side's no-drop rule, `ChartAllyReputation` if nothing else uses it, and the `side=` argument of `ChartOpenCommand`.
- Assault Orders: the catalyst row, the special item row, `FactionSkirmish.insertVerdict` and its refusal strings, and player-inserted skirmish records.
- The skirmish checks against `chart_skirmish_factions.csv` (`SkirmishFactions.takesSides` in `FactionSkirmish` and `SkirmishContests`): factions on that list never hold territory, so they never have officers. The list itself stays as the board's exclusion (`TerritoryFactions.canHoldTerritory`), renamed to say so.

Saves stay loadable: the old ally faction and the Assault Orders item keep stub rows, Assault Orders in cargo is swept out on load, old player-inserted skirmish records are dropped, and an old run's `SkirmishJoin` data decodes as no side.

## Build order

1. [x] Remove the side choice and the ally side; both sides hostile to the player and each other, with run-only attacker and defender copies of the factions hidden from intel; transponder reputation mapped to the real factions. Built: `SkirmishSide`, `SkirmishJoin`, `ChartSkirmishes`, the entry choices and the `side=` console argument are gone, and so is the protected station with its run failure (`ChartRun.protectedFleetId`, `ChartContent`, `SettledObjective`). The copies are `exiledSector_skirmish_attacker` and `exiledSector_skirmish_defender` (faction files with `showInIntelTab:false`, Nexerelin rows in `mod_factions.csv`), dressed by `RunFactionDress.hostileCopyOf` in `SkirmishBattle.takeSide`, which also keeps the real faction on `ChartIds.REAL_FACTION_MEMORY_KEY` for `ChartTransponderReputation.realFactionOf`. `ChartAllyReputation` became `RunFactionReputation`, keeping the copies' and old ally's standing fixed; the ally faction keeps its stub files.
2. [x] Officer fleets on both sides: the defender's flagship fleet at the station, the attacker's at its own rank, no fleet copying or divisor; both flagships marked on the map and named in the tooltip and intel. Built: `SkirmishPlanner.officerFleet` sizes each from its rank and `SkirmishBattle.spawnOfficerFleet` builds, dresses (`OfficerFleets.dress`) and reveals both; the attacker waits apart until a defender fleet falls (`SkirmishAdvance`). `SkirmishFight` carries both ranks and the contest on the run node, `FactionSkirmish.atlasLines` names both officers and `ChartObjectiveIntel` adds the defender's line.
3. [x] Same-faction challenges get a cell and a battle. Built: `SkirmishContests` places them like any challenge; pushes stay between factions.
4. [x] Finishing: both kills complete the run, two officer defeats in one run, the fight's result rules, both officers out of the turn while the run is open, and the attacker's win moving the cell. Built: `SkirmishObjective` tracks both flagships in the run's anomaly state (`SkirmishFlagships`, `SkirmishFlagship`) from `ChartObjectives.fleetGone`, `reconcile` and `bossFellInBattle`; a player kill runs `OfficerDefeat.officerBeaten` and, while the other stands, `OfficerTurns.officerBeatenInOpenRun`. `SkirmishFlagships.result` applies the result rules and `FactionSkirmish.settle` hands them to `ContestOutcomes`. `TurnSetup.openRunOfficerIds` keeps both officers out of the turn, and `AtlasProgress.completed` settles anomalies before completing the cell, keeping a claim that changed hands.
5. [x] Delete Assault Orders with its save sweep; drop the skirmish `takesSides` checks and rename the board exclusion list. Built: the catalyst row is gone and the item row is a retired stub swept by `RetiredItems`; `SkirmishRecords.dropPlayerInserted` drops old player-inserted records on load. `SkirmishFactions` became `TerritoryExclusions` reading `atlas_territoryless_factions.csv` and, for mods merging into it, the old `chart_skirmish_factions.csv`.
6. [x] Strings (en and zh_CN), `docs/docs.md` Faction Skirmish section, tests (`FactionSkirmishTest`, `SkirmishContestsTest`, `SkirmishPlannerTest`, `ContestOutcomesTest`, `OfficerTurnTest`, `ChartContentGeneratorTest`, `ChartObjectivesTest`, `AtlasOpeningTest`, `AtlasCatalystsTest`, `AtlasNodeTextTest`, `ChartOpenCommandTest`), and move `completed/anomalies/faction_skirmish.md` out of the way. Built: new tests `SkirmishFlagshipsTest`, `SkirmishObjectiveTest`, `TerritoryExclusionsTest` and `AtlasProgressTest`; the old plan is deleted.
