# Threat Expanse modifiers

Hull items dropped in Threat Expanse systems can carry a Threat Expanse modifier. The biome is abyssal hyperspace holding only Threat fleets, so every modifier works through vanilla's Fragment Swarm: growing it, regrowing it, and spending its fragments in place of flux, armor or replacement rate. Two of them scale with affinity of every kind, since the Threat learns from all three lines alike.

Status: proposed 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_THREAT_EXPANSE`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 30% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | of the Swarm (suffix) | new, fleet-wide: increased sensor strength per ship with a Fragment Swarm | 1% per ship in the fleet | the swarms keep watch |
| Specialist Crew | Witnessed (prefix) | new: `FRAGMENT_SWARM_SIZE_PERCENT_PER_AFFINITY` | 1% increased swarm size per 5 affinity of every kind | crews who have watched the swarms |
| Drive Assembly | of the Hunt (suffix) | new: increased top speed while the swarm is nearly full | 10-15% at 90% of the swarm's size or more | a full swarm on the hunt |
| Reactor Regulator | Fabricating (prefix) | new: `FRAGMENT_REPLACEMENT_RATE_PERCENT_PER_AFFINITY` | 1% increased fragment replacement rate per 5 affinity of every kind | a reactor feeding the fabricator |
| Fire Control Suite | of Unmaking (suffix) | new: hull damage dealt regrows fragments | 1 fragment per 300 hull damage | eating what it breaks |
| Shield Emitter | Fractal (prefix) | new: fragments absorb shield damage | 10-15% reduced shield damage taken, spending 1 fragment per 30 damage absorbed | fragments thrown in front of shots |
| Phase Coil Array | of the Void (suffix) | new: fragments pay phase upkeep | 50% of the upkeep, 1 fragment per 50 flux | spending the swarm to stay unseen |
| Launch Assembly | Hive-fed (prefix) | new: lost fighters cost fragments instead of replacement rate | 5 fragments per fighter | the hive rebuilds its own |
| Armor Plate | Unmade (prefix) | new: fragments the full swarm can't hold repair armor instead | each repairs 0.5% of maximum armor on the most damaged cell, up to 10% of maximum armor per combat | the swarm patches the hull |

Every modifier but of the Swarm needs the ship to have a swarm (`RoilingSwarmEffect.getSwarmFor`) and does nothing without one; the tooltip says so. A ship has one from vanilla's Fragment Swarm hull mod or the Threat volume's Fragment Swarm notable. Of the Swarm counts the fleet's ships with either and raises the fleet's sensor strength, synced per fleet like the Rift's `MOVE_SLOW_BURN_LEVEL_FLAT`. Vanilla sizes the swarm from `fragment_swarm_size_mod` (20, 40, 60 or 100 by hull size) and regrows it from `fragment_swarm_respawn_rate_mult` (1, 2, 3 or 5), which the existing `FRAGMENT_SWARM_SIZE_PERCENT` and `FRAGMENT_REPLACEMENT_RATE_PERCENT` already raise. Witnessed and Fabricating are `AffinityScaling` rows in `SpecialSkillEffect` on the all-affinities filter with the small step (5), targeting those two.

Fragments are spent with `despawnMembers` and added with `addMembers`, never above the swarm's maintained size, and an effect with too few fragments left does nothing. Of the Void halves phase upkeep while the swarm holds fragments and spends one per 50 flux it saved. Hive-fed restores the replacement rate a destroyed fighter cost, if the swarm has 5 fragments to spend. Unmade counts the fragments vanilla's regrowth would add while the swarm is already full and turns each into armor repair, until it has restored 10% of the ship's maximum armor that combat.

## Build order

1. [ ] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
2. [ ] The new effects, each in its ship part's `skilleffect` package with tests: a shared swarm helper (find, spend, add up to size) beside the existing fragment effects in `shipsystem`, the two `AffinityScaling` rows, and the fleet-wide sensor strength.
3. [ ] `docs/docs.md` for the fleet's swarm count, the fragment spending and the armor repair from a full swarm.

## Open questions

- Whether several ships carrying of the Swarm stack, or the fleet counts it once.
- Whether the affinity effects should read the ship's highest affinity instead of all three summed.
- Whether Fractal should skip beam hits, which reach the shield every frame and would drain the swarm steadily.
