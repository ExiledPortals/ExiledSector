# Exiled Sector: mechanics

How the non-obvious effects work. Magnitudes live in `data/skilltrees/skill_types.json` and are placeholders.

Rule for anyone editing this file: one or two short paragraphs per section, less is better. Say what the
effect does and where the code lives. Leave out edge cases, rationale and anything the code already makes
obvious. Shorten an existing section rather than grow it. Describe only what is built, never planned or
missing features. Put a new section, as a `###` heading, inside the
`<details>` block of the domain it belongs to.

<details>
<summary><b>Skill tree</b></summary>

How tree effects are totalled and applied to a ship.

### Affinity

Roots and `affinity` nodes grant Low Tech, High Tech or Midline affinity (`*_AFFINITY_FLAT`), which
apply no stat themselves. `EffectTotals` sums each ship's permanent affinity first and turns it into tree MULT
bonuses for every whole step of 5, 10, 15 or 20, from the one table in `AffinitySteps`, so diminishing returns, NPC
scaling and the summary see the stat bonuses. `ModuleTreeShare` steps the shared nodes' affinity the same way.

Effects backed by `AffinityScaling` (`*_PER_*_AFFINITY`, on the affinity hull items) apply nothing themselves:
`AffinitySteps.scaledBonuses` adds their target stat once per whole step of the affinities they read, summed when
they read all three, in a contribution of its own so module sharing is unaffected. `AffinitySteps.totals` first
multiplies the summed Low Tech Affinity by its summed `LOW_TECH_AFFINITY_PERCENT`, then adds
`HIGHEST_AFFINITY_FLAT_PER_S_MOD` (already multiplied by the ship's s-mods in `HullUpgradeEffectSource`) to the
highest affinity, then `LOWEST_AFFINITY_AS_HIGHEST` raises the lowest to match it, and last each `AFFINITY_EXCHANGE_TO_*`
empties the other two affinities (never below zero) into its own at its share. With `STRIKECRAFT_GAIN_AFFINITY_BONUSES`,
`AffinityStrikecraft` gives launched strikecraft every step and scaled affinity bonus except logistics ones (and
strikecraft ones, which already reach them), rescaling their already built hull and armor. `ModuleTreeShare` gives modules
no bonus the parent exchanged away.

### Diminishing returns

`EffectTotals` shrinks permanent tree and hull item bonuses to armor, hull, flux capacity, flux dissipation,
top speed and weapon range once their net total passes a LunaLib threshold, keeping only the efficiency
percent of the excess (`DiminishingReturns`, `DiminishedStat`). Thresholds are percent of the hull's base
value (`StatBaseline`), and flat bonuses count as the percent they add. Range counts percent and MULT per
weapon type, cutting broader scopes only as far as every type they reach allows. Drawbacks, temporary nodes
and vanilla hull mods passed through by nodes are untouched.

### Temporary nodes

`temporaryAfterDeploymentSeconds` applies a node's effects only for that long after deployment. Only effects
on stats the game reads live in combat opt in. Anything else (creation-time stats, campaign stats, listeners,
conditional effects) makes the loader log an error and ignore the duration.

### Module effects

The tree applies only to the parent ship. On the parent's first combat frame `ModuleTreeShare` gives each
living child module a share of the parent's shareable tree effects (`ModuleShareSetting`, 50% by default, 100% for stations), plus
the `MODULE_*` effects at full strength.
`ModuleBoost` applies both, then rescales the module's maximum hull, armor cells, shield arc (up to 360) and
each weapon's maximum ammo once; cells above their maximum are clamped first so armor carried from an earlier
engagement isn't raised again.
A ship docked into another's module slot (Arma Armatura's core units) keeps its own tree through the ship tag its variant carries:
`ShipRecords.resolve` lends it to any member outside its fleet's list, refit module tabs included, it gets no share of the
parent's tree, and `SocketCustody.addDockedShips` counts it as owned so its items stay socketed.

### Ship mass

`SHIP_MASS_PERCENT` pools in a dynamic stat and `StatTarget.ShipMass` sets the ship's mass after creation, from the
mass it was built with (kept in custom data), never below 10%. Ram damage dealt scales with the rammer's mass, while
damage taken depends only on the other ship's, so it raises ram damage without making rams cost more.

### Phantom hull mods

A node that recreates a vanilla hull mod also places the real one as a zero-OP perma mod tagged
`exiledSector_installed_<id>`, so other mods' `hasHullMod` checks see it. `PhantomHullMods.install` wraps
each such hull mod's effect in `PhantomHullModEffect`, which forwards to the original except on tree-placed
copies, where it does nothing, can't be removed and is hidden from refit and Codex. Fleet-wide hull mods
(High Resolution Sensors, Phase Field) are never phantoms.

### Hull mod conflicts

`data/config/exiledSector/hullmod_conflicts.csv` lists hull mods that can't share a ship: each row names a hull
mod and its `;`-separated partners, and a pair counts both ways. The game merges the file across mods by the
`hullmod` column, so another mod adds its own rows (a row with the same key replaces ours), and `HullModConflicts`
reads it in `SkillTypeLoader`. A node excludes the partners of the hull mods it places (`vanillaHullMod`,
`phantomHullMods`); a node that places none names the hull mods it stands in for in `exclusiveHullMods` and
excludes their partners instead. Two nodes whose hull mods are partners exclude each other.

`NodeEligibility` refuses a node when an excluded hull mod is on the ship or comes from another allocated node,
for the player and NPC builds alike. Most rows are other mods' hull mods that remove vanilla ones in code, which no
data shows. `SkillTypesDataConsistencyTest` rejects pairs listed twice, rows without notes and rows naming no
node's hull mod.

### Phantom hull mod conflicts

`HullModConflictResolver` removes a conflicting hull mod that turns up after the node. A hull mod that tries to remove a phantom
through MagicLib has its cause removed instead. When the conflict is built-in, permanent or an S-mod,
`PhantomConflictWatch` refunds the node instead, as below.

Unremovable conflicts (mostly hidden built-in hull mods) are learned at runtime by `PhantomConflictWatch`,
from a MagicLib removal record or a phantom vanishing while its tag stays. `LearnedPhantomConflicts` keeps
them forever in `exiledSector_learnedHullModConflicts.json` in the common folder. From then on the phantom
isn't placed, `NodeEligibility` refuses the node with `LEARNED_CONFLICT`, and player ships get the node and
its dependants refunded like a respec.

</details>

<details>
<summary><b>Weapons and damage</b></summary>

Weapon stats, weapon mounts and effects that change or add to hits.

### The weapon stat hierarchy

Names follow `<SCOPE_>WEAPON_<STAT>_<FLAT|PERCENT|MULT>`, with scopes ALL > BALLISTIC / MISSILE / ENERGY and
ENERGY > NON_BEAM_ENERGY / BEAM; unsupported combinations are rejected at load. FLAT and PERCENT from every
scope reaching a weapon pool additively. The same MULT across nodes adds into one multiplier (floored at 100%
less) that multiplies with the rest. Beam flux cost is the exception: it compounds with energy flux cost
(-10% and -10% gives 81%).

Where the engine lacks a stat: NON_BEAM_ENERGY damage and range write the energy stat plus a hidden
cancelling beam entry, and beam or non-beam energy ammo is written per weapon at ship creation (right in
combat, missing from refit tooltips, not usable on temporary nodes). Turn rate exists only for all weapons
and for beams. Tooltips merge identical child effects on one node into the parent line.

### Weapon range cap

`NON_MISSILE_WEAPON_RANGE_CAP_FLAT` uses the engine's range threshold, as Safety Overrides does, and multiplies range
past it by zero. It runs after the other effects and again once the ship is built, setting its flat so the summed
threshold becomes the lower of the cap and any threshold the ship already has.

### Weapon slot conversion

Slot effects such as `BALLISTIC_SLOTS_HYBRID` (Dual-Feed Mounts) are rules: a slot type, an optional size and a new
type (`SlotConversionBacking`). `SkillTreeHullMod` collects the rules of every tree node and hull item in order and
`WeaponSlotConversion` gives the variant its own copy of the hull spec with each weapon mount retyped by the matching
rule whose type takes the most weapon types (universal, then hybrid, synergy or composite, then single types; the
earliest rule among equals). Rules read the original type, so conversions never chain. Saves keep only the hull id,
so the copy is rebuilt each time hull mods apply; slots swap back once their rule is gone, and `UnfitWeapons` returns
weapons that no longer fit to cargo. After a tree edit, `ShipEditSession` swaps the refit screen's own variant at once and `RefitShipDisplay`
rebuilds its slot buttons. The ship summary names mount types where a losing rule's type takes a weapon type the
winner's doesn't.

### Ballistic counts as energy

`BALLISTIC_COUNTS_AS_ENERGY` makes `BallisticEnergyMirror` copy every energy and non-beam energy
weapon effect from the ship's sources into a ballistic one before diminishing returns. Energy checks in Dynamic Grid,
energy range per sensor strength, Disintegration, Ion Arc and refractions also accept ballistic weapons
(`BallisticAsEnergy`). Vanilla and other mods' energy bonuses don't reach ballistics.

### Damage conversion

Conversion effects (`DamageConversion`, read by `DamageConversionListener`) turn all damage from weapons in one kind of
mount, or from beams of one damage type, into another type: mount type (after slot conversion), size and hardpoint or
turret. The first owned conversion
matching a mount applies. Shots from weapon drones use the mount of the weapon they copy. Beams read their original
type from the weapon each tick, since their damage object lasts the whole firing.

A conversion to EMP moves the hit's whole damage into EMP. `HitDamageRestorer` makes the change and restores the hit
once it lands; refractions and Phase-Entangled Wings use the same restorer, so their changes stack on one hit, and Lion's
Gaze cuts its simulated hits from the original damage.

### Refracting Projectiles

`NON_BEAM_ENERGY_WEAPON_REFRACTION_CHANCE_PERCENT` gives a non-beam energy shot that hits a shield a chance to
refract to the nearest unhit enemy within weapon range, up to Max Refractions (default 5). Missiles and
weapons in `energy_chain_blocklist.csv` never refract. As with Lion's Gaze, a pooled drone fires the real
weapon as an instant single shot, then hands the projectile to the firing ship so its on-hit effects and
kill credit apply.

`NON_BEAM_ENERGY_WEAPON_REFRACTION_FALLOFF_PERCENT` lowers each refraction's base damage and restores it after the
hit, rather than using a modifier, so EMP and on-hit effects stay at full strength.

### Projectile echoes

`NON_MISSILE_PROJECTILE_ECHO_CHANCE_PERCENT` (`ProjectileEchoListener`) rolls on each hit an enemy ship takes from
this ship's non-missile, non-beam projectiles. On success the same weapon spawns another copy of its projectile,
aimed at the target, costing no flux or ammo. Echoes are tagged and never echo again.

### Disintegration

`DISINTEGRATION_ARMOR_DAMAGE_PERCENT` (energy hits) and `BALLISTIC_DISINTEGRATION_ARMOR_DAMAGE_PERCENT` (ballistic
hits, allies included) queue that share of a hit's damage on a 5 by 5 patch of armor cells around the impact, on top of the hit
itself. Each target strips its queued armor evenly over 5 seconds (`ArmorDecay`). `ARMOR_BURN_DAMAGE_PERCENT`
(`ArmorBurnListener`) queues its share of each weapon hit on an enemy's armor on the struck cell alone, over 3 seconds, so
burns on one cell add up.

### Thermo-Accelerator

The flux cost reduction is an ordinary ship-wide weapon stat, and `ThermoAcceleratorListener` stops every hardpoint weapon from firing
each frame, so only turrets benefit. Decorative and system slots are left alone.

### Shrouded damage and healing

Shrouded Lens Damage and Rift Lightning Damage raise the damage of explosions the ship sources, picked out by their
vanilla sound set; the Lens also needs vanilla's no-friendly-fire collision class, which keeps Abyssal Glare out
(`ShroudedDamageListener`). Hungering Rift Healing adds percentage points to vanilla's `hungering_rift_heal_mod`, the
share of a Hungering Rift heal the ship receives.

### Missile snare

`MISSILE_SNARE_TOP_SPEED_PERCENT` multiplies the top speed of an enemy ship hit by this ship's missiles for 2 seconds,
refreshed by each hit, capped at 90% less. As with shield disruption, the strongest source applies
(`MissileSnareListener`).

### Kill flux vent

`KILL_FLUX_VENT_PERCENT` watches every enemy ship this ship damages and, when one's hull reaches zero, removes that
share of maximum flux from whichever ship dealt the killing blow, if it has the effect. Strikecraft and station
modules don't count (`KillFluxVentListener`).

### Flak bursts

`MISSILE_KILL_FLAK_DAMAGE_FLAT` remembers each enemy missile this ship's weapons hit and, on the next frame, bursts
at any whose hitpoints reached zero. The burst deals flat fragmentation damage to enemy missiles and strike craft
within 100 su, and missiles it destroys don't burst again (`FlakBurstListener`).

### Concussion Shells

A large ballistic projectile hit on an enemy ship's armor or hull rolls `LARGE_BALLISTIC_HIT_DISABLE_CHANCE_PERCENT`
and temporarily disables whichever working weapon or engine sits nearest the impact (`ConcussionShellListener`).

### Auxiliary Screen

`SMALLER_ALLY_DAMAGE_TAKEN_PERCENT` multiplies the hull, armor and shield damage taken by allied ships of a smaller
hull size within 800 su, rechecked every 0.25 seconds. Strike craft and station modules don't count, and only the
strongest source applies (`AuxiliaryScreenField`).

### Defensive Compact

`DAMAGE_TAKEN_NEAR_HELD_OBJECTIVE_PERCENT` reduces hull, armor and shield damage taken while any combat objective
owned by the ship's side is within 1500 su, checked every frame (`HeldObjective`).

### League Requisitions

`SUPPLY_USE_REDUCTION_PERCENT_PER_HULL_TYPE` adds up over the player fleet's active ships and is multiplied by the
number of different base hulls among them, so skins count as their base hull and mothballed ships don't count. The
total, capped at 15%, reduces every member's maintenance supply use at each fleet sync
(`FleetWideEffects.applySupplyUsePerHullType`).

### Lion's Roar

`RATE_OF_FIRE_AURA_PERCENT` multiplies the ballistic, energy and missile rate of fire of enemy ships within 1000 su,
rechecked every 0.25 seconds. Strike craft don't count, and only the strongest source applies (`RateOfFireAuraField`).

### Honor Guard

`TOP_SPEED_SHARE_OF_FLAGSHIP_PERCENT` adds a flat share of the flagship's hull top speed, before any modifiers, while
that flagship is alive on the field, rechecked every second. The flagship is the ship captained by this ship's fleet
commander, so the flagship itself gains nothing (`FlagshipPaceListener`).

### Executor's Decree

`NO_RETREAT` cancels the ship's retreat whenever it starts, whether from a retreat order, a full retreat or its own AI,
and gives it a free search and destroy order instead (`NoRetreatListener`).

### Strategic Fuel Reserve

`FUEL_USE_REDUCTION_PERCENT_WHILE_HALF_FUELED` reduces only the fuel use of the ship that has it, while the player
fleet's fuel is at least half its total fuel capacity. A transient script rechecks it every 0.25 seconds
(`FleetWideEffects.applyHalfFueledFuelUse`, `PlayerFleetConditionScript`).

### Range per sensor strength

`ENERGY_WEAPON_RANGE_PER_SENSOR_STRENGTH_FLAT` applies after every other tree effect and again once the ship is built,
so sensor strength from the tree, such as COMSEC Discipline's, and from hull mods counts toward it
(`WeaponSkillEffect.applySensorRange`).

</details>

<details>
<summary><b>Beams</b></summary>

### Lion's Gaze

A beam hit on an enemy ship splits to up to N more targets (`BEAM_WEAPON_SPLIT_TARGETS_FLAT`): the nearest
hittable hostile non-fighters within half the beam's range of the impact, picked again every 0.2 s or when one
dies. Modules of the hit ship, or the ship its hit module belongs to, are skipped unless it's a station. Damage is
shared evenly. The
original target's share is a damage modifier (so it also scales that hit's EMP), and the split runs before
every other damage-dealt effect, so they all see the shared amount.

Each split target is hit by a real beam from an invisible, invulnerable drone placed just outside the
original target's shield or hull. The drone copies the ship's captain, weapon stat modifiers and damage-dealt
listeners, and fires for at least 1 s, then while the original beam hits plus 0.3 s. Drones carry no hull
mods except pure markers listed in `drone_marker_hullmods.csv` (shared with Refracting Projectiles). Weapons
in `split_beam_effect_blocklist.csv`, or ones a drone can't carry, fall back to simulated splits: per-tick
instant damage plus a MagicLib fake beam, without the weapon's effect code.

### High Scatter Amplifier

`BEAM_WEAPON_HARD_FLUX_PERCENT` converts that share (capped at 100%) of a beam hit's shield flux from soft to
hard on the target, friendly fire included, never above its total flux. Beams that already force hard flux are skipped.

### Beam shield disruption

`BEAM_WEAPON_SHIELD_DISRUPTION_PERCENT` works like vanilla's Graviton Beam: a beam hit on any shields, allied ones included, multiplies the
target's shield damage taken from every source for 1 second, refreshed by each hit. Several sources don't stack; the
strongest one applies (`BeamShieldDisruptionListener`).

</details>

<details>
<summary><b>Shields</b></summary>

### Shared Fate

Shield hits are reduced by the summed `SHIELD_DAMAGE_SHARED_PERCENT` (capped at 90%) while a living,
non-fighter ally within 1000 su is not overloaded or venting. The flux the removed share would have caused,
at this ship's shield efficiency, goes to those allies as hard flux, split evenly and uncapped, shield or
not. Armour and hull damage are never shared. The notable's -20% weapon damage pools through the weapon stat
hierarchy.

### Soft flux shields

`SHIELD_SOFT_FLUX_CONVERSION_PERCENT` is the engine's own shield soft flux conversion stat, used by the Hardlight Veil
and the Low Arc core. `SHIELD_DAMAGE_PASSED_TO_ALLY_PERCENT` shares Shared Fate's listener and 90% cap, sending its
share to the nearest qualifying ally with flux room as soft flux; what that ally can't hold returns as hard flux.
`SHIELD_ARC_FIXED` sets the shield arc to its magnitude in degrees after the ship is built, since arc is only read then.

</details>

<details>
<summary><b>Survival</b></summary>

### Displacer Failsafe

`DISPLACER_FAILSAFE_HULL_PERCENT` (`DisplacerFailsafeListener`) watches damage taken. The first time hull drops
below the threshold in a deployment, the ship is moved 1200 su away from the nearest enemy ship, kept inside the map
and turned up to 90 degrees to land clear of other hulls, its velocity zeroed, and it vents unless it is already
overloaded or venting.

### Rise From the Ashes

`RiseFromTheAshesListener` cancels the first hull hit that would kill the ship, sets its hull to a share of
maximum, restores armor and applies a damage taken multiplier for 6 s. It happens once per fleet member per
battle, tracked in the combat engine's custom data, so redeploying doesn't reset it. A ship's lethal hits go
through one `special/LethalHitArbiter`: Ludd's Light detonates on any lethal hit, even one a dive or Rise
then saves, then Rise From the Ashes and then the Phase Anchor dive try to save the ship, and the first that
does wins.

</details>

<details>
<summary><b>NPC fleets</b></summary>

### Generated NPC trees

`NpcSkillTreeBuilder.generate` builds each tree deterministically from the sector seed, fleet id and member
id and stores it in an `NpcTreeTag`. The design type picks the root. Removable hull mods with an equivalent
node are stripped and their OP buys those nodes. If `npc_faction_volumes.csv` gives the faction a volume, the
ship takes one notable or keystone there through its faction's real wormhole anchor (`NpcItems.anchorFor`),
socketed only if it allocates a node behind it. NPC ships carry nothing else, the anchor never drops, and a
recovered ship loses it.

The rest of the budget goes to notables and keystones drawn by seeded weight (`NpcRelevance` × tier), then
small nodes. `NpcRelevance` multiplies theme tag weights by `npc_faction_focus.csv` and
`npc_ship_system_focus.csv` (by ship system or defense id, `NpcFocus`), which also steers choice nodes'
option picks. Every step is a breadth-first search through nodes `NodeEligibility` allows, never crossing
wormholes. Shield Shunt and Makeshift Shield Generator need `req_unscripted_hull`, so NPCs never take them on a
hull whose built-in hull mods or ship system come from another mod (`ShipProfile.scriptedHull`).

Station fleets (market stations and the Remnant Nexus) are levelled like any other fleet while
`NpcTreeConfig.levelsStations` is on, and the station is always chosen. A station's `ShipProfile` counts its
modules' fighter bays, and `NpcStationNodes` stops it aiming for nodes or options whose every effect is movement,
phase, ship system, or fighter without bays.

### NPC levels

An NPC's level is its node count besides the root. Open-world ships roll 80% to 120% of a random player combat
ship's tree power (`NpcTreeSize.roll`), capped by category in `NpcLevelCaps`: `npc_levels.csv` first, then
AI-core commander (35), pirate flag (15), otherwise faction (25). The cap is stored per ship as a variant tag,
because battles merge fleets. Chart fleets are uncapped.

In fixed mode (`mode` column, else the LunaLib "NPC Level Mode") a ship instead rolls between `min` (else the
LunaLib percent of its cap) and its cap (`NpcLevelLimit.rollFixed`). Faction rows win over category rows. A
`keep_tree` of `wipe` is tagged on the ship and `ShipTreeSync.adoptNpcTree` then leaves a recovered ship blank;
without the column the LunaLib "Recovered Ships Start Blank" decides.

#### Bounties

`NpcBounties` turns a bounty fleet into a 0 to 1 difficulty that raises the cap from the category cap towards
25, and the flagship gets 3 more levels. Person bounties always use the pirate category.

#### Chart fleets

Chart ships roll a level in their tier's `level_min` to `level_max` from `star_chart_tiers.csv`, scaled by
Fleet Strength %, and bosses roll one higher. Outside charts, a won battle's chart chance is `chart_drops.csv`'s
battle chance per 100 enemy deployment points defeated, scaled by battle difficulty, at the highest tier whose
`level_min` the strongest defeated NPC reached (`ChartDrops.forBattle`). With the skill tree switched off, the
tier instead rises one per `defeated_dp_per_tier` deployment points defeated. Chart fleets are built with
`doNotPrune`, so vanilla never trades their fleet points past 30 ships for bigger hulls (`ChartContentGenerator.fleetParams`).

Node-granting lines such as Freebooting store node ids with the fleet's enemy rules. An enemy ship that gets an NPC tree
has the node placed first as a free grant; the builder avoids nodes that conflict with it and only paths through it
once its own path reaches it (`NpcSkillTreeBuilder`, kept in the tree tag by `NpcTreeTag`). A recovered ship loses the
grant. Every other enemy ship, and all of them while the skill tree is switched off, takes only the node's own effects
(`SkillTreeGrantedNodeEffects`). Hulls the node isn't allowed on get nothing.

</details>

<details>
<summary><b>Hull upgrades</b></summary>

Hull upgrade points, the tree unlock and hull items socketed into ship sections.

### Hull upgrade points

`HullUpgradePoints` decides a player ship's points from the LunaLib "Hull Upgrade Points" setting; NPC ships
have none. With Refit Schematics a ship has its stored points (`ShipSkillData.frameworkPoints`), at least 1. A
schematic applies to any ship with fewer points than its tier (Scrapped 2, Standard 3, Military 4, Domain 5)
and sets the points straight to that tier, skipping any between (`HullUpgradeRules.applySchematic`). With Star Chart Progress
every ship has 1, plus one each once the highest cleared chart tier reaches 5, 10 and 15, schematics stop
dropping, and stored points are kept for switching back. Switching never locks an unlocked section.

### Master Schematics

With the LunaLib setting "Require Master Schematics" on (and Hull Upgrades on), a player ship's tree stays shut until a
Master Schematic is used on it on the Hull Upgrades panel (`HullUpgradeRules.applyMasterSchematic`, flag
`ShipSkillData.skillTreeUnlocked`). `MasterSchematic.treeOpen` gates the tree's effects, phantom hull mods and the OP
reserve. A locked ship keeps earning XP. Switching the setting on unlocks every ship with a node besides its root, and a
save with no unlocked ship gets one Master Schematic once (`MasterSchematicActivation`). They drop from battles and from every
site roll (salvage, Tech Mining, chart sites and chart clears), which reaches hull loot through
`HullUpgradeLootSource.addSiteFinds`.

A locked ship counts as 0 tree power in `PlayerTreePower`, and an open-world NPC ship that rolls 0 nodes gets no tree, so
the share of NPC ships with trees follows the share of unlocked player ships. Chart ships are levelled only at their tier's
`tree_share` in `star_chart_tiers.csv` (bosses always). A captured NPC ship keeps its tree but arrives locked.

### Battle loot

Battle drops come from `battle_loot.csv`: each row expects its rate per 100 deployment points an enemy fleet lost,
curved over the whole battle (linear, or diminishing past 2000 DP with the square root), then times battle difficulty
capped at 3x, weighted by the row's `difficulty_scaling` (1 full, 0 ignored; crafting items ignore it), and the
fleet's `FleetLootRules`. Fleets outside Star Charts get the row's `regular_share`, and core
rolls their crafting materials per battle (`BattleMaterials`). A chart fleet gets the full rate, its run's loot
multipliers and, for the boss, its spoils multiplier as extra DP; `ChartFleetLoot` rolls its materials from the DP its
ledger still owes, so rebuilt salvage defenders can't be farmed. Endlessly launched station fleets
(`FEEDSTOCK_ONLY_MEMORY_KEY`) drop only base feedstock.

Hull items read the hull rows (`BattleItemDrops`): an enemy ship's tree raises the share rolled with two modifiers
(`levelBoost`), never the count. Chart fleets roll subroutines from the `subroutine` row, and star charts and
catalysts from `chart_drops.csv` and crafting items from `chart_materials.csv` (set at tiers 1 and 15,
linear between, `ChartMaterials`) as a whole-chart amount, each fleet taking its fleet points at spawn over what its
tier spawns (`ChartFleetLoot.expectedFleetPoints`). Fleets an anomaly placed carry its id
(`ANOMALY_MEMORY_KEY`) and drop all of this times its `fleet_loot` in `chart_anomalies.csv`
(`ChartFleetLoot.anomalyFleetLoot`), except the boss. Each dropped chart rolls its tier from the row's `minus_2` to
`plus_2` weights around the run tier (`ChartDrops.dropTier`). An anomaly's own fleet loot (`LootShaper.fleetItems`)
comes on top.

### Hull item drops

Hull items share `item_salvage.csv` with subroutines but keep only half its rolls: each item of the site cascade, the
targeted extras and a unique hull item from the site's unique roll survive at `HullItemDrops.SALVAGE_SHARE`
(`ItemTypeSupport.salvageShare`). A site's `hullItemShare` scales its basic items and targeted extras on top, so the
chart boss's chest and the chart reward (0) drop only uniques. Battle drops (`battle_loot.csv`) and market stock (`market_hull_items.csv`)
carry their own rates. A dropped non-unique hull item has one or two modifiers
(`HullItemDrops.DROP_LIMITS` through `ItemTypeSupport.dropLimits`), storm-touched loot included, so it never drops rare;
crafting can still take it to four.

### Hull item mechanics

A hull item with a targeting effect rolls one other section as its target (`HullItem.targetSection`, from its seed,
stored once rolled); the Extended Warranty Contract's 100% doubles that item, drawbacks included. The Tuning
Nanoswarm rerolls the target (`Item.tuneTrait`). On load a unique item follows its definition's current pool
(`ItemDefinition.withCurrentUniquePool`): it rolls modifiers added since it dropped, from its seed, and loses those
taken out. A modifier whose effect no longer loads is kept.

A unique whose `mechanic` is `one_of` keeps just one of its listed modifiers, picked from its seed when it is
rolled (`ItemRoller`), so the choice is frozen with the item.

### Crewed and automated ships

A ship is automated when vanilla's `Misc.isAutomated` would say so: the `automated` hullmod or the `automated` tag
on its variant or hull spec (`ShipProfile.automated`). Crew Quarters needs `req_crewed`, so an automated ship can't
unlock it, and one that already did keeps it inactive until it is locked again.

A hull item's `crew` column is `crewed`, `automated` or blank. `HullItem.requirementTag` turns it into `req_crewed`
or `req_automated`: `HullUpgradeRules.canSocket` refuses the wrong kind of ship, and `ShipSections.of` marks an item
already socketed on one inactive, like an unmet section.

</details>

<details>
<summary><b>Items and subroutines</b></summary>

Drops, crafting and the socketed items that change a tree.

### Unique drop sources

A unique's `source` column (`UniqueSource`) can name `site:` and `faction:` sources. A unique with a site source
drops only from those salvage sites' unique rolls and never from battles or transposition. A faction source
restricts nothing: in a battle where that faction lost the most deployment points, the unique's drop weight is
doubled (`ItemDrops.pickUniqueFromFaction`, `FACTION_SOURCE_WEIGHT_MULT`).

### Item volumes

A hull item or subroutine can name a `volume`, one of the toggled tree volumes. `ItemVolumes` asks the skill tree's
`VolumeToggles`, bound at application load: a definition whose volume's mod isn't installed is never loaded, so its
faction sources are never checked, and one whose volume is switched off stays loaded but is left out of every drop
pool (`ItemDrops`, `ItemUnlock.canDrop`) and of the Metamorph Nanoswarm.

### Exclusive modifiers

A modifier row's `exclusive_groups` column names groups, and an item never carries two modifiers that share one
(`ModifierExclusivity`). Rolls, rerolls, crafting adds and replacements skip a modifier that clashes with one already
there, and a granted modifier that clashes with a kept one is refused. Unshielded, Phaselocked and Brittle charts share
`crippled_defense`, so a chart leaves at least one defence working, and Jammed and Severed share `command_point_rate`.

### Advanced search

The hull items panel's advanced search compares a stat's total on each item: every line of that stat, hybrid lines
included, from the implicits and explicits together, summed as the item shows them, rounded to the shown decimals and
signed, so a reduction is negative (`ItemStatTotals`, built once per storage row). A line that scales with hull size
gives one total per hull size, and the row matches if any of them is in range. An item without the stat fails the row
even when min and max are blank.

An AND group needs every row, a COUNT group at least its count, and a row or group with no stat picked is ignored
(`ModifierSearch`, kept on the session's `ItemStorageFilter` and applied only when Search is pressed). The stat list
narrows to the stats the chosen socket types can carry (`ItemStatPool`), read from core `ModifierParts`: each part's
pools, unique items and implicits, the corruption pool on every part, and the parts biome, engram and Storm Front
sources reserve their modifiers for (`ModifierSources`).

### Wormhole anchors

An anchor drops, stocks or shows in showcase salvage only while it is obtainable
(`WormholeAnchorDefinitions.obtainable`, sharing its checks with `WormholeAnchor.inertness`): the player has the
Janus Device (`JanusDevice`, `AnchorJanusGate`, lifted by the "Allow Wormhole Anchors Before the Janus Device"
setting, Hyper-Nav Beacon exempt), its own unlock is met, and no closed `withheld` gate holds its exit. Each gate
in `node_gates.json` sets `anchors` (`withheld` by default, or `free`) and `rewardAnchor`. Nightcross is `free`
and opens on `anchorFound` once any of its anchors reaches the player (`AnchorFinds`). The Singularity and Existential
Threat battles add one volume anchor to their loot once per save (`GateRewardAnchors`). NPC anchors ignore all of
this, and open wormholes stay open.

An anchor with `guaranteed_drop` in `wormhole_anchors.csv` drops from every won battle that defeats one of its
factions instead of rolling its chance (`FactionFleetDrops`); the Shrouded anchor does this for Dweller fleets. An anchor whose factions are all missing, or whose playable factions Nexerelin has all eliminated, drops from the
salvage and tech mining sites in `anchor_salvage.csv` instead (`FactionlessAnchors`).

### Corruption

A Corrupted Nanoswarm locks the item and picks one equally likely outcome from its list in `CorruptionOutcome`
(`ItemCrafting.corrupt`). A replaced hull item implicit comes from `hull_item_corruption_implicits.csv` or any
section's implicits (`ItemImplicits.corruptionPool`). Unique hull items can also become a rare of their section's base with 3 to 4
modifiers, keeping their implicits. Subroutines add an implicit from `subroutine_corruption_implicits.csv`
instead of replacing one, and their rare outcome only corrupts them.

Star charts add two outcomes (`StarChartCorruption`): one tier up, reaching tier 16 from 15, or a reroll with 8 modifiers,
4 per side. Tier 16 (`ChartTiers.CORRUPTED_TIER`) has its own row in `star_chart_tiers.csv` but is reached only this way.
Chart drops, the Cartography Cipher and combining stop at 15, and a tier 16 clear records as tier 15 (`ItemUnlock`).

### Socketed item scaling

A contribution whose origin is a `SocketedItem` belongs to a socketed item of one category, hull item or subroutine.
`SocketedItemScaling.withScaledItems` runs first in `EffectTotals.of`: it sums the permanent scaling percents of each
category from every source and multiplies that category's item magnitudes by them, never scaling the scaling effects
themselves. Per-section effects such as `SHIELD_GENERATOR_ITEM_EFFECT_PERCENT` are `SlotItemEffect`s, which
`HullUpgradesDomain` registers for every `ShipSection`.

Hull items carry a `HullItemOrigin`. `SkillTreeEffectSource` splits a subroutine's lines, the Root Conduit's affinity
and the Empty Shell affinity into a `SubroutineOrigin` contribution, and gives augmenting Doctrine lines and gated nodes a
`RadiusLineOrigin`, so a gating line's strength scales too. Nodes a subroutine grants or transforms keep their own
values. `nodeEffects` folds both origins back into their node for module sharing.

### Modspec caches

The Contraband Modspec Cache (`contraband_modspec_cache`, kind `MODSPEC_CACHE`) is a corrupted unique
subroutine that rolls one to three notables at creation. In an allocated standard socket they are allocated
with no path and no cost. Its kind keeps it out of generic pools, it has no level gate, and NPCs never
socket one.

### Empty shells

A subroutine whose lines hold `EVERY_AFFINITY_FLAT_PER_EMPTY_SHELL_NODE` (Empty Shell Axion) is born corrupted and grants
every small, notable and keystone node in its radius through the socket grant links (`EmptyShells.radiusNodeIds`,
via `NodeGrantLinks`). `NodeAllocator.grantSocketedNodes` turns paid nodes it covers into free ones, refunding
their OP and items, and `AllocatedSkillEffects` drops an Empty Shell node's own effects and gives the socket +1 (the line's
value) to every affinity per Empty Shell node. Unsocketing releases the granted nodes like a Modspec Cache's.

### Root Conduit

A `ROOT_CONDUIT` subroutine is born corrupted and freezes one affinity from its seed (`Subroutine.conduitAffinity`).
`RootConduits` finds the shortest path from its socket to the starting root through the whole tree graph, wormhole
links included and allocation ignored, caches it per ship until the socket, root or tree layout changes, and the
socket grants +5 of that affinity for every 5 nodes between the two ends. The connector renderer draws the path's
connectors with the `CONDUIT` look in that affinity's colour (`AffinityColours`). It is player-only.

### Gated subroutines

A subroutine line backed by `RadiusGate` (`GATES_RADIUS_*`, Phoenix Ash Archive) gates the allocated nodes in its
radius behind a `CombatGate`: below 50% hull, ship system active or phased. `SkillTreeEffectSource` gives those nodes
a gated contribution holding only their `supportsTemporaryGating` effects, times the line's value; vanilla hull mod
nodes and other effects are dropped. Gated contributions stay out of permanent totals, affinity and diminishing
returns, and `ShipCombatPlan` applies them while the gate holds and zeroes them when it stops.

</details>

<details>
<summary><b>Star Charts</b></summary>

### Wormhole Gateway

The Wormhole Tech Blueprint is added to the Limbo Mothballed Equipment Cache's extra salvage, or straight to cargo
when that cache is already opened or missing, the latter once the Janus Device is obtained (`LimboBlueprintCache`).
Without Vaults of Knowledge, right-clicking it sets `$exiledSector_gatewayUnlocked`, which `WormholeGateway` checks
before it can be built, and every chart tier can be opened.

With Vaults of Knowledge, the blueprint is instead the item cost of the tier 0 gateway research. The gateway opens
charts up to tier 5; the Gateway Array and Gateway Nexus researches each unlock an upgrade of the structure, which opens up to
tier 10 and then every tier (`GatewayTierLimit` reads the player's built gateway, checked by `AtlasOpening`).
Completing the first tier 5 and tier 10 chart puts each upgrade item into cargo with a popup, as do already cleared
tiers on load (`GatewayUpgradeRewards`).

### No colony requirement

The No Colony Requirement setting (`ColonyFreeAtlas`) hides the gateway from the build list and lets wormholes open
anywhere outside a chart system. Using the blueprint, which is then a right-click item even with Vaults of Knowledge,
grants the Outer Reach Atlas ability (`AtlasAbility`). Every tier opens, and the three gateway researches are removed
from each faction's tree one frame after load, once Vaults of Knowledge has re-added them; switching the setting off
gives them back.

### The atlas

Each save has one atlas: `AtlasState` keeps the seed and per-system flags in persistent data, and `AtlasGenerator`
rebuilds everything else from the seed. A chart opens a system joined by a lane to a completed one (`AtlasReach`), and
the system supplies the layout seed, faction, region recipe, anomalies and the lines from
`atlas_node_effects.csv` for infested, cleansed and special-site systems, kept on the run as an
`AtlasRunNode` (`AtlasOpening`). An anomaly with its own `atlas_anomaly_chances.csv` row rolls on that chance instead of the
shared one. Powerful Boss (`PowerfulBoss`) does, and its run's boss always drops a chart one tier up
(`LootShaper.guaranteesBossTierUp`); anomaly effects reach the content plan, so its line sizes the boss. Chart drops
inside a run never fall below its tier.

Killing the boss is the only way to complete a system, and it grants an atlas point for the first clear of each tier (`AtlasProgress`).
A Sensor Array also reveals 6 lanes, an Infestation cleanses the systems it infested, and a Bastion gives a key fragment and
allows one attempt. A system's anomalies are its rolled ones plus records inserted into `AtlasState`, which
`AtlasAnomalies.canInsert` gates and any run there spends, at most 9 in all (`MAX_PER_SYSTEM`), unless a Relay Network chain moves on. A run that closes without killing its boss spends the system's
rolled anomalies, all but star ones. Biomes tile the map from sites about five cells apart, and each sets how many of its cells hold a
system (`AtlasBiomes`). Normal space wins most sites and chains into the volumes factions hold, while a special biome
never sits beside another site of its own type, so it stays a small pocket. Each biome lists the only factions it holds
(the derelict graveyard holds only derelicts), and normal space's list is the pool every faction territory draws from.
Its `modded_factions` entry adds every installed faction Nexerelin would count as playable: listed in a mod's
`exerelinFactionConfig/mod_factions.csv` and not set `playableFaction: false` (`ModdedChartFactions`). Revealed
territories keep their faction (`AtlasState.recordReveal`), so a faction added mid-save only claims space revealed later.
The Threat Expanse holds only the Threat, and no other faction's infestation reaches it (`AtlasGenerator`). Its systems use the starless `DEEP_SPACE` layout, one world at the centre
under vanilla's abyssal light (`ChartStarSystemGenerator`). Abyssal Rift systems have no star or planets and copy only
the look of Random Assortment of Things' Abyss: Colossal Photospheres sized as in its Sea of Harmony, which fleets fly
over, under abyssal fog and darkness, with none of RAT's sensor or storm effects (`ChartAbyss`). A biome with its own
`chart_poi.csv` sites (`biomes` column) places only those and spends every site budget on them (`ChartPois.inBiome`);
the Rift's are three abyssal stations rebuilt from RAT's with its drop groups, set around the photospheres (`ChartAbyss.siteSpots`). A biome's `*` anomaly
weight covers every anomaly it doesn't list; the Rift's is 0, and no anomaly a biome weighs at 0 can be inserted there
(`AtlasAnomalies.canInsert`). The Threat Expanse's fleets are built like vanilla's abyssal Threat
fleets, filled to the chart's fleet points with a fabricator leading each boss (`ChartThreatFleets`). A boss in an
`abyss` biome (the Rift and the Threat Expanse) is never a station, even under a Faction Skirmish (`AtlasOpening.request`). Hyperspace clouds are impassable terrain clumping inside biomes and ridging along some
biome borders: no system sits under one and lanes route around them, and a grid of cleared corridors keeps the
Gateway and every Bastion joined to the rest of the map (`AtlasTerrain`). A biome or Threat infestation behind
an unlock is decided when first seen, and that is saved.

### Biome modifiers

A biome can own one modifier per hull item part (`biome_modifiers.csv`), reserved so no other roll draws it
(`BiomeModifiers`). A non-unique hull item dropped while the player is in a run system of that biome has a 30% chance,
fixed by its seed, to gain its part's modifier: `LootSources.shapeDrops` passes each drop through every loot source's `shapeDrop`, called from the
drop helpers in `ItemLootListener` and from the hull item battle drops. The item keeps its other modifiers, except one
sharing the added effect; on a full side a random modifier makes way (`GrantedModifiers.add`).

Of the Abyssal Rift's, the moving-slowly burn level adds up over the fleet's active ships, capped at +6, onto vanilla's
`move_slow_speed_bonus_mod` at each fleet sync (`FleetWideEffects.applyMoveSlowBurnLevel`). The phase-entry repair
counts only armor actually restored toward its per-deployment cap (`PhaseEntryRepairListener`), and the drone damage bonus
rides on each strikecraft the ship launches, counting a strikecraft with no minimum crew as a drone (`DroneDamageListener`).
Borrowed Grace stacks give a fixed 1% each, so the Armor Plate's stacks (every 150 damage) work without the node; with
both, one pool holds both maximums and stacks come every 100 damage (`BorrowedGraceListener`).

### Pirate Badlands modifiers

Ram-plated changes only collision damage. The engine applies it with no projectile and no weapon stats, so a hit counts
as a collision when it also carries no EMP, is kinetic or energy, and the two hulls are in contact; dealt damage is
multiplied against enemy ships only, damage taken from any collision, never below zero (`CollisionDamageListener`).

Of the Smuggler adds up each active ship's share, capped at 30%, as a percent on the fleet's detected-at range while
the transponder is off, reapplied at each fleet sync and, for the player, when the transponder is toggled
(`FleetWideEffects.applyTransponderOffSensorProfile`). Ambusher's counts from each ship's own deployment (`OpeningVolley`).

### Remnant Expanse modifiers

The shared target bonus applies to enemies an allied non-strikecraft ship holds as its ship target, and to that
target's station modules; the list is rebuilt every 0.5 seconds, not per hit (`SharedTargetDamageListener`). The EMP
bonus scales each hit's EMP after the damage modifier, so it multiplies with damage bonuses (`EmpDamageListener`). The
shield reduction climbs to full over 3 seconds while no non-decorative weapon fires and falls over 3 seconds while any
does, checked every 0.1 seconds (`HoldFireShieldListener`).

The armor repair works on 0.5-second ticks: a cell whose armor dropped since the last tick restarts its 5-second
delay, and repair pauses while an enemy ship (not strikecraft) is within 1500 su. Each point restored adds 1 hard
flux, so repair is also cut to the flux room left and stops at full flux, and it ends for the deployment once it has
restored 50% of the ship's total armor (`UnhitArmorRegenListener`).

### Furnace modifiers

The Command Suite's corona resistance adds up over the fleet's active ships, capped at 100%, and at each fleet sync
multiplies every member's vanilla `corona_resistance`, which also covers flares, hyperspace storms and pulsar beams
(`FleetWideEffects.applyCoronaResistance`). The Shield Emitter scales `HULL_DAMAGE_TAKEN_PERCENT`, capped like the typed
damage taken stats, with Low Tech Affinity.

The Armor Plate's hull repair runs only above 50% flux and stops at 20% of maximum hull per deployment
(`HighFluxMendingListener`). The Reactor Regulator's dissipation bonus moves in whole 5% steps of hard flux
(`HardFluxDissipationListener`), and the Phase Coil Array lowers hard flux on each phase entry without changing total flux
(`PhaseEntryFluxShedListener`). Destroyed strikecraft burst on the lethal hit, reaching ships whose edge is within range
(`CinderBurstListener`).

### Frozen Fringe modifiers

The Overwintered crew's supply cut applies while vanilla counts the player fleet as moving slowly (`Misc.isSlowMoving`,
standing still included): the strongest value among its active ships multiplies every ship's maintenance supply use. A
transient script reapplies it every quarter second, as stat rebuilds drop it (`FleetWideEffects.applyMoveSlowSupplyUse`,
`PlayerFleetConditionScript`).

Patient measures from this ship to the target ship's center (`DistantTargetDamageListener`). Glacial compares speed with
top speed without the zero-flux boost, so turning in place still counts as stationary. Of the Long Night holds the ship's
deployed time at its phase-entry value while phased (`PhasedPeakTimeListener`).

### Collapsed Reach modifiers

The Specialist Crew's black-hole bonus adds up over the fleet's active ships and only works in a Star Chart system
with a black hole among its stars. It adds its share to every hull item part's targeted extra rolls, so it sums with
the chart's item quantity, for fleet battles and salvage sites alike (`BlackHoleSalvage`, `BlackHoleHullItems`). The
Armor Plate ejects a missile that directly hits armor or hull by zeroing the hit and marking the missile as having
dealt damage, which skips its explosion and on-hit effect; proximity-fused explosions aren't caught (`MissileEjectionListener`).

The Phase Coil Array's drag joins the Gravity Drag node's listener: while phased it adds to the node's doubled drag,
under the same per-ship cap (`GravityDragListener`). The Launch Assembly's stacks belong to the carrier, one for each
of its strikecraft destroyed, each expiring after 20 seconds (`AccretionStacks`).

### Threat Expanse modifiers

They spend and regrow vanilla Fragment Swarm fragments through `FragmentSwarms`: a spend needs that many fragments not
already fading out, and regrowth never fills the swarm past its maintained size. The fleet's sensor strength gains the
highest carrier's percent once for each active ship whose variant has `fragment_swarm`, the Threat volume's notable
included, at each fleet sync (`FleetWideEffects.applySwarmSensorStrength`). Shield damage spends 1 fragment per 30
absorbed, beams included (`FractalShieldListener`). Phase hulls can't carry a swarm, so the phase upkeep cover draws on
the nearest allied ship's swarm within 1000 su (`PhaseVoidUpkeepListener`). The strikecraft rebuild watches each bay's
replacement rate and restores a drop when it can spend the fragments (`HiveReplacementListener`).

The armor repair counts, once a second while the swarm is full, the fragments vanilla's regrowth would add at the
swarm's respawn rate, and each repairs the most damaged armor cell. Only armor actually restored counts toward its 10%
per-deployment cap (`SwarmOverflowRepairListener`).

### Derelict Graveyard modifiers

The recovery chance adds up over the fleet's active ships, capped at 30%, and is rolled when vanilla adds battle
d-mods to a ship the player can recover: a `DModAdderPlugin` that wins the roll adds none (`RecoveryDModAvoidance`).
The weapon and wing share adds up the same way, uncapped, and when post-battle salvage is generated each weapon and
wing stack gains that share, the fraction rolled (`SalvagedArmaments`, from `ItemLootListener`).

Each enemy ship destroyed within 1000 su repairs every armor cell by 5% of its maximum, and only armor actually
restored counts toward the rolled per-deployment cap (`WreckPlatingListener`, through `ArmorCells.repairWithinCap`
as the Rift's phase-entry repair). Free phase upkeep counts any non-strikecraft hulk on the battlefield
(`WreckShelterListener`), and the per-d-mod lines count d-mods as `TOP_SPEED_MULT_PER_DMOD` does.

### Storm Belt modifiers

The terrain penalty reduction adds up over the fleet's active ships, capped at 50%, as a flat modifier on the
commander's `nav_penalty_mult`, the stat vanilla's Navigation skill lowers (`FleetWideEffects.applyTerrainMovementPenalty`).
The speed burst starts on any EMP arc striking the ship, shields included (`ArcStrikeSpeedListener`).

Phase-exit and armor arcs (`ArcDischarge`) go to the nearest enemy ships, not strikecraft, within 600 su, cycling when
there are fewer ships than arcs, with energy and EMP damage set by the ship's hull size. Armor damage taken builds the
armor's discharges, holding one while no enemy is in range (`ArmorArcListener`).

### Chart tier groups

Chart tiers fall into three groups: low (1-5), mid (6-10) and high (11-16) (`TierGroup`). A chart modifier line gives
each group its own range (`low=15-20/mid=50-70/high=150-200`), and the chart's tier picks which one applies; the roll
is kept as a position in the range, so a chart raised into the next group keeps its relative roll. A modifier rolls only
on tiers its groups cover, so one written as `mid=/high=` never appears below tier 6 (`ModifierLine.rollsAtTier`).

Wormhole entries are set per group, 6, 3 and 1 by default (`StarChartSettings.wormholeEntries`). Each prefix adds 5%,
15% or 25% item quantity by group, on top of its own line (`ChartGenContext.QUANTITY_PERCENT_PER_PREFIX`). Every entry
burns fuel on arrival, 1000 at tier 1 plus 100 per tier above it, and can't start without it (`ChartEntryFuel`).

### Chart vanilla loot

Each chart site copies its salvage spec's drops and its own, extra by its drop type's bonus plus item quantity, for the
groups `chart_loot.csv` maps to a type, then adds its kind's rows; chart fleets get the `fleet` and `boss` rows per
100 fleet points (`ChartVanillaLoot`). A row with `requires_mod` loads only with that mod, and colony item groups are
never mapped.

A won chart battle also adds vanilla's own battle salvage for the same extra share (`ChartShipSalvage`): weapons and
wings from the lost chart ships' fittings on vanilla's recovery chances, scrap for their fleet points, and the cargo
vanilla took from their fleets. Fittings and cargo are snapshotted at the engagement, ships the player recovers add
nothing, and all of it is scaled by vanilla's player share of the damage (core `EncounterSalvage`).

### Atlas Catalysts

Atlas Catalysts are special items listed in `atlas_catalysts.csv` (`AtlasCatalysts`) and used on a revealed atlas system.
Most insert their anomaly as a player record through `AtlasAnomalies.canInsert` and `insert` (`AnomalyCatalystUse`), so
an anomaly they add takes the system's own faction. Whether it fits is the anomaly's `insertVerdict` (its `fitsAt` and
`holdsAt` by default; the Faction Skirmish checks the current owner, captures included). Redeployment Orders instead hand the system to another faction from its
biome's list, raced on the atlas seed, the system and its current owner so a reload never changes the pick, and stored
as a capture (`RedeploymentCatalystUse`). A row with the `star` effect may insert a star anomaly through
`AtlasAnomalies.canInsert` and `insert` with `forcingStar`, which skip only the insertable check, so it never replaces a star
anomaly already there; it never drops. Both go through core `Consumables.use`, which spends the item only once the change has taken.

Every chart drop source inside a live run rolls its `catalyst_chance` from `chart_drops.csv` as an expected count,
picking by weight among catalysts whose `min_tier` the run's tier reaches (`ChartDrops.roll`, `AtlasCatalysts.pickDrop`).
The Catalyst % chart line and the quantity lines scale it, and nothing outside chart systems drops one.

### Chart bosses

A chart's boss is a fleet or a station, picked by weight from the `chart_bosses.csv` rows allowed at its tier
(`ChartBosses`). A station is only picked when `chart_boss_stations.csv` has one for the run's faction at that tier,
and rows without factions serve every faction without its own. A faction in a row's `excluded_factions` never gets
that type, so the Remnant and derelicts always get a fleet boss. Each row's `fleet_share` scales the boss size for
the boss's fleet: the whole boss, or the station's guard. The boss counts as killed once its fleet no longer holds
the flagship or station it spawned with, whatever removed it (`ChartRun.bossFlagshipId`, `ChartObjectives.reconcile`).
The first battle that damages the boss adds one basic hull item unless its loot already held one, once per run
(`ChartFleetLoot.withBossGuarantees`). The boss's chest (`exiledSector_chart_boss` in `item_salvage.csv`) holds items
and parts but no crafting items or basic hull items. The completion reward's credits go straight to the player, and its items and
charts join the loot of the battle that kills the flagship (`ChartRewardLootSource`); a boss killed outside a player
battle puts them in the cargo instead.

A station boss is a vanilla station-mode fleet with no market (`VanillaChartContentEngine.createStation`) orbiting
the boss's spot, and its guard holds a passive orbit beside it, so engaging either pulls in the other. The station
is the boss and carries the boss loot. Station rows whose variant the game has not loaded are dropped
at load, and a station that still fails to spawn is replaced by a full-size fleet boss (`ChartContentGenerator`).
Either kind is detected by the player from anywhere in the system, so it always shows on the radar
(`ChartContentGenerator.revealBoss`).

### Chart system rules

The chart's player-side lines are set once on entering the system (`ChartRunScript`) and reach the player's ships at
ship creation (`ChartPlayerModifiers`). Each hostile chart fleet is granted the enemy-side lines when it is generated: they
are stored on the fleet and the hidden hull mod is added to its ships, again after each inflation
(`ChartEnemyInflationListener`), which then apply them as granted nodes, stats and
damage listeners at ship creation (`ChartEnemyShipEffects`). Only battle size and command point rate are battle rules
(`ChartBattlePlugin`). Inert switches off the ship system of each player fleet ship once, as it is created; the engine
keeps it off for that battle. Overridden swallows the autopilot key, stops manual control from turning autopilot off and
turns it back on whenever the player ship has no AI (`ChartAutopilotPlugin`). Ionized strikes a random player-side ship
every 8 seconds with a shield-piercing, EMP-only arc (`ChartArcStrikePlugin`). Erratic, from 60 seconds into a battle, forces
random player-side ballistic and energy weapons to fire: a hardpoint fires where it points, a turret first swings to a random
angle in its arc (`ChartMisfirePlugin`). Tarnished gives player fleet ships with
S-mods, and their fighters, a listener cutting damage dealt by 5% for each S-mod (`ChartSModDamageListener`).

Twinned spawns a second fleet the size of the boss's fleet, or of a station's guard, from its own seed at its own spot,
so its ships and loot differ; only the original counts for the objective. A hull size bar keeps the fleet out at the Gateway while it holds a non-civilian
ship of that size (`ChartEntryBans`). Hostile chart fleets, convoys and woken dormant fleets ignore comm requests, so no
tithe, bribe or high-reputation talk turns them (`ChartContentGenerator.HOSTILITY_FLAGS`). Only bosses, their station
guards and the Depot Guard always engage; other chart fleets weigh their strength against the player's like vanilla
fleets, so a much weaker one keeps away or shadows the player until nearby allies make the fight worth taking.

### Transverse Jump in charts

In any chart system, Transverse Jump takes the fleet back to where the chart was opened, the same as the exit
wormhole's Return (`ChartRunActions.returnToOrigin`), at vanilla's fuel and CR cost. While the player is inside,
`ChartRunScript` swaps the fleet's vanilla `FractureJumpAbility` for `ChartTransverseJumpAbility`, and
`ChartTransverseJump` swaps it back once the jump is idle outside and before every save, so saves never hold it. A
Transverse Jump plugin from another mod stays blocked like the other escape abilities.

</details>

<details>
<summary><b>Chart anomalies</b></summary>

### Anomaly placement

A run's anomalies are its atlas system's own, decided once before layout (`ChartAnomalies.planned`, ignoring tier
ranges); Star Charts roll no anomaly lines. Each id needs a `chart_anomalies.csv` row with its `requires_mod` loaded,
and one that conflicts with an anomaly already taken (both replace the star, or `excludes`) is dropped. A star anomaly
picks the star or system type, a `shape` adds terrain after layout and a `generate` places entities after the sites and
fleets (role interfaces in package `charts.anomaly`), and the run record keeps the ids that placed. Each anomaly shapes
with its own seeded random, and the run state a `generate` records reaches the run only once the whole content is built
(`AnomalyContentSite.commitStates`). Run hooks reach only the live run's anomalies (`ChartAnomalies`, `AnomalyRun`). An
anomaly's `effects` lines join the run's context for generation and loot (`ChartGenContext.withAnomalyEffects`).

`AnomalyContentSite.addSite` places chart POIs on clear orbits through the same `ChartSites.spawn` as the system's own sites,
which leaves out a site whose vanilla loot fails: derelicts around a black hole, a research station
around a neutron star whatever the tier (`StarAnomalies`), and caches in an Asteroid Storm's belts. Stellar Flares need
a visible star, so a starless system never holds them, rolled or inserted (`StellarFlares`).

### Spatial Breach

The Spatial Breach (`SpatialBreach`, `BreachTerrainPlugin`, numbers in `BreachRules`) is a terrain field fixed at its
core that starts closed. Once the player fleet flies over the core it eases open to a tier-based maximum over 60
seconds (faster with the growth line), then collapses over 60 seconds and removes itself, its core and its unlooted
caches. The core can't be clicked. Every 0.1 days the player fleet inside burns fuel and its ships use more supplies
(`BreachSupplyBuff`), both stronger toward the core. Its caches are its only loot: planned at creation, they form once
the field reaches them and vanish once the collapse passes them, with item rarity raised by depth and item quantity by
the cache item line, and content sites it covers get a smaller rarity bonus (`StarChartLootSource.modifiersFor`).
While it is open, every fleet in the system that has an AI, isn't a station and isn't a dormant debris fleet gets an
attack order on the breach ahead of its own orders (`BreachCharge`, checked each tick so late arrivals join); the
collapse removes that order and the fleets go back to what they were doing.

### Neutron Star charts

A Neutron Star chart has at least 7 planets to hide behind from the pulsar beam (`LayoutShaper.minPlanets`).
`ChartStarSystemGenerator.topUpPlanets` adds planet-only orbits past the layout's last one, then into gaps between
orbits once the layout's orbit limit is reached, then further out. Every neutron star in a chart gets
`ChartCoronaTerrainPlugin` and `ChartPulsarBeamTerrainPlugin` in place of vanilla's corona and beam
(`setPulsarIfNeutron`). They push and drain only the player fleet, so the system's other fleets stay where they are.

### Faction Skirmish

The anomaly rolls only on atlas border systems whose owner and a lane neighbour of another faction both take sides
(`FactionSkirmish.fitsAt`, never in special or infested systems; `chart_skirmish_factions.csv` lists the factions that
don't). The attacker is raced from the neighbours' current owners, weighted by lanes (`AtlasBorders.attacker`), and the
player picks a side before the wormhole opens, kept on the `AtlasRunNode`. The defender always has a station, and the
attacker matches the defender's ordinary fleets plus a commander that pushes on the station once the rest have fallen
(`SkirmishPlanner` through `adjustPlan`, `SkirmishBattle`, `ChartSkirmishes`). The joined side is 30% weaker, drops nothing
and flies as the run-only faction `exiledSector_skirmish_ally`, which is friendly to the player (`RunFactionDress`);
per-fleet `$cfai_makeHostile_<faction>` flags set the two sides against each other.

Siding with the attacker or staying out makes the station the boss, counted whoever destroys it. Siding with the
defender makes the commander the boss and protects the station: losing it loses the run (`ChartRun.objectiveLost`).
Completing a run on the attacker's side records the system as captured in `AtlasState`, and captures feed the owner
reader that opening, tooltips, borders and atlas traffic use (`AtlasOpening.ownerOf`, `territoryOwnerOf`).

### Relay Network

The anomaly places a vanilla nav buoy, comm relay and sensor array, neutral and `$objectiveNonFunctional`, sometimes
orbiting a guarded point of interest (package `charts.anomaly.relay`). On the run script's one-second timer a relay
syncs once the fleet holds station slowly beside the nav buoy, keeps its transponder on near the comm relay, or fires
an Active Sensor Burst near the sensor array (`ChartRelays.check`), and it wakes once its dialog has also restored power
for supplies and heavy machinery (`RelayDialog`; flags on the run record in `RelayStates`). A woken nav buoy or sensor
array turns player-owned, so vanilla's objective plugins give their bonus; a woken comm relay multiplies every other
fleet's `detected_by_player_range_mult` and raises its extended detection range, so the player sees them at any range.

The stage rides on an inserted record (`AtlasAnomalyRecord.Chain`); a rolled network is stage 1 of 3. When the run
closes, boss killed or not, `RelayStages.settle` moves the record to an adjacent system further
from the Gateway, or two lanes out if none qualifies (`AtlasAnomalies.onward` takes the nearest), if all three relays
woke, or at the same stage if the network could not be placed, and removes it otherwise. Each stage past the first adds the `relay_stage` lines of `atlas_node_effects.csv` once more (`RelayStages.join`), and the last stage's boss, if it dies with all three
relays woken, drops one unique hull item or subroutine whatever its source (`ChartFleetLoot.guaranteedUnique`).

### Supply Convoy

The anomaly spawns one convoy fleet at the system edge: freighters and tankers plus warships built like the map's boss at
170% of its size, flying the run-only faction `exiledSector_convoy` under its own name with the chart faction's crest
(`SupplyConvoy`, `ConvoyFleets`, `ConvoyRules`). Five days after the player first arrives, the system script `ConvoyScript`
sends it past two to four planets or stations to unload at an existing planet, avoiding planets another anomaly holds where
it can, with intel in `ConvoyIntel`. The convoy is non-hostile until the player attacks it through `ConvoyEncounterDialog` or
hits it with Interdiction Pulse (which also holds it); then it turns hostile for the run, gains Sustained Burn and runs for the
destination (`ConvoyHostility`). Asteroids, nebulae and slowing anomaly terrain cut its burn (`ConvoyTerrain`).

Beating the convoy drops ordinary chart fleet loot, and each share of its freighter fleet points lost also drops that share
of its cargo (the `convoy` scope of `chart_loot.csv` plus the `exiledSector_chart_convoy` item row) as cargo pods
(`ConvoyState`, `ConvoyCargo`). On unloading, `ConvoyCargo.deliveredCache` rolls the Exiled Sector loot that beating the
surviving fleet would give (`ItemLootListener.addUnfoughtBattleLoot` runs every loot source without wreck salvage) and the
cargo still aboard, and keeps 80% of each stack. The fleet's Exiled Sector loot is then spent (`ChartFleetLoot.spendLoot`,
`FleetLootRules.NONE`); its weapons, cargo and vanilla drops stay with it. The planet then offers a raid (`ConvoyCacheDialog`,
`anomaly.ChartRaid`, vanilla's non-market raid without reputation, response, cooldowns or rules) at a tier-based difficulty
and danger level; the cache objective yields the whole cache and stays until taken.

### Anomalous Debris

The anomaly places one to four visual-only debris fields plus the extra-debris line, each hiding a dormant fleet built
above the map boss's size and quality, its ships drawn by weight from `chart_factions.csv` factions and each captained by an AI core
of a tier-based level (`AnomalousDebris`, `DormantFleets`, numbers in `DebrisRules`). Those cores never drop: each
ship's variant carries vanilla's no-captain-core tag and the fleet's chart drops leave out AI cores. The fleets fly as the neutral
`exiledSector_dormant` faction with vanilla's dormant setup and no fleet AI, so they never move or join a battle. A 0
detected-range multiplier hides each one until the player fleet comes within 600 su of its field's edge
(`AnomalousDebrisTerrainPlugin`).

`DormantFleetDialog` replaces the first encounter: engaging wakes only that fleet, hostile for the rest of the run with
no reputation change and an AI that holds its field (`DormantFleets.wake`), then hands over to vanilla's encounter.
Corrupting first spends a Corrupted Nanoswarm and rebuilds the fleet stronger, and the first engagement won against it
drops an engram (`DormantFleetLoot`). An engram rerolls a non-unique hull item with four modifiers, one always its own
from the reserved pool that no other roll draws from (`Engram`, core `ModifierSources`). An immutable
modifier and an inhibited side's modifiers are kept, and the reroll fills up to two per side and four in total around them.
Crafting that rerolls values (Tuning Nanoswarm, corruption) finds the reserved modifier through `Item.explicitModifier`.

### Excavation Sites

The anomaly marks one to three solid planets plus the extra-sites line, each rolled 50/50 as an open or a collapsed
shaft and leaning toward one drop type, kept on the run record (`ExcavationSites`, `ExcavationStates`, numbers in
`ExcavationRules`; package `charts.anomaly.excavation`). A site's items are the `excavation` scope of `chart_loot.csv`
plus the `exiledSector_chart_excavation` item row, sized by tier and the site quantity line, with +200% to its lean
(`ExcavationLoot`). `ExcavationSiteDialog` opens on the planet while the site stands, before a convoy cache.

An open shaft starts a fleet of the chart faction, or a derelict picket, built with `maxShipSize` 2 and kept in the
planet's memory until beaten, repairing between fights like vanilla's salvage defenders (`HeldGuards`); it carries the site's vanilla drops and rolls its items in `ChartFleetLoot`. The
encounter pulls in no other fleet and allows no AI retreat (`ExcavationEncounters`), and `ChartCampaignPlugin` picks
`ExcavationBattleCreationPlugin` for it: a small dark map with rock fields along the edges, no objectives, and no cruiser or
capital ship on either side. `ExcavationBattlePlugin`, a global combat plugin, also removes them from the reserves in any
battle against a shaft fleet, so the rule holds even when another mod's battle plugin builds the map. A collapsed shaft is a
`anomaly.ChartRaid` at a tier-based difficulty whose objective yields the site's items scaled by raid effectiveness.

### Storm Front

The anomaly adds one moving `BaseTerrain` band, 3000 su wide and as long as the system is wide, that enters at a random
edge when the run opens and leaves past the far edge 30 days later, then stops affecting anything
(`StormFront`, `StormFrontTerrainPlugin`, geometry in `StormPath`, numbers in `StormRules`; package
`charts.anomaly.stormfront`). Fleets inside it have 50% less sensor range, convoy fleets slow in it as in nebulae, and chart
intel names the next worlds and POIs it reaches. The size and strike lines scale its width and strike rate.

A battle is in the storm when the player fleet is inside the band as it starts. The global `StormBattlePlugin` then
fires an EMP arc with no source ship every 6 seconds at a random non-phased ship of either side, weighted by hull size
and 10% stronger per tier, so shields block it. `StarChartLootSource` records the flag at the first engagement, so
auto-resolved battles count too, and chart fleets drop 50% more loot. A won storm battle against chart fleets also
adds one basic hull item rerolled around a reserved storm modifier (`StormFront.battleBonus`, `StormLoot`,
`GrantedModifiers`).

### Derelict Forge

The anomaly places one neutral station on a clear orbit and rolls three programs (plus the programs line) from
`chart_forge_programs.csv` by weight and tier, with one run (plus the runs line), kept on the run record
(`DerelictForge`, `ForgeStates`, `ForgePrograms`; package `charts.anomaly.forge`). Its `effects` column adds +100%
Resource %. Like a vanilla cryosleeper, the forge is guarded until beaten: `ForgeGuards` builds derelict drones (half
the tier's fleet size, cruisers at most) under one alpha-core Guardian into the forge's `$defenderFleet`, and
`ForgeDialog` hands them to vanilla `SalvageDefenderInteraction`. As at every chart site with vanilla defenders, a win
leaves the main sector's derelict tally untouched (`ChartDefenderTallies`). It then
lists the programs and their costs, the same at every tier and scaled by the cost line (`ForgeRules`), and opens the
Crafting Bench in Forge mode (`ForgeCraftingBench`), which takes hull items only.

The programs are `ForgeOperation`s in core (`ItemForging`), refusing mirrored, corrupted and locked items as nanoswarms
do. Graft groups modifiers by the `skilleffect` subpackage of each line's effect, so a hybrid counts for every part it
touches. Perfect and Retemper take the best end of a range for the player: the higher value, or the lower one for
effects whose description colours invert (`ModifierLine.bestRoll`). Excise, Anchor Reroll and Graft refuse uniques, and
Excise and Anchor Reroll leave immutable modifiers alone. `ChartForge.run` takes the cost and the run only once the
program has changed the item.

### Depot

The anomaly (id `listening_post`) places a depot of the chart faction on a clear orbit away from the inner wormhole and three to five
non-aggressive pickets with 50% more sensor range in a ring 1800 su around it, always shown on the map (package
`charts.anomaly.listeningpost`, numbers in `ListeningPostRules`). Suspicion, the alarm and the vault's share live on the run record
(`PostState`). Every 0.1 days `PostScript` asks each picket how well it sees the player: sensor contact (outer half of
its range, transponder off) builds suspicion 25% a day, which drains 10% a day out of contact; identification starts
that picket's half-day report. Full suspicion, a report going out, or a picket left with ships after a battle with the
player raises the alarm, which never comes down.

Once raised, the vault loses 20% a day down to 25%, slowed by the scuttling line, and the pickets fly to the depot, where
the first to arrive becomes the aggressive Depot Guard and the rest merge into it with their drops, each adding a fleet's item roll (`Pickets.merge`); engaging from `PostDialog` merges
every living picket at once, so one fight wins the vault. The vault is the `listening_post` scope of `chart_loot.csv`
plus the `exiledSector_chart_listening_post` item row, +100% to Star Charts, blueprints and subroutines, times the
share left (`VaultLoot`). Taking it before the alarm flags the atlas cell `LANES_REVEALED`, which reveals 3 lanes
around it like a Sensor Array (`AtlasReach`).

### Pre-Collapse Facility

Only rolls or inserts with Ashes of the Domain: Vaults of Knowledge enabled (`requires_mod` `aotd_vok`), as a chart
version of VoK's Pre-Collapse Facility. It marks one solid planet that no other anomaly holds, kept on the run record
with its stage (`PreCollapseFacility`, `FacilityStates`; package `charts.anomaly.facility`). `FacilityDialog` opens on
the planet until it is salvaged; engaging brings a Remnant fleet of three quarters the tier's fleet size,
but never fewer combat points than VoK's own roll between its `aotd_defence_min_val` and `aotd_defence_max_val`
settings (`FacilityGuards`, `VaultsOfKnowledgeCompat.defenceFleetPoints`), kept in the planet's memory until beaten
and repairing between fights (`HeldGuards`), with no other fleet pulled in and no AI retreat.

The salvage is the `pre_collapse_facility` scope of `chart_loot.csv` plus the `exiledSector_chart_pre_collapse_facility`
item row, plus VoK's own `ashes_research` drop group rolled 10 times and 2 more per tier for research databanks
(`FacilityLoot`, numbers in `FacilityRules`). The planet never gets VoK's market condition, so VoK's intel and rules
don't see it. Its Facility Survey catalyst has `weight` 0 in `atlas_catalysts.csv`, so `AtlasCatalysts.pickDrop` never
drops it and only `ExiledSectorGrantAtlasCatalysts` hands it out; a catalyst whose anomaly needs a missing mod isn't loaded.

### Coronal Hypershunt

A rare star anomaly (`CoronalHypershunt`, package `charts.anomaly.hypershunt`): the system is a single blue supergiant,
and its `fitsAt` keeps it 15 or more cells from the Gateway, both for the atlas roll and for its catalyst. It places
vanilla's `coronal_tap` entity in a pointing-down orbit just outside the star, guarded like the Derelict Forge:
`HypershuntGuards` builds an officered Omega fleet of the tier's fleet size, never fewer than vanilla's 80 defender
points, with vanilla's Omega weapon drops, and `HypershuntDialog` hands it to `SalvageDefenderInteraction`
(`VanillaDefenders`, shared with the forge). Its chart loot is booked on the hypershunt itself, so a rematch with a
kept fleet drops loot only for the ships not yet beaten.

`HypershuntDialog` takes every interaction with the hypershunt during its run, so vanilla's rules never offer the repair
or add hypershunt intel; once beaten it only says the structure is beyond repair. Its Hypershunt Survey catalyst has
the `star` effect and `weight` 0, so only `ExiledSectorGrantAtlasCatalysts` hands it out.

### Remnant Nexus and Domain-era Mothership

These anomalies never roll or get inserted: an anomaly with a `biomes` column in `chart_anomalies.csv` sits in
exactly one system of each pocket (biome site) of those biomes, the one with the lowest atlas-seed hash among its
systems that are not special sites or infested (`AtlasBiomeAnomalies`); several in one biome pick in `chart_anomalies.csv` order and
never share a system. It comes first among the node's anomalies, so
it always fits under the limit of 9, and a failed run doesn't spend it.

Each places a station-mode fleet of its faction, marked with the run's tier and carrying a chart fleet's loot, on a
clear orbit away from the inner wormhole (package `charts.anomaly.landmark`): a damaged Nexus up to tier 7 and a full
one after, or the survey mothership. A system script launches up to two or three fleets of 35% of the tier's fleet
size, 5% more per fleet lost (at most 60%), that patrol the system, drop no chart items and stop once the station
dies. `NexusFleetManager` is vanilla's `RemnantStationFleetManager` without its hyperspace encounter point;
`MothershipFleetManager` does the same on `SourceBasedFleetManager` with derelict drones up to cruisers.

### Abyssal Shipyard

A `biomes` landmark like the Nexus: exactly one system in each Abyssal Rift pocket holds a neutral shipyard orbiting a
photosphere (`charts.anomaly.shipyard`). Its guards are RAT's lineup in our own fleet: a Chronos-cored Morkoth flagship
and three escorts at tier 1, one more every two tiers up to RAT's nine (`ShipyardGuards`), fought through vanilla's
defender interaction. Once they are beaten the shipyard can be salvaged once, for the `abyssal_shipyard` scope of
`chart_loot.csv` (RAT's drop groups) and the `exiledSector_chart_abyssal_shipyard` item row (`ShipyardDialog`).

### Abyssal Research Complex

A second forge site and a Rift landmark like the Shipyard (`AbyssalResearchComplex`). `ForgeSite` gives each forge its
anomaly id, texts, bench art and guards, so the Complex shares `ForgeDialog`, `ChartForge` and the run record with the
Derelict Forge. RAT's Seraph lineup guards it (`ComplexGuards`, built like the Shipyard's through `CoredLineup`). Its
only program, Abyssal Infusion, runs once for 500 Nanite Feedstock: core `ForgeOperation.INFUSE` adds the modifier
`ForgeInfusions` names for the item's part, the Rift's from `biome_modifiers.csv`, and refuses a full side rather than
replacing a modifier.

### Abyssal Storm

A rolled anomaly whose `fitsAt` holds it to the Abyssal Rift, the only anomaly the Rift weighs above 0
(`charts.anomaly.abyssstorm`). It adds one system-wide terrain that, while the player is in the system, runs RAT's Sea
of Solitude cycle: a storm of 10 to 16 seconds every 27 to 33, flashing in bursts (`StormCycle`). Each flash raises the
detected range of every fleet outside the abyssal fog by up to 150% more and +150 su, for 0.1 days and refreshed every
quarter second (`AbyssalStormTerrainPlugin`). Its `effects` column adds 30% item quantity found in the system. Its Abyssal Terminus
catalyst has weight 0, so only `ExiledSectorGrantAtlasCatalysts` gives it.

</details>
