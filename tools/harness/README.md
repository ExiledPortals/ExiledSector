# Game harness

Launches the test install (`E:\Dev\Starsector`), loads a copy of a save, runs a scenario of steps against Exiled Sector screens, writes results and screenshots, and quits. The harness code in `exiledsector.harness` stays dormant unless the game starts with `-Dexiledsector.harness.save` or `-Dexiledsector.harness.newGame`, which only `run-harness.ps1` passes.

```powershell
.\tools\harness\run-harness.ps1 -Scenario tools\harness\examples\open-screens.json
```

`-Save` picks a save folder name under `E:\Dev\Starsector\saves` (or an absolute path); without it the scenario's `"save"` field is used, then the fixture save `harness_fixture`. The run works on copies in `E:\Dev\Starsector\harness-runs\<timestamp>\`, so real saves are never touched. That folder ends up holding `results.json`, `starsector.log`, the JVM's `console.log`, `scenario.json` and `screenshots\*.png`. The script prints one PASS or FAIL line per step and exits 0 when every step passed, 1 when a step failed, 2 when no results were written (the game crashed or never loaded the save) and 3 when it timed out (`-TimeoutSeconds`, default 600).

The game always runs windowed, never full screen, because the engine's screenshots break in full screen. `-Resolution` defaults to 1920x1080, the most common player resolution, and the script refuses anything above it.

The save must load with the current build: saves made before Exiled Sector 0.10.0 don't, and the script warns about them. `-GameDir C:\Games\Starsector` runs the playthrough install instead, with its own mods and saves (still on copies). An install with `starsector-core\fr.vmparams` is launched like `fr.bat`, from those JVM options, so Fast Rendering works; otherwise the script uses `vmparams`.

The game runs whatever is deployed to `E:\Dev\Starsector\mods\ExiledSector`, so deploy first; the script prints the deployed jar time.

## Fixture save

`fixture.json` starts a new game from the title screen, adds a ship of each hull size, saves, and the script copies that save to `saves\harness_fixture`. A run that uses the fixture builds it first when it's missing or older than `fixture.json`, and rebuilds it once and runs again when it no longer loads with the current build. `-RebuildFixture` forces a rebuild.

A scenario with a `"newGame"` object starts a new game instead of loading a save. `choices` lists the new game dialog options to pick in order after the character screen; an option the dialog only shows in dev mode can still be picked. It defaults to `["ngcDevStart"]`, the vanilla quick start, and `fixture.json` uses `nex_NGCDevStart` because the test install runs Nexerelin. `characterName` defaults to `Harness`. `"writeFixture": "name"` copies the save the run made to `saves\<name>` once every step passed.

`showcase-fixture.json` builds `saves\showcase_fixture` for recordings: a fleet rebuilt from an older playthrough, with its loadouts, levels and skill tree builds redone on the current tree, a ready fleet and no other fleets nearby. It isn't rebuilt automatically, so run it again after tree changes:

```powershell
.\tools\harness\run-harness.ps1 -Scenario tools\harness\showcase-fixture.json
```

## Scenario

```json
{
  "save": "save_UcheNiazi_1228493034787100064",
  "quitWhenDone": true,
  "steps": [
    {"step": "wait", "seconds": 3},
    {"step": "ignoreErrorsSoFar"},
    {"step": "openAtlas"},
    {"step": "wait", "frames": 30},
    {"step": "screenshot", "name": "atlas"},
    {"step": "expectNoErrors"}
  ]
}
```

| step | fields | passes when |
|---|---|---|
| `wait` | `frames`, `seconds` | both have passed |
| `mark` | `text` | always; logs `[ExiledSector] [harness] mark: text` |
| `startClip` | `name` | always; a recorded run cuts a clip from here (ending any open clip) |
| `endClip` | | a clip was open; the run ending also ends an open clip |
| `showcase` | `hideUI` (false), `paused` (false) | always; hides the version and FPS readout and, in the campaign, clears the message log, unpauses unless `paused` is set, and hides the campaign UI when `hideUI` is set |
| `openAtlas`, `openAtlasTree` | | the screen opened over the campaign |
| `openSkillTree` | `ship`: `flagship` (default), a fleet index, a hull id or a ship name | the refit screen opened on that ship and its skill tree is showing |
| `closeScreen` | | the top Exiled Sector screen closed |
| `focusAtlasSystem` | `system` (0), `anomalies`, `catalysts` | the nth system nearest the atlas origin (no specials) was reached, the anomalies were inserted into it regardless of its star, the player got one of each catalyst adding a `catalysts` anomaly and switched it on, and the atlas opened focused on it; the cell and each insertion's verdict go under `data.focusedSystem` |
| `zoom` | `to` | the top screen's camera jumped to that zoom, keeping its centre |
| `screenshot` | `name` | the frame was saved as `screenshots\<name>.png` |
| `expectScreen` | `name`: `atlas`, `atlasTree`, `skillTree`, `craftingBench`, a screen class name or `none` | the top Exiled Sector screen matches |
| `expectNoErrors` | | no ERROR log line since the last error check |
| `ignoreErrorsSoFar` | | always; later error checks skip earlier errors |
| `expectLog` | `contains`, `withinSeconds` (10) | a log line since launch contains the text |
| `simulate` | `ship`, as for `openSkillTree` | the refit screen ran the combat simulator on that ship and its opponent picker is closed |
| `spawn` | `variant`, `side`: `enemy` (default) or `ally`, `count` (1), `distance` (2500) | the ships spawned, enemies in a row ahead of the player ship, allies beside it |
| `autopilot` | `on` (true) | the player ship's autopilot is in that state |
| `speed` | `mult` | combat time runs at that multiple; `1` resets it |
| `recordStats` | `ship`: `player` (default), `enemy`, a hull id, a variant id or a ship name; `name` | the ship's modified stats, with each mod by source, are in `results.json` under `data.<name>` |
| `recordDamage` | `name` (`damage`), `seconds` of combat time (20), `untilNoEnemies`, `minEnemyDamage` (0) | the time is up or no enemy is left, and the enemy side took at least `minEnemyDamage`; hull, armor, shield and EMP damage per side and per ship go under `data.<name>` |
| `endSimulation` | | the simulator closed and the campaign is back |
| `addShip` | `variant`, `count` (1), `name`, `strip` (false), `weapons` (slot id to weapon id), `wings`, `hullMods`, `vents`, `capacitors`, `flagship` (false), `replaceFleet` (false) | the ships joined the player fleet at full CR; `strip` first removes the variant's weapons, wings, hull mods, vents and capacitors like the refit screen's Strip, then the given weapons, wings, hull mods, vents and capacitors are fitted (given weapons replace all of the variant's, unknown ones are skipped and listed); the ship became the flagship when set, and every ship the fleet had before was removed when `replaceFleet` is set |
| `levelShip` | `ship`, as for `openSkillTree`; `level` | the ship reached that level, or the level cap |
| `buildTree` | `root` (a root node type such as `root_high_tech`), `goals` (node type ids) | with the skill tree open, the root was chosen if the ship had none, and paths were allocated as a player would to the nearest node of each goal type in turn, cycling through the goals until the points ran out or a pass allocated nothing, and any points left went to small nodes next to the build, goal types first; goal types with no node allocated are listed as out of reach |
| `clearFleets` | `radius` (0, the whole location) | every other fleet in the player's location within the radius, stations aside, was despawned |
| `readyFleet` | `suppliesShare` (0.6) | every ship was repaired to full CR, crew was set to the fleet's minimum plus a tenth, fuel filled, and supplies set to that share of the free cargo space |
| `saveGame` | | the campaign saved |
| `addCargo` | `commodity`, `amount` | the commodity was added to the player fleet's cargo |
| `foundColony` | `size` (4), `industries`, `supply` (commodity id to amount), `nearHypershunt` (false) | a player colony was founded on the nearest uncolonised planet (to the player, or within range of the nearest coronal hypershunt, which is made usable), with those industries and extra supply, the player faction counts as named, and the player fleet moved beside it; details go under `data.colony` |
| `openChart` | `seed` (1, or the case seed in a suite), `tier` (1), `anomalies`, `region`, `layout`, `anomalyData` (anomaly id to data), `atlasNodes` | a Star Chart run opened beside the player fleet with one entry and the fleet jumped inside, and every forced anomaly is in the run (conflicting or mod-gated ones are dropped silently, so this fails); the atlas nodes add their chart lines as if allocated, without touching the atlas; details go under `data.chartRun` |
| `lockPhasePlanet` | | the first planet in the run that can be locked was surveyed and locked for the Phase Displacement Gate; the lock goes under `data.phaseLock` |
| `defeatChartBoss` | | the run's boss fleet was destroyed outside battle and the run's objective counts as complete |
| `leaveChart` | | the player fleet jumped back out of the run and the run closed |
| `passDays` | `days`, `speed` (50) | that many campaign days passed, with the engine stepped `speed` times a frame in 0.1 s steps, like fast-forward, and the player fleet left alone by other fleets |
| `expectPhasePull` | `name` (`phasePull`) | the gate has a pull job that isn't paused; its days, pause and wormhole radius go under `data.<name>` |
| `expectPhasedPlanet` | | the pull finished, its wormhole is gone, and the locked planet orbits in the gate's system with its type, kept conditions and traits; details go under `data.phasedPlanet` |

`examples/phase-pull.json` drags a Star Chart planet into a new colony's system: it founds the colony with a Phase Displacement Gate, locks a planet in a run, clears the run, and passes the 120-day pull.

Combat steps run between `simulate` and `endSimulation`; `examples/simulate-combat.json` runs a short fight. Combat screenshots are taken below the combat HUD.

Each step times out after 30 seconds unless it waits longer by design. A failing step doesn't stop the run. Screen opens record the `[perf]` timing line in the step detail.

## Suites

A scenario with `"groups"` instead of `"steps"` is a suite: many cases that run one after another in one game session. A group is usually one anomaly; its `setup` steps run at the start of each of its cases, and `requiresMods` lists mod ids it needs, so its cases are skipped when one is missing. Each case has an `id`, optional `tags` and `seed`, and its `steps`. A case's full id is `group/case`, and its seed defaults to a hash of that id; `openChart` without a `seed` uses it.

```json
{
  "groups": [
    {"id": "excavation_sites", "tags": ["dense_nebula"], "setup": [{"step": "wait", "frames": 5}], "cases": [
      {"id": "open_shaft", "steps": [{"step": "openChart"}, {"step": "leaveChart"}]}
    ]}
  ]
}
```

Before the first case the harness records a baseline: the player fleet's location, members, CR, hull and cargo, the transponder, the atlas and item state, and a census of Star Chart content. Every case starts with `ignoreErrorsSoFar`, ends with `expectNoErrors`, and then tears down: it ends any combat, closes any dialog, restores the baseline, closes any open run, and fails when the run stays open or Star Chart content is left behind. A failing step skips the rest of its case and goes straight to the teardown. A failing teardown stops the session, because the cases after it would start from a dirty game; the script then relaunches the game for the remaining cases, at most `-MaxRelaunches` (10) times.

`-Group excavation_sites`, `-Case excavation_sites/open_shaft` and `-Tag dense_nebula` run only the matching cases, and `-StartAfter` skips every case up to and including the named one. The script prints a CASE PASS, FAIL, SKIP or NOT RUN line per case and a total per group, and `results.json` lists them under `cases`, `skippedCases`, `notRunCases` and `abortedAfterCase`.

## Recording

`-Record` films the game window for showcase videos. Once the window appears the script centres it on the primary monitor, keeps it on top, and records its 1920x1080 client area with ffmpeg's Desktop Duplication grab (`ddagrab`) at `-RecordFps` (60), without the mouse cursor or sound, on the NVIDIA encoder or `libx264` when that fails. The game has to stay windowed: Starsector breaks screen capture in full screen and borderless modes. Keep the window uncovered and the mouse off it while a take runs, since a hovering cursor opens tooltips in the shot.

The whole run lands in `recording.mp4` in the run folder. Every `startClip` and `endClip` pair is also cut into `clips\NN_<name>.mp4` with half a second of padding on each side, so a scenario can stage several shots in one launch. Clip times come from the wall clock, matched to the recording's length, so they stay within a frame or two.

Start a take with a `showcase` step so the shot has no version readout, Version Checker messages or pause banner. Clips play at a constant frame rate.

```powershell
.\tools\harness\run-harness.ps1 -Scenario tools\harness\examples\record-atlas.json -Record
```
