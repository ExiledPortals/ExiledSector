# Hull item modifier theme tags review

One group per hull item class (the socket it goes in). Each group lists the class's implicit, then its rollable prefixes on the left and suffixes on the right, then the fixed lines of its unique items. Biome modifiers are listed in the pool of the class they drop on, marked with their biome. Effects are the codex wording from the mod's own text code, with each line's roll range. Modifiers not tied to one class are listed at the end.

## Weapon Mount

Base item: Fire Control Suite. 16 unique items.

Implicit: 10 to 20% increased weapon turn rate. (<code>weapon</code>)

<table>
<tr><th>Prefixes (14)</th><th>Suffixes (25)</th></tr>
<tr valign="top"><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>5 to 10% increased ballistic weapon damage.</td><td><code>ballistic</code> <code>damage</code></td></tr>
<tr><td>5 to 10% increased non-beam energy weapon damage.</td><td><code>energy</code> <code>damage</code></td></tr>
<tr><td>5 to 10% increased beam weapon damage.</td><td><code>beam</code> <code>damage</code></td></tr>
<tr><td>5 to 10% increased missile weapon damage.</td><td><code>missile</code> <code>damage</code></td></tr>
<tr><td>5 to 7% increased ballistic weapon rate of fire.</td><td><code>ballistic</code> <code>rate_of_fire</code></td></tr>
<tr><td>5 to 7% increased energy weapon rate of fire. Burst beams instead recharge faster between bursts.</td><td><code>energy</code> <code>rate_of_fire</code></td></tr>
<tr><td>4 to 6% increased missile weapon rate of fire.</td><td><code>missile</code> <code>rate_of_fire</code></td></tr>
<tr><td>3 to 5% increased weapon damage.</td><td><code>damage</code></td></tr>
<tr><td>3 to 5% increased ballistic weapon damage. 3 to 5% increased ballistic weapon range.</td><td><code>ballistic</code> <code>damage</code> <code>range</code></td></tr>
<tr><td>Ballistic projectile hits on an enemy ship's armor or hull have a 5 to 10% chance to pierce.</td><td><code>ballistic</code> <code>on_hit</code></td></tr>
<tr><td>Non-beam energy weapons have a 3 to 5% chance to refract when hitting enemy shields.</td><td><code>energy</code> <code>on_hit</code></td></tr>
<tr><td>5 to 10% of beam weapon damage to shields is dealt as hard flux.</td><td><code>beam</code> <code>on_hit</code></td></tr>
<tr><td>Beam weapon hits split their damage evenly across the target and up to 1 additional nearby enemy ship. Strikecraft are never chosen. The target acquisition range is half the beam weapon's range.</td><td><code>beam</code> <code>on_hit</code></td></tr>
<tr><td>10 to 15% more EMP damage.<br><i>Remnant Expanse drop only</i></td><td><code>emp</code> <code>damage</code></td></tr>
</table>

</td><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>5 to 10% increased ballistic weapon range.</td><td><code>ballistic</code> <code>range</code></td></tr>
<tr><td>5 to 10% increased non-beam energy weapon range.</td><td><code>energy</code> <code>range</code></td></tr>
<tr><td>10 to 15% increased beam weapon range.</td><td><code>beam</code> <code>range</code></td></tr>
<tr><td>10 to 15% increased ballistic weapon ammo capacity.</td><td><code>ballistic</code> <code>ammo</code></td></tr>
<tr><td>10 to 15% increased energy weapon ammo capacity.</td><td><code>energy</code> <code>ammo</code></td></tr>
<tr><td>10 to 15% increased missile weapon ammo capacity.</td><td><code>missile</code> <code>ammo</code></td></tr>
<tr><td>10 to 15% reduced weapon recoil.</td><td><code>weapon</code></td></tr>
<tr><td>10 to 20% increased damage to missiles.</td><td><code>point_defense</code> <code>damage</code></td></tr>
<tr><td>10 to 15% increased ballistic weapon projectile speed.</td><td><code>ballistic</code></td></tr>
<tr><td>5 to 10% increased energy weapon projectile speed.</td><td><code>energy</code></td></tr>
<tr><td>5 to 10% increased missile weapon projectile speed.</td><td><code>missile</code></td></tr>
<tr><td>5 to 10% increased ballistic weapon ammo regeneration rate.</td><td><code>ballistic</code> <code>ammo</code></td></tr>
<tr><td>10 to 15% increased missile weapon ammo regeneration rate.</td><td><code>missile</code> <code>ammo</code></td></tr>
<tr><td>10 to 15% increased missile turn rate.</td><td><code>missile</code></td></tr>
<tr><td>15 to 25% increased missile hitpoints.</td><td><code>missile</code></td></tr>
<tr><td>10 to 20% increased chance for missiles to resist enemy ECM and flares.</td><td><code>missile</code> <code>electronic_warfare</code></td></tr>
<tr><td>Point-defense weapons get the best possible target leading, regardless of combat readiness. Point-defense weapons can identify and ignore decoy flares.</td><td><code>point_defense</code></td></tr>
<tr><td>10 to 20% of beam weapon damage to shields is dealt as hard flux.<br><i>Abyssal Rift drop only</i></td><td><code>beam</code> <code>on_hit</code></td></tr>
<tr><td>10 to 20% increased damage to overloaded, venting or flamed-out ships.<br><i>Pirate Badlands drop only</i></td><td><code>damage</code> <code>conditional</code></td></tr>
<tr><td>Weapon hits on enemy armor burn it for 3 to 5% of the hit's damage over 3 seconds.<br><i>Furnace drop only</i></td><td><code>damage</code> <code>on_hit</code></td></tr>
<tr><td>1% increased energy weapon damage for every 5 High Tech Affinity.<br><i>Frozen Fringe drop only</i></td><td><code>energy</code> <code>damage</code> <code>high_tech</code></td></tr>
<tr><td>Beam weapon hits on armor and hull deal extra EMP damage equal to 5 to 10% of their damage.<br><i>Collapsed Reach drop only</i></td><td><code>beam</code> <code>emp</code> <code>on_hit</code></td></tr>
<tr><td>Every 300 hull damage this ship deals to enemy ships regrows 1 of its fragment swarm's fragments, up to the swarm's full size.<br><i>Threat Expanse drop only</i></td><td><code>on_hit</code></td></tr>
<tr><td>5 to 10% increased damage to ships without d-mods.<br><i>Derelict Graveyard drop only</i></td><td><code>damage</code> <code>conditional</code></td></tr>
<tr><td>1% increased weapon rate of fire for every 10 Midline Affinity.<br><i>Storm Belt drop only</i></td><td><code>rate_of_fire</code> <code>midline</code></td></tr>
</table>

</td></tr>
</table>

### Unique items

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>15 to 20% increased ballistic weapon range.<br><i>Coatl Bastion Gunnery Team</i></td><td><code>ballistic</code> <code>range</code></td></tr>
<tr><td>10 to 15% increased ballistic weapon rate of fire.<br><i>Coatl Bastion Gunnery Team</i></td><td><code>ballistic</code> <code>rate_of_fire</code></td></tr>
<tr><td>10 to 15% less ballistic weapon flux cost.<br><i>Coatl Bastion Gunnery Team</i></td><td><code>ballistic</code> <code>flux</code></td></tr>
<tr><td>40 to 60% increased missile weapon ammo capacity.<br><i>Gryphon Missile Crew</i></td><td><code>missile</code> <code>ammo</code></td></tr>
<tr><td>20 to 30% increased missile weapon ammo regeneration rate.<br><i>Gryphon Missile Crew</i></td><td><code>missile</code> <code>ammo</code></td></tr>
<tr><td>25 to 30% increased missile weapon rate of fire.<br><i>Gryphon Missile Crew</i></td><td><code>missile</code> <code>rate_of_fire</code></td></tr>
<tr><td>Missile weapons that cannot regenerate ammo reload 10 to 20% of their base ammo every minute in combat.<br><i>Nanoforge Seed</i></td><td><code>missile</code> <code>ammo</code></td></tr>
<tr><td>15 to 20% increased weapon rate of fire. Burst beams instead recharge faster between bursts.<br><i>Deserter Gunners</i></td><td><code>rate_of_fire</code></td></tr>
<tr><td>3 to 5% increased weapon malfunction chance.<br><i>Deserter Gunners</i></td><td><code>weapon</code></td></tr>
<tr><td>20 to 30% increased damage to capital ships and stations.<br><i>Planetkiller Resonance Circuit</i></td><td><code>damage</code></td></tr>
<tr><td>Kinetic damage from weapons in ballistic hardpoints is converted to high explosive damage.<br><i>Chalcedon Charge Shells</i></td><td><code>ballistic</code> <code>kinetic</code> <code>high_explosive</code></td></tr>
<tr><td>Energy weapon mounts become ballistic mounts.<br><i>Purifier Breech Refit</i></td><td><code>energy</code> <code>ballistic</code></td></tr>
<tr><td>Ballistic weapon mounts become energy mounts.<br><i>Culann Conversion Kit</i></td><td><code>ballistic</code> <code>energy</code></td></tr>
<tr><td>All damage from weapons in small energy mounts is converted to EMP damage.<br><i>Ion Discharge Coils</i></td><td><code>energy</code> <code>emp</code></td></tr>
<tr><td>Energy damage from beam weapons is converted to high explosive damage.<br><i>Corrosion Lens Filter</i></td><td><code>beam</code> <code>energy_damage</code> <code>high_explosive</code></td></tr>
<tr><td>1% increased ballistic weapon damage for every 5 Low Tech Affinity.<br><i>Old Guard Gun Captains</i></td><td><code>ballistic</code> <code>damage</code> <code>low_tech</code></td></tr>
<tr><td>1% increased energy weapon damage for every 5 High Tech Affinity.<br><i>Tachyon Lens Technicians</i></td><td><code>energy</code> <code>damage</code> <code>high_tech</code></td></tr>
<tr><td>1% increased missile weapon damage for every 5 Midline Affinity.<br><i>Line Yard Ordnance Crew</i></td><td><code>missile</code> <code>damage</code> <code>midline</code></td></tr>
<tr><td>Missile weapon mounts become energy mounts.<br><i>Ordo Feed Conversion</i></td><td><code>missile</code> <code>energy</code></td></tr>
<tr><td>Non-missile projectiles that hit an enemy ship have a 15 to 25% chance to be fired again from the same weapon.<br><i>Reality Disruptor Echo</i></td><td><code>ballistic</code> <code>energy</code> <code>on_hit</code></td></tr>
<tr><td>Small weapon mounts become universal mounts.<br><i>Facet Mount Geometry</i></td><td><code>weapon</code></td></tr>
<tr><td>+1 to +2 to ordnance point cost of small weapons.<br><i>Facet Mount Geometry</i></td><td><code>weapon</code></td></tr>
</table>

## Crew Quarters

Base item: Specialist Crew. 8 unique items.

Implicit: 5 to 10% reduced minimum crew required. (<code>crew</code>)

<table>
<tr><th>Prefixes (15)</th><th>Suffixes (8)</th></tr>
<tr valign="top"><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>10 to 15% increased crew capacity.</td><td><code>crew</code></td></tr>
<tr><td>10 to 15% reduced crew casualties.</td><td><code>crew</code></td></tr>
<tr><td>10 to 15% less supply use for maintenance.</td><td><code>supplies</code></td></tr>
<tr><td>5 to 10% less weapon and engine repair time.</td><td><code>repair</code> <code>weapon</code></td></tr>
<tr><td>5 to 10% increased peak performance time.</td><td><code>combat_readiness</code></td></tr>
<tr><td>5 to 10% less negative effects from d-mods.</td><td><code>d_mods</code></td></tr>
<tr><td>5 to 8% increased crew capacity. 5 to 8% reduced crew casualties.</td><td><code>crew</code></td></tr>
<tr><td>Enemy ships destroyed within 1000 to 1500 su of this ship have their survivors pressed into service. Each wreck is only claimed once. Takes 5 to 10% of each such wreck's skeleton crew. They can feed Live Munitions straight away; after combat, the rest join your fleet if there is room.</td><td><code>crew</code> <code>on_kill</code> <code>nearby_ships</code></td></tr>
<tr><td>+30 to +40 to Low Tech Affinity.<br><i>Pirate Badlands drop only</i></td><td><code>low_tech</code></td></tr>
<tr><td>+30 to +40 to Low Tech Affinity.<br><i>Furnace drop only</i></td><td><code>low_tech</code></td></tr>
<tr><td>Your fleet has 3 to 5% less supply use for maintenance while moving slowly. Only the strongest applies.<br><i>Frozen Fringe drop only</i></td><td><code>supplies</code></td></tr>
<tr><td>Your fleet has 5% increased quantity of hull items found in Star Chart systems with a black hole.<br><i>Collapsed Reach drop only</i></td><td><code>salvage</code></td></tr>
<tr><td>1% increased fragment swarm size for every 5 affinity.<br><i>Threat Expanse drop only</i></td><td><code>low_tech</code> <code>midline</code> <code>high_tech</code></td></tr>
<tr><td>Your fleet has 5 to 10% increased quantity of weapons and strikecraft wings in post-battle salvage.<br><i>Derelict Graveyard drop only</i></td><td><code>salvage</code></td></tr>
<tr><td>+30 to +40 to Midline Affinity.<br><i>Storm Belt drop only</i></td><td><code>midline</code></td></tr>
</table>

</td><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>Your fleet has 3 to 5% increased post-battle salvage.</td><td><code>salvage</code></td></tr>
<tr><td>10 to 15% increased repair rate per day.</td><td><code>repair</code></td></tr>
<tr><td>10 to 20% increased combat readiness recovery rate.</td><td><code>combat_readiness</code></td></tr>
<tr><td>Your fleet gains effective planetary raid strength equal to 5 to 10% of this ship's maximum crew capacity, up to the total number of marines in the fleet.</td><td><code>crew</code></td></tr>
<tr><td>Your fleet has -5/10/15/25 to supplies needed to perform surveys. Your fleet has -5/10/15/25 to heavy machinery needed to perform surveys.</td><td><code>supplies</code></td></tr>
<tr><td>Your fleet has 1 to 3% increased post-battle salvage. 5 to 8% increased repair rate per day.</td><td><code>salvage</code> <code>repair</code></td></tr>
<tr><td>If disabled, this ship is almost always recoverable after the battle.</td><td><code>salvage</code></td></tr>
<tr><td>+30 to +40 to High Tech Affinity.<br><i>Remnant Expanse drop only</i></td><td><code>high_tech</code></td></tr>
</table>

</td></tr>
</table>

### Unique items

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>+2 to +3 to ECM rating.<br><i>COMSEC Signals Detachment</i></td><td><code>electronic_warfare</code></td></tr>
<tr><td>20 to 30% reduced sensor profile.<br><i>COMSEC Signals Detachment</i></td><td><code>sensors</code></td></tr>
<tr><td>25 to 40% reduced electronic warfare penalty against this ship's weapon range.<br><i>COMSEC Signals Detachment</i></td><td><code>electronic_warfare</code></td></tr>
<tr><td>20 to 30% increased crew capacity.<br><i>Auxiliary Conscripts</i></td><td><code>crew</code></td></tr>
<tr><td>15 to 20% reduced minimum crew required.<br><i>Auxiliary Conscripts</i></td><td><code>crew</code></td></tr>
<tr><td>Your fleet has +50 to +100 to effective planetary raid strength, up to the total number of marines in the fleet.<br><i>Olinadu Security Detachment</i></td><td><code>logistics</code></td></tr>
<tr><td>10 to 15% reduced crew casualties.<br><i>Olinadu Security Detachment</i></td><td><code>crew</code></td></tr>
<tr><td>25 to 40% increased repair rate per day.<br><i>Asher Salvage Guild Crew</i></td><td><code>repair</code></td></tr>
<tr><td>Your fleet has 8 to 12% increased post-battle salvage.<br><i>Asher Salvage Guild Crew</i></td><td><code>salvage</code></td></tr>
<tr><td>30 to 50% increased damage to automated ships.<br><i>Pather Cell</i></td><td><code>damage</code></td></tr>
<tr><td>30 to 40% reduced peak performance time.<br><i>Pather Cell</i></td><td><code>combat_readiness</code></td></tr>
<tr><td>Your fleet has 6 to 11% increased post-battle salvage.<br><i>Salvager Crew</i></td><td><code>salvage</code></td></tr>
<tr><td>Your fleet has -5 to -10 to supplies needed to perform surveys.<br><i>Salvager Crew</i></td><td><code>supplies</code></td></tr>
<tr><td>15 to 25% increased repair rate per day.<br><i>Salvager Crew</i></td><td><code>repair</code></td></tr>
<tr><td>Your fleet has -5 to -10 to heavy machinery needed to perform surveys.<br><i>Salvager Crew</i></td><td><code>logistics</code></td></tr>
<tr><td>Your fleet has 10 to 15% increased post-battle salvage.<br><i>Ko Combine Shipbreakers</i></td><td><code>salvage</code></td></tr>
<tr><td>If disabled, this ship is almost always recoverable after the battle.<br><i>Ko Combine Shipbreakers</i></td><td><code>salvage</code></td></tr>
<tr><td>5% less armor damage taken for each d-mod, up to a maximum of 5 d-mods.<br><i>Kanta's Den Scrapdogs</i></td><td><code>armour</code> <code>damage_taken</code> <code>d_mods</code></td></tr>
<tr><td>3% more weapon damage for each d-mod, up to a maximum of 5 d-mods.<br><i>Kanta's Den Scrapdogs</i></td><td><code>damage</code> <code>d_mods</code></td></tr>
<tr><td>2% more top speed for each d-mod, up to a maximum of 5 d-mods.<br><i>Kanta's Den Scrapdogs</i></td><td><code>mobility</code> <code>d_mods</code></td></tr>
<tr><td>7 to 10% more negative effects from combat d-mods for each d-mod, up to a maximum of 5 d-mods.<br><i>Kanta's Den Scrapdogs</i></td><td><code>d_mods</code></td></tr>
<tr><td>21 to 30% more negative effects from logistics d-mods for each d-mod, up to a maximum of 5 d-mods.<br><i>Kanta's Den Scrapdogs</i></td><td><code>d_mods</code> <code>logistics</code></td></tr>
</table>

## Bridge

Base item: Command Suite. 8 unique items.

Implicit: 10 to 20% increased combat readiness recovery rate. (<code>combat_readiness</code>)

<table>
<tr><th>Prefixes (18)</th><th>Suffixes (14)</th></tr>
<tr valign="top"><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>3 to 5% increased maximum combat readiness.</td><td><code>combat_readiness</code></td></tr>
<tr><td>5 to 10% increased peak performance time.</td><td><code>combat_readiness</code></td></tr>
<tr><td>5 to 10% reduced rate of combat readiness loss from extended deployment.</td><td><code>combat_readiness</code></td></tr>
<tr><td>10 to 20% increased target leading accuracy of autofiring weapons.</td><td><code>weapon</code></td></tr>
<tr><td>+30 to +50 to this ship's sensor strength.</td><td><code>sensors</code></td></tr>
<tr><td>5 to 10% reduced ship system cooldown.</td><td><code>ship_system</code></td></tr>
<tr><td>+1/1/2/2 to ECM rating.</td><td><code>electronic_warfare</code></td></tr>
<tr><td>5 to 10% increased ship system charge regeneration.</td><td><code>ship_system</code></td></tr>
<tr><td>3 to 5% reduced weapon malfunction chance.</td><td><code>weapon</code></td></tr>
<tr><td>2 to 3% increased maximum combat readiness. 2 to 4% increased peak performance time.</td><td><code>combat_readiness</code></td></tr>
<tr><td>+20 to +30 to this ship's sensor strength. +1/1/1/1 to ECM rating.</td><td><code>sensors</code> <code>electronic_warfare</code></td></tr>
<tr><td>3 to 5% increased time flow.<br><i>Abyssal Rift drop only</i></td><td><code>mobility</code></td></tr>
<tr><td>10 to 15% increased weapon damage for the first 30 seconds after this ship deploys.<br><i>Pirate Badlands drop only</i></td><td><code>damage</code> <code>conditional</code></td></tr>
<tr><td>5 to 10% increased damage to enemy ships an allied ship has targeted.<br><i>Remnant Expanse drop only</i></td><td><code>damage</code> <code>conditional</code></td></tr>
<tr><td>Ships in your fleet have 5% less combat readiness loss from being in a solar corona or a deep hyperspace storm.<br><i>Furnace drop only</i></td><td><code>combat_readiness</code> <code>logistics</code></td></tr>
<tr><td>5 to 10% increased damage to targets more than 1000 su away.<br><i>Frozen Fringe drop only</i></td><td><code>damage</code> <code>range</code></td></tr>
<tr><td>5 to 10% increased damage to ships with a lower top speed than this one.<br><i>Collapsed Reach drop only</i></td><td><code>damage</code> <code>conditional</code></td></tr>
<tr><td>Your fleet has 5% reduced terrain movement penalty, up to a maximum of 50%.<br><i>Storm Belt drop only</i></td><td><code>logistics</code> <code>mobility</code></td></tr>
</table>

</td><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>10 to 15% increased cargo capacity.</td><td><code>cargo</code></td></tr>
<tr><td>5 to 10% less supply use for maintenance.</td><td><code>supplies</code></td></tr>
<tr><td>5 to 10% increased damage to frigates.</td><td><code>damage</code></td></tr>
<tr><td>5 to 10% increased damage to destroyers.</td><td><code>damage</code></td></tr>
<tr><td>5 to 10% increased damage to cruisers.</td><td><code>damage</code></td></tr>
<tr><td>5 to 10% increased damage to capital ships and stations.</td><td><code>damage</code></td></tr>
<tr><td>10 to 15% increased repair rate per day.</td><td><code>repair</code></td></tr>
<tr><td>Your fleet has +1/1/2/2 to nav rating.</td><td><code>fleet_support</code> <code>mobility</code></td></tr>
<tr><td>+200 to +300 to in-combat sensor/vision range.</td><td><code>sensors</code></td></tr>
<tr><td>+20 to +35 to range from which combat objectives can be captured.</td><td><code>fleet_support</code></td></tr>
<tr><td>5 to 10% increased cargo capacity. 3 to 5% less supply use for maintenance.</td><td><code>cargo</code> <code>supplies</code></td></tr>
<tr><td>25 to 50% increased command point recovery rate while this ship is the flagship.</td><td><code>fleet_support</code> <code>conditional</code></td></tr>
<tr><td>Your fleet has 1% increased sensor strength for each ship in it with a Fragment Swarm.<br><i>Threat Expanse drop only</i></td><td><code>sensors</code></td></tr>
<tr><td>Your fleet has a 5% chance for each ship it recovers after a battle to gain no d-mods, up to a maximum of 30%.<br><i>Derelict Graveyard drop only</i></td><td><code>salvage</code> <code>d_mods</code></td></tr>
</table>

</td></tr>
</table>

### Unique items

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>20 to 30% less phase cloak activation cost.<br><i>Alpha Site Survivor</i></td><td><code>phase</code> <code>flux</code></td></tr>
<tr><td>15 to 25% increased top speed while phased.<br><i>Alpha Site Survivor</i></td><td><code>phase</code> <code>mobility</code> <code>while_phased</code></td></tr>
<tr><td>25 to 40% increased peak performance time.<br><i>Tri-Tachyon Morale Assurance Officer</i></td><td><code>combat_readiness</code></td></tr>
<tr><td>25 to 50% increased crew casualties.<br><i>Tri-Tachyon Morale Assurance Officer</i></td><td><code>crew</code></td></tr>
<tr><td>25 to 40% reduced crew casualties.<br><i>Back-Alley Cyber-Surgeon</i></td><td><code>crew</code></td></tr>
<tr><td>5 to 10% increased maximum combat readiness.<br><i>Back-Alley Cyber-Surgeon</i></td><td><code>combat_readiness</code></td></tr>
<tr><td>100% increased effect of the targeted hull item.<br><i>Extended Warranty Contract</i></td><td><code>none</code> (tagless)</td></tr>
<tr><td>10 to 15% more supply use for maintenance.<br><i>Extended Warranty Contract</i></td><td><code>supplies</code></td></tr>
<tr><td>1% increased weapon range for every 10 affinity.<br><i>Heritage Fire Control</i></td><td><code>range</code> <code>low_tech</code> <code>midline</code> <code>high_tech</code></td></tr>
<tr><td>+20 to your highest affinity for each s-mod on this ship.<br><i>Lion's Guard Blueprint Vault</i></td><td><code>low_tech</code> <code>midline</code> <code>high_tech</code></td></tr>
<tr><td>20% more time flow.<br><i>Tesseract Stillwell</i></td><td><code>mobility</code></td></tr>
<tr><td>Non-missile weapon range can't exceed 500 su.<br><i>Tesseract Stillwell</i></td><td><code>range</code></td></tr>
<tr><td>30 to 45% increased combat readiness recovery rate.<br><i>Remnant Battle Relay</i></td><td><code>combat_readiness</code></td></tr>
<tr><td>20 to 30% increased peak performance time.<br><i>Remnant Battle Relay</i></td><td><code>combat_readiness</code></td></tr>
</table>

## Engine Room

Base item: Drive Assembly. 5 unique items.

Implicit: 5 to 10% increased top speed. (<code>mobility</code>)

<table>
<tr><th>Prefixes (14)</th><th>Suffixes (16)</th></tr>
<tr valign="top"><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>5 to 15% increased top speed.</td><td><code>mobility</code></td></tr>
<tr><td>20 to 30% increased maneuverability.</td><td><code>mobility</code></td></tr>
<tr><td>+5 to +10 to zero-flux speed bonus.</td><td><code>mobility</code> <code>flux_level</code></td></tr>
<tr><td>10 to 20% increased engine durability.</td><td><code>mobility</code> <code>defence</code></td></tr>
<tr><td>+10 to +20 to acceleration.</td><td><code>mobility</code></td></tr>
<tr><td>+15 to +25 to deceleration.</td><td><code>mobility</code></td></tr>
<tr><td>+10/8/6/4 to top speed.</td><td><code>mobility</code></td></tr>
<tr><td>The zero-flux speed boost also works while flux is at or below 2 to 5%.</td><td><code>mobility</code> <code>flux_level</code></td></tr>
<tr><td>Top speed changes with flux, from (+10 to +20) su/s at no flux to (-10 to -20) su/s at full flux, with no change at half flux. Below half flux, also up to (10 to 20%) increased acceleration and turn acceleration.</td><td><code>mobility</code> <code>flux_level</code></td></tr>
<tr><td>3 to 8% increased top speed. 10 to 15% increased maneuverability.</td><td><code>mobility</code></td></tr>
<tr><td>+5 to +10 to acceleration. +5 to +10 to deceleration.</td><td><code>mobility</code></td></tr>
<tr><td>2% increased top speed for every allied ship within 1000 su, up to a maximum of 10%.<br><i>Remnant Expanse drop only</i></td><td><code>mobility</code> <code>nearby_ships</code></td></tr>
<tr><td>+30 to +40 to High Tech Affinity.<br><i>Frozen Fringe drop only</i></td><td><code>high_tech</code></td></tr>
<tr><td>3 to 5% increased time flow while at top speed.<br><i>Collapsed Reach drop only</i></td><td><code>mobility</code> <code>conditional</code></td></tr>
</table>

</td><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>10 to 15% less fuel consumption rate.</td><td><code>fuel</code></td></tr>
<tr><td>10 to 20% increased fuel capacity.</td><td><code>fuel</code></td></tr>
<tr><td>10 to 15% reduced sensor profile.</td><td><code>sensors</code></td></tr>
<tr><td>While within 700 su of a larger friendly ship, up to 5 to 10% increased top speed, fading out over a further 500 su. Doubled for a destroyer escorting a capital ship.</td><td><code>mobility</code> <code>nearby_ships</code></td></tr>
<tr><td>While within 700 su of a larger friendly ship, up to 10 to 15% increased maneuverability, fading out over a further 500 su. Doubled for a destroyer escorting a capital ship.</td><td><code>mobility</code> <code>nearby_ships</code></td></tr>
<tr><td>+1 to max burn level.</td><td><code>logistics</code> <code>mobility</code></td></tr>
<tr><td>5 to 8% less fuel consumption rate. 5 to 10% increased fuel capacity.</td><td><code>fuel</code></td></tr>
<tr><td>+5 to +15 to Low Tech Affinity.</td><td><code>low_tech</code></td></tr>
<tr><td>+5 to +15 to High Tech Affinity.</td><td><code>high_tech</code></td></tr>
<tr><td>+5 to +15 to Midline Affinity.</td><td><code>midline</code></td></tr>
<tr><td>Your fleet has +1 to the burn level at which it is considered to be moving slowly, up to a maximum of +6.<br><i>Abyssal Rift drop only</i></td><td><code>logistics</code> <code>mobility</code> <code>sensors</code></td></tr>
<tr><td>20 to 30% increased top speed while below 50% hull.<br><i>Pirate Badlands drop only</i></td><td><code>mobility</code> <code>low_hull</code></td></tr>
<tr><td>10 to 20% increased Low Tech Affinity.<br><i>Furnace drop only</i></td><td><code>low_tech</code></td></tr>
<tr><td>10 to 15% increased top speed while this ship's fragment swarm is at least 90% full.<br><i>Threat Expanse drop only</i></td><td><code>mobility</code> <code>conditional</code></td></tr>
<tr><td>3% less fuel and supply use for each d-mod, up to a maximum of 5 d-mods.<br><i>Derelict Graveyard drop only</i></td><td><code>fuel</code> <code>supplies</code> <code>d_mods</code></td></tr>
<tr><td>20% increased top speed and maneuverability for 3 seconds after an EMP arc strikes this ship.<br><i>Storm Belt drop only</i></td><td><code>mobility</code> <code>emp</code> <code>conditional</code></td></tr>
</table>

</td></tr>
</table>

### Unique items

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>20 to 30% increased fuel capacity.<br><i>Sindrian Fuel Company Engineers</i></td><td><code>fuel</code></td></tr>
<tr><td>11 to 16% less fuel consumption rate.<br><i>Sindrian Fuel Company Engineers</i></td><td><code>fuel</code></td></tr>
<tr><td>+1 to +2 to max burn level.<br><i>Sindrian Fuel Company Engineers</i></td><td><code>logistics</code> <code>mobility</code></td></tr>
<tr><td>+1 to max burn level.<br><i>Galatia Hyperspace Physics Group</i></td><td><code>logistics</code> <code>mobility</code></td></tr>
<tr><td>11 to 16% less fuel consumption rate.<br><i>Galatia Hyperspace Physics Group</i></td><td><code>fuel</code></td></tr>
<tr><td>20 to 30% increased this ship's sensor strength.<br><i>Galatia Hyperspace Physics Group</i></td><td><code>sensors</code></td></tr>
<tr><td>+80 to +100 to this ship's sensor strength.<br><i>Galatia Hyperspace Physics Group</i></td><td><code>sensors</code></td></tr>
<tr><td>+1 to +2 to max burn level.<br><i>Gate Hauler Drive Coil</i></td><td><code>logistics</code> <code>mobility</code></td></tr>
<tr><td>+15 to +25 to zero-flux speed bonus.<br><i>Gate Hauler Drive Coil</i></td><td><code>mobility</code> <code>flux_level</code></td></tr>
<tr><td>The zero-flux speed boost works at any flux level.<br><i>Gate Hauler Drive Coil</i></td><td><code>mobility</code> <code>flux</code></td></tr>
<tr><td>Once per deployment, when hull drops below 30%, this ship teleports 1200 su away from the nearest enemy and vents its flux.<br><i>Displacer Failsafe</i></td><td><code>low_hull</code> <code>mobility</code> <code>flux</code></td></tr>
<tr><td>10 to 15% increased top speed.<br><i>Shard Microburn Cluster</i></td><td><code>mobility</code></td></tr>
<tr><td>20 to 30% increased acceleration.<br><i>Shard Microburn Cluster</i></td><td><code>mobility</code></td></tr>
<tr><td>15 to 25% reduced sensor profile.<br><i>Shard Microburn Cluster</i></td><td><code>sensors</code></td></tr>
</table>

## Flight Deck

Base item: Launch Assembly. 5 unique items.

Implicit: Strikecraft launched from this ship have 5 to 10% increased weapon rate of fire. (<code>strikecraft</code> <code>rate_of_fire</code>)

<table>
<tr><th>Prefixes (19)</th><th>Suffixes (13)</th></tr>
<tr valign="top"><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>5 to 10% reduced strikecraft refit time.</td><td><code>strikecraft</code></td></tr>
<tr><td>8 to 12% increased rate at which the strikecraft replacement rate recovers.</td><td><code>strikecraft</code></td></tr>
<tr><td>4 to 6% reduced rate at which the strikecraft replacement rate decreases due to strikecraft losses.</td><td><code>strikecraft</code></td></tr>
<tr><td>-10 to -15% of base refit time to strikecraft relaunch time.</td><td><code>strikecraft</code></td></tr>
<tr><td>Strikecraft launched from this ship have 15 to 25% reduced pilot casualties.</td><td><code>strikecraft</code> <code>crew</code></td></tr>
<tr><td>3 to 5% reduced strikecraft refit time. 4 to 6% increased rate at which the strikecraft replacement rate recovers.</td><td><code>strikecraft</code></td></tr>
<tr><td>5 to 10% reduced minimum crew required per strikecraft bay, up to 80% in total.</td><td><code>crew</code> <code>strikecraft</code></td></tr>
<tr><td>+5 to +15 to Low Tech Affinity.</td><td><code>low_tech</code></td></tr>
<tr><td>+5 to +15 to High Tech Affinity.</td><td><code>high_tech</code></td></tr>
<tr><td>+5 to +15 to Midline Affinity.</td><td><code>midline</code></td></tr>
<tr><td>Strikecraft launched from this ship deal 5 to 10% increased damage to drone strikecraft.<br><i>Abyssal Rift drop only</i></td><td><code>strikecraft</code> <code>damage</code></td></tr>
<tr><td>Strikecraft launched from this ship deal 15 to 20% increased damage to ships below 50% hull.<br><i>Pirate Badlands drop only</i></td><td><code>strikecraft</code> <code>damage</code> <code>conditional</code></td></tr>
<tr><td>Strikecraft launched from this ship have 2% increased weapon damage for every allied strikecraft within 500 su, up to a maximum of 20%.<br><i>Remnant Expanse drop only</i></td><td><code>strikecraft</code> <code>damage</code> <code>nearby_ships</code></td></tr>
<tr><td>Strikecraft launched from this ship burst when destroyed, dealing 100 to 150 fragmentation damage to enemy ships and strikecraft within 150 su.<br><i>Furnace drop only</i></td><td><code>strikecraft</code> <code>fragmentation</code> <code>damage</code></td></tr>
<tr><td>20 to 30% increased rate at which the strikecraft replacement rate recovers while strikecraft regroup.<br><i>Frozen Fringe drop only</i></td><td><code>strikecraft</code> <code>conditional</code></td></tr>
<tr><td>Strikecraft launched from this ship deal 1% increased damage for each strikecraft this ship lost in the last 20 seconds, up to a maximum of 20 strikecraft.<br><i>Collapsed Reach drop only</i></td><td><code>strikecraft</code> <code>damage</code> <code>conditional</code></td></tr>
<tr><td>Each time a strikecraft is replaced, this ship's fragment swarm spends 5 fragments, if it has them, to restore 100% of the strikecraft replacement rate lost.<br><i>Threat Expanse drop only</i></td><td><code>strikecraft</code></td></tr>
<tr><td>+10% to strikecraft replacement rate whenever an enemy ship is destroyed within 1500 su.<br><i>Derelict Graveyard drop only</i></td><td><code>strikecraft</code> <code>on_kill</code> <code>nearby_ships</code></td></tr>
<tr><td>Strikecraft launched from this ship deal extra EMP damage equal to 10 to 15% of each hit's damage.<br><i>Storm Belt drop only</i></td><td><code>strikecraft</code> <code>emp</code> <code>damage</code></td></tr>
</table>

</td><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>Strikecraft launched from this ship have 5 to 8% increased weapon damage.</td><td><code>strikecraft</code> <code>damage</code></td></tr>
<tr><td>Strikecraft launched from this ship have 10 to 15% increased armor.</td><td><code>strikecraft</code> <code>armour</code></td></tr>
<tr><td>Strikecraft launched from this ship have 7 to 10% increased top speed.</td><td><code>strikecraft</code> <code>mobility</code></td></tr>
<tr><td>5 to 10% increased strikecraft engagement range.</td><td><code>strikecraft</code> <code>range</code></td></tr>
<tr><td>Strikecraft launched from this ship have 5 to 10% reduced damage taken by shields.</td><td><code>strikecraft</code> <code>shield</code> <code>damage_taken</code></td></tr>
<tr><td>Strikecraft launched from this ship have 10 to 15% increased damage to other strikecraft and missiles.</td><td><code>strikecraft</code> <code>point_defense</code> <code>damage</code></td></tr>
<tr><td>Strikecraft launched from this ship have +25 to +50 to weapon range.</td><td><code>strikecraft</code> <code>range</code></td></tr>
<tr><td>Fighters launched from this ship have 8 to 12% increased weapon damage.</td><td><code>strikecraft</code> <code>damage</code></td></tr>
<tr><td>Interceptors launched from this ship have 8 to 12% increased weapon damage.</td><td><code>strikecraft</code> <code>damage</code></td></tr>
<tr><td>Bombers launched from this ship have 8 to 12% increased weapon damage.</td><td><code>strikecraft</code> <code>damage</code></td></tr>
<tr><td>Strikecraft launched from this ship have 3 to 5% increased weapon damage. Strikecraft launched from this ship have 4 to 6% increased top speed.</td><td><code>strikecraft</code> <code>damage</code> <code>mobility</code></td></tr>
<tr><td>+5 to +10 to cargo capacity per strikecraft bay.</td><td><code>cargo</code> <code>strikecraft</code></td></tr>
<tr><td>Strikecraft launched from this ship take 1 to 2% less damage for every living strikecraft of their wing out in space, up to a maximum of 80% less.</td><td><code>strikecraft</code> <code>damage_taken</code></td></tr>
</table>

</td></tr>
</table>

### Unique items

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>20 to 30% increased rate at which the strikecraft replacement rate recovers.<br><i>Converted Hangar Deck Crew</i></td><td><code>strikecraft</code></td></tr>
<tr><td>15 to 20% reduced strikecraft refit time.<br><i>Converted Hangar Deck Crew</i></td><td><code>strikecraft</code></td></tr>
<tr><td>+1 to number of strikecraft bays.<br><i>Converted Hangar Deck Crew</i></td><td><code>strikecraft</code></td></tr>
<tr><td>Strikecraft launched from this ship have 1% increased weapon damage for every 10 affinity.<br><i>Mixed Doctrine Flight Instructors</i></td><td><code>strikecraft</code> <code>damage</code> <code>low_tech</code> <code>midline</code> <code>high_tech</code></td></tr>
<tr><td>Strikecraft launched from this ship take 1% less damage for every 15 affinity.<br><i>Deck Armorers' Guild</i></td><td><code>strikecraft</code> <code>damage_taken</code> <code>low_tech</code> <code>midline</code> <code>high_tech</code></td></tr>
<tr><td>Strikecraft launched from this ship gain all of its affinity bonuses except logistics ones.<br><i>Pather Wing Zealots</i></td><td><code>strikecraft</code> <code>low_tech</code> <code>midline</code> <code>high_tech</code></td></tr>
<tr><td>10 to 15% reduced rate at which the strikecraft replacement rate decreases due to strikecraft losses.<br><i>Drone Fabricator Bay</i></td><td><code>strikecraft</code></td></tr>
<tr><td>20 to 30% increased rate at which the strikecraft replacement rate recovers.<br><i>Drone Fabricator Bay</i></td><td><code>strikecraft</code></td></tr>
</table>

## Reactor

Base item: Reactor Regulator. 3 unique items.

Implicit: 10 to 15% increased venting speed. (<code>flux</code>)

<table>
<tr><th>Prefixes (17)</th><th>Suffixes (9)</th></tr>
<tr valign="top"><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>10 to 15% increased venting speed.</td><td><code>flux</code></td></tr>
<tr><td>10 to 15% reduced EMP damage taken.</td><td><code>emp</code> <code>damage_taken</code></td></tr>
<tr><td>5 to 10% increased flux capacity.</td><td><code>flux</code></td></tr>
<tr><td>5 to 10% increased flux dissipation.</td><td><code>flux</code></td></tr>
<tr><td>10 to 15% increased flux capacity granted by each flux capacitor.</td><td><code>flux</code></td></tr>
<tr><td>+1 to +2 to flux dissipation per flux vent.</td><td><code>flux</code></td></tr>
<tr><td>+80/170/340/510 to flux capacity.</td><td><code>flux</code></td></tr>
<tr><td>+10/15/20/25 to flux dissipation.</td><td><code>flux</code></td></tr>
<tr><td>3 to 5% increased flux capacity. 3 to 5% increased flux dissipation.</td><td><code>flux</code></td></tr>
<tr><td>20 to 30% reduced sensor profile.<br><i>Abyssal Rift drop only</i></td><td><code>sensors</code></td></tr>
<tr><td>15 to 20% increased flux dissipation. 5 to 10% reduced peak performance time.<br><i>Pirate Badlands drop only</i></td><td><code>flux</code> <code>combat_readiness</code></td></tr>
<tr><td>1% increased flux dissipation for every 5% hard flux level.<br><i>Furnace drop only</i></td><td><code>flux</code> <code>flux_level</code></td></tr>
<tr><td>10 to 15% increased flux dissipation while this ship has no hard flux.<br><i>Frozen Fringe drop only</i></td><td><code>flux</code> <code>flux_level</code></td></tr>
<tr><td>10 to 20% reduced overload duration.<br><i>Collapsed Reach drop only</i></td><td><code>flux</code></td></tr>
<tr><td>1% increased fragment replacement rate for every 5 affinity.<br><i>Threat Expanse drop only</i></td><td><code>low_tech</code> <code>midline</code> <code>high_tech</code></td></tr>
<tr><td>2 to 3% more flux dissipation for each d-mod, up to a maximum of 5 d-mods.<br><i>Derelict Graveyard drop only</i></td><td><code>flux</code> <code>d_mods</code></td></tr>
<tr><td>Taking EMP damage dissipates hard flux equal to 20 to 30% of that EMP damage.<br><i>Storm Belt drop only</i></td><td><code>flux</code> <code>emp</code></td></tr>
</table>

</td><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>5 to 10% less non-beam energy weapon flux cost.</td><td><code>energy</code> <code>flux</code></td></tr>
<tr><td>5 to 10% less ballistic weapon flux cost.</td><td><code>ballistic</code> <code>flux</code></td></tr>
<tr><td>5 to 10% reduced beam weapon flux cost.</td><td><code>beam</code> <code>flux</code></td></tr>
<tr><td>10 to 15% increased energy weapon ammo regeneration rate.</td><td><code>energy</code> <code>ammo</code></td></tr>
<tr><td>3 to 5% reduced weapon flux cost.</td><td><code>weapon</code> <code>flux</code></td></tr>
<tr><td>3 to 5% less non-beam energy weapon flux cost. 3 to 5% less ballistic weapon flux cost.</td><td><code>energy</code> <code>flux</code> <code>ballistic</code></td></tr>
<tr><td>Weapon rate of fire changes with flux, from (5 to 15%) increased at no flux to (5 to 15%) reduced at full flux, with no change at half flux. Venting or being overloaded counts as full flux.</td><td><code>rate_of_fire</code> <code>flux_level</code></td></tr>
<tr><td>Once shields have been down for 0.75/1.1/2/3 seconds while not venting or overloaded, (3 to 5%) increased energy weapon rate of fire and (3 to 5%) reduced energy weapon flux cost.</td><td><code>energy</code> <code>rate_of_fire</code> <code>flux</code> <code>shield</code></td></tr>
<tr><td>10 to 15% hard flux dissipation while shields are active.<br><i>Remnant Expanse drop only</i></td><td><code>flux</code> <code>shield</code></td></tr>
</table>

</td></tr>
</table>

### Unique items

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>+1 to ship system charges.<br><i>Synchrotron Cell</i></td><td><code>ship_system</code></td></tr>
<tr><td>20 to 30% increased fuel capacity.<br><i>Synchrotron Cell</i></td><td><code>fuel</code></td></tr>
<tr><td>+1 to +2 to max burn level.<br><i>Synchrotron Cell</i></td><td><code>logistics</code> <code>mobility</code></td></tr>
<tr><td>Your lowest affinity counts as equal to your highest.<br><i>Absence of Light</i></td><td><code>low_tech</code> <code>midline</code> <code>high_tech</code></td></tr>
<tr><td>Removes all High Tech and Midline Affinity and grants 50% of it as Low Tech Affinity.<br><i>Equivalent Exchange Core</i></td><td><code>low_tech</code></td></tr>
<tr><td>Removes all Low Tech and Midline Affinity and grants 50% of it as High Tech Affinity.<br><i>Equivalent Exchange Core</i></td><td><code>high_tech</code></td></tr>
<tr><td>Removes all Low Tech and High Tech Affinity and grants 50% of it as Midline Affinity.<br><i>Equivalent Exchange Core</i></td><td><code>midline</code></td></tr>
</table>

## Armor Plating

Base item: Armor Plate. 4 unique items.

Implicit: 5 to 10% increased armor. (<code>armour</code>)

<table>
<tr><th>Prefixes (16)</th><th>Suffixes (9)</th></tr>
<tr valign="top"><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>+20/30/40/50 to armor.</td><td><code>armour</code></td></tr>
<tr><td>+80/200/400/600 to hull points.</td><td><code>hull</code></td></tr>
<tr><td>4 to 6% increased hull points.</td><td><code>hull</code></td></tr>
<tr><td>3 to 5% increased armor.</td><td><code>armour</code></td></tr>
<tr><td>10 to 20% increased weapon durability.</td><td><code>weapon</code></td></tr>
<tr><td>1 to 2% less armor damage taken for each d-mod, up to a maximum of 5 d-mods.</td><td><code>armour</code> <code>damage_taken</code> <code>d_mods</code></td></tr>
<tr><td>+10/15/20/25 to armor. +40/100/200/300 to hull points.</td><td><code>armour</code> <code>hull</code></td></tr>
<tr><td>Every 150 armor and hull damage this ship takes grants a stack of Borrowed Grace lasting 15 seconds, up to 10 stacks. Each stack grants 1% increased top speed, flux dissipation and weapon rate of fire, and 1% reduced weapon flux cost and damage taken. Stack timers pause while the ship is phased.<br><i>Abyssal Rift drop only</i></td><td><code>mobility</code> <code>flux</code> <code>rate_of_fire</code> <code>damage_taken</code></td></tr>
<tr><td>50% more collision damage dealt to enemy ships and 50% less collision damage taken.<br><i>Pirate Badlands drop only</i></td><td><code>damage</code> <code>damage_taken</code></td></tr>
<tr><td>Armor cells not hit for 5 seconds repair 0.5% of their maximum armor per second while no enemy ship is within 1500 su, adding 1 hard flux per point repaired, up to 50% of this ship's total armor per deployment.<br><i>Remnant Expanse drop only</i></td><td><code>armour</code> <code>repair</code></td></tr>
<tr><td>Repairs 0.5% of maximum hull per second while above 50% flux level, up to 20% of maximum hull per deployment.<br><i>Furnace drop only</i></td><td><code>hull</code> <code>repair</code> <code>flux_level</code></td></tr>
<tr><td>1% increased armor for every 10 High Tech Affinity.<br><i>Frozen Fringe drop only</i></td><td><code>armour</code> <code>high_tech</code></td></tr>
<tr><td>5 to 10% chance for missiles about to hit armor or hull to be ejected into p-space, dealing no damage.<br><i>Collapsed Reach drop only</i></td><td><code>damage_taken</code></td></tr>
<tr><td>Each fragment this ship's fragment swarm would regrow while full instead repairs 0.5% of this ship's total armor on its most damaged armor cell, up to 10% of its total armor per deployment.<br><i>Threat Expanse drop only</i></td><td><code>armour</code> <code>repair</code></td></tr>
<tr><td>Whenever an enemy ship is destroyed within 1000 su, every armor cell repairs 5% of its maximum armor, up to 10 to 15% of this ship's total armor per deployment.<br><i>Derelict Graveyard drop only</i></td><td><code>armour</code> <code>repair</code> <code>on_kill</code> <code>nearby_ships</code></td></tr>
<tr><td>Every 500 damage this ship's armor absorbs discharges 1 EMP arc at the nearest enemy ship within 600 su, dealing 100/150/200/300 energy and EMP damage.<br><i>Storm Belt drop only</i></td><td><code>armour</code> <code>emp</code> <code>damage</code></td></tr>
</table>

</td><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>4 to 6% reduced kinetic damage taken.</td><td><code>kinetic</code> <code>damage_taken</code></td></tr>
<tr><td>4 to 6% reduced high-explosive damage taken.</td><td><code>high_explosive</code> <code>damage_taken</code></td></tr>
<tr><td>4 to 6% reduced fragmentation damage taken.</td><td><code>fragmentation</code> <code>damage_taken</code></td></tr>
<tr><td>4 to 6% reduced energy damage taken.</td><td><code>energy_damage</code> <code>damage_taken</code></td></tr>
<tr><td>10 to 15% reduced sensor profile.</td><td><code>sensors</code></td></tr>
<tr><td>15 to 25% less combat readiness loss from being in a solar corona or a deep hyperspace storm.</td><td><code>combat_readiness</code> <code>logistics</code></td></tr>
<tr><td>5 to 10% reduced kinetic damage taken. 5 to 10% reduced high-explosive damage taken.</td><td><code>kinetic</code> <code>damage_taken</code> <code>high_explosive</code></td></tr>
<tr><td>5 to 10% reduced energy damage taken. 5 to 10% reduced fragmentation damage taken.</td><td><code>energy_damage</code> <code>damage_taken</code> <code>fragmentation</code></td></tr>
<tr><td>After 5 seconds without taking hull damage, repairs 0.25 to 0.5% of maximum hull per second in combat, up to 100% of maximum hull in total per deployment.</td><td><code>hull</code> <code>repair</code></td></tr>
</table>

</td></tr>
</table>

### Unique items

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>30 to 50% increased hull points.<br><i>Fullerene Spool Integration</i></td><td><code>hull</code></td></tr>
<tr><td>20 to 30% increased cargo capacity.<br><i>Fullerene Spool Integration</i></td><td><code>cargo</code></td></tr>
<tr><td>20 to 30% increased armor.<br><i>Vambrace Plating</i></td><td><code>armour</code></td></tr>
<tr><td>5 to 10% reduced top speed.<br><i>Vambrace Plating</i></td><td><code>mobility</code></td></tr>
<tr><td>1% increased armor for every 10 Low Tech Affinity.<br><i>Bulwark Riveters</i></td><td><code>armour</code> <code>low_tech</code></td></tr>
<tr><td>30 to 45% increased repair rate per day.<br><i>Derelict Nanite Lattice</i></td><td><code>repair</code></td></tr>
<tr><td>15 to 25% less negative effects from d-mods.<br><i>Derelict Nanite Lattice</i></td><td><code>d_mods</code></td></tr>
</table>

## Shield Generator

Base item: Shield Emitter. 4 unique items.

Implicit: 5 to 10% increased flux capacity. 5 to 10% increased flux dissipation. (<code>flux</code>)

<table>
<tr><th>Prefixes (17)</th><th>Suffixes (9)</th></tr>
<tr valign="top"><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>5 to 10% less damage taken by shields.</td><td><code>shield</code> <code>damage_taken</code></td></tr>
<tr><td>5 to 10% less shield flux upkeep.</td><td><code>shield</code> <code>flux</code></td></tr>
<tr><td>+15 to +30 to shield arc.</td><td><code>shield</code></td></tr>
<tr><td>5 to 10% increased shield arc.</td><td><code>shield</code></td></tr>
<tr><td>Flux taken by the shield charges a reserve of up to 1000, which drains by 100 per second. While charged, up to 10 to 15% increased ballistic and energy weapon rate of fire, at full strength from 500 reserve. Non-beam weapons also cost less flux per shot, so their faster fire costs no extra flux.</td><td><code>shield</code> <code>ballistic</code> <code>energy</code> <code>rate_of_fire</code></td></tr>
<tr><td>Disperses 3 to 5% of damage taken by shields to allied ships within 1000 su that aren't overloaded or venting, split evenly between them as hard flux.</td><td><code>shield</code> <code>damage_taken</code> <code>nearby_ships</code></td></tr>
<tr><td>3 to 5% less damage taken by shields. 3 to 5% less shield flux upkeep.</td><td><code>shield</code> <code>damage_taken</code> <code>flux</code></td></tr>
<tr><td>Installs a makeshift, front-facing shield if this ship has none.</td><td><code>front_shield</code> <code>shieldless</code></td></tr>
<tr><td>15 to 20% less shield flux upkeep.<br><i>Abyssal Rift drop only</i></td><td><code>shield</code> <code>flux</code></td></tr>
<tr><td>5 to 10% reduced damage taken by shields while moving faster than 75% of top speed.<br><i>Pirate Badlands drop only</i></td><td><code>shield</code> <code>damage_taken</code> <code>conditional</code></td></tr>
<tr><td>Up to 5 to 10% reduced damage taken by shields, building up over 3 seconds while no weapons fire and fading over 3 seconds while any do.<br><i>Remnant Expanse drop only</i></td><td><code>shield</code> <code>damage_taken</code> <code>conditional</code></td></tr>
<tr><td>1% reduced hull damage taken for every 10 Low Tech Affinity.<br><i>Furnace drop only</i></td><td><code>hull</code> <code>damage_taken</code> <code>low_tech</code></td></tr>
<tr><td>30 to 40% reduced shield flux upkeep while moving slower than 10% of top speed.<br><i>Frozen Fringe drop only</i></td><td><code>shield</code> <code>flux</code> <code>conditional</code></td></tr>
<tr><td>5% chance for non-missile projectiles hitting shields to be deflected for no flux.<br><i>Collapsed Reach drop only</i></td><td><code>shield</code> <code>damage_taken</code></td></tr>
<tr><td>10 to 15% reduced damage taken by shields while this ship's fragment swarm has fragments, spending 1 fragment for every 30 damage absorbed.<br><i>Threat Expanse drop only</i></td><td><code>shield</code> <code>damage_taken</code></td></tr>
<tr><td>10 to 15% less damage taken by shields from automated ships.<br><i>Derelict Graveyard drop only</i></td><td><code>shield</code> <code>damage_taken</code></td></tr>
<tr><td>1% reduced chance for shields to be pierced by EMP arcs for every 10 Midline Affinity.<br><i>Storm Belt drop only</i></td><td><code>shield</code> <code>emp</code> <code>midline</code></td></tr>
</table>

</td><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>10 to 15% increased shield raise rate.</td><td><code>shield</code></td></tr>
<tr><td>10 to 15% increased shield turn rate.</td><td><code>omni_shield</code></td></tr>
<tr><td>15 to 25% reduced chance for shields to be pierced by EMP arcs.</td><td><code>shield</code> <code>emp</code></td></tr>
<tr><td>5 to 10% reduced energy damage taken.</td><td><code>energy_damage</code> <code>damage_taken</code></td></tr>
<tr><td>5 to 8% increased shield raise rate. 5 to 8% increased shield turn rate.</td><td><code>omni_shield</code></td></tr>
<tr><td>5 to 10% of damage taken by shields is converted to soft flux.</td><td><code>shield</code> <code>flux</code></td></tr>
<tr><td>+5 to +15 to Low Tech Affinity.</td><td><code>low_tech</code></td></tr>
<tr><td>+5 to +15 to High Tech Affinity.</td><td><code>high_tech</code></td></tr>
<tr><td>+5 to +15 to Midline Affinity.</td><td><code>midline</code></td></tr>
</table>

</td></tr>
</table>

### Unique items

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>50% of damage taken by shields is converted to soft flux.<br><i>Low Arc Shield Emitter Core</i></td><td><code>shield</code> <code>flux</code></td></tr>
<tr><td>Shield arc is fixed at 60 degrees and can't be modified.<br><i>Low Arc Shield Emitter Core</i></td><td><code>shield</code></td></tr>
<tr><td>25 to 35% of damage taken by shields is converted to soft flux.<br><i>Hardlight Veil Projectors</i></td><td><code>shield</code> <code>flux</code></td></tr>
<tr><td>25 to 35% of damage taken by shields is passed to the nearest allied ship within 1000 su as soft flux.<br><i>Cosmic Shunt Array</i></td><td><code>shield</code> <code>damage_taken</code> <code>nearby_ships</code></td></tr>
<tr><td>15 to 20% of damage taken by shields is converted to soft flux.<br><i>Radiant Shield Lattice</i></td><td><code>shield</code> <code>flux</code></td></tr>
<tr><td>10 to 15% reduced energy damage taken.<br><i>Radiant Shield Lattice</i></td><td><code>energy_damage</code> <code>damage_taken</code></td></tr>
</table>

## Phase Coil

Base item: Phase Coil Array. 0 unique items.

Implicit: 10 to 20% increased flux dissipation while phased. (<code>flux</code> <code>phase</code> <code>while_phased</code>)

<table>
<tr><th>Prefixes (8)</th><th>Suffixes (17)</th></tr>
<tr valign="top"><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>10 to 15% less phase cloak activation cost.</td><td><code>phase</code> <code>flux</code></td></tr>
<tr><td>5 to 10% less phase cloak flux upkeep.</td><td><code>phase</code> <code>flux</code></td></tr>
<tr><td>10 to 15% increased top speed while phased.</td><td><code>phase</code> <code>mobility</code> <code>while_phased</code></td></tr>
<tr><td>5 to 10% increased hard flux level at which phased speed reaches its minimum.</td><td><code>phase</code> <code>mobility</code> <code>flux_level</code></td></tr>
<tr><td>10 to 15% reduced phase cloak cooldown.</td><td><code>phase</code></td></tr>
<tr><td>15 to 25% increased acceleration while phased.</td><td><code>phase</code> <code>mobility</code> <code>while_phased</code></td></tr>
<tr><td>5 to 10% increased phase cloak time flow bonus.</td><td><code>phase</code> <code>mobility</code> <code>while_phased</code></td></tr>
<tr><td>3 to 5% less phase cloak activation cost. 3 to 5% less phase cloak flux upkeep.</td><td><code>phase</code> <code>flux</code></td></tr>
</table>

</td><td>

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>10 to 15% reduced sensor profile.</td><td><code>sensors</code></td></tr>
<tr><td>10 to 15% increased venting speed.</td><td><code>flux</code></td></tr>
<tr><td>5 to 10% increased peak performance time.</td><td><code>combat_readiness</code></td></tr>
<tr><td>15 to 25% increased ammo regeneration while phased.</td><td><code>ammo</code> <code>phase</code> <code>while_phased</code></td></tr>
<tr><td>10 to 15% increased flux dissipation while phased.</td><td><code>flux</code> <code>phase</code> <code>while_phased</code></td></tr>
<tr><td>5 to 10% reduced ship system cooldown.</td><td><code>ship_system</code></td></tr>
<tr><td>5 to 8% increased flux dissipation while phased. 5 to 8% increased ammo regeneration while phased.</td><td><code>flux</code> <code>phase</code> <code>while_phased</code> <code>ammo</code></td></tr>
<tr><td>1 to 2% increased time flow.</td><td><code>mobility</code></td></tr>
<tr><td>When this ship enters phase, every armor cell repairs 10% of its maximum armor, up to 50% of this ship's total armor per deployment.<br><i>Abyssal Rift drop only</i></td><td><code>armour</code> <code>repair</code> <code>phase</code></td></tr>
<tr><td>Your fleet has 10% reduced sensor profile while its transponder is off, up to a maximum of 30%.<br><i>Pirate Badlands drop only</i></td><td><code>sensors</code></td></tr>
<tr><td>Your fleet has +1 to nav rating for every 50 High Tech Affinity.<br><i>Remnant Expanse drop only</i></td><td><code>fleet_support</code> <code>mobility</code> <code>high_tech</code></td></tr>
<tr><td>When this ship enters phase, 10 to 20% of its hard flux is converted to soft flux.<br><i>Furnace drop only</i></td><td><code>flux</code> <code>phase</code></td></tr>
<tr><td>The peak performance timer pauses while phased.<br><i>Frozen Fringe drop only</i></td><td><code>combat_readiness</code> <code>phase</code> <code>while_phased</code></td></tr>
<tr><td>Enemy ships within 1500 su have up to 10 to 15% less time flow while this ship is phased, strongest at close range. Gravitational drag from every source is capped at 25% per ship.<br><i>Collapsed Reach drop only</i></td><td><code>phase</code> <code>while_phased</code> <code>nearby_ships</code></td></tr>
<tr><td>Fragments from this ship's fragment swarm, or else the nearest allied one within 1000 su, cover 50% of phase cloak flux upkeep while phased, spending 1 fragment for every 50 flux covered.<br><i>Threat Expanse drop only</i></td><td><code>phase</code> <code>flux</code> <code>while_phased</code></td></tr>
<tr><td>No phase cloak flux upkeep while within 500 su of a wreck.<br><i>Derelict Graveyard drop only</i></td><td><code>phase</code> <code>flux</code> <code>conditional</code></td></tr>
<tr><td>When this ship leaves phase, it discharges 3 EMP arcs at the nearest enemy ships within 600 su, each dealing 100/150/200/300 energy and EMP damage.<br><i>Storm Belt drop only</i></td><td><code>phase</code> <code>emp</code> <code>damage</code></td></tr>
</table>

</td></tr>
</table>

## Not tied to one hull item class

Corruption implicits, and the Storm Front and Anomalous Debris modifiers, which those anomalies grant onto a hull item of any class.

### Implicit

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>7 to 13% less supply use for maintenance.</td><td><code>supplies</code></td></tr>
<tr><td>7% increased cargo capacity.</td><td><code>cargo</code></td></tr>
<tr><td>7% increased fuel capacity.</td><td><code>fuel</code></td></tr>
<tr><td>7% increased crew capacity.</td><td><code>crew</code></td></tr>
<tr><td>5% increased weapon damage.</td><td><code>damage</code></td></tr>
<tr><td>7% increased flux capacity.</td><td><code>flux</code></td></tr>
<tr><td>7% increased flux dissipation.</td><td><code>flux</code></td></tr>
</table>

### Prefix

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>25% reduced EMP damage taken.<br><i>Storm Front drop</i></td><td><code>emp</code> <code>damage_taken</code></td></tr>
<tr><td>7 to 13% increased weapon rate of fire. Burst beams instead recharge faster between bursts. 7 to 13% reduced weapon flux cost.<br><i>Anomalous Debris drop</i></td><td><code>rate_of_fire</code> <code>flux</code></td></tr>
<tr><td>Enemy ships hit by this ship's missiles have 10 to 20% less top speed for 2 seconds.<br><i>Anomalous Debris drop</i></td><td><code>missile</code> <code>on_hit</code></td></tr>
</table>

### Suffix

<table>
<tr><th>Effect</th><th>Tags</th></tr>
<tr><td>Beam weapons disrupt the target's shields, increasing all shield damage taken by 7 to 13%.<br><i>Anomalous Debris drop</i></td><td><code>beam</code> <code>damage</code> <code>on_hit</code></td></tr>
<tr><td>7 to 13% of damage taken by shields is converted to soft flux.<br><i>Anomalous Debris drop</i></td><td><code>shield</code> <code>flux</code></td></tr>
<tr><td>Destroying an enemy ship removes flux equal to 7 to 13% of this ship's maximum flux.<br><i>Anomalous Debris drop</i></td><td><code>flux</code> <code>on_kill</code></td></tr>
<tr><td>+1 to ship system charges. 7 to 13% increased ship system charge regeneration.<br><i>Anomalous Debris drop</i></td><td><code>ship_system</code></td></tr>
<tr><td>Ballistic weapon hits on armor strip an additional 7 to 13% of the hit's damage from the armor around the impact over 5 seconds.<br><i>Anomalous Debris drop</i></td><td><code>ballistic</code> <code>damage</code> <code>on_hit</code></td></tr>
<tr><td>Non-beam energy weapons have a 8% chance to refract when hitting enemy shields.<br><i>Storm Front drop</i></td><td><code>energy</code> <code>on_hit</code></td></tr>
</table>
