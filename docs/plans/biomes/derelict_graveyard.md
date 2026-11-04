# Derelict Graveyard modifiers

Hull items dropped in Derelict Graveyard systems can carry a Derelict Graveyard modifier. The biome is old battlefields and dead fleets, so its set rewards scarred ships and feeds on destruction: bonuses per d-mod, damage to damaged hulls, and repairs and fighters taken from ships dying nearby.

Status: proposed 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_DERELICT_GRAVEYARD`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 30% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | of the Boneyard (suffix) | new, fleet-wide: ships recovered after battle come with fewer d-mods | 1 fewer | knowing which wrecks are worth towing |
| Specialist Crew | Grave-robbing (prefix) | new: more weapons and fighter wings in post-battle salvage | 10-20% | stripping the dead |
| Drive Assembly | of the Hulk (suffix) | new: reduced fuel and supply use per d-mod | 3% per d-mod | a patched drive that runs lean |
| Reactor Regulator | Jury-rigged (prefix) | new: increased flux dissipation per d-mod | 2-3% per d-mod | bypassed safeties |
| Fire Control Suite | of the Graveyard (suffix) | new: increased damage to ships with d-mods | 10-15% | aiming for old wounds |
| Shield Emitter | Derelict-patched (prefix) | new: reduced shield damage taken from automated ships | 10-15% | tuned against Domain derelicts |
| Phase Coil Array | of the Wreck (suffix) | new: no phase upkeep near a hulk | within 500 su | hiding among the dead |
| Launch Assembly | Reclaimer (prefix) | new: a nearby enemy ship destroyed restores replacement rate | 10% per ship within 1500 su | fighters rebuilt from fresh wreckage |
| Armor Plate | Hulk-plated (prefix) | new: a nearby enemy ship destroyed repairs armor | 5% of every cell per ship within 1000 su | plates cut from fresh wrecks |

The per-d-mod effects sit beside the existing `TOP_SPEED_MULT_PER_DMOD` and `WEAPON_DAMAGE_MULT_PER_DMOD` and count d-mods the same way.

Command Suite: when ship recovery runs after a battle in a fleet holding one, each recovered ship loses one d-mod it would have rolled, never going below none. More than one in the fleet doesn't stack. It needs a hook at recovery time; check whether vanilla exposes the d-mod roll or whether the d-mod has to come off afterwards.

Grave-robbing: raises the weapons and wings in the post-battle loot only, not other salvage. Read at post-battle loot time as `POST_BATTLE_SALVAGE_PERCENT` is (`starsector_salvage_bonus_read_timing`).

## Build order

1. [ ] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
2. [ ] The new effects, each in its ship part's `skilleffect` package with tests.
3. [ ] `docs/docs.md` for the recovery d-mod cut and the salvage share.

## Open questions

- Whether "ship with d-mods" for the Fire Control Suite should scale per d-mod instead, like the other three.
- Whether the Phase Coil Array should count derelict hulls from before the battle, or only wrecks made in it.
