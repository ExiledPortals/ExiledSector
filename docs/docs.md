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
reaches it.

The rest of the budget goes to goals. Notables and keystones are drawn by seeded weighted random
(relevance × tier) until their path lengths cover the remaining budget, and the ship then takes the
nearest affordable goal, path and all, one at a time. Small nodes are only taken as path steps until no
notable or keystone fits, and then they fill whatever is left.

Every step is a breadth-first search from the allocated nodes. A node is traversable only if the ship can
use it: requirement tags, exclusivity and hull size are checked through `NodeEligibility`, and optional
nodes pick an option the ship can use. Only the wormhole pair joining the core to the ship's own faction
volume can be crossed. Its far end comes free, as for players.

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
`kind` column, one of `VOLUME`, `DOCTRINE`, `BYPASS` or `UNIQUE`, read into `SubroutineKind` on the
definition and never stored on the item. A row with a missing or unknown kind is skipped with an error in
the log. Only `UNIQUE` is built so far; the other kinds are planned in `docs/plans/subroutines_rework.md`.

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

Volume and Bypass subroutines are born corrupted (`SubroutineKind.bornCorrupted`). Every way a subroutine
comes into being ends in the `Subroutine` constructors or its `readResolve`: rolling a drop, a console grant,
claiming or previewing an `npc:<definition>/<seed>` id, absorbing a cargo stack, a transposition, and an
`UnresolvedItem` turning back into a subroutine. There, an item of a born-corrupted kind with no corruption
yet gets `CorruptionOutcome.INNATE`. That counts as corrupted everywhere, so every currency refuses it
exactly as it refuses an item a Corrupted Nanoswarm has touched, and its frame takes the corruption colour.
A Corrupted Nanoswarm only ever rolls the other four outcomes (`CorruptionOutcome.NANOSWARM_OUTCOMES`). The
tooltip and the workbench show "Corrupted from the start" for `INNATE`, which claims nothing was changed,
and plain "Corrupted" for the rest.

Volume and Bypass subroutines also roll one tree node when they are created (`SubroutineKind.rollsNode`).
The candidates come from `SubroutineNodePools`, where each kind plugs in a function from the definition to
its candidate node ids. No kind has a pool yet; Volume (the notables of one volume) and Bypass (the core
keystones) will register theirs when they are built. The pick is a pure function of the candidates and the
item's seed: the ids are trimmed, de-duplicated and sorted, then one is chosen with a `SplittableRandom`
seeded from the item seed and a fixed salt. So the same seed always lands on the same node, and an NPC's
`npc:` id decodes to the node the player gets when the ship is recovered. The chosen id is stored in the
`rolledNodeId` field of `Subroutine` and never rolled again, so a later change to the tree or the pool
cannot reroll an existing item. It is copied by mirroring and by every conversion, and an `UnresolvedItem`
keeps it while the definition is missing. If the kind has no pool, the pool is empty or it throws, the field
stays empty and nothing crashes; an item loaded with an empty field tries again on the next load. A
subroutine in cargo carries only its seed, so it takes its node when it is absorbed into item storage. The
tooltip names the node through the tree's node and type names, and a stored id that is no longer in the
tree shows as an unknown node.

A subroutine fits a tree socket only if its definition has a kind. Doctrine and Bypass subroutines are
limited to one per ship and Volume and Unique ones are not (`SubroutineKind.socketLimitPerShip`).
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

## Wormhole anchors

A wormhole anchor is a socketable of its own type (`ItemType.WORMHOLE_ANCHOR`, class `WormholeAnchor`, saved
under the XStream alias `exiledSector.WormholeAnchor`). It is not a subroutine. So far only the item exists:
socketing one does nothing yet, nothing drops it, and the faction wormholes in the tree are unchanged. The plan
for the rest is section 6 of `docs/plans/subroutines_rework.md`.

The definitions are in `data/config/exiledSector/wormhole_anchors.csv`, one row per tree volume, with the
columns `id`, `volume`, `exit_node`, `name`, `icon`, `rarity`, `description` and `unlock`. `volume` is a tree
region other than `core`, and `exit_node` is the node the anchor will open onto, today the far end of the
volume's wormhole pair (`exoship` for ExoTech). `WormholeAnchorDefinitions` loads the file after the star charts
and keeps the volume and exit of each anchor. It skips, with an error in the log, a row whose volume is not a
region, a row without an exit, a second row for a volume that already has an anchor, and an id that a
subroutine, hull item or star chart already uses. The definitions sit in their own registry, like star charts,
so `ItemDefinitions.all()` never lists them. `ofType(ItemType.WORMHOLE_ANCHOR)` does. Names and descriptions
are translated as `socketable.<id>.name` and `.description`, like every other item definition.

`ItemDataIntegrity.checkWormholeAnchors` runs at startup against the declared skill tree. It reports a
missing or repeated id, an id used by another item file, a bad or repeated volume, an exit that is not in the
tree, sits in another region or is not a wormhole tier node, and an unlock that `ItemUnlock` does not
understand (an item unlock id, `cleared_chart_tier_N` or blank are all accepted, the same set the runtime
honours). A
modifier row listing `wormhole_anchor` in its `item_types` is an error too, since anchors roll nothing.

An anchor has no prefixes, suffixes or implicits and is never corrupted or mirrored: it ignores stored traits,
and its explicit and implicit lists are always empty. `ItemCrafting.canUse` refuses every currency on it, even
one a mod extends to the anchor type, so the workbench shows nothing usable. `ItemDisassembly` refuses to break
it down, and item storage leaves it out of disassembly marking and the right-click disassembly prompt. The drop and NPC pickers in `ItemDrops` skip
the anchor type outright, on top of anchors not being in `ItemDefinitions.all()`, so salvage, battle loot,
chart loot and NPC fleets never produce one.

`canSocketInto` accepts the standard tree socket (`SkillTier.SOCKET`), like a subroutine. Item storage lists
anchors with the subroutines when a tree socket is being filled, and the tree's storage button ("Socket
Items") counts both. Anchors have no `ItemKind`, so storage and workbench text that names an item's kind uses
`Item.kindName()`, which falls back to the definition's noun. An anchor whose area is off cannot be socketed:
`TreeAllocationSession.socketRefusal` returns the anchor's own refusal (`socketable.anchor.areaOffRefusal`),
which plays the refusal sound and shows in the storage panel in both the place-on-socket and targeted-socket
flows. A socketed anchor's tree tooltip shows the same volume and inert lines as its item tooltip. Area gating reuses the area toggles: `AreaToggles` and `TreeRegionFilter` already drop a disabled
area's nodes from the active tree, so an anchor counts as active only while its exit node is in the active
tree (`SkillTree.get`). That covers Kesteven and Enigma (LOST_SECTOR), ExoTech and Abyss (RAT) and Nightcross
and Stargazer, whatever the setting or missing mod that turned them off. The definition stays loaded either
way, so an anchor the player owns is never lost when an area is turned off. Its tooltip names the volume it
leads to and says when it is inert because its area is off.

The `unlock` column on an anchor is a condition for use, not for dropping. The ExoTech anchor, the Hyper-Nav
Beacon, carries `rat_player_exoship`, an `ItemUnlock` that is met once RAT reports the Exoship recovered or
owned, and latches like the other item unlocks. It reads RAT through
`RandomAssortmentCompat.exoshipConfirmedRecovered`, which fails closed: if RAT's Exoship state can't be read,
the unlock is not met and nothing is latched. The tree's ExoTech beacon gate (`exoshipGateOpen`) still fails
open so its area stays usable. Until the unlock is met the tooltip says the beacon is inert. Nothing enforces it
yet, because socketing does nothing yet.

For testing, the console command `ExiledSectorGrantWormholeAnchors [volume]` adds one anchor per volume, or
only the named volume's, to the player's cargo.

## Node radii

Some subroutine kinds planned for the rework (Bypass, Doctrine Archives) act on every node within a radius
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
so a lookup is a single map read and costs nothing per frame. The tree canvas can draw the radius as a ring
around its centre, which the hover on a socket holding a radius subroutine will turn on once those
subroutines exist.

## Hull Upgrade icons

The Hull Upgrade icons in `graphics/icons/hull_upgrades/` are generated from vanilla hull sprites by
`tools/HullUpgradeIcons.java`, which traces each sprite into a schematic on a holo panel tinted with the
socketable rarity colours, with one to four pips for the hull size. Run it from the repository root with
`java tools/HullUpgradeIcons.java --overwrite`. `--frigate`, `--destroyer`, `--cruiser` and `--capital` pick
other sprites (relative to `--ships`, which defaults to the dev install's `graphics/ships`, or absolute), and
`--preview <file>` writes a sheet of all twelve icons. The rarity colours are copied from `ItemRarity`,
so change both together.

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
