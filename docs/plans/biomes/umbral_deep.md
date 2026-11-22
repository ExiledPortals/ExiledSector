# Umbral Deep modifiers

Hull items dropped in Umbral Deep systems can carry an Umbral Deep modifier. The biome is abyssal hyperspace holding only Shrouded Dweller fleets, so every modifier copies one of their combat mechanics: the shroud's blindness to sensors, Hungering Rift's healing, the lunge away from damage, Inimical Emanation's repair disruption, Tenebrous Expulsion's ejecta, the Lens strikes, the Churning Locus vortices and the free vents of the shrouded hullmods. Three of them work through Shrouded hullmods and nodes, as the Threat Expanse set works through the Fragment Swarm. The other six work on any ship; phase hulls can't take a shrouded hullmod and the Phase Coil Array only fits phase hulls.

No name, line or description names the Dwellers. Lines name vanilla's own terms ("Shrouded hullmod or node", Hungering Rift) where they need to, and nothing more.

Status: designed 2026-11-22, not built. Rows go in `item_modifiers.csv` ending `_UMBRAL_DEEP` and are mapped in `biome_modifiers.csv`, through the existing framework (`../completed/anomalies/abyssal_rift.md`, Biome modifiers). Values are placeholders.

## Modifiers

| Item | Name | Effect | Placeholder | Dweller mechanic |
|---|---|---|---|---|
| Command Suite | of the Unseen (suffix) | `SENSOR_PROFILE_PERCENT_PER_SHROUDED_SHIP`, fleet-wide: reduced fleet sensor profile for each ship with a Shrouded hullmod or node | 2-3% for each ship, up to a maximum of 30% | dweller hulls have no sensor profile |
| Specialist Crew | Inured (prefix) | `CREW_LOSS_PERCENT_PER_SHROUDED`: reduced crew casualties for each Shrouded hullmod or node on this ship | 15-25% for each | offsets the shroud's 50% increased crew casualties |
| Drive Assembly | Convulsive (prefix) | `HULL_LOSS_LUNGE_SPEED_PERCENT`: after this ship loses 10% of its maximum hull, increased top speed and maneuverability for 4 seconds | 20-30%, at most once every 10 seconds | Convulsive Lunge and Shrouded Mantle's recoil from damage |
| Reactor Regulator | of Exhalation (suffix) | `VENT_RATE_PERCENT_PER_SHROUDED`: increased active vent rate for each Shrouded hullmod or node on this ship | 20-30% for each | shrouded hullmods keep striking for free while venting |
| Fire Control Suite | Inimical (prefix) | `HULL_HIT_REPAIR_DISRUPTION_CHANCE_PERCENT`: chance for a hull hit from this ship's weapons to double the target's weapon and engine repair time for 5 seconds | 10-20% chance | Inimical Emanation's repair disruption (vanilla `RDRepairRateDebuff`) |
| Shield Emitter | of Expulsion (suffix) | `OVERLOAD_EJECTA_FLAT`: when this ship overloads, it expels ejecta that block shots | 2-3 ejecta, once per overload | Tenebrous Expulsion's Shrouded Ejecta |
| Phase Coil Array | of Glare (suffix) | `PHASE_EXIT_LENS_STRIKES_FLAT`: when this ship leaves phase, it strikes random enemies within 400 su for 75 energy damage each | 2-4 strikes | Shrouded Lens strikes |
| Launch Assembly | Churning (prefix) | `STRIKECRAFT_DEATH_VORTEX_CHANCE_PERCENT`: chance for a destroyed strikecraft from this ship to release a vortex that hunts nearby enemies | 5-10% chance | Churning Locus and its Shrouded Vortex |
| Armor Plate | Hungering (prefix) | `HULL_DAMAGE_ALLY_REPAIR_PERCENT`: hull damage dealt by this ship repairs the most damaged ship in your fleet within 2000 su | 5-10% of the damage dealt | Hungering Rift healing its allies |

## Mechanics

A Shrouded hullmod is any hullmod whose spec has vanilla's `shrouded` tag: Shrouded Mantle, Shrouded Lens and Shrouded Thunderhead. A Shrouded node is an allocated node of one of the six Shrouded types: the Shrouded Mantle, Shrouded Lens and Shrouded Thunderhead notables, Shrouded Lens Damage, Rift Lightning Damage and Hungering Rift Healing. The three notables add their real hullmod as a phantom perma mod, so a notable and the hullmod it grants count once, not twice. One shared counter gives each ship its count of Shrouded hullmods and nodes; Inured and of Exhalation scale with it and do nothing at 0, and of the Unseen counts the fleet's ships with at least 1. Their lines say "Shrouded hullmod or node".

Of the Unseen follows the Threat Expanse's of the Swarm: the fleet takes the value of its highest carrier once, multiplied by its number of ships with a Shrouded hullmod or node, synced per fleet. It modifies the fleet's detected range as the Abyssal Rift's sensor profile modifier does.

Convulsive tracks hull loss the way Shrouded Mantle does: the burst triggers once hull drops 10% of maximum below the highest hull since the last burst, then waits 10 seconds. Inimical applies vanilla's own repair debuff, so the target shows vanilla's status for it; a ship already under it only has the 5 seconds refreshed. Hungering repairs any allied ship, shrouded or not, picked by missing hull as Hungering Rift picks; the dealing ship may pick itself. Fighters and modules aren't picked.

Of Expulsion and Churning spawn vanilla's `shrouded_ejecta_wing` and `shrouded_vortex_wing` on this ship's side, as Tenebrous Expulsion and Churning Locus spawn them. Ejecta slow to a stop and block shots for about 15 seconds. A vortex hunts the nearest enemy, loses hull over about 20 seconds and explodes on death for up to 2000 energy damage. Of Glare's strikes copy Shrouded Lens: a 75 energy damage explosion of radius 50 with no friendly fire, on random enemy ships and missiles within 400 su (edge to edge), closer targets more likely, skipping phased ones. Each hit is a separate random target, and the same target can be hit more than once.

## To verify before building

- Spawned ejecta and vortices on the player's side: they carry the built-in dweller hullmod and its AI. Check that the AI works for side 0, that the fleet AI's stop rule (a non-dweller ship on its side) doesn't stop the wings, and that they leave no hulks or recovery entries after battle.
- The vortex explosion uses the vortex's own owner, so it can't hit our side (vanilla's is no friendly fire).
- Whether the Mantle's own lunge and Convulsive's burst fight each other when both trigger together. If they do, Convulsive waits for the lunge to end.

## Build order

1. [ ] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, the `chart.biomeModifier.header.umbral_deep` header and the line strings (en and zh_CN).
2. [ ] The new effects, each in its ship part's `skilleffect` package with tests: the shared Shrouded hullmod and node counter; the fleet-wide sensor profile (logistics, beside `FleetWideEffects.applySwarmSensorStrength`); the two per-count stat rows; the hull-loss burst (movement); the repair disruption (onhit); the overload ejecta (shield); the phase exit strikes (phase); the strikecraft vortex (fighter); the ally repair (defense).
3. [ ] `docs/docs.md`, Biome modifiers: only if a mechanic here needs it (likely the fleet's shrouded ship count, as for the Threat Expanse).
