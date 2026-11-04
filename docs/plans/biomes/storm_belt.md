# Storm Belt modifiers

Hull items dropped in Storm Belt systems can carry a Storm Belt modifier. The biome is deep hyperspace cloud and frequent storms, so its set is about lightning, taking EMP and turning it to use and arcing it back out, and the Midline workhorses that ply the storms: three of its items grant or read Midline Affinity.

Status: proposed 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_STORM_BELT`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 30% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Stormwise (prefix) | new, fleet-wide: reduced deep hyperspace slowdown | 5% per ship, up to 50% | storm pilots |
| Specialist Crew | Storm-seasoned (prefix) | existing `MIDLINE_AFFINITY_FLAT` | 30-40 | crews of the hulls that work the storms |
| Drive Assembly | of the Gale (suffix) | new: speed burst after an EMP arc hits the ship | 20% top speed and maneuverability for 3 s | riding the strike |
| Reactor Regulator | Grounded (prefix) | new: EMP damage taken vents hard flux | 20-30% of the EMP damage | grounding the charge |
| Fire Control Suite | of the Tempest (suffix) | new: `WEAPON_FIRE_RATE_PERCENT_PER_MIDLINE_AFFINITY` | 1% increased weapon fire rate per 10 Midline Affinity | a hail of fire |
| Shield Emitter | Faraday (prefix) | new: `SHIELD_PIERCE_CHANCE_PERCENT_PER_MIDLINE_AFFINITY` | 1% chance to avoid EMP arcs piercing per 10 Midline Affinity | a Faraday cage |
| Phase Coil Array | of the Eye (suffix) | new: leaving phase discharges EMP arcs | 3 arcs at enemies within 600 su | the eye of the storm |
| Launch Assembly | Thunderwing (prefix) | new: strikecraft hits deal extra EMP | 10-15% of the hit's damage | charged wings |
| Armor Plate | Charged (prefix) | new: absorbed damage builds into an EMP arc | one arc per 500 damage the armor absorbs | a lightning rod |

Command Suite: every ship carrying it reduces the burn level the whole fleet loses inside deep hyperspace clouds, as the Navigation skill does, up to 50% for the fleet. It is a flat modifier on the fleet stat that skill uses, synced per fleet like the Rift's `MOVE_SLOW_BURN_LEVEL_FLAT`. Storm strike CR loss is left out: vanilla's `CORONA_EFFECT_MULT` already covers it, and the Furnace uses that stat.

Of the Tempest and Faraday are new `AffinityScaling` rows in `SpecialSkillEffect` on the Midline filter with the large step (10), targeting the all-weapons rate of fire (`WeaponStatFamily.RATE_OF_FIRE`) and a reduction to `SHIELD_PIERCE_CHANCE_PERCENT`; the crew's affinity feeds both. Phase Coil Array and Armor Plate arcs hit the nearest enemies in range, with damage scaled by hull size (placeholder).

These stay separate from the Storm Front anomaly's Stormproof and Thunderhead modifiers, which are reserved to that anomaly.

## Build order

1. [ ] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
2. [ ] The new effects, each in its ship part's `skilleffect` package with tests: the fleet-wide hyperspace slowdown, the two Midline `AffinityScaling` rows, the EMP-hit speed burst, the EMP-to-hard-flux vent, the phase-exit arcs, strikecraft EMP and the armor arc.
3. [ ] `docs/docs.md` for the non-obvious ones (the phase and armor arcs).

## Open questions

- Which fleet stat vanilla's Navigation skill uses for the deep hyperspace slowdown, and whether it stacks flat or multiplies.
