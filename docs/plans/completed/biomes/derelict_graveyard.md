# Derelict Graveyard modifiers

Hull items dropped in Derelict Graveyard systems can carry a Derelict Graveyard modifier. The biome is old battlefields and dead fleets, so its set rewards scarred ships and feeds on destruction: bonuses per d-mod, damage to pristine hulls, and repairs and fighters taken from ships dying nearby.

Status: built 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_DERELICT_GRAVEYARD`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 10% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | of the Boneyard (suffix) | new, fleet-wide: chance for recovered ships to avoid d-mods | 5% | knowing which wrecks are worth towing |
| Specialist Crew | Grave-robbing (prefix) | new: more weapons and fighter wings in post-battle salvage | 5-10% | stripping the dead |
| Drive Assembly | of the Hulk (suffix) | new: reduced fuel and supply use per d-mod | 3% per d-mod | a patched drive that runs lean |
| Reactor Regulator | Jury-rigged (prefix) | new: increased flux dissipation per d-mod | 2-3% per d-mod | bypassed safeties |
| Fire Control Suite | of the Graveyard (suffix) | new: increased damage to ships without d-mods | 5-10% | putting fresh hulls in the ground |
| Shield Emitter | Derelict-patched (prefix) | new: reduced shield damage taken from automated ships | 10-15% | tuned against Domain derelicts |
| Phase Coil Array | of the Wreck (suffix) | new: no phase upkeep near a hulk | within 500 su | hiding among the dead |
| Launch Assembly | Reclaimer (prefix) | new: a nearby enemy ship destroyed restores replacement rate | 10% per ship within 1500 su | fighters rebuilt from fresh wreckage |
| Armor Plate | Hulk-plated (prefix) | new: a nearby enemy ship destroyed repairs armor | 5% of every cell per ship within 1000 su, up to 10-15% of total armor per combat | plates cut from fresh wrecks |

The per-d-mod effects sit beside the existing `TOP_SPEED_MULT_PER_DMOD` and `WEAPON_DAMAGE_MULT_PER_DMOD` and count d-mods the same way.

Command Suite: when ship recovery runs after a battle in a fleet holding one, each recovered ship has a 5% chance to come with no d-mods from the battle. Vanilla exposes the roll: `FleetEncounterContext.getRecoverableShips` adds battle d-mods through `DModManager.addDMods`, which first asks the generic plugins for a `DModAdderPlugin`. Ours wins that pick when the recoverer is the player's fleet and the chance roll succeeds, and then adds nothing. The roll happens when the recoverable list is built, so it covers every ship offered for recovery, the player's own and the enemy's. Each active ship carrying it adds its chance, up to 30% for the fleet, as the Rift's `MOVE_SLOW_BURN_LEVEL_FLAT` adds up to its cap.

Hulk-plated stops repairing once it has restored 10-15% of the ship's total armor in that combat. The rolled value is that cap; the 5% per wreck is fixed.

Grave-robbing: raises the weapons and wings in the post-battle loot only, not other salvage. Read at post-battle loot time as `POST_BATTLE_SALVAGE_PERCENT` is (`starsector_salvage_bonus_read_timing`): when the loot is generated, every weapon and wing stack in it gains the fleet's summed share, the fraction rolled. Ships carrying it add up with no cap, as the salvage bonus does.

Effect lines follow how each value combines: the per-d-mod lines are their own multipliers ("more", "less"), as is the shield reduction ("less shield damage taken"); the damage bonus to ships without d-mods and the salvage share are summed ("increased"); the replacement rate gain is flat points ("+10% to strikecraft replacement rate").

## Build order

1. [x] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
2. [x] The new effects, each in its ship part's `skilleffect` package with tests. Built: `RECOVERED_SHIP_DMOD_AVOID_CHANCE_PERCENT` (`logistics.RecoveryDModAvoidance`, registered on game load), `POST_BATTLE_WEAPON_SALVAGE_PERCENT` (`logistics.SalvagedArmaments`, called from `ItemLootListener.reportEncounterLootGenerated`), `FUEL_AND_SUPPLY_USE_MULT_PER_DMOD` (logistics), `FLUX_DISSIPATION_MULT_PER_DMOD` (flux), `DAMAGE_TO_PRISTINE_SHIP_PERCENT` (`weapon.PristineShipDamageListener`), `SHIELD_DAMAGE_TAKEN_FROM_AUTOMATED_MULT` (`shield.AutomatedShieldDamageListener`), `NO_PHASE_UPKEEP_NEAR_WRECK_RANGE` (`phase.WreckShelterListener`), `NEARBY_DESTRUCTION_REPLACEMENT_RATE_PERCENT` and `NEARBY_DESTRUCTION_ARMOR_REPAIR_CAP_PERCENT` (`aura.WreckReclaimListener`, `aura.WreckPlatingListener`). The capped armor repair moved into `ArmorCells.repairWithinCap`, shared with the Rift's phase-entry repair.
3. [x] `docs/docs.md` for the recovery d-mod chance, the armor repair cap and the salvage share (Derelict Graveyard modifiers).

## Decisions

- The Command Suite's chance stacks per ship in the fleet, like the Rift's `MOVE_SLOW_BURN_LEVEL_FLAT`, capped at 30% (`RecoveryDModAvoidance.MAX_CHANCE_PERCENT`).
- The Phase Coil Array counts any non-strikecraft hulk on the battlefield within range. Vanilla battles bring no derelicts from before the fight, so in practice these are wrecks made in it.
