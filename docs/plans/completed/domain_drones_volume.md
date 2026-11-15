# Domain Drones Volume

A ship tree volume built around the Domain's derelict war drones and survey motherships. It sits between the Pirate and Stargazer volumes, and a derelict survey mothership takes the place of the volume's star. The crew learns by reverse engineering: stripping drone wrecks and the autoforges of drifting motherships. On automated ships the core logs and indexes the wreck data instead (`exiled_sector_automated_flavour`).

Status: planned 2026-11-14, built 2026-11-15 on branch `domain-drones-volume`. Every magnitude here is a placeholder pending balance. The anchor uses the threat anchor's icon until its own is picked, the node icons are blank, and the concave diamond outline decoration was left out (only the mothership image and the outer circle ring are placed).

## Facts checked

- Vanilla derelict hulls: Picket, Sentry, Warden, Defender (frigates), Bastillon, Berserker (destroyers), Rampart (cruiser), Guardian (battleship), plus the `station_derelict_survey_mothership` with its pod and mast modules. Their shared description says war drones were kept out of core Domain space and sent against rebel worlds "to tip the terrible balance of attrition". The mothership description calls it a von Neumann probe whose autoforge builds survey ships and "the supplies required" for its mission.
- Rugged Construction (`rugged`, `com.fs.starfarer.api.impl.hullmods.RuggedConstruction`) is a hidden, 0 OP hull mod built into the Picket, Sentry, Defender, Bastillon, Berserker and Rampart, and a few non-derelict hulls (Hound, Gremlin, Grendel, Onslaught Mk.I). It reduces most negative d-mod effects, makes the ship far likelier to be recoverable and cuts recovery supply cost.
- The Defender's and Bastillon's ship system is `flarelauncher`, which fires the `flarelauncher1` weapon. The Sentry's is Fast Missile Racks.
- The mothership sprite is `graphics/stations/survey_mothership.png` (custom entity `derelict_mothership`).
- Second-in-Command 2.1.1 inspirations: Derelict Operations (-6% DP per d-mod, max 5), Derelict Fortifications, Wide-Range Transmission (fighters copy their mothership's core). Dustkeeper add-on: Escort Drones (1/3/5/7 escort drones by hull size on automated ships, 1/2/3/4 otherwise).
- Our code: `PerDModPercentEffect` with `SkillEffectSupport.countedDMods` already caps at `MAX_COUNTED_DMODS = 5`. `FactionEncounterGate` already models "fought a fleet whose faction maps to this volume" (Remnant). `TemporaryCombatSpawn` spawns combat-only ships (Coalition Flight Doctrine, Phantom Wake). Fighter stats reach wings through `SkillTreeHullMod.applyEffectsToFighterSpawnedByShip`. The phantom hull mod path (`SkillType.phantomHullMods`) places a real hull mod id as a tagged perma mod.

## Placement and wiring

- Region tag `domain_drones`, added to `SkillTags.VOLUME` and the editor's `REGION_TAGS`.
- The mothership is an `image` decoration at about (24000, -1500), using the vanilla sprite, tagged `domain_drones`. That is about 7k from the pirate star (18524, 4228) and 7.6k from the Stargazer star (29054, -7476), clear of the [REDACTED] and Omega volumes.
- Gate `gate_derelicts_fought`: opens on the first battle against a derelict fleet, a new `FactionEncounterGate` entry like Remnant (`$exiledSector_foughtDerelicts`, no win needed), with withheld anchors. Derelict fleets turn up early, so the volume opens early in a playthrough.
- `derelict,domain_drones` row in `npc_faction_volumes.csv`, so drone fleets carry the volume.
- `wormhole_anchor_domain_drones` row in `wormhole_anchors.csv`, crest faction `derelict`, dropped by derelict fleets.
- Layout follows the Domain Explorarium crest (approved 2026-11-15), 28 nodes. Three Affinity nodes (north, east, west) and the wormhole exit (south) sit on a small hub ring of radius 0.3R around the mothership, joined by curved connectors. Each hub starts two wheels of two lead-ins and a notable. The straight wheel runs out along the hub's direction, lead-ins at 0.62R and 1.0R, notable at 1.45R on a point of the diamond. The curved wheel bends clockwise, lead-ins at (0.58R, +22°) and (0.82R, +36°), notable on the outer circle at (1.0R, +45°). Diamond points: north Terrible Balance, east Sentry Reserve Racks, south Defender Flare Bays, west Rugged Drone Frame. Circle: northeast Mothership Uplink, southeast Autoforge Escorts, southwest Feed the Forges, northwest Countless Cycles. R is about 2000, picked to clear the neighbouring volumes. The concave diamond outline and the outer circle are decorations only.

## Notables

Each notable is one wheel: the notable plus small lead-in nodes of the one type chosen for it (picked 2026-11-14).

### Rugged Drone Frame

Grants Rugged Construction as a phantom hull mod: d-mod effects reduced, far likelier to be recoverable, cheaper recovery. Check first that a hidden hull mod survives the phantom path (refit hiding, `PhantomHullMods.install` wrapping). Lead-ins: Reduced D-Mod Effect.

### Countless Cycles

-2% deployment points per d-mod, up to 5 d-mods. A new `DEPLOYMENT_POINTS_PERCENT_PER_DMOD` effect on `PerDModPercentEffect`, in the logistics part. Lead-ins: Reduced Monthly Supplies.

### Terrible Balance

When an allied ship within 1500 su is destroyed, this ship gains +5% weapon damage and rate of fire for the rest of the battle, up to 5 stacks. Built on `NearbyDestructionListener` filtered to allies, in the aura part. Unlike Blood in the Water it counts only allied losses and lasts the whole battle. Lead-ins: All Weapon Damage.

### Autoforge Escorts

The ship deploys with 1/2/3/4 escort drones by hull size that stay near it; a destroyed drone is rebuilt after 60s. Candidate drones are vanilla `borer_wing` (mining drone, fits the mothership) or `terminator_wing`. They launch as a fighter wing that orbits the ship (like the Tempest's Terminator drones), so strikecraft stats such as the lead-ins' armor reach them. Lead-ins: Strikecraft Armor, a new small type on the existing `FIGHTER_ARMOR_PERCENT` effect.

### Feed the Forges

After a battle, the ship's autoforge turns wreckage into supplies equal to X% of the enemy deployment points destroyed. Logistics part, read at post-battle loot alongside the other loot hooks (`starsector_salvage_bonus_read_timing`). Lead-ins: Cargo Capacity.

### Sentry Reserve Racks

The first time each missile weapon runs dry in a battle, it reloads fully once. Weapon part, a listener tracking which weapons have used their reserve. Lead-ins: Missile Weapon Damage.

### Defender Flare Bays

When an enemy missile homes on the ship, it fires decoy flares automatically, at most once every Y seconds. Spawns `flarelauncher1` projectiles the way the Flare Launcher system does. Defence part. Lead-ins: Point Defense Damage to Missiles and Fighters.

### Mothership Uplink

The ship's strikecraft gain X% of the ship's own percent bonuses to weapon damage and rate of fire, read from the carrier's stats when each fighter launches. Fighter part, applied through `applyEffectsToFighterSpawnedByShip`. Lead-ins: Strikecraft Weapon Damage.

## Steps

1. Done: region tag (`SkillTags`), gate (`FactionEncounterGate.DERELICTS`, `node_gates.json`), faction volume row and anchor row. The anchor icon is still the threat anchor's placeholder (TODO in `WormholeAnchorDataTest`).
2. Done: effects and tests. Rugged Drone Frame copies Rugged Construction with `DMOD_EFFECT_MULT`, the new `DMOD_ACQUIRE_CHANCE_MULT`, `SHIP_RECOVERY_CHANCE_BONUS` and the new `SUPPLIES_TO_RECOVER_MULT`; Countless Cycles is `DEPLOYMENT_POINTS_PERCENT_PER_DMOD`; Terrible Balance `TerribleBalanceListener`; Autoforge Escorts `AutoforgeEscortsListener` with Terminator drones, sharing `CarrierLaunch` with Coalition Flight Doctrine; Feed the Forges `ForgeSupplies`; Sentry Reserve Racks `ReserveRacksListener` (a share of max ammo); Defender Flare Bays `FlareBaysListener` (flares per burst, 1000 su, 10 s); Mothership Uplink `MothershipUplink`.
3. Done: skill types with theme tags, names and flavour (en and zh_CN), plus the Strikecraft Armor lead-in and effects for All Weapon Damage. Icons stay blank.
4. Done: layout in `ship_skill_tree.json` (mothership image, outer circle ring, hub ring, wheels, wormhole exit). The diamond outline was left out.
5. Done: `docs/docs.md` sections Rugged Drone Frame through Feed the Forges, and the gate in Wormhole anchors.
6. Done: README and forum post sentence.

