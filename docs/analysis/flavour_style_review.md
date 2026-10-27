# Flavour style review against vanilla

All suggestions below were applied on 2026-10-27, in English and zh_CN, except where the user chose to keep the original text.

Review of every node `flavour` in `data/skilltrees/skill_types.json` (92 of 216 types have one) and every `description` in `data/config/exiledSector/hull_items.csv` (59), against vanilla Starsector 0.98a writing. Vanilla corpus read: the story and anomaly rows of `rules.csv` (Galatia Academy, Ziggurat, Planetkiller, Gate Hauler, Kanta, salvage, ruins, abyssal hyperspace, AI cores, Onslaught Mk.I), the narrative strings in the abyss, Dweller and Shrouded Java classes, and `descriptions.csv`, `special_items.csv` and `hull_mods.csv`. Volume branches (Tania and others) were not reviewed beyond what is already merged into `parallel-work`.

## How vanilla writes this kind of text

The closest vanilla register for node and item flavour is the codex: special items, commodities, custom entities and ship descriptions. The story dialogue sets the same voice in second person.

- It opens on the thing itself, as a noun phrase or fragment: what it is, who made it, where it came from. The history follows, then the people around it.
- Knowledge is hedged. Operating principles are "dimly understood", things are "believed", "rumored", "presumably", "some say". Experts admit what they don't know. Mysteries are never explained, only their effects or the readings.
- The humour is dry and institutional. Domain regulations with subsection numbers, lawsuits still pending at the Collapse, marketing that insists a device is not an AI, Hegemony bureaucrats, Tri-Tachyon subscriptions. It usually lands in a short, flat last sentence that deflates the one before.
- The uncanny comes through one concrete wrong detail, an absence, an instrument reading or a crew reaction, never horror adjectives. Strong words are left to Luddic voices. Dwellers are never named in prose, and the Threat's and Omega's origins are never explained.
- Alien or salvaged hardware follows a fixed order: provenance by inference ("clearly a fabricator, because..."), then what can be measured, then what stays unknown, then a deadpan human note. The most exotic items replace the description with one in-world quote and an attribution line, such as an intercepted log with "speaker unknown". That would suit one or two of the Omega items if you want variety.
- Machine voices are formatted, not narrated. Remnant uses lowercase, period-chopped fragments with bracketed slots. Domain systems use all-caps legal notices. Threat lookups are TriPad database errors.
- One specific per beat: a place, a hull class, a cycle, a maker. Not three proper nouns in one sentence.
- Flavour and mechanics are separate. A vanilla hullmod with flavour gives it one sentence (Solar Shielding, Defensive Targeting Array, Shielded Cargo Holds) and then states the effect in a separate paragraph in plain rules language.
- Spelling is US (armor, maneuver, stabilized, kilometer, center). Vanilla uses "fighters" or "strike craft", "normal space" or "n-space", "p-space", "cycles", lowercase "alpha/beta/gamma core", "abyssal hyperspace". It uses the Oxford comma and spaced hyphens ( - ) for asides, never em dashes.
- It never addresses the player with imperatives in a description, never uses exclamation marks outside dialogue, and never uses modern idiom or real-world references.

## Systemic differences

### 1. Effect summaries in the flavour

About 60 of the 75 lore flavours end with, or contain, a sentence that restates the effect in tooltip voice: "This ship hits cruisers and capital ships harder.", "Each d-mod this ship carries makes...", "Ballistic and energy weapons regain their ammunition faster." Vanilla never does this in a flavour sentence, and `docs/tooltip_wording.md` says flavour "never states a mechanic". The effect lines below already say it, so the player reads it twice.

Rule of thumb for the fix: drop any sentence that starts "This ship...", "The ship..." or "<Weapons> <verb> <more/faster>" and only summarises the stat. Keep a clause that describes the hardware or the cost as part of the story ("the shield emitters were never built for that load", "the faithful accept that some payloads go off before they ever reach the enemy"). Where the cut leaves the text ending flat, end on a short dry closer instead, the vanilla way. Most per-entry suggestions below are this cut.

### 2. One framing formula, repeated

"X shows the crew how to Y", "teaches the crew", "lets the crew", "the crew learns to" appears 32 times in 75 lore flavours, nearly always in the same order: source, crew, verb, effect. The crew-modification theme is a design decision and stays, but vanilla never repeats one sentence frame across a set. Vary it by opening on the object or source as vanilla does ("Breacher loads the Navy keeps for boarding actions: shells hardened to punch through plating."), then bring the crew in second, or let the crew be implied.

### 3. Rules text and UI instructions in flavour slots

Several flavours are the vanilla hullmod description or our own effect lines pasted into the grey italic slot: Adaptive Phase Coils, Ballistic Rangefinder, Converted Fighter Bay, Defensive Targeting Array, ECM Package, Missile Autoloader, Neural Interface, both Shield Conversions, High Capacitance Banks, Wormhole, Wormhole Exit and Exoship. For the `vanillaHullMod` nodes the hullmod description is already shown, and for the wormhole and Exoship nodes the `effectText` already says the same thing, so these duplicate. The wormhole texts also contain "Ctrl-click" instructions. Each gets a one-line flavour below.

### 4. Repeated tics

Single uses read like vanilla, but repetition turns them into tics:
- "Nobody aboard can explain / the crew have stopped asking / have agreed not to ask / nobody asks": Candle's Grace, Cosmic Shunt Array, Radiant Shield Lattice, Reality Disruptor Echo, COMSEC Detachment. Vanilla conveys the unknown through readings and hedged experts ("Cursory investigation suggests...", "Not fully understood, but clearly...").
- Luddics and prayer: Perseverance Through Faith, Asher Salvage Guild, Converted Hangar Deck Crew, Chalcedon Charge Shells, Righteous Fury. Vanilla Luddics get patience, hardship, handcraft, tithes, penance and "the Faithful" as well as prayer.
- "machine ordo / machine nexus / machine frigate / machine warship" for Remnant items. Vanilla names them plainly ("Remnant", "Nexus" capitalised); the euphemism four times over stands out.

### 5. Spelling, agreement and terms

- UK spellings: Armour (Vambrace Plating), mould (Salvager Crew), stabilised (Fire Control Suite), centre (Black Ledger Coil Tuning). Vanilla is US, as is the rest of our text.
- "realspace" (Phase Coil Array): vanilla says "normal space" or "n-space".
- "Gamma core", "Alpha core": vanilla writes them lowercase.
- Crew agreement: most texts treat "the crew" as singular ("the crew learns", "pulls"), but the Abyss, Exo-Tech and Remnant ones use plural ("the crew work out", "rewire", "splice", "decode", "have stopped asking"). Vanilla (US) is singular. Pick one; singular matches vanilla.
- "strikecraft" in flavour: `tooltip_wording.md` makes it the house term for effect lines, which is fine, but in flavour vanilla's "fighters" or "strike craft" reads more natively. Optional.
- No Oxford comma anywhere in our flavour; vanilla uses it consistently. Optional, since the house style may prefer none.

### 6. Tone outliers

Modern idiom that vanilla's narrator never uses: "absurdly thick, absurdly heavy and absolutely worth it". Crustacean Integration and the Back-Alley Cyber-Surgeon are kept as they are. Ion Bolter Rounds instructs the player ("Keep pressure on an enemy shield..."). Each has a rewrite below.

## Nodes

Fine as they are, apart from the systemic notes: Nanoforge Mending, Terrifying Presence, Lion's Gaze, Phase-Entangled Wings, Repair Coordinator, Fragment Swarm, Secondary Fabricator, Fragment Coordinator, Chronos Rhythms, Black Ledger Coil Tuning (spelling only), Swarm Logic, Fortress Lattice, Lumen Harmonics, Ordo Signal Mesh, Subroutine Socket.

Frozen Heart's lowercase machine voice copies the Enigma voice from LOST_SECTOR, where the node comes from. That voice is itself modelled on vanilla's Remnant comm greetings: lowercase, period-chopped fragments with bracketed slots. So it fits both. Keep it.

The Navarchy of Tania nodes (Dual-Feed Mounts, Dysis Drive, Carina Charge, Solaris Overdrive, Rise From the Ashes, Thermo-Accelerator) were written by the Tania mod author and stay exactly as they are; they were not reviewed.

Crustacean Integration, Ludd's Light and At Any Cost are deliberate jokes and stay that way. Ludd's Light and At Any Cost have no flavour yet; if they get one, keep it jokey.

| Node | Issue | Suggested flavour |
|---|---|---|
| Heavy Ballistics Integration | Effect summary | Stripping decommissioned Onslaughts at the Agreus shipbreaking yards shows the crew how the Navy seats its heaviest guns directly into the frame. The Ko Combine bills for the scrap, not the lesson. |
| Ground Support | Effect summary | Raids flown alongside pirate crews teach the crew to turn spare berths and cargo lifts into a drop platform for the marines, and to keep a few guns pointed at the ground while they work. |
| TriPad Backdoor | Effect summary | Every TriPad in the Sector ships with backdoors Tri-Tachyon officially denies, and a crew that learns them can talk a nav buoy or comm relay into changing sides mid-battle. Company support declines to comment, but will gladly sell an upgrade. |
| Disintegration | "come apart" then "taking those wrecks apart"; effect folded in | Hulls killed by Remnant energy weapons come apart around every impact in a way no human weapon manages. Salvors who study enough of the wrecks learn to tune this ship's energy weapons to the same pattern, and try not to think about what it resembles. |
| Machine Precision | "Gamma" capitalised; effect summary | A recovered gamma core, kept powered just long enough to be questioned, gives up the targeting routines Remnant drones use to lead a target. Afterwards it is disposed of, in accordance with Hegemony regulations and the crew's nerves. |
| XIV Torpedo Doctrine | "thrusters into its rear thrusters" | XIV Battlegroup strike manuals, kept by the Hegemony since the Domain, teach that a torpedo should close fast and fly straight. Following them, the crew rewires each missile's maneuvering thrusters to push it forward instead, on the principle that a missile which cannot turn must not miss. |
| Refracting Projectiles | Effect summary | Pulling apart the energy converters of wrecked Tri-Tachyon warships shows the crew how to tune this ship's energy bolts to glance off an enemy shield and carry on to another target. Tri-Tachyon sales literature calls this efficiency. |
| Hullbreacher Rounds | Effect summary | Former Hegemony Navy gunners in the crew remember the breacher loads the Navy keeps for boarding actions, shells hardened to punch through plating instead of bursting on it. The crew sees no reason to save them for boarding. |
| Navy Heartbeat | Effect summary | Sailing alongside Hegemony battle lines teaches the crew the Navy's habit of arriving at speed and firing before the enemy has settled, guns thumping out what the Navy calls its heartbeat. |
| League Picket Targeting | Effect summary | Studying the sensor pickets the League posts along its Hegemony border shows the crew how to slave energy weapon targeting to the ship's own sensors. The pickets are the one League institution every member world agrees to fund. |
| Adaptive Phase Coils | Vanilla hullmod rules text | Phase coils lose their grip on normal space as hard flux builds. A crew willing to rewind them by hand can keep that grip a while longer. |
| Ballistic Rangefinder | Rules text; duplicates hullmod | A rangefinder slaved to the ship's largest ballistic mount, feeding its solution to every smaller gun aboard. |
| Converted Fighter Bay | Rules text | The built-in bays are stripped of launch rails and chassis forges, leaving a great deal of oddly shaped cargo space. |
| Defensive Targeting Array | Rules text; duplicates hullmod | Use vanilla's own one-line flavour from the hullmod description, about fighter comm links repurposed for short-range targeting, rather than the mechanics. |
| ECM Package | Rules gloss, awkward grammar | Jammers, decoy emitters and a deliberately noisy reactor or two, tuned so that enemy fire control never quite trusts its own solutions. |
| Hybrid P-space Shrouding (renamed from Hybrid Physics Shrouding; applied) | Long; effect summary | Leaked papers from Tri-Tachyon's Hybrid Physics Division (on phase work the corporation swears it abandoned under the Treaty of Thule) show the crew how to bleed this ship's sensor emissions into the masking field phase ships throw over a fleet. Tri-Tachyon maintains the papers are forgeries. |
| League Reload Drill | Effect summary | League fleets drawn from a dozen squabbling polities still share one reload drill, so that any ship can rearm any other. It is the one thing the member worlds have never argued over, and serving under a League commission drills it into the crew. |
| Missile Autoloader | Rules text | Rack-side autoloaders that refill the small launchers from a common magazine, for as long as the magazine lasts. |
| Neural Interface | Long rules text | A direct neural link between this ship and the flagship. Captains who use one describe it as standing on two bridges at once, and are sometimes vague afterwards about which one they were on. |
| Critical Point Refit | Effect summary (minor) | A prototype modspec from Kesteven R&D shows the crew how to reinforce a light hull's critical structural points, where thin plating fails first. |
| Inertial Supercharging | Effect summary (minor) | A Kesteven R&D modspec lets the crew feed the ship's own momentum into its guns. Kesteven's notes recommend never slowing down. |
| ABSORB Reserve | Effect summary (minor) | Disassembling the ABSORB loop from a Kingstork taught the crew how to bank the energy their shields block instead of venting it, and how to spend it on the guns. |
| Volatile Flux Tuning | Mechanics list | A prototype flux injector from Kesteven R&D lets the crew trade staying power for tempo. The modspec describes the side effects as manageable. |
| High Capacitance Banks | Mechanics; "defense slot" is a LOST_SECTOR term; no Kesteven framing | A Kesteven R&D modspec for capacitor banks far larger than any sane design would carry. The ship's systems rarely run dry, and the crew has learned not to stand near the banks. |
| Shield Conversion - Front | Vanilla rules text | The omni-directional emitter is locked forward and its projectors refocused into one wide arc. |
| Shield Conversion - Omni | Rules text | The fixed frontal emitter is replaced with a rotating mount salvaged from an omni-shield hull, which never quite covers what the old one did. |
| Shared Fate | Bland; no closer | League battle lines drawn from rival polities learned to stand or fall together, if only because none would let another take the credit for retreating first. |
| Knight's Charge | Effect summary | The Knights of Ludd hold that a righteous ship meets its enemy at full burn, and drilling alongside them at Ceyx teaches the crew to trim the engines and controls for the charge. |
| Martyr's Payload | Partial effect summary | Picking through the wrecks of Luddic Path carriers showed the crew how the Path tears the engine governors out of its bombers and arms their warheads the moment they leave the deck. The faithful accept that some payloads go off before they ever reach the enemy. |
| Restriction Breeds Creativity | Effect summary | Captured pirate hulls show the crew how freebooters keep a battered ship fighting, bracing broken frames and patched plates around the damage instead of hiding it. Pirates call this maintenance. |
| Perseverance Through Faith | Effect summary; prayer tic | Luddic faithful among the crew hold that a scarred ship, like a penitent, finds its strength in hardship. They keep its battered guns fighting with patient hands, and refuse every offer to have them properly repaired. |
| Waste Not | Effect summary | Freebooters who signed on at a pirate base taught the crew to strip every wreck to the frame and to patch the ship back to fighting trim from whatever a battle leaves behind. Nothing is wasted, least of all the dead. |
| Wormhole | UI instructions; duplicates effectText | Two ends of one passage through the ship's systems, stitched together in a way no shipwright intended. |
| Wormhole Exit | Rules and UI text; duplicates effectText | The far end of a faction's wormhole. Nothing on this side leads back to it. |
| Exoship | Rules and UI text after the first paragraph | The last Exoship of Exo-Tech, warping from system to system across the Sector. It keeps its own schedule and answers only to its own beacon. |
| Sindrian Shielding | Effect summary | Sindria's day side is scoured by radiation, and array workers who fled the Askonia Crisis to sign on with this crew know how to harden an emitter against it. They learned the hard way, as most Sindrians learn most things. |
| Freebooter's Charge | Mechanics list | Pirate captains who joined the crew drill the habits that make freebooters so dangerous up close: charge in, fire everything, and count the casualties later. They rarely do the counting. |
| Dynamic Grid | Long; effect sentence | Nightcross fits its dynamic coils to almost every hull it builds and never sells them on their own, so the crew pulls a set from a boarded Nightcross wreck. The warranty, naturally, does not cover third-party installation. |
| Ion Bolter Rounds | Instructs the player | The NCA Vendor sells Ion Bolters to any well-funded mercenary, and taking one apart shows the crew how to overdrive this ship's energy shots the same way. The terms of sale forbid exactly this, in very small print. |
| Hardlight Marking | Effect summary | Nightcross Restricted Hardlight Rifles, looted from a hidden NCA research outpost, keep count of every hit they land on a target, and the crew learns to port that counter into this carrier's wing datalink. |
| Coalition Flight Doctrine | Long; mechanic at the end | Every League member world sends its own carrier detachment to a joint fleet, and the League's flight doctrine makes them fly as one: a bomber from one world takes orders from a flight lead from another. Pilots from rival worlds fly together mostly so each can report on the other. |
| Candle's Grace Replica | Effect summary; "nobody aboard" tic | Inside a recovered Ancient Prototype, where every conduit converges on a perfectly spherical void, the crew finds the Candle's Grace device that drags at the timeflow of anything near it. Who or what Candle's Grace was is not recorded, and the replica is mostly guesswork. |
| Beautiful Nightmare | Effect summary | Stargazers fight hardest when they are dying, and the Stardust Core recovered from one, the remains of what was once a person, shows the crew how to strip the ship's safeties the moment its hull starts to fail. |
| Furor Machinae | Second sentence repeats the first | Combat recordings pulled from a Stargazer's data core show its fire rate climbing with every hit, as if something inside it were growing angrier. The crew rewires this ship's fire control to chase the same rhythm, and tries not to wonder what it is chasing. |
| Language of the Lost | Effect summary | The broken transmissions Stargazer fleets whisper across every channel carry fragments of intrusion code. The crew learns to strip that code out and fire it back. |
| Corrosion Beam | Effect summary | A corrosion emitter salvaged from a Stargazer wreck still drags stardust through whatever it touches, and the crew tunes this ship's beams to do the same. |
| Exo-Tech Grid Splice | Mechanics list; plural crew | Under Exo-Tech instruction, the crew splices phase-coil components into the ship's power lattice, fusing it with the flux grid the way Exo-Tech builds its own hulls. Exo-Tech insists the sluggish vents are a feature. |
| Exo-Tech Coil Tuning | Effect summary | Exo-Tech technicians teach the crew to retune the phase coils to their own tolerances, which are tighter than anything the coils were built for. |
| Phase-Fed Magazines | "twice as fast" is a number | An Exo-Tech armorer shows the crew how to route the ammunition feeders through the phase coils, so the magazines keep filling while the ship is out of normal space. |
| Hypatia-Pattern Warpfield | Mechanics list | Exo-Tech engineers walk the crew through scaling down the Hypatia's warp field to wrap the energy mounts. It was not designed to be scaled down, and makes this known. |
| Borrowed Grace | Mechanics list; plural crew | Having taken apart enough Seraph hulls to see how they feed on their own wounds, the crew rewires the ship to do the same. The shield emitters were never built for that load. |
| Salvaged Abyssal Grid | Mechanics list; plural crew | Picking apart the power grids of abyssal drone wrecks, the crew works out how their couplings survive the storms of abyssal hyperspace and rebuilds the ship's energy mounts to match. |
| Seraph Hunting Routines | Effect summary (minor) | Code pulled from a dissected Seraph core, pieced back together and patched into the damage-control software. It mends the ship on what the guns take from others. |
| Alpha-Grade Fire Control | "Alpha" capitalised; effect summary | Fire-control routines lifted from a Tri-Tachyon line ship, written by an alpha core that never meant to share them. |
| Flux Arbitrage | Effect summary (minor) | Corporate engineers treat flux like capital: borrow hard and repay fast. Their venting schedules, copied from a captured Aurora's maintenance logs, carry a confidentiality notice longer than the schedules themselves. |
| Quarterly Targets | Effect summary | Tri-Tachyon fleet doctrine ranks every target by the value it carries, and its threat tables push gunners toward the largest hulls on the field. Frigates, the tables note, are rarely worth the ammunition. |
| Tachyon Lance Licensing | Effect summary | A Tri-Tachyon commission hands its captains a binder of licensed beam calibration tables, each page watermarked and every one of them copied by a crew that never planned to renew. |
| Escrowed Emitters | Effect summary | Tri-Tachyon sells its shield emitters with firmware locked behind subscription keys. A crew that cracks the lock on a salvaged emitter finds the full rated performance sitting under it, and the subscription reminders keep arriving regardless. |
| Hostile Takeover | Effect summary | Tri-Tachyon's boardroom answer to every fight: commit everything and win before the bill arrives. The bill always arrives. |
| Blessed Plating | Effect summary (minor) | Luddic shipwrights bless every plate they hang, and they hang far more of them than any yard would allow. Their slow, devout layering takes cycles to learn and longer to finish. |
| Faithful Hands | Effect summary | The faithful do not trust machines to mend machines. Hand-repair crews drilled in the Church's way keep a ship's systems fighting through damage, slowly and without complaint. |
| Pilgrim's Burn | Effect summary | Pilgrim fleets cross the Sector on whatever drives they can keep lit. Their frugal burn discipline is learned between stations, with the tanks running low. |
| Righteous Fury | Effect summary; prayer tic | Knights of Ludd gunners load each round as if it were a verdict. Their gunnery is crude, patient, and difficult to argue with. |
| Penitent Hulls | Lore: vanilla moved Gilead's heavy industry to Asher | Church salvage yards on Asher send no ship to the breakers while it can still fly, and keep their scarred hulls flying on patience and very little else. |
| Pilgrim Volunteers | Effect summary | Pilgrims who sign on at Hesperus count it a blessing to stand their stations through the worst of a fight and keep vigil without complaint. |
| Plain Rites | "this refinement" has no referent | Luddic doctrine calls a ship system's refinements vanity. Stripped back to plain, honest machinery, it does what it was built to do and nothing more. |
| Unblinking Watch | Effect summary | Remnant point defense never tires and never flinches. Its tracking routines, recovered from a gutted drone, carry none of the hesitation a human gunner would add. |
| Cold Logic | Conditional "would keep" | Machines do not tire. The duty cycles Remnant cores run on their own systems were never meant for a human crew, but they work. |
| Tireless Cadence | Conditional mood; long | Remnant warships never stand down, their systems cycling at a tempo built for machines that never tire. Copied into this ship's reactor and fire control, that tempo is easy to keep in battle and hard to recover from afterwards. |

124 types have no flavour. Most are plain stat nodes or vanilla hull mod nodes, which show the hullmod description and need none. A few notables also have none and would read better with one line: Live Munitions, All Must Serve, Husk Plating, Rationing, Combat Vision, Optimised Lenses and Temporary Acceleration.

## Hull items

Fine as they are: Back-Alley Cyber-Surgeon (kept by choice), Coatl Bastion Gunnery Team, COMSEC Signals Detachment, Auxiliary Conscripts, Tri-Tachyon Morale Assurance Officer, Gryphon Missile Crew, Pather Cell, Galatia Hyperspace Physics Group, Nanoforge Seed, Synchrotron Cell, Fullerene Spool Integration, Deserter Gunners, Gate Hauler Drive Coil, Kanta's Den Scrapdogs, Purifier Breech Refit, Low Arc Shield Emitter Core, Culann Conversion Kit, Ion Discharge Coils, Hardlight Veil Projectors, Extended Warranty Contract, Corrosion Lens Filter, Old Guard Gun Captains, Tachyon Lens Technicians, Line Yard Ordnance Crew, Bulwark Riveters, Heritage Fire Control, Mixed Doctrine Flight Instructors, Deck Armorers' Guild, Lion's Guard Blueprint Vault, Bridge Officer, Specialist Crew, Drive Assembly, Reactor Regulator, Armor Plate, Deck Crew, Shield Emitter, Ordo Feed Conversion, Displacer Failsafe, Pather Wing Zealots, Facet Mount Geometry. The hull items are already much closer to vanilla than the nodes: object-first, short, with dry closers.

| Item | Issue | Suggested description |
|---|---|---|
| Alpha Site Survivor | "black lab", "seen the other side" cliché | One of very few to leave Alpha Site alive after Project Ziggurat. They don't talk about it, and sometimes hum something under their breath while tuning the phase coils. |
| Sindrian Fuel Company Engineers | "kilometre" is the wrong scale for a fuel load; invented company name reads like vanilla canon | Engineers from Sindria's antimatter plant, trained under the Diktat's production quotas. They squeeze every last light-year out of a fuel load and resent being asked how. |
| Olinadu Security Detachment | "the police is" | Contract officers from Olinadu, where the police are a corporation and the corporation is the law. They are very good at kicking in doors and coming home afterwards. |
| Asher Salvage Guild Crew | Prayer tic | A crew from the Church's own Asher Salvage Guild, raised among the mines and refineries of a world of penance. They patch a hull with spare plating and patience, and find something worth keeping in every wreck. |
| Converted Hangar Deck Crew | Prayer tic | Deck crew from a Church carrier, where every spare bay holds another wing. They turn strike craft around with a speed the Church prefers not to call efficiency. |
| Salvager Crew | "mould" | An independent salvage outfit in the mold of Ibrahim Interstellar Transport. They know which wrecks are worth the trip and how to keep a ship flying on scraps. |
| Ko Combine Shipbreakers | Vague "yard" | Shipbreakers from the Ko Combine's yards on Agreus. Knowing exactly how a hull comes apart also tells them how to keep this one together long enough to be hauled home. |
| Vambrace Plating | Ad-copy voice; "Armour" | Applique armor recovered with the Onslaught Mk.I. Modern forges could not make it, and modern hulls were never meant to carry it. |
| Planetkiller Resonance Circuit | "managed to reverse engineer" contradicts vanilla's "dimly understood" | A resonance circuit salvaged from a planetkiller, its operating principles as dimly understood as the warhead's own. Pointed at anything smaller than a world, it finds the largest thing in range and tries to shake it apart. |
| Chalcedon Charge Shells | Prayer tic (minor) | Solid shot hollowed out by Pather armorers on Chalcedon and packed with stolen mining charge. Whatever it strikes breaks apart in Ludd's fire, which the armorers consider the point. |
| Cosmic Shunt Array | "Nobody aboard can explain" tic | A shunt array that spills shield strain into the stardust between ships, where the nearest ally's grid catches it. The engineering team's report runs to one page, most of it apology. |
| Absence of Light | Vague second sentence | A reactor shard pulled from a Stargazer wreck, burning with a light that casts no shadow. Near it, whatever the crew does seems to work as well as the best they have ever done. |
| Fire Control Suite | "stabilised" | Targeting computers, stabilized feeds and a gunnery officer who reads the solution aloud. Shells land where they were meant to, more often. |
| Phase Coil Array | "realspace" | Replacement phase coils wound to a tighter tolerance than the originals. Slipping out of normal space takes less effort, and the ship moves more freely while it is gone. |
| Radiant Shield Lattice | "machine warship" euphemism; "stopped asking" tic | Shield lattice cut from the prow of a dormant Remnant warship. It runs cold under fire that should melt it, and sensors show no reason why. |
| Equivalent Exchange Core | "machine nexus"; the name is a real-world reference | A reactor governor salvaged from a Remnant Nexus, still balancing a ledger no human wrote. Whatever the ship gives up, it takes back in another form, and never as much as it was given. |
| Shard Microburn Cluster | Fine; optional | Drive nodes pried from a shard found drifting far from any star. They burn without any fuel the crew can find, and the ship slips past sensors the way the shard once did. |
| Tesseract Stillwell | Fine; minor | Recovered from the heart of a tesseract wreck, it holds a pocket where time runs slow outside and fast within. The crew moves quicker than it should around it, and its shots fade sooner than they used to. |
| Reality Disruptor Echo | "agreed not to ask" tic | Fire control salvaged alongside a reality disruptor from wreckage that should not exist. Now and then a shot leaves the barrel twice. The logs record both, with identical timestamps. |
