# Furnace modifiers

Hull items dropped in Furnace systems can carry a Furnace modifier. The biome is hot, tight systems around giant stars, so its set rewards running hot and old Low Tech: bonuses at high flux, burning damage, hull that reflows in the heat, and Low Tech Affinity on three of its items.

Status: built 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_FURNACE`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 10% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Sunward (prefix) | new, fleet-wide: reduced corona and storm CR loss | 5% per ship | flare-watch officers |
| Specialist Crew | Stoker (prefix) | existing `LOW_TECH_AFFINITY_FLAT` | 30-40 | crews who shovel the old way |
| Drive Assembly | of the Bellows (suffix) | new: increased Low Tech Affinity | 10-20% | old drives stoked hot |
| Reactor Regulator | Molten (prefix) | new: flux dissipation rises with hard flux | 1% per 5% hard flux | a reactor that runs best hot |
| Fire Control Suite | of the Furnace (suffix) | new: hits on armor burn | 3-5% of the hit again over 3 s | incendiary rounds |
| Shield Emitter | Kiln-fired (prefix) | new: `HULL_DAMAGE_TAKEN_PERCENT_PER_LOW_TECH_AFFINITY` | 1% reduced per 10 Low Tech Affinity | a hull fired like pottery |
| Phase Coil Array | of the Sun (suffix) | new: entering phase turns hard flux into soft flux | 10-20% of hard flux | heat shed into p-space |
| Launch Assembly | Cinderwing (prefix) | new: destroyed fighters burst | 100-150 fragmentation damage within 150 su | cinders |
| Armor Plate | Slag-cast (prefix) | new: hull regenerates at high flux | 0.5% per second above 50% flux, up to 20% of maximum hull per deployment | a hull that reflows when hot |

Sunward: every ship carrying it reduces the whole fleet's `CORONA_EFFECT_MULT`, which vanilla applies to star corona, flare and hyperspace storm CR loss, with no cap, so 20 carrying ships make the fleet immune. It is the fleet-wide form of the existing per-ship `CORONA_RESISTANCE_MULT`, synced per fleet like the Rift's `MOVE_SLOW_BURN_LEVEL_FLAT`.

Of the Bellows raises the ship's summed Low Tech Affinity in `AffinitySteps.totals`, so it multiplies Stoker's affinity and every other source. Kiln-fired is a new `AffinityScaling` row in `SpecialSkillEffect` on the Low Tech filter with the large step (10); it needs a plain hull damage taken stat in `defense` as its target, which doesn't exist yet.

Of the Furnace: the burn is a separate damage-over-time on the hit armor cell, not a modifier on the hit, and several burns on one cell add up. Cinderwing's burst damages enemy ships and fighters only. Slag-cast follows `NANOFORGE_HULL_REGEN_PERCENT`'s limit, which is per deployment.

As built: vanilla reads `corona_resistance` per fleet member, so Sunward sums the fleet's active carriers into one multiplier on every member's stat at each fleet sync, floored at immunity (100%). The burn strips only the struck cell's armor (it never reaches hull once the cell is bare) and comes from weapon hits on enemy ships, beside Disintegration through the shared `ArmorDecay`. Molten steps in whole 5% of hard flux. The Cinderwing placeholder had no damage number, so it rolls 100-150.

## Build order

1. [x] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
2. [x] The new effects, each in its ship part's `skilleffect` package with tests, the Low Tech Affinity increase in `AffinitySteps`, and the hull damage taken stat with its `AffinityScaling` row. Built: `FLEET_CORONA_RESISTANCE_PERCENT` (`FleetWideEffects.applyCoronaResistance`), `LOW_TECH_AFFINITY_PERCENT` (`AffinitySteps.totals`), `FLUX_DISSIPATION_PERCENT_PER_HARD_FLUX` (`flux.HardFluxDissipationListener`), `ARMOR_BURN_DAMAGE_PERCENT` (`onhit.ArmorBurnListener`), `HULL_DAMAGE_TAKEN_PERCENT` and `HULL_DAMAGE_TAKEN_PERCENT_PER_LOW_TECH_AFFINITY`, `PHASE_ENTRY_HARD_FLUX_TO_SOFT_PERCENT` (`phase.PhaseEntryFluxShedListener`), `STRIKECRAFT_DEATH_BURST_DAMAGE_FLAT` (`fighter.CinderBurstListener`), `HIGH_FLUX_HULL_REGEN_PERCENT` (`flux.HighFluxMendingListener`).
3. [x] `docs/docs.md` for the burn, the Low Tech Affinity increase and the hull regeneration (Affinity, Disintegration and Furnace modifiers sections).

## Decisions

- Of the Bellows applies to the summed Low Tech Affinity before `HIGHEST_AFFINITY_FLAT_PER_S_MOD` and the affinity exchanges, so the s-mod bonus and exchanged affinity are not increased.
- Sunward also reduces pulsar beam CR loss: vanilla's pulsar beam reads the same `corona_resistance` stat, as the per-ship `CORONA_RESISTANCE_MULT` already does.
