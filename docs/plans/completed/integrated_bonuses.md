# Integrated bonuses

Integrated effects for every notable and keystone that has none yet, extending `integrated_nodes.md`. Proposed 2026-11-08.

Status: every bonus below approved and built 2026-11-08, Tania's included; not yet checked in game. All magnitudes are placeholders.

An integrated node keeps its normal effects and gains the bonus listed here, the way an s-mod adds a line to its hull mod. "X instead of Y" changes one of the normal values. The `integrated` block in `skill_types.json` is a full replacement, so each block repeats the normal effects with the bonus applied. "(new)" marks a bonus that needs a new effect or a new parameter on an existing effect; everything else uses an effect that already exists.

Where vanilla or the source mod has an s-mod bonus, that bonus is used. Where its s-mod effect is a penalty that the node doesn't carry (Fragment Swarm, the Kesteven hull mods), the bonus is our own.

Integration opens to every faction volume, not only the core notables and keystones `integrated_nodes.md` started with. Ludd's Light, At Any Cost and Crustacean Integration are joke nodes, so their bonuses stay jokey.

## Built differently from the tables

- Doctrine notables integrate through the rewritten node: integration already used the effective type, and load repair (`IntegratedNodes.forgetInapplicable`, `DoctrineArchives.rewritesIntoIntegrable`) now keeps an integration while a rewriting archive can give the node an integrable type.
- Shrouded Lens and Thunderhead use `CREW_LOSS_PERCENT` -50, which cancels vanilla's +50% exactly; Shrouded Mantle reuses `HUNGERING_RIFT_HEALING_PERCENT`.
- Rise From the Ashes reduces damage taken (50% less), so the integrated bonus is 75% less.
- Honor Guard's 1500 su and Phantom Wake's 600 su burst range are constants.
- Rift Harmonics' beams roll at most once a second per weapon.

## Hegemony

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Hullbreacher Rounds | 20% ballistic pierce chance | A pierced shot can pierce again (new) |
| Heavy Ballistics Integration | -5 large ballistic OP | -2 medium ballistic OP (new effect) |
| XIV Torpedo Doctrine | +30% missile speed, -30% missile maneuverability | +10% missile damage (`MISSILE_WEAPON_DAMAGE_PERCENT`) |
| Navy Heartbeat | 3% more ballistic damage per burn level above default | +1 burn level (`BURN_LEVEL_FLAT`), which also raises its own bonus |
| Concussion Shells | 10% disable chance on large ballistic hits | Medium ballistic hits also roll it (new) |
| Hold the Line | -15% armor damage taken at low speed | -15% shield upkeep at low speed (`SHIELD_UPKEEP_AT_LOW_SPEED_PERCENT`) |
| Domain-Pattern Laminate | -8% kinetic and HE damage taken | -8% energy damage taken (`ENERGY_DAMAGE_TAKEN_PERCENT`) |
| Flak Screen Doctrine | 100 frag flak burst within 100 su | Burst radius 150 su (new) |
| Auxiliary Screen | Smaller allies within 800 su take 10% less damage | Range 1200 su (new) |

## Core

Safety Overrides and Unstable Injector stay without integrated effects.

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Neural Interface | Transfer resets the linked ship's system cooldown and adds a charge | Transfer to the linked ship is instant, vanilla's s-mod bonus (new: `INSTANT_NEURAL_TRANSFER_FROM`) |

## Pirate

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Ground Support | Raid strength from 25% of max crew | 50% of max crew |
| Cheap Shots | +20% damage to helpless ships | Destroying a helpless ship grants a Pirate Frenzy stack (new trigger) |
| Running Dark | -10% fleet sensor profile with transponder off | +1 burn level while the transponder is off (new) |
| Blood in the Water | Pirate Frenzy stack on nearby enemy kills | Pirate Frenzy stacks last 15 seconds instead of 10 (new) |
| Cut and Run | +30% top speed while retreating | -30% damage taken while retreating (new) |
| Haul the Prize | -1 burn level, tows the slowest ship +1 burn | No -1 burn level penalty |
| Pick Off the Stragglers | +10% damage to isolated ships | +20% instead of +10% |
| Restriction Breeds Creativity | 2% less armor damage taken per d-mod | 2% less hull damage taken per d-mod (new) |
| Waste Not | +10% CR recovery, +3/6/10/15% post-battle salvage | +5/10/15/25% salvage instead |
| Freebooter's Charge | +30% damage, +50% crew loss, +10% shield damage taken, -10% armor | No armor penalty |

## Tri-Tachyon

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| TriPad Backdoor | 100% more objective capture rate | 200% instead of 100% |
| Refracting Projectiles | 20% refract chance on shields, 20% less damage per refraction | 10% refract chance on armor or hull hits (new) |
| Hybrid P-space Shrouding | 50% of sensor strength joins the phase field, -25% corona effect | 100% of sensor strength instead of 50% |
| Phase-Entangled Wings | Wing shares damage, 10% less damage taken per living wingmate | 15% less per wingmate instead of 10% |
| Ion Cascade | EMP on disabled parts dealt as HE | +15% EMP damage (`EMP_DAMAGE_PERCENT`) |
| Phase Ambush Doctrine | +25% damage on phase exit, tapering | The taper lasts twice as long (new) |
| Phantom Wake | Afterimage draws fire for 3 seconds | The afterimage bursts for 500 EMP damage when it fades (new) |
| Cease and Desist | Energy hits stack Injunction on the target | Injunction also slows the target's vent rate by the same amount (new) |

## REDACTED

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Nanoforge Mending | 1% hull per second after a pause, capped per deployment | Per-deployment cap 50% higher (new) |
| Machine Precision | +100% autofire accuracy, +5% weapon range | +10% weapon range instead of +5% |
| Terrifying Presence | -25% enemy autofire leading within range | Range 50% wider (new) |
| Relay Protocols | +10 High Tech Affinity, nav rating per affinity | +10% top speed (`TOP_SPEED_PERCENT`) |
| Adaptive Munitions | 1% damage type swap chance per affinity step | 1.5% per step instead of 1% |
| Equilibrium Protocol | +20% damage while affinities are all equal | Also applies while they are within 5 of each other (new) |
| Ion Spill | 25% of EMP on shields dealt as energy | 35% instead of 25% |
| Dormant Repair Cycle | Unhit armor regenerates (0.5) | 0.7 instead of 0.5 |

## Omega

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Disintegration | Energy hits strip 20% extra armor | 30% instead of 20% |
| Rift Harmonics | 30% rift explosion chance on energy projectile hits | Beams also trigger it, at most once a second (new) |
| Spark Rift | 10% rift chance on strikecraft energy hits | 20% instead of 10% |
| Temporal Exhaust | +20% time flow on system use, tapering over 4 seconds, rift trail | Tapers over 6 seconds instead of 4, time flow unchanged (new) |
| Resonator Payload | Missile hits trigger a rift series | One more explosion per series (new) |

## Sindrian Diktat

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Lion's Gaze | Beams split to 1 extra target, and the split beams share the beam's damage (nerf, new) | Each split beam deals full damage (new) |
| Crustacean Integration | +15% CR recovery, +10% top speed, +30% peak CR time | -10% supplies per month; the crabs eat what the crew won't (joke node) |
| Sindrian Shielding | -10% shield damage taken, -25% corona effect | -15% shield damage taken instead of -10% |
| Lion's Guard Arsenal | +100 medium energy range, -15% missile damage | The +100 range also applies to small and large energy slots (new); the missile penalty stays |
| Strategic Fuel Reserve | -10% fuel use while at least half fueled | -20% instead of -10% |
| Executor's Decree | Can't retreat, +15% damage, -5% damage taken | -10% damage taken instead of -5% |
| Lion's Roar | Enemies in range have 10% less rate of fire | 15% instead of 10% |
| Honor Guard | +50% of the flagship's base top speed | -10% damage taken within 1500 su of the flagship (new) |

## Luddic

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Ludd's Light | Explodes on death for up to 2000% of fuel capacity, allies included | Double the blast area, allies still included (new, joke node) |
| At Any Cost | Touching any hull is fatal, rams the biggest enemy | Only touching the hull of an enemy cruiser or capital is fatal (new, joke node) |
| Knight's Charge | +20% maneuverability, +25% top speed | +35% top speed instead of +25% |
| Martyr's Payload | Faster bombers, 5% payload detonation chance | Detonations no longer damage allies (new) |
| Perseverance Through Faith | 2% more weapon damage per d-mod | 2% increased weapon rate of fire and 2% reduced weapon flux cost per d-mod (new) |
| Pather's Prow | 50% more collision damage dealt and less taken | 75% instead of 50% |
| Litany of the Fallen | Hull damage reduction per crew lost, capped | Cap 50% higher (new) |
| Sanctified Scars | Allies take less armor damage per d-mod on this ship | Allies in range also gain 2% top speed per d-mod on this ship; an aura, so only the strongest applies (new) |
| Rite of Mortification | Overloads become 10% hull damage, dissipating 50% hard flux | 5% hull damage instead of 10% |

## Persean League

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| COMSEC Discipline | -25% EW penalty, +15% sensor strength | +1 ECM rating (`ELECTRONIC_WARFARE`) |
| Defensive Compact | -20% damage taken near a held objective | Range 2500 su instead of 1500 (new) |
| Joint Strike Package | Strikecraft damage per nearby strikecraft, capped | Cap 50% higher (new) |
| League Requisitions | 3% less fleet maintenance per hull type | 1% less fleet fuel use per hull type (new) |
| Kazeron Recovery Standards | 10% d-mod avoid chance on recovery | 20% instead of 10% |
| League Picket Targeting | +1 energy range per sensor strength | Ballistic weapons gain it too (new) |
| League Reload Drill | +50% ballistic and energy ammo regen | Ballistic and energy weapons that use ammo have a 10% chance not to consume it when firing (new) |
| Shared Fate | 20% of shield damage shared with allies, -20% weapon damage | -10% weapon damage instead of -20% |
| Coalition Flight Doctrine | Wings lend each other 1 copy of each strikecraft type | Strikecraft launched from this ship take 15% less damage (`FIGHTER_DAMAGE_TAKEN_MULT`) |

## Kesteven

LOST_SECTOR's s-mod effects are penalties. Critical Point Refit, Volatile Flux Tuning and Inertial Supercharging take them on as normal effects, and integrating removes them.

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Critical Point Refit | Armor bonus scaled by low base armor, -30% maneuverability (nerf, `MANEUVERABILITY_MULT`) | No maneuverability penalty |
| Inertial Supercharging | Damage per su/s of speed, +50% weapon damage taken on frigates (nerf, new) | No weapon damage taken penalty |
| ABSORB Reserve | Shield flux charges a rate of fire reserve | Reserve maximum 50% higher (new) |
| Volatile Flux Tuning | Rate of fire swings ±40% with flux, plus capacity and range changes, +33% EMP damage taken (nerf, `EMP_DAMAGE_TAKEN_PERCENT`) | No EMP damage taken penalty |
| Kesteven High-Energy Weapon Systems (not on the tree) | +100 medium energy range, -10% energy flux cost | -20% energy flux cost instead of -10% |
| High Capacitance Banks (not on the tree) | +1 system charge, +10% charge regen on Augmented hulls | +20% charge regen instead of +10% |

## Enigma

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Live Munitions | +3% missile damage and top speed per Heartless stack, 15% crew sacrifice chance per stack (nerf from 9%) | 20% crew sacrifice chance, and +4% missile damage and top speed per stack |
| All Must Serve | Takes 5% of nearby wrecks' skeleton crew (nerf from 10%) | 10% instead of 5% |
| Husk Plating | Armor per Heartless stack, -3% mobility per stack | -2% mobility per stack instead of -3% |
| Frozen Heart | Heartless stacks from nearby deaths, 400 EMP radiation surge per stack, -10% top speed and maneuverability | 300 EMP surge, and -8% top speed and maneuverability |

## Nightcross

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Dynamic Grid | Energy bonus while shields are down, +10% shield damage taken | No shield damage penalty |
| Ion Bolter Rounds | Up to 15% arc-through chance on shields | Up to 20% instead of 15% |
| Hardlight Marking | Strikecraft marks, up to 30% more damage taken | Up to 35% instead of 30% |
| Repair Coordinator | Repairs 1% of an ally's armor per second | 1.5% per second instead of 1% |

## Stargazer

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Candle's Grace Replica | Up to 5% less enemy time flow nearby | Up to 8% instead of 5% |
| Beautiful Nightmare | Terminal Descent below a hull threshold, +10% shield damage taken during it | No shield damage penalty during Terminal Descent |
| Furor Machinae | Up to 15% rate of fire from fury | Up to 20% instead of 15% |
| Language of the Lost | +1 EW, 5% enemy weapon malfunction aura | +2 EW instead of +1 |
| Corrosion Beam | Beam time launches stardust motes, capped | One more mote in flight at once (new) |

## ExoTech

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Exo-Tech Grid Splice | +10% time flow, lower profile and EMP taken, -25% vent rate, -5 dissipation per vent | No vent rate penalty |
| Exo-Tech Coil Tuning | +20% phase flux threshold, +25% dissipation while phased | +30% threshold instead of +20% |
| Phase-Fed Magazines | +100% ammo regen while phased | +25% ballistic and energy ammo (`BALLISTIC_WEAPON_AMMO_PERCENT`, `ENERGY_WEAPON_AMMO_PERCENT`) |
| Hypatia-Pattern Warpfield | +15% energy damage, +20% energy rate of fire, zero-flux boost up to 10% flux | Zero-flux boost up to 15% flux instead of 10% |

## Abyss

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Borrowed Grace | Stacks from damage taken, +20% shield damage taken | No shield damage penalty |
| Salvaged Abyssal Grid | -10% energy flux cost, +100 energy range, less EMP and corona | +150 energy range instead of +100 |
| Seraph Hunting Routines | Heals from damage dealt, capped at 20% hull per battle | Capped at 30% instead of 20% |
| Chronos Rhythms | -20% system cooldown, +20% charge regen | -30% and +30% instead of 20% |

## Threat

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Fragment Swarm | Vanilla Fragment Swarm | 10% of shield damage absorbed by fragments (`SHIELD_DAMAGE_ABSORBED_BY_FRAGMENTS_PERCENT`); vanilla's s-mod effect is only a penalty |
| Secondary Fabricator | +30% fragment replacement rate | +50% instead of +30%, vanilla's s-mod bonus |
| Fragment Coordinator | +60% fragment swarm size | +100% instead of +60%, vanilla's s-mod bonus |

## Shrouded

Vanilla's Shrouded Lens and Thunderhead have no s-mod effect (the code is commented out). Shrouded Mantle's s-mod raises the Hungering Rift's heal on human ships by 50%.

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Shrouded Lens | Vanilla Shrouded Lens, +50% crew casualties | No extra crew casualties (`CREW_LOSS_MULT` -33 offsets it) |
| Shrouded Mantle | Vanilla Shrouded Mantle | Hungering Rift heals human ships 50% more, vanilla's s-mod bonus (new: `HUNGERING_RIFT_HEAL_MOD_HUMAN_SHIPS`) |
| Shrouded Thunderhead | Vanilla Shrouded Thunderhead, +50% crew casualties | No extra crew casualties (`CREW_LOSS_MULT` -33 offsets it) |

## Tania

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Dual-Feed Mounts | Ballistic mounts become hybrid, +25% energy flux cost | +10% energy flux cost instead of +25% |
| Dysis Drive | Top speed swings ±50/40/30/20 with flux | ±60/50/40/25 instead |
| Carina Charge | 0.08% damage per su/s of speed | 0.1% instead of 0.08% |
| Solaris Overdrive | Free weapon fire for 4 seconds after system use | 6 seconds instead of 4 |
| Rise From the Ashes | Survives a killing blow once, then 50% more damage taken briefly | 25% more damage taken instead of 50% |
| Thermo-Accelerator | -50% turret flux cost, hardpoints can't fire | -60% turret flux cost instead of -50% |
| Merite Lunge | +50% mass, -35% maneuverability | -20% maneuverability instead of -35% |

## Doctrine Archives

Plain stat notables, so each raises one line.

| Node | Normal effect | Integrated bonus |
| --- | --- | --- |
| Alpha-Grade Fire Control | +50% autofire accuracy, +10% energy range | +15% energy range instead of +10% |
| Flux Arbitrage | +10% dissipation, +25% vent rate | +15% dissipation instead of +10% |
| Quarterly Targets | +10% damage to cruisers and capitals | +15% instead of +10% |
| Tachyon Lance Licensing | +15% beam damage, +10% beam range | +15% beam range instead of +10% |
| Black Ledger Coil Tuning | -25% phase activation cost, +25% dissipation while phased | -40% activation cost instead of -25% |
| Escrowed Emitters | -10% shield damage taken, -25% shield upkeep | -15% shield damage taken instead of -10% |
| Hostile Takeover | +20% energy damage, +5% time flow, -30% peak CR time | -15% peak CR time instead of -30% |
| Blessed Plating | +20% armor, +15% hull | +30% armor instead of +20% |
| Faithful Hands | -25% repair time, +25% CR recovery | -35% repair time instead of -25% |
| Pilgrim's Burn | +1 burn level, -15% fuel use | -25% fuel use instead of -15% |
| Righteous Fury | +15% ballistic and missile damage | +20% each instead of +15% |
| Penitent Hulls | -25% d-mod effects, -10% supplies per month | -40% d-mod effects instead of -25% |
| Pilgrim Volunteers | +20% peak CR time, -25% crew loss | +30% peak CR time instead of +20% |
| Swarm Logic | +25% fighter replacement recovery, +15% fighter damage | +25% fighter damage instead of +15% |
| Unblinking Watch | +50% PD damage to missiles, +30% weapon turn rate | +50% turn rate instead of +30% |
| Cold Logic | -25% CR loss per second, +10% max CR | +15% max CR instead of +10% |
| Fortress Lattice | +50% shield turn and raise rate, -25% EMP taken | +100% shield turn and raise rate instead of +50% |
| Lumen Harmonics | +10% energy rate of fire, -10% energy flux cost | +15% energy rate of fire instead of +10% |
| Ordo Signal Mesh | +1 EW, +sensor strength | +2 EW instead of +1 |
| Tireless Cadence | +10% time flow, +15% dissipation, -50% CR recovery | -25% CR recovery instead of -50% |

## Steps

1. Done. Bonuses agreed with the user, 2026-11-08.
2. Done. The nerfs to the normal nodes in `skill_types.json`: Lion's Gaze, Critical Point Refit, Inertial Supercharging, Volatile Flux Tuning, Live Munitions and All Must Serve.
3. Done. The new effects, each in its part's `skilleffect` package with tests and en/zh_CN lines. Shared pieces: `KillWatch`, `PerDModPercentEffect`, `Flagships`, `TaperedBonus`, `DModAuraField`, `EnergySlotRangeListener`, `FleetWideEffects.perHullTypeReduction`.
4. Done. The `integrated` blocks for every type above, 127 in all, in `skill_types.json`, built on one branch per volume and merged on `integrated-bonuses`.
