# Exiled Sector: skill effect mechanics

How the non-obvious effects work. Magnitudes live in `data/skilltrees/skill_types.json` and are placeholders.

Rule for anyone editing this file: one or two short paragraphs per section, less is better. Say what the
effect does and where the code lives. Leave out edge cases, rationale and anything the code already makes
obvious. Shorten an existing section rather than grow it. Describe only what is built, never planned or
missing features.

## Shared Fate

Shield hits are reduced by the summed `SHIELD_DAMAGE_SHARED_PERCENT` (capped at 90%) while a living,
non-fighter ally within 1000 su is not overloaded or venting. The flux the removed share would have caused,
at this ship's shield efficiency, goes to those allies as hard flux, split evenly and uncapped, shield or
not. Armour and hull damage are never shared. The notable's -20% weapon damage pools through the weapon stat
hierarchy.

## Lion's Gaze

A beam hit on an enemy ship splits to up to N more targets (`BEAM_WEAPON_SPLIT_TARGETS_FLAT`): the nearest
hittable hostile non-fighters within half the beam's range of the impact, picked again every 0.2 s or when one
dies. Damage is shared evenly. The
original target's share is a damage modifier (so it also scales that hit's EMP), and the split runs before
every other damage-dealt effect, so they all see the shared amount.

Each split target is hit by a real beam from an invisible, invulnerable drone placed just outside the
original target's shield or hull. The drone copies the ship's captain, weapon stat modifiers and damage-dealt
listeners, and fires for at least 1 s, then while the original beam hits plus 0.3 s. Drones carry no hull
mods except pure markers listed in `drone_marker_hullmods.csv` (shared with Refracting Projectiles). Weapons
in `split_beam_effect_blocklist.csv`, or ones a drone can't carry, fall back to simulated splits: per-tick
instant damage plus a MagicLib fake beam, without the weapon's effect code.

## High Scatter Amplifier

`BEAM_WEAPON_HARD_FLUX_PERCENT` converts that share (capped at 100%) of a beam hit's shield flux from soft to
hard on the target, never above its total flux. Beams that already force hard flux are skipped.

## The weapon stat hierarchy

Names follow `<SCOPE_>WEAPON_<STAT>_<FLAT|PERCENT|MULT>`, with scopes ALL > BALLISTIC / MISSILE / ENERGY and
ENERGY > NON_BEAM_ENERGY / BEAM; unsupported combinations are rejected at load. FLAT and PERCENT from every
scope reaching a weapon pool additively. The same MULT across nodes adds into one multiplier (floored at 100%
less) that multiplies with the rest. Beam flux cost is the exception: it compounds with energy flux cost
(-10% and -10% gives 81%).

Where the engine lacks a stat: NON_BEAM_ENERGY damage and range write the energy stat plus a hidden
cancelling beam entry, and beam or non-beam energy ammo is written per weapon at ship creation (right in
combat, missing from refit tooltips, not usable on temporary nodes). Turn rate exists only for all weapons
and for beams. Tooltips merge identical child effects on one node into the parent line.

## Refracting Projectiles

`NON_BEAM_ENERGY_WEAPON_CHAIN_CHANCE_PERCENT` gives a non-beam energy shot that hits a shield a chance to
refract to the nearest unhit enemy within weapon range, up to Max Refractions (default 5). Missiles and
weapons in `energy_chain_blocklist.csv` never refract. As with Lion's Gaze, a pooled drone fires the real
weapon as an instant single shot, then hands the projectile to the firing ship so its on-hit effects and
kill credit apply.

`NON_BEAM_ENERGY_WEAPON_CHAIN_FALLOFF_PERCENT` lowers each refraction's base damage and restores it after the
hit, rather than using a modifier, so EMP and on-hit effects stay at full strength.

## Module effects

The tree applies only to the parent ship. On the parent's first combat frame `ModuleTreeShare` gives each
living child module a share of the parent's shareable tree effects (`ModuleShareSetting`, 50% by default, 100% for stations), plus
the `MODULE_*` effects at full strength.
`ModuleBoost` applies both, then rescales the module's maximum hull, armor cells, shield arc (up to 360) and
each weapon's maximum ammo once; cells above their maximum are clamped first so armor carried from an earlier
engagement isn't raised again.
A ship docked into another's module slot (Arma Armatura's core units) keeps its own tree through the ship tag its variant carries:
`ShipRecords.resolve` lends it to any member outside its fleet's list, refit module tabs included, it gets no share of the
parent's tree, and `SocketCustody.addDockedShips` counts it as owned so its items stay socketed.

## Ballistic counts as energy

`BALLISTIC_COUNTS_AS_ENERGY` makes `BallisticEnergyMirror` copy every energy and non-beam energy
weapon effect from the ship's sources into a ballistic one before diminishing returns. Energy checks in Dynamic Grid,
energy range per sensor strength, Disintegration, Ion Arc and energy chains also accept ballistic weapons
(`BallisticAsEnergy`). Vanilla and other mods' energy bonuses don't reach ballistics.

## Hybrid ballistic slots

`BALLISTIC_SLOTS_HYBRID` (Dual-Feed Mounts) gives the variant its own copy of the hull spec with every non-built-in
ballistic slot retyped to hybrid (`HybridBallisticSlots`). Saves keep only the hull id, so the copy is rebuilt each
time hull mods apply. `SkillTreeHullMod` swaps the slots back once the effect is gone, and `UnfitWeapons` returns
weapons that no longer fit to cargo. After a tree edit, `ShipEditSession` swaps the refit screen's own variant at once
and `RefitShipDisplay` rebuilds its slot buttons.

## Rise From the Ashes

`RiseFromTheAshesListener` cancels the first hull hit that would kill the ship, sets its hull to a share of
maximum, restores armor and applies a damage taken multiplier for 6 s. It happens once per fleet member per
battle, tracked in the combat engine's custom data, so redeploying doesn't reset it. A ship's lethal hits go
through one `special/LethalHitArbiter`: Ludd's Light detonates on any lethal hit, even one a dive or Rise
then saves, then Rise From the Ashes and then the Phase Anchor dive try to save the ship, and the first that
does wins.

## Thermo-Accelerator

The rate of fire bonus is ship-wide, and `ThermoAcceleratorListener` stops every hardpoint weapon from firing
each frame, so only turrets benefit. Decorative and system slots are left alone.

## Temporary nodes

`temporaryAfterDeploymentSeconds` applies a node's effects only for that long after deployment. Only effects
on stats the game reads live in combat opt in. Anything else (creation-time stats, campaign stats, listeners,
conditional effects) makes the loader log an error and ignore the duration.

## Affinity

Roots and `small_flux_optional` nodes grant Low Tech, High Tech or Midline affinity (`*_AFFINITY_FLAT`), which
apply no stat themselves. `EffectTotals` sums each ship's permanent affinity first and turns it into tree MULT
bonuses for every whole step of 5 and 10, from the one table in `AffinitySteps`, so diminishing returns, NPC
scaling and the summary see the stat bonuses. `ModuleTreeShare` steps the shared nodes' affinity the same way.

## Diminishing returns

`EffectTotals` shrinks permanent tree and hull item bonuses to armor, hull, flux capacity, flux dissipation,
top speed and weapon range once their net total passes a LunaLib threshold, keeping only the efficiency
percent of the excess (`DiminishingReturns`, `DiminishedStat`). Thresholds are percent of the hull's base
value (`StatBaseline`), and flat bonuses count as the percent they add. Range counts percent and MULT per
weapon type, cutting broader scopes only as far as every type they reach allows. Drawbacks, temporary nodes
and vanilla hull mods passed through by nodes are untouched.

## Phantom hull mods

A node that recreates a vanilla hull mod also places the real one as a zero-OP perma mod tagged
`exiledSector_installed_<id>`, so other mods' `hasHullMod` checks see it. `PhantomHullMods.install` wraps
each such hull mod's effect in `PhantomHullModEffect`, which forwards to the original except on tree-placed
copies, where it does nothing, can't be removed and is hidden from refit and Codex. Fleet-wide hull mods
(High Resolution Sensors, Phase Field) are never phantoms.

## Phantom hull mod conflicts

Known conflicts go in a node's `exclusiveHullMods`, and `HullModConflictResolver` removes one that turns up
later. A hull mod that tries to remove a phantom through MagicLib has its cause removed instead.

Unremovable conflicts (mostly hidden built-in hull mods) are learned at runtime by `PhantomConflictWatch`,
from a MagicLib removal record or a phantom vanishing while its tag stays. `LearnedPhantomConflicts` keeps
them forever in `exiledSector_learnedHullModConflicts.json` in the common folder. From then on the phantom
isn't placed, `NodeEligibility` refuses the node with `LEARNED_CONFLICT`, and player ships get the node and
its dependants refunded like a respec.

## Generated NPC trees

`NpcSkillTreeBuilder.generate` builds each tree deterministically from the sector seed, fleet id and member
id and stores it in an `NpcTreeTag`. The design type picks the root. Removable hull mods with an equivalent
node are stripped and their OP buys those nodes. If `npc_faction_volumes.csv` gives the faction a volume, the
ship takes one notable or keystone there through its faction's real wormhole anchor (`NpcItems.anchorFor`),
socketed only if it allocates a node behind it. NPC ships carry nothing else, the anchor never drops, and a
recovered ship loses it.

The rest of the budget goes to notables and keystones drawn by seeded weight (`NpcRelevance` × tier), then
small nodes. `NpcRelevance` multiplies theme tag weights by `npc_faction_focus.csv` and
`npc_ship_system_focus.csv` (by ship system or defense id, `NpcFocus`), which also steers optional nodes'
option picks. Every step is a breadth-first search through nodes `NodeEligibility` allows, never crossing
wormholes.

Station fleets (market stations and the Remnant Nexus) are levelled like any other fleet while
`NpcTreeConfig.levelsStations` is on, and the station is always chosen. A station's `ShipProfile` counts its
modules' fighter bays, and `NpcStationNodes` stops it aiming for nodes or options whose every effect is movement,
phase, ship system, or fighter without bays.

## NPC levels

An NPC's level is its node count besides the root. Open-world ships roll 80% to 120% of a random player combat
ship's tree power (`NpcTreeSize.roll`), capped by category in `NpcLevelCaps`: `npc_levels.csv` first, then
AI-core commander (35), pirate flag (15), otherwise faction (25). The cap is stored per ship as a variant tag,
because battles merge fleets. Chart fleets are uncapped.

### Bounties

`NpcBounties` turns a bounty fleet into a 0 to 1 difficulty that raises the cap from the category cap towards
25, and the flagship gets 3 more levels. Person bounties always use the pirate category.

### Chart fleets

Chart ships roll a level in their tier's `level_min` to `level_max` from `star_chart_tiers.csv`, scaled by
Fleet Strength %, and bosses roll one higher. Outside charts, a won battle's chart chance is `chart_drops.csv`'s
battle chance per 100 enemy deployment points defeated, scaled by battle difficulty, at the highest tier whose
`level_min` the strongest defeated NPC reached (`ChartDrops.forBattle`). With the skill tree switched off, the
tier instead rises one per `strength_per_tier` deployment points defeated.

### Chart bosses

A chart's boss is a fleet or a station, picked by weight from the `chart_bosses.csv` rows allowed at its tier
(`ChartBosses`). A station is only picked when `chart_boss_stations.csv` has one for the run's faction at that tier,
and rows without factions serve every faction without its own. Each row's `fleet_share` scales the boss size for
the boss's fleet: the whole boss, or the station's guard.

A station boss is a vanilla station-mode fleet with no market (`VanillaChartContentEngine.createStation`) orbiting
the boss's spot, and its guard holds a passive orbit beside it, so engaging either pulls in the other. The station
is the objective target and carries the boss loot. Station rows whose variant the game has not loaded are dropped
at load, and a station that still fails to spawn is replaced by a full-size fleet boss (`ChartContentGenerator`).

## Chart expansions

A chart's `CHART_<EXPANSION>_CHANCE` lines sum into a per-expansion chance in `ChartGenContext`, rolled on its
own seed per expansion when the content is planned (`ChartExpansions.planned`), and only for ids that have a row in
`chart_expansions.csv` in tier range with their `requires_mod` loaded. Each planned `ChartExpansion` (package
`charts.expansion`) places its entities after the sites and fleets, and the run record keeps the ids that placed.

The Spatial Breach (`SpatialBreach`, `BreachTerrainPlugin`, numbers in `BreachRules`) is a terrain field fixed at its
core that starts closed. Once the player fleet flies over the core it eases open to a tier-based maximum over 60
seconds (faster with the growth modifier), then collapses over 60 seconds and removes itself, its core and its unlooted
caches. Every 0.1 days fleets inside lose sensor range and burn level, and the player fleet also burns fuel while its
ships use more supplies (`BreachSupplyBuff`), all stronger toward the core. Caches planned at creation form once the
field reaches them and vanish once the collapse passes them, with item rarity raised by depth, and content sites it
covers get a smaller rarity bonus (`StarChartLootSource.modifiersFor`). Closing the core early removes the field and
its unlooted caches and drops salvage and items scaled by the largest size it reached (`BreachCoreDialog`).

## The atlas

Each save has one atlas: `AtlasState` keeps the seed and per-system flags in persistent data, and `AtlasGenerator`
rebuilds everything else from the seed. A chart opens a system joined by a lane to a completed one (`AtlasReach`), and
the system supplies the layout seed, faction, region recipe, phenomenon, a forced expansion and the lines from
`atlas_node_effects.csv` for Powerful Boss, infested, cleansed and special-site systems, kept on the run as an
`AtlasRunNode` (`AtlasOpening`). A Powerful Boss always drops a chart one tier up, and chart drops inside a run never
fall below its tier.

Killing the objective completes the system and grants an atlas point for the first clear of each tier (`AtlasProgress`).
A Sensor Array also reveals 6 lanes, a Nexus cleanses the systems it infested, and a Bastion gives a key fragment and
allows one attempt. A run that closes without its objective spends the system's markers (Powerful Boss, expansion,
overlay phenomenon). Biomes tile the map from sites about five cells apart, and each sets how many of its cells hold a
system (`AtlasBiomes`). Normal space wins most sites and chains into the volumes factions hold, while a special biome
never sits beside another site of its own type, so it stays a small pocket. Each biome lists the only factions it holds
(the derelict graveyard holds only derelicts), and normal space's list is the pool every faction territory draws from. Hyperspace clouds are impassable terrain clumping inside biomes and ridging along some
biome borders: no system sits under one and lanes route around them, and a grid of cleared corridors keeps the
Gateway and every Bastion joined to the rest of the map (`AtlasTerrain`). A biome or Threat infestation behind
an unlock is decided when first seen, and that is saved.

## Modspec caches

The Contraband Modspec Cache (`contraband_modspec_cache`, kind `MODSPEC_CACHE`) is a corrupted unique
subroutine that rolls one to three notables at creation. In an allocated standard socket they are allocated
with no path and no cost. Its kind keeps it out of generic pools, it has no level gate, and NPCs never
socket one.

## Wormhole anchors

An anchor drops, stocks or shows in showcase salvage only while it is obtainable
(`WormholeAnchorDefinitions.obtainable`, sharing its checks with `WormholeAnchor.inertness`): the player has the
Janus Device (`JanusDevice`, `AnchorJanusGate`, lifted by the "Allow Wormhole Anchors Before the Janus Device"
setting, Hyper-Nav Beacon exempt), its own unlock is met, and no closed `withheld` gate holds its exit. Each gate
in `node_gates.json` sets `anchors` (`withheld` by default, or `free`) and `rewardAnchor`. Nightcross is `free`
and opens on `anchorFound` once any of its anchors reaches the player (`AnchorFinds`). The Singularity and Existential
Threat battles add one area anchor to their loot once per save (`GateRewardAnchors`). NPC anchors ignore all of
this, and open wormholes stay open.

An anchor whose factions are all missing, or whose playable factions Nexerelin has all eliminated, drops from the
salvage and tech mining sites in `anchor_salvage.csv` instead (`FactionlessAnchors`).

## Corruption

A Corrupted Nanoswarm locks the item and picks one equally likely outcome from its list in `CorruptionOutcome`
(`ItemCrafting.corrupt`). A replaced hull item implicit comes from `hull_item_corruption_implicits.csv` or any
section's implicits (`ItemImplicits.corruptionPool`). Unique hull items can also become a rare of their section's base with 3 to 4
modifiers, keeping their implicits. Subroutines add an implicit from `subroutine_corruption_implicits.csv`
instead of replacing one, and their rare outcome only corrupts them.

## Hull upgrade points

`HullUpgradePoints` decides a player ship's points from the LunaLib "Hull Upgrade Points" setting; NPC ships
have none. With Refit Schematics a ship has its stored points (`ShipSkillData.frameworkPoints`), at least 1. A
schematic only applies to a ship with exactly one point fewer than its tier (Scrapped 2, Standard 3, Military
4, Domain 5) and sets the points to that tier (`HullUpgradeRules.applySchematic`). With Star Chart Progress
every ship has 1, plus one each once the highest cleared chart tier reaches 5, 10 and 15, schematics stop
dropping, and stored points are kept for switching back. Switching never locks an unlocked section.
