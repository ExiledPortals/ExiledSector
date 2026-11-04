# Threat Expanse modifiers

Hull items dropped in Threat Expanse systems can carry a Threat Expanse modifier. The biome is abyssal hyperspace holding only Threat fleets, so its set is the cold machine that unmakes things: no regard for its own readiness, stripping armor, eating what it hits, and fragments.

Status: proposed 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_THREAT_EXPANSE`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 30% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Dispassionate (prefix) | new: low CR causes no malfunctions | weapon and engine malfunctions only | indifference |
| Specialist Crew | none, or Witnessed (prefix) | existing `CREW_LOSS_PERCENT` | 20% reduced | crews who have seen the Threat |
| Drive Assembly | of the Hunt (suffix) | new: increased top speed closing on an enemy | 15-20% within 2000 su | relentless pursuit |
| Reactor Regulator | Consuming (prefix) | new: hull damage dealt vents flux | 1-2% of the damage | feeding on what it breaks |
| Fire Control Suite | of Unmaking (suffix) | new: hull hits strip the struck cell's maximum armor | 1% per hit, up to 20%, for the combat | disassembly |
| Shield Emitter | Fractal (prefix) | new: blocked damage spawns a fragment | one per 1500 damage blocked | fragments |
| Phase Coil Array | of the Void (suffix) | new: leaving phase next to an enemy damages it | energy damage within 300 su | stepping out of nothing |
| Launch Assembly | Hive-fed (prefix) | new: destroyed fighters leave a fragment | lasting 10 s | the hive |
| Armor Plate | Unmade (prefix) | new: reduced damage taken from ships with no crew | 10-15% | built to face machines |

Dispassionate: removes the weapon and engine malfunction chances vanilla adds at low CR. It doesn't stop CR falling or the other low-CR penalties. Check first that vanilla applies those chances as stat modifiers this can cancel.

Fractal and Hive-fed spawn a short-lived fragment drone of ours that attacks the nearest enemy, not vanilla's Fragment Swarm, which only works on ships with the Threat's hull mod (and which `FRAGMENT_SWARM_SIZE_PERCENT` already raises). Of Unmaking lowers the struck cell's maximum, so repairs can't bring it back that combat.

## Build order

1. [ ] The nine rows (eight if the crew gets none) in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
2. [ ] The new effects, each in its ship part's `skilleffect` package with tests, and the shared fragment drone used by the shield and the launch assembly.
3. [ ] `docs/docs.md` for the fragments, the armor stripping and the malfunction removal.

## Open questions

- Whether the Specialist Crew gets a modifier. The Abyssal Rift gives it none because "the abyss is all AI", and the Threat is all machine too.
- Whether the fragment drone reuses vanilla's fragment art and weapon, and how it fits with Fast Rendering (GL11 only, render thread only).
