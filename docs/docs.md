# Exiled Sector: skill effect mechanics

This document explains how the less obvious skill effects work, with a focus on the ones that behave
differently from vanilla Starsector or have no vanilla equivalent. The magnitudes quoted are the
defaults in `data/skilltrees/skill_types.json` at the time of writing. Most of them are placeholders and
will change with balancing.

## Shared Fate

The Shared Fate notable combines `SHIELD_DAMAGE_SHARED_PERCENT` at 20 with `WEAPON_DAMAGE_PERCENT` at
−20. The smaller Shared Damage Taken nodes add to the same shared percentage. Nothing in vanilla works
like this.

When something hits the ship's shield, the game first looks for friendly ships within 1000 su that are
alive, not hulks, not fighters, and not overloaded or venting. If there are none, the hit lands normally.
If there are, the hit is reduced by the shared percentage before it lands. That percentage is the total
from every allocated node, capped at 90%. Only shield hits are shared; armour and hull damage never are.

Once the reduced hit has landed, the mod works out how much flux the removed portion would have caused on
this ship's shield and hands that amount to the allies as hard flux, split evenly between them. Distance
doesn't matter, and neither does the ally's own shield: the flux is calculated with this ship's shield
efficiency, and an ally with no shield, or with its shield down, still receives its share. The mod places
no cap on how much flux an ally can receive this way.

The price is a 20% reduction to the damage of every weapon on the ship. It goes through the weapon stat
hierarchy described further down, so it pools with any other weapon damage modifiers.

## Lion's Gaze

Lion's Gaze is the beam-splitting keystone (`beam_split`, using `BEAM_WEAPON_SPLIT_TARGETS_FLAT` at 1).

Any beam hit on an enemy ship starts a split, whether it lands on shield or hull. The split looks for up
to N additional targets, where N is the total magnitude, choosing the nearest other hostile ships that are
alive, aren't fighters, can be hit (phased ships and other effects' drones are skipped) and are within half the beam
weapon's range of the impact point. The beam's damage is then shared evenly
between the original target and the split targets, so with one extra target each receives half. The
original target's reduction is applied as a damage modifier, which also scales the EMP of that hit. The
beam's own special effects, such as those of the Graviton Beam or Tachyon Lance, are not reduced; only the
damage is shared. The split runs before every other damage-dealt effect on the ship, so effects that read a hit's
damage, such as Disintegration's armour stripping, see the shared amount whatever order the nodes were
allocated in. Energy chaining is ordered the same way.

Normally each split target is fired on by a real beam. The mod creates an invisible, invulnerable drone
that carries a copy of the firing weapon and places it just outside the original target's shield (or its
hull, if the shield is down), on the line towards the split target. A connecting beam is drawn from the
original impact point to the drone, so the result looks like the beam refracting off the target. Because
the drone fires a genuine beam, the weapon's own effect code runs, damage is applied per second as usual,
normal hard and soft flux rules apply, and the AI reacts to it as it would to any other beam.

The drone is set up to behave like the ship it's standing in for. It receives a copy of the ship's
captain, with the same personality, AI core, level and skills. The ship's weapon stat modifiers are copied
onto it every quarter of a second. The ship's damage-dealt listeners are shared with it when it is
created (apart from other drone-spawning effects), so on-hit effects from other nodes still apply.
Listeners that also run every frame, such as Energy Weapon Mastery's, are attached through a pass-through
that calls the firing ship's own listener. The drone's own copy of such a listener is removed, so the
bonus follows the firing ship's flux rather than the drone's, and the listener still runs only once per
frame. Distance-based bonuses such as Energy Weapon Mastery's range falloff are measured from the drone,
because that is where the split beam starts. The split share is applied before any shared listener sees
the hit, so effects that scale with damage dealt work from the shared amount.

Drones don't carry the firing ship's hull mods, because those would apply their full effects again.
Some weapon scripts check their ship for a hull mod, though, such as the ET-IX Dawnstar generator
modes or the feedback-error misfire hull mods. Hull mods listed in
`data/config/exiledSector/drone_marker_hullmods.csv` are copied onto the drone on its first tick when the
firing ship has them, so those checks see the same answer. The copy happens after the game has applied
the drone's hull mods, so their creation effects never run on it. Each listed hull mod's per-frame code
still runs on the drone, so only hull mods that are pure markers in combat belong on the list. The same
applies to Refracting Projectiles drones. Other mods can add rows to the file.

Once a drone starts firing it keeps going for at least one second, and after that for as long as the
original beam keeps hitting, plus a 0.3 second grace period. Both times run on the firing ship's clock,
so time dilation doesn't cut them short. It stops straight away if its target dies or
the firing ship is destroyed or leaves the battle. Each drone runs its own timer, so it stops and removes
itself even after the firing ship has stopped running.

Some beams can't be put on a drone. That happens when the weapon's effect code is listed in
`data/config/exiledSector/split_beam_effect_blocklist.csv`, when the weapon's size has no drone slot, or
when the drone hull fails to load. In those cases the split is simulated instead. Each damage tick of the
original beam applies instant damage to every split target, equal to the tick's damage multiplied by the
split share. EMP is added in proportion to the weapon's EMP-to-damage ratio, the flux is soft unless the
beam forces hard flux, and a MagicLib fake beam provides the visual. Simulated splits don't run the
weapon's effect code, and the AI doesn't treat them as beams.

## High Scatter Amplifier

The High Scatter Amplifier notable uses `BEAM_WEAPON_HARD_FLUX_PERCENT` at 50 and `BEAM_WEAPON_RANGE_MULT`
at −25. It is unlocked by the High Scatter Amp blueprint and can't be combined with the vanilla High
Scatter Amp or Advanced Optics hull mods, or with the Advanced Optics node. It's a deliberately simplified
take on the vanilla hull mod.

The hard flux conversion happens on the target after the hit lands. It takes the percentage from the
firing ship (capped at 100%), converts that share of the hit's shield flux from soft to hard, and never
pushes hard flux above the target's current total flux. Beams that already force hard flux are left
alone. The missing damage bonus is planned and would use `BEAM_WEAPON_DAMAGE_PERCENT`.

## The weapon stat hierarchy

Every effect that modifies a weapon-type stat follows the naming pattern
`<SCOPE_>WEAPON_<STAT>_<FLAT|PERCENT|MULT>`. `WEAPON_DAMAGE_PERCENT` affects every weapon,
`ENERGY_WEAPON_RANGE_MULT` affects only energy weapons, and `BEAM_WEAPON_DAMAGE_PERCENT` affects only
beams. The scopes form a tree:

```
ALL weapons (no prefix)
├─ BALLISTIC
├─ MISSILE
└─ ENERGY
   ├─ NON_BEAM_ENERGY
   └─ BEAM
```

A parent scope always covers all of its children. FLAT adds a fixed amount, PERCENT means "increased" or
"reduced" by a percentage, and MULT means "more" or "less". Only combinations the engine can support
cleanly are generated; any other name is rejected when the data loads.

Flat and percentage modifiers from every scope that reaches a weapon are added together before the vanilla 
formula runs, so +10% to all weapons and +10% to beams gives beams +20%. The same MULT effect from several nodes is
added into one multiplier (never past 100% less), which then multiplies with every other multiplier on the stat.
The engine's energy stats already apply to beams, so an ENERGY effect writes only to the energy stat and 
beams no longer receive the bonus twice.

Beam flux cost is the exception to that pooling: the engine multiplies the energy flux cost modifiers by the
beam flux cost multiplier, so a BEAM flux cost percentage stacks multiplicatively with WEAPON and ENERGY ones.
-10% and -10% gives beams 81% of their base flux cost, not 80%.

The engine has no stat for non-beam energy weapons only. NON_BEAM_ENERGY damage and range therefore put
the bonus on the energy stat and a matching cancelling entry on the beam stat. Because damage and range
pool per weapon, beams end up exactly where they would have been without the node. The cancelling entry
never appears in a tooltip.

The engine keeps a single ammo stat for all energy weapons, so ammo and ammo regeneration for beams or for
non-beam energy weapons are applied directly to each matching weapon when the ship is created. The result
is the same as a native stat would give and is correct in combat, but refit weapon tooltips don't show it.
Regeneration is only exact against energy regeneration modifiers that exist when the ship is created, and
these effects can't be used on temporary nodes.

Weapon turn rate only has two engine stats, one for every non-beam weapon and one for beams, so the only
turn rate effects are the all-weapons one (which now includes beams, Armored Weapon Mounts' penalty
included) and the beam one. For missiles, projectile speed means the missile's maximum flight speed; for
energy weapons it covers both projectile speed and beam travel speed. Autofire accuracy and ECCM chance
both start from zero, so their effects exist only as PERCENT.

When a node has matching child effects with the same stat, mode and value, its tooltip merges them into
the parent. Ballistic, missile, non-beam energy and beam damage at +10% each read simply as "Increases
weapon damage by 10%". A parent and a child on the same node stay on separate lines, because they stack.

## Refracting Projectiles

`NON_BEAM_ENERGY_WEAPON_CHAIN_CHANCE_PERCENT` gives a non-beam energy projectile that hits a shield a
chance to refract. The refracted shot goes for the nearest enemy ship within the weapon's range of the
impact point that the chain hasn't hit yet. If it hits an enemy shield, it can refract again, up to the
"Max Refractions" setting (5 by default). Beams never refract, and neither do weapons that fire missiles or
weapons listed in `data/config/exiledSector/energy_chain_blocklist.csv`.

The refracted shot is fired by the real weapon, in the same way as Lion's Gaze. An invisible,
invulnerable drone carrying a copy of the weapon is placed at the shield that was hit, on the line
towards the next target. If that target is behind the hit ship, the drone sits just past the far side
of the shield, otherwise just in front of the impact point. It fires once, leading a moving target. A
brief streak in the projectile's colours is drawn from the impact point to the drone, so the shot looks
as if it passed through the shield. Because the weapon really fires, its own effect code, sound and
muzzle flash all run.

The drone's copy of the weapon is built as an instant single shot: no charge-up, a burst of one, and a
long cooldown that is reset whenever the drone is reused. A Pulse Laser or Ion Pulser therefore refracts
as exactly one bolt with no delay. The drone copies the ship's weapon stat modifiers (damage, range,
projectile speed) before each shot, and a flat range bonus stretches its range to match the firing
weapon's, including range from slot-based effects. Each ship keeps a pool of up to 8 drones, reusing an
idle drone with the same weapon. A drone that stays idle for 3 seconds is removed.

Right after firing, the drone tags its shot with the chain state (the ships already hit, the number of
refractions so far and its damage multiplier) and hands the shot back to the firing ship. From then on
the shot behaves like one of the ship's own: it can't hit the ship that fired it, the ship's own on-hit
effects apply (Energy Weapon Mastery included), and kills are credited to it. A shot that hits in the
same frame it was fired is handled by the drone instead, using the drone's copies of the ship's
damage-dealt listeners.

`NON_BEAM_ENERGY_WEAPON_CHAIN_FALLOFF_PERCENT` makes each refraction weaker, but only in terms of damage
dealt. On-hit effects run at full strength and EMP has no falloff at all. To achieve that, the falloff
lowers the hit's base damage and restores it immediately afterwards, rather than using a damage
modifier, because damage modifiers would scale the EMP as well.

## Temporary nodes

A node with `temporaryAfterDeploymentSeconds` only applies its effects for that many seconds after the
ship deploys, and its tooltip states the duration. Only effects that opt in can be used this way: stats
the game keeps reading during combat. That covers movement, flux capacity and dissipation, peak CR time and
CR loss, damage-taken multipliers, combat repair times, fighter refit and relaunch time, and weapon damage, range, flux
cost, fire rate, projectile speed and recoil. Stats fixed when the ship is built (hull, armor, shield arc,
fighter bays, weapon ammo), campaign stats, combat listeners and conditional effects can't be. If any other
effect appears on a temporary node, the loader logs an error and ignores the duration.

## Phantom hull mods

Many nodes recreate a vanilla hull mod (Safety Overrides, Hardened Shields and so on) with the mod's own
effects. Other mods only check whether a ship has the real hull mod (`variant.hasHullMod`), so without
help they would miss these nodes. Examples are Ship Mastery System masteries and Second-in-Command skills.
Each such node therefore also places the real hull mod on the ship as a phantom. It's a permanent hull
mod that costs no OP, carries the tag `exiledSector_installed_<id>`, and does nothing on its own, because
the node already applies the effects.

At startup, `PhantomHullMods.install` points each of those hull mods' effect class at
`PhantomHullModEffect`. That wrapper keeps the original vanilla effect and forwards everything to it,
except on ships where the tree placed the copy. There it applies nothing, can't be removed, and its
tooltip names the node that provides it. A copy installed the normal way still works as usual.
Phantoms are hidden from the refit screen's installed hull mod list and from the ship's Codex related
entries. Hull mods with fleet-wide effects (High Resolution Sensors, Phase Field) can't be wrapped and
so are never phantoms. Phantoms for another mod's hull mods only activate when that mod is installed.

## Phantom hull mod conflicts

Because a phantom is the real hull mod, other mods' rules about that hull mod apply to it. Three layers
keep a node and a hull mod that can't coexist from both ending up on a ship. First, a node's
`exclusiveHullMods` list names known conflicts: the node is refused while the ship has one, and if one
turns up later while the node is allocated, `HullModConflictResolver` removes it with the skill conflict
warning. Second, most conflicting hull mods refuse themselves in `isApplicableToShip` when they see the
other hull mod, and they see the phantom, so the refit screen greys them out with their own reason.
Third, a hull mod that tries to remove the phantom through MagicLib's `MagicIncompatibleHullmods` leaves
a `[removed, cause]` record; the resolver reads it, removes the cause instead and keeps the phantom.

That still misses hull mods that can't be removed, mostly hidden hull mods built into one mod's hulls
(Cetan Frame, Necro Scavenger and others). MagicLib keeps no list of incompatibilities to read ahead of
time; each mod calls it from its own hull mod code while the ship is built. So these conflicts are
learned the first time they show up. `PhantomConflictWatch.inspect` runs in both stats passes of
`SkillTreeHullMod`, which is kept last, so every other hull mod has already run, and in the after
creation pass it runs before the resolver clears MagicLib's warning. It learns from two signals.

The first is a removal attempt. The engine's `removeMod` ignores permanent hull mods and MagicLib only
calls `removeMod`, so these mods never actually take the phantom off, and the ship would keep a hull mod
its hull forbids. When MagicLib's warning hull mod is on the ship and its record names one of the node's
phantoms and a cause that is on the ship and can't be removed (built in, permanent or an S-mod), that
hull mod is learned. The record is only read while the warning is present, because asking MagicLib about
a ship it has no record for clears its records for every ship. A record with no named cause teaches
nothing.

The second is a real strip. A few mods take hull mods off the list directly (Emergent Threats'
Amorphous Alloy calls `getHullMods().remove`), so a phantom can go missing while its tag stays. That is
learned for the exact hull id (the skin, not the base hull) without guessing a culprit, because whoever
does it, the node can't work on that hull.

`LearnedPhantomConflicts` keeps what was learned in `exiledSector_learnedHullModConflicts.json` in
Starsector's shared common folder, so a conflict found once applies to every save. It is read once at
startup and written only when something new is learned; entries for mods that aren't loaded are kept
and simply never match. From then on `ShipTreeSync` stops placing that phantom on ships with the
conflict (a learned hull mod only counts where it can't be removed; an ordinary removable copy on
another hull is left to the resolver, which removes it with the warning), and `NodeEligibility`
refuses the node there with `LEARNED_CONFLICT`, naming the hull mod or the hull's built-in systems.
The NPC tree builder sees learned conflicts the same way.

On a player ship the node that placed the phantom is also removed, together with every node that
depended on it (`RespecPlan`), and refunded the same way a respec would refund it, with a campaign
message naming the cause. The removal is queued rather than done inside the hull mod callback, both
when a conflict is learned and whenever `ShipTreeSync` finds a learned conflict holding back one of the
ship's phantoms, so a removal lost to a reload is queued again on the next sync. The queue is cleared
on every game load and applied by the open tree panel, when the panel next opens, or by
`SkillTreeInstaller` once the game is unpaused. Each removal is checked again when it is applied: the
ship must still be in the player fleet and still have the conflict, so a sold ship is never refunded and
removing the conflicting hull mod in time cancels it. NPC ships help teach the table but are never
changed. None of this polls: detection only reads the variant during stats passes the game already
runs, the tree check is one lookup per node per snapshot and is skipped entirely while nothing has been
learned, and the queue costs a single flag read per frame.

A conflict that only shows up after the ship is created is found when the refit preview rebuilds after
the node is allocated, so the first allocation on such a hull is refunded at once and the node is
refused there afterwards. Nodes whose conflicting mod names no cause, like Necro Scavenger, rely on the
`exclusiveHullMods` list instead. A learned entry is never removed, even if a later version of that mod
drops the conflict.

## Generated NPC trees

NPC trees are generated per ship by `NpcSkillTreeBuilder.generate`, seeded from the sector seed, fleet id
and member id, so a ship always gets the same tree. The result is stored as a variant tag (`NpcTreeTag`);
the build name shown in the inspector comes from the two most common theme tags among its nodes.

The hull's design type (`getManufacturer`, falling back to the base hull's for skins) picks the Low Tech,
Midline or High Tech root, and any other design type picks one at random.

Removable hull mods with an equivalent node are stripped first. Their nodes are taken nearest first, and the
stripped OP becomes extra nodes at the per-node OP cost. A hull mod whose node is out of reach is put back
and its OP is never counted, so the budget can't be overspent. While this happens the ship is judged as if
the stripped hull mods were already gone, so Converted Hangar's own bay doesn't stop it taking the
no-fighter-bays node that replaces it.

Next, `npc_faction_volumes.csv` maps the fleet's faction to one of the seven vanilla faction volumes. If it
has one, the ship takes one notable or keystone there, weighted by relevance and tier, when the budget
reaches it. It gets there through an implicit wormhole anchor for its volume (see "Wormhole anchors"): the
nearest standard socket it can afford is treated as holding that anchor. The ship pays for the socket's path
and its node slot like a player would, and the volume's exit then opens for free. If the ship already has an
allocated socket that holds no item, that socket takes the anchor at no cost. No item is created and nothing
drops from it. The socket holding the implicit anchor is never claimed for a rolled item, so
`NpcFleetLeveller.decide` never fills it. A faction without a volume gets no anchor, and an exit whose skill
type is locked for NPCs is never opened. The anchor is stored in the tree tag as an `anchors:` field
(`socket=exit`), so decoding the tag rebuilds the same link.

The rest of the budget goes to goals. Notables and keystones are drawn by seeded weighted random
(relevance × tier) until their path lengths cover the remaining budget, and the ship then takes the
nearest affordable goal, path and all, one at a time. Small nodes are only taken as path steps until no
notable or keystone fits, and then they fill whatever is left.

Every step is a breadth-first search from the allocated nodes. A node is traversable only if the ship can
use it: requirement tags, exclusivity and hull size are checked through `NodeEligibility`, and optional
nodes pick an option the ship can use. Wormhole nodes are never walked into: the core shortcuts are not
crossed at all, and an exit is only reached by jumping from the socket that holds the ship's implicit anchor.

When the player recovers an NPC ship, its tree becomes the player's, but an implicit anchor is not an item the
player owns. `ShipTreeSync` drops the implicit anchor while adopting the tree and releases every node it
reached, so the recovered ship keeps no volume it has no anchor for. The empty socket stays allocated.

`NpcRelevance` weights theme tags: matching weapon kinds, fighter bays, shields, phase cloaks and heavy armour
lift their themes, and so do the themes of nodes that own the ship's built-in hull mods.

## Renamed skill effects

Socketables freeze their rolled effects by name, so renaming a skill effect would otherwise orphan every
item already rolled with it. `data/config/exiledSector/effect_aliases.csv` (columns `alias,effect`) maps an
old name to its current one, and every lookup of an effect by name falls back to it. A row is skipped if
its target is not a skill effect, or if the alias is still an effect of its own. The file is merged across
mods like the other CSVs. A socketable pool entry naming an effect that is neither current nor aliased is
skipped with a warning; the rest of the definition still loads.

## Subroutines

Subroutines are the socketables that go in tree sockets. They never roll prefixes or suffixes: a row in
`data/config/exiledSector/subroutines.csv` has no pool columns, and no row of `item_modifiers.csv` may list
`subroutine` in its `item_types` (`ItemDataIntegrity` reports it). What a subroutine does comes from its
`kind` column, one of `MODSPEC_CACHE`, `DOCTRINE`, `BYPASS` or `UNIQUE`, read into `SubroutineKind` on the
definition and never stored on the item. A row with a missing or unknown kind is skipped with an error in
the log. `UNIQUE`, `MODSPEC_CACHE` (see "Modspec caches" below) and `DOCTRINE` (see "Doctrine Archives") are
built. `BYPASS` is parked: the kind exists but no row uses it, and its design and the code already built
for it are in `docs/plans/bypass_subroutine.md`.

Only some kinds take part in the generic item pools: the drop pickers in `ItemDrops` (salvage sites, battle
loot, chart loot, tech-mining and the targeted subroutine bonus) and the subroutines NPC trees put in their
sockets (`NpcItems.pickDefinition`). `SubroutineKind.inGenericPools` decides it per kind, and
`ItemDrops.isDroppable` applies it to every picker at once. `MODSPEC_CACHE`, `DOCTRINE` and `BYPASS` are left out,
so NPCs never socket them and nothing drops them by chance; each gets a drop path of its own (modspec
caches have theirs, see "Modspec caches"). Only `UNIQUE` stays in, and unique subroutines are still
limited to their own sources. Since no non-unique kind is in the generic pools, the general salvage roll's
basic pick and an NPC's basic socket roll currently find no subroutine at all.

Unique subroutines are hand-made rows with `kind` set to `UNIQUE` and `unique` set to `true`; a `UNIQUE` row
without the flag is skipped. They are the one subroutine kind that carries plain skill effects. Their `lines`
column lists those effects, either written inline (`HULL_MULT:5:8`) or as `@ID` references to rows of
`item_modifiers.csv` whose `item_types` include `unique_subroutine`. That scope exists only for these lines,
so a modifier meant for a unique subroutine has to opt in, and its slot must be `prefix`. The plain
`subroutine` scope is still an error. Lines carry no weight or tier range, because every line always rolls,
once. A line is either a fixed value (`min` equal to `max`) or a small range rolled when the item is created
and frozen on it like any other socketable roll. A rebalance of the row changes new drops only. Other kinds
may not list lines. Internally the lines load as the definition's prefixes, so they reach the ship the same
way every socketed item's rolls do: `AllocatedSkillEffects` adds them while the subroutine sits in an
allocated tree socket, and drops them when the socket is unallocated or the item leaves it. The tooltip and
the codex list them like a unique hull item's effects.

No currency works on a unique subroutine. `ItemCrafting.canUse` refuses every currency, including ones other
mods register, without the item being corrupted, and the workbench says why. In particular Tuning
Nanoswarms do not reroll their frozen values (an open decision in the plan; the current answer is no). A
corrupted or transposed unique hull item never turns into a unique subroutine, since transposition stays
within the item's type and skips unique subroutines.

Each unique subroutine names where it comes from in its `source` column, as `faction:FACTION_ID` or
`site:SALVAGE_SITE_ID`, several separated by `;`. A source restricts the existing unique drop paths rather
than adding new ones. `faction:` uniques can only be drawn by `NpcItems.pickDefinition` for a fleet of that
faction, which is the small unique share of the items a levelled NPC flagship carries in its tree sockets;
the fleet is flagged for the sensor alert, and destroying or recovering the ship drops the item as usual.
`site:` uniques can only be drawn by the unique roll of that `item_salvage.csv` site, so the site needs a
`uniqueChance` above zero. Unique hull items have no source and still drop from any site with a unique roll.
`ItemDrops.pickUnique`, which transposition uses, never draws a unique with a source. Like every unique, they
only drop from player level 15 and after their `unlock` condition, if they have one. `ItemDataIntegrity`
reports a unique subroutine with no lines, no source or a malformed source.

A source can still name something that never drops anything. After the item data and `item_salvage.csv`
have loaded, `UniqueSourceCheck` logs a warning for every `faction:` source whose id is not one of the
faction specs the game loaded (`SettingsAPI.getAllFactionSpecs`, so a modded faction counts while its mod is
active), and for every `site:` source with no row in `item_salvage.csv` or a compat salvage file, or whose
row has a `uniqueChance` of zero. The chart sites such as `exiledSector_chart_warden` are rows of
`item_salvage.csv` like any other. These are warnings only; the definition still loads. If no faction specs
are available the faction check is skipped.

The four domain subroutines (`domain_subroutine_military`, `_industrial`, `_consumer` and `_management`)
were deleted with no compensation. Their ids are listed in `DeletedSubroutines.DELETED_DEFINITION_IDS`, and
only those ids are ever removed, checked after `item_id_aliases.csv` and only while no definition uses the id
again. `DeletedSubroutines.removeFromSave` runs once from `onGameLoad`. It removes those items from item
storage whatever class they loaded as (0.8.0 saved every item as `exiledSector.Socketable`), unsockets them
from every ship's tree sockets including `npc:` ids left in recovered ships, and removes their stacks from the
player fleet's cargo and every submarket of every market, the same cargo list `MaterialMigration` converts.
It logs one line with the counts and is skipped if no item definitions loaded at all. Decoding an NPC tree tag
also drops a socketed retired id.

Any other item with a missing definition, a subroutine included, loads as an `UnresolvedItem`, keeps its
rolls and its socket, and comes back once the definition loads again. So a broken `subroutines.csv` row or a
mod that fails to load never deletes anything. Retiring another subroutine later means adding its id to
`DELETED_DEFINITION_IDS`.

Modspec caches and Bypass subroutines are born corrupted (`SubroutineKind.bornCorrupted`). Every way a subroutine
comes into being ends in the `Subroutine` constructors or its `readResolve`: rolling a drop, a console grant,
claiming or previewing an `npc:<definition>/<seed>` id, absorbing a cargo stack, a transposition, and an
`UnresolvedItem` turning back into a subroutine. There, an item of a born-corrupted kind with no corruption
yet gets `CorruptionOutcome.INNATE`. That counts as corrupted everywhere, so every currency refuses it
exactly as it refuses an item a Corrupted Nanoswarm has touched, and its frame takes the corruption colour.
A Corrupted Nanoswarm only ever rolls the other four outcomes (`CorruptionOutcome.NANOSWARM_OUTCOMES`). The
tooltip and the workbench show plain "Corrupted" for every outcome, `INNATE` included.

Modspec caches and Bypass subroutines also roll tree nodes when they are created (`SubroutineKind.rollsNode`).
The candidates come from `SubroutineNodePools`, where each kind plugs in a function from the definition to
its candidate node ids, and optionally a node count rolled from the seed (one by default). Modspec caches register
theirs (every notable of the tree and a count from their chances, see "Modspec caches"). Bypass is parked and registers no pool, so a Bypass item would roll nothing. The pick is a pure function of the candidates and the
item's seed: the ids are trimmed, de-duplicated and sorted, then the nodes are chosen with a `SplittableRandom`
seeded from the item seed and a fixed salt. So the same seed always lands on the same nodes, and an NPC's
`npc:` id decodes to the node the player gets when the ship is recovered. The chosen ids are stored in the
`rolledNodeIds` field of `Subroutine` and never rolled again, so a later change to the tree or the pool
cannot reroll an existing item. It is copied by mirroring and by every conversion, and an `UnresolvedItem`
keeps it while the definition is missing. If the kind has no pool, the pool is empty or it throws, the field
stays empty and nothing crashes; an item loaded with an empty field tries again on the next load. A
subroutine in cargo carries only its seed, so it takes its nodes when it is absorbed into item storage. The
tooltip names the nodes through the tree's node and type names, and a stored id that is no longer in the
tree shows as an unknown node.

A subroutine fits a tree socket only if its definition has a kind. Doctrine and Bypass subroutines are
limited to one per ship and modspec caches and Unique ones are not (`SubroutineKind.socketLimitPerShip`).
`SubroutineSocketLimits` counts the other subroutines of the same kind already in the ship's tree sockets,
leaving out the socket being filled and the item itself, so swapping one for another in the same socket is
allowed. Installing from the subroutine storage panel checks it and shows the refusal in the panel's notice
line until the next install attempt, a successful install, or a change of selected item or targeted socket.
NPC trees skip a socket roll that would break it, and if a transposition turns a socketed subroutine into a
kind the ship already has its fill of, `SocketCustody.unsocketIfMisfit` unsockets it.

A definition can also carry its own limit in the `socket_limit` column of `subroutines.csv`: the most copies
of that definition one ship can have in its tree sockets. Blank or 0 means no limit, and anything other than
a whole number of 0 or more skips the row with an error (`ItemDataIntegrity` reports it too). Every current
unique subroutine has a limit of 1. The definition limit is checked in the same places and the same way as
the kind limit: it counts the other socketed items with the same definition id, leaving out the socket being
filled and the item itself, so moving a copy or swapping one copy for another in the same socket is allowed.
`npc:` items count through their definition, so a fleet never rolls more copies than the limit. Installing a
copy past the limit is refused with its own message naming the item; when an item is over both limits, the
kind refusal is shown.

A subroutine whose definition is missing loads as an `UnresolvedItem` and counts towards no kind, so a ship
can end up over a limit once the definition returns. `SocketTypeMigration.run`, called from `onGameLoad`,
therefore also runs `SubroutineSocketLimits.unsocketExtras`. For each ship it walks the sockets in order of
socket node id, keeps the first subroutines of each limited kind and of each limited definition up to their
limits, and unsockets the rest. An item unsocketed for one limit does not count towards the other. They stay in
item storage. One info line in the log names what was returned.

## Modspec caches

A modspec cache is a subroutine of kind `MODSPEC_CACHE`. The one shipped row is the Contraband Modspec Cache
(`contraband_modspec_cache`), a unique-rarity subroutine: a smuggler's cache of cracked modspecs from every
faction. It is corrupted from the start and rolls one, two or three notables from anywhere in the tree when it
is created. While it sits in an allocated standard socket, those notables are allocated on the ship without a
path to them, and they cost nothing: no node slot, no OP, no banked free allocation and no item. The socket
the player allocated is the price. It fits any standard socket and several can sit on one ship. NPC trees never
socket one.

The row has `unique` set to `true`, so it shows in the unique colour and the codex treats it as a unique, but
its kind keeps it out of the generic pools (`SubroutineKind.inGenericPools`), so no generic unique roll, NPC
socket roll or transposition ever picks it; it drops only through its own paths (see "Drops" below). Unlike
other uniques it has no player level gate.

### Definitions and the node pool

Four columns of `subroutines.csv` are only for this kind: `excluded_nodes` lists notable node ids that never
roll (separated by `;`), `node_count_chances` holds the relative chances of rolling one, two, three or more
nodes (`0.9;0.09;0.01` today, so 90%, 9% and 1%), and `drop_chance` and `salvage_chance` set its drops.
`ModspecCaches` reads them after the item definitions load. A row whose `node_count_chances` is not a list of
numbers of 0 or more with at least one above 0 is logged and rolls nothing, and so is a `MODSPEC_CACHE`
definition with no usable row. Nothing is excluded today.

The candidates are the active tree's notables (tier `NOTABLE`, type not optional, since a grant cannot choose
an option) in every region, minus the excluded ids. Only active nodes are used, so a node whose area is off
never rolls. The node count is rolled from the item seed with its own salt, then that many distinct
candidates are picked from the seed by `SubroutineNodePools` (a seeded partial shuffle of the sorted
candidates), capped at the number of candidates. The rolled ids are frozen on the item as described under
"Subroutines". If a frozen node is later renamed through `node_replacements.csv`, the grant and the tooltips
follow the replacement (`Subroutine.currentNodeIds`).

`ItemDataIntegrity.checkModspecCaches` checks at startup that each `MODSPEC_CACHE` row has valid node count
chances, that every excluded id is a notable in the tree, that something is left to roll, and that both drop
chances are numbers of 0 or more. It also reports the cache columns filled on another kind and warns about a
row that can never drop.

### The grant

The skills side never reads item storage. `NodeGrants` holds a lookup from a socketed item id to the nodes it
grants, which `NodeGrantLinks` installs at startup; it returns the rolled nodes of a modspec cache and nothing
for `npc:` ids. `ShipSkillData.grantedNodesBySocket` maps every allocated socket that holds such an item to its
nodes. It is cached against the ship's data revision and the lookup's revision, which is bumped on load and
whenever the tree opens, so nothing is looked up per frame.

When the tree opens, after every allocation change and after socketing, `NodeAllocator.grantSocketedNodes`
first releases granted nodes whose grant has gone (see "Removal"), then allocates each linked notable that is
not allocated yet, through `AllocationGate.grant`. That verdict skips the path check, the node cap and the OP
check, and ignores an item cost, but keeps the rest of `NodeEligibility` with the ship's facts, so hull size,
hull and ship requirements, hull mod and learned conflicts, exclusivity, effect blocks, locked types and gate
tags all apply. A notable that cannot be granted stays unallocated and is tried again at the next change.

`ShipSkillData.allocateGranted` allocates the node and records it in the persisted `grantFreeNodeIds` set.
A node in that set counts as free (`isFreeNode`), so it is left out of the spent OP, and it is left out of
`usedNodeSlots`, which every node cap check uses instead of the allocated count: `AllocationGate`, opening an
anchor's wormhole, waking dormant nodes, the template step budget, the nodes bar and the template bar. It
never consumes or returns a banked free allocation and no item is charged for it.

A granted notable counts as reachable, so no check releases it or refuses because of it, but it is never a
path seed. `ShipSkillData.reachableAllocatedNodeIds` adds it after the path search, without searching onwards
from it, and only while its socket is itself reachable. Every reachability and stranding check goes through
that search: `canDeallocate`, `releaseNewlyStranded` and `previewRelease`, `releaseUnreachable` (used by
`WormholeAnchorMigration` and when a recovered NPC tree is adopted), `RespecPlan`, and the planning copy of
`TemplatePlanner`, which mirrors the live sockets and the free set. `AllocationGate.isConnected` does not count
a granted notable as a connection, so the nodes beyond it still need a real path; otherwise a cache would
quietly work like a Bypass subroutine. `wakeDormantNodes` follows the same rule: it first wakes dormant nodes
whose neighbours are allocated, to a fixed point, without counting a notable that only a grant holds as a
neighbour and without waking a free node whose grant has gone, and only then wakes, in a last pass that
cascades no further, the dormant notables whose grant still holds them; those need no node slot. So when an
area comes back, a granted notable returns but the nodes behind it return only if a real path (or an open
anchor) reaches them, and the rest are released as usual. Templates captured from the ship leave out granted
notables.

### Removal

A granted node is free only while its cache grants it. Removing the cache, replacing it, unallocating its
socket or stranding the socket releases every node in `grantFreeNodeIds` that no allocated socket grants any
more, whether or not a path also reaches it, and then anything that only that node held up. `releaseUnreachable`
does this before its reachability pass, so `releaseNewlyStranded`, `previewRelease` and the refresh sweep
(`releaseUngrantedFreeNodes`) all agree. A socket that went dormant with its area still holds its grant, so turning an area off and on never
releases the nodes its cache granted. Releasing a granted node returns nothing, since it cost nothing, so
removing a cache asks no confirmation unless paid nodes that depended on a granted node would go too; then the
socket uses the arm-and-repeat confirmation with `socketable.cache.releaseConfirm`, naming how many nodes go and
the OP they return.

While the grant alone holds a notable, it cannot be unallocated on its own
(`AllocationGate.Refusal.HELD_BY_SUBROUTINE`), since the next refresh would grant it again; its tooltip names
the cache. A granted notable that a path also reaches can be unallocated, and the cache grants it again, free,
at the next refresh. A respec of the socket removes the socket first and the notables after it.
`SocketCustody.reconcile`, which unsockets the items of a ship that has left the player's custody, also releases
the free nodes those items granted, any node only a grant held, and anything stranded by those releases, and
refunds item charges of released paid nodes to the player's cargo. It leaves an anchor's volume alone as before.
A ship destroyed or disabled in battle and not recovered gets no release or refund: its whole tree is forgotten.

If the ship already has a notable allocated and paid for (by path) when the cache is socketed, the cache does
nothing for that node and its tooltip says so; it is not made free, and removing the cache leaves it. If two
caches on one ship roll the same notable, the one in the socket with the lowest node id is the active one and
the other idles; removing either keeps the notable as long as one is left.

A granted notable inside a Doctrine Archive's radius is rewritten or augmented like any other allocated node
in range, since the doctrine acts on whatever nodes are allocated there however they got there. A transformed
notable shows its replacement, and the grant is still keyed on the node, so removing either item behaves as
described for that item.

### Display

The item tooltip and the socket's tree tooltip show one "Allocates X" line per rolled node and nothing else
about the grant. A node with a problem gets it in brackets on its line: idle because the ship already has it
allocated and paid for, idle because another cache on the ship allocates it, inactive (followed by the
eligibility reason on its own line), inert because its area is off, or an unknown node that left the tree. In a
socket the states come from the tree's allocation snapshot (`Snapshot.nodeGrantStatuses`, cached per
snapshot). The item tooltip shows the last states the tree computed for the ship holding it
(`NodeGrantStatus`, a transient cache cleared on load and forgotten when the item leaves custody), or only the
area and unknown-node cases when it is not socketed. A cache can be socketed even when some or all of its nodes
are inert; a node whose area goes off goes dormant with the area, stays free, and wakes through the grant when
it returns. A granted node's own tooltip adds "Allocated by" with the cache's name.

### Drops

The cache drops rarely, from two data-driven paths, with no change to the generic pools. `drop_chance`
registers it with `FactionFleetDrops` under its own source group (`modspec_cache`) as an any-faction source:
after a won battle it rolls at `drop_chance` per 100 deployment points of destroyed or disabled ships of any
faction, capped at one in two. `salvage_chance` adds a roll to every item roll of a salvage site
(`ItemDrops.roll`, which also serves tech mining, chart fleet loot and chart rewards): the chance is
`salvage_chance` times the site's first item chance times the chart's item multiplier. Rates are placeholders.

For testing, the console command `ExiledSectorGrantModspecCache [nodes]` adds one cache to the player's cargo
with a random seed, or with a seed that rolls the given number of nodes.

## Wormhole anchors

A wormhole anchor is a socketable of its own type (`ItemType.WORMHOLE_ANCHOR`, class `WormholeAnchor`, saved
under the XStream alias `exiledSector.WormholeAnchor`). It is not a subroutine. Every faction volume is reached
only through its anchor: the tree has no fixed wormhole from the core into any volume any more.

### Exits

Each volume has one exit node, the end a wormhole opens onto. Exits have their own tier, `WORMHOLE_EXIT`, and
the twelve faction exits use the `wormhole_exit` skill type; the Exoship's own type has that tier too. A tier
rather than a flag on the wormhole tier keeps the rule in one place: everything that asks what kind of node it
is gets the answer from the type, and drawing code that treats both alike asks `SkillTier.isWormhole()`. An
exit has no `pairedWith`. `WormholePairValidator` and the loader only pair `WORMHOLE` nodes, so an exit with no
partner is valid, and the editor reports an exit that was given one.

An exit is never allocated by a path. `AllocationGate` refuses it as not connected whatever its neighbours,
and the reachability search in `ShipSkillData` never enters an exit along a connection, only by jumping from
the socket whose anchor opens it. A volume node next to an exit is therefore unreachable unless the exit was
opened.

The three core shortcuts (`hightech_wormhole_1`/`midline_wormhole_1`, `hightech_wormhole_2`/`lowtech_wormhole_2`
and `lowtech_wormhole_1`/`wormhole_6`) are ordinary `WORMHOLE` pairs and work as before. The core ends of the old
faction wormholes, the faction-side ends of the faction-to-faction legs and the `exotech_beacon` node were
removed. None of them held other nodes together. The layout orbit anchor they sat on is now called
`core_ring_anchor`, so searches for `wormhole_anchor` only find the item.

### Opening and travelling

The partner of a node is a per-ship lookup, `ShipSkillData.wormholePartner`. It returns the static pairing of a
core shortcut, or the socket-to-exit and exit-to-socket link of a socketed anchor. The items side installs the
lookup from an item id to the exit it may open (`WormholeAnchorLinks`, behind `AnchorLinks.ExitLookup`), so the
skills package never reads item storage itself. A socket and an exit are linked while the socket is allocated
and holds an anchor whose exit is in the active tree. Whether the anchor may open is a separate question, asked
only when opening: its `unlock` must be met, the exit's `gate_*` tags must be met and the exit's own type must be
unlocked. Keeping the two apart means a condition that reads false for a moment, for example RAT's Exoship
state failing to load, never breaks the link of a wormhole that is already open. The links are cached per ship
and rebuilt only when the ship's data revision or the lookup's revision changes. Socketing and unsocketing bump
the data revision, and the lookup's revision is bumped on load and whenever the tree opens.
Every caller reads the same links: the live data for clicks, jumps and deallocation feedback, the drawn snapshot
for ghost flights and hidden checks, and `TemplatePlanner`'s planning copy, whose socketed items are mirrored
from the live ship and whose revision is bumped when they are.

An open wormhole has its exit allocated. Opening allocates the exit at no OP cost, as a free partner, but it
takes a node slot under the allocated-node cap. `ShipSkillData.openAnchorWormholes` opens every linked exit that
fits and whose anchor may open. It runs when an anchor is socketed, when the tree opens and after every
allocation change in the tree, so an anchor that was waiting for a slot or a gate opens as soon as it can. It
only ever opens; it never closes or releases anything. If two anchors on one ship lead to the same exit, only the
one in the socket with the lowest node id links; the other stays inert.

Ctrl clicking a socket with an open anchor jumps the view to its exit, and ctrl clicking the exit jumps back,
through the same `wormholeJumpTarget` as the core shortcuts. An anchor link only jumps while both ends are
allocated, so ctrl clicking a socket whose wormhole is still closed opens the storage panel for that socket as
usual. For the Hyper-Nav Beacon the jump target is the
Exoship, so ctrl clicking the socket boards it, and ctrl clicking the Exoship returns to the socket. The
hyperspace overlay draws a route for each open anchor wormhole from the core to the volume, alongside the core
shortcuts, and the ghost flights fly along open anchor wormholes too.

A socket linked to a faction exit draws as a wormhole. `SocketWormholes` eases each such socket from socket to
wormhole over 3 seconds once the link exists (the socket is allocated and holds the anchor), and back over 3
seconds once the link goes away: the frame, anchor icon and arcs shrink and fade while the exit's wormhole art
grows in, in the exit's `wormholeColor`. The wormhole shows closed until the exit is allocated, then opens over
`WormholeOpenness.OPEN_SECONDS` like any other wormhole, and connector ends follow the same blend. Sockets
already linked when the tree opens start as wormholes. The Hyper-Nav Beacon's socket stays a socket, because its
exit is the Exoship, and so does an inert duplicate anchor, which has no link. The connector line styling at the
socket end stays the socket's.

The socket's tooltip shows the anchor's volume and whether its wormhole is open, closed for want of a free node
slot, inert because another anchor on the ship already opens that volume, or inert and why: its area is off, its
unlock is unmet (for the beacon, until the Exoship is recovered or owned), or its exit's gate is unmet. An open
wormhole shows as open even if one of those conditions reads false later. Each gate has its own line
(`socketable.anchor.gate.<gate tag>`), with a general line for any other locked exit.

### One anchor per volume

A ship can hold one anchor per volume. `WormholeAnchorSocketLimits.refusal` refuses a second anchor for a volume
already anchored in another socket of the ship, leaving out the socket being filled and the item itself, so
swapping anchors in one socket is allowed. The refusal (`socketable.anchor.volumeLimit`) shows in the storage
panel in both the place-on-socket and targeted-socket flows, the same way the subroutine limits do.
`SocketTypeMigration.run` also runs `WormholeAnchorSocketLimits.unsocketExtras` on load: for each ship it keeps
the first anchor per volume in order of socket node id and returns the rest to item storage.

### Closing the wormhole

Unallocating the socket, removing the anchor from it, replacing it with another item, or unallocating the exit
closes the wormhole. Unallocating the exit unallocates its socket as well, the way either end of a pair always
took the other, and the anchor goes back to item storage. Every allocated node that the change strands is
released in the same step: the nodes that were reachable from the starting root before it and are not after it.
That is the same reachability search the deallocation check uses, so nodes still reachable some other way stay,
and nodes that were already unreachable before the change are left alone. `NodeAllocator` takes the reachable set
before each deallocation, socketing and unsocketing and calls `ShipSkillData.releaseNewlyStranded` after it.
Released nodes are refunded like an ordinary unallocation: the OP is free again, item charges are returned through
the ledger and refunded to the cargo, and free allocations go back to the bank.

Closing is always allowed. `canDeallocate` still refuses a deallocation that would strand nodes, except for the
nodes that only the wormhole being closed reaches. Before the tree closes a wormhole that would release anything
besides the socket and its exit, it asks first: the first click (or right click, or install over the socket)
plays the refusal sound and shows `socketable.anchor.closeConfirm` in the socket's tooltip, and in the storage
panel's notice line for an install, or a right click while the panel is open, naming how many nodes would be released and the OP they return. Doing the
same thing again within a few seconds confirms it. The preview is `ShipSkillData.previewRelease`, which applies
the change to a copy of the ship and runs the release there. The confirmation is generic over socket changes, so
a later radius subroutine whose removal strands nodes gets the same warning by going through the same calls.
Modspec caches already do (see "Modspec caches"). A notable granted by a modspec cache is never
released by closing a wormhole, even inside the volume being closed, because the reachability search counts it
through its grant rather than through a path.

Nothing else releases nodes. An anchor that reads inert, waits for a slot, or whose area is off leaves the
volume's allocated nodes alone. When a ship leaves the player's custody, `SocketCustody.reconcile` unsockets its
items as before but releases nothing of an anchor's volume, because a ship can be out of reach only for a while,
for example in a Nexerelin special task group's fleet that has not spawned yet. It only releases the notables
granted by the modspec caches it unsockets (see "Modspec caches"), and not for a ship lost in battle. A ship lost in battle and not recovered has its
whole tree forgotten. When an area is turned off, `forgetUnknownNodes` makes its exit and volume nodes dormant
rather than releasing them, and they wake through the anchor's link when the area returns.

### Old saves

Saves from before anchors have faction volume nodes allocated through wormholes that no longer exist. On load,
`forgetUnknownNodes` first unallocates and refunds every removed node, including the old core ends and
`exotech_beacon`. The `node_replacements.csv` row that mapped `exotech_wormhole_2` to `exotech_beacon` was
removed, so that older id is forgotten the same way. Then `WormholeAnchorMigration` releases, on every ship, the
nodes outside the core that nothing reaches any more, refunds item charges for owned ships and logs one summary
line. It runs once per save: it records `exiledSector_wormholeAnchorMigrationDone` in the sector's persistent
data and never sweeps again, including on a new game, where it finds nothing. No anchor is given, not even into
storage: the player has to find one. The row mapping `exotech_wormhole_1` to `exoship` stays. Volume nodes that
were dormant because their area was off when the migration ran are dealt with when the area returns: their exit
has no anchor to wake through, so `wakeDormantNodes` releases them as it does any dormant node that cannot
reconnect.

### Definitions and drops

The definitions are in `data/config/exiledSector/wormhole_anchors.csv`, one row per tree volume, with the
columns `id`, `volume`, `exit_node`, `name`, `icon`, `color`, `crest_faction`, `rarity`, `description`, `unlock`,
`drop_factions` and `drop_chance`. `volume` is a tree region other than `core`, and `exit_node` is the volume's
exit (`exoship` for ExoTech). `color` and `crest_faction` style the icon (see "Anchor icons"). `WormholeAnchorDefinitions` loads the file after the star charts
and keeps the volume and exit of each anchor. It skips, with an error in the log, a row whose volume is not a
region, a row without an exit, a second row for a volume that already has an anchor, and an id that a
subroutine, hull item or star chart already uses. The definitions sit in their own registry, like star charts,
so `ItemDefinitions.all()` never lists them. `ofType(ItemType.WORMHOLE_ANCHOR)` does. Names and descriptions
are translated as `socketable.<id>.name` and `.description`, like every other item definition.

`ItemDataIntegrity.checkWormholeAnchors` runs at startup against the declared skill tree. It reports a
missing or repeated id, an id used by another item file, a bad or repeated volume, an exit that is not in the
tree, sits in another region or is not a `WORMHOLE_EXIT` node, a drop chance that is not a number of 0 or
more, and an unlock that `ItemUnlock` does not understand (an item unlock id, `cleared_chart_tier_N` or blank
are all accepted, the same set the runtime honours). A modifier row listing `wormhole_anchor` in its
`item_types` is an error too, since anchors roll nothing.

Anchors drop from battles, through `FactionFleetDrops`, a drop path keyed by item definition. Sources are
registered in named groups, so each kind of item replaces only its own: anchors register under their item type
and modspec caches under `modspec_cache`. Each anchor registers its volume, the extra faction ids in
`drop_factions` (separated by `;`) and its `drop_chance`. A defeated ship counts towards an anchor if its
fleet's faction maps to the anchor's volume in `npc_faction_volumes.csv`, or is one of its extra factions.
After a won battle, `ItemLootListener` sums the deployment points the player destroyed or disabled per faction
and rolls each anchor once: the chance is `drop_chance` per 100 qualifying deployment points, capped at one in
two. An anchor whose area is off never drops (`FactionFleetDrops.available`). The general drop and NPC pickers
in `ItemDrops` still skip anchors, so salvage, tech mining, chart loot and NPC sockets never produce one.

The volumes with no vanilla faction take their extra factions from the mods that add them: `kesteven` and
`enigma` from LOST_SECTOR, `nightcross` and `stargazer` from Nightcross, and the Abyssal factions of RAT for the
Abyss. The ExoTech anchor, the Hyper-Nav Beacon, drops rarely from `rat_exotech` fleets, and
`HyperNavBeaconGrant` gives the player one beacon, once per save, the first time its `rat_player_exoship` unlock
is met. It checks on load and on every economy tick, and remembers the grant in sector memory.

An anchor has no prefixes, suffixes or implicits and is never corrupted or mirrored: it ignores stored traits,
and its explicit and implicit lists are always empty. `ItemCrafting.canUse` refuses every currency on it, even
one a mod extends to the anchor type, so the workbench shows nothing usable. It disassembles into parts like any
other item.

`canSocketInto` accepts the standard tree socket (`SkillTier.SOCKET`), like a subroutine. Item storage lists
anchors with the subroutines when a tree socket is being filled, and the tree's storage button ("Socket
Items") counts both. Anchors have no `ItemKind`, so storage and workbench text that names an item's kind uses
`Item.kindName()`, which falls back to the definition's noun. An anchor whose area is off cannot be socketed:
`TreeAllocationSession.socketRefusal` returns the anchor's own refusal (`socketable.anchor.areaOffRefusal`),
which plays the refusal sound and shows in the storage panel in both the place-on-socket and targeted-socket
flows. A socketed anchor's tree tooltip shows the same volume and inert lines as its item tooltip, plus whether
its wormhole is open on this ship. Area gating reuses the area toggles: `AreaToggles` and `TreeRegionFilter` already drop a disabled
area's nodes from the active tree, so an anchor counts as active only while its exit node is in the active
tree (`SkillTree.get`). That covers Kesteven and Enigma (LOST_SECTOR), ExoTech and Abyss (RAT) and Nightcross
and Stargazer, whatever the setting or missing mod that turned them off. The definition stays loaded either
way, so an anchor the player owns is never lost when an area is turned off. Its tooltip names the volume it
leads to and says when it is inert because its area is off.

The `unlock` column on an anchor is a condition for use, not for dropping. The ExoTech anchor, the Hyper-Nav
Beacon, carries `rat_player_exoship`, an `ItemUnlock` that is met once RAT reports the Exoship recovered or
owned, and latches like the other item unlocks. It reads RAT through
`RandomAssortmentCompat.exoshipConfirmedRecovered`, which fails closed: if RAT's Exoship state can't be read,
the unlock is not met and nothing is latched. Until the unlock is met the beacon is inert and its tooltip says
so. The `exoship` skill type no longer carries the `ratPlayerExoship` unlock condition: the anchor holds it, and
its unlock latches in sector memory, so a failed read of RAT later can't lock the Exoship node itself. The
`RAT_PLAYER_EXOSHIP` condition type still exists for data that names it, and reads the same fail-closed check.

For testing, the console command `ExiledSectorGrantWormholeAnchors [volume]` adds one anchor per volume, or
only the named volume's, to the player's cargo.

### ExoTech and the Exoship

The Exoship is the ExoTech exit. It keeps its carried branches (`ExoshipNodes.isCarried`), its voyage, escorts
and boarding view, and the ghost-flight and connector rules for carried nodes. The beacon node is gone. The
Hyper-Nav Beacon item looks like every other anchor (see "Anchor icons"), in ExoTech gold with the ExoTech
crest. `AnimatedItemIcons` still draws `graphics/stations/buoy00.png` at 1.6 times the icon box with
`buoy00glow.png` pulsing orange over it, for any data that uses that path, but no anchor does any more.

### Anchor icons

Each anchor has its own icon and a cargo animation in its faction's colour. The `color` column (`#RRGGBB`) is the
faction colour and `crest_faction` names the faction whose crest is the badge. A blank colour falls back to the
anchor's default purple, a colour that is not `#RRGGBB` logs an error and does the same, and a blank crest
faction shows no badge. `ItemDataIntegrity` reports a bad colour. The anchor's `frameColor` is its faction colour,
so item storage frames it in that colour too.

The icons are baked, not tinted in game. `tools/WormholeAnchorIcons.java` takes vanilla's
`graphics/icons/cargo/wormhole_anchor.png`, recolours its purple glow (hue 265 to 345, ramping in with
saturation) to each row's colour while keeping the brightness, and writes
`graphics/icons/cargo/exiledSector_wormhole_anchor_<volume>.png`. Rerun it with `--overwrite` after changing a
colour. The baked icon is what the codex, tooltips, tree sockets and item storage show.

Wherever a `CargoIconLook` is drawn (cargo, the workbench and its tiles), `CargoIconLooks.forItem` gives an anchor
an anchor look, and `CargoIconFx.render` hands it to `AnchorIconFx` instead of the rarity brackets and glows. All
sizes are in an 80 by 80 design box scaled to the icon. When the look is framed, the icon is inset as framed
items are, a faction-coloured frame with a soft additive inner band surrounds it, and ten small lights
(`graphics/fx/glow64.png`) sit along each half of the frame, from the top-left corner to the bottom-right one. A
lit light grows, and slides inward just enough that its glow stays inside the icon box. The
crest of `crest_faction` (read from the faction spec, so mod crests are never copied) sits in a circle in the
bottom-right corner, drawn as a textured fan with a dark rim, plus a faction-coloured ring when framed. A faction
that isn't loaded shows no badge.

With item animations on Full, each icon runs a six-second cycle, offset per stack so a cargo grid never pulses in
step:

- Behind the icon, a tunnel of vanilla's `graphics/fx/radial_fx.png` is mapped around the gem (texture u around
  the circle, v as 1/radius plus a scroll), so streaks stream outward while the mesh slowly spins. It fades at
  the centre and the rim, and its radius is the gem's distance to the nearest box edge, so it never leaves the
  icon. It brightens while the aperture is open.
- 0 to 22% of the cycle, four chevrons spiral in onto the gem's diagonals and close on it, then blink twice.
  They start no further out than the gem's distance to the nearest edge allows.
- 30 to 78%, the gem opens: a dark core with a `star_halo.png` rim in the faction colour.
- 34 to 78%, three `shields256ringd.png` rings fall into the aperture one after another, spinning.
- From about 30%, a light wave runs along both halves of the frame and meets at the crest at the very end of
  the cycle, where the badge flashes, just before the next lock-on.

On Calm or Off the icon is still: frame, dim lights and badge, no tunnel, aperture or motion. The socket
workbench draws the anchor unframed, so it has no frame or lights there. Nothing here polls or allocates more
than a few colours per frame, and the vanilla textures are loaded on first use through `SpriteCache`.

## Node radii

Some subroutine kinds (Doctrine Archives, and the parked Bypass subroutine) act on every node within a radius
of a node. A radius is the straight-line distance between node positions on the tree, the same `x`/`y`
the tree is drawn with. A node exactly on the edge counts as inside. The centre node is never in its own
radius, so a mechanic that should also affect the centre has to add it itself.

There are three sizes, small, medium and large, set in `data/config/exiledSector/node_radii.json` in tree
units. Neighbouring nodes sit roughly 250 units apart, and the placeholder sizes of 400, 700 and 1000 reach
about 2, 4 and 7 nodes around a typical node. Each size must be at least as large as the one before, so a
smaller radius always lies inside a larger one. If the file is missing or invalid, the error is logged and
every radius covers nothing. A size of zero also covers nothing.

The Exoship node and the nodes it carries move with the ship, so they are never inside any radius and
never a centre.

`SkillTree.radii()` holds which nodes fall inside each size around every node. It is built once when the
tree loads and again whenever the active tree changes, for example when a mod area is switched on or off,
so a lookup is a single map read and costs nothing per frame. The tree canvas draws the radius as a ring
around its centre while the pointer is over a socket holding a radius subroutine, over an allocated socket
while such a subroutine is being placed, or over such a subroutine in the item storage panel (centred on the
socket it sits in on this ship, otherwise on the socket the panel is filling). `SubroutineRadius` picks the
centre and size through the kind, so each radius kind chooses its own centre: a Doctrine Archive uses its
socket.

While a radius subroutine sits in a socket on the shown ship, the canvas also marks its radius all the time
(`NodeRadiusRingRenderer.renderSocketed`) with vanilla's neutrino detector band (the `abilities`/`neutrino_detector`
texture, drawn like `GraviticScanAbility`), its outer edge fixed on the radius and fading inward, with two
passes pulsing against each other. The texture's transparent lead-in is trimmed so the glow starts right at
the edge. It takes the doctrine's faction colour (the `faction` column's base UI colour), or the tree's glow
colour without one. The hover ring draws on top of it. `TreeAllocationSession.socketedRadiusFootprint` finds the socketed
subroutine each frame by walking the ship's few socketed items.

## Doctrine Archives

A Doctrine Archive is a subroutine of kind `DOCTRINE`, one per vanilla faction volume. While it sits in an
allocated standard socket it rewrites every node within its radius on that ship only. The nodes still have
to be allocated to give anything, and only one Doctrine Archive fits on a ship (`SubroutineKind`). Installing
a second is refused, extras are unsocketed when a save loads (`SubroutineSocketLimits`), and restoring an NPC
tree from its tag skips a socket past the limit. Doctrine Archives have unique rarity (`SubroutineKind.uniqueRarity`,
read through `ItemDefinition.hasUniqueRarity`) without being `unique` in `subroutines.csv`, so they stay out of
the unique drop and unlock rules and still take Corrupted Nanoswarms and Replicator Nanoseeds. Nothing drops them
yet; the console command `ExiledSectorGrantDoctrineArchives [faction]` adds them for testing.

Each doctrine has a row in `data/config/exiledSector/doctrine_archives.csv`, keyed by the subroutine's id.
`behaviour` is `TRANSFORM` or `AUGMENT`, `radius` is `small`, `medium` or `large` (the sizes from "Node
radii"), `faction` is the vanilla faction id whose colour marks the radius, `notables`, `keystones` and
`small_nodes` list the skill types a transforming doctrine may put in
place of each tier, and `modifiers` lists `@ID` or `@ID:weight` references to `item_modifiers.csv` rows
whose `item_types` include `doctrine`, with `min_lines` and `max_lines` giving how many of them an
augmenting doctrine adds to each node. Doctrine modifiers must be prefixes. `DoctrineArchives` loads the
file after the item definitions and skips, with an error, a row whose id is not a `DOCTRINE` subroutine, a
bad behaviour or radius, a transform row without replacements or with modifiers, an augment row with
replacement types or without modifiers, and line counts that are not whole numbers from 1 up. A `DOCTRINE`
subroutine with no row is logged and rewrites nothing. `ItemDataIntegrity.checkDoctrineArchives` checks the
same at startup against the declared skill types, and also that each listed type exists, has the tier of
its column, is not optional and has no item cost.

The rewrite of a node depends only on the item's seed and the node's id. The item tooltip shows the seed as
a firmware revision ("Rev. 4471", `DoctrineRewriter.revision`). Small, notable and keystone nodes in range
are rewritten; sockets, roots and wormholes never are, and the socket itself is never in its own radius.

An augmenting doctrine gives every node in range between `min_lines` and `max_lines` extra lines, drawn
from its modifier pool by weight without repeats and rolled within each modifier's range, all from a
random seeded by the item seed and node id. Nodes that only apply for a while after deployment get none.
The lines reach the ship through `AllocatedSkillEffects` like the node's own effects, so they apply only
while the node is allocated, add per effect like every other tree multiplier, and also reach nodes that
pass a vanilla hull mod through (`ResolvedTree` adds them beside the vanilla effect).

A transforming doctrine replaces each notable, keystone or small node in range whose tier has a
replacement pool. The pool is put in an order by hashing the seed, node id and type id, so adding a type
to the pool never reshuffles the others. The first candidate that passes is used: it must be a known type
other than the node's own, not optional and without an item cost, and pass `NodeEligibility.check` as if
it stood on that node, against the ship's facts and against every allocated node outside the rewrite and
every replacement already chosen (hull size, hull requirements, hull mod and learned conflicts,
exclusivity and blocking effects). A notable or keystone is never given a type the ship already has
allocated elsewhere or that another node in range already took, so bonuses never double; small-node
replacements may repeat. Nodes are resolved in node id order. If no candidate passes, the node keeps its
own type, and that type is then reserved like an allocated one: the pass runs again with it in place until
every node either has a replacement or keeps its type, so a kept node never ends up duplicated by or
exclusive with a replacement. When the ship's facts are not known yet, only exclusivity is checked. A
transformed node counts only as its replacement for exclusivity (`AllocatedNode.isRewritten`), so its
original's hull mod and node exclusions stop applying while it is transformed.

The rewrites live in a per-ship layer, never in the shared `SkillTree`. `DoctrineOverrides` builds a
`DoctrineLayer` for a `ShipSkillData` the first time it is asked and caches it against the data's revision,
the loaded doctrine data and the radius cache, so it is rebuilt only when the ship's allocations or sockets
change, never per frame. `SkillNode.resolveEffectiveType` returns the replacement for a transformed node, so
effects, phantom hull mods, exclusivity checks, search, the bonus summary and the drawn tree all see it,
while `resolveBaseType` gives the node's own type. Ship facts are bound to the data by the stats pass
(`SkillTreeHullMod`) and the tree screen (`NodeAllocator`); when the relevant facts change (hull size, hull,
shield, the hull mods the pools care about), the layer is rebuilt. Facts that do not appear in that
fingerprint, such as a civilian-grade hull mod or a newly learned phantom conflict, take effect at the next
change to the ship's allocations or sockets. NPC trees never bind facts, since they never hold a doctrine. The tree screen also rebuilds the layer
of its planning copy whenever it mirrors the live sockets.

Removing the doctrine, unallocating its socket or moving it to another ship drops the layer: transformed
nodes turn back into their originals and stay allocated, and augment lines disappear. Allocation itself
still checks a node's own type, so a node whose original is ineligible on the ship cannot be allocated even
if its replacement could be.

While a node is transformed, only its replacement's exclusions apply, so the player can allocate nodes and
install hull mods that its original would have excluded. When the doctrine is removed, the original comes
back with its exclusions, the same as allocating a node that conflicts with something already there. A
conflicting node allocated meanwhile simply stays allocated beside it. On the next stats pass,
`HullModConflictResolver.removeConflicts` strips every removable hull mod the original excludes (not
built-ins, s-mods or hull mods the tree placed) with the usual skill conflict warning. If the original
places a phantom hull mod and the player has meanwhile built in or s-modded a hull mod that MagicLib strips
that phantom against, the next stats pass places the phantom, MagicLib removes it and reports the cause, and
`PhantomConflictWatch.learnFromRemovalAttempt` learns the conflict and queues a revert. That revert
deallocates the restored node and every node `RespecPlan` strands behind it, refunds them and announces it.
A conflict that was already learned does the same without the strip attempt, through
`queueRevertsForBlocked`.

Before any of this happens, the socket's tooltip and the item storage panel's notice (when the panel
targets that socket) name the removable hull mods that removing the doctrine or unallocating its socket
would strip (`DoctrineRevertConflicts`). The phantom case is not predicted, since a conflict MagicLib has not
reported yet is not known in advance.

On the tree, rewritten node icons are tinted (amber for transformed, light blue for augmented). A rewritten
notable's coloured ring stack and ambient glow take the doctrine's faction colour in place of pink, and a socket
holding a doctrine is ringed by the keystone aurora belt in that colour, dimmed while the socket is unallocated
(`DoctrineColors` turns a doctrine or faction id into the colour, shared with the radius band). A
transformed node shows its replacement's name, icon and tooltip, then "Originally: <name>"; an augmented
node lists its added lines; both name the doctrine and its revision. The archive's tooltip, and the tooltip
of a socket holding one, shows "Limited to 1 doctrine archive" under the title, then its firmware revision
and radius.

The first draft has augmenting doctrines for the Hegemony (armour, hull and ballistic lines), the Lion's
Guard of the Sindrian Diktat (combat readiness, range and energy lines) and the Persean League (fighter,
logistics and escort lines), with one or two lines per node, and the pirates, whose lines pair speed or
damage with a combat readiness or d-mod drawback. Tri-Tachyon, the Luddic Church and the [REDACTED] volume
transform. Every doctrine has a large radius. Their notable pools are their own volume's notables plus
three or four curated `doctrine_*` notables each, Tri-Tachyon and [REDACTED] have a curated keystone each,
the Luddic doctrine uses its volume's two keystones, and the Luddic doctrine turns small nodes into Plain
Rites, which gives nothing. Every magnitude is a placeholder.

## Hull Upgrade icons

The Hull Upgrade icons in `graphics/icons/hull_upgrades/` are generated from vanilla hull sprites by
`tools/HullUpgradeIcons.java`, which traces each sprite into a schematic on a holo panel tinted with the
socketable rarity colours, with one to four pips for the hull size. Run it from the repository root with
`java tools/HullUpgradeIcons.java --overwrite`. `--frigate`, `--destroyer`, `--cruiser` and `--capital` pick
other sprites (relative to `--ships`, which defaults to the dev install's `graphics/ships`, or absolute), and
`--preview <file>` writes a sheet of all twelve icons. The rarity colours are copied from `ItemRarity`,
so change both together.

## Star Chart loot

Loot in a chart system has three layers: the sites placed in it, the sources loot comes out of (salvaging a
site, beating a chart fleet, finishing the objective) and the drop types (hull items, weapons, currency and
so on). A drop bonus raises its type at every source in the system, and a site bonus only changes what gets
placed. The completion reward is the exception: only Bounty and Quantity scale it. The full design is in
`docs/plans/chart_loot.md`; every amount in the data is a placeholder.

### Sites

Each tier has four site budgets in `star_chart_tiers.csv`, one per category: caches, Domain sites, stations
and derelicts. `ChartContentPlanner` spends them in the order Domain, stations, derelicts, caches, each with
the usual pick weighted towards sites whose cost is near the average. When what is left is smaller than the
cheapest member's cost, one more of the cheapest is bought with a chance of leftover ÷ cost, so a small
budget bonus still adds sites on average. The 24-site cap counts all categories together, so it cuts the
cheap caches first. Plenty multiplies all four budgets and Stockpiles, Relics, Outposts and Wreckage one
each. Domain sites can bring their vanilla drone defenders, so a Domain bonus also adds fights.

A row in `chart_poi.csv` with `requires_mod` is only placed while that mod is enabled. The Industrial
Evolution arsenal station also needs IndEvo's derelict and derelict station settings on.

### Drop bonuses

`ChartGenContext` adds up the percentages for each drop type. `dropMult(type)` is 1 plus the type's own
bonus, plus Salvage for the item types (hull sections, subroutines, star charts), plus Quantity for every
type except colony items, all divided by 100. Chart quality counts as 1% Quantity per point. The chart
tooltip lists the totals that aren't zero.

### Our items and materials

The general hull item and subroutine rolls in `item_salvage.csv` are scaled by Salvage and Quantity. A
bonus to one type, such as Reactor items, adds a separate targeted roll instead of changing the general
odds: the expected number of extra items is the bonus times the rule's base expected item count, divided
by the number of hull sections that can drop (subroutines are their own share). The targeted items are
only ever of that type, and a type that can't drop yet gets nothing. Nanite Feedstock and Currency scale
the material rolls separately. Currency covers the nanoswarms, the Inhibitors and the ciphers, and ciphers
still only drop inside chart systems. Star charts use the Star Chart bonus.

### Vanilla loot

`chart_loot.csv` maps vanilla drop groups to the weapon, resource, blueprint and AI core types, and holds
base entries per site category, per chart fleet and for the warden. When a site is placed, each mapped
entry in its salvage spec, and each of the site's own entries such as a wreck's hull-sized value drop, gets
a copy for the bonus. A random entry's copy gets the extra share of its chances as whole chances, rounded
with the run seed. Scaling its `valueMult` instead would not survive: when a site turns into a debris field,
vanilla resets `valueMult` on every random entry to 0.25, which would turn the copy into a full duplicate.
A value entry's copy scales `valueMult`, which the debris field keeps. Base entries for the site's category
are added on top, scaled by the bonus.

Groups the game hasn't loaded are skipped, because vanilla throws on an unknown group. Colony item groups
(`rare_tech` and its variants) can't be mapped at all, so no bonus scales the colony items in vanilla's own
site specs. A wreck the player recovers as a ship pays none of its salvage loot, as in vanilla.

### Chart fleets and the warden

Each fleet records its starting deployment points (`$exiledSector_chartStartDp`) when it spawns. In every
engagement the player wins, `ItemLootListener` traces each destroyed or disabled enemy ship to its fleet
with `BattleAPI.getSourceFleet`. When the battle's loot is built, each chart fleet of the live run rolls the
`exiledSector_chart_fleet` rule and the `chart_fleet` chart drop once, scaled by the share of its starting
points destroyed. A running total in fleet memory (`$exiledSector_chartLootDpPaid`) caps a fleet at one
full roll however many battles it takes. The warden also rolls `exiledSector_chart_warden` and
`chart_warden`, scaled by the Hoard. Each fleet rolls on its own, so an error only loses that fleet's loot.
Battles inside a chart system don't roll the general battle chart drop.

The vanilla fleet rows in `chart_loot.csv` are sized per 100 fleet points at spawn and scaled by the bonus;
the warden adds its spoils rows on top, also scaled by the Hoard. Vanilla pays a fleet's drop entries the
first time it is on the losing side of a battle, even if most of it retreats.

Defenders that vanilla spawns at a site (`SalvageGenFromSeed`, such as the drones at Domain sites) are adopted
the first time the player wins an engagement against them: `ChartFleetLoot.adoptDefender` finds the run's
site whose `$defenderFleet` is that fleet and gives the fleet the run key. Their starting points and paid
total live on the site's memory, not the fleet's, because vanilla clears a defender fleet's memory every
time the player leaves the fight and, unless the player won outright, may keep the fleet for a rematch. A
defender's starting points are recorded at the first engagement the player wins, so a rematch against a
fleet already worn down in a lost engagement pays its remainder as a full share, still capped at one roll. The vanilla fleet rows go onto
the defenders once per site, for the same reason. Defenders roll the fleet rule, never the warden's, and
don't count towards the objective. Vanilla's defender plugin is left alone: it is picked by priority, so
registering our own would take over other mods' strength and size choices.

### Colony items

Each run makes exactly one colony item roll when its system is built, from its own seeded random. The
chance is `ceiling × (1 − e^(−t × m ÷ ceiling))`, where `t` is the tier's `colony_item_chance` and `m` is 1
plus the Providence bonus. Small chances come through almost unchanged and large ones bend towards the
ceiling without reaching it. The ceiling is the reserved `ceiling` row of `chart_colony_items.csv` and must
be below 1. On a success the item is picked by the weights in that file and goes to the warden, or else a
random station or Domain site, or else any site, as vanilla extra salvage. It is paid once, in the battle
loot or when the site is salvaged.

## Star Chart drawbacks

Most chart prefixes strengthen the enemy at generation through `ChartGenContext`. The rest act on the player's fleet while it is inside the system, and every one of them covers the whole fleet: every ship and the fighters they launch, never allied fleets.

### Ship drawbacks

`ChartFleetStat` lists each stat and how it is applied. Rolls of one stat are summed first and then turned into one multiplier, clamped to 10%-1000%, so the lines say "more" and "less". `ChartRunScript` puts the run's multipliers into the `ChartFleetConditions` snapshot while the player fleet is inside a live run and clears it when the fleet leaves. `ChartRunEffects.removeFrom` (run close) and `ChartLoadSweep` (every load) also clear it. Each change marks the player fleet's members for a stat update, and nothing is saved.

`SkillTreeHullMod` applies the snapshot after the tree, under `ChartFleetConditions.MOD_ID`, to ships whose fleet member belongs to the player fleet. Because it runs in `applyEffectsBeforeShipCreation`, creation-time stats such as shield arc are covered, and the refit screen shows the reduced values while the fleet is inside. Fighters get the same stats in `applyEffectsToFighterSpawnedByShip`. The per-type damage taken stats only cover armour and hull, so each damage type also sets its shield variant.

### Battle drawbacks

`ChartBattlePlugin` is registered under `plugins` in `data/config/settings.json`, so it starts with every battle. It stays off unless the battle is a campaign battle (not a simulation) and the snapshot holds a battle stat. It refreshes on its first frame and then once a second.

- Enemy deployment cost is a multiplier on `DEPLOYMENT_POINTS_MOD` in each enemy reserve's fleet member stats, which `FleetMember.getDeploymentPointsCost` reads directly. Those stats are transient and rebuilt on every stat update, so nothing reaches the save, and the plugin reapplies the multiplier each refresh. Not every chart ship carries the tree hullmod, which is why this lives in the plugin. The player's side (Cramped) is a ship drawback on the same stat. Changing the deployment caps themselves doesn't work: the engine floors the commander's battle size bonus at 0 and caps each side at 60% of the battle size.
- Command point recovery is a flat on the task manager's `getCPRateModifier()`, sized so the total rate is multiplied. That modifier has a base of 0, so a multiplier on it would do nothing.
- Enemy objective capture is a multiplier on each enemy ship's `SHIP_OBJECTIVE_CAP_RATE_MULT`. The engine uses the side's best ship and floors it at 1, so this only works as a bonus, never to slow the player.
- Battle size (Contained and Sprawling, which pay no item quantity) changes both sides' caps by a flat total. The battle size option has no API setter, so on its first frame the plugin reads both caps, splits the change between the sides in proportion to them, and fixes them with `setMaxFleetPoints`. That call stops the engine recomputing caps for the rest of the battle. The split never changes mid-battle anyway, so the plugin only re-adds captured objectives' bonus deployment points each refresh, the same way the engine does (battle size times `getBattleSizeFractionBonus`). Unlike the engine, it doesn't cap a side at 60% of the battle size.

### Hunted

Hunted adds a flat sensor range bonus (`ChartIds.ENEMY_SENSOR_MOD_ID`) to every chart fleet when it spawns. Fleet stat modifiers are saved with the fleet, so the bonus lasts for the run with no further work, and vanilla AI pursues the player once it sees them. The engine drops anything beyond `sensorRangeMax` (5000 su) from detection checks, so fleets further away than that still can't see the player.

## Reflection

Starsector's script class loader won't load a mod class whose source names `java.lang.reflect` (it also
blocks direct file access), and `ScriptSandboxRestrictionsTest` fails the build if any source file does.
Where the mod has to reach code the API doesn't expose, it goes through `java.lang.invoke` method handles
instead. Every use is guarded. If a lookup or call fails, for example because a game or mod update
renamed something, that one feature logs the error once and switches itself off until the game
restarts, and everything else keeps working.

### The refit screen (`ui/refit/UiReflection`)

The refit screen's widgets are obfuscated engine classes outside the API. `UiReflection` reaches them by
calling `Class.getMethods` and `Class.getDeclaredFields` through method handles, loading the
`java.lang.reflect` types by name so the source never mentions them. It finds methods by name and
parameter count and fields by type, caches what it finds per class, and makes them accessible with
`trySetAccessible`. All of the uses below run from `PhantomHullModRefitHider`. That runs whenever the game
rebuilds an installed hull mod list (the phantom and skill tree hull mods trigger it from
`getDisplayCategoryIndex`) and after character stats refresh.

The refit panel is found from the core UI (`getCoreUI` on an open interaction dialog, otherwise `getCore`
on the campaign UI) through `getCurrentTab`, then `getRefitPanel`, `getModDisplay` and
`getShipDisplay().getCurrentVariant()`.

To hide phantom hull mods, the installed hull mod list (the child widget with `collapseEmptySlots`) is
walked row by row, and each row's `HullModSpecAPI` field identifies its hull mod. Rows for phantoms the tree
placed are taken out with `removeItem` and `collapseEmptySlots`, the list is resized from `getItemHeight`
and `getItemPad`, and the widget is re-laid out with `pack`. Mod lists inside dialogs opened over the refit
screen get the same treatment.

`SkillTreeChipClickTarget` makes the Exiled Sector Skill Tree hull mod clickable. It finds that hull mod's
row and icon button, lays an invisible click target over the list, and checks `getItems` on each click to
make sure the row is still shown before opening the tree. `SkillTreeModsButton` reads the hull mod panel's
Build In button (`getPerm`) to size and place the Skill Tree button directly below it.

If any of these fails, phantom hull mods stay visible, clicking the hull mod does nothing, or the button
doesn't appear. The LunaLib refit button always still works.

### Second-in-Command (`compat/SecondInCommandCompat`)

To check whether a Second-in-Command skill is active for a fleet without depending on that mod at compile
time, the mod loads `second_in_command.SCUtils` and `second_in_command.SCData` by name through the game's
script class loader. It then looks up `SCUtils.getFleetData` and `SCData.isSkillActive` as public method
handles. If either lookup or call fails, the Second-in-Command checks switch off. The startup
compatibility self-check reports the two methods as missing if they can't be found.

### MagicLib (`compat/MagicLibCompat`)

MagicLib is a normal dependency, so its calls are made directly. Reflection is only used by the startup
self-check, which looks up `MagicIncompatibleHullmods.getReason` and `removeHullmodWithWarning` as public
method handles. That confirms they still exist with the expected signatures, and the log says so if not.

### Phantom hull mods (`effects/PhantomHullMods`)

Each phantom wraps the vanilla hull mod's own effect. The original effect class is loaded by name through
the script class loader and created with its public no-argument constructor, called as a method handle.
If that fails, the error is logged, and a copy of that hull mod installed the normal way does nothing in
that session.

### Engine internals used without reflection

These don't use reflection, but they rely on behaviour outside the API, so they can break the same way:

- `SkillTreeRefitButton.openPanel` finds LunaLib's `RefitButtonAdder` among the sector's transient scripts
  and opens the skill tree panel the same way LunaLib's own refit button does.
- `WeaponDroneFactory.createSingleShot` builds a refraction drone's weapon while the shared weapon spec
  briefly has no charge-up, a burst of one and a long refire delay, because the engine copies those values
  when it builds a weapon. The spec is restored in a `finally` block.
