# Remnant Expanse modifiers

Hull items dropped in Remnant Expanse systems can carry a Remnant Expanse modifier. The biome is Remnant space, so its set is about networked machines: bonuses from nearby allies and shared targets, ion damage, fortress shields and self-repair.

Status: proposed 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_REMNANT_EXPANSE`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 30% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Relay-linked (prefix) | new: increased damage to a shared target | 5-10% when an ally targets it too | networked fire control |
| Specialist Crew | of the Purge (suffix) | existing `DAMAGE_TO_AUTOMATED_SHIP_PERCENT` | 10-15% | crews who learned to hunt the machines |
| Drive Assembly | Lattice (prefix) | new: increased top speed per nearby ally | 3% per ally within 1000 su, up to 15% | formation flight |
| Reactor Regulator | of the Nexus (suffix) | new: dissipation clears hard flux while shields are up | 10-15% of flux dissipation | Remnant shield tech |
| Fire Control Suite | Ion-patterned (prefix) | new: increased EMP damage | 15-25% | Remnant ion weapons |
| Shield Emitter | Fortress-tuned (prefix) | new: reduced shield damage taken while not firing | 20-30% | the Fortress Shield |
| Phase Coil Array | of the Relay (suffix) | new: nav rating while phased | +2 | a phased battle relay |
| Launch Assembly | Swarm-linked (prefix) | new: fighter damage per nearby friendly fighter | 2% per fighter within 500 su, up to 20% | drone swarms |
| Armor Plate | Self-mending (prefix) | new: armor regenerates on cells not hit recently | 0.5% per second after 5 s unhit | nanite repair |

Relay-linked: a target counts as shared when another allied ship has it as its ship target. Lattice counts allied ships only, not fighters. Of the Relay adds to the fleet's nav rating while the ship is phased, as a nav buoy does.

Of the Nexus: a share of the ship's flux dissipation applies to hard flux while its shield is up, as vanilla's hard-flux dissipation fraction does.

## Build order

1. [ ] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
2. [ ] The new effects, each in its ship part's `skilleffect` package with tests.
3. [ ] `docs/docs.md` for the shared target and the phased nav rating.

## Open questions

- Whether the Specialist Crew gets a modifier here. The Abyssal Rift gives it none because "the abyss is all AI"; Remnant space is all AI too. Of the Purge frames it as the crew learning to fight them instead.
- Whether Lattice and Swarm-linked are too alike. Both count nearby allies; one could count allied ships targeting the same enemy instead.
