# Frozen Fringe modifiers

Hull items dropped in Frozen Fringe systems can carry a Frozen Fringe modifier. The biome is cold, wide systems around dim stars, so its set is about patience and stillness, reach and staying efficient while the ship holds still or coasts, and cold-running High Tech: three of its modifiers grant or read High Tech Affinity.

Status: proposed 2026-11-04. The framework is built (`../completed/anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_FROZEN_FRINGE`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 30% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Patient (prefix) | new: increased damage to distant targets | 5-10% beyond 1000 su | wide, empty systems |
| Specialist Crew | Overwintered (prefix) | new, fleet-wide: reduced supply use while moving slowly | 3-5% | rationing through the long dark |
| Drive Assembly | Superconducting (prefix) | existing `HIGH_TECH_AFFINITY_FLAT` | 30-40 | drive coils that only work this cold |
| Reactor Regulator | Cryo-cooled (prefix) | new: increased flux dissipation with no hard flux | 10-15% | cold radiators |
| Fire Control Suite | of the Frost (suffix) | existing `ENERGY_WEAPON_DAMAGE_PERCENT_PER_HIGH_TECH_AFFINITY` | 1% per 5 High Tech Affinity | supercooled emitters |
| Shield Emitter | Glacial (prefix) | new: no shield upkeep while nearly stationary | below 10% of top speed | stillness |
| Phase Coil Array | of the Long Night (suffix) | new: peak CR timer paused while phased | | cold sleep |
| Launch Assembly | Cold-stored (prefix) | new: faster replacement rate recovery while regrouping | 20-30% | wings kept on ice |
| Armor Plate | Permafrost (prefix) | new: `ARMOR_PERCENT_PER_HIGH_TECH_AFFINITY` | 1% per 10 High Tech Affinity | a frozen layer that never cracks |

Overwintered: fleet-wide while the fleet counts as moving slowly (vanilla's go-slow burn level, which the Rift's "of the Depths" drive raises). It is a multiplier on the fleet's monthly supply use, from the strongest carrying ship only.

The Drive Assembly's affinity feeds the Fire Control Suite and Armor Plate on the same ship. Of the Frost is the existing High Tech affinity effect, covering beam and non-beam energy weapons. Permafrost is a new `SpecialSkillEffect` row beside `ARMOR_PERCENT_PER_LOW_TECH_AFFINITY`, an `AffinityScaling` on the High Tech filter with the large step (10).

## Build order

1. [ ] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
2. [ ] The new effects, each in its ship part's `skilleffect` package with tests, and `ARMOR_PERCENT_PER_HIGH_TECH_AFFINITY` in `SpecialSkillEffect`.
3. [ ] `docs/docs.md` for the slow-moving supply cut.

## Open questions

- Whether "distant" for Patient is measured from the ship or from each weapon's base range.
- Whether Glacial should also count the ship holding position while it turns.
