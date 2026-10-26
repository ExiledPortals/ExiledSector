# Unique hull items and subroutines

Only groundwork step 1 is built. This plan adds unique hull items and subroutines for the modded factions that have tree volumes (the Navarchy of Tania, Nightcross, Stargazer, the RAT Abyss and ExoTech), plus affinity items that work for any faction.

A unique must do something no rolled modifier can. It changes how a build works (converting damage, reshaping weapon slots, rerouting flux, bending affinity, gating tree nodes) instead of bundling stats that rare items already roll. A unique may carry at most one plain supporting stat. The existing vanilla-themed uniques are older than this rule. Names are working names and all magnitudes are placeholders.

## Rules for every item

- Each item echoes its faction without copying a volume notable.
- Descriptions are flavour only, written as the crew acquiring or fitting the item, with no numbers. Effect lines follow `docs/tooltip_wording.md`.
- Every item, subroutines included, gets its own suitable art, picked to show what that item is. Never reuse a generic section icon or another item's icon. Faction items take art from the source mod that matches their subject: a weapon sprite, ship crop, hull mod or system icon, or a portrait. For example, the Fafnir HVA for the Osmium Shell Stock, the Hypatia for the Mini Warp-Drive, and Nightcross's Hardlight Matrix for the Hardlight Veil. Affinity items use vanilla or our own art in the matching affinity's colour. For each item, show the user a sheet of about three candidates and let them pick. Then blend the chosen art into one palette rather than pasting it, and save it under `graphics/icons/socketables/` with the mod prefix. Credit every copied asset in the credits file. Tania and Nightcross art is free to reuse with credit. RAT needs Lukas04's approval for anything new, including RAT names such as Ordnance Redistributor, Temporal Gear and Equivalent Exchange, so ask before building the Abyss and ExoTech items. Never use Tania boss-faction art.
- Each mod's items only exist while that mod is loaded and its area isn't switched off. They load from a per-mod file or behind a `requires_mod` column, so `UniqueSourceCheck` never sees unknown faction ids.
- None of these items go on NPC ships.

## Groundwork

1. [x] Add a `source` column to `hull_items.csv`, using the same `faction:` and `site:` syntax as subroutines (`UniqueSource`). `BattleItemDrops` already asks for `pickUniqueFrom(FACTION, battle.uniqueFactionId())`, and an empty list still means "drops anywhere". The editor serialiser must learn the column. By default each faction item drops from that faction's fleets. Built: `HullItem.parseDefinition` reads the column into `ItemDefinition.uniqueSources`; `HullItemDataRules` reports bad sources (shared `ItemDataIntegrity.checkUniqueSources`, also used by subroutines) and warns on a source on a common item; the editor's hull item panel has a Drop source field, validated and written like subroutines (the column is added to older files only once used). Tests in `BattleItemDropsTest`, `ItemDataIntegrityTest` and the editor's `models.test.ts`.
2. Faction ids are `navarchy_of_tania`, `nightcross`, `stargazer` and `rat_exotech`. The Abyss is `rat_abyssals` plus its biome factions (`rat_abyssals_deep`, `_harmony`, `_primordials`, `_serenity`, `_solitude`, `_wastes`), but not `rat_abyssals_sim`.
3. Give unique subroutines an optional radius, as Doctrine Archives have. The gated subroutines and Empty Shell Axion need it.

## Mechanic families

Each family is built once as framework and then used by several items.

### Damage conversion

These rules are adapted from Grim Dawn:

- A conversion turns a share of one damage type into another. The types are kinetic, high explosive, fragmentation and energy. EMP can be converted to, but never from.
- A local conversion, limited to one weapon type such as "ballistic weapons", applies first. A global conversion ("all weapons") then applies to whatever is left.
- Damage converts only once. Converted damage is never converted again, so conversions never chain.
- When the conversions from one type add up to more than 100%, they are scaled down in proportion so that they total 100%.
- Weapon bonuses apply before conversion and carry over, because they belong to the weapon, not the damage type. The target's damage taken modifiers apply per resulting type, so converted damage uses the new type's modifiers.
Implementation: conversions use a `DamageDealtModifier` on the firing ship.
- A full (100%) conversion calls `DamageAPI.setType`.
- A partial conversion keeps the original type at reduced damage and applies one extra hit of the new type at the same point. That extra hit is flagged, so it never converts again and never triggers on-hit effects a second time.
- Beam damage objects persist for the whole firing, so beams are reduced only through `getModifier().modifyMult` with a returned id, never through `setDamage`.
- Check the ordering against energy chains, pierce and Disintegration before building.

Line wording: "30% of kinetic damage from ballistic weapons is converted to high explosive damage".

### Weapon slot conversion

This generalises `HybridBallisticSlots` into a retype rule: a source slot type, an optional size and a target type. It uses the same hull spec copy, swap-back and `UnfitWeapons` handling. Built-in slots never change. When two rules match the same slot, the first one applies and the editor or summary shows the conflict. Dual-Feed Mounts becomes one such rule.

### Item doubling

A doubler rolls one other section as its target. Every modifier of the hull item installed in that section is doubled, drawbacks included. A doubler never doubles another doubler, and it does nothing while the target section is empty. It hooks into `Item.rolls()`, the single path from items to ship effects.

The target is a rolled value, like a modifier's roll, so the Tuning Nanoswarm (`RECALIBRATION`, the Divine Orb equivalent) rerolls it to a different section. `ItemCrafting.canRecalibrate` must accept a doubler even when it has no modifier ranges to vary. Its tooltip shows the current target, and the expanded F1 view lists the sections it can roll.

### Shield damage as soft flux

The engine's `shieldSoftFluxConversion` stat (`SHIELD_SOFT_FLUX_CONVERSION_PERCENT`) is the base. The items below push it further: a fixed-arc shield that takes everything as soft flux, armor and hull damage absorbed as soft flux while shields are down, and shield flux passed to an ally (`FluxTrackerAPI.increaseFlux`). Absorbing stops while the receiving ship's flux is full.

### Affinity

Every permanent source already feeds one total per affinity in `AffinitySteps.totals` before anything else resolves. So items can add affinity, read it, convert it between types, or pass its step bonuses on to fighters.

### Gated subroutines

The nodes allocated in the subroutine's radius apply only while a combat condition holds, at several times their strength. The conditions are below 50% hull, ship system active, or phased. Like temporary nodes, only stats the game reads live in combat can be gated. Other effects in the radius are ignored, and the tooltip says so.

## Navarchy of Tania

| Item | Section | Effect | Family |
|---|---|---|---|
| Fafnir Osmium Shell Stock | weapon_mount | 40% of kinetic damage from ballistic weapons is converted to high explosive damage | conversion |
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
| Ion Discharge Coils | weapon_mount | 25% of all weapon damage is converted to EMP damage | conversion |
| Hardlight Veil Projectors | shield_generator | While shields are down, 30% of armor and hull damage taken is converted to soft flux; can't be used with Shield Shunt | soft flux |
| Extended Warranty Contract | bridge | Doubles the modifiers of the hull item in its target section | doubling |
| Nightcross Doctrine Archive (kept, subroutine) | | Adds energy rate of fire, energy flux cost, vent rate, EMP damage taken and fighter damage modifiers to nodes in radius | |

The Retrofit is the mirror of Tania's Breech Refit. The Hardlight Veil suits Dynamic Grid ships, which want their shields down. The warranty is Nightcross's corporate upsell, and it doubles the drawbacks too.

The Hardlight Veil and Shield Shunt exclude each other, whether the shunt is the vanilla hull mod or our `shield_shunt` node. The Veil can't be installed on a ship with Shield Shunt, and the shunt node and hull mod can't be added while the Veil is installed. This needs an eligibility check for hull items that reads the same exclusivity data as `NodeEligibility`, so the refit, the bench and the tree all refuse it the same way.

## Stargazer

| Item | Section | Effect | Family |
|---|---|---|---|
| Corrosion Lens Filter | weapon_mount | 50% of energy damage from beam weapons is converted to high explosive damage | conversion |
| Cosmic Shunt Array | shield_generator | 30% of shield damage taken is passed as soft flux to the nearest allied ship within range | soft flux |
| Absence of Light | reactor | Your lowest affinity counts as equal to your highest | affinity |
| Empty Shell Axion (subroutine) | | Nodes in radius can't be allocated; each grants +1 to every affinity instead | affinity |

The Corrosion Lens Filter turns beams into armor breakers, which they otherwise never are. Empty Shell Axion is always corrupted and refunds the nodes in its radius when socketed.

## RAT Abyss

| Item | Section | Effect | Family |
|---|---|---|---|
| Photosphere Lens (kept) | shield_generator | Part of shield damage taken is converted to soft flux; reduced energy damage taken | soft flux |
| Abyssal Skimmer Drive (kept) | engine_room | Increased top speed and acceleration; reduced sensor profile | |
| Chronal Decay Core | bridge | 20% more timeflow; weapon range can't exceed 500 su | |
| Equivalent Exchange Core | reactor | Removes all affinity of the two types it doesn't roll and grants half that amount as the rolled type | affinity |
| Ordnance Redistributor | weapon_mount | Missile slots become energy slots | slot |
| Temporal Gear (subroutine) | | Allocated nodes in radius apply only while the ship system is active, at triple strength | gated |

The Chronal Decay Core follows the Chronos core: time bends around the ship, but range is capped. It uses the engine's weapon range threshold, as Safety Overrides does.

The Equivalent Exchange Core rolls one affinity type at creation, and that roll is frozen. With a High Tech roll, for example, 20 Low Tech and 10 Midline become 0 and 0, and 15 is added to High Tech. It runs after every other affinity source and conversion, including Absence of Light, and before the step bonuses.

## ExoTech

| Item | Section | Effect | Family |
|---|---|---|---|
| Gilgamesh Timeline Gunners (kept) | weapon_mount | Non-missile shots have a chance to fire a second copy from an alternate timeline | |
| Superposition Mounts | weapon_mount | Small weapon slots become universal slots; small weapons cost more OP | slot |
| Hypatia Mini Warp-Drive | engine_room | Once per deployment, when hull drops below 30%, the ship warps away from the nearest enemy and vents | |
| Gamma Core Bays | flight_deck | Fighters gain the ship's affinity bonuses | affinity |
| Superposition Lattice (subroutine) | | Allocated nodes in radius apply only while phased, at triple strength | gated |

The Gilgamesh gunners' extra shot spawns from an event listener, not a per-frame scan. The warp-drive is the Hypatia's "Rapid Response" drive. Gamma Core Bays applies the ship's affinity step bonuses (hull, armor, flux) to its fighters when they launch.

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

## Open decisions

- Root Conduit: what is N? How many can a ship socket?
- Affinity hull items: should they stay seven single-line uniques, or share items? Should they also roll as explicit modifiers on rare bases?
- Damage conversion: should EMP count as a conversion target at all, or only the four damage types?
- Gated subroutines: is triple strength right? Should each condition get one subroutine, or should one subroutine roll its condition?
- Should faction uniques also drop from the general pool at a lower weight?

## Bind the vanilla uniques to their factions

Fill the `source` column (groundwork step 1) for the existing vanilla-themed unique hull items, so each one drops only from the faction it names. The factions come from the vanilla market files in `data/campaign/econ/` and the item descriptions.

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

Check that every faction still drops at least one unique, and that `ItemUnlockTest` and the drop tests still pass.
