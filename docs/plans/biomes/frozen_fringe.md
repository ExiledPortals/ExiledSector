# Frozen Fringe modifiers

Hull items dropped in Frozen Fringe systems can carry a Frozen Fringe modifier. The biome is cold, wide systems around dim stars, so its set is about patience and stillness: reach, slowing enemies, and staying efficient while the ship holds still or coasts.

Status: proposed 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_FROZEN_FRINGE`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 30% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Patient (prefix) | new: increased damage to distant targets | 5-10% beyond 1000 su | wide, empty systems |
| Specialist Crew | Overwintered (prefix) | new, fleet-wide: reduced supply use while moving slowly | 10-15% | rationing through the long dark |
| Drive Assembly | Coldstart (prefix) | new: flamed-out engines relight faster | 30-50% | engines that start frozen |
| Reactor Regulator | Cryo-cooled (prefix) | new: increased flux dissipation with no hard flux | 10-15% | cold radiators |
| Fire Control Suite | of the Frost (suffix) | new: non-missile hits slow the target | 3% for 3 s, up to 15% | frost rounds |
| Shield Emitter | Glacial (prefix) | new: no shield upkeep while nearly stationary | below 10% of top speed | stillness |
| Phase Coil Array | of the Long Night (suffix) | new: peak CR timer paused while phased | | cold sleep |
| Launch Assembly | Cold-stored (prefix) | new: faster replacement rate recovery while regrouping | 25-40% | wings kept on ice |
| Armor Plate | Permafrost (prefix) | new: higher minimum armor fraction | 10-15% of maximum | a frozen layer that never cracks |

Overwintered: fleet-wide while the fleet counts as moving slowly (vanilla's go-slow burn level, which the Rift's "of the Depths" drive raises). It is a multiplier on the fleet's monthly supply use, from the strongest carrying ship only.

Of the Frost: stacks on the target, refreshed by each hit, from every ship with it, up to 15% in total. Permafrost raises vanilla's minimum armor fraction (5%), the share of an armor cell's maximum that still reduces damage once the cell is stripped.

## Build order

1. [ ] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
2. [ ] The new effects, each in its ship part's `skilleffect` package with tests.
3. [ ] `docs/docs.md` for the frost stacks and the minimum armor fraction.

## Open questions

- Whether "distant" for Patient is measured from the ship or from each weapon's base range.
- Whether Glacial should also count the ship holding position while it turns.
