# Module skill tree share: plan

Ships with station modules (Navarchy of Tania plates, Knights of Ludd, Roider Union, UAF and others) leave their modules behind today: the tree applies only to the parent. This plan gives every module a share of the parent's tree, so a plate gets, say, half of the parent's flux, armor, weapon and defence bonuses. Nothing is doubled on the parent, and nothing with its own behaviour (auras, drones, death effects, once-per-battle saves, campaign stats) is copied.

All numbers are placeholders, like every other magnitude in the mod. `docs/analysis/balance.csv` is left alone.

Status: planned, nothing built. The Tania volume is built on `parallel-work`. Since its 2026-10-22 redesign it has no module nodes; `MODULE_HULL_PERCENT` and `MODULE_ARMOR_PERCENT` remain as unused building blocks.

## Decided so far

- The parent pushes a share of its resolved tree onto each module at deployment. The tree hull mod is not put on module variants, and phantom hull mods are never placed on modules.
- Only effects known to be static and useful on a module are shared. Everything else stays on the parent.
- A node whose hull mod the module already has (fitted by the player or built in) gives that module no share.
- The Tania nodes stay parent-only.

## How modules work in the engine

These facts were checked against `starfarer_obf.jar` (decompile cache in `E:\Dev\Tools\decompile-cache`) and `starfarer.api.zip`. They decide most of the design.

- Every module is its own ship with its own variant, stats, flux pool, shield, weapons, armor and hull. It rides the parent and never moves on its own. It usually has no engines and no ship system; Black Hawk plates have Phaedra Canisters and Aracari plates have Asperitas Damper.
- In combat each module gets a new throwaway fleet member called "temp", built from the module variant with a fresh id on every deployment (`loading.specs._:252-253`). `module.getFleetMember()`, `getFleetMemberId()` and the module's `stats.getFleetMember()` all return it. It has no fleet data and no fleet commander. Its captain getter returns a default person, but the module ship's captain is the parent's captain object, so personal skills apply to the module.
- The parent is built first, with both creation hooks. Then the modules are spawned, then each module gets its saved status (dead modules become hulks, detached ones are removed), then the parent alone is advanced ten times while holding fire (`CombatFleetManager:1041-1124`). The parent's AdvanceableListeners run in those ticks, so a parent listener sees its modules before the fight starts.
- A module's own hull mods run their `applyEffectsAfterShipCreation` before the parent link is set, so `getParentStation()` is null there.
- `parent.getVariant().getModuleVariant(slot)` returns the shared global stock variant until that module has been customised (`HullVariantSpec:1731-1748`). Writing a hull mod or tag onto it changes every ship using that stock module, NPCs included, for the session, and the change isn't saved.
- A module keeps its own hull size. Tania plates match their parent (Merite, Aetos destroyer; Argentavis, Touraco, Aracari cruiser; Black Hawk capital). Vanilla Onslaught Mk.I Vambrace modules are frigates on a capital.
- CR is copied from the parent once at spawn, then decays on the module's own peak time. Tania plates overwrite module CR with the parent's every frame.
- When a module fires, the module is the damage source: the parent's DamageDealtModifiers never see module shots. When a module is hit, only its own listeners hear about it. Damage to a module is credited to the parent's fleet member.
- Fighters from module bays are launched by the module. Only the module variant's hull mods get `applyEffectsToFighterSpawnedByShip`, so our fighter effects never reach them today.
- Battle-wide counting: ECM and nav ratings skip modules, but the ECM range penalty does hit them, so module ECCM matters. Command point recovery (`CommRelayScript`) and objective capture count every module. Deployed strength and pursuit checks skip modules.
- Fleet logistics (cargo, fuel, crew, sensors, survey, salvage, corona, recovery, CR repair) read real fleet members only. Module stats are never read for them.
- Between engagements the parent's `FleetMemberStatus` stores each module's hull (clamped to 0..1) and armor cells (cell / `getMaxArmorInCell()`, not clamped). Armor raised above the cell max in one engagement starts the next one already raised. Campaign repair clears values above 1 on its first tick.
- A module is untargetable while its `getHullDamageTakenMult` is 0 or less (`Ship:5075-5089`).
- `Misc.isActiveModule` calls a module with 0 OP, no weapon groups and no bays inactive (vanilla Vambraces). Inactive modules are pure armor.

## Design

### When the share is applied

A new `ModuleTreeShare` in `exiledsector.effects` (it needs the package-private parts of `ResolvedTree`) is attached by `SkillTreeHullMod.applyEffectsAfterShipCreation` to any ship whose hull has station-module slots, unless the ship is a station. On the parent's first tick it reads `getChildModulesCopy()` and pushes the share onto every living module. Modules that are dead or detached at deployment are skipped. Modules can't appear later in a battle, so one pass is enough.

`ModuleBonusListener` (the Tania module nodes) is folded into the same pass, so hull and armor are rescaled once. Two rescalers in a row would clamp away each other's boost.

### What a module gets

The share is computed from the parent's allocated nodes, not from the pooled tree entries, because pooled MULT entries have no node identity left. For each allocated node:

1. Skip it if it is a temporary node, a vanilla passthrough (`vanillaHullMod`), or classed EXCLUDE as a whole (see "Nodes shared all or nothing").
2. Skip it on a module that already has any of the node's phantom hull mods of its own (`InstalledHullMods.hasHullModOfItsOwn` on the module variant) or any hull mod in the node's `exclusiveHullMods`. Example: an Argentavis plate has Dedicated Targeting Core built in, so the parent's Dedicated Targeting Core node gives it nothing instead of +70% range.
3. Take the node's effects at the parent's hull size, keep only the effects classed SHARE or SHARE-REBUILD, multiply each magnitude by the share, and pool MULT effects the same way `ResolvedTree` does.
4. Apply the pooled result to the module's stats under one mod id per effect (`exiledSector_moduleShare_<parentId>_<effect>`).

Positive "increased" and "more" values scale linearly. The damage-taken caps are re-applied to the module's pooled totals, so a parent sitting at the 80% cap gives the module half of the cap, not half of the raw total.

### Share size

A LunaLib setting, `exiledSector_moduleTreeShare`, from 0 to 100% in steps of 5, default 50%. At 0 nothing is pushed and the listener isn't attached. The Tania module nodes are not scaled by it.

### Hull size

Effects are resolved at the parent's hull size, from the parent's already resolved tree, so no second resolve and no cache thrash. For Tania this is the module's size anyway. On vanilla Vambraces (frigate modules on a capital) a capital's flat armor at 50% is large for a frigate plate. That is accepted for the first build and listed as an open question.

### Creation-time stats

Armor, hull, shield arc, ammo capacity and fighter bays are fixed when the ship is built.

- Hull: raise the module's hull bonus, then `setMaxHitpoints` and `setHitpoints` by the effective ratio.
- Armor: raise the armor bonus, then set every cell to `min(cell, getMaxArmorInCell()) × ratio`. The clamp stops the boost compounding through `FleetMemberStatus` between engagements.
- Ammo: per weapon `setMaxAmmo` and `resetAmmo`, the way `StatTarget.PerWeaponAmmo.overrideWeapon` does it.
- Shield arc: `getShield().setArc` when the module has a shield.
- PD reclassify: call the effect's `applyAfterShipCreation` on the module.
- Fighter bays, weapon durability (no max-health setter found) and anything else fixed at build: not shared.

`getMaxArmorInCell` keeps its creation value, so armor repair (Repair Coordinator, Heartless) still caps against the unboosted maximum. That is the same as the fighter armor effect today and is accepted.

### Listeners

No listener effect is shared in the first build. A few are sane per ship and could follow later (see NEEDS-TEST in the effect catalogue): Beam Hard Flux, Disintegration, Medium Energy Slot Range, ABSORB Reserve, Ion Arc. Each needs `ensureListener` on the module and its key written on the module's stats.

### Tooltip

One line in the tree's summary on ships with modules: "This ship's modules have N% of its weapon, flux and defence bonuses." It names the categories, not every effect. The exact wording follows `docs/tooltip_wording.md` once the whitelist is final.

### NPCs

NPC trees resolve the same way, so NPC modular ships get the share automatically, on top of `NpcBonusScale`. Stations get no trees (`NpcShipSelector.java:14`) and are skipped anyway.

### What is never done

- Never write to a module variant. Doing so mutates the stock variant shared by every ship (see engine facts), and our sync, phantom placement and conflict resolver all assume one variant per ship.
- Never run `HullModConflictResolver`, `PhantomConflictWatch` or `ShipTreeSync` on a module. They would strip the player's own hull mods from plates (Heavy Armor, which Tania rewards) or learn bogus conflicts under the module hull id.
- Never key anything per fleet member on a module. Use `getParentStation()` and the parent's member.

## Nodes shared all or nothing

Some nodes pair an upside with a downside on different effects. A per-effect whitelist could hand a module only one half. These nodes are therefore decided as a whole.

| Node | Effects | Decision | Why |
|---|---|---|---|
| shared_fate | shield damage shared +20, weapon damage -20 | EXCLUDE | The -20 damage pays for flux sharing, which isn't shared. A plate would get only the penalty. |
| safety_overrides | dissipation ×2, vent ×0, range past 450 ×0.25, speed, zero-flux, peak CR | EXCLUDE | The range cap would be a pure penalty on plates, the mobility that pays for it does nothing, and Tania strips Safety Overrides from its ships. |
| missleracks | missile ammo +100%, missile RoF -15% | SHARE both | Ammo needs the rebuild; the RoF penalty must come with it. |
| adaptiveshields | arc -30%, convert to omni | EXCLUDE | Conversion isn't shared, so an already-omni module would get only the arc cut. |
| frontemitter | arc +100%, convert to front | EXCLUDE | Conversion isn't shared, so a module with a front shield would get +100% arc free. |
| advancedoptics | beam range +200, beam turn ×0.7 | SHARE both | Range and turn travel together. |
| high_scatter_amp | beam damage +10%, hard flux on beams, beam range past 200 halved | EXCLUDE | The hard-flux part is a listener and isn't shared; the module would keep the range penalty. |
| unstable_injector | range -15%, fighter refit +25%, top speed | EXCLUDE | Only the penalties would land on a module. |
| nskr_volatile | flux-scaled RoF and speed, +67% capacity and capacitors, range past 800 halved, fighter range -75% | EXCLUDE | Plates idle near zero flux and would sit at almost permanent +40% RoF; the 800 range cap is a pure penalty. Revisit by sharing only the capacity part. |
| heavyarmor | armor +30%, hull flat, maneuver -20% | SHARE armor and hull | The maneuver penalty is a no-op on modules. Accepted: modules don't move, so it costs them nothing. |
| frontshield | creates a front shield, speed ×0.8 | EXCLUDE | Creating shields on plates is structural, and Tania blocks makeshift shields. |
| shield_shunt | armor +15%, removes the shield | EXCLUDE | Removing module shields is destructive; giving shieldless plates the armor alone is free value vanilla never allows. |
| reckless_assault | includes armor -10%, crew loss +50% | Share the combat effects, both signs | Penalties on whitelisted stats travel with the node. |
| exogrid | includes vent -25%, dissipation -5 per vent | Share both | Same rule. |
| seraphs_grace | repair time -20%, shield damage taken +20% (pooled MULT) | Share both | Same rule. |
| torpedo_specialisation | missile speed +30%, missile maneuver -30% | Share both | Same rule. |

General rule: a node is shared only if every effect on it is SHARE, SHARE-REBUILD, or a no-op on modules (movement, campaign). A node whose upside is an excluded effect is excluded whole, so its penalty never travels alone.

## Catalogue 1: phantom hull mod nodes on modules

Our phantom nodes don't run the hull mod's own code. The node places the real id on the parent's variant as a tagged perma mod, `PhantomHullModEffect` skips the original effect on tagged copies, and the node's real effect is our own `SkillEffect`s. Only five nodes run vanilla code directly: `ballistic_rangefinder`, `defensive_targeting_array`, `missile_autoloader`, `neural_interface` and `fragment_swarm`.

So "applying the hull mod to a module" can mean two things, and the table covers both:

- "Real id on module": placing the hull mod id on the module variant, as the parent has it. Even inert, other mods' `hasHullMod` checks react to it, vanilla applicability is bypassed, and the five passthroughs would run vanilla code on the module.
- "Share": pushing the node's own effects onto the module's stats, as this plan does.

Parent phantoms already reach Tania plates in two cases: the plate hull mod checks the parent for Point Defense AI and Solar Shielding (`tania_Navarchy_Armor_Plate.java:189-198`) and passes their effects on. Only Heavy Armor is read from the module's own variant.

| Node (hull mod) | Real id on module: what goes wrong | Share: what the node's effects do on a module | Treatment |
|---|---|---|---|
| hbi | Nothing reacts to it. On other mods' modules it would lower module OP and could leave the module over OP when the node is removed. | Large ballistic OP cost only matters in refit; Tania plates have no large slots. | EXCLUDE |
| ground_support | Nothing reacts. | Ground support is summed over real fleet members only; the node is campaign-only. | EXCLUDE |
| safety_overrides (safetyoverrides) | Black Hawk plates are capital, so vanilla's "no capitals" rule is bypassed. Ancient Frame strips Safety Overrides from Tania mains. | Speed, boost and CR do nothing; vent ×0 duplicates the plate; the plate gets dissipation ×2 and loses 75% of range past 450. | EXCLUDE (all or nothing) |
| advancedshieldemitter | Nothing reacts. | Shield turn and unfold rates; no-op on shieldless plates. | SHARE (NEEDS-TEST: whether turn and unfold are read live) |
| adaptive_coils | Nothing reacts. | Phase flux threshold; modules never phase. | EXCLUDE (no-op) |
| advancedoptics | Nothing reacts. | Beam range and beam turn; works on module beams, mostly no-op on Tania composite slots. | SHARE (both effects) |
| turretgyros | Nothing reacts; the plate's s-mod bonus reads the parent's s-mods only. | Weapon turn rate, live. | SHARE (NEEDS-TEST: turn rate live) |
| armoredweapons | Nothing reacts. | Armor % (rebuild), weapon durability (fixed at build), turn ×0.75, recoil ×0.75. | SHARE armor, turn and recoil; durability left out |
| autorepair | Nothing reacts. | Weapon repair time works; engine part is a no-op. | SHARE |
| auxiliarythrusters | Nothing reacts. | Modules have speed and acceleration 0. | EXCLUDE (no-op) |
| ballistic_rangefinder | Vanilla passthrough runs on the module: its range listener sizes off the largest ballistic slot. Tania slots are composite, so no listener; on modules with real ballistic slots each module gets its own. Argentavis mains already have it built in. | Passthrough, not shareable. | EXCLUDE |
| blast_doors | Nothing reacts. | Hull +20% (rebuild); crew loss is counted on real members only. | SHARE hull |
| converted_hangar | Tania gives the parent -35% top speed per plate with Converted Hangar while the plate lives, and never removes it (`:110-112`). Example: an Argentavis with it on both plates loses 70% top speed and gains no bay. Vanilla code on a 0-supplies plate would also compute infinite CR per deployment if Tania didn't suppress it. | Bays are fixed at build and the module has no wing. | EXCLUDE |
| converted_fighterbay | Nothing reacts. | Removes built-in-wing bays and adds cargo; plates have neither. | EXCLUDE |
| dedicated_targeting_core | Built into Argentavis and Aracari plates; next to built-in Distributed Fire Control on Touraco and Black Hawk plates, which vanilla forbids. | Node range ids differ from the built-in's, so an Argentavis plate would get +70% instead of +35%. Aetos and Merite plates are destroyers and get 0. | SHARE, skipped on modules with DTC, ITU, DFC or advancedcore |
| defensive_targeting_array | Built into Argentavis and Merite plates. Vanilla passthrough uses the same mod id and would overwrite the built-in copy. | Plates have no bays, so no fighters. | EXCLUDE |
| eccm | Nothing reacts; the plate reads the parent's ECCM s-mod only. | Useful: the ECM range penalty does hit modules, and the missile stats work on composite missiles. | SHARE |
| ecm | Ancient Frame's range synergy reads the main only. | ECM rating skips modules, so no contribution. | EXCLUDE (no-op) |
| expanded_deck_crew | Nothing reacts. | No bays on plates; crew is campaign-only. | EXCLUDE |
| magazines | Nothing reacts. | Ballistic and energy ammo +50%, needs the per-weapon rebuild. | SHARE-REBUILD |
| missleracks | Nothing reacts. | Missile ammo (rebuild) and the missile RoF penalty. | SHARE both (all or nothing) |
| extendedshieldemitter | Nothing reacts. | Shield arc is fixed at build; no-op on shieldless plates. | SHARE-REBUILD (`setArc`) |
| shield_shunt | Nothing reacts. | Removing shields from modules is destructive; armor alone would be free. | EXCLUDE |
| frontshield | Tania strips makeshift shields from plates through MagicLib with a warning. Our resolver can't match the free-text cause, and the next sync would re-add it, so it would churn on every build. | Every shieldless plate would grow a 90-degree shield on its halved flux. | EXCLUDE |
| missile_autoloader | Vanilla passthrough counts small missile slots; Tania composite slots don't count, so a no-op there. On modules with missile slots each module gets its own reload pool. | Passthrough, not shareable. | EXCLUDE |
| neural_interface | The player's neural link picks the first deployed member with the flag, and modules are in that list with the player as captain. Example: the link partner becomes a stationary Touraco plate. | Passthrough, not shareable. | EXCLUDE |
| hardenedshieldemitter | Nothing reacts. | Shield damage taken and pierce; no-op on shieldless plates. | SHARE |
| hardened_subsystems | Nothing reacts. | Peak CR and CR loss; Tania overwrites module CR every frame, other mods unknown. | EXCLUDE |
| heavyarmor | Tania gives the plate +75/150/200/250 flat armor when the module variant has Heavy Armor (`:54-56`). Placing the id is a real Tania synergy, but it's the player's choice to fit. | Armor +30% and hull flat (rebuild); maneuver penalty is a no-op. | SHARE armor and hull; skipped on a module with real Heavy Armor |
| high_scatter_amp | Nothing reacts. | The hard-flux listener would have to sit on the module, which isn't shared; the range penalty would land alone. | EXCLUDE (all or nothing) |
| nskr_criticalArmor | Untagged, LOST_SECTOR's code strips Heavy Armor from the plate. | Flat armor for low base armor, evaluated on the module's own armor and size: Argentavis plate +483, Merite plate +443, the others 0. Its `exclusiveHullMods` includes heavyarmor. | SHARE-REBUILD, skipped on modules with Heavy Armor |
| nskr_inertial | Nothing reacts. | Damage from the module's own velocity; unknown whether a module reports the parent's velocity or zero. | EXCLUDE (listener; NEEDS-TEST later) |
| nskr_acoils | Nothing reacts. | Medium energy slot range (listener) and -10% energy flux cost. | SHARE the flux cost; range listener later |
| nskr_absorption | Nothing reacts. | RoF from shield damage absorbed; no-op on shieldless plates. | EXCLUDE (listener; later for shielded modules) |
| nskr_volatile | Untagged, strips Safety Overrides. | Near-permanent +40% RoF on idle plates, 800 range cap, afterimages every 0.05 s per module. | EXCLUDE (all or nothing) |
| nskr_bigBats | Nothing reacts. | +1 system charge: Black Hawk Phaedra goes 3 to 4; Aracari Damper has no charges. A 50% share makes +0.5, which rounds. | NEEDS-TEST (rounding), gated on the module system having charges |
| pointdefenseai | Tania already passes PD AI from the parent to plates (flare ignore, target leading, +50% to missiles and fighters). | Flags would duplicate harmlessly; the small-weapon PD reclassify is new on modules. | SHARE the reclassify only; the flags are already covered by Tania |
| targetingunit | Untagged, the refit says it isn't applicable. | Range by hull size: an Argentavis plate would stack it with its built-in DTC, which vanilla forbids; capital plates get the capital +40%. | SHARE, skipped on modules with DTC, DFC or advancedcore |
| nav_relay | Nothing reacts. | Nav rating skips modules. | EXCLUDE (no-op) |
| frontemitter | Nothing reacts. | Arc +100% without the conversion. | EXCLUDE (all or nothing) |
| adaptiveshields | Nothing reacts. | Arc -30% without the conversion. | EXCLUDE (all or nothing) |
| operations_center | Vanilla code reads the temp member's commander, which is null, so it would stay off. | If it ever applied, command point recovery would be summed once per module. | EXCLUDE |
| phase_anchor | Untagged vanilla code crashes on a non-phase module: `PhaseAnchorScript.advance` calls `getPhaseCloak().getSpecAPI()` without a null check. It also uses the battle-wide dive. | Our dive listener would dive a phased module off-screen, call `setRetreating` on it and use up the one dive per battle. | EXCLUDE |
| recovery_shuttles | Nothing reacts. | Fighter crew loss from module bays isn't counted at all. | EXCLUDE (no-op) |
| reinforcedhull | Nothing reacts. | Hull +40% (rebuild): a Black Hawk plate gains about 6000 hull. Recovery chance is campaign-only. | SHARE hull |
| fluxbreakers | Nothing reacts. | EMP taken ×0.7 works; the vent part is a no-op on Tania plates (vent ×0). | SHARE |
| stabilizedshieldemitter | Nothing reacts. | Shield upkeep; no-op on shieldless plates. | SHARE |
| unstable_injector | Nothing reacts. | Only the penalties land. | EXCLUDE (all or nothing) |
| additional_berthing | Nothing reacts. | Crew is campaign-only. | EXCLUDE (no-op) |
| augmentedengines | Nothing reacts. | Burn level is campaign-only. | EXCLUDE (no-op) |
| auxiliary_fuel_tanks | Nothing reacts. | Fuel is campaign-only. | EXCLUDE (no-op) |
| efficiency_overhaul | Nothing reacts. | All campaign stats. Unverified whether raising min crew on a temp member trips a crew-based CR penalty on modules with crew. | EXCLUDE |
| expanded_cargo_holds | Nothing reacts. | Cargo is campaign-only. | EXCLUDE (no-op) |
| insulatedengine | Nothing reacts. | Hull +10% (rebuild); engines and sensor profile don't apply. | SHARE hull |
| militarized_subsystems | Vanilla code reads the temp member and likely gives 0. | Campaign stats, plus the same min crew question. | EXCLUDE |
| solar_shielding | Tania already passes it from the parent (-10% energy damage taken on plates). | Corona resistance is campaign-only. | EXCLUDE (Tania covers it) |
| surveying_equipment | Nothing reacts. | Survey costs are summed over real members. | EXCLUDE (no-op) |
| fragment_swarm | Tania strips it from plates through MagicLib. The free-text cause would make `PhantomConflictWatch` learn a bogus conflict and could refund the parent's node, and MagicLib adds its warning hull mod to the plate. | Vanilla passthrough: one swarm per module (capital plates 100 fragments), bypassing the node's item cost. | EXCLUDE |
| secondary_fabricator | Nothing reacts. | No swarm on modules. | EXCLUDE (no-op) |
| fragment_coordinator | Nothing reacts. | No swarm on modules. | EXCLUDE (no-op) |

## Catalogue 2: every effect used by the tree

Of the 232 effects used in `skill_types.json`, this is how each is treated on modules. SHARE effects need no explanation beyond their name. Every other effect has its reason or an example of what would go wrong.

### SHARE

Weapons: `BALLISTIC_WEAPON_DAMAGE_PERCENT`, `BALLISTIC_WEAPON_PROJECTILE_SPEED_PERCENT`, `BALLISTIC_WEAPON_RANGE_MULT`, `BALLISTIC_WEAPON_RANGE_PERCENT`, `BEAM_WEAPON_DAMAGE_PERCENT`, `BEAM_WEAPON_RANGE_FLAT`, `BEAM_WEAPON_RANGE_MULT`, `BEAM_WEAPON_RANGE_PERCENT`, `BEAM_WEAPON_TURN_RATE_MULT`, `ENERGY_WEAPON_DAMAGE_PERCENT`, `ENERGY_WEAPON_FIRE_RATE_PERCENT`, `ENERGY_WEAPON_FLUX_COST_PERCENT`, `ENERGY_WEAPON_RANGE_FLAT`, `ENERGY_WEAPON_RANGE_MULT`, `ENERGY_WEAPON_RANGE_PERCENT`, `MISSILE_WEAPON_DAMAGE_PERCENT`, `MISSILE_WEAPON_FIRE_RATE_PERCENT`, `MISSILE_WEAPON_PROJECTILE_SPEED_PERCENT`, `MISSILE_WEAPON_FLIGHT_ACCELERATION_PERCENT`, `MISSILE_WEAPON_FLIGHT_MANEUVERABILITY_PERCENT`, `MISSILE_WEAPON_FLIGHT_TURN_ACCELERATION_PERCENT`, `MISSILE_WEAPON_FLIGHT_TURN_RATE_PERCENT`, `MISSILE_WEAPON_ECCM_CHANCE_PERCENT`, `NON_BEAM_ENERGY_WEAPON_DAMAGE_PERCENT`, `NON_BEAM_ENERGY_WEAPON_RANGE_FLAT`, `WEAPON_DAMAGE_PERCENT`, `WEAPON_AUTOFIRE_ACCURACY_PERCENT`, `WEAPON_RECOIL_MULT`, `DAMAGE_TO_CAPITAL_PERCENT`, `DAMAGE_TO_CRUISER_PERCENT`, `PD_DAMAGE_TO_MISSILES_PERCENT`, `PD_BEST_TARGET_LEADING`, `PD_IGNORES_DECOY_FLARES`.

Flux: `FLUX_CAPACITY_FLAT`, `FLUX_CAPACITY_MULT`, `FLUX_CAPACITY_PERCENT`, `FLUX_DISSIPATION_FLAT`, `FLUX_DISSIPATION_MULT`, `FLUX_DISSIPATION_PERCENT`, `FLUX_CAPACITOR_EFFECTIVENESS_PERCENT`, `FLUX_DISSIPATION_PER_FLUX_VENT_FLAT`.

Defence: `EMP_DAMAGE_TAKEN_MULT`, `EMP_DAMAGE_TAKEN_PERCENT`, `ENERGY_DAMAGE_TAKEN_PERCENT`, `SHIELD_DAMAGE_TAKEN_MULT`, `SHIELD_PIERCE_CHANCE_MULT`, `SHIELD_UPKEEP_MULT`, `ELECTRONIC_WARFARE_PENALTY_MULT`, `REPAIR_TIME_MULT`, `REPAIR_TIME_PERCENT`, `BREAK_PROBABILITY_PERCENT`.

Systems and fighters, no-ops unless the module has them: `SYSTEM_CHARGE_REGEN_PERCENT`, `SYSTEM_COOLDOWN_PERCENT`, `PHASE_CLOAK_ACTIVATION_COST_MULT`, `PHASE_CLOAK_FLUX_THRESHOLD_PERCENT`, `FIGHTER_ENGAGEMENT_RANGE_MULT`, `FIGHTER_REFIT_TIME_PERCENT`, `FIGHTER_REPLACEMENT_DECAY_PERCENT`, `FIGHTER_REPLACEMENT_RECOVERY_PERCENT`.

Notes on the SHARE list:
- `NON_BEAM_ENERGY_*` must be pushed through `effect.apply` with the scaled magnitude, never written straight to the stat, so the hidden beam offset entry follows (`StatTarget.Compensated`).
- `PD_BEST_TARGET_LEADING` and `PD_IGNORES_DECOY_FLARES` ignore magnitude, so they apply in full at any share.
- `FLUX_CAPACITOR_EFFECTIVENESS_PERCENT` and `FLUX_DISSIPATION_PER_FLUX_VENT_FLAT` count the module's own capacitors and vents, usually none, so they are mostly no-ops.
- `ELECTRONIC_WARFARE_PENALTY_MULT` is useful: the ECM range penalty hits modules.
- `SHIELD_DAMAGE_TAKEN_MULT` pools upsides and downsides from several nodes (hardened -10, seraphs_grace +20). The module gets the share of the parent's net value.
- Tania plates already take up to 25% less damage from the parent's s-mods, and Asperitas Damper on top. Shared damage-taken reductions compound with both.

### SHARE-REBUILD

These are fixed at ship build and need the rebuild described under "Creation-time stats".

- `ARMOR_FLAT`, `ARMOR_PERCENT`: armor bonus, then clamped cell rescale. Example of the clamp mattering: without it, a plate at +10% would start the next engagement at 1.21×, then 1.331×.
- `ARMOR_FLAT_FOR_LOW_BASE_ARMOR`: evaluated on the module's own base armor and size (see nskr_criticalArmor above).
- `HULL_FLAT`, `HULL_MULT`, `HULL_PERCENT`: hull bonus, then `setMaxHitpoints` and `setHitpoints`.
- `BALLISTIC_WEAPON_AMMO_PERCENT`, `ENERGY_WEAPON_AMMO_PERCENT`, `MISSILE_WEAPON_AMMO_PERCENT`: per-weapon `setMaxAmmo` and `resetAmmo`.
- `SHIELD_ARC_FLAT`, `SHIELD_ARC_PERCENT`, `SHIELD_ARC_MULT`: `setArc` on modules with a shield. `SHIELD_ARC_MULT` is also the downside of adaptiveshields, which is excluded whole.
- `PD_RECLASSIFY_SMALL_WEAPONS`: `applyAfterShipCreation` on the module. Applies in full whatever the share.

### EXCLUDE: does nothing on a module

These are harmless if pushed but have no effect, so they're left out to save work.

- Movement, because modules don't move: `ACCELERATION_FLAT`, `DECELERATION_FLAT`, `MANEUVERABILITY_PERCENT`, `TOP_SPEED_FLAT`, `TOP_SPEED_MULT`, `TOP_SPEED_PERCENT`, `ZERO_FLUX_ALWAYS_ON`, `ZERO_FLUX_BOOST_FLUX_LEVEL_PERCENT`, `TIMEFLOW_PERCENT`. Timeflow could do harm as well: at a 50% share the module's weapons and flux would tick at a different rate from the parent it is locked to.
- Phase, because modules never phase: `AMMO_REGEN_WHILE_PHASED_PERCENT`, `COMBAT_BOOST_WHILE_PHASED`, `FLUX_DISSIPATION_WHILE_PHASED_PERCENT`. Each would also run a per-frame check for nothing.
- Engines, usually absent: `ENGINE_DURABILITY_PERCENT`.
- Campaign and fleet logistics, never read from module stats: `BURN_LEVEL_FLAT`, `CARGO_CAPACITY_FLAT`, `CARGO_CAPACITY_MULT`, `CARGO_CAPACITY_PERCENT`, `CARGO_CAPACITY_PER_FIGHTER_BAY`, `CORONA_RESISTANCE_MULT`, `CREW_CAPACITY_FLAT`, `CREW_CAPACITY_MULT`, `CREW_LOSS_MULT`, `CREW_LOSS_PERCENT`, `CR_RECOVERY_RATE_PERCENT`, `FUEL_CAPACITY_FLAT`, `FUEL_CAPACITY_MULT`, `FUEL_CAPACITY_PERCENT`, `FUEL_USE_MULT`, `GROUND_SUPPORT_PER_MAX_CREW_PERCENT`, `MAX_COMBAT_READINESS_PERCENT`, `MIN_CREW_MULT`, `MIN_CREW_PERCENT`, `MIN_CREW_PERCENT_PER_FIGHTER_BAY`, `MIN_CREW_PER_FIGHTER_BAY`, `PHASE_FIELD_CONTRIBUTION_PERCENT`, `PHASE_HULL_SENSOR_PROFILE_PERCENT`, `POST_BATTLE_SALVAGE_PERCENT`, `REMOVE_CIVILIAN_HULL_PENALTY`, `REPAIR_RATE_PER_DAY_PERCENT`, `SENSOR_PROFILE_MULT`, `SENSOR_PROFILE_PERCENT`, `SENSOR_STRENGTH_ALWAYS_COUNTS`, `SENSOR_STRENGTH_FLAT`, `SHIP_RECOVERY_CHANCE_BONUS`, `SUPPLIES_PER_MONTH_MULT`, `SURVEY_COST_REDUCTION_HEAVY_MACHINERY`, `SURVEY_COST_REDUCTION_SUPPLIES`, `CONVERTED_HANGAR_MIN_CREW_FLAT`.
- Battle-wide ratings that skip modules: `ELECTRONIC_WARFARE` (ECM rating), `NAV_RATING`.
- Refit-only: `BALLISTIC_WEAPON_LARGE_OP_COST_FLAT` (module variants have their own OP budget).
- Lost Sector Augmented checks, which read the module variant and are always false: `AUGMENTED_FLUX_SCALED_PENALTY_REDUCTION_PERCENT`, `AUGMENTED_INERTIAL_PROJECTILE_SPEED_PERCENT`, `AUGMENTED_SYSTEM_REGEN_PERCENT`.
- Threat swarm, absent on modules: `FRAGMENT_REPLACEMENT_RATE_PERCENT`, `FRAGMENT_SWARM_SIZE_PERCENT`.
- Fighter launch hooks, which never run for module-launched fighters: `BOMBER_PAYLOAD_DETONATION_CHANCE_PERCENT`, `BOMBER_ROLE_ACCELERATION_PERCENT`, `BOMBER_ROLE_TOP_SPEED_PERCENT`, `FIGHTER_FOCUS_FIRE_DAMAGE_PERCENT_PER_HIT`, `FIGHTER_FOCUS_FIRE_MAX_DAMAGE_PERCENT`, `FIGHTER_TOP_SPEED_PERCENT`, `FIGHTER_WEAPON_DAMAGE_PERCENT`, `FIGHTER_WING_DAMAGE_TAKEN_PERCENT_PER_FIGHTER`, `FIGHTER_WING_SHARED_DAMAGE_PERCENT`.
- Parent-only by design: `RAM_BIGGEST_ENEMY` (already returns early on modules), `TOP_SPEED_PERCENT_PER_DESTROYED_MODULE`, `MANEUVERABILITY_PERCENT_PER_DESTROYED_MODULE`, and the Tania `MODULE_HULL_PERCENT`, `MODULE_ARMOR_PERCENT`, `MODULE_WEAPON_DAMAGE_PERCENT`, `MODULE_WEAPON_RANGE_PERCENT`, `MODULE_WEAPON_RATE_OF_FIRE_PERCENT`, which are the parent-to-module channel themselves and would apply twice if copied.

### EXCLUDE: would misbehave on a module

| Effect | What goes wrong on a module |
|---|---|
| `REPAIR_COORDINATOR_ARMOR_PERCENT` | Each plate becomes another repair aura. The cap is per coordinator (10%, up to 30% per ally), so a Black Hawk's two plates triple its repair, and plates repair the parent and each other. |
| `MALFUNCTION_AURA_PERCENT` | Each plate is another aura source, so the 1000 su circle is centred on every plate and coverage widens. |
| `TERRIFYING_PRESENCE_ACCURACY_PENALTY_PERCENT` | Penalties stack per source (mod id includes the ship id), so a parent with two plates at 50% gives about twice the penalty to every nearby enemy. |
| `GRAVITY_DRAG_TIMEFLOW_PERCENT` | Sources sum per target up to 25%, so plates add slowing on top of the parent. |
| `ESCORT_MANEUVER_BONUS_PERCENT`, `ESCORT_SPEED_BONUS_PERCENT`, `ESCORT_WEAPON_RANGE_BONUS_PERCENT` | A module always "escorts" its own larger parent at zero distance, so a plate gets the full escort range bonus even when the parent fights alone. |
| `EXPLODE_ON_DEATH` (Ludd's Light) | Every destroyed plate detonates against its own parent. The blast reads the fuel of the module's temp member, so it is either an empty fireball or, if resolved to the parent, the parent's fuel × 2000% per plate. |
| `DEATH_ON_COLLISION` (At Any Cost) | Has no magnitude to scale. Any enemy touching a plate kills it. |
| `RISE_FROM_THE_ASHES_HULL_PERCENT`, `RISE_FROM_THE_ASHES_ARMOR_RESTORE_PERCENT`, `RISE_FROM_THE_ASHES_DAMAGE_TAKEN_PERCENT` | Keyed by fleet member id, and each module deployment gets a fresh temp id, so every plate would come back from death once per deployment. |
| `PHASE_ANCHOR_EMERGENCY_DIVE` | One dive per battle on both sides. A phased module would use it up, retreat off-screen and charge CR to its temp member. |
| `THERMO_ACCELERATOR_RATE_OF_FIRE_PERCENT` | Module hardpoints would never fire. The Aetos plate's only weapon is a hardpoint. |
| `TERMINAL_DESCENT_HULL_DAMAGE_TAKEN_PERCENT`, `TERMINAL_DESCENT_FLUX_DISSIPATION_PERCENT`, `TERMINAL_DESCENT_SYSTEM_REGEN_PERCENT`, `TERMINAL_DESCENT_SHIELD_DAMAGE_TAKEN_PERCENT` | Each plate enters descent on its own hull, blocks its own venting and loses peak time. |
| `HEARTLESS_ON_NEARBY_DESTRUCTION_RANGE_FLAT`, `HEARTLESS_MAX_STACKS_FLAT`, `HEARTLESS_ARMOR_PERCENT_PER_STACK`, `HEARTLESS_ARMOR_RESTORE_PERCENT`, `HEARTLESS_MOBILITY_PENALTY_PERCENT_PER_STACK`, `HEARTLESS_RADIATION_EMP_FLAT` | Every module polls for wrecks and stacks on its own, so one kill gives a stack per plate, and each stack's radiation burst EMPs one of the plate's own turrets. |
| `HEARTLESS_CREW_DEATH_CHANCE_PERCENT_PER_STACK`, `HEARTLESS_MISSILE_DAMAGE_PERCENT_PER_STACK`, `HEARTLESS_MISSILE_SPEED_PERCENT_PER_STACK` | Every missile a plate fires rolls against the player's fleet crew through the shared ledger. |
| `CREW_STEAL_RANGE_FLAT`, `CREW_STEAL_SKELETON_CREW_PERCENT` | Each module credits the same wreck to the fleet ledger, so a parent with two plates steals three times the crew per kill. |
| `SERAPHIM_HEAL_PERCENT`, `SERAPHIM_HEAL_CAP_PERCENT`, `SERAPHIM_DAMAGE_BONUS_PERCENT`, `SERAPHIM_FULL_BONUS_DAMAGE_FLAT` | Per-module ramps and heal pools. The threshold shares inverted: half the threshold means the plate reaches the full bonus twice as fast. |
| `SERAPHS_GRACE_MAX_STACKS_FLAT`, `SERAPHS_GRACE_PERCENT_PER_STACK` | Stacks and percent per stack both halve, so the plate gets 25%; plates take the most hits and stack fastest. |
| `SHIELD_DAMAGE_SHARED_PERCENT` | Plates and parent would pass shield flux in circles. |
| `NON_BEAM_ENERGY_WEAPON_CHAIN_CHANCE_PERCENT`, `NON_BEAM_ENERGY_WEAPON_CHAIN_FALLOFF_PERCENT` | Each module gets its own refraction drone pool, and the falloff shares inverted (milder), so the plate would chain better than the parent. |
| `BEAM_WEAPON_SPLIT_TARGETS_FLAT` (Lion's Gaze) | `Math.round(0.5)` is 1, so a 50% share is still a full split, with a drone pool per module. |
| `WING_SCRAMBLE_COPIES` | Would run on module bays and build temporary fleet members per wing per module. |
| `WEAPON_RANGE_THRESHOLD_FLAT`, `WEAPON_RANGE_FALLOFF_MULT` | Shares inverted: a 450 threshold becomes 225, a harsher cap than the parent's, while the falloff gets milder. |
| `CONVERT_SHIELD_TO_FRONT`, `CONVERT_SHIELD_TO_OMNI`, `CREATE_FRONT_SHIELD_IF_NONE`, `REMOVE_SHIELD` | Structural shield changes. Eligibility only ever saw the parent's shield, and plates would grow, lose or flip shields. |
| `REMOVE_ALL_FIGHTER_BAYS`, `CONVERTED_HANGAR_FIGHTER_BAYS_FLAT` | Bays are fixed at build. A late bay has no wing, and it flips `Misc.isActiveModule`. |
| `CONVERTED_HANGAR_REFIT_TIME_MULT`, `CONVERTED_HANGAR_RELAUNCH_TIME_FLAT`, `CONVERTED_HANGAR_REPLACEMENT_RATE_MULT` | A plate with its own bays would refit slower because the parent took Converted Hangar. |
| `DMOD_EFFECT_MULT` | Counts the module's own d-mods and re-runs the d-mod hull mods after the module is built, leaving creation-time stats stale. |
| `COMMAND_POINT_RECOVERY_WHILE_FLAGSHIP` | `CommRelayScript` sums command point rate over every deployed ship including modules, and the module's captain is the commander. Two plates would add their share on top of the parent's. |
| `OBJECTIVE_CAPTURE_RANGE_FLAT`, `OBJECTIVE_CAPTURE_RATE_MULT` | Objective capture counts modules, so a plate off the hull extends the parent's capture reach. |
| `COMBAT_VISION` | Each plate's vision circle duplicates the parent's. |
| `FLUX_SCALED_TOP_SPEED_FLAT` | No speed effect, but the afterimages still spawn every 0.05 s per module. |
| `CR_LOSS_PER_SECOND_MULT`, `PEAK_CR_DURATION_MULT`, `PEAK_CR_DURATION_PERCENT`, `VENT_RATE_MULT`, `VENT_RATE_PERCENT` | Tania overwrites module CR every frame and sets plate vent rate to 0; other mods' modules decay CR on their own. Left out until tested. |
| `SYSTEM_CHARGES_FLAT` | +1 becomes +0.5, which rounds; only meaningful on modules with a charge system (Black Hawk). |
| `MISSILE_WEAPON_GUIDANCE_FLAT` | A flag-like value; how the engine treats 0.5 is unknown. |
| `FIGHTER_CREW_LOSS_MULT` | Fighter crew loss from module bays isn't counted at all. |

### NEEDS-TEST

Left out of the first build. Each could be added once checked in game.

| Effect | What to test |
|---|---|
| `BEAM_WEAPON_HARD_FLUX_PERCENT`, `DISINTEGRATION_ARMOR_DAMAGE_PERCENT`, `ION_ARC_CHANCE_PERCENT`, `MEDIUM_ENERGY_SLOT_WEAPON_RANGE_FLAT`, `SHIELD_ABSORB_RATE_OF_FIRE_PERCENT` | Self-contained per hit or per weapon, so sane per module. Needs the listener installed on the module and its key written on module stats. |
| `BALLISTIC_WEAPON_PIERCE_CHANCE_PERCENT`, `CORROSION_MOTE_DAMAGE_FLAT` | Drone and mote caps are per source, so each module adds a full cap. |
| `FLUX_SCALED_RATE_OF_FIRE_PERCENT`, `FUROR_RATE_OF_FIRE_PERCENT`, `DYNAMIC_GRID_ENERGY_WEAPON_BONUS_PERCENT`, `NANOFORGE_HULL_REGEN_PERCENT` | Each runs on the module's own flux, damage dealt, shield or hull. Nanoforge captures its pool from max hull on the first tick, so the hull rebuild must come first. |
| `NON_BEAM_WEAPON_DAMAGE_PER_SPEED_PERCENT` | Whether a module reports the parent's velocity or zero. |
| `BALLISTIC_WEAPON_AMMO_REGEN_PERCENT`, `ENERGY_WEAPON_AMMO_REGEN_PERCENT` | Tagged non-live, but another effect writes the same stat mid-combat, so probably live. |
| `SHIELD_RAISE_RATE_PERCENT`, `SHIELD_TURN_RATE_PERCENT`, `WEAPON_TURN_RATE_MULT`, `WEAPON_TURN_RATE_PERCENT`, `WEAPON_DURABILITY_PERCENT` | Whether the engine reads them live or only at build. |
| `ARMOR_DAMAGE_TAKEN_MULT_PER_DMOD`, `WEAPON_DAMAGE_MULT_PER_DMOD`, `BALLISTIC_WEAPON_DAMAGE_PER_BURN_LEVEL_MULT`, `ENERGY_WEAPON_RANGE_PER_SENSOR_STRENGTH_FLAT` | They compute from the module's own d-mods, burn level or sensor strength, usually zero. To follow the parent, push the parent's computed result instead. |

## Edge cases

- Pooled MULT entries. `ResolvedTree` sums every node's MULT for one effect into a single entry with no node identity. That's why the share is built from allocated nodes, not from pooled entries.
- Damage-taken caps. Above the 80% cap the parent's entries are replaced by one cap entry. The share re-applies the cap to the module's own totals.
- Damage taken can never reach "100% less" through the tree, so a module can't be made untargetable. Keep it that way: a module at a hull damage multiplier of 0 is untargetable, and a station core then dies with its last module.
- Temporary nodes. Their entries sit in `entries()` and expire only on the parent, so they are skipped. Otherwise they'd stay on modules for the whole battle.
- Hull-size values. Resolved at the parent's size. Vanilla Vambraces (frigate modules on a capital) get capital-sized flats at the share.
- Inverted shares. Thresholds and penalties scaled down can get stronger, not weaker (range threshold, Seraphim threshold, chain falloff). Those are excluded.
- Flags and integers ignore the share (PD flags, beam split, shield conversion, system charges). Each is decided explicitly above.
- Detached and dead modules at deployment are skipped. A module can't come back mid-battle.
- Retreat and redeploy build fresh module ships and a fresh listener. The armor clamp keeps the stored status from compounding.
- Inactive modules (vanilla Vambraces: 0 OP, no weapons, no bays) only benefit from armor and hull. The share can skip everything else on them to save work.
- Tania plates halve flux capacity and dissipation and set vent rate to 0. Shared flux works on top of that, but never reaches the parent: Tania's flux grid reads the module's vent and capacitor counts, not its stats.
- Tania plates overwrite module CR every frame. No CR effects are shared.
- Tania plates already pass Point Defense AI and Solar Shielding from the parent. Their nodes' module shares are skipped so they aren't counted twice.
- Tania's Ancient Frame and Modular Operations Facility react to phantoms on the parent (`hasHullMod`). That is unchanged by this plan.
- Personal officer skills already apply to modules through the shared captain. The share stacks with them, as it does on the parent.
- Our refit Skill Tree button may open on a module tab (see "Found along the way"). It must never be able to edit a module's tree.

## Found along the way

These came up during the research and aren't part of the share itself.

- Shared Fate already treats a ship's own modules as allies (`ShieldSkillEffect.java:303-305`), so a Shared Fate parent sends shield flux into its own plates today. Fix: skip ships whose parent station is the owner, and sibling modules.
- The refit Skill Tree button has no module guard (`SkillTreeRefitButton.shouldShow`, `SkillTreeRefitButton.java:56`). LunaLib passes the selected module's fake member and variant, and opening the panel there runs `ShipTreeSync.memberChanged`. That would put our hull mod onto the module variant, which can be the global stock variant. Unverified in game; it should be confirmed and guarded first.
- Tania's Fixed Frontal Shield blanket-`unmodify()`s shield absorption, shield damage taken, arc, upkeep and unfold rate in its after-creation hook (`tania_Fixed_Frontal_Shield.java:45-60`). Our shield nodes on Tania main ships are therefore probably wiped. Unverified in game; it belongs in the Tania compat notes.
- Ludd's Light and Repair Coordinator already include a ship's own modules among nearby ships. Ludd's Light hitting friendlies is intended. Repair Coordinator repairing plates is fine.

## Build order

1. Confirm the refit button issue in game and guard `SkillTreeRefitButton.shouldShow` and the panel against module members and variants. Fix the Shared Fate ally filter.
2. Add the classification. Each `SkillEffect` gets a `ModuleShare` value: SHARE, SHARE_REBUILD, EXCLUDE (default). Each skill type gets an optional `moduleShare` override (`all`, `none`) for the all-or-nothing nodes, in `skill_types.json` and the editor serialisers.
3. Build `ModuleTreeShare`: node walk, skip rules, scaling, MULT pooling, damage-taken caps, and the push onto module stats.
4. Fold `ModuleBonusListener` into it and write one rescaler for hull, armor, ammo and shield arc, with the armor clamp.
5. Add the LunaLib setting and the tree summary line, in English and Chinese.
6. Docs: one short `docs/docs.md` section on what modules get and where the code lives.
7. Test in game on Tania ships (Argentavis for built-in DTC, Black Hawk for capital plates and Phaedra, Aetos for the single hardpoint plate), a vanilla Onslaught Mk.I, and one Knights of Ludd or Roider Union modular ship. Also retreat and redeploy, and two engagements in one encounter.
8. Later, one at a time: the NEEDS-TEST listeners.

## Tests

- Classification: every effect used in `skill_types.json` has a module share value, and nothing is SHARE whose stat is fixed at build without a rebuild.
- All-or-nothing nodes: a node with an excluded upside never shares its downside.
- Skip rules: a module with the node's hull mod (fitted or built in) or an exclusive hull mod gets nothing from that node.
- Scaling: PERCENT, FLAT and pooled MULT at 50%; damage-taken cap re-applied; temporary nodes skipped; vanilla passthroughs skipped.
- Rebuild: hull ratio, armor clamp across two engagements (cell at 1.1 × max stays 1.1), ammo per weapon, shield arc only with a shield.
- Merge with the Tania module nodes: one rescale, Tania values not scaled by the share.
- Share 0 attaches nothing; stations attach nothing.

## Open questions

- Share default and range (50%, 0 to 100% in steps of 5).
- Hull size for flat values: the parent's (planned) or the module's (needs its own resolve cache key).
- Should the share be per node with all-or-nothing everywhere, instead of per effect with the listed exceptions?
- Should inactive modules (pure armor) get only armor and hull?
- Should the share be shown per stat in a tooltip, or only as the one summary line?
