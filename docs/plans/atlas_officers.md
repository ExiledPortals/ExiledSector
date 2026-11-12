# Atlas officers

The Outer Reaches are frontier space the factions are fighting over. Each faction holds small territories that start at 4 to 10 systems, each guarded by a named officer who persists across the save. Every officer the player beats, and every fourth other chart boss, the atlas takes a turn: officers push into unclaimed space and into each other's territories, challenge neighbouring officers, and rise or fall in rank. Beating an officer lets the player pick one of three items that officer will carry the next time they are beaten.

Status: designing, nothing built. Started 2026-11-12. Inspired by the Nemesis system of Middle-earth: Shadow of Mordor and Shadow of War (named, persistent enemy officers who remember the player, rise in rank and fight each other); never use that name. Builds on `atlas.md`, `completed/anomalies/faction_skirmish.md` and `anomalies/overview.md`.

## Territories

A territory is a connected group of systems owned by one faction, with one command centre system held by its officer. Territories replace today's large faction territories (`AtlasTerritories`, sites 16 cells apart, a couple of hundred systems each).

Territories sit close together so the player isn't crossing empty space to reach the next fight, and they often border each other so factions can fight. Unclaimed space is only the gaps between territories, at most 3 systems across. Command centres are placed when a chunk is first revealed, at least 3 lanes from the nearest other command centre; once the starting claims are made, any revealed unclaimed system more than 2 lanes from every territory gets a new command centre (placeholders). Candidate command centre cells come from a hash of the seed and the chunk, taken in order until the spacing holds. A command centre's faction depends on what the player can see, so it is decided at reveal and saved, like the Threat infestation decision for a chunk. The territory's starting claim is the command centre plus the nearest systems by lanes up to a size rolled from 4 to 10, skipping completed systems, the Gateway, special sites, infested systems and other territories. Claims are saved explicitly, since turns change them.

Unclaimed systems belong to independents and mercenaries: each one picks `independent` or `mercenary` on its cell hash (placeholder weights, today's `chart_factions.csv` 5 to 3). They have no officer and no border. Biomes whose faction pool holds only factions that never hold a territory (the Threat Expanse, Remnant Expanse, Abyssal Rift) keep their own factions in unclaimed systems. Elsewhere a region's faction pool no longer sets unclaimed systems' faction; it weights which factions can hold territories there, so the Pirate Badlands fill with pirate territories rather than pirate-owned empty space.

The 4 to 10 is only the starting size. A territory has no size limit: it grows by capturing systems on its turns and shrinks when it loses them. As a soft brake, once a territory has more than 6 systems its chance to declare Expand is multiplied by 6 divided by its size, so it halves at 12 systems and quarters at 24 (placeholder). A territory that loses its command centre breaks up (see Officers losing).

One reader returns a system's owner: infestation, then territory claim, then the unclaimed faction. `AtlasOpening.ownerOf` and `territoryOwnerOf`, the node tooltips, `AtlasTerritoryBorders`, `AtlasFleetTraffic` and Faction Skirmish all go through it. The Faction Territories toggle draws territory borders and each command centre's officer.

## Factions coming and going

No faction is ever wiped out for good. The atlas is endless, and new territories keep appearing in newly revealed space. Each new command centre goes to an officer waiting beyond the frontier (see Ranks): a fled officer first, as below; otherwise one who has never held a command centre, from the lowest rank that still has one whose faction the region allows. So the first territories around the Gateway are all Lieutenants', and each higher rank arrives further out, with the Admirals furthest. Within that rank, the officer is picked by their faction's weight against what the visible atlas already holds:

- a faction with a fled officer waiting for a new territory (see Officers losing) gets weight 10, and that fled officer takes it, so an officer the player just beat tends to turn up again further out and can be chased;
- a faction with no territory on the visible atlas gets weight 4;
- a faction already on the visible atlas gets weight 1 divided by one more than the number of territories it has there.

All placeholders. The command centre's region must still allow the faction (its faction pool). Remnant, derelict, Threat and Abyssal factions never hold territories (reuse `chart_skirmish_factions.csv`); they stay infestations and biome landmarks. Independents and mercenaries never hold territories either, since they are the unclaimed space.

Safeguards:

- at most 3 territories per faction on the visible atlas, so one faction can't paint the frontier;
- the command centre spacing above;
- the board's fixed number of officers (see Ranks): when a command centre is placed and no waiting officer's faction fits its region, the active officer of an allowed faction furthest from any system the player can reach leaves their territory, which breaks up, and moves to it with their rank, scars and items;
- only territories with a system in revealed space take turns, so unexplored space never simulates.

## Commissions

The player's commission faction (`Misc.getCommissionFactionId()`) never holds a territory, so every officer on the board is someone the player may fight. The command centre roll and the absence weighting give it no weight while the commission lasts. The player faction never holds one either, so a player with their own faction needs nothing more.

When the player takes a commission or switches to another one, the commissioned faction's territories are handed over on the next atlas turn or the next atlas opening, whichever comes first, and after the open run closes if it is inside one of them. Each territory gets a new faction from the absence weighting, keeping its command centre and claims. Its officer leaves the board, and a new officer of the new faction takes the command centre with the same rank and every item the old one carried, identified if the old one was. A fled officer of that faction who is waiting for a territory leaves too, and their carried items go to the next new officer to take a command centre. The replacements join the saved turn results, so the board's next opening shows them in the shuffle (old portrait fading out, new one fading in). Nothing is saved for this: the check is whether any territory belongs to the current commission faction. When a commission ends, nothing is handed back; the faction returns through the normal weighting.

Chart fleets stay hostile whatever the player's standing with their faction, and keep ignoring comms, so no tithe, bribe or high-reputation talk turns them (`ChartContentGenerator.HOSTILITY_FLAGS`, as today). Those would let the player pass a system without clearing it, which breaks completion. A territory of a faction the player is not hostile to, such as an ally of their commission faction, plays like any other. The only difference is reputation: if the player's transponder is on when a battle against that faction's fleets starts, they take vanilla's penalty for attacking a non-hostile fleet with the transponder on (`RepActions.COMBAT_AGGRESSIVE`: a large reputation loss and at best Hostile, so it declares war on that faction), and vanilla then decides what that does to the commission. With the transponder off there is no change, which is today's behaviour (`MEMORY_KEY_NO_REP_IMPACT` and `ChartAllyReputation`). Factions hostile to the player never cost reputation.

## Officers

Each territory has one officer, the boss of its command centre system. An officer has a persistent identity: name, portrait, faction, rank, a personality trait, their flagship, scars, the item they carry and a short history of their last few events. The fleet itself is rebuilt from the record every run, so chart systems keep the zero-trace rule.

Each officer has one flagship for life, picked when the officer is generated: a hull and variant from their faction's known ships, leaning larger for higher starting ranks (placeholder), with a ship name and a seed for its NPC tree. The ship never changes, whatever happens to their rank. The rest of the boss fleet is built as today, scaling with the chart's tier and modifiers and the rank's boss size, and the flagship is put in as its flagship (as anomaly core flagships are, `addCoreFlagship`). Its NPC tree is regenerated from the same seed every run, at the rank's Fleet Strength, so it stays the same ship. The flagship can never be recovered, since the officer comes back in it. A replacement Lieutenant gets a new flagship.

An officer never moves on a turn. Expanding, pushing and challenging change claims and ranks, never command centres, and a challenge winner keeps their own command centre. An officer changes system only when they lose their command centre (see Officers losing), so a player heading for an officer always closes in.

An officer record is one encoded string in `AtlasState` (as inserted anomaly records and capture records are), storing only ids and numbers: record id, faction id, name and portrait seeds, rank, command centre cell, trait id, flagship hull, variant and name seed, tree seed, d-mod ids, scars (scar id and stack count each), carried items (id and base count each), pending choice, defeat count, the Lieutenant's last-chance mark, status (active, waiting) and the last three history entries.

The comms greeting and the system tooltip refer to that history ("You ran me out of Kesteven's Reach"), templated through i18n with the atlas system names.

## Ranks

Ranks measure an officer's influence across the whole frontier, not a faction's chain of command. Five military ranks, lowest first: Lieutenant, Commander, Captain, Commodore, Admiral. Each rank row in a new `atlas_officer_ranks.csv` sets boss size, Fleet Strength (tree size), how often the officer acts each turn, and how many places it has. The ranks form a pyramid with a fixed number of places (placeholder: 3 Admirals, 6 Commodores, 8 Captains, 12 Commanders, 16 Lieutenants, 45 officers in all). A higher rank pushes harder and wins more.

The board is generated full when the atlas is created: every place gets an officer, with factions drawn from those that can hold territories (placeholder). Officers without a command centre wait beyond the frontier, and take command centres as space is revealed, lowest rank first (see Factions coming and going). Only officers who have held a command centre take part in turns, so an officer still beyond the frontier keeps their starting rank until they arrive.

Each officer has renown, a number that measures how they are doing and decides their strength within their rank; the board sorts by it. An officer gains 1 renown for each system they claim by Expand or Push, 3 for each challenge won, 2 for each Defend that holds, and each turn 1 for every 4 systems they hold. Every loss, to the player or another officer, halves it (placeholders). Every place is always filled: a rank change is always a swap, and the only officers who die are Lieutenants, whose place goes at once to a new Lieutenant of the same faction. A commission handover's replacement keeps the old officer's place (see Commissions).

The player almost never loses, so most rank changes come from the officers themselves. Ranks change in four ways, and each change shows in the board's shuffle:

- At the end of each turn, for each pair of neighbouring ranks, if the lower rank's most renowned officer has more renown than the higher rank's least renowned, they swap. At most one swap per pair each turn, and an officer who already changed rank that turn is skipped. Being pushed down this way is not a loss and adds no scar.
- A challenge won by the lower-rank officer swaps the two officers' ranks. A challenge won by the higher-rank officer, or between equal ranks, changes no rank.
- An officer beaten by the player drops a rank, swapping with the most renowned officer of the rank below (see Officers losing).
- An officer whose fleet defeats the player (the player loses or retreats from the battle) moves up a rank, swapping with the least renowned officer of the rank above, and gains nothing else. An Admiral stays an Admiral.

Officers above Lieutenant never die. Every loss, to the player or in a challenge, adds a scar, and a loss to the player or to a lower-rank officer also pushes them down the pyramid, so a much-beaten officer sinks and grows more scarred. A Lieutenant can't drop a rank, so their first loss only marks them: beaten by the player they flee as usual, beaten in a challenge they keep their command centre. A marked Lieutenant who loses again before winning a fight dies for good; their replacement takes their command centre if they held one, or waits beyond the frontier, with no scars or carried items. Winning a fight, against an officer or the player, clears the mark.

## Turns

A turn happens when the player defeats an officer (their command centre's boss, or the attacking officer in a Faction Skirmish), on every fourth other chart boss killed, and when an officer's fleet defeats the player. A boss kill counts the moment the boss objective completes (`ChartObjectives.completed`, which calls `AtlasProgress.completed`), not when the run closes; the count of other boss kills is saved in `AtlasState`. A run where the player retreats, loses or leaves without killing the boss gives no turn unless it was an officer who beat them.

With the command centre spacing above, a player heading for the nearest officer clears about one or two other systems on the way, so this gives a little over one turn per officer fight, against more than two and a half with a turn on every boss kill. A player farming systems away from any officer still moves the board, every fourth boss.

The run is still open when its turn resolves, so the turn never touches the open run's system (`AtlasAnomalies.openRunCell`) or the officer fighting in it.

Officers declare their next action one turn ahead, so the board can show what each one intends. At the end of every turn, each active officer rolls its intent, weighted by rank and trait:

- Expand: claim an unclaimed system on the territory's border, less likely the larger the territory.
- Push: attack a border system of a neighbouring territory of another faction.
- Challenge: attack the neighbouring officer directly.
- Defend: an officer who is the target of a push or challenge may defend instead, adding to their roll against it.
- Hold: nothing.

A turn first applies the player's kill (if the boss was an officer, see Officers losing), then resolves the declared intents, dropping any whose officer or target was beaten or whose territory broke up, then declares the next intents.

A declared Push or Challenge appears on the map as a Faction Skirmish inserted into the contested system (an inserted anomaly record from the world, as `atlas.md` planned). If the player runs it before the next turn, the skirmish's existing side choice decides it. If not, it resolves abstractly on the next turn: equal ranks win half the time each, and each rank between them adds 6.25 points to the higher rank's chance, so an Admiral beats a Lieutenant 75% of the time. A declared Defend adds 10 points to the defender (placeholders). A won push moves the system; a won challenge also swaps ranks when the challenger was the lower rank (see Ranks).

Completing a system inside a territory removes it from the territory, since the player cleared its garrison, and officers may claim it again on later turns. So territories shrink where the player plays and refill from their neighbours, and the war follows the frontier.

The turn is a pure function of the saved state, the seed and a turn counter in `AtlasState`, so it is deterministic and unit-testable. A cap on events per turn keeps the news readable. The turn's results (who beat whom, rank changes, deaths, new and fled officers) are saved in `AtlasState` until the next turn, for the board to show (see Officers board).

## Faction Skirmish

Skirmishes move from generator rolls on today's territory borders to the world's inserted records on the borders between neighbouring officers' territories. The attacker's commander is the attacking officer, and the defender's station guards the contested system.

- Siding with the attacker and winning moves the system and counts as the attacker winning the push or challenge.
- Siding with the defender and killing the attacking officer counts as the player defeating that officer (reward choice, scar, flight).
- Staying out resolves the push on its roll when the run closes, whatever the player destroyed.

## Officers losing

When the player kills an officer's flagship as the boss objective, the officer escapes (they get out, as vanilla officers do), drops a rank, gains a scar and loses their command centre. Their flagship comes back with them, repaired but carrying the scar's d-mod (see Scars). Their territory breaks up: its systems become unclaimed, and neighbouring officers can claim them over the next turns.

The fled officer then waits, and takes the next new territory of their faction that appears, which the faction weighting above makes likely to be soon. The atlas tooltip and officer intel say where they went, or "beyond the frontier" while they wait.

Fleeing can't outrun the player. A fled officer's new system must be within a few lanes (placeholder 3) of a system the player can reach: a new territory only takes a waiting officer if its command centre is that close. New territories appear as space is revealed, which happens next to the player's progress, so a waiting officer turns up just ahead of the player. An officer whose command centre a rival takes flees under the same rule.

A Lieutenant beaten by the player flees once and dies on their next loss (see Ranks).

## Reward choice

After the player defeats an officer, a prompt offers three items. The player must pick one, since picking has no downside, and the officer carries it: the next time the player defeats that officer, it drops in addition to their usual boss loot. Each defeat drops everything the officer carries and then offers a new choice. A player who loses, retreats or leaves without the kill gets no choice.

An officer who wins a challenge takes every item the loser carries, and carried items stack, so they gather on the officers who keep winning. The player can steer an item onto an officer they want to chase by helping that officer win.

The three offers are drawn without repeats from one pool, weighted by each item's drop chance:

- hull item base types (the nine non-unique `hull_items.csv` rows, by `rarity`), dropping as a normal rolled hull item at the run tier;
- biome hull items: a hull item base type that always carries a biome modifier, the command centre biome's own for its part, or with none a random one from the biomes the player has opened (as storm loot does, through `GrantedModifiers.add`);
- crafting items (by their `chart_materials.csv` share at the run tier), each offered as a single item with no count shown;
- Prefix and Suffix Inhibitors, each offered as a single item;
- Engrams (by their `engrams.csv` weight, from their `min_tier`);
- rarely, a unique hull item or subroutine (`unique` true, by `rarity`), leaning towards uniques whose `source` is the officer's faction.

The tables use different scales, so each category gets a share of the pool before the item weights apply within it: 35% hull item base types, 15% biome hull items, 30% crafting items, 7% inhibitors, 8% Engrams, 5% uniques (placeholders). These shares and each crafting item's tier (low, mid or high) sit in a new `atlas_officer_rewards.csv`.

Every officer starts with one item already carried, rolled from the same pool when the officer is generated, so the first fight with anyone is worth something. A higher rank rolls it rarer: each rank row in `atlas_officer_ranks.csv` sets that rank's unique share and its crafting item tier weights (placeholder: from 2% uniques and low, mid and high tiers 70/25/5 for a Lieutenant up to 15% and 10/45/45 for an Admiral). A starting crafting item rolls its base count like a picked one. A replacement Lieutenant starts with a Lieutenant's roll.

A crafting item offer silently rolls a base count when picked, saved on the record: 1 to 7 for a low-tier crafting item, 1 to 3 for mid tier, 1 for high tier. When it drops, the count is scaled by the item quantity and crafting item modifiers (`CHART_ITEM_PERCENT`, `CHART_CRAFTING_ITEM_PERCENT`) of the chart the player ran for that defeat. Carried stacks of one crafting item add their base counts together.

Each option shows its icon, name and normal item tooltip, and the officer's tooltip on the atlas shows what they carry, so the player can pick a target to chase. The pending choice is saved on the officer record until answered, so a reload re-opens the prompt. The prompt opens once the post-battle dialog has closed, as a full-screen picker over the campaign (core `CampaignOverlay`).

## Officers board

An Officers button at the bottom right of the atlas screen opens the officers board: one flowchart of every live officer, never split by faction. Ranks are rows, Admiral at the top, so the board narrows towards the few most powerful officers. Within a row, officers sort by renown (see Ranks). Each officer is their portrait, picked from their faction's vanilla portrait set, with their faction's flag (`FactionAPI.getLogo`) in miniature at its bottom right; everything else is in the tooltip. Fled officers waiting for a territory sit in their rank row with a dimmed portrait.

Arrows between portraits show the war. Plans to claim unclaimed space never show on the board, only on the atlas. Solid arrows run from each winner to the officer they beat on the last turn. Dashed arrows run from each officer to the target of their declared Push or Challenge for the next turn, and a shield mark on a target shows it has declared Defend. Hovering a portrait brightens its own arrows and fades the rest.

The first time the board opens after a turn, it plays a four-second shuffle: portraits start where they stood before the turn and slide to their new places, with a rising mark on those who ranked up, a falling mark on those who ranked down, a fade to nothing for those who died and a fade in for new officers. A Replay button plays it again until the next turn. The pre-turn positions come from the saved turn results, and a seen-turn counter in `AtlasState` makes it play once.

Hovering a portrait shows the officer's faction flag at full size, then their name, rank, faction, trait, scars, carried items and recent history, then their flagship alone: its hull, name, d-mods and scars. The rest of the fleet depends on the chart that is run, so the board never shows it. The flagship's tooltip data is built from the officer record without spawning anything, and cached until the record changes, so hovering never rebuilds it. Clicking an identified officer's portrait closes the board and pans the atlas camera to their command centre; it does nothing for an officer beyond the frontier.

An officer starts unidentified. Unidentified officers show as sensor ghosts on the board: the skill tree's dancing fleet triangles in place of a portrait, with no tooltip, arrows or click. A portrait-sized ghost needs more triangles than the skill tree's three (placeholder 7). `SkillTreeNodeGhostRenderer` moves to core `exiledsector.ui.decoration` with the triangle count, size and scatter as parameters, so the skill tree and the board share it. The shuffle moves ghosts too, so the player sees an unknown officer rise or fall. An officer becomes identified when the player stumbles on them, by bringing their system into view on the atlas or fighting them in any system. Identified is saved on the officer record and never lost.

The system an identified officer is in is always visible on the atlas, even outside normal visibility (2 lanes from a completed system, 6 around a Sensor Array), and follows the officer when they move. Only that system shows: the systems and lanes around it stay fogged until the player's own progress reveals them, and the usual reachability rule still applies. This is how the player sees where a beaten officer went.

## Removing Powerful Boss

Officers replace the Powerful Boss anomaly, so it is deleted as part of this work: `charts.anomaly.PowerfulBoss`, its registry entry in `ChartAnomalies`, its `chart_anomalies.csv` and `atlas_anomaly_chances.csv` rows, the `powerful_boss` case in `AtlasSystemArt`, its strings in both catalogues, its icon, `PowerfulBossTest` and its rows in the other tests, its sections in `docs/docs.md`, `atlas.md`, `anomalies/overview.md` and `atlas_tree_anomalies.md`. Saved anomaly records naming it are dropped on load. Its `guaranteesBossTierUp` loot hook goes too: officers don't guarantee a tier-up chart, since bosses already drop plenty of charts. The Flagship Orders catalyst, which targets it, is deleted as well: its `special_items.csv` and `atlas_catalysts.csv` rows and strings.

## Scars

Each time an officer loses to the player, the battle picks a scar from how the player beat them. The scar adapts the flagship against it, adds a themed d-mod and shows as an epithet and a tooltip line. Scar rows live in a new `atlas_officer_scars.csv`: detector and threshold, theme tag, effects with the hull feature each needs, d-mods in order, and levels.

### Detecting a scar

A combat plugin finds the officer's flagship by its member id (`bossFlagshipId`) when combat starts and reads two kinds of signal, watching only that one ship:

- What was fired at the flagship. A damage listener counts every hit at its damage as fired, before the flagship's shields, armour or resistances reduce it, by its source: weapon type, damage type, beam, PD or strike role, missile or mine, fighter and its wing role, collision, and the attacking ship (hull size, phase, the player's flagship, ship system active). Counting what was fired rather than what landed keeps the flagship's own build out of it: a Shield Shunt hull and a strong-shield hull get the same tally from the same player fleet. The same plugin checks a few states of the flagship each frame (overload starting, flux level, peak CR, the angle each hit lands from).
- What the player brought, read once from the player side's deployed ships: hull sizes and design types, weapon OP by type, damage type, range and beam, fighter wings and roles, hull mods and d-mods, officer levels, electronic warfare rating and held objectives.

The tally lives in the combat engine's custom data. When the boss objective completes, every scar's detector is scored as its measure over its threshold, and the highest score of at least 1 wins among the scars that are eligible: under their stack cap (placeholder 3) and with at least one effect that fits the hull. With none, the officer gains Veteran, which has no cap. Only the winning scar id goes into the officer record. A challenge loss has no battle to read, so it adds the scar whose theme tag is the winning faction's strongest tag in `npc_faction_focus.csv`, or Veteran.

The scars that read the flagship's own state (Overloaded, Flux-Pinned, Outlasted, Glass-Jawed, Rust-Bitten) are about the hull's own weakness on purpose, and the stack cap stops one hull taking the same one forever. Long-Shot, Kited, Outpaced and Pinned Down compare against the flagship's own range or speed by design, and their effects close that gap.

### Scar effects

Scars don't grant tree nodes or hull items, so no bonus meant for the player is reused or rebalanced by them. They are a new skill effect group, `ScarSkillEffect` in `exiledsector.skills.skilleffect.scar`, registered in `SkillEffectRegistry`. Each constant is its own effect (named `SCAR_...`, ending in `_PERCENT` or `_MULT` by the usual rule) built on the framework's shared stat and listener shapes, with its magnitude per stack in `atlas_officer_scars.csv` rather than `skill_types.json`, so scars tune apart from every node and modifier. The few effects nothing else has yet (marked * below) get their own listeners in the same package.

A new `ShipEffectSource` in `charts.officer` (as `ChartEnemyShipEffects` does for chart modifiers) reads the flagship's scars from its fleet's memory and contributes their effects times their stacks, on top of the flagship's NPC tree. Each scar row lists its effects with the hull feature each needs (shields, front or omni shield, fighter bays, missile slots, phase cloak); the flagship takes every effect that fits. Every scar also adds 1 level to the flagship's tree (placeholder), and a few add more.

### D-mods

Each defeat by the player adds one d-mod to the flagship, saved on the record: the first in the scar row's list that the ship doesn't have and that applies to it, then any vanilla d-mod that applies. Scars without a theme take one at random. D-mods stop at 5; scars keep coming after that.

### Scar list

Placeholder names, thresholds and magnitudes. The first group reads what was fired at the flagship and its own state; the second reads the player's deployed fleet, which the flagship's build can't skew.

| Scar | Detected by | Effects | D-mods |
|---|---|---|---|
| Flak-Scarred | Missiles are the largest share of damage fired | PD damage to missiles up, missile damage taken down | Damaged Weapon Mounts |
| Torpedo-Burned | Strike missiles, or a few very large hits, are a large share | High explosive damage taken down, armour up | Compromised Armor |
| Mine-Torn | Damage from mines | Mine damage taken down*, manoeuvrability up | Degraded Engines |
| Kinetic-Scarred | Kinetic is the largest damage type fired | With shields: shield damage taken down. Without: kinetic damage taken down | Degraded Shields, Compromised Armor |
| Blast-Scarred | High explosive is the largest damage type fired | Armour up, high explosive damage taken down | Compromised Armor |
| Sun-Scorched | Energy is the largest damage type fired | Energy damage taken down | Faulty Power Grid |
| Shrapnel-Pocked | Fragmentation is a large share | Fragmentation damage taken down, hull up | Compromised Hull |
| Ion-Struck | EMP is a large share of damage fired | EMP damage taken down, weapon and engine durability up | Unreliable Subsystems |
| Beam-Seared | Beams are the largest share | Beam damage taken down*, flux dissipation up | Degraded Shields, Faulty Power Grid |
| Wasp-Stung | Fighters are the largest share | PD damage to fighters up. With bays: its wings' damage to fighters up | Damaged Flight Deck, Damaged Weapon Mounts |
| Bomber-Gutted | Bomber-role wings are a large share | With shields: shield arc and turn rate up. Without: armour up | Degraded Shields, Compromised Armor |
| Ghost-Haunted | Phase ships are a large share | Autofire accuracy, projectile speed and sensor strength up | Glitched Sensor Array |
| Backstabbed | Most hits land from behind | Front shield: shield arc up. Omni shield: shield turn rate up. Without: manoeuvrability up | Degraded Engines |
| Long-Shot | Most damage comes from beyond its own longest weapon range | Weapon range up | Glitched Sensor Array |
| Kited | Its attackers were faster than it, and it dealt little damage | Top speed and zero-flux speed up | Degraded Engines, Erratic Fuel Injector |
| Rammed | Collision damage is a large share | Collision damage taken down*, hull up | Structural Damage |
| Overloaded | It overloaded at least once | Overload duration down, flux capacity up | Faulty Power Grid |
| Flux-Pinned | Long stretches above 90% flux | Hard flux dissipation and vent rate up | Faulty Power Grid |
| Swarmed | Many different attackers within a few seconds | Damage taken down while allies are near | Structural Damage |
| Duellist's Grudge | The killing blow came from the player's flagship | Damage to the player's flagship up* | Ill-Advised Modifications |
| Glass-Jawed | Died within 30 seconds of its first hit | Damage taken down at low hull, hull up | Structural Damage, Compromised Hull |
| Outlasted | Its peak CR ran out before it died | Peak CR duration up | Unreliable Subsystems |
| Rust-Bitten | Several weapon malfunctions | Malfunctions down, max CR up | Damaged Weapon Mounts |
| Abandoned | Its escorts died before it fought | Stats up while no allies are near*, 3 more levels | Malfunctioning Comms |
| System-Shocked | Many hits came while the attacker's ship system was active | System cooldown down, flux capacity up | Unreliable Subsystems |
| Pinned Down | Most of the fight out of weapon range of every enemy | Weapon range and top speed up | Degraded Engines |
| Fighter-Weary | Fighter bays are a large share of the deployed fleet | PD damage to fighters up | Damaged Flight Deck, Damaged Weapon Mounts |
| Phase-Wary | Phase ships are a large share of the deployed fleet | Sensor strength and projectile speed up | Glitched Sensor Array |
| Gnat-Plagued | At least half the deployed ships are frigates | Damage to frigates and autofire accuracy up | Damaged Weapon Mounts |
| Destroyer-Bane | Destroyers lead the deployed fleet | Damage to destroyers up | random |
| Cruiser-Bane | Cruisers lead the deployed fleet | Damage to cruisers up | random |
| Titan-Breaker | Capitals lead the deployed fleet | Damage to capitals up | random |
| Ironbreaker | Low-tech affinity dominates the deployed fleet | Part of its kinetic and energy fire turns high explosive | Compromised Armor |
| Shieldbreaker | High-tech affinity dominates | Shield pierce chance up | Degraded Shields |
| Core-Hunter | Many automated hulls deployed | Damage to automated ships up, shield damage taken from them down | Faulty Automated Systems, random |
| Overdrive-Wise | Many deployed ships have Safety Overrides | Weapon range and top speed up | Degraded Engines |
| Gleam-Hater | Few d-mods across the deployed fleet | Damage to pristine ships up | random |
| Scrap-Hunter | Many d-mods across the deployed fleet | Damage to d-modded ships up* | random |
| Signal-Blind | The player had the higher electronic warfare rating | Electronic warfare penalty down, sensor strength up | Glitched Sensor Array |
| Relay-Starved | The player held most of the objectives | Objective capture range and nav rating up | Malfunctioning Comms |
| Range-Wary | The deployed fleet's average weapon range is above the flagship's | Weapon range up | Glitched Sensor Array |
| Beam-Wary | Beams are a large share of the player's weapon OP | Beam damage taken down*, flux dissipation up | Degraded Shields |
| Kinetic-Wary | Kinetic is a large share of the player's weapon OP | With shields: shield damage taken down. Without: kinetic damage taken down | Degraded Shields, Compromised Armor |
| Ordnance-Wary | Missiles are a large share of the player's weapon OP | PD damage to missiles up | Damaged Weapon Mounts |
| Storm-Wary | EMP weapons are a large share of the player's weapon OP | EMP damage taken down, engine durability up | Unreliable Subsystems |
| Outpaced | The deployed fleet's average top speed is above the flagship's | Top speed up | Degraded Engines |
| Outclassed | The player's officers averaged a higher level than the officer | 3 more levels | random |
| Humbled | The player won while deploying fewer DP than the boss fleet | 4 more levels | random |
| Veteran | No other scar passed its threshold | 2 more levels | random |

## Save footprint

`AtlasState` gains the officer records (one per board place), the territory claims (packed cell to territory id, a few hundred entries), the turn counter, the declared intents, the last turn's results, the seen-turn counter and the command centre decision per revealed chunk. Territory claims replace the capture records; old saves drop their captures (only our saves matter before 1.0).

## Code layout

A new stage package `exiledsector.charts.officer` (added to the CLAUDE.md stage list) holds the records, territories, turn engine, ranks, flagship building, the scar tally and detectors, the scar effect source, identification and reward pool. The scar effects themselves are a new core part package `exiledsector.skills.skilleffect.scar` (`ScarSkillEffect`, its listeners and its tests mirrored), since the registry lives in core; the CLAUDE.md skill effect line gains it as the one package for effects only officers' flagships get. The territory owner reader and command centre placement sit in `charts.atlas`, since they replace `AtlasTerritories`. The reward picker and the officers board go in `exiledsector.ui.atlas`; the shared sensor ghost renderer in core `exiledsector.ui.decoration`. New CSVs go into the editor serialisers.

## Decisions

Decided 2026-11-12:

- Officers are bound to a faction and fight other factions' officers for atlas systems.
- A turn happens on every officer the player defeats, every fourth other boss kill, and every battle an officer wins against the player, never on a run closing.
- The player almost never loses, so officers mostly rise on renown earned in turns, swapping with the least renowned officer of the rank above.
- Starting ranks spread out from the Gateway: never-placed officers take command centres lowest rank first, so Lieutenants surround the Gateway and Admirals are furthest.
- Every officer starts carrying one item, rolled rarer for higher ranks (more uniques and rarer crafting items).
- Biome hull items, Prefix and Suffix Inhibitors and Engrams are reward types.
- Rewards are a choice of three, weighted by drop chance, from hull item base types, crafting items and rarely a unique. The chosen item drops on the officer's next defeat on top of their usual loot. No choice without the kill. There is no reward for revenge or for repeat losses.
- Factions are never wiped out: new territories keep appearing, weighted towards factions missing from the visible atlas, so recently beaten officers can be chased.
- Territories start at 4 to 10 systems with no size limit after that, growing and shrinking by captures every turn.
- Unclaimed space is independents and mercenaries.
- An officer who wins a challenge takes the loser's carried items.
- A Lieutenant survives their first loss and dies if they lose again before winning a fight; a new Lieutenant of their faction takes their place at once.
- Officers above Lieutenant never die. Losing to the player or to a lower-rank officer drops them a rank, and every loss adds a scar.
- An officer who defeats the player moves up a rank by swapping with the least renowned officer above, and gains nothing else.
- The ranks are a pyramid of fixed places (3, 6, 8, 12, 16), generated full at the start, and every place is always filled.
- Between officers, the higher rank wins more often, up to 75% for an Admiral against a Lieutenant.
- "Officer" is the general word; "captain" is only the rank. A faction's group of systems is a territory, and the system its officer sits in is the command centre.
- No second-in-command: each territory has one officer.
- Territories are close together, often bordering, with gaps of at most 3 systems.
- Every new officer starts as a Lieutenant.
- A challenge won by the lower rank swaps ranks; any other result changes no rank.
- The reward prompt can't be skipped. Crafting item offers show as one item with a hidden rolled count, scaled by the chart's item quantity and crafting item modifiers.
- No anomaly reveals officers for now.
- Five ranks.
- Carried items stack.
- Powerful Boss and the Flagship Orders catalyst are deleted; officers replace them and never guarantee a tier-up chart.
- An officers board opens from the bottom right of the atlas screen: one flowchart of all officers, not split by faction, with ranks as influence and only a few officers at the top ranks. Five military ranks, Lieutenant to Admiral.
- Each officer on the board is their faction portrait with a miniature faction flag at the bottom right; the tooltip shows the flag, every detail and the officer's flagship alone. Clicking a portrait pans the atlas camera to their system.
- Each officer has one flagship for life. It never changes and can never be recovered; the rest of the boss fleet scales with the chart's tier and modifiers as any boss fleet does.
- Every defeat by the player adds one d-mod to the flagship, themed by the scar, up to 5.
- Scars are read from damage as fired at the flagship, before its own reductions, and from the player's deployed fleet, so the flagship's build doesn't skew them. Both kinds are scored on the same scale.
- A scar's effects fit the hull: each effect names the hull feature it needs. Each scar stacks at most 3 times; Veteran has no cap.
- Scar effects are their own skill effect group on top of the flagship's NPC tree, never tree nodes or hull items, with their own magnitudes.
- The board shows who beat whom on the last turn and who intends to attack or defend against whom on the next, so officers declare their intents a turn ahead. Plans to claim unclaimed space only show on the atlas.
- The first board opening after a turn plays a four-second shuffle of rank changes, deaths and new officers, with a Replay button.
- Unidentified officers are real sensor ghosts (dancing fleet triangles), with more triangles than the skill tree's.
- An identified officer's system is always visible on the atlas; its surroundings stay fogged.
- Officers never move on a turn; they change system only when they lose their command centre, and only to somewhere close to what the player can reach, so the player can always catch them.
- The player's commission faction never holds a territory; taking or switching a commission hands its territories to other factions, with replacement officers keeping rank and carried items.
- Chart fleets can't be talked down by tithe, bribe or high reputation, since that breaks completion. Fighting a faction the player isn't hostile to costs reputation only with the transponder on.

## Open questions

1. Keeping a hunted officer's chain going: what happens to the reward chain when a hunted officer finally dies?
2. Should the three offers after a defeat also lean rarer for higher ranks, or only the starting item?
3. Every number in this plan is a placeholder to tune in play.

## Build order

1. [ ] Territories: command centre placement by spacing, command centre faction decided at reveal with the absence weighting, starting claims, the unclaimed faction, the owner reader replacing `AtlasTerritories` and the capture records, borders and fleet traffic through it, and dev console commands to list, claim and release. Tests for determinism, claim connectivity and the faction weighting.
2. [ ] Officer records: generation, the flagship pick (hull, variant, name and tree seed) put in as the boss fleet's unrecoverable flagship, the command centre boss as the officer, rank effects (`atlas_officer_ranks.csv`), tooltips, intel and the comms greeting with history. Delete Powerful Boss (see Removing Powerful Boss) in the same step, since command centres take over its bigger boss.
3. [ ] The turn engine on boss defeat: intents declared a turn ahead (expand, push, challenge, defend, hold); abstract resolution; the turn triggers and the saved boss count, the full starting board placed lowest rank first, the rank pyramid, renown and its swaps, challenge swaps and replacing dead Lieutenants; caps and safeguards; saved turn results. Tests that a turn is a pure function of state, seed and counter.
4. [ ] Officers board: the bottom-right button, rank rows of portraits, the full tooltip with the flagship built from the record and cached, last-turn and next-turn arrows, the four-second shuffle and Replay, click to pan the atlas camera, identification on view and fights, the shared sensor ghost renderer moved to core, and identified officers' systems always visible on the atlas.
5. [ ] Faction Skirmish on territories: pushes and challenges inserted as skirmishes, the attacking officer as commander, outcomes feeding the turn engine.
6. [ ] Officers losing: escape, territory break-up, waiting for a new territory, and the chase through new territories.
7. [ ] Commissions: no territories for the commission faction, the hand-over on a new or switched commission with replacement officers in the turn results, and the transponder reputation loss against factions the player isn't hostile to. Tests for the hand-over and that no territory ever belongs to the commission faction.
8. [ ] Reward choice: the weighted pool with biome hull items, inhibitors and Engrams, `atlas_officer_rewards.csv`, each officer's starting item rolled by rank, the hidden crafting item counts, the full-screen picker, the pending choice on the record, the carried drop on the next defeat, the atlas tooltip line.
9. [x] Scars: the combat plugin and damage tally on the flagship, the player fleet snapshot, the detectors and scoring, `atlas_officer_scars.csv`, `ScarSkillEffect` with its new listeners, the officer scar effect source, themed d-mods up to 5, the challenge-loss scar, epithets and tooltip lines. Tests for scoring, eligibility and stack caps, hull-fit effects and the d-mod order. Built in core `skills.skilleffect.scar` and star-charts `charts.officer.scar` (entry point `Scars`, plus `ScarCombatPlugin` and `ScarEffectSource`), with the `ExiledSectorGrantScars` console command. Left for integration: the officer record calls `Scars.grant` on every officer fleet's flagship, even with no scars; after a defeat it uses `Scars.pickAfterBattle`, `Scars.withScar`, `Scars.dModFor` and `Scars.extraLevels`; challenge losses use `Scars.pickAfterChallengeLoss`; the board and tooltips use `ScarText.epithet` and `ScarText.tooltipLines`.
10. [ ] Strings (en and zh_CN), the `docs/docs.md` section, the editor serialisers for the new CSVs.
