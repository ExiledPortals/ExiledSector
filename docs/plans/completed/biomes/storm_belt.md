# Storm Belt modifiers

Hull items dropped in Storm Belt systems can carry a Storm Belt modifier. The biome is deep hyperspace cloud and frequent storms, so its set is about lightning, taking EMP and turning it to use and arcing it back out, and the Midline workhorses that ply the storms: three of its items grant or read Midline Affinity.

Status: built 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_STORM_BELT`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 10% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Stormwise (prefix) | new, fleet-wide: `TERRAIN_MOVEMENT_PENALTY_PERCENT`, reduced terrain movement penalty | 5% per ship, up to 50% | storm pilots |
| Specialist Crew | Storm-seasoned (prefix) | existing `MIDLINE_AFFINITY_FLAT` | 30-40 | crews of the hulls that work the storms |
| Drive Assembly | of the Gale (suffix) | new: `EMP_ARC_STRIKE_SPEED_PERCENT`, speed burst after an EMP arc hits the ship | 20% top speed and maneuverability for 3 s | riding the strike |
| Reactor Regulator | Discharging (prefix) | new: `EMP_DAMAGE_TAKEN_HARD_FLUX_VENT_PERCENT`, EMP damage taken vents hard flux | 20-30% of the EMP damage | grounding the charge |
| Fire Control Suite | of the Tempest (suffix) | new: `WEAPON_RATE_OF_FIRE_PERCENT_PER_MIDLINE_AFFINITY` | 1% increased weapon rate of fire per 10 Midline Affinity | a hail of fire |
| Shield Emitter | Faraday (prefix) | new: `SHIELD_PIERCE_CHANCE_PERCENT_PER_MIDLINE_AFFINITY` | 1% chance to avoid EMP arcs piercing per 10 Midline Affinity | a Faraday cage |
| Phase Coil Array | of the Eye (suffix) | new: `PHASE_EXIT_EMP_ARCS_FLAT`, leaving phase discharges EMP arcs | 3 arcs at enemies within 600 su | the eye of the storm |
| Launch Assembly | Thunderwing (prefix) | new: `STRIKECRAFT_EMP_DAMAGE_PERCENT`, strikecraft hits deal extra EMP | 10-15% of the hit's damage | charged wings |
| Armor Plate | Charged (prefix) | new: `ARMOR_DAMAGE_EMP_ARCS_FLAT`, absorbed damage builds into an EMP arc | one arc per 500 damage the armor absorbs | a lightning rod |

Command Suite: every ship carrying it reduces the burn level the whole fleet loses inside deep hyperspace clouds, as the Navigation skill does, up to 50% for the fleet. Navigation lowers the commander's character stat `nav_penalty_mult` (`Stats.NAVIGATION_PENALTY_MULT`) by a flat 0.3, and vanilla multiplies every terrain slowdown by it (deep hyperspace clouds and their storms, the abyss; `Misc.getFleetRadiusTerrainEffectMult`), so the effect is a flat -0.05 per ship on that stat, adding to Navigation's, synced per fleet like the Rift's `MOVE_SLOW_BURN_LEVEL_FLAT`. Because it is the same stat, the line names it as vanilla does, terrain movement penalty. Storm strike CR loss is left out: vanilla's `CORONA_EFFECT_MULT` already covers it, and the Furnace uses that stat.

Of the Tempest and Faraday are new `AffinityScaling` rows in `SpecialSkillEffect` on the Midline filter with the large step (10), targeting the all-weapons rate of fire (`WeaponStatFamily.RATE_OF_FIRE`) and a reduction to `SHIELD_PIERCE_CHANCE_PERCENT`; the crew's affinity feeds both. Phase Coil Array and Armor Plate arcs hit the nearest enemies in range, with damage scaled by hull size (placeholder).

As built: the Reactor Regulator prefix is Discharging, since Grounded already names the reactor's `EMP_DAMAGE_TAKEN_PERCENT_REACTOR` prefix. The fire rate effect is `WEAPON_RATE_OF_FIRE_PERCENT_PER_MIDLINE_AFFINITY`, matching its target `WEAPON_RATE_OF_FIRE_PERCENT`. The Gale burst starts on any EMP arc striking the ship, shields included, and a new strike restarts the 3 seconds. Phase and armor arcs share `ArcDischarge`: each arc goes to the nearest enemy ship (not strikecraft) within 600 su, cycling when there are fewer ships than arcs, dealing 100/150/200/300 energy and as much EMP by the ship's hull size (placeholders); the modifier value is the arc count (3 for the coil, 1 per 500 armor damage for the plate). Armor damage taken builds the plate's discharges, and with no enemy in range one discharge is held for the next armor hit. Strikecraft EMP goes on hull and armor hits only, since EMP does nothing to shields.

Storm Front battles in a Storm Belt system give every hull item they drop its Storm Belt modifier (`StormLoot`).

## Build order

1. [x] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN). Built: rows ending `_STORM_BELT`, names in `core/zh_CN.json` as `socketable.modifier.*`.
2. [x] The new effects, each in its ship part's `skilleffect` package with tests: the fleet-wide hyperspace slowdown, the two Midline `AffinityScaling` rows, the EMP-hit speed burst, the EMP-to-hard-flux vent, the phase-exit arcs, strikecraft EMP and the armor arc. Built: `TERRAIN_MOVEMENT_PENALTY_PERCENT` (`FleetWideEffects.applyTerrainMovementPenalty`), the two rows in `SpecialSkillEffect`, `EMP_ARC_STRIKE_SPEED_PERCENT` (`movement.ArcStrikeSpeedListener`), `EMP_DAMAGE_TAKEN_HARD_FLUX_VENT_PERCENT` (`flux.EmpHardFluxVentListener`), `PHASE_EXIT_EMP_ARCS_FLAT` (`phase.PhaseExitArcListener`), `STRIKECRAFT_EMP_DAMAGE_PERCENT` (`fighter.StrikecraftEmpListener`, through `HitDamageRestorer.addEmpForThisHit`) and `ARMOR_DAMAGE_EMP_ARCS_FLAT` (`defense.ArmorArcListener`), with the shared `skilleffect.ArcDischarge`.
3. [x] `docs/docs.md` for the non-obvious ones (the phase and armor arcs). Built: the Storm Belt modifiers section.

## Decided

- Navigation's stat is the commander's `nav_penalty_mult`, not a fleet stat; it stacks flat (summed, then multiplied into each terrain penalty), so the line says reduced.
