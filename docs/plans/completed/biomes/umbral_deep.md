# Umbral Deep modifiers

Hull items dropped in Umbral Deep systems can carry an Umbral Deep modifier. The biome is abyssal hyperspace holding only Shrouded Dweller fleets, so every modifier copies one of their combat mechanics: the shroud's blindness to sensors, Hungering Rift's healing, the lunge away from damage, Inimical Emanation's repair disruption, the Eye's shield venting hard flux, the Lens strikes, the vortex's death explosion and the free vents of the shrouded hullmods. Three of them work through Shrouded hullmods and nodes, as the Threat Expanse set works through the Fragment Swarm. The other six work on any ship; phase hulls can't take a shrouded hullmod and the Phase Coil Array only fits phase hulls.

No name, line or description names the Dwellers. Lines name vanilla's own terms ("Shrouded hullmod or node", Hungering Rift) where they need to, and nothing more.

Status: built 2026-11-22. Of the Gaze and the shroud burst Churning replace of Expulsion and the vortex Churning, which can't be built (see Findings). Rows are in `item_modifiers.csv` ending `_UMBRAL_DEEP` and mapped in `biome_modifiers.csv`, through the existing framework (`../anomalies/abyssal_rift.md`, Biome modifiers). Values are placeholders.

## Modifiers

| Item | Name | Effect | Placeholder | Dweller mechanic |
|---|---|---|---|---|
| Command Suite | of the Unseen (suffix) | `SENSOR_PROFILE_PERCENT_PER_SHROUDED_SHIP`, fleet-wide: reduced fleet sensor profile for each ship with a Shrouded hullmod or node | 2-3% for each ship, up to a maximum of 30% | dweller hulls have no sensor profile |
| Specialist Crew | Inured (prefix) | `CREW_LOSS_PERCENT_PER_SHROUDED`: reduced crew casualties for each Shrouded hullmod or node on this ship | 15-25% for each | offsets the shroud's 50% increased crew casualties |
| Drive Assembly | Convulsive (prefix) | `HULL_LOSS_LUNGE_SPEED_PERCENT`: after this ship loses 10% of its maximum hull, increased top speed and maneuverability for 4 seconds | 20-30%, at most once every 10 seconds | Convulsive Lunge and Shrouded Mantle's recoil from damage |
| Reactor Regulator | of Exhalation (suffix) | `VENT_RATE_PERCENT_PER_SHROUDED`: increased venting speed for each Shrouded hullmod or node on this ship | 20-30% for each | shrouded hullmods keep striking for free while venting |
| Fire Control Suite | Inimical (prefix) | `HULL_HIT_REPAIR_DISRUPTION_CHANCE_PERCENT`: chance for a hull hit from this ship's weapons to double the target's weapon and engine repair time for 5 seconds | 10-20% chance | Inimical Emanation's repair disruption (vanilla `RDRepairRateDebuff`) |
| Shield Emitter | of the Gaze (suffix) | `HARD_FLUX_DISSIPATION_PERCENT`: hard flux dissipation while shields are active, the existing effect the Remnant Expanse's of the Nexus uses | 10-15% | the Shrouded Eye venting hard flux behind its shield |
| Phase Coil Array | of Glare (suffix) | `PHASE_EXIT_LENS_STRIKES_FLAT`: when this ship leaves phase, it strikes random enemies within 400 su for 75 energy damage each | 2-4 strikes | Shrouded Lens strikes |
| Launch Assembly | Churning (prefix) | `STRIKECRAFT_DEATH_SHROUD_BURST_DAMAGE_FLAT`: strikecraft from this ship collapse into a shroud when destroyed, dealing energy damage to enemy ships and strikecraft within 150 su | 75-100 damage | the Shrouded Vortex's death explosion |
| Armor Plate | Hungering (prefix) | `HULL_DAMAGE_ALLY_REPAIR_PERCENT`: hull damage dealt by this ship repairs the most damaged allied ship within 2000 su | 5-10% of the damage dealt | Hungering Rift healing its allies |

## Mechanics

A Shrouded hullmod is any hullmod whose spec has vanilla's `shrouded` tag: Shrouded Mantle, Shrouded Lens and Shrouded Thunderhead. A Shrouded node is an allocated node of one of the six Shrouded types: the Shrouded Mantle, Shrouded Lens and Shrouded Thunderhead notables, Shrouded Lens Damage, Rift Lightning Damage and Hungering Rift Healing. The three notables add their real hullmod as a phantom perma mod, so a notable and the hullmod it grants count once, not twice. One shared counter gives each ship its count of Shrouded hullmods and nodes; Inured and of Exhalation scale with it and do nothing at 0, and of the Unseen counts the fleet's ships with at least 1. Their lines say "Shrouded hullmod or node".

Of the Unseen follows the Threat Expanse's of the Swarm: the fleet takes the value of its strongest carrier once, multiplied by its number of ships with a Shrouded hullmod or node, synced per fleet. It modifies the fleet's detected range as the Abyssal Rift's sensor profile modifier does.

Convulsive tracks hull loss the way Shrouded Mantle does: the burst triggers once hull drops 10% of maximum below the highest hull since the last burst, then waits 10 seconds. Inimical applies vanilla's own repair debuff, so the target shows vanilla's status for it; a ship already under it only has the 5 seconds refreshed. Hungering repairs any allied ship, shrouded or not, picked by missing hull; the dealing ship may pick itself. Fighters and modules aren't picked.

Churning works like the Furnace's Cinderwing (both share `fighter.StrikecraftDeathBurst`) with energy damage, and looks like a dweller dying: the swirling and dark shroud particles and the pink flare vanilla's `DwellerCombatPlugin` spawns for a dying fighter-sized dweller, with the Lens explosion's sound.

Of Glare's strikes copy Shrouded Lens: a 75 energy damage explosion of radius 50 with no friendly fire, on random enemy ships and missiles within 400 su (edge to edge), closer targets more likely, skipping phased ones. Each hit is a separate random target, and the same target can be hit more than once.

## Findings

- Spawned ejecta and vortices on the player's side: their AI works for side 0 (vanilla picks it by the `dweller_vortex` and `dweller_ejecta` hull tags and steers by the ship's own owner), the fleet AI's stop rule skips fighter wings, and they leave no hulks or recovery entries (hulk chance 0, `no_battle_salvage`). They still can't be spawned cleanly: both wings carry the built-in `dweller_hullmod`, whose `addStrategyAI()` returns true, so spawning one adds `DwellerCombatStrategyForBothSidesPlugin` to the battle. Its constructor gives both sides' task managers a flat +1,000,000,000 command points that is never removed, which breaks command points in every battle without Dwellers. Vanilla only turns this off for the human-ship shroud hullmods, so there is no clean way to spawn the wings for our side. Of Expulsion and the vortex Churning were dropped for of the Gaze and the shroud burst Churning.
- The vortex explosion uses the vortex as its source with `PROJECTILE_NO_FF`, so it can't hit its own side. Confirmed; it no longer matters, since the shroud burst damages only hostile ships through `applyDamage`.
- Shrouded Mantle's lunge pushes the ship's velocity and decelerates it every frame, so Convulsive's maneuverability would fight it. Convulsive waits while the Mantle's lunge runs, reading vanilla's `ShroudedMantleHullmodData.lunging` field.

## Build order

1. [x] The rows in `item_modifiers.csv` and `biome_modifiers.csv` (nine), the `chart.biomeModifier.header.umbral_deep` header (`starCharts` catalogue) and the line strings (en and zh_CN, `core` catalogue).
2. [x] The new effects, each in its ship part's `skilleffect` package with tests. Built: the counter `special.ShroudedCount`, reading the core `PlacedNode` interface that skill-tree's `AllocatedNode` implements; the per-count stat rows `special.PerShroudedPercentEffect` (`CREW_LOSS_PERCENT_PER_SHROUDED` in `LogisticsSkillEffect`, `VENT_RATE_PERCENT_PER_SHROUDED` in `FluxSkillEffect`); the fleet-wide sensor profile `SENSOR_PROFILE_PERCENT_PER_SHROUDED_SHIP` (`LogisticsSkillEffect`, `FleetWideEffects.shroudedSensorProfilePercent`); `movement.HullLossLungeListener` with `movement.MantleLunge`; `onhit.HullHitRepairDisruptionListener`; `phase.PhaseExitLensStrikeListener` with `phase.LensStrike`; `defense.HullDamageAllyRepairListener`. Hull damage dealt is routed through the shared `skilleffect.DealtHullDamage`, which `onhit.HullDamageRegrowthListener` now uses too. Later: `fighter.ShroudBurstListener` on the shared `fighter.StrikecraftDeathBurst`, which `fighter.CinderBurstListener` now extends; of the Gaze reuses `HARD_FLUX_DISSIPATION_PERCENT`.
3. [x] `docs/docs.md`, Biome modifiers: no change. The general section covers the biome, and the lines say what each modifier counts.

## Decisions

- No effect ids were renamed. Of Exhalation's line says "venting speed", vanilla's term already used by `VENT_RATE_PERCENT`.
- Several ships carrying of the Unseen don't stack: the fleet takes the strongest carrier's value once.
- A gated Shrouded node (one inside a subroutine's radius that only works under a combat condition) doesn't count. A Shrouded node granted from outside the tree (`GrantedNodeOrigin`) counts only through a Shrouded hullmod it puts on the ship.
- Hungering banks its share of hull damage and repairs every 0.5 seconds, picking the allied ship missing the most hull rather than a weighted random one. A share with no damaged ally in range is lost.
- Inimical rolls once per weapon hit that isn't a shield hit; a beam rolls its chance for each second of contact.
- Of Glare's strikes cost no flux, start their beam at the ship instead of a shroud, and skip hulks and neutral objects. They use the Lens explosion's sound and collision class, so Shrouded Lens Damage raises their damage too.
