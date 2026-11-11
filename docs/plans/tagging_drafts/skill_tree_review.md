# Skill tree theme tags review

One table per volume. Nodes of the same type are listed once with their count. Effects are the tooltip lines from the description golden file; numbers that differ by hull size read frigate/destroyer/cruiser/capital. Only theme tags are shown, not requirement or gate tags. Choice nodes list each option with its own tags under the choice's tags.

## Core

231 nodes, 99 types.

| Node | Effect | Tags |
|---|---|---|
| Flux Capacity (x3) | 2% more flux capacity. | `flux` |
| Flux Dissipation (x2) | 2% more flux dissipation. | `flux` |
| Missile Weapon Damage (x4) | 3% increased missile weapon damage. | `missile` `damage` |
| Beam Weapon Damage (x3) | 3% increased beam weapon damage. | `beam` `energy` `damage` |
| Non-Beam Energy Weapon Damage (x2) | 3% increased non-beam energy weapon damage. | `energy` `damage` |
| Armor (x5) | +15/30/50/75 to armor. | `armour` |
| Flux Coil Adjunct | 15% increased flux capacity. +600/1200/1800/3000 to flux capacity. | `flux` |
| Safety Overrides | The zero-flux speed boost works at any flux level. 100% more flux dissipation. 67% less peak performance time. 100% less venting speed. +450 to range before falloff effectiveness applies. 75% less weapon range beyond the range threshold. +50/30/20/10 to top speed. +100/60/40/20 to acceleration. +100/60/40/20 to deceleration. | `flux` `mobility` `combat_readiness` |
| Heavy Armor | 20% less maneuverability. +150/300/400/500 to armor. | `armour` |
| Armored Weapon Mounts | 10% increased armor. 80% increased weapon durability. 25% less weapon turn rate. 25% less weapon recoil. | `armour` `weapon` |
| Reinforced Bulkheads | 40% increased hull points. If disabled, this ship is almost always recoverable after the battle. 100% reduced chance of this ship breaking apart when destroyed. | `hull` `salvage` |
| Blast Doors | 20% increased hull points. 60% less crew casualties. | `hull` `crew` |
| More Hull (x6) | 5% more hull points. | `hull` |
| Resistant Flux Conduits | 30% less EMP damage taken. 10% increased venting speed. | `flux` `emp` `damage_taken` |
| Automated Repair Unit | 25% less weapon and engine repair time. | `repair` |
| Repair Time (x3) | 15% less weapon and engine repair time. | `repair` |
| Hardened Subsystems | 30% less rate of combat readiness loss from extended deployment. | `combat_readiness` |
| Advanced Turret Gyros | 40% increased weapon turn rate. | `weapon` |
| Weapon Turn Rate (x3) | 20% increased weapon turn rate. | `weapon` |
| Ballistic Rangefinder | Small and medium ballistic weapons and hybrid weapons of every size gain 100% of Ballistic Rangefinder's base range bonus, based on this ship's largest ballistic slot. | `ballistic` `range` |
| Expanded Magazines | 50% increased ballistic weapon ammo capacity. 50% increased energy weapon ammo capacity. | `ammo` `ballistic` `energy` |
| Integrated Point Defense AI | Point-defense weapons can identify and ignore decoy flares. Point-defense weapons get the best possible target leading, regardless of combat readiness. All small non-missile, non-strike weapons are classified as point-defense, automatically target missiles and are affected by point-defense stat modifiers. | `point_defense` |
| Expanded Missile Racks | 100% increased missile weapon ammo capacity. 15% reduced missile weapon rate of fire. | `missile` `ammo` |
| Missile Autoloader | Small missile weapons in small missile mounts reload when they run out of ammo, drawing on 100% of Missile Autoloader's shared reload capacity. Reloaded weapons can't fire for 10 seconds. | `missile` `ammo` |
| Neural Interface | Transferring to either linked ship resets its ship system cooldown and adds a charge. | `ship_system` `fleet_support` |
| ECCM Package | 50% increased chance for missiles to resist enemy ECM and flares. Significantly improved missile guidance algorithm. 25% increased missile weapon projectile speed. 150% increased missile acceleration. 50% increased missile turn rate. 150% increased missile turn acceleration. 50% less electronic warfare penalty against this ship's weapon range. | `missile` `electronic_warfare` |
| High Scatter Amplifier | 50% of beam weapon damage to shields is dealt as hard flux. 25% less beam weapon range. 10% increased beam weapon damage. | `beam` `energy` `damage` |
| Advanced Optics | +200 to beam weapon range. 30% less beam weapon turn rate. | `beam` `energy` `range` |
| Dedicated Targeting Core | Cruiser/Capital only: 35/55% increased ballistic weapon range. 35/55% increased energy weapon range. | `range` |
| Integrated Targeting Unit | 10/20/35/55% increased ballistic weapon range. 10/20/35/55% increased energy weapon range. | `range` |
| ECM Package | +1/2/3/4 to ECM rating. | `electronic_warfare` |
| Defensive Targeting Array | The vanilla Defensive Targeting Array hull mod's effects (its text comes from the game). | `strikecraft` `point_defense` |
| Accelerated Shields | 50% increased shield turn rate. 50% increased shield raise rate. | `shield` |
| Extended Shields | +40 to shield arc. | `shield` |
| Hardened Shields | 10% less damage taken by shields. 50% less chance for shields to be pierced by EMP arcs. | `shield` `damage_taken` |
| Stabilized Shields | 40% less shield flux upkeep. | `shield` |
| Shield Shunt | 15% increased armor. Removes this ship's shield. | `armour` `shieldless` |
| Makeshift Shield Generator | 20% less top speed. Installs a makeshift, front-facing shield if this ship has none. | `front_shield` |
| Shield Conversion - Front | 100% increased shield arc. Converts this ship's shield to front-facing. | `front_shield` |
| Shield Conversion - Omni | 30% less shield arc. Converts this ship's shield to omni-directional. | `omni_shield` |
| Auxiliary Thrusters | 50% increased maneuverability. | `mobility` |
| Unstable Injector | 15% less ballistic weapon range. 15% less energy weapon range. 25% increased strikecraft refit time. +25/20/15/15 to top speed. | `mobility` |
| Nav Relay | Your fleet has +2/3/4/5 to nav rating. | `fleet_support` `mobility` |
| Adaptive Phase Coils | 40% increased hard flux level at which phased speed reaches its minimum. | `phase` `flux` |
| Phase Anchor | 100% less phase cloak activation cost. 100% more flux dissipation, weapon rate of fire and ammo regeneration while phased. While at risk of being destroyed or disabled, this ship can instead perform an emergency dive into phase space, retreating from battle and suffering an additional combat readiness penalty equal to 100% of its deployment cost. The ship must have enough combat readiness to cover this cost. Only one ship per battle, regardless of side, can perform this dive. | `phase` `while_phased` |
| Additional Berthing | +25/35/60/125 to crew capacity. | `crew` |
| Augmented Drive Field (x2) | +1 to max burn level. | `mobility` `fuel` |
| Auxiliary Fuel Tanks | 20% increased fuel capacity. +25/35/60/125 to fuel capacity. | `fuel` |
| Efficiency Overhaul | 20% less minimum crew required. 20% less supply use for maintenance. 20% less fuel consumption rate. 50% increased combat readiness recovery rate. 50% increased repair rate per day. | `supplies` `fuel` `crew` |
| Expanded Cargo Holds | 20% increased cargo capacity. +25/35/60/125 to cargo capacity. | `cargo` |
| High Resolution Sensors | This ship's sensor strength always contributes to fleet-wide sensor strength. +40/50/60/70 to this ship's sensor strength. | `sensors` |
| Insulated Engine Assembly | 60% increased engine durability. 10% increased hull points. 50% less sensor profile. | `sensors` `hull` |
| Militarized Subsystems | Removes the penalties of a civilian-grade hull. +1 to max burn level. 100% increased minimum crew required. | `logistics` `mobility` |
| Solar Shielding | 75% less combat readiness loss from being in a solar corona or a deep hyperspace storm. | `logistics` |
| Surveying Equipment | Your fleet has -5/7/12/22 to heavy machinery needed to perform surveys. Your fleet has -3/5/10/20 to supplies needed to perform surveys. | `logistics` |
| Converted Hangar | +1 to number of strikecraft bays, plus any extra bays from Vast Hangar. 50% more strikecraft refit time. The strikecraft replacement rate both decays and recovers 50% more slowly. +40% of base refit time to strikecraft relaunch time. +20 to minimum crew required. +1 to deployment points and supplies to recover for every 5 ordnance points of strikecraft wings, at least +1. Combat readiness lost per deployment rises with it. | `strikecraft` |
| Converted Fighter Bay | Removes all of this ship's strikecraft bays. 20% reduced minimum crew required per strikecraft bay, up to 80% in total. +50 to cargo capacity per strikecraft bay. | `cargo` `crew` |
| Expanded Deck Crew | 15% reduced rate at which the strikecraft replacement rate decreases due to strikecraft losses. 25% increased rate at which the strikecraft replacement rate recovers. +20 to minimum crew required per strikecraft bay. | `strikecraft` |
| Recovery Shuttles | Strikecraft launched from this ship have 75% less pilot casualties. | `strikecraft` `crew` |
| Operations Center | 250% increased command point recovery rate while this ship is the flagship. | `fleet_support` |
| Escort Package | While within 700 su of a larger friendly ship, up to 25% increased maneuverability, fading out over a further 500 su. Doubled for a destroyer escorting a capital ship. While within 700 su of a larger friendly ship, up to 10% increased top speed, fading out over a further 500 su. Doubled for a destroyer escorting a capital ship. While within 700 su of a larger friendly ship, up to 15% increased ballistic and energy weapon range, fading out over a further 500 su. Doubled for a destroyer escorting a capital ship. | `fleet_support` `mobility` `range` |
| Low Tech | +10 to Low Tech Affinity. | `low_tech` |
| Affinity (x68) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| Midline | +10 to Midline Affinity. | `midline` |
| High Tech | +10 to High Tech Affinity. | `high_tech` |
| Strikecraft Weapon Damage (x2) | Strikecraft launched from this ship have 3% increased weapon damage. | `strikecraft` `damage` |
| Flux Distributor | 15% increased flux dissipation. +30/60/90/150 to flux dissipation. | `flux` |
| Survey Cost Reduction (x2) | Your fleet has -1/3/5/10 to heavy machinery needed to perform surveys. Your fleet has -1/3/5/10 to supplies needed to perform surveys. | `logistics` |
| Sensor Strength (x2) | +10/12/20/30 to this ship's sensor strength. | `sensors` |
| Crew Capacity (x3) | +5/15/25/50 to crew capacity. | `crew` |
| Engine Durability and Sensor Profile (x2) | 25% increased engine durability. 10% reduced sensor profile. | `sensors` `hull` |
| Cargo Capacity (x2) | +5/15/25/50 to cargo capacity. | `cargo` |
| Fuel (x2) | +5/15/25/50 to fuel capacity. | `fuel` |
| Shield Arc (x4) | +15 to shield arc. | `shield` |
| Shield Raise Rate (x2) | 25% increased shield raise rate. | `shield` |
| Shield Turn Rate (x2) | 25% increased shield turn rate. | `omni_shield` |
| Shield Raise Rate or Shield Turn Rate | Choose Shield Raise Rate: 25% increased shield raise rate.<br>Choose Shield Turn Rate: 25% increased shield turn rate. | `shield`<br>Shield Raise Rate: `shield`<br>Shield Turn Rate: `omni_shield` |
| Ballistic Weapon Damage (x4) | 3% increased ballistic weapon damage. | `ballistic` `damage` |
| Ballistic and Energy Weapon Range (x4) | 3/4/5/6% increased ballistic weapon range. 3/4/5/6% increased energy weapon range. | `range` |
| Shield Flux/Damage Reduction (x2) | 5% less damage taken by shields. | `shield` `damage_taken` |
| Shield Upkeep Reduction (x2) | 10% less shield flux upkeep. | `shield` |
| Top Speed (x2) | +6/5/4/4 to top speed. | `mobility` |
| Phase Cloak Flux Threshold (x2) | 5% increased hard flux level at which phased speed reaches its minimum. | `phase` |
| Less EMP Damage Taken | 20% less EMP damage taken. | `emp` `damage_taken` |
| Increased Vent Rate | 20% increased venting speed. | `flux` |
| Vent Rate and EMP Damage | 5% increased venting speed. 10% less EMP damage taken. | `flux` `emp` `damage_taken` |
| Weapon Durability (x2) | 5% increased weapon durability. | `weapon` |
| Wormhole (x6) | Allocating this wormhole also allocates its paired wormhole at no cost. Unallocating either end unallocates both. Right-click to travel through it. | (none) |
| Point Defense Damage to Missiles and Fighters (x2) | 10% increased damage to missiles. 10% increased damage to fighters. | `point_defense` |
| Maneuverability (x3) | 25% increased maneuverability. | `mobility` |
| Strikecraft Replacement (x2) | 5% increased rate at which the strikecraft replacement rate recovers. 5% reduced rate at which the strikecraft replacement rate decreases due to strikecraft losses. | `strikecraft` |
| Reduced Monthly Supplies (x2) | 5% less supply use for maintenance. | `supplies` |
| Rationing | 10% less supply use for maintenance. 10% less fuel consumption rate. | `supplies` `fuel` |
| Ballistic Projectile Speed | 10% increased ballistic weapon projectile speed. | `ballistic` |
| Peak CR Duration (x2) | 25% increased peak performance time. | `combat_readiness` |
| ECM Resistance (x2) | 10% less electronic warfare penalty against this ship's weapon range. | `electronic_warfare` |
| Ballistic and Energy Weapon Range While Escorting (x2) | While within 700 su of a larger friendly ship, up to 5% increased ballistic and energy weapon range, fading out over a further 500 su. Doubled for a destroyer escorting a capital ship. | `range` `fleet_support` |
| Top Speed While Escorting | While within 700 su of a larger friendly ship, up to 5% increased top speed, fading out over a further 500 su. Doubled for a destroyer escorting a capital ship. | `mobility` `fleet_support` |
| Subroutine Socket (x6) | Holds one subroutine. | (none) |

## Sindrian Diktat

35 nodes, 18 types.

| Node | Effect | Tags |
|---|---|---|
| Lion's Gaze | Beam weapon hits split their damage evenly across the target and up to 1 additional nearby enemy ship. Strikecraft are never chosen. The target acquisition range is half the beam weapon's range. | `beam` `energy` |
| Affinity (x8) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| Crustacean Integration | 15% increased combat readiness recovery rate. 10% increased top speed. 30% increased peak performance time. | `combat_readiness` `mobility` |
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Lion's Guard Arsenal | +100 to the base range of weapons in medium energy slots. 15% less missile weapon damage. | `energy` `range` |
| Ballistic and Energy Weapon Range | 3/4/5/6% increased ballistic weapon range. 3/4/5/6% increased energy weapon range. | `range` |
| Additional Beam Splits (x3) | Beam weapon hits split their damage evenly across the target and up to 1 additional nearby enemy ship. Strikecraft are never chosen. The target acquisition range is half the beam weapon's range. | `beam` `energy` |
| Beam Weapon Damage (x2) | 3% increased beam weapon damage. | `beam` `energy` `damage` |
| Sindrian Shielding | 10% less damage taken by shields. 25% less combat readiness loss from being in a solar corona or a deep hyperspace storm. | `shield` `damage_taken` |
| Energy Damage Resistance (x4) | 5% reduced energy damage taken. | `energy_damage` `damage_taken` |
| Top Speed (x2) | +6/5/4/4 to top speed. | `mobility` |
| Honor Guard | Additional top speed equal to 50% of your flagship's base top speed while your flagship is deployed. | `mobility` `damage_taken` |
| ECM Resistance (x2) | 10% less electronic warfare penalty against this ship's weapon range. | `electronic_warfare` |
| Lion's Roar | Enemy ships within 1000 su have 10% less weapon rate of fire. Only the strongest debuff of this effect applies at any one time. | `fleet_support` `nearby_ships` |
| More Hull (x2) | 5% more hull points. | `hull` |
| Executor's Decree | This ship can't retreat. 15% increased weapon damage. 5% reduced damage taken. | `damage` `damage_taken` |
| Fuel (x2) | +5/15/25/50 to fuel capacity. | `fuel` |
| Strategic Fuel Reserve | 10% reduced fuel use while your fleet carries at least half its fuel capacity. | `fuel` |

## Luddic

36 nodes, 18 types.

| Node | Effect | Tags |
|---|---|---|
| Top Speed (x3) | +6/5/4/4 to top speed. | `mobility` |
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Affinity (x7) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| Knight's Charge | 20% increased maneuverability. 25% increased top speed. These effects only last for the first 60 seconds after the ship is deployed. | `mobility` |
| Perseverance Through Faith | 2% more weapon damage for each d-mod, up to a maximum of 5 d-mods. | `d_mods` `damage` |
| Reduced D-Mod Effect (x5) | 4% less negative effects from d-mods. | `d_mods` |
| Ludd's Light | When this ship is destroyed, it detonates violently, dealing high-explosive damage equal to up to 2000% of its maximum fuel capacity to nearby ships, tapering off with distance. | `high_explosive` `damage` |
| At Any Cost | Touching another ship's hull, however slightly, is instantly fatal to this ship. Strikecraft, wrecks and asteroids don't count. When this ship isn't flown by you, it flies straight at the biggest enemy ship, intent on colliding. | `hull` `damage` |
| Temporary Maneuverability (x2) | 50% increased maneuverability. These effects only last for the first 90 seconds after the ship is deployed. | `mobility` |
| Strikecraft Speed (x3) | Strikecraft launched from this ship have 5% increased top speed. | `strikecraft` |
| Martyr's Payload | Bombers launched from this ship have 50% increased top speed. Bombers launched from this ship have 50% increased acceleration. Every 3 seconds, each bomber launched from this ship that still carries any payload has a 5% chance to set off one full salvo from every loaded weapon at once, destroying itself and damaging every ship nearby, friend or foe. Bombers within 600 su of this ship's hull never detonate. | `strikecraft` |
| Increased Mass (x2) | 10% increased mass. | `hull` |
| Pather's Prow | 50% more collision damage dealt to enemy ships and 50% less collision damage taken. | `hull` |
| Armor (x2) | +15/30/50/75 to armor. | `armour` |
| Litany of the Fallen | 1% reduced hull damage taken for every 10 crew lost this deployment, up to a maximum of 20%. | `hull` `damage_taken` |
| More Hull (x2) | 5% more hull points. | `hull` |
| Rite of Mortification | Overloads are replaced by hull damage equal to 10% of maximum hull. Replacing an overload dissipates 50% of hard flux. | `flux` `hull` |
| Sanctified Scars | Allied ships within 1000 su have 1% reduced armor damage taken for each d-mod on this ship, up to a maximum of 5 d-mods. Only the strongest buff of this effect applies at any one time. | `fleet_support` `d_mods` |

## Tri-Tachyon

36 nodes, 19 types.

| Node | Effect | Tags |
|---|---|---|
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Affinity (x7) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| Hybrid P-space Shrouding | 50% of this ship's sensor strength counts toward the fleet-wide reduction in detected-at range that phase ships provide. Only applies while your fleet's transponder is off. 25% less combat readiness loss from being in a solar corona or a deep hyperspace storm. | `phase` `sensors` |
| TriPad Backdoor | 100% more combat objective capture rate. | `fleet_support` |
| Objective Capture Range (x3) | +35 to range from which combat objectives can be captured. | `fleet_support` |
| Refracting Projectiles | Non-beam energy weapons have a 20% chance to refract when hitting enemy shields. Each refraction deals 20% less damage than the shot before it. | `energy` `on_hit` |
| Non-Beam Energy Weapon Damage (x3) | 3% increased non-beam energy weapon damage. | `energy` `damage` |
| Engine Durability and Sensor Profile (x3) | 25% increased engine durability. 10% reduced sensor profile. | `sensors` `hull` |
| Strikecraft Speed | Strikecraft launched from this ship have 5% increased top speed. | `strikecraft` |
| Strikecraft Replacement (x2) | 5% increased rate at which the strikecraft replacement rate recovers. 5% reduced rate at which the strikecraft replacement rate decreases due to strikecraft losses. | `strikecraft` |
| Phase-Entangled Wings | When a strikecraft launched from this ship takes a hit smaller than its max hull or flux, 100% of that damage is shared across its wing. Strikecraft launched from this ship take 10% less damage for every living strikecraft of their wing out in space, up to a maximum of 80% less. | `strikecraft` `damage_taken` |
| EMP Damage (x2) | 5% increased EMP damage. | `emp` `damage` |
| Ion Cascade | 30% of EMP damage this ship deals to the nearest disabled weapon or engine is dealt as high explosive damage instead. | `emp` `high_explosive` `on_hit` |
| Phase Cloak Activation Cost (x2) | 5% less phase cloak activation cost. | `phase` `flux` |
| Phase Ambush Doctrine | When this ship leaves phase, it deals 25% increased weapon damage, tapering off over 3 seconds. | `phase` `damage` |
| Phase Cloak Cooldown (x2) | 5% reduced phase cloak cooldown. | `phase` |
| Phantom Wake | When this ship enters phase, it leaves an afterimage that draws enemy fire for 3 seconds. | `phase` |
| Energy Weapon Fire Rate (x2) | 3% increased energy weapon rate of fire. Burst beams instead recharge faster between bursts. | `energy` `rate_of_fire` |
| Cease and Desist | Energy weapon hits give enemy ships an Injunction stack for 10 seconds, at most once a second. Ships can hold up to 10 Injunction stacks. Enemy ships recharge their ship systems 5% slower for each Injunction stack. | `energy` `on_hit` |

## Remnant (REDACTED)

32 nodes, 15 types.

| Node | Effect | Tags |
|---|---|---|
| Affinity (x13) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Machine Precision | 100% increased target leading accuracy of autofiring weapons. 5% increased energy weapon range. 5% increased ballistic weapon range. 5% increased beam weapon range. | `range` |
| Combat Readiness (x2) | 2% increased maximum combat readiness. | `combat_readiness` |
| Terrifying Presence | Enemy ships within 1000 su have -25% to the target leading accuracy of their autofiring weapons. Penalties from several ships with this effect stack. | `fleet_support` `nearby_ships` |
| Nanoforge Mending | After 5 seconds without taking hull damage, repairs 1% of maximum hull per second in combat, up to 100% of maximum hull in total per deployment. | `hull` `repair` |
| Adaptive Munitions | 1% chance for each weapon hit to deal kinetic damage to shields or high explosive damage to armor instead, for every 5 High Tech Affinity. | `kinetic` `high_explosive` `high_tech` `on_hit` |
| Beam or Non-Beam Energy Weapon Damage (x2) | Choose Beam Weapon Damage: 3% increased beam weapon damage.<br>Choose Non-Beam Energy Weapon Damage: 3% increased non-beam energy weapon damage. | `beam` `energy` `damage`<br>Beam Weapon Damage: `beam` `energy` `damage`<br>Non-Beam Energy Weapon Damage: `energy` `damage` |
| Equilibrium Protocol | 20% increased weapon damage while this ship's affinities are all equal. | `damage` |
| EMP Damage (x2) | 5% increased EMP damage. | `emp` `damage` |
| Ion Spill | 25% of EMP damage this ship deals to shields is dealt as energy damage instead. | `emp` `energy_damage` `on_hit` |
| Armor (x2) | +15/30/50/75 to armor. | `armour` |
| Dormant Repair Cycle | Armor cells not hit for 5 seconds repair 0.5% of their maximum armor per second while no enemy ship is within 1500 su, adding 1 hard flux per point repaired, up to 20% of this ship's total armor per deployment. | `armour` `repair` |
| Greater High Tech Affinity (x2) | +10 to High Tech Affinity. | `high_tech` |
| Relay Protocols | +10 to High Tech Affinity. Your fleet has +1 to nav rating for every 50 High Tech Affinity. | `high_tech` `fleet_support` `mobility` |

## Hegemony

40 nodes, 18 types.

| Node | Effect | Tags |
|---|---|---|
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Hullbreacher Rounds | Ballistic projectile hits on an enemy ship's armor or hull have a 20% chance to pierce. | `ballistic` `on_hit` |
| Affinity (x11) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| Heavy Ballistics Integration | -5 to ordnance point cost of large ballistic weapons. | `ballistic` |
| Ballistic Projectile Speed (x5) | 10% increased ballistic weapon projectile speed. | `ballistic` |
| XIV Torpedo Doctrine | 30% increased missile weapon projectile speed. 30% reduced missile maneuverability. | `missile` |
| Missile Top Speed (x2) | 5% increased missile weapon projectile speed. | `missile` |
| Navy Heartbeat | 3% more ballistic weapon damage for every burn level this ship has above its hull's default. | `ballistic` `damage` `mobility` |
| Ballistic Weapon Damage (x5) | 3% increased ballistic weapon damage. | `ballistic` `damage` |
| Missile Weapon Damage | 3% increased missile weapon damage. | `missile` `damage` |
| Concussion Shells | Large ballistic weapon hits on an enemy ship's armor or hull have a 10% chance for every second between the weapon's shots to disable the weapon or engine nearest the impact. | `ballistic` `on_hit` |
| Hold the Line | 15% reduced armor damage taken while moving slower than 25% of top speed. | `armour` `damage_taken` |
| Domain-Pattern Laminate | 8% reduced kinetic damage taken. 8% reduced high-explosive damage taken. | `armour` `kinetic` `high_explosive` `damage_taken` |
| Flak Screen Doctrine | Enemy missiles destroyed by this ship burst for 100 fragmentation damage to other enemy missiles and fighters within 100 su. | `point_defense` `fragmentation` `on_kill` |
| Armor (x2) | +15/30/50/75 to armor. | `armour` |
| Fragmentation Damage Resistance (x2) | 4% reduced fragmentation damage taken. | `fragmentation` `damage_taken` |
| Point Defense Damage to Missiles and Fighters (x2) | 10% increased damage to missiles. 10% increased damage to fighters. | `point_defense` |
| Auxiliary Screen | Allied ships of a smaller hull size within 800 su take 10% less damage. Only the strongest buff of this effect applies at any one time. | `fleet_support` `damage_taken` `nearby_ships` |

## Pirate

42 nodes, 22 types.

| Node | Effect | Tags |
|---|---|---|
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Affinity (x7) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| Waste Not | 10% increased combat readiness recovery rate. Your fleet has 3/6/10/15% increased post-battle salvage. | `combat_readiness` `salvage` |
| Ground Support | Your fleet gains effective planetary raid strength equal to 25% of this ship's maximum crew capacity, up to the total number of marines in the fleet. | `crew` |
| Restriction Breeds Creativity | 2% less armor damage taken for each d-mod, up to a maximum of 5 d-mods. | `d_mods` `armour` `damage_taken` |
| Reduced D-Mod Effect (x3) | 4% less negative effects from d-mods. | `d_mods` |
| Freebooter's Charge | 50% increased crew casualties. 30% increased weapon damage. 10% more damage taken by shields. 10% reduced armor. | `damage` |
| Maneuverability (x3) | 25% increased maneuverability. | `mobility` |
| Crew Capacity (x3) | +5/15/25/50 to crew capacity. | `crew` |
| Combat Readiness (x3) | 2% increased maximum combat readiness. | `combat_readiness` |
| Damage to Helpless Ships (x2) | 5% increased damage to overloaded, venting or flamed-out ships. | `damage` |
| Cheap Shots | 20% increased damage to overloaded, venting or flamed-out ships. | `damage` `on_kill` |
| Weapon Flux Cost (x2) | 3% reduced weapon flux cost. | `weapon` `flux` |
| Blood in the Water | Whenever an enemy ship is destroyed within 1000 su, this ship gains a Pirate Frenzy stack for 10 seconds. Ships can hold up to 5 Pirate Frenzy stacks. 5% increased weapon rate of fire for each Pirate Frenzy stack. 5% reduced weapon flux cost for each Pirate Frenzy stack. | `rate_of_fire` `on_kill` `nearby_ships` |
| Damage to Slower Ships (x2) | 4% increased damage to ships with a lower top speed than this one. | `damage` |
| Pick Off The Stragglers | 10% increased damage to ships with no allies within 1000 su. | `damage` `nearby_ships` |
| Acceleration (x2) | 10% increased acceleration. | `mobility` |
| Cut and Run | 30% increased top speed while retreating. | `mobility` `damage_taken` |
| Sensor Profile (x2) | 10% reduced sensor profile. | `sensors` |
| Running Dark | Your fleet has 10% reduced sensor profile while its transponder is off, up to a maximum of 30%. | `sensors` `logistics` |
| Fuel (x2) | +5/15/25/50 to fuel capacity. | `fuel` |
| Haul the Prize | -1 to max burn level. The slowest ship in your fleet of this ship's hull size or smaller has +1 to max burn level, up to this ship's max burn level. | `logistics` `mobility` |

## Persean League

38 nodes, 19 types.

| Node | Effect | Tags |
|---|---|---|
| League Reload Drill | 50% increased energy weapon ammo regeneration rate. 50% increased ballistic weapon ammo regeneration rate. | `ammo` `ballistic` `energy` |
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Affinity (x9) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| League Picket Targeting | +1 to energy weapon range for every point of this ship's sensor strength. | `energy` `range` `sensors` |
| Shared Fate | Disperses 20% of damage taken by shields to allied ships within 1000 su that aren't overloaded or venting, split evenly between them as hard flux. 20% reduced weapon damage. | `shield` `damage_taken` `nearby_ships` |
| Shared Damage Taken (x3) | Disperses 5% of damage taken by shields to allied ships within 1000 su that aren't overloaded or venting, split evenly between them as hard flux. | `shield` `damage_taken` `nearby_ships` |
| Sensor Strength (x3) | +10/12/20/30 to this ship's sensor strength. | `sensors` |
| Ammo Capacity (x3) | 20% increased ballistic weapon ammo capacity. 20% increased energy weapon ammo capacity. | `ammo` `ballistic` `energy` |
| Coalition Flight Doctrine | When this ship deploys, its wings that share a role lend each other strikecraft. Each wing keeps at least 1 of its own strikecraft and fills the rest of its normal wing size with up to 1 of every other strikecraft type in its group. All its wings use the engagement range of its longest-ranged wing. Strikecraft in each group fly at the group's average top speed. | `strikecraft` |
| Increased Sensor Strength (x2) | 5% increased this ship's sensor strength. | `sensors` |
| COMSEC Discipline | 25% reduced electronic warfare penalty against this ship's weapon range. 15% increased this ship's sensor strength. | `electronic_warfare` `sensors` |
| Objective Capture Range (x2) | +35 to range from which combat objectives can be captured. | `fleet_support` |
| Defensive Compact | 20% reduced damage taken while within 1500 su of a combat objective your side holds. | `damage_taken` `fleet_support` |
| Strikecraft Weapon Damage (x2) | Strikecraft launched from this ship have 3% increased weapon damage. | `strikecraft` `damage` |
| Joint Strike Package | Strikecraft launched from this ship have 4% increased weapon damage for every allied strikecraft within 500 su, up to a maximum of 20%. | `strikecraft` `damage` `nearby_ships` |
| Reduced Monthly Supplies (x2) | 5% less supply use for maintenance. | `supplies` |
| League Requisitions | Your fleet has 3% reduced supply use for maintenance for every different hull type among its ships, up to a maximum of 15%. | `supplies` `fuel` |
| Reduced D-Mod Effect (x2) | 4% less negative effects from d-mods. | `d_mods` |
| Kazeron Recovery Standards | Your fleet has a 10% chance for each ship it recovers after a battle to gain no d-mods, up to a maximum of 30%. | `salvage` `d_mods` |

## Omega

21 nodes, 11 types.

| Node | Effect | Tags |
|---|---|---|
| Combat Readiness (x2) | 2% increased maximum combat readiness. | `combat_readiness` |
| Disintegration | Energy weapon hits on armor strip an additional 20% of the hit's damage from the armor around the impact over 5 seconds. | `energy` `damage` `on_hit` |
| Rift Explosion Damage (x6) | 10% increased rift explosion damage. Strikecraft launched from this ship have 10% increased rift explosion damage. | `damage` |
| Rift Harmonics | Energy projectile hits have a 30% chance to trigger a rift explosion, dealing energy damage equal to 50% of the weapon's base damage per shot, at least 30 and up to a maximum of 500. | `energy` `damage` `on_hit` |
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Affinity (x5) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| Strikecraft Weapon Range | Strikecraft launched from this ship have 10% increased weapon range. | `strikecraft` `range` |
| Spark Rift | Strikecraft launched from this ship have a 10% chance for energy projectile hits to trigger a rift explosion, dealing energy damage equal to 50% of the weapon's base damage per shot, at least 30 and up to a maximum of 500. | `strikecraft` `on_hit` |
| Ship System Cooldown | 5% reduced ship system cooldown. | `ship_system` |
| Temporal Exhaust | When this ship activates its ship system, it gains 20% increased time flow, tapering off over 4 seconds, and leaves a trail of rift explosions behind its engines, each dealing 100 energy damage, at most once every 30 seconds. | `ship_system` `mobility` |
| Resonator Payload | Missile hits have a 1% chance for every 5 of your highest affinity to trigger a series of 4 rift explosions, each dealing energy damage equal to 10% of the missile's base damage, up to a maximum of 100. | `missile` `on_hit` |

## Kesteven

16 nodes, 10 types.

| Node | Effect | Tags |
|---|---|---|
| Armor (x2) | +15/30/50/75 to armor. | `armour` |
| Critical Point Refit | Up to +400/700/900/1200 to armor. The bonus is highest on hulls with the least base armor and falls to +150/300/400/500 at 400/700/900/1200 base armor. Can't be allocated on phase ships or on hulls with more base armor than that. 30% less maneuverability. | `armour` |
| Inertial Supercharging | Frigate: 0.08% increased ballistic and non-beam energy weapon damage for every su/s of the ship's current speed. On hulls with Augmented Systems, also grants increased projectile speed equal to 100% of the speed-based damage bonus. 50% increased damage taken by weapons. / Destroyer: 0.08% increased ballistic and non-beam energy weapon damage for every su/s of the ship's current speed. On hulls with Augmented Systems, also grants increased projectile speed equal to 100% of the speed-based damage bonus. / Cruiser: 0.08% increased ballistic and non-beam energy weapon damage for every su/s of the ship's current speed. On hulls with Augmented Systems, also grants increased projectile speed equal to 100% of the speed-based damage bonus. / Capital: 0.08% increased ballistic and non-beam energy weapon damage for every su/s of the ship's current speed. On hulls with Augmented Systems, also grants increased projectile speed equal to 100% of the speed-based damage bonus. | `damage` `mobility` |
| Energy Weapon Flux Cost (x2) | 3% reduced energy weapon flux cost. | `energy` `flux` |
| Volatile Flux Tuning | Weapon rate of fire changes with flux, from 40% increased at no flux to 40% reduced at full flux, with no change at half flux. Venting or being overloaded counts as full flux. 67% increased flux capacity. 67% increased flux capacity granted by each flux capacitor. +800 to range before falloff effectiveness applies. 50% less weapon range beyond the range threshold. 75% less strikecraft engagement range. On hulls with Augmented Systems, 50% reduced effect of the high-flux top speed and rate of fire penalties. 33% increased EMP damage taken. Top speed changes with flux, from +50/40/30/20 su/s at no flux to -50/40/30/20 su/s at full flux, with no change at half flux. Below half flux, also up to 50/40/30/20% increased acceleration and turn acceleration. | `flux` `flux_level` `rate_of_fire` `mobility` |
| ABSORB Reserve | Flux taken by the shield charges a reserve of up to 1000, which drains by 100 per second. While charged, up to 50% increased ballistic and energy weapon rate of fire, at full strength from 500 reserve. Non-beam weapons also cost less flux per shot, so their faster fire costs no extra flux. | `shield` `rate_of_fire` |
| Shield Flux/Damage Reduction (x2) | 5% less damage taken by shields. | `shield` `damage_taken` |
| Top Speed (x2) | +6/5/4/4 to top speed. | `mobility` |
| Affinity (x3) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |

## Enigma

14 nodes, 8 types.

| Node | Effect | Tags |
|---|---|---|
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Frozen Heart | Whenever a non-strikecraft ship is destroyed within 1500 su, this ship gains a Heartless stack until the end of combat. Ships can hold up to 5 Heartless stacks. Each new Heartless stack sets off a radiation surge that deals 400 EMP damage to a random weapon and to a random engine on this ship. 15% increased maximum combat readiness. 10% reduced top speed. 10% reduced maneuverability. | `combat_readiness` `armour` `on_kill` |
| All Must Serve | Enemy ships destroyed within 1500 su of this ship have their survivors pressed into service. Each wreck is only claimed once. Takes 5% of each such wreck's skeleton crew. They can feed Live Munitions straight away; after combat, the rest join your fleet if there is room. | `crew` `on_kill` |
| Live Munitions | Each Heartless stack has a 15% chance to sacrifice a crew member for each missile fired. 3% increased missile damage for each Heartless stack while the fleet has crew left to give. 3% increased missile top speed for each Heartless stack while the fleet has crew left to give. | `missile` `damage` `on_kill` |
| Husk Plating | Gaining a Heartless stack restores 5% of maximum armor to every armor cell. Each Heartless stack grants 2% more armor in every armor cell, above its normal maximum. 3% reduced top speed and maneuverability for each Heartless stack. | `armour` `repair` `on_kill` |
| Missile Weapon Damage (x3) | 3% increased missile weapon damage. | `missile` `damage` |
| Weapon Durability (x3) | 5% increased weapon durability. | `weapon` |
| Crew Capacity (x3) | +5/15/25/50 to crew capacity. | `crew` |

## Nightcross

17 nodes, 10 types.

| Node | Effect | Tags |
|---|---|---|
| Affinity (x3) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Energy Weapon Fire Rate (x3) | 3% increased energy weapon rate of fire. Burst beams instead recharge faster between bursts. | `energy` `rate_of_fire` |
| Ion Bolter Rounds | Energy shots that hit an enemy shield can arc through to its hull, dealing 25% of the shot's damage plus 50% as EMP. The chance is 15% when the target is at full hard flux and falls with its hard flux. | `energy` `emp` `on_hit` |
| Strikecraft Weapon Damage (x3) | Strikecraft launched from this ship have 3% increased weapon damage. | `strikecraft` `damage` |
| Hardlight Marking | Each hit by a strikecraft launched from this ship marks the enemy ship it hits, which then takes 2% increased damage from this ship's strikecraft for each mark. The marks clear 2 seconds after the last hit. Each second of beam contact counts as one hit. Strikecraft marks grant up to a maximum of 30% increased damage taken. | `strikecraft` `damage` `on_hit` |
| Dynamic Grid | Once shields have been down for 0.75/1.1/2/3 seconds while not venting or overloaded, 15% increased energy weapon rate of fire and 15% reduced energy weapon flux cost. 25% increased shield raise rate. 10% more damage taken by shields. 12/10/8/6% increased venting speed. | `energy` `rate_of_fire` `shield` |
| More Hull | 5% more hull points. | `hull` |
| Armor (x2) | +15/30/50/75 to armor. | `armour` |
| Repair Coordinator | Repairs the armor of the closest damaged allied ship within 1000 su by 1% of its total armor per second. Each ship with this effect restores at most 10% of any one ally's armor per battle, and each ally regains at most 30% in total from all of them. Pauses while overloaded or venting. | `armour` `repair` `fleet_support` |

## Stargazer

22 nodes, 12 types.

| Node | Effect | Tags |
|---|---|---|
| Top Speed | +6/5/4/4 to top speed. | `mobility` |
| Candle's Grace Replica | Enemy ships within 1500 su have up to 5% less time flow, strongest at close range, or up to 10% less while this ship is phased. Gravitational drag from every source is capped at 25% per ship. | `mobility` `nearby_ships` |
| Maneuverability (x2) | 25% increased maneuverability. | `mobility` |
| Affinity (x4) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Beam Weapon Damage (x3) | 3% increased beam weapon damage. | `beam` `energy` `damage` |
| Corrosion Beam | Every 2 seconds a beam weapon spends on an enemy ship's armor or hull, it launches a homing stardust mote at the target dealing 200 energy damage and 200 EMP. At most 20 of this ship's motes fly at once. | `beam` `energy` `on_hit` |
| Energy Weapon Fire Rate (x3) | 3% increased energy weapon rate of fire. Burst beams instead recharge faster between bursts. | `energy` `rate_of_fire` |
| Furor Machinae | Damage dealt to enemy ships builds fury, granting up to 15% increased weapon rate of fire, full after 2000/4000/8000/20000 damage. Shield hits count at half, EMP at a quarter. Fury fades by 1% per second. | `rate_of_fire` `on_hit` |
| Beautiful Nightmare | When its hull drops below 25% during a battle, the ship enters Terminal Descent until its hull is back above 40%. During Terminal Descent, 25% reduced hull damage taken, but peak performance time stops and venting is blocked. 15% increased flux dissipation during Terminal Descent. 15% increased ship system charge regeneration during Terminal Descent. 10% increased damage taken by shields during Terminal Descent. | `low_hull` `hull` `damage_taken` `combat_readiness` |
| ECM Resistance (x3) | 10% less electronic warfare penalty against this ship's weapon range. | `electronic_warfare` |
| Language of the Lost | +1 to ECM rating. Enemy ships within 1000 su have 5% chance for their weapons to malfunction. Only the strongest debuff of this effect applies at any one time. | `electronic_warfare` `nearby_ships` |

## ExoTech

17 nodes, 9 types.

| Node | Effect | Tags |
|---|---|---|
| Exoship | Docks while a Hyper-Nav Beacon is in an allocated Subroutine Socket. Docking allocates it at no cost. Undocking releases every node reached through it. Right-click to return to the beacon's socket. | (none) |
| Energy Weapon Flux Cost (x3) | 3% reduced energy weapon flux cost. | `energy` `flux` |
| Hypatia-Pattern Warpfield | 15% increased energy weapon damage. 20% increased energy weapon rate of fire. Burst beams instead recharge faster between bursts. The zero-flux speed boost also works while flux is at or below 10%. | `energy` `damage` `rate_of_fire` |
| Sensor Profile (x3) | 10% reduced sensor profile. | `sensors` |
| Phase-Fed Magazines | 100% increased ammo regeneration while phased. | `phase` `ammo` `while_phased` |
| Phase Cloak Flux Threshold (x3) | 5% increased hard flux level at which phased speed reaches its minimum. | `phase` |
| Exo-Tech Coil Tuning | 20% increased hard flux level at which phased speed reaches its minimum. 25% increased flux dissipation while phased. | `phase` `flux` `while_phased` |
| Timeflow (x3) | 2% increased time flow. | `mobility` |
| Exo-Tech Grid Splice | 10% increased time flow. 25% reduced sensor profile. 25% reduced sensor profile on phase hulls. 25% reduced EMP damage taken. 25% reduced venting speed. -5 to flux dissipation per flux vent. | `mobility` `sensors` `flux` |

## Abyss

19 nodes, 10 types.

| Node | Effect | Tags |
|---|---|---|
| Affinity (x3) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Non-Beam Energy Weapon Damage (x3) | 3% increased non-beam energy weapon damage. | `energy` `damage` |
| Salvaged Abyssal Grid | 10% reduced energy weapon flux cost. +100 to energy weapon range. 25% reduced EMP damage taken. 25% less combat readiness loss from being in a solar corona or a deep hyperspace storm. | `energy` `range` `flux` `emp` |
| Ship System Cooldown (x3) | 5% reduced ship system cooldown. | `ship_system` |
| Chronos Rhythms | 20% reduced ship system cooldown. 20% increased ship system charge regeneration. | `ship_system` |
| Weapon Durability (x3) | 5% increased weapon durability. | `weapon` |
| Borrowed Grace | Every 100 armor and hull damage this ship takes grants a stack of Borrowed Grace lasting 15 seconds, up to 20 stacks. Each stack grants 1% increased top speed, flux dissipation and weapon rate of fire, and 1% reduced weapon flux cost and damage taken. Stack timers pause while the ship is phased. 20% increased weapon durability. 20% reduced weapon and engine repair time. 20% more damage taken by shields. | `damage_taken` `mobility` `rate_of_fire` `flux` |
| Armor (x2) | +15/30/50/75 to armor. | `armour` |
| Seraph Hunting Routines | Repairs hull equal to 10% of the armor and hull damage this ship deals to enemy ships. Seraph Hunting Routines repair at most 20% of this ship's maximum hull per battle. Up to 30% increased ballistic and energy weapon damage, building up as this ship deals armor and hull damage to enemy ships. At the full bonus, the zero-flux speed boost works at any flux level. The full Seraph Hunting Routines damage bonus is reached after dealing 75000/125000/175000/225000 armor and hull damage. | `repair` `damage` `on_hit` |

## Threat

8 nodes, 6 types.

| Node | Effect | Tags |
|---|---|---|
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Fragment Swarm | The vanilla Fragment Swarm hull mod's effects (its text comes from the game). | `weapon` |
| Fragment Swarm Size (x2) | 10% increased fragment swarm size. | `weapon` |
| Fragment Coordinator | 60% increased fragment swarm size. | `weapon` |
| Fragment Replacement Rate (x2) | 5% increased fragment replacement rate. | `weapon` |
| Secondary Fabricator | 30% increased fragment replacement rate. | `weapon` |

## Tania

23 nodes, 16 types.

| Node | Effect | Tags |
|---|---|---|
| Solaris Overdrive | Weapons cost no flux to fire for 4 seconds after this ship's system activates, at most once every 60 seconds. 50% increased weapon projectile speed for those seconds. 25% increased weapon rate of fire for those seconds. | `ship_system` `flux` `rate_of_fire` |
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Affinity (x3) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
| Rise From the Ashes | Once per battle, the hit that would destroy this ship instead leaves it with 25% of its maximum hull points. Surviving that hit restores 50% of maximum armor to every armor cell. 50% less damage taken for 6 seconds after surviving that hit. | `hull` `armour` `low_hull` |
| Increased Hull (x2) | 2% more hull points. | `hull` |
| Top Speed (x2) | +6/5/4/4 to top speed. | `mobility` |
| Weapon Fire Rate (x2) | 3% increased weapon rate of fire. Burst beams instead recharge faster between bursts. | `rate_of_fire` |
| Dual-Feed Mounts | Ballistic weapon mounts become hybrid mounts. 25% increased energy weapon flux cost. | `ballistic` `energy` |
| Thermo-Accelerator | Turret weapons have 50% reduced flux cost. Hardpoint weapons can't fire. | `weapon` `flux` |
| Maneuverability (x2) | 25% increased maneuverability. | `mobility` |
| Armor | +15/30/50/75 to armor. | `armour` |
| Ballistic or Energy Weapon Damage (x2) | Choose Ballistic Weapon Damage: 3% increased ballistic weapon damage.<br>Choose Non-Beam Energy Weapon Damage: 3% increased non-beam energy weapon damage. | `ballistic` `energy` `damage`<br>Ballistic Weapon Damage: `ballistic` `damage`<br>Non-Beam Energy Weapon Damage: `energy` `damage` |
| Ballistic Projectile Speed | 10% increased ballistic weapon projectile speed. | `ballistic` |
| Dysis Drive | Top speed changes with flux, from +50/40/30/20 su/s at no flux to -50/40/30/20 su/s at full flux, with no change at half flux. Below half flux, also up to 50/40/30/20% increased acceleration and turn acceleration. | `mobility` `flux_level` |
| Carina Charge | 0.08% increased ballistic and non-beam energy weapon damage for every su/s of the ship's current speed. | `damage` `mobility` |
| Merite Lunge | 50% increased mass. 35% less maneuverability. | `hull` `mobility` |

## Shrouded

17 nodes, 8 types.

| Node | Effect | Tags |
|---|---|---|
| Wormhole Exit | Opens while a wormhole anchor for this volume is in an allocated Subroutine Socket. Opening allocates it at no cost. Closing the wormhole releases every node reached through it. Right-click to travel back to the anchor's socket. | (none) |
| Shrouded Mantle | The vanilla Shrouded Mantle hull mod's effects (its text comes from the game). | `hull` `repair` |
| Hungering Rift Healing (x3) | +10% to Hungering Rift healing received. | `hull` `repair` |
| Shrouded Lens Damage (x3) | 10% increased Shrouded Lens damage. | `damage` |
| Shrouded Lens | Strikes a random target within 400 su 1 to 4 times a second, depending on hull size, dealing 75 energy damage in an area. Each strike generates 75 flux. Keeps striking while venting or overloaded and can't be turned off. 50% increased crew casualties. | `damage` `nearby_ships` |
| Rift Lightning Damage (x3) | 10% increased Rift Lightning damage. | `damage` |
| Shrouded Thunderhead | Fires Rift Lightning at points this ship's weapons hit, more often the more hits landed in the last 5 seconds. Beams and other soft flux hits don't count. Each bolt deals 200 to 500 energy damage and 400 to 1000 EMP damage, depending on hull size, and generates flux equal to its energy damage. 50% increased crew casualties. | `damage` `on_hit` |
| Affinity (x4) | Choose High Tech Affinity: +5 to High Tech Affinity.<br>Choose Midline Affinity: +5 to Midline Affinity.<br>Choose Low Tech Affinity: +5 to Low Tech Affinity. | `low_tech` `midline` `high_tech`<br>High Tech Affinity: `high_tech`<br>Midline Affinity: `midline`<br>Low Tech Affinity: `low_tech` |
