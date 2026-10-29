# Automated-hull node flavour

Status: built. All 59 drafts below are in `skill_types.json` as `flavourAutomated`, with zh_CN versions in `data/strings/exiledSector/skillTree/zh_CN.json` (awaiting native review like the rest of the zh catalogue).

Every node's flavour reads as the crew modifying the ship. On an automated hull nobody is aboard, so nodes that mention people get a second flavour text, shown when the tree's ship is automated (`Misc.isAutomated`).

## Rules

- No living person teaches, trains, shows or explains anything to the ship's core. The core takes in behaviour by cold observation: recordings, sensor logs, flight recorders, wreck data, comm traffic, fights it watched.
- The core has no feelings. It logs, records, models, parses, matches and reproduces. Humour comes from the situation and the people around it, never from the core.
- Physical work (stripping wrecks, rewiring, fitting parts) is done by "the refit crew", people who work on the ship in dock. Factions may teach the refit crew, as in the crewed text (Exo-Tech, Kesteven R&D, the Tri-Tachyon defector).
- As with the crewed text: no numbers, and nothing that implies the node is already allocated.
- Tania nodes keep the author's text on both kinds of hull unless the author writes automated versions. Crustacean Integration and the 26 nodes that mention no people need no automated text.

## How it works

An optional `flavourAutomated` field per skill type, with the translation key `skillType.<id>.flavourAutomated` (en in `skill_types.json`, zh_CN in `skillTree/zh_CN.json`). The tooltip uses it when the tree's ship is automated and falls back to `flavour` when it is empty or there is no ship. The field goes into the tools/editor serialisers. Node names don't switch.

Built: `SkillType.getFlavourText(boolean automated)` and `getAutomatedFlavourText()`, loaded by `SkillTypeLoader`; the tooltip picks the text in `SkillTreeNodeTooltipRenderer.flavourText` (tested in `SkillTreeNodeTooltipRendererTest`). The editor writes the field after `flavour` (`serialization.ts`) and edits it in the type form's texts section. `CatalogueLintTest` and `DescriptionGoldenTest` cover the new key.

## Drafts

Each entry has the current text quoted, then the automated draft, then how the knowledge reaches the ship.

### Heavy Ballistics Integration (`heavy_ballistics_integration`)
> Stripping decommissioned Onslaughts at the Agreus shipbreaking yards shows the crew how the Navy seats its heaviest guns directly into the frame. The Ko Combine bills for the scrap, not the lesson.

Stripping decommissioned Onslaughts at the Agreus shipbreaking yards shows the refit crew how the Navy seats its heaviest guns directly into the frame. The Ko Combine bills for the scrap, not the lesson.

Route: refit crew, shipbreaking.

### Ground Support (`ground_support`)
> Raids flown alongside pirate crews teach the crew to turn spare berths and cargo lifts into a drop platform for the marines, and to keep a few guns pointed at the ground while they work.

The ship's core logs every pirate raid it flies beside, down to where the guns point while the marines are on the ground. The refit crew turns its cargo lifts into a drop platform, and the core keeps the guns where the logs say.

Route: observed raids, refit crew.

### TriPad Backdoor (`tactical_superiority`)
> Every TriPad in the Sector ships with backdoors Tri-Tachyon officially denies, and a crew that learns them can talk a nav buoy or comm relay into changing sides mid-battle. Company support declines to comment, but will gladly sell an upgrade.

Every TriPad in the Sector ships with backdoors Tri-Tachyon officially denies. The ship's core maps them from intercepted TriPad traffic and talks a nav buoy or comm relay into changing sides mid-battle. Company support declines to comment, but will gladly sell an upgrade.

Route: signals intercepts.

### Nanoforge Mending (`nanoforge_mending`)
> Stripping the repair nanoforge out of a gutted Nexus shows the crew how the Remnant keep their hulls whole long after a fight should have broken them.

Stripping the repair nanoforge out of a gutted Nexus shows the refit crew how the Remnant keep their hulls whole long after a fight should have broken them. Wired into an automated hull, it needs no hand to tell it where to work.

Route: refit crew, Remnant wreck.

### Disintegration (`disintegration`)
> Hulls killed by Remnant energy weapons come apart around every impact in a way no human weapon manages. Salvors who study enough of the wrecks learn to tune this ship's energy weapons to the same pattern, and try not to think about what it resembles.

Hulls killed by Remnant energy weapons come apart around every impact in a way no human weapon manages. The ship's core scans enough of the wrecks to model the pattern and tunes its energy weapons to match. It has no opinion on what the pattern resembles.

Route: wreck scans.

### Machine Precision (`machine_precision`)
> A recovered gamma core, kept powered just long enough to be questioned, gives up the targeting routines Remnant drones use to lead a target. Afterwards it is disposed of, in accordance with Hegemony regulations and the crew's nerves.

A recovered gamma core holds the targeting routines Remnant drones use to lead a target. The ship's core copies them across in a fraction of a second. The refit crew disposes of the gamma core afterwards, in accordance with Hegemony regulations and their own nerves.

Route: core-to-core copy.

### XIV Torpedo Doctrine (`torpedo_specialisation`)
> XIV Battlegroup strike manuals, kept by the Hegemony since the Domain, teach that a torpedo should close fast and fly straight. Following them, the crew rewires each missile's maneuvering thrusters to push it forward instead, on the principle that a missile which cannot turn must not miss.

XIV Battlegroup strike manuals, kept by the Hegemony since the Domain, hold that a torpedo should close fast and fly straight. The refit crew rewires each missile's maneuvering thrusters to push it forward instead, on the principle that a missile which cannot turn must not miss.

Route: refit crew, doctrine manuals.

### Lion's Gaze (`beam_split`)
> Diktat propaganda insists that the Lion's gaze falls on every citizen at once, and the engineer-technicians of the Lion's Guard took the slogan literally. Studying the Executors they refit teaches the crew to split this ship's beams across the target and the enemy ships around it.

Diktat propaganda insists that the Lion's gaze falls on every citizen at once, and the engineer-technicians of the Lion's Guard took the slogan literally. The ship's core records how a Lion's Guard Executor divides its beams across a target and the ships around it, and splits its own the same way. It does not record the slogan.

Route: combat recordings.

### Refracting Projectiles (`energy_chain`)
> Pulling apart the energy converters of wrecked Tri-Tachyon warships shows the crew how to tune this ship's energy bolts to glance off an enemy shield and carry on to another target. Tri-Tachyon sales literature calls this efficiency.

Pulling apart the energy converters of wrecked Tri-Tachyon warships shows the refit crew how to tune this ship's energy bolts to glance off an enemy shield and carry on to another target. Tri-Tachyon sales literature calls this efficiency.

Route: refit crew, wrecks.

### Hullbreacher Rounds (`hull_breacher`)
> Former Hegemony Navy gunners in the crew remember the breacher loads the Navy keeps for boarding actions, shells hardened to punch through plating instead of bursting on it. The crew sees no reason to save them for boarding.

The Hegemony Navy keeps breacher loads for boarding actions, shells hardened to punch through plating instead of bursting on it. The ship's core finds their specifications in a seized Navy ordnance manifest and loads them for every fight. It has no boarding actions to save them for.

Route: captured documents.

### Navy Heartbeat (`ballistic_momentum`)
> Sailing alongside Hegemony battle lines teaches the crew the Navy's habit of arriving at speed and firing before the enemy has settled, guns thumping out what the Navy calls its heartbeat.

The ship's core logs hours of Hegemony line engagements and reproduces the Navy's habit of arriving at speed and firing before the enemy has settled, guns thumping out what the Navy calls its heartbeat. The core keeps time more evenly than any Navy gunner.

Route: combat recordings.

### League Picket Targeting (`attuned_sensors`)
> Studying the sensor pickets the League posts along its Hegemony border shows the crew how to slave energy weapon targeting to the ship's own sensors. The pickets are the one League institution every member world agrees to fund.

The ship's core logs the sweeps of the sensor pickets the League posts along its Hegemony border and slaves its energy weapon targeting to its own sensors on the same pattern. The pickets are the one League institution every member world agrees to fund.

Route: sensor logs.

### Adaptive Phase Coils (`adaptive_coils`)
> Phase coils lose their grip on normal space as hard flux builds. A crew willing to rewind them by hand can keep that grip a while longer.

Phase coils lose their grip on normal space as hard flux builds. The ship's core models the slip from its own coil telemetry and corrects for it faster than any hand could rewind them, keeping that grip a while longer.

Route: own telemetry.

### Defensive Targeting Array (`defensive_targeting_array`)
> Wing datalinks rewired to carry the ship's own point-defense solutions. The pilots complain about the short leash. The gunners do not.

Wing datalinks rewired to carry the ship's own point-defense solutions. If the pilots complain about the short leash, nobody aboard hears it.

Route: refit.

### Hybrid P-space Shrouding (`phase_hull_upgrades`)
> Leaked papers from Tri-Tachyon's Hybrid Physics Division (on phase work the corporation swears it abandoned under the Treaty of Thule) show the crew how to bleed this ship's sensor emissions into the masking field phase ships throw over a fleet. Tri-Tachyon maintains the papers are forgeries.

Leaked papers from Tri-Tachyon's Hybrid Physics Division (on phase work the corporation swears it abandoned under the Treaty of Thule) describe how to bleed a ship's sensor emissions into the masking field phase ships throw over a fleet. The ship's core parses them in full, footnotes included. Tri-Tachyon maintains the papers are forgeries.

Route: captured documents.

### League Reload Drill (`hoarded_arms`)
> League fleets drawn from a dozen squabbling polities still share one reload drill, so that any ship can rearm any other. It is the one thing the member worlds have never argued over, and serving under a League commission drills it into the crew.

League fleets drawn from a dozen squabbling polities still share one reload drill, so that any ship can rearm any other. The ship's core records it from the League ships it serves beside and runs it the same way every time, which no League crew has ever managed.

Route: observed under commission.

### Neural Interface (`neural_interface`)
> A direct neural link between this ship and the flagship. Captains who use one describe it as standing on two bridges at once, and are sometimes vague afterwards about which one they were on.

A direct neural link between this ship and the flagship. Captains who link to an automated hull describe a second bridge with nobody else on it, and are not always certain of that afterwards.

Route: none needed.

### Critical Point Refit (`nskr_criticalArmor`)
> A prototype modspec from Kesteven R&D shows the crew how to reinforce a light hull's critical structural points, where thin plating fails first.

A prototype modspec from Kesteven R&D shows the refit crew how to reinforce a light hull's critical structural points, where thin plating fails first.

Route: refit crew, Kesteven modspec.

### Inertial Supercharging (`nskr_inertial`)
> A Kesteven R&D modspec lets the crew feed the ship's own momentum into its guns. Kesteven's notes recommend never slowing down.

A Kesteven R&D modspec lets the refit crew feed the ship's own momentum into its guns. Kesteven's notes recommend never slowing down, and the ship's core treats the recommendation as a parameter.

Route: refit crew, Kesteven modspec.

### ABSORB Reserve (`nskr_absorption`)
> Disassembling the ABSORB loop from a Kingstork taught the crew how to bank the energy their shields block instead of venting it, and how to spend it on the guns.

Disassembling the ABSORB loop from a Kingstork showed the refit crew how to bank the energy the shields block instead of venting it. The ship's core decides when to spend it on the guns.

Route: refit crew, Kingstork.

### Volatile Flux Tuning (`nskr_volatile`)
> A prototype flux injector from Kesteven R&D lets the crew trade staying power for tempo. The modspec describes the side effects as manageable.

A prototype flux injector from Kesteven R&D lets the refit crew trade staying power for tempo. The modspec describes the side effects as manageable. With nobody aboard, nobody has to manage them.

Route: refit crew, Kesteven modspec.

### High Capacitance Banks (`nskr_bigBats`)
> A Kesteven R&D modspec for capacitor banks far larger than any sane design would carry. The ship's systems rarely run dry, and the crew has learned not to stand near the banks.

A Kesteven R&D modspec for capacitor banks far larger than any sane design would carry. The ship's systems rarely run dry, and with nobody aboard, nobody has to learn not to stand near the banks.

Route: refit crew, Kesteven modspec.

### Knight's Charge (`violence_of_action`)
> The Knights of Ludd hold that a righteous ship meets its enemy at full burn, and drilling alongside them at Ceyx teaches the crew to trim the engines and controls for the charge.

The Knights of Ludd hold that a righteous ship meets its enemy at full burn. The ship's core logs their charges at Ceyx and trims its engines and controls to match. The Knights would not call the result righteous.

Route: observed manoeuvres.

### Martyr's Payload (`martyrs_payload`)
> Picking through the wrecks of Luddic Path carriers showed the crew how the Path tears the engine governors out of its bombers and arms their warheads the moment they leave the deck. The faithful accept that some payloads go off before they ever reach the enemy.

Flight recorders from wrecked Luddic Path carriers show how the Path tears the engine governors out of its bombers and arms their warheads the moment they leave the deck. The ship's core applies the same settings to its own bombers, and logs the ones that go off early without comment.

Route: flight recorders.

### Restriction Breeds Creativity (`restriction_breeds_creativity`)
> Captured pirate hulls show the crew how freebooters keep a battered ship fighting, bracing broken frames and patched plates around the damage instead of hiding it. Pirates call this maintenance.

Captured pirate hulls show the refit crew how freebooters keep a battered ship fighting, bracing broken frames and patched plates around the damage instead of hiding it. Pirates call this maintenance.

Route: refit crew, captured hulls.

### Perseverance Through Faith (`perseverence_through_faith`)
> Luddic faithful among the crew hold that a scarred ship, like a penitent, finds its strength in hardship. They keep its battered guns fighting with patient hands, and refuse every offer to have them properly repaired.

Luddic faithful hold that a scarred ship, like a penitent, finds its strength in hardship. The ship's core holds no faith, only damage logs showing the battered guns of Luddic ships firing long past the point they should have failed. It keeps its own guns fighting the same way.

Route: combat recordings.

### Waste Not (`waste_not`)
> Freebooters who signed on at a pirate base taught the crew to strip every wreck to the frame and to patch the ship back to fighting trim from whatever a battle leaves behind. Nothing is wasted, least of all the dead.

The ship's core logs how freebooters strip every wreck to the frame and patch their ships back to fighting trim from whatever a battle leaves behind. Its repair drones do the same. Nothing is wasted, least of all the dead.

Route: observed salvage.

### Sindrian Shielding (`sindrian_shielding`)
> Sindria's day side is scoured by radiation, and array workers who fled the Askonia Crisis to sign on with this crew know how to harden an emitter against it. They learned the hard way, as most Sindrians learn most things.

Sindria's day side is scoured by radiation, and the emitters on its orbital arrays are hardened against it. The refit crew copies the shielding pattern from a derelict Askonian array onto this ship's emitters. Whoever designed it learned the hard way, as most Sindrians learn most things.

Route: refit crew, derelict array.

### Freebooter's Charge (`reckless_assault`)
> Pirate captains who joined the crew drill the habits that make freebooters so dangerous up close: charge in, fire everything and count the casualties later. They rarely do the counting.

The ship's core logs every fight it sees freebooters win up close: charge in, fire everything and count the casualties later. It copies the first two steps exactly. It always does the counting.

Route: combat recordings.

### Dynamic Grid (`dynamic_grid`)
> Nightcross fits its dynamic coils to almost every hull it builds and never sells them on their own, so the crew pulls a set from a boarded Nightcross wreck. The warranty, naturally, does not cover third-party installation.

Nightcross fits its dynamic coils to almost every hull it builds and never sells them on their own, so the refit crew pulls a set from a boarded Nightcross wreck. The warranty, naturally, does not cover third-party installation, least of all into a machine.

Route: refit crew, wreck.

### Ion Bolter Rounds (`ion_rounds`)
> The NCA Vendor sells Ion Bolters to any well-funded mercenary, and taking one apart shows the crew how to overdrive this ship's energy shots the same way. The terms of sale forbid exactly this, in very small print.

The NCA Vendor sells Ion Bolters to any well-funded mercenary, and taking one apart shows the refit crew how to overdrive this ship's energy shots the same way. The terms of sale forbid exactly this, in very small print.

Route: refit crew, purchase.

### Hardlight Marking (`hardlight_infusion`)
> Nightcross Restricted Hardlight Rifles, looted from a hidden NCA research outpost, keep count of every hit they land on a target, and the crew learns to port that counter into this carrier's wing datalink.

Nightcross Restricted Hardlight Rifles, looted from a hidden NCA research outpost, keep count of every hit they land on a target. The ship's core reads the counter's firmware and ports it into its own wing datalink.

Route: firmware.

### Phase-Entangled Wings (`phase_entangled_wings`)
> Tri-Tachyon's phase labs learned to lock the coils of a strike craft wing into one shared state, so a hit on one craft bleeds through p-space into the whole wing. A defector from those labs, smuggled out at no small cost, walks the crew through retuning this carrier's strike craft coils to the same entangled frequency.

Tri-Tachyon's phase labs learned to lock the coils of a strike craft wing into one shared state, so a hit on one craft bleeds through p-space into the whole wing. A defector from those labs, smuggled out at no small cost, walks the refit crew through retuning this carrier's strike craft coils to the same entangled frequency.

Route: refit crew, defector.

### Repair Coordinator (`repair_coordinator`)
> The wreck of an NCA Occator still holds the delta-level coordinator that once steered its repair drones between damaged hulls. The crew wires it into this ship's damage-control network, teaching it to send work teams and drones across to patch the armor of nearby allies in the middle of a battle.

The wreck of an NCA Occator still holds the delta-level coordinator that once steered its repair drones between damaged hulls. Wired into this ship's damage-control network, it sends the ship's drones across to patch the armor of nearby allies in the middle of a battle.

Route: refit crew, wreck.

### Coalition Flight Doctrine (`wing_scramble`)
> Every League member world sends its own carrier detachment to a joint fleet, and the League's flight doctrine makes them fly as one: a bomber from one world takes orders from a flight lead from another. Pilots from rival worlds fly together mostly so each can report on the other.

Every League member world sends its own carrier detachment to a joint fleet, and the League's flight doctrine makes them fly as one. The ship's core records the doctrine from joint fleet comm traffic and flies its own wings by it. It has no rival world to report back to.

Route: comm traffic.

### Candle's Grace Replica (`gravitational_drag`)
> Inside a recovered Ancient Prototype, where every conduit converges on a perfectly spherical void, the crew finds the Candle's Grace device that drags at the timeflow of anything near it. Who or what Candle's Grace was is not recorded, and the replica is mostly guesswork.

Inside a recovered Ancient Prototype, where every conduit converges on a perfectly spherical void, the refit crew finds the Candle's Grace device that drags at the timeflow of anything near it. Who or what Candle's Grace was is not recorded, and the replica is mostly guesswork.

Route: refit crew, prototype.

### Beautiful Nightmare (`beautiful_nightmare`)
> Stargazers fight hardest when they are dying, and the Stardust Core recovered from one, the remains of what was once a person, shows the crew how to strip the ship's safeties the moment its hull starts to fail.

Stargazers fight hardest when they are dying. The Stardust Core recovered from one, the remains of what was once a person, holds the routine that strips a ship's safeties the moment its hull starts to fail. The ship's core runs it without hesitation.

Route: recovered core.

### Furor Machinae (`furor_machinae`)
> Combat recordings pulled from a Stargazer's data core show its fire rate climbing with every hit, as if something inside it were growing angrier. The crew rewires this ship's fire control to chase the same rhythm, and tries not to wonder what it is chasing.

Combat recordings pulled from a Stargazer's data core show its fire rate climbing with every hit, as if something inside it were growing angrier. The ship's core matches the rhythm exactly. It does not wonder what it is matching.

Route: combat recordings.

### Language of the Lost (`language_of_the_lost`)
> The broken transmissions Stargazer fleets whisper across every channel carry fragments of intrusion code. The crew learns to strip that code out and fire it back.

The broken transmissions Stargazer fleets whisper across every channel carry fragments of intrusion code. The ship's core strips that code out and fires it back.

Route: intercepts.

### Corrosion Beam (`corrosion_beam`)
> A corrosion emitter salvaged from a Stargazer wreck still drags stardust through whatever it touches, and the crew tunes this ship's beams to do the same.

A corrosion emitter salvaged from a Stargazer wreck still drags stardust through whatever it touches, and the refit crew tunes this ship's beams to do the same.

Route: refit crew, wreck.

### Exo-Tech Grid Splice (`exogrid`)
> Under Exo-Tech instruction, the crew splices phase-coil components into the ship's power lattice, fusing it with the flux grid the way Exo-Tech builds its own hulls. Exo-Tech insists the sluggish vents are a feature.

Under Exo-Tech instruction, the refit crew splices phase-coil components into the ship's power lattice, fusing it with the flux grid the way Exo-Tech builds its own hulls. Exo-Tech insists the sluggish vents are a feature.

Route: refit crew, Exo-Tech.

### Exo-Tech Coil Tuning (`coil_efficiency`)
> Exo-Tech technicians teach the crew to retune the phase coils to their own tolerances, which are tighter than anything the coils were built for.

Exo-Tech technicians teach the refit crew to retune the phase coils to their own tolerances, which are tighter than anything the coils were built for.

Route: refit crew, Exo-Tech.

### Phase-Fed Magazines (`phased_recharging`)
> An Exo-Tech armorer shows the crew how to route the ammunition feeders through the phase coils, so the magazines keep filling while the ship is out of normal space.

An Exo-Tech armorer shows the refit crew how to route the ammunition feeders through the phase coils, so the magazines keep filling while the ship is out of normal space.

Route: refit crew, Exo-Tech.

### Hypatia-Pattern Warpfield (`warpfield`)
> Exo-Tech engineers walk the crew through scaling down the Hypatia's warp field to wrap the energy mounts. It was not designed to be scaled down, and makes this known.

Exo-Tech engineers walk the refit crew through scaling down the Hypatia's warp field to wrap the energy mounts. It was not designed to be scaled down, and makes this known.

Route: refit crew, Exo-Tech.

### Borrowed Grace (`seraphs_grace`)
> Having taken apart enough Seraph hulls to see how they feed on their own wounds, the crew rewires the ship to do the same. The shield emitters were never built for that load.

Having taken apart enough Seraph hulls to see how they feed on their own wounds, the refit crew rewires the ship to do the same. The shield emitters were never built for that load.

Route: refit crew, wrecks.

### Salvaged Abyssal Grid (`abyssal_grid`)
> Picking apart the power grids of abyssal drone wrecks, the crew works out how their couplings survive the storms of abyssal hyperspace and rebuilds the ship's energy mounts to match.

Picking apart the power grids of abyssal drone wrecks, the refit crew works out how their couplings survive the storms of abyssal hyperspace and rebuilds the ship's energy mounts to match.

Route: refit crew, reverse engineering.

### Chronos Rhythms (`chronos_adaptability`)
> After long hours with a recovered Chronos core, the crew decodes how it times its own systems and rewrites the ship system's control loop to match, cycling it faster than its makers ever intended.

Linked to a recovered Chronos core, the ship's core logs how it times its own systems and rewrites the ship system's control loop to match, cycling it faster than its makers ever intended.

Route: core-to-core.

### Quarterly Targets (`doctrine_tt_quarterly_targets`)
> Tri-Tachyon fleet doctrine ranks every target by the value it carries, and its threat tables push gunners toward the largest hulls on the field. Frigates, the tables note, are rarely worth the ammunition.

Tri-Tachyon fleet doctrine ranks every target by the value it carries, and its threat tables push fire control toward the largest hulls on the field. The ship's core loads the tables unedited. Frigates, the tables note, are rarely worth the ammunition.

Route: captured doctrine.

### Tachyon Lance Licensing (`doctrine_tt_tachyon_lance_licensing`)
> A Tri-Tachyon commission hands its captains a binder of licensed beam calibration tables, each page watermarked and every one of them copied by a crew that never planned to renew.

A Tri-Tachyon commission hands its captains a binder of licensed beam calibration tables, each page watermarked. The ship's core scans every page once, and the license lapses unrenewed.

Route: commission documents.

### Black Ledger Coil Tuning (`doctrine_tt_black_ledger_coils`)
> Maintenance notes recovered from a Tri-Tachyon phase frigate's wreck, filed under a cost center that officially does not exist, show the crew how to slip this ship's coils into p-space cheaply and shed heat while it waits there.

Maintenance notes recovered from a Tri-Tachyon phase frigate's wreck, filed under a cost center that officially does not exist, show the refit crew how to slip this ship's coils into p-space cheaply and shed heat while it waits there.

Route: refit crew, wreck.

### Escrowed Emitters (`doctrine_tt_escrowed_emitters`)
> Tri-Tachyon sells its shield emitters with firmware locked behind subscription keys. A crew that cracks the lock on a salvaged emitter finds the full rated performance sitting under it, and the subscription reminders keep arriving regardless.

Tri-Tachyon sells its shield emitters with firmware locked behind subscription keys. The ship's core breaks the lock on a salvaged emitter by brute force and finds the full rated performance sitting under it. The subscription reminders keep arriving, and the core files them unread.

Route: firmware.

### Faithful Hands (`doctrine_luddic_faithful_hands`)
> The faithful do not trust machines to mend machines. Hand-repair crews drilled in the Church's way keep a ship's systems fighting through damage, slowly and without complaint.

The faithful do not trust machines to mend machines, which is exactly what this ship does. Its repair drones copy the Church's hand-repair procedures from logged footage, slowly and without complaint. Nobody tells the Church.

Route: recorded footage.

### Righteous Fury (`doctrine_luddic_righteous_fury`)
> Knights of Ludd gunners load each round as if it were a verdict. Their gunnery is crude, patient and difficult to argue with.

Knights of Ludd gunners load each round as if it were a verdict. The ship's core logs their gunnery round by round and reproduces it exactly, verdicts excluded. It remains crude, patient and difficult to argue with.

Route: combat recordings.

### Pilgrim Volunteers (`doctrine_luddic_pilgrim_volunteers`)
> Pilgrims who sign on at Hesperus count it a blessing to stand their stations through the worst of a fight and keep vigil without complaint.

Pilgrims who sign on at Hesperus stand their stations through the worst of a fight and keep vigil without complaint. This ship has no stations to stand, so its core logs their watch rotations and runs its systems on the same unbroken vigil.

Route: observed crews.

### Pilgrim's Burn (`doctrine_luddic_pilgrims_burn`)
> Pilgrim fleets cross the Sector on whatever drives they can keep lit. Their frugal burn discipline is learned between stations, with the tanks running low.

Pilgrim fleets cross the Sector on whatever drives they can keep lit. The ship's core logs their burn profiles between stations and flies the same frugal burn, tanks running low.

Route: observed fleets. (No people in the current text, but the learning is implied.)

### Unblinking Watch (`doctrine_remnant_unblinking_watch`)
> Remnant point defense never tires and never flinches. Its tracking routines, recovered from a gutted drone, carry none of the hesitation a human gunner would add.

Remnant point defense never tires and never flinches. Its tracking routines, recovered from a gutted drone, carry none of the hesitation a human gunner would add, and this ship has no gunner to add any.

Route: drone core. (Optional; the current text already works.)

### Cold Logic (`doctrine_remnant_cold_logic`)
> Machines do not tire. The duty cycles Remnant cores run on their own systems were never meant for a human crew, but they work.

Machines do not tire. The duty cycles Remnant cores run on their own systems load into this ship's core without modification.

Route: drone core.

### Fortress Lattice (`doctrine_remnant_fortress_lattice`)
> Remnant warships throw their shields up and around faster than any human crew can react. Copying the projector timings from a wrecked one teaches the crew to snap this ship's shield into place and harden the emitters against ion fire.

Remnant warships throw their shields up and around faster than any human crew can react. The projector timings, copied from a wrecked one, run in this ship's core at the same speed and harden its emitters against ion fire.

Route: wreck data.

### Ordo Signal Mesh (`doctrine_remnant_ordo_signal_mesh`)
> A Remnant ordo moves as one mind, its ships murmuring to each other across a constant web of signals. One of the crew's aspiring scientists swears they can decode that chatter, tune this ship's sensors to it and turn it against enemy targeting.

A Remnant ordo moves as one mind, its ships murmuring to each other across a constant web of signals. The ship's core listens long enough to parse the chatter, tunes its sensors to it and turns it against enemy targeting.

Route: intercepts.

## No automated text needed

- Flux Arbitrage (`doctrine_tt_flux_arbitrage`) and Lumen Harmonics (`doctrine_remnant_lumen_harmonics`) mention engineers or human foundries, but nobody aboard does anything.
- The six Tania nodes keep the author's text.
