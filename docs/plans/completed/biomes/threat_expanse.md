# Threat Expanse modifiers

Hull items dropped in Threat Expanse systems can carry a Threat Expanse modifier. The biome is abyssal hyperspace holding only Threat fleets, so every modifier works through vanilla's Fragment Swarm: growing it, regrowing it, and spending its fragments in place of flux, armor or replacement rate. Two of them scale with affinity of every kind, since the Threat learns from all three lines alike.

Status: built 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_THREAT_EXPANSE`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 10% of hull items dropped here. Values are placeholders.

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | of the Swarm (suffix) | `SENSOR_STRENGTH_PERCENT_PER_FRAGMENT_SWARM`, fleet-wide: increased sensor strength per ship with a Fragment Swarm | 1% per ship in the fleet | the swarms keep watch |
| Specialist Crew | Witnessed (prefix) | `FRAGMENT_SWARM_SIZE_PERCENT_PER_AFFINITY` | 1% increased swarm size per 5 affinity of every kind | crews who have watched the swarms |
| Drive Assembly | of the Hunt (suffix) | `TOP_SPEED_WHILE_SWARM_FULL_PERCENT`: increased top speed while the swarm is nearly full | 10-15% at 90% of the swarm's size or more | a full swarm on the hunt |
| Reactor Regulator | Fabricating (prefix) | `FRAGMENT_REPLACEMENT_RATE_PERCENT_PER_AFFINITY` | 1% increased fragment replacement rate per 5 affinity of every kind | a reactor feeding the fabricator |
| Fire Control Suite | of Unmaking (suffix) | `HULL_DAMAGE_FRAGMENT_REGROWTH_FLAT`: hull damage dealt regrows fragments | 1 fragment per 300 hull damage | eating what it breaks |
| Shield Emitter | Fractal (prefix) | `SHIELD_DAMAGE_ABSORBED_BY_FRAGMENTS_PERCENT`: fragments absorb shield damage | 10-15% reduced shield damage taken, spending 1 fragment per 30 damage absorbed | fragments thrown in front of shots |
| Phase Coil Array | of the Void (suffix) | `PHASE_UPKEEP_COVERED_BY_FRAGMENTS_PERCENT`: fragments cover phase upkeep | 50% of the upkeep, 1 fragment per 50 flux | spending the swarm to stay unseen |
| Launch Assembly | Hive-fed (prefix) | `FIGHTER_REPLACEMENT_RATE_RESTORED_BY_FRAGMENTS_PERCENT`: replacing a fighter costs fragments instead of replacement rate | 5 fragments per fighter, restoring 100% of the rate it cost | the hive rebuilds its own |
| Armor Plate | Unmade (prefix) | `SWARM_OVERFLOW_ARMOR_REPAIR_PERCENT`: fragments the full swarm can't hold repair armor instead | each repairs 0.5% of maximum armor on the most damaged cell, up to 10% of maximum armor per combat | the swarm patches the hull |

Every modifier but of the Swarm and of the Void needs the ship to have a swarm (`RoilingSwarmEffect.getSwarmFor`) and does nothing without one; each line names the ship's Fragment Swarm. A ship has one from vanilla's Fragment Swarm hull mod or the Threat volume's Fragment Swarm notable. Of the Swarm counts the fleet's ships with either and raises the fleet's sensor strength, synced per fleet like the Rift's `MOVE_SLOW_BURN_LEVEL_FLAT`. Vanilla sizes the swarm from `fragment_swarm_size_mod` (20, 40, 60 or 100 by hull size) and regrows it from `fragment_swarm_respawn_rate_mult` (1, 2, 3 or 5), which the existing `FRAGMENT_SWARM_SIZE_PERCENT` and `FRAGMENT_REPLACEMENT_RATE_PERCENT` already raise. Witnessed and Fabricating are `AffinityScaling` rows in `SpecialSkillEffect` on the all-affinities filter with the small step (5), targeting those two.

Fragments are spent with `despawnMembers` and added with `addMembers`, never above the swarm's maintained size, and an effect with too few fragments left does nothing. Of the Void halves phase upkeep while a swarm holds fragments and spends one per 50 flux it covered. Phase hulls can't take a Fragment Swarm (vanilla's hull mod refuses them and the notable is `req_non_phase_hull`), and the Phase Coil Array only fits phase hulls, so as planned the modifier could never work; it draws on the ship's own swarm if it has one, or else the nearest allied ship's swarm within 1000 su. Hive-fed restores the replacement rate a fighter replacement cost, if the swarm has 5 fragments to spend; its rolled value is the share of the rate restored (100%) rather than the fragment cost, so item scaling improves it instead of raising the cost. Of Unmaking rolls the fragments per 300 hull damage (1) for the same reason. Unmade counts the fragments vanilla's regrowth would add while the swarm is already full and turns each into armor repair, until it has restored 10% of the ship's maximum armor that combat.

## Build order

1. [x] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
2. [x] The new effects, each in its ship part's `skilleffect` package with tests: a shared swarm helper (find, spend, add up to size) beside the existing fragment effects in `shipsystem`, the two `AffinityScaling` rows, and the fleet-wide sensor strength. Built: `shipsystem.FragmentSwarms`; `FleetWideEffects.applySwarmSensorStrength` (logistics); the two affinity rows in `SpecialSkillEffect`; `TOP_SPEED_WHILE_SWARM_FULL_PERCENT` (a `ConditionalStatEffect` in movement); `HullDamageRegrowthListener` (onhit); `FractalShieldListener` (shield); `PhaseVoidUpkeepListener` (phase); `HiveReplacementListener` (fighter); `SwarmOverflowRepairListener` (defense).
3. [x] `docs/docs.md` for the fleet's swarm count, the fragment spending and the armor repair from a full swarm (Threat Expanse modifiers).

## Decisions

- Several ships carrying of the Swarm don't stack: the fleet takes the highest carrier's value once, times its ships with a Fragment Swarm.
- The affinity effects read all three affinities summed, like the other all-affinity rows.
- Fractal counts beam hits like any other: it spends 1 fragment per 30 damage absorbed in total, so a beam drains the swarm at the same rate per damage as any shot.
