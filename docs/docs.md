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
alive, can be hit (phased ships and other effects' drones are skipped) and are within half the beam
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
