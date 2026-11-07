# Collapsed Reach modifiers

Hull items dropped in Collapsed Reach systems can carry a Collapsed Reach modifier. The biome leans on black holes, neutron stars and pulsars, so its set is about gravity and time: nothing slower escapes, time flow, deflected shots, pulsar radiation and missiles falling into p-space.

Status: built 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_COLLAPSED_REACH`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 10% of hull items dropped here. Values are placeholders.

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Inescapable (prefix) | new: increased damage to slower ships | 5-10% to ships with a lower top speed than this one | nothing slower escapes the horizon |
| Specialist Crew | Horizon-diving (prefix) | new: more hull items found in systems with a black hole | 5% | crews who salvage at the edge |
| Drive Assembly | Frame-dragging (prefix) | new: time flow at top speed | 3-5% | relativistic drive |
| Reactor Regulator | Degenerate (prefix) | new: shorter overloads | 10-20% | degenerate matter |
| Fire Control Suite | of the Pulsar (suffix) | new: beams deal extra EMP | 5-10% of their damage | pulsar radiation |
| Shield Emitter | Lensing (prefix) | new: chance to deflect projectiles for no flux | 5% of non-missile projectiles | gravitational lensing |
| Phase Coil Array | of the Singularity (suffix) | borrowed `GRAVITY_DRAG_TIMEFLOW_MULT`, phased only | up to 10-15% while phased | a singularity in p-space |
| Launch Assembly | Accreting (prefix) | new: a stack of increased fighter damage for each fighter lost | 1% per stack, up to 20%, each lasting 20 s | accretion |
| Armor Plate | Horizon-plated (prefix) | new: chance for missiles about to hit hull or armor to be ejected into p-space | 5-10% | a lip of event horizon |

Inescapable compares current top speeds, so slowed, flamed-out and time-dilated targets count. Of the Singularity uses the node's drag field and its per-ship cap, but only while the ship is phased.

Horizon-diving adds to the hull item quantity of loot found in a system whose star or any other star in it is a black hole, as `CHART_ITEM_PERCENT` does for a chart, but for hull items only. Accreting gives the ship's wings a stack each time one of its fighters is destroyed, and each stack expires on its own after 20 s; the cap is 20 stacks, so 20% at the placeholder 1% per stack. Horizon-plated catches the missile at the moment it hits, since that is when the engine reports the hit: the hit is zeroed and the missile is marked as having dealt damage, so it vanishes with no explosion, on-hit effect or damage; missiles hitting shields are left alone.

## Build order

1. [x] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN). Built: effects `DAMAGE_TO_SLOWER_SHIP_PERCENT`, `BLACK_HOLE_HULL_ITEM_PERCENT`, `TIMEFLOW_AT_TOP_SPEED_PERCENT`, `OVERLOAD_DURATION_PERCENT`, `BEAM_WEAPON_EMP_PERCENT`, `SHIELD_DEFLECT_CHANCE_PERCENT`, `GRAVITY_DRAG_PHASED_TIMEFLOW_MULT`, `STRIKECRAFT_DAMAGE_PER_LOST_STRIKECRAFT_PERCENT` and `MISSILE_EJECTION_CHANCE_PERCENT`, each with a `_COLLAPSED_REACH` modifier.
2. [x] The new effects, each in its ship part's `skilleffect` package with tests, and a phased-only form of the Gravity Drag listener. Built: `weapon.SlowerShipDamageListener`, `logistics.BlackHoleSalvage` read by Star Charts `charts.loot.BlackHoleHullItems` (hooked into `StarChartLootSource`), `movement.MovementSkillEffect.isAtTopSpeed` (a `ConditionalStatEffect`), the overload time mod in `flux`, `onhit.BeamEmpListener` (with `HitDamageRestorer.addEmpForThisHit`), `shield.ShieldDeflectionListener`, the phased key in `aura.GravityDragListener`, `fighter.AccretionListener` and `AccretionStacks`, `defense.MissileEjectionListener`.
3. [x] `docs/docs.md` for the black-hole hull item bonus, the missile ejection and the phased Gravity Drag. Built: the Collapsed Reach modifiers section.

## Decisions

- Horizon-diving adds up over the fleet's active ships, like post-battle salvage, and works only in Star Chart systems: the bonus rides on the chart's loot multipliers, which outside Star Charts don't exist.
- Horizon-plated catches only missiles that hit directly. Proximity-fused missiles explode near the ship, and that explosion is left alone.
