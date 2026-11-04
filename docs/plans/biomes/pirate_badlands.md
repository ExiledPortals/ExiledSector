# Pirate Badlands modifiers

Hull items dropped in Pirate Badlands systems can carry a Pirate Badlands modifier. The biome is pirate country, so its set is about ambush and kicking ships that are down: opening strikes, bonuses against crippled or overloaded targets, running away, ramming and smuggling. Like the pirate doctrine, one modifier carries a downside.

Status: proposed 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_PIRATE_BADLANDS`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 30% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Ambusher's (prefix) | new: increased damage early in combat | 10-15% for the first 30 s | the opening volley |
| Specialist Crew | Press-ganged (prefix) | new: enemy survivors join the crew after a won battle | share of enemy crew lost, up to capacity | pressed into service |
| Drive Assembly | of the Getaway (suffix) | new: increased top speed at low hull | 20-30% below 50% hull | knowing when to run |
| Reactor Regulator | Bootleg (prefix) | hybrid: `FLUX_DISSIPATION_PERCENT` and `PEAK_CR_DURATION_PERCENT` | 15-20%, and 5-10% reduced | a hot-wired reactor |
| Fire Control Suite | of Plunder (suffix) | new: increased damage to helpless ships | 10-20% to overloaded, venting or flamed-out ships | kicking them while down |
| Shield Emitter | Hit-and-run (prefix) | new: reduced shield damage taken at speed | 15-20% above 75% top speed | never sitting still |
| Phase Coil Array | of the Smuggler (suffix) | new, fleet-wide: reduced sensor profile with the transponder off | 10% per ship, up to 30% | running dark |
| Launch Assembly | Reaver (prefix) | new: strikecraft deal increased damage to damaged ships | 15-20% below 50% hull | finishing runs |
| Armor Plate | Ram-plated (prefix) | new: collisions hurt enemies more and this ship less | 50% more dealt, 50% less taken | a ram prow |

Press-ganged: after a won battle, a share of the crew the enemy lost (placeholder 10%) joins the fleet, limited by free crew capacity. It counts the crew of ships destroyed or disabled while this ship was deployed, and more than one in the fleet doesn't stack. Of the Smuggler is a flat modifier on the fleet's sensor profile while the transponder is off, synced per fleet like the Rift's `MOVE_SLOW_BURN_LEVEL_FLAT`.

Ram-plated: applied in a damage listener on collision damage only, both ways.

## Build order

1. [ ] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
2. [ ] The new effects, each in its ship part's `skilleffect` package with tests.
3. [ ] `docs/docs.md` for the press-ganging and the collision damage.

## Open questions

- Whether a biome modifier should carry a downside at all. Bootleg is the only one in any biome.
- Whether Press-ganged should give marines instead of crew, or a mix.
