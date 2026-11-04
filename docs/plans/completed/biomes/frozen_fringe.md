# Frozen Fringe modifiers

Hull items dropped in Frozen Fringe systems can carry a Frozen Fringe modifier. The biome is cold, wide systems around dim stars, so its set is about patience and stillness, reach and staying efficient while the ship holds still or coasts, and cold-running High Tech: three of its modifiers grant or read High Tech Affinity.

Status: built 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_FROZEN_FRINGE`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 10% of hull items dropped here. Values are placeholders.

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Patient (prefix) | new `DAMAGE_TO_DISTANT_TARGETS_PERCENT`: increased damage to targets more than 1000 su away | 5-10% | wide, empty systems |
| Specialist Crew | Overwintered (prefix) | new `MOVE_SLOW_SUPPLY_USE_MULT`, fleet-wide: less supply use for maintenance while moving slowly | 3-5% less | rationing through the long dark |
| Drive Assembly | Superconducting (prefix) | existing `HIGH_TECH_AFFINITY_FLAT` | 30-40 | drive coils that only work this cold |
| Reactor Regulator | Cryo-cooled (prefix) | new `FLUX_DISSIPATION_WITHOUT_HARD_FLUX_PERCENT`: increased flux dissipation with no hard flux | 10-15% | cold radiators |
| Fire Control Suite | of the Frost (suffix) | existing `ENERGY_WEAPON_DAMAGE_PERCENT_PER_HIGH_TECH_AFFINITY` | 1% per 5 High Tech Affinity | supercooled emitters |
| Shield Emitter | Glacial (prefix) | new `SHIELD_UPKEEP_AT_LOW_SPEED_PERCENT`: reduced shield upkeep while nearly stationary (below 10% of top speed) | 30-40% | stillness |
| Phase Coil Array | of the Long Night (suffix) | new `PEAK_TIME_PAUSED_WHILE_PHASED`: peak CR timer paused while phased | | cold sleep |
| Launch Assembly | Cold-stored (prefix) | new `FIGHTER_REPLACEMENT_RECOVERY_WHILE_REGROUPING_PERCENT`: increased replacement rate recovery while regrouping | 20-30% | wings kept on ice |
| Armor Plate | Permafrost (prefix) | new `ARMOR_PERCENT_PER_HIGH_TECH_AFFINITY` | 1% per 10 High Tech Affinity | a frozen layer that never cracks |

Overwintered: fleet-wide while vanilla counts the player fleet as moving slowly (`Misc.isSlowMoving`, which includes standing still, at or below the go-slow burn level that the Rift's "of the Depths" drive raises). It is a multiplier on every ship's monthly maintenance supply use, from the strongest carrying ship only. Only the player fleet gets it, since AI fleets use no supplies.

The Drive Assembly's affinity feeds the Fire Control Suite and Armor Plate on the same ship. Of the Frost is the existing High Tech affinity effect, covering beam and non-beam energy weapons. Permafrost is a new `SpecialSkillEffect` row beside `ARMOR_PERCENT_PER_LOW_TECH_AFFINITY`, an `AffinityScaling` on the High Tech filter with the large step (10).

Patient's distance is measured from this ship to the target ship's center at the moment of the hit, so missiles and long-range weapons both count; the weapon's own range doesn't matter. Glacial's value is the speed threshold as a percent of top speed (without the zero-flux boost); only the ship's speed is checked, so a ship turning in place still counts as stationary. Of the Long Night holds the ship's time deployed at its phase-entry value, so phased time doesn't use up peak performance; once peak time has run out, combat readiness still decays as normal. Cold-stored scales vanilla's `replacement_rate_increase_mult` while the carrier's strikecraft are set to regroup.

## Build order

1. [x] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN). Built: rows beside the Abyssal Rift's; modifier names in `core/zh_CN.json`.
2. [x] The new effects, each in its ship part's `skilleffect` package with tests, and `ARMOR_PERCENT_PER_HIGH_TECH_AFFINITY` in `SpecialSkillEffect`. Built: `WeaponSkillEffect.DAMAGE_TO_DISTANT_TARGETS_PERCENT` (`DistantTargetDamageListener`), `LogisticsSkillEffect.MOVE_SLOW_SUPPLY_USE_MULT` (`FleetWideEffects.applyMoveSlowSupplyUse`, run by `MoveSlowSupplyScript` from the mod plugin), `FluxSkillEffect.FLUX_DISSIPATION_WITHOUT_HARD_FLUX_PERCENT`, `ShieldSkillEffect.SHIELD_UPKEEP_AT_LOW_SPEED_PERCENT` (`LowSpeed`), `PhaseSkillEffect.PEAK_TIME_PAUSED_WHILE_PHASED` (`PhasedPeakTimeListener`), `FighterSkillEffect.FIGHTER_REPLACEMENT_RECOVERY_WHILE_REGROUPING_PERCENT`.
3. [x] `docs/docs.md` for the slow-moving supply cut. Built: the Frozen Fringe modifiers section.
