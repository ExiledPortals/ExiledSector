# Remnant Expanse modifiers

Hull items dropped in Remnant Expanse systems can carry a Remnant Expanse modifier. The biome is Remnant space, so its set is about networked machines: bonuses from nearby allies and shared targets, ion damage, fortress shields, self-repair, and High Tech Affinity on two of its items.

Status: built 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_REMNANT_EXPANSE`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 10% of hull items dropped here. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Relay-linked (prefix) | new: increased damage to a shared target | 5-10% when an ally targets it too | networked fire control |
| Specialist Crew | of the Expanse (suffix) | existing `HIGH_TECH_AFFINITY_FLAT` | 30-40 | crews who studied Remnant hulls |
| Drive Assembly | Lattice (prefix) | new: increased top speed per nearby allied ship | 2% per ship within 1000 su, up to 10% | formation flight |
| Reactor Regulator | of the Nexus (suffix) | new: dissipation clears hard flux while shields are up | 10-15% of flux dissipation | Remnant shield tech |
| Fire Control Suite | Ion-patterned (prefix) | new: more EMP damage | 10-15% | Remnant ion weapons |
| Shield Emitter | Fortress-tuned (prefix) | new: reduced shield damage taken while not firing, tapering in and out | 5-10%, over 3 s each way | the Fortress Shield |
| Phase Coil Array | of the Relay (suffix) | new: `NAV_RATING_PER_HIGH_TECH_AFFINITY` | +1 nav rating per 50 High Tech Affinity | a phased battle relay |
| Launch Assembly | Swarm-linked (prefix) | new: fighter damage per nearby friendly fighter | 2% per fighter within 500 su, up to 20% | drone swarms |
| Armor Plate | Self-mending (prefix) | new: armor regenerates on cells not hit recently, for hard flux, with no enemy ship near | 0.5% per second after 5 s unhit, up to 50% of total armor per combat | nanite repair |

Relay-linked: every 0.5 s the ship's listener notes which enemies an allied ship has as its ship target (`getShipTarget`), and its damage listener raises hits on those. The scan is one pass over allied ships per interval, not per hit. Lattice counts allied ships only, never fighters, drones or modules.

Fortress-tuned: the reduction climbs to full over 3 s while none of the ship's weapons fire, and falls to nothing over 3 s once any fires. Of the Relay is a new `AffinityScaling` row in `SpecialSkillEffect` on the High Tech filter targeting `BattleSkillEffect.NAV_RATING`, with a new step of 50; the crew's affinity feeds it. Of the Nexus: a share of the ship's flux dissipation applies to hard flux while its shield is up, as vanilla's hard-flux dissipation fraction does.

Self-mending: each armor point restored adds hard flux (placeholder 1 per point), and repair stops while the ship's flux is full, while any enemy ship (not fighter) is within 1500 su, or once it has restored 50% of the ship's total armor that combat.

Ion-patterned reads "more", not "increased": vanilla applies a hit's damage modifier to its EMP too (`computeFluxDealt`), so scaling the EMP on top of that modifier multiplies with every damage bonus instead of adding to them.

## Build order

1. [x] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN). Built: rows ending `_REMNANT_EXPANSE`; the crew row reuses `HIGH_TECH_AFFINITY_FLAT`.
2. [x] The new effects, each in its ship part's `skilleffect` package with tests, and the nav rating `AffinityScaling` row with its step of 50. Built: `SHARED_TARGET_DAMAGE_PERCENT` (`weapon.SharedTargetDamageListener`), `TOP_SPEED_PERCENT_PER_NEARBY_ALLY` (`aura.FormationSpeedListener`), `HARD_FLUX_DISSIPATION_PERCENT` (`flux`, percentage points on vanilla's hard flux dissipation fraction), `EMP_DAMAGE_PERCENT` (`weapon.EmpDamageListener`, through `HitDamageRestorer.scaleEmpForThisHit`), `HOLD_FIRE_SHIELD_DAMAGE_REDUCTION_PERCENT` (`shield.HoldFireShieldListener`), `STRIKECRAFT_WEAPON_DAMAGE_PERCENT_PER_NEARBY_STRIKECRAFT` (`fighter.SwarmLinkListener`), `UNHIT_ARMOR_REGEN_PERCENT` (`defense.UnhitArmorRegenListener`) and `NAV_RATING_PER_HIGH_TECH_AFFINITY` (`SpecialSkillEffect`, `AffinityScaling.NAV_RATING_STEP`).
3. [x] `docs/docs.md` for the shared target, the shield taper and the armor repair's flux cost and limits. Built: the Remnant Expanse modifiers section.

## Decisions

- Relay-linked counts only allies' ship targets, not targets their weapons happen to be firing at, and only allied ships, never strikecraft; a hit on a station module counts when the station itself is the shared target.
- Self-mending costs 1 hard flux per armor point restored (`UnhitArmorRegenListener.HARD_FLUX_PER_ARMOR`), and each tick's repair is cut to the flux room left, so it never overloads the ship.
- Swarm-linked counts allied strikecraft from any carrier, never ship-system drones, and raises the strikecraft's weapon damage (summed with its other weapon damage sources).
