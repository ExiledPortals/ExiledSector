# Anomaly: Fire Ship Hangar

Battles in the system can hold a combat objective modelled on Secrets of the Frontier's Hyperwave Transmitter: a derelict hangar that, while a side holds it, keeps launching Dram tankers wired with Ludd's Light for that side. The Drams charge the nearest enemy at full burn and blow up on contact.

Status: proposed 2026-11-02 (community suggestion). Not built. Needs Secrets of the Frontier. See `overview.md` for the shared framework.

## SotF's objectives

Secrets of the Frontier (`secretsofthefrontier`, 0.15.1 checked) adds battle objectives in `data/config/battle_objectives.json`. Its reinforcers (`sotf_objective_reinf` and the faction variants, effect class `data.scripts.combat.obj.SotfReinforcerEffect`) periodically spawn droneships for whichever side holds them: every 20 seconds while fewer than a threshold are alive, it picks a variant for the objective's faction, adds it to an empty fleet, spawns it with `getFleetManager(owner).spawnFleetMember`, tags the variant `$sotf_reinforcementship` and sets 70% CR.

The hook other mods can use is data, not code: SotF's own battle creation plugin (`SotfBattleCreationPluginImpl`, picked at `MOD_GENERAL` when its `enableTacticalExpansion` setting is on) chooses each battle's objectives from `data/config/sotf/objective_spawns.csv`, read with `getMergedSpreadsheetDataForMod`, so any mod can add rows. Columns: `id,obj_id,tag,hyperspace,escape,central_spawn,flag,not_flag,minFP,maxFP,weight`. A row with a `tag` only rolls in star systems with that tag, and `flag` and `not_flag` check sector memory keys.

## The hangar objective

- Our `data/config/battle_objectives.json` row `exiledSector_objective_fire_ship_hangar` (name Fire Ship Hangar, placeholder SotF reinforcer icon copied in with a mod prefix), with our own effect class `FireShipHangarEffect` extending vanilla's `BaseBattleObjectiveEffect`. It copies SotF's reinforcer pattern, not its code, so we don't compile against SotF.
- While a side holds it, every 20 seconds (placeholder) it spawns one Dram at the objective for that side, through an empty fleet as SotF does, up to 2 alive at once (placeholder), at 70% CR, tagged so we can find them.
- Each Dram gets Ludd's Light's explosion (`SpecialSkillEffect.EXPLODE_ON_DEATH`, `DeathExplosionListener`, placeholder 2000) and a ram AI of ours (`FireShipRamAI`, a `ShipAIPlugin`): pick the nearest non-fighter enemy, burn straight at it with full throttle and ship system, and die and detonate on touching any ship. It retargets when the target dies and self-destructs after a time limit (placeholder 30 seconds).
- It's an ordinary capturable objective, so either side can take it and it can change hands; holding it is a fire-ship tap for whoever has it.

## Placing it in battles

Our own `data/config/sotf/objective_spawns.csv` with one row: `obj_id` the hangar, `tag` a system tag the anomaly puts on its chart system (`exiledSector_fire_ship_hangar`), `escape` false, `central_spawn` true, weight placeholder 3 so it shows up in most battles there. SotF merges the row and its battle creation does the rest. The anomaly itself claims no battles (`battleFor` stays null), so SotF's plugin is the one that runs; battles another anomaly claims (an Excavation shaft) get no hangar. The tag goes away with the chart system when the run closes, so no save trace.

## Without SotF

The anomaly never rolls: its `chart_anomalies.csv` row has `requires_mod` `secretsofthefrontier`, and `fitsAt` also refuses while SotF's tactical expansion is off, since then nothing places objectives (read from SotF's settings; how is a check below). SotF gets a `CompatTarget` with `TESTED_VERSIONS` 0.15.1. A version that places the hangar without SotF, through our own battle creation, is possible later but not part of this plan.

## Rewards

Battles in the system drop increased item quantity (placeholder +25%) through the anomaly's battle loot hooks (`holdsPlayerBattle`, `battleLootMult`), whoever held the hangar.

## Checks before building

1. How to read SotF's `enableTacticalExpansion` (its `sotf_settings.ini` through `Global.getSettings().loadJSON` with SotF's mod id, or a LunaLib setting if it has one).
2. That spawned Drams stay out of the post-battle report, losses and recovery; SotF's reinforcers are spawned the same way, so their handling is the reference.
3. Whether Ludd's Light's explosion hurts the holder's own ships nearby (it should).
4. How the ram AI copes with collisions against allies, asteroids and shields.

## Build order

1. [ ] Spike with SotF enabled: our row in `objective_spawns.csv` showing up in a tagged system's battles, the objective spawning Drams for its holder, Ludd's Light on them, the battle result check.
2. [ ] The anomaly class (`exiledsector.charts.anomaly.firehangar`), its insertable `chart_anomalies.csv` row with `requires_mod`, the system tag, `fitsAt`, atlas marker icon (placeholder); the SotF `CompatTarget`.
3. [ ] `battle_objectives.json` row and icon, `FireShipHangarEffect` and `FireShipRamAI`.
4. [ ] The loot hook; strings (en and zh_CN draft) including the objective's long description; the `docs/docs.md` section; tests for the ram target picking and spawn pacing; README compatibility line and `docs/forum_post.bbcode`.

## Open questions

1. Whether to also tag chart systems so SotF's own Hyperwave Transmitters roll there (its rows filter on vanilla theme tags, so ours would need new rows pointing at its objectives). Proposed no for now.
