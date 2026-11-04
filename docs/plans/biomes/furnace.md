# Furnace modifiers

Hull items dropped in Furnace systems can carry a Furnace modifier. The biome is hot, tight systems around giant stars, so its set rewards running hot and old Low Tech: bonuses at high flux, burning damage, hull that reflows in the heat, and Low Tech Affinity on three of its items.

Status: proposed 2026-11-04. The framework is built (`../completed/anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_FURNACE`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 30% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

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
| Launch Assembly | Cinderwing (prefix) | new: destroyed fighters burst | fragmentation damage within 150 su | cinders |
| Armor Plate | Slag-cast (prefix) | new: hull regenerates at high flux | 0.5% per second above 50% flux, up to 20% of maximum hull per combat | a hull that reflows when hot |

Sunward: every ship carrying it reduces the whole fleet's `CORONA_EFFECT_MULT`, which vanilla applies to star corona, flare and hyperspace storm CR loss, with no cap, so 20 carrying ships make the fleet immune. It is the fleet-wide form of the existing per-ship `CORONA_RESISTANCE_MULT`, synced per fleet like the Rift's `MOVE_SLOW_BURN_LEVEL_FLAT`.

Of the Bellows raises the ship's summed Low Tech Affinity in `AffinitySteps.totals`, so it multiplies Stoker's affinity and every other source. Kiln-fired is a new `AffinityScaling` row in `SpecialSkillEffect` on the Low Tech filter with the large step (10); it needs a plain hull damage taken stat in `defense` as its target, which doesn't exist yet.

Of the Furnace: the burn is a separate damage-over-time on the hit armor cell, not a modifier on the hit, and several burns on one cell add up. Cinderwing's burst damages enemy ships and fighters only. Slag-cast follows `NANOFORGE_HULL_REGEN_PERCENT`'s per-combat limit.

## Build order

1. [ ] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
2. [ ] The new effects, each in its ship part's `skilleffect` package with tests, the Low Tech Affinity increase in `AffinitySteps`, and the hull damage taken stat with its `AffinityScaling` row.
3. [ ] `docs/docs.md` for the burn, the Low Tech Affinity increase and the hull regeneration.

## Open questions

- Whether Of the Bellows applies before or after `HIGHEST_AFFINITY_FLAT_PER_S_MOD` and the affinity exchanges.
- Whether Sunward should also reduce pulsar beam CR loss, if vanilla runs that through the same stat.
