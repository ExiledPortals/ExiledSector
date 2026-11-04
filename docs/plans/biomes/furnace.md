# Furnace modifiers

Hull items dropped in Furnace systems can carry a Furnace modifier. The biome is hot, tight systems around giant stars, so its set rewards running hot: bonuses at high flux, burning damage, and turning heat back on attackers.

Status: proposed 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_FURNACE`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 30% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Sunward (prefix) | new, fleet-wide: reduced corona and storm CR loss | 10% per ship, up to 50% | flare-watch officers |
| Specialist Crew | Stoker (prefix) | new: slower CR loss while flux is above 50% | 15-25% | crews who work in the heat |
| Drive Assembly | of the Flare (suffix) | new: exhaust damages enemies close behind | fragmentation damage per second within 300 su | a flaming wake |
| Reactor Regulator | Molten (prefix) | new: flux dissipation rises with hard flux | 1% per 5% hard flux | a reactor that runs best hot |
| Fire Control Suite | of the Furnace (suffix) | new: hits on armor burn | 10-15% of the hit again over 3 s | incendiary rounds |
| Shield Emitter | Flare-shrouded (prefix) | new: shield flux taken added to the attacker as soft flux | 10-15% | heat thrown back |
| Phase Coil Array | of the Sun (suffix) | new: entering phase turns hard flux into soft flux | 10-20% of hard flux | heat shed into p-space |
| Launch Assembly | Cinderwing (prefix) | new: destroyed fighters burst | fragmentation damage within 150 su | cinders |
| Armor Plate | Slag-cast (prefix) | new: armor regenerates at high flux | 0.5% per second above 50% flux | plates that reflow when hot |

Sunward: every ship carrying it reduces the whole fleet's `CORONA_EFFECT_MULT`, which vanilla applies to star corona, flare and hyperspace storm CR loss, up to 50% for the fleet. It is the fleet-wide form of the existing per-ship `CORONA_RESISTANCE_MULT`, synced per fleet like the Rift's `MOVE_SLOW_BURN_LEVEL_FLAT`.

Of the Furnace: the burn is a separate damage-over-time on the hit armor cell, not a modifier on the hit. Several burns on one cell add up. Of the Flare damages only enemies inside a cone behind the engines while they burn.

## Build order

1. [ ] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
2. [ ] The new effects, each in its ship part's `skilleffect` package with tests.
3. [ ] `docs/docs.md` for the burn, the exhaust and the shield's flux return.

## Open questions

- Whether Flare-shrouded's returned flux counts against beams, which hit the shield every frame.
- Whether Sunward should also reduce pulsar beam CR loss, if vanilla runs that through the same stat.
