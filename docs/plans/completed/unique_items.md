# Unique hull items and subroutines

Every stage is built. This plan adds unique hull items and subroutines for the modded factions that have tree volumes (the Navarchy of Tania, Nightcross and Stargazer), for the vanilla Remnant, Omega and Luddic Path, plus affinity items that work for any faction. The RAT Abyss and ExoTech items were dropped in favour of the vanilla Remnant and Omega themes.

A unique must do something no rolled modifier can. It changes how a build works (converting damage, reshaping weapon slots, rerouting flux, bending affinity, gating tree nodes) instead of bundling stats that rare items already roll. A unique may carry at most one plain supporting stat. The existing vanilla-themed uniques are older than this rule. Names are working names and all magnitudes are placeholders.

## Rules for every item

- Each item echoes its faction without copying a volume notable.
- Descriptions are flavour only, written as the crew acquiring or fitting the item, with no numbers. Effect lines follow `docs/tooltip_wording.md`.
- Every item, subroutines included, gets its own suitable art, picked to show what that item is. Never reuse a generic section icon or another item's icon. Faction items take art from the source mod that matches their subject: a weapon sprite, ship crop, hull mod or system icon, or a portrait. For example, the Fafnir HVA for the Osmium Shell Stock, the Hypatia for the Mini Warp-Drive, and Nightcross's Hardlight Matrix for the Hardlight Veil. Affinity items use vanilla or our own art in the matching affinity's colour. For each item, show the user a sheet of about three candidates and let them pick. Then blend the chosen art into one palette rather than pasting it, and save it under `graphics/icons/socketables/` with the mod prefix. Credit every copied asset in the credits file. Tania and Nightcross art is free to reuse with credit. Vanilla art is free to use and credited under Starsector. Never use Tania boss-faction art.
- Each mod's items only exist while that mod is loaded and its area isn't switched off. They load from a per-mod file or behind a `requires_mod` column, so `UniqueSourceCheck` never sees unknown faction ids.
- None of these items go on NPC ships.

## Groundwork

1. [x] Add a `source` column to `hull_items.csv`, using the same `faction:` and `site:` syntax as subroutines (`UniqueSource`). The editor serialiser must learn the column. A `faction:` source restricts nothing: it doubles the item's drop weight in battles where that faction lost the most, while a `site:` source still limits the item to those sites. Built: `HullItem.parseDefinition` reads the column into `ItemDefinition.uniqueSources`; `HullItemDataRules` reports bad sources (shared `ItemDataIntegrity.checkUniqueSources`, also used by subroutines) and warns on a source on a common item; the editor's hull item panel has a Drop source field, validated and written like subroutines (the column is added to older files only once used). The faction weighting is `ItemDrops.pickUniqueFromFaction` (`FACTION_SOURCE_WEIGHT_MULT`). Tests in `BattleItemDropsTest`, `UniqueSubroutinesTest`, `ItemDataIntegrityTest` and the editor's `models.test.ts`.
2. Faction ids are `navarchy_of_tania`, `nightcross`, `stargazer`, `remnant`, `omega` and `luddic_path`.
3. [x] Give unique subroutines an optional radius, as Doctrine Archives have. The gated subroutines and Empty Shell Axion need it. Built: an optional `radius` column in `subroutines.csv` (`small`, `medium`, `large`), read for UNIQUE subroutines by `UniqueSubroutineRadii`; `SubroutineRadius` centres it on the socket, so the tree ring and the storage hover preview show it, and `Subroutine.kindLines` adds a radius line (shared `socketable.radiusLine` and `socketable.radius.*` keys, formerly Doctrine-only). `SubroutineDataRules` rejects an unknown size or a radius on another kind. The editor has a Radius picker on unique subroutines, written like the other optional columns. No shipped unique has a radius yet, and the radius has no effect until the gated and affinity mechanics read it. Tests in `DoctrineOverridesTest`, `ItemDataIntegrityTest`, the editor's `models.test.ts` and `itemsPage.test.tsx`.

## Stages

Each stage builds, tests and gets committed on its own. A framework stage adds the mechanic and its effects with no items. An item stage adds data and art.

1. [x] Weapon slot conversion: generalise `HybridBallisticSlots` into ordered rules, make Dual-Feed Mounts one of them, add the slot effects the faction items need, and show a conflict in the ship summary. Built: `WeaponSlotConversion` (formerly `HybridBallisticSlots`) syncs a variant's hull spec copy to an ordered rule list, recording each slot's original type in a tag; `SlotConversionBacking` gives `BALLISTIC_SLOTS_HYBRID`, `ENERGY_SLOTS_BALLISTIC`, `BALLISTIC_SLOTS_ENERGY`, `MISSILE_SLOTS_ENERGY` and `SMALL_SLOTS_UNIVERSAL` their rules; `SkillTreeHullMod.syncSlotConversions` collects them from tree nodes and hull items alike; `UnfitWeapons` now checks every slot type; `ShipTreeSummaryRenderer` names conflicting mount types (`summary.slotConflict`). Superposition Mounts' extra OP cost for small weapons is left for its item stage. Tests in `WeaponSlotConversionTest`, `SkillTreeHullModSlotConversionTest` and `UnfitWeaponsTest`.
2. [x] Damage conversion, with its effects (EMP as a target included). Built: every conversion is 100% and bound to a kind of mount. `DamageConversion` holds the bindings, `DamageConversionListener` changes each hit's type or moves its damage into EMP through `HitDamageRestorer` (formerly `ChainHitDamageRestorer`, now shared by energy chains and Wing Shared Fate). Drones record the mount of the weapon they copy (`WeaponDroneFactory.mountOf`), and Beam Split cuts simulated hits from the original damage. Effects: `BALLISTIC_HARDPOINT_KINETIC_TO_HIGH_EXPLOSIVE`, `ENERGY_TURRET_BEAM_ENERGY_TO_HIGH_EXPLOSIVE` and `SMALL_ENERGY_MOUNT_DAMAGE_TO_EMP`. Tests in `DamageConversionTest`, `DamageConversionListenerTest`, `HitDamageRestorerTest` and `WingSharedFateListenerTest`.
3. [x] Shield damage as soft flux, with its effects and the hull item eligibility check that keeps the Hardlight Veil and Shield Shunt apart. Built: `SHIELDS_DOWN_HULL_DAMAGE_TO_SOFT_FLUX_PERCENT` (`ShieldsDownSoftFluxListener`), `SHIELD_DAMAGE_PASSED_TO_ALLY_PERCENT` (added to `SharedShieldDamageListener`) and `SHIELD_ARC_FIXED`; the Low Arc core's soft flux uses the existing `SHIELD_SOFT_FLUX_CONVERSION_PERCENT` at 100. Effects list excluded hull mods (`SkillEffect.exclusiveHullModIds`), gathered by `ItemHullModExclusions` through the `excludedHullMods` domain hook and checked by `HullUpgradeRules.socket`, `NodeEligibility` (`ITEM_CONFLICT`) and `HullModConflictResolver`. Tests in `ShieldsDownSoftFluxListenerTest`, `SharedShieldDamageListenerTest`, `SkillEffectTest`, `NodeEligibilityTest`, `SkillTreeHullModTest` and `HullUpgradeTest`. Reworked after review: the shields-down effect was too strong, so the Veil now rolls 25-35% `SHIELD_SOFT_FLUX_CONVERSION_PERCENT`, the engine's own stat; `ShieldsDownSoftFluxListener` and the item hull mod exclusions were removed, and the Veil works alongside Shield Shunt.
4. [x] Item doubling, including the Tuning Nanoswarm reroll of its target. Built: an optional `mechanic` column in `hull_items.csv` (only `doubler`), read by `HullItem.parseDefinition` into `ItemDefinition.mechanic` and checked by `HullItemDataRules`; the editor has a Mechanic picker on unique hull items. `HullItem` stores its rolled `targetSection` and shows it as its first implicit line, listing the choices in the F1 view. The doubling hooks into `ShipSections.doubledSections` and `HullUpgradeEffectSource` rather than `Item.rolls()`, since it needs the whole ship. `ItemCrafting` recalibrates a doubler's target through `Item.hasRerollableTarget`/`rerollTarget`. Tests in `ShipSectionsTest`, `HullUpgradeTest` and the editor's `models.test.ts`.
5. [x] Mod-gated items: a `requires_mod` column (or per-mod files) for hull items and subroutines, honouring the mod's area switch, with editor support. Built as an optional `area` column holding a toggled region id (`tania`, `nightcross`, `stargazer`, `exotech`, `abyss`, `enigma`, `kesteven`) rather than a mod id, since the switch is per area and Stargazer comes with the Nightcross mod. `ItemAreas` (bound to `AreaToggles.ITEM_AREAS` by `SkillTreeDomain`) keeps `ItemDefinitions` from loading an item whose area's mod is missing and keeps a switched-off area's items out of `ItemDrops`, `ItemUnlock.canDrop` and Transposition. `ItemDataIntegrity.checkArea` rejects unknown areas; the editor has an Area picker on hull items and subroutines. Tests in `ItemAreasTest`, `ItemDataIntegrityTest` and the editor's `models.test.ts`.
6. [x] Tania and Nightcross items from the families above: Black Hawk Breech Refit, Project Aria Retrofit, Fafnir Osmium Shell Stock, Ion Discharge Coils, Low Arc Shield Emitter Core, Hardlight Veil Projectors and Extended Warranty Contract, each with an art sheet. Built: `unique_*` rows in `hull_items.csv` (area `tania` or `nightcross`, source their faction) with their modifiers in `item_modifiers.csv`, zh drafts in `hullUpgrades/zh_CN.json`. The Hardlight Veil rolls 25-35%, and the Extended Warranty Contract carries 10-15% more supply use as its one supporting stat, since every item needs a modifier. Icons `graphics/icons/socketables/tania_*` and `na_*` come from `tools/item_art` (`node tools/item_art/tania.js save F B L` and `nightcross.js save A I V W` pick a candidate per item); the user was away, so I picked from the sheets in `graphics/unused/item_art_sheets/`. Credited in `CREDITS.md`.
7. [x] Stargazer items from the families above: Corrosion Lens Filter and Cosmic Shunt Array. Check the Stargazer art permission first. Built: the Stargazer art ships inside Nightcross Armory, so YukiM's grant covers it. `unique_corrosion_lens_filter` and `unique_cosmic_shunt_array` (area `stargazer`, source `faction:stargazer`, the shunt rolling 25-35%) with icons from `tools/item_art/stargazer.js` (`save L S`), picked from `graphics/unused/item_art_sheets/`.
8. [x] Affinity: the family, then Farige Blueprint Vault, Absence of Light, Empty Shell Axion and the affinity hull items. Waits on the affinity hull item decision. Root Conduit also waits on N.
    - [x] The family and the affinity hull items. Built: `AffinityScaling` backs the seven `*_PER_*_AFFINITY` effects in `SpecialSkillEffect`, resolved by `AffinitySteps.scaledBonuses`; the new `FIGHTER_DAMAGE_TAKEN_MULT` covers the deck armorers, and the existing `WEAPON_RANGE_PERCENT` the fire control. The seven uniques have vanilla art recoloured per affinity (`tools/item_art/affinity.js`), and the bench tooltip of an installed one shows its current bonus (`AffinityItemLines`, via `SkillTreeHullMod.affinityTotals`). Tests in `AffinityStepsTest`.
    - [x] Farige Blueprint Vault and Absence of Light. Built: `HIGHEST_AFFINITY_FLAT_PER_S_MOD` (Farige, +3) and `LOWEST_AFFINITY_AS_HIGHEST` (Absence), resolved in `AffinitySteps.totals`. Decided without the user: Farige feeds the ship's highest affinity rather than the root's, which is nearly always the same and needs no tree lookup in Exiled Lib. Icons from `tools/item_art/affinity_uniques.js`. Tests in `AffinityStepsTest` and `ShipSectionsTest`.
    - [x] Empty Shell Axion. Built: `unique_empty_shell_axion` (medium radius, area `stargazer`) with the `EVERY_AFFINITY_FLAT_PER_HOLLOW_NODE` line; `EmptyShells` and the socket-aware `NodeGrants.GrantLookup` make it take over its radius as granted nodes, which keeps the tree connected where "can't be allocated" would strand the socket. Roots, sockets and wormholes are never hollowed. Tests in `EmptyShellsTest`.
    - [x] Root Conduit. Built: the `ROOT_CONDUIT` subroutine kind (born corrupted, one per ship, dropped by any faction's battles at 0.01 per 100 deployment points, since only uniques stay in the generic pools) and the `root_conduit` row; `RootConduits` caches the path and grants the affinity through `AllocatedSkillEffects`; `SkillTreeNodeConnectorRenderer` glows the path with `ConnectorKind.CONDUIT`. Tests in `RootConduitsTest`.
9. [x] Gated subroutines and Phoenix Ash Archive. Waits on the gated subroutine decision. Built: `CombatGate` (Exiled Lib) and an optional gate on `EffectContribution`, kept out of permanent totals and grouped apart in `EffectTotals`; `RadiusGate` backs `GATES_RADIUS_BELOW_HALF_HULL`, `GATES_RADIUS_WHILE_SYSTEM_ACTIVE` and `GATES_RADIUS_WHILE_PHASED`; `GatedSubroutines` maps the nodes in a gating radius, `SkillTreeEffectSource` gates them, and `ShipCombatPlan` toggles them in combat; the ship summary prefixes gated totals. `unique_phoenix_ash_archive` (Tania, medium radius, triple strength below 50% hull). Decided without the user: triple strength stays, and each condition gets its own subroutine; the system-active and phased subroutines are left for a later volume, since their effects already exist. Tests in `GatedSubroutinesTest` and `ShipCombatPlanTest`.
10. [x] Remnant, Omega and Luddic Path items, replacing the RAT Abyss and ExoTech items. Built: the effects `TIMEFLOW_MULT`, `ACCELERATION_PERCENT`, `NON_MISSILE_WEAPON_RANGE_CAP_FLAT`, `SMALL_WEAPON_OP_COST_FLAT` (the engine's small weapon OP mods), `DISPLACER_FAILSAFE_HULL_PERCENT` (`DisplacerFailsafeListener`), `NON_MISSILE_PROJECTILE_ECHO_CHANCE_PERCENT` (`ProjectileEchoListener`), `STRIKECRAFT_GAIN_AFFINITY_BONUSES` (`AffinityStrikecraft`, hooked into `SkillTreeHullMod`'s fighter application) and the three `AFFINITY_EXCHANGE_TO_*_PERCENT` (resolved last in `AffinitySteps.totals`); the `one_of` hull item mechanic (`ItemDefinition.keepsOneModifier`, rolled in `ItemRoller`), which the Equivalent Exchange Core uses to freeze its affinity. Nine `unique_*` rows in `hull_items.csv` and `unique_p_space_lattice` in `subroutines.csv`, sources `faction:remnant`, `faction:omega` or `faction:luddic_path`, no area. Icons `graphics/icons/socketables/exiledSector_*` from vanilla sprites via `tools/item_art/remnant_omega.js` (`save 0 1 1 0 0 0 0 1 0 0`), picked from the sheets in `graphics/unused/item_art_sheets/`. Decided while building: the echo rolls on hit rather than on firing, since the engine has no fire event, and fires from the weapon toward the target at no flux or ammo cost; the Displacer Failsafe moves the ship 1200 su; the Radiant Shield Lattice rolls 15-20% soft flux, less than the Hardlight Veil, since it also reduces energy damage taken; small weapons cost 1-2 more OP on the Facet Mount Geometry. Tests in `AffinityStepsTest`, `DisplacerFailsafeListenerTest`, `ProjectileEchoListenerTest`, `AffinityStrikecraftTest`, `ItemRollerTest` and `UniqueSourceCheckTest`.

## Mechanic families

Each family is built once as framework and then used by several items.

### Damage conversion

Every conversion turns 100% of the matching damage into another type, and is bound to a kind of mount: mount type, size, and hardpoint or turret. The mount type is read after slot conversion, so slot items can feed conversion items. When two owned conversions match one mount, the first applies. A full conversion changes the hit's type, so it stays one hit with full armour penetration. A conversion to EMP moves all of the hit's damage into EMP, so those weapons do nothing to shields. EMP is never converted from.

Line wording: "Kinetic damage from weapons in ballistic hardpoints is converted to high explosive damage".

### Weapon slot conversion

This generalises `HybridBallisticSlots` into a retype rule: a source slot type, an optional size and a target type. It uses the same hull spec copy, swap-back and `UnfitWeapons` handling. Built-in slots never change. When two rules match the same slot, the first one applies and the editor or summary shows the conflict. Dual-Feed Mounts becomes one such rule.

### Item doubling

A doubler rolls one other section as its target. Every modifier of the hull item installed in that section is doubled, drawbacks included. A doubler never doubles another doubler, and it does nothing while the target section is empty. It hooks into `Item.rolls()`, the single path from items to ship effects.

The target is a rolled value, like a modifier's roll, so the Tuning Nanoswarm (`RECALIBRATION`, the Divine Orb equivalent) rerolls it to a different section. `ItemCrafting.canRecalibrate` must accept a doubler even when it has no modifier ranges to vary. Its tooltip shows the current target, and the expanded F1 view lists the sections it can roll.

### Shield damage as soft flux

The engine's `shieldSoftFluxConversion` stat (`SHIELD_SOFT_FLUX_CONVERSION_PERCENT`) is the base. The items below push it further: a fixed-arc shield that takes everything as soft flux, a larger share of shield damage as soft flux, and shield flux passed to an ally (`FluxTrackerAPI.increaseFlux`). Absorbing stops while the receiving ship's flux is full.

### Affinity

Every permanent source already feeds one total per affinity in `AffinitySteps.totals` before anything else resolves. So items can add affinity, read it, convert it between types, or pass its step bonuses on to fighters.

### Gated subroutines

The nodes allocated in the subroutine's radius apply only while a combat condition holds, at several times their strength. The conditions are below 50% hull, ship system active, or phased. Like temporary nodes, only stats the game reads live in combat can be gated. Other effects in the radius are ignored, and the tooltip says so.

## Navarchy of Tania

| Item | Section | Effect | Family |
|---|---|---|---|
| Fafnir Osmium Shell Stock | weapon_mount | Kinetic damage from weapons in ballistic hardpoints is converted to high explosive damage | conversion |
| Black Hawk Breech Refit | weapon_mount | Energy slots become ballistic slots | slot |
| Low Arc Shield Emitter Core | shield_generator | All shield damage taken is converted to soft flux; shield arc is fixed and can't be modified | soft flux |
| Farige Blueprint Vault | bridge | +3 to the root's affinity for each s-mod on the ship | affinity |
| Phoenix Ash Archive (subroutine) | | Allocated nodes in radius apply only below 50% hull, at triple strength | gated |

The Fafnir is Tania's osmium-shell "Spear of Carina". The emitter core is Tania's own Low Arc Shield Emitter turned into a trade: a narrow shield that never builds hard flux. The vault ties Tania's s-mod pillar to affinity.

## Nightcross

| Item | Section | Effect | Family |
|---|---|---|---|
| Helical Drive Coils (kept) | engine_room | Increased zero-flux speed; the zero-flux boost stays on while the ship system is ready | |
| Chroma Reactor Tap (kept) | reactor | Increased flux capacity; reduced energy weapon flux cost; reduced ship system cooldown | |
| Project Aria Retrofit | weapon_mount | Ballistic slots become energy slots | slot |
| Ion Discharge Coils | weapon_mount | All damage from weapons in small energy mounts is converted to EMP damage | conversion |
| Hardlight Veil Projectors | shield_generator | 30% of shield damage taken is converted to soft flux | soft flux |
| Extended Warranty Contract | bridge | Doubles the modifiers of the hull item in its target section | doubling |
| Nightcross Doctrine Archive (kept, subroutine) | | Adds energy rate of fire, energy flux cost, vent rate, EMP damage taken and fighter damage modifiers to nodes in radius | |

The Retrofit is the mirror of Tania's Breech Refit. The warranty is Nightcross's corporate upsell, and it doubles the drawbacks too.

## Stargazer

| Item | Section | Effect | Family |
|---|---|---|---|
| Corrosion Lens Filter | weapon_mount | Energy damage from beam weapons in energy turrets is converted to high explosive damage | conversion |
| Cosmic Shunt Array | shield_generator | 30% of shield damage taken is passed as soft flux to the nearest allied ship within range | soft flux |
| Absence of Light | reactor | Your lowest affinity counts as equal to your highest | affinity |
| Empty Shell Axion (subroutine) | | Nodes in radius can't be allocated; each grants +1 to every affinity instead | affinity |

The Corrosion Lens Filter turns beams into armor breakers, which they otherwise never are. Empty Shell Axion is always corrupted and refunds the nodes in its radius when socketed.

## Remnant

| Item | Section | Effect | Family |
|---|---|---|---|
| Radiant Shield Lattice | shield_generator | Part of shield damage taken is converted to soft flux; reduced energy damage taken | soft flux |
| Equivalent Exchange Core | reactor | Removes all affinity of the two types it doesn't roll and grants half that amount as the rolled type | affinity |
| Ordo Feed Conversion | weapon_mount | Missile slots become energy slots | slot |
| Displacer Failsafe | engine_room | Once per deployment, when hull drops below 30%, the ship teleports away from the nearest enemy and vents | |

The Equivalent Exchange Core lists all three exchanges and keeps one (`one_of`), picked when it drops and frozen. With a High Tech roll, for example, 20 Low Tech and 10 Midline become 0 and 0, and 15 is added to High Tech. It runs after every other affinity source, including Absence of Light, and before the step bonuses.

## Luddic Path

| Item | Section | Effect | Family |
|---|---|---|---|
| Pather Wing Zealots | flight_deck | Strikecraft gain the ship's affinity bonuses to hull, armor and flux | affinity |

## Omega

| Item | Section | Effect | Family |
|---|---|---|---|
| Shard Microburn Cluster | engine_room | Increased top speed and acceleration; reduced sensor profile | |
| Tesseract Stillwell | bridge | 20% more timeflow; non-missile weapon range can't exceed 500 su | |
| Reality Disruptor Echo | weapon_mount | Non-missile projectiles that hit have a chance to be fired again from the same weapon | |
| Facet Mount Geometry | weapon_mount | Small weapon slots become universal slots; small weapons cost more OP | slot |
| P-Space Lattice (subroutine) | | Allocated nodes in radius apply only while phased, at triple strength | gated |

The Tesseract Stillwell caps range with the engine's range threshold, as Safety Overrides does. The Shard Microburn Cluster is plain stats, an exception to the rule above kept from the earlier design.

## Affinity items

### Root Conduit (subroutine)

This is a corrupted subroutine with its own kind. It always drops corrupted, rolled with one of Low Tech, High Tech or Midline, and that roll is frozen.

- It grants +5 of its affinity for every N nodes between its socket and the ship's root. The count is the shortest path through the tree graph, and it runs through wormhole exits, so a socket inside a volume counts the nodes on both sides of the wormhole. It ignores allocation and excludes both ends. It is recomputed only when the socket, root or tree layout changes, and is cached on the ship's data.
- The connectors along that shortest path get an extra bright glow in the affinity's colour, the same colour as the root's affinity number. A fixed tie-break picks one path among equally short ones. The glow is drawn from the cached path, never searched per frame.
- The affinity is a permanent contribution, so it counts towards the step bonuses and towards everything else in this plan that reads affinity.
- It is player-only.

### Affinity hull items

Each line is a new effect that reads the ship's affinity total after `AffinitySteps.totals` and adds its stat once per whole step. These are "increased" (summed) bonuses, unlike the step bonuses themselves, which are "more". The exception is the fighter damage taken line, which is its own multiplier and so reads "less". "Affinity" alone means all three types summed.

| Item | Section | Effect |
|---|---|---|
| Old Guard Gun Captains | weapon_mount | 1% increased ballistic weapon damage for every 5 Low Tech Affinity |
| Tachyon Lens Technicians | weapon_mount | 1% increased energy weapon damage for every 5 High Tech Affinity |
| Line Yard Ordnance Crew | weapon_mount | 1% increased missile weapon damage for every 5 Midline Affinity |
| Bulwark Riveters | armor_plating | 1% increased armor for every 10 Low Tech Affinity |
| Heritage Fire Control | bridge | 1% increased weapon range for every 10 affinity |
| Mixed Doctrine Flight Instructors | flight_deck | 1% increased fighter weapon damage for every 10 affinity |
| Deck Armorers' Guild | flight_deck | 1% less damage taken by fighters for every 15 affinity |

These need:
- One new backing for "stat per N affinity". Its step size and affinity filter live in the enum constructor, and the rolled magnitude is the bonus per step.
- A new weapon range effect for all weapon types. Only per-type range effects exist today.
- A new fighter damage taken MULT.
- A tooltip line showing the bonus the ship currently gets from its affinity (`AffinityTooltipLines`).

## Decisions

Decided without the user, while they were away: Root Conduit's N is 5 and a ship can socket one; the affinity hull items stay seven single-line uniques and never roll as explicit modifiers. Decided: EMP is a conversion target, never a source, and every conversion is 100% and bound to a kind of mount. Faction uniques drop from the general pool at their normal weight, and at double weight from their faction's battles (groundwork step 1).

## Bind the vanilla uniques to their factions (built)

Fill the `source` column (groundwork step 1) for the existing vanilla-themed unique hull items, so each one is twice as likely to drop from battles against the faction it names, while still dropping everywhere else. The factions come from the vanilla market files in `data/campaign/econ/` and the item descriptions.

| Item | Source |
|---|---|
| Coatl Bastion Gunnery Team | `faction:hegemony` |
| COMSEC Signals Detachment | `faction:hegemony` |
| Auxiliary Conscripts | `faction:hegemony` |
| Tri-Tachyon Morale Assurance Officer | `faction:tritachyon` |
| Sindrian Fuel Company Engineers | `faction:sindrian_diktat` |
| Gryphon Missile Crew | `faction:persean` |
| Olinadu Security Detachment | `faction:persean` |
| Asher Salvage Guild Crew | `faction:luddic_church` |
| Converted Hangar Deck Crew | `faction:luddic_church` |
| Pather Cell | `faction:luddic_path` |
| Back-Alley Cyber-Surgeon | `faction:pirates` |
| Kanta's Den Scrapdogs | `faction:pirates` |
| Deserter Gunners | `faction:pirates` |
| Salvager Crew | `faction:independent` |
| Ko Combine Shipbreakers | `faction:independent` |
| Galatia Hyperspace Physics Group | `faction:independent` |

Some items stay unbound. Alpha Site Survivor, Vambrace Plating, the Gate Hauler Drive Coil and the Planetkiller Resonance Circuit are tied to a story event through `unlock`, not to a faction. The Nanoforge Seed, Synchrotron Cell and Fullerene Spool Integration name no faction.

The unique subroutines keep their `site:` sources. The ones that name a faction also gain it as a second source: `faction:tritachyon` for the Halcyon Fire-Control Lattice, and `faction:pirates` for the Cracked Drive Governor. Battle drops only roll unique hull items today (`BattleItemDrops` filters on `HULL_ITEM`), so a faction source on a subroutine does nothing until battles can also roll unique subroutines.

Check that `ItemUnlockTest` and the drop tests still pass.

Built: the sources above are in the `source` column of `hull_items.csv` and `subroutines.csv`; every faction id matches a vanilla faction, so `UniqueSourceCheck` stays quiet.
