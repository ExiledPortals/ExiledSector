# Star Chart fleet drawbacks

Charts need more prefix drawbacks. Today all but one of them strengthen the enemy (fleet size, extra fleets, officers, wardens, fleet quality), and the exception is Shrouded's sensor range. This plan adds drawbacks that weaken the player's whole fleet while it is inside the chart system. It builds the "Player fleet drawbacks" idea from `docs/plans/star_charts.md` for the lines below. The rest of that list stays planned there.

Every drawback here is a prefix, like the existing ones (the user's call on 2026-10-20, replacing the 2026-10-19 note that player drawbacks would be suffixes). Each one is a hybrid modifier in `star_chart_modifiers.csv`: the drawback plus `CHART_QUANTITY_PERCENT`, in the same three tier bands as the existing prefixes. All values and pool weights are placeholders pending balance.

Every drawback applies to the entire player fleet: every ship, and the fighters those ships launch. Allied fleets that join a battle are not affected.

## Groups

### Ship drawbacks (done)

These change the stats of every ship in the player fleet, so they also show in the refit screen while the player is inside the system.

| Modifier | Effect | Name |
|---|---|---|
| `HYBRID_CHART_FRAGMENTATION_DAMAGE_TAKEN` | more fragmentation damage taken (armour, hull and shields) | Splintering |
| `HYBRID_CHART_KINETIC_DAMAGE_TAKEN` | more kinetic damage taken | Battering |
| `HYBRID_CHART_HIGH_EXPLOSIVE_DAMAGE_TAKEN` | more high explosive damage taken | Volatile |
| `HYBRID_CHART_ENERGY_DAMAGE_TAKEN` | more energy damage taken | Searing |
| `HYBRID_CHART_TOP_SPEED` | less top speed | Viscous |
| `HYBRID_CHART_MANEUVERABILITY` | less maneuverability | Turbulent |
| `HYBRID_CHART_SHIELD_ARC` | less shield arc | Constricting |
| `HYBRID_CHART_PHASE_ACTIVATION_COST` | more phase cloak activation cost | Anchoring |
| `HYBRID_CHART_FLUX_DISSIPATION` | less flux dissipation | Sweltering |
| `HYBRID_CHART_WEAPON_RANGE` | less ballistic, energy and missile weapon range | Myopic |

### Battle drawbacks (done)

These change the battle itself for every fight the player fleet has inside the system.

| Modifier | Effect | Name |
|---|---|---|
| `HYBRID_CHART_DEPLOYMENT_POINTS` | less maximum deployment points for the player's side | Cramped |
| `HYBRID_CHART_ENEMY_DEPLOYMENT_POINTS` | more maximum deployment points for the enemy side | Besieged |
| `HYBRID_CHART_COMMAND_POINT_RATE` | less command point recovery rate | Jammed |
| `HYBRID_CHART_ENEMY_OBJECTIVE_CAPTURE` | enemy ships capture combat objectives faster | Contested |

### Campaign drawbacks (done)

These act on the campaign map while the player fleet is in the system.

| Modifier | Effect | Name |
|---|---|---|
| `HYBRID_CHART_HUNTED` | enemy fleets get +5000 sensor range, so they see the player and vanilla AI hunts them | Hunted |
| `HYBRID_CHART_SENSOR_RANGE` (exists) | less sensor range | Shrouded |

## Interpretations (confirmed by the user 2026-10-20)

- "Increased dissipation" is read as reduced flux dissipation, since more dissipation would be a bonus.
- "Increased max deployment points" is read as the enemy side getting more, since the player getting more would be a bonus. "Reduce the max deployment points" is the player's side.
- "Reduced objective capture time" is read as enemy ships capturing faster. Slowing the player's capture instead isn't possible through the capture rate stat: the engine takes the best ship's rate on a side and floors it at 1, so a multiplier below 1 does nothing.

## How they apply

- One snapshot, `ChartFleetConditions`, holds the active ship and battle drawbacks as multipliers per `ChartFleetStat`. `ChartRunScript` sets it while the player fleet is inside a live chart system and clears it when the fleet leaves or the run closes. `ChartRunEffects.removeFrom` and `ChartLoadSweep.liftEffectsOutsideSystem` also clear it, so loading a save always starts clean. On every change it marks the player fleet's members for a stat update. Nothing is saved.
- Ship drawbacks: `SkillTreeHullMod` (on every player fleet member, applied last) applies the snapshot after the tree in `applyEffectsBeforeShipCreation`, only to members of the player fleet, under `ChartFleetConditions.MOD_ID` as their own multipliers. Several rolls of one stat add up first and are applied as one multiplier, clamped to 10%-1000%, so the lines say "more" and "less". Shield arc is fixed when the ship is built, which this covers. Fighters get the same stats in `applyEffectsToFighterSpawnedByShip`.
- The per-type damage taken stats only cover armour and hull. Shields have their own per-type stats, which get the same multiplier.

### Battle drawbacks (verified against the decompiled engine 2026-10-20)

A combat plugin registered under `plugins` in `data/config/settings.json` applies these to the player's side when a battle inside the system starts.

- Deployment points: `CombatEngine.computeFleetStrengths()` runs every unpaused frame and overwrites `setMaxStrength`, so that call doesn't stick. `setMaxFleetPoints` sticks but freezes both sides for the whole battle. The usable hook is the commander's `Stats.DEPLOYMENT_POINTS_FRACTION_OF_BATTLE_SIZE_BONUS_MOD`, on the `PersonAPI` from `getFleetCommanderPreferPlayer()`. It is added per side after the base split, so changing one side doesn't change the other. The engine floors that bonus at the MIN fraction (0 by default), so a negative flat only cancels objective bonuses. These are character stats saved with the game, so the plugin removes the modifier when the battle ends, `ChartRunScript` removes it on the next campaign frame, and every load removes it from the player. Don't use the MIN fraction mod: the enemy's floor reads the player commander's value.
- Command points: the rate is the commander's `command_point_rate` plus `getCPRateModifier()` (base 0, where Comm Relays add their flat bonus). A multiplier on that modifier does nothing useful, so the drawback is a flat `-X * getCPRateMult()` on `getTaskManager(false).getCPRateModifier()`, refreshed about once a second, keeping the total above 0. A negative rate runs the timer backwards.
- Objective capture: `BattleObjective.advance()` reads each ship's `SHIP_OBJECTIVE_CAP_RATE_MULT` live and uses the side's best, floored at 1. Enemy ships get a multiplier above 1, applied by the plugin as they deploy.

### Hunted

The user chose the minimal version on 2026-10-20: no assignments or memory flags, just a flat sensor range bonus on every chart fleet at spawn (`ChartContentGenerator.applyEnemySensorRange`). Fleet stat modifiers are saved with the fleet, so nothing else is needed, and vanilla AI pursues what it can see. The engine drops anything beyond `sensorRangeMax` (5000 su) from detection checks, so fleets further away still can't see the player. Lifting that cap would need `setExtendedDetectedAtRange` on the player fleet; not done.

## Build order

1. [x] Ship drawbacks: `ChartFleetStat`, `ChartFleetConditions`, `ChartEffect` entries, `ChartGenContext.fleetStatMults`, run script and hullmod hooks, data rows and pool weights in every chart, strings (en and zh_CN), `ChartFleetConditionsTest` and `ChartRunEffectsTest`. Uncommitted on `main`.
2. [x] Battle drawbacks: `ChartBattlePlugin` (registered in `data/config/settings.json`), commander-stat cleanup in the plugin, `ChartRunScript` and `StarCharts.onGameLoad`, data rows, pool weights, strings, `ChartBattlePluginTest`. Uncommitted on `main`.
   Open (review 2026-10-20): Cramped and Besieged don't work as built. The engine floors the commander bonus at 0 and caps each side at 60% of battle size, and the first-frame `setMaxStrength` only changes the opening deployment screen. Proposed fix, awaiting the user: Cramped as more deployment cost for the player's ships, Besieged as less for chart-fleet ships, or dropped.
3. [x] Hunted: enemy sensor bonus at spawn, data row, pool weight, strings, test in `ChartBattlePluginTest`. Uncommitted on `main`.
4. [x] `docs/docs.md` section "Star Chart drawbacks". Uncommitted on `main`.
