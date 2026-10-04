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
damage is shared.

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

## Reworked vanilla hull mods

Escort Package keeps vanilla's structure but splits it into four separately tunable magnitudes:
manoeuvrability, speed, weapon range and the proximity range. The defaults match vanilla at +25%, +10% and
+20% within 700 su, fading out over the next 500 su and doubled for a destroyer escorting a capital. The
bonus is recalculated about once a second. Vanilla's S-mod shield damage reduction for destroyers isn't
included.

The Phase Anchor emergency dive (`PHASE_ANCHOR_EMERGENCY_DIVE`) uses vanilla's trigger, animation and
timing. It also shares vanilla's once-per-battle flag, so a ship with the vanilla Phase Anchor and a ship
with this node share one dive per battle between them. Where vanilla charges the ship's full deployment
cost in CR, the node charges a percentage of it set by its magnitude. It can't be used on temporary nodes.

Reduced D-mod effect (`DMOD_EFFECT_MULT`) adjusts vanilla's D-mod effect multiplier and then re-applies
the ship's D-mods. D-mods are applied before this mod's hull mod, so without the re-apply they would never
see the new multiplier.

Ground Support (`GROUND_SUPPORT_FLAT` plus `GROUND_SUPPORT_PER_MAX_CREW_PERCENT`) grants the vanilla
hull mod's flat ground support plus a share of the ship's maximum crew capacity, and is mutually
exclusive with Ground Support and Advanced Ground Support.

Militarized Subsystems requires a civilian hull and applies the vanilla hull mod's effects itself: the
civilian-grade sensor penalties are removed, maximum burn level goes up by 1 and minimum crew by 100%. It
places Militarized Subsystems as a phantom, so vanilla's civilian-hull checks (skills, Additional
Berthing, Auxiliary Fuel Tanks, Expanded Cargo Holds, Assault Package) and Second-in-Command treat the
ship as militarized.

Second-in-Command's hull mod synergies (Redistribution for Shield Shunt, Enhanced Overrides for Safety
Overrides and so on) work through the phantoms, because SiC checks `variant.hasHullMod`. The Converted
Hangar penalties are waived by the vanilla flags or by SiC's Reconfiguration skill. The skill check stays
because SiC sets those flags in its own hull mod, which may apply after ours.

Every node that stands in for a vanilla hull mod places that hull mod as a phantom (`phantomHullMods`).
While the node is allocated, the real hull mod sits on the ship as a permanent mod costing no OP, so any mod
that checks for it (Ship Mastery System masteries, Second-in-Command skills, other mods) sees it. At startup
this mod wraps each hull mod's vanilla effect: on normal ships it behaves exactly as vanilla, but on copies
the skill tree placed it does nothing and shows a tooltip naming the node instead, so the node's own effects
are the only ones that apply. The refit screen's installed hull mod lists hide these copies, including the
copy beside the Add and Build In dialogs. The engine's list widget is the only caller of the wrapper's
`getDisplayCategoryIndex`, so every rebuild of a list holding a phantom schedules one hide pass that runs
later in the same frame, before anything is drawn. Every refit sync also ends by refreshing character stats,
so a `CharacterStatsRefreshListener` hides the main list straight away. It reaches the lists through the
game's own method names (`UiReflection` is the one file allowed to use reflection). If the screen's structure
ever changes, it stops quietly or logs one error and the rows stay visible. The dialogs' selectable tables
still list phantoms. When the Codex opens, a ship's entry drops phantoms from its Related Entries (unless the
hull's own entry relates to that hull mod); the "Hull mods:" line on ship pages and tooltips still lists them,
because nothing calls into the hull mod while that line is built. If the game created the effect before this mod could wrap it, the log says so
and the node falls back to not placing the hull mod, and its tooltip stops claiming it does. Vanilla hull
mods and their nodes are always 1:1: no hull mod is split across several node types, and no node type
stands in for more than one hull mod (`SkillTypesDataConsistencyTest` enforces this).

A node's own hull mods (its phantoms and its passthrough `vanillaHullMod`) always count as exclusive, so
`exclusiveHullMods` only lists other hull mods. Two nodes are mutually exclusive whenever either one's
`exclusiveHullMods` names the other's own hull mod, so Advanced Optics listing `high_scatter_amp` is enough to
keep the two nodes apart. `exclusiveSkillTypes` is only for nodes with no hull mod of their own; the data
test rejects entries the hull mod lists already imply.

The phantom is a permanent mod, so another mod's hull mod that strips it as incompatible through MagicLib
can't remove it; this mod removes the incompatible hull mod instead and shows the usual conflict warning.
Safety Overrides also stays mutually exclusive with the strippers it knows about (LOST_SECTOR, HTE, NSP,
A_S-F, Tahlan, UAF and Neoteric ones), which keeps them out of the refit screen in the first place.
Removing ExiledSector from a save leaves these hull mods behind
as real, permanent hull mods, so deallocate the nodes first.

High Resolution Sensors and Phase Field aren't phantoms, because the game creates their fleet-wide effect
separately and it can't be wrapped. The LOST_SECTOR nodes aren't phantoms either.

Ballistic Rangefinder, Missile Autoloader, Defensive Targeting Array and Neural Interface are passthrough
nodes (`vanillaHullMod`). Rather than installing the hull mod, they run its code under the hull mod's id.
The Ballistic Rangefinder and Missile Autoloader tooltips include vanilla-style tables built from vanilla's
own numbers, with the row that applies to the current ship highlighted.

## Other notables worth describing

Disintegration makes energy hits on armour strip an extra percentage of the hit's damage directly from the
surrounding armour cells, using vanilla's own armour damage spread (1/15 to the inner 3×3 cells and 1/30
to the outer ring, skipping the corners). It respects the target's armour damage resistance, treats each
beam damage tick as a separate hit, never damages hull, and shows no floating damage numbers.

Terrifying Presence reduces the autofire aim accuracy of enemy ships within 1000 su by a number of
percentage points. It updates four times a second, stacks across several sources, and lifts as soon as an
enemy leaves range or the source ship dies or retreats.

## Temporary nodes

A node with `temporaryAfterDeploymentSeconds` only applies its effects for that many seconds after the
ship deploys, and its tooltip states the duration. Only effects that opt in can be used this way: stats
the game keeps reading during combat. That covers movement, flux capacity and dissipation, peak CR time and
CR loss, damage-taken multipliers, combat repair times, fighter refit and relaunch time, and weapon damage, range, flux
cost, fire rate, projectile speed and recoil. Stats fixed when the ship is built (hull, armor, shield arc,
fighter bays, weapon ammo), campaign stats, combat listeners and conditional effects can't be. If any other
effect appears on a temporary node, the loader logs an error and ignores the duration.

## Areas and the area toggles

Every node, star, ring belt and static image carries exactly one region tag: `inner` or one of the faction
regions (`luddic`, `tritachyon`, `hegemony`, `sindrian_dictat`, `pirate`, `REDACTED`, `persean_league`,
`lost_sector`). The editor sets it with the Area chips on nodes, stars, static images and asteroid-belt
anchors. Plain orbit anchors aren't drawn in game and carry no tag. Connector curves and hidden connectors
belong to the two nodes they join, so they don't need one.

The Optional Areas setting decides which regions are switched off. The Lost Sector area (the Kesteven and
Frozen Heart stars) is Auto by default, which shows it only when Lost Sector is installed; On and Off
override that. `SkillTree` keeps the whole parsed tree and builds the active tree from it with
`TreeRegionFilter`, at startup and again at the start of every game load, so a change takes effect on the
next load. Switching a region off removes its nodes, every wormhole whose paired end is in it, the
connections, curves and hidden connectors touching those nodes, and its stars, ring belts and images.

Saved trees then lose the removed nodes through the usual unknown-node cleanup: OP and banked free
allocations go back to the ship, and item costs go back to the player's cargo. Turning the region back on doesn't restore them. `AreaToggleDataTest` checks that every
decoration has a region and that switching a toggled region off leaves every remaining node reachable from
a root, with no dangling links or half wormholes.
