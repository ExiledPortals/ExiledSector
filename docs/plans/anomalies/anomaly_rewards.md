# Anomaly rewards

Most anomalies are interesting to play but reward more of the same: extra quantity, an extra cache, a few reserved modifiers. Anomalous Debris is the exception, since its engrams are a crafting outcome nothing else gives. This plan gives each anomaly a reward that only it grants, the way each Path of Exile league mechanic has its own crafting or item outcome.

Status: proposed 2026-11-11. Sections 1 and 2 are decided; every anomaly in section 3 is decided, some only in outline until the user details them. Section 1 and the feedstock drops of section 2 are built. All numbers are placeholders.

## 1. Storm Front: biome modifiers on every hull item (built)

The storm stops granting its own reserved modifiers. Instead, every non-unique hull item a storm battle drops carries at least one biome modifier:

- In a run system whose biome has biome modifiers, it is that biome's modifier for the item's part, as the 30% biome roll would add it.
- In a system whose biome has none (normal space, or a biome without rows in `biome_modifiers.csv`), it is a random biome modifier of the item's part, picked from the item's seed among the biomes the player has opened (`BiomeModifiers.modifiersFor`, `AtlasData.regionOpen`).
- An item that already gained one from the 30% roll keeps it and gets nothing more. An item whose part has no biome modifier to take is left as it is; a full side makes way for the granted one, as `GrantedModifiers.add` does.

A storm battle is one the existing flag marks (`StarChartLootSource.recordEngagement`), and every hull item it drops counts: the chart fleet roll, the hull item battle drops and the Supply Convoy's cargo pods. The +50% loot quantity stays, so the storm remains the place to fight.

As built: battle drops take their own path, `LootSources.shapeBattleDrops` calling `LootSource.shapeBattleDrop` after the usual shaping. `StarChartLootSource.shapeBattleDrop` hands each item to the anomalies holding the player's battle (`LootShaper.shapeBattleDrop`), and the Storm Front's grant lives in `StormLoot.withBiomeModifier`. Fleet loot, hull item battle drops, boss rewards won in battle and convoy pods (`ConvoyCargo.lostCargo`, through `ChartCargoLoot.rollFromBattle`) take that path; salvage inside the storm does not. The two storm modifiers and their reservation are removed, since every Storm Belt part already holds a modifier, and `StormFront.battleBonus` with them. Tests: `StormLootTest`, `GrantedModifiersTest`.

## 2. Supply Convoy: infused feedstock and a Harvest-style bench (decided in outline)

The convoy's freighters also haul infused feedstock, cargo the player spends at a new Crafting Bench mode for tag-driven crafts, in the manner of Path of Exile's Harvest.

### Harvest in Path of Exile, for reference

Researched 2026-11-11. Costs are from Maxroll's guide as of 3.24 (March 2024), and the history is from general knowledge of the game, so neither is checked against the current patch.

- Harvest began as the 3.11 league: the Sacred Grove, where the player planted seeds, fought the monsters they grew into, and was given crafts in return. Each craft was stored in a Horticrafting Station (three per station) and could be used, traded or kept. Many crafts were fully deterministic (augment a modifier of a tag, remove one, remove one and add another, fracture a modifier, reforge keeping all prefixes or suffixes), which made it the most powerful crafting system the game had had.
- After that league the deterministic crafts were pulled back in stages for being too strong: augment and remove went, then the keep-prefix and keep-suffix reforges and fracture.
- Since the lifeforce rework, Harvest monsters drop crystallised lifeforce in three colours, Wild (yellow), Vivid (green) and Primal (blue), plus the rare Sacred lifeforce. The Horticrafting bench is now a fixed list like the Crafting Bench: the player puts a rare item in it, picks any craft, and pays its lifeforce. Each craft takes one colour, so the colour the player has decides what they can do. Lifeforce stacks and trades like currency.
- The crafts on the bench, by group:
  - Reforge a rare with new random modifiers, guaranteeing one of a named tag (fire, cold, lightning, physical, chaos, life, defence, attack, caster, speed, critical): 50 to 150 lifeforce of the tag's colour.
  - Reforge, much more likely or much less likely to keep the same modifiers: 200 Wild.
  - Change a resistance modifier into another element at a similar tier: 500 of the target element's colour.
  - Add a modifier of a named tag and remove another at random: 15000 to 20000 lifeforce plus one Sacred.
  - Specials: randomise an item's influences and reforge it, synthesise an item, reforge an influenced item with an influence modifier (1000 to 5000 lifeforce, the last two plus one Sacred), and enchants.
- Sources: [PoE Wiki, Harvest crafting](https://www.poewiki.net/wiki/Harvest_crafting), [PoE Wiki, Horticrafting Station](https://poewiki.net/wiki/Horticrafting_Station), [Maxroll, Harvest Crafting Guide](https://maxroll.gg/poe/crafting/harvest-crafting-guide).

### Our version

The convoy's cargo pods (`ConvoyCargo`, by share of freighter fleet points lost) and its delivered cache (80% of it) also hold infused feedstock: Nanite Feedstock steeped in vanilla's abyss (the abyssal hyperspace, the Shrouded Dwellers and the Threat). This plan says so to tell it apart from Random Assortment of Things' Abyss; player-facing text never mentions RAT or its Abyss. Three commodity-like crafting items stand in for the lifeforce colours, each covering a family of theme tags (`theme_tags.csv`). There is no counterpart to Sacred lifeforce.

| Feedstock (placeholder names) | Item id | Tags it crafts |
|---|---|---|
| Threat-infused Feedstock | `exiledSector_threat_feedstock` | `ballistic`, `non_beam_energy`, `beam`, `missile`, `strikecraft` |
| Shroud-infused Feedstock | `exiledSector_shroud_feedstock` | `defence`, `range`, `ammo`, `logistics` (with `sensors`) |
| Phase-infused Feedstock | `exiledSector_phase_feedstock` | `flux`, `rate_of_fire`, `phase`, `ship_system`, `mobility` |

The crafts are offered for parent tags only, except the weapon tags named above, and never for `weapon`, `repair`, `d_mods`, `fleet`, `conditional`, `damage_type`, `combat_readiness` or the star chart tags; `affinity` (low tech, midline, high tech) costs the same amount of all three.

A convoy carries roughly equal amounts of all three: each feedstock's amount is a shared amount rolled at random up or down by as much as 15%, so they are never quite even. The shared amount uses the quantity mechanics chart crafting items already have: a row per feedstock in `chart_materials.csv` style, 150 at tier 1 and 1000 at tier 15 and linear between (placeholders), the same for all three, then scaled by the run's crafting item and item quantity lines. It is split over the freighters by fleet points, so the cargo pods and the delivered cache take their usual shares.

The feedstock is spent at the Crafting Bench itself, in its Infused Crafting tray (see the bench rework below), never tied to a site. The tray lists every craft with its cost and applies it to the hull item in the socket, for hull items only to start:

- Reforge a non-unique hull item, guaranteeing a modifier with a chosen tag. Cost in that tag's feedstock, more for narrow tags (`front_shield`) than broad ones (`defence`).
- Add a modifier with a chosen tag and remove another at random, for a large cost in that tag's feedstock. The one deterministic craft, kept expensive.

The crafts are `ForgeOperation`s in core like the Derelict Forge's, refusing mirrored, corrupted and locked items as nanoswarms do, and respecting reserved modifiers, exclusive groups and the biome limit. The forge stays the place for picking an exact modifier (Graft a chosen part, Excise a chosen modifier, Perfect); the bench works by tag and is portable.

### Crafting Bench rework

There is no separate Requisition bench. The Crafting Bench (core `ui/socket`: `CraftingBench`, `CraftingBenchPanel`, `BenchSideBySide`) gains three expanding trays in Hull Items mode, each opening leftwards from its icon at the bench's right side, acting on the hull item in the socket. The Star Charts mode gets none of them and loses its inhibitors, so charts are crafted without any (its `usesInhibitors` turns false, as the forges' already is):

- Engrams: every engram the player holds, moved out of the bench's crafting items (`StarChartsDomain.hullCraftingBenchCraftingItems`).
- Infused Crafting: the feedstock crafts above, each with its cost and the player's count of that feedstock.
- Inhibitors: the inhibitors the player holds, of which up to four can be active (section 3, Excavation Sites).

Only one tray is open at a time. While one is open, the central arc of crafting items, the socket and the details window move further left to make room. Everything on the bench can still be used together: closing a tray keeps the active inhibitors, and opening another keeps them too. Engrams and infused crafts change the item as soon as they are used; inhibitors are the only items that wait, changing the next crafting item used. Each active inhibitor's icon orbits the item in the socket, in the central window, until that crafting item is used.

The Prefix and Suffix Inhibitors join the new inhibitors: their tiles beside the socket go, and they are picked in the Inhibitors tray like the rest, counting toward the four. They keep their battle and chart drops, made as rare as the Immutable Nanoswarm's (its `battle_loot.csv` and `chart_materials.csv` rows). Since they are core items that work without Star Charts, the tray, the inhibitor framework and the new inhibitors sit in core, and Star Charts adds the excavation drops. Each tray is a content hook, so the Engrams and Infused Crafting trays appear only while Star Charts is on.

## 3. The other anomalies

Each entry names the anomaly, its reward today, and its new reward. Entries marked decided are the user's picks; the user details the new inhibitors and the relay choices when their step starts. The rest wait on review.

### Depot (decided)

Today: a vault with more charts, blueprints and subroutines, and three revealed lanes if taken unseen.

After Heist's experimented bases, the vault holds prototype hull items: non-unique items with one extra implicit drawn from their base's pool (a new `prototype_implicits.csv` keyed by base), so they can't be crafted anywhere else. Reaching the vault unseen lets the player pick one of three prototypes laid out in its dialog before taking the rest; after the alarm the scuttling leaves one at random. A second, smaller idea: a picket destroyed before its report goes out drops intercepted orders that show the boss's fleet and loot multipliers on the chart tooltip.

Prototype implicits per base, decided 2026-11-13. Each base has three, except where one implicit comes in a version per damage or strike craft type. Effects marked new don't exist yet; values given are the user's, the rest are placeholders.

- Command Suite: +1 ship system charge (`SYSTEM_CHARGES_FLAT`); 50% reduced rate of combat readiness loss after peak performance time (`CR_LOSS_PER_SECOND_PERCENT`); increased weapon range for nearby escorts (`ESCORT_WEAPON_RANGE_BONUS_PERCENT`).
- Specialist Crew: less crew salary (`CREW_SALARY_MULT`); 10% increased quantity of hull items found (new); 10% increased quantity of crafting items found (new).
- Drive Assembly: more collision damage (`RAM_PROW_COLLISION_DAMAGE_MULT`); increased damage to slower ships (`DAMAGE_TO_SLOWER_SHIP_PERCENT`); 0.04% increased ballistic and non-beam energy damage per su/s of speed, half the tree's value (`NON_BEAM_WEAPON_DAMAGE_PER_SPEED_PERCENT`).
- Reactor Regulator: increased flux dissipation per Heartless stack (new); increased flux dissipation per Pirate Frenzy stack (new); 3% of EMP damage dealt is dissipated as hard flux (new).
- Fire Control Suite: 5-10% increased kinetic, high explosive, energy and fragmentation damage, one implicit each (four new); 5-10% increased EMP damage (`EMP_DAMAGE_PERCENT`).
- Shield Emitter: the ship's shield damage taken also applies to its armor damage taken at 25% effectiveness, so 30% less shield damage taken gives 7.5% less armor damage taken (new); 5% chance for energy projectiles hitting the shield to phase through it, dealing no damage (new, looking like a ballistic pierce); the same for ballistic projectiles (new).
- Phase Coil Array: no phase upkeep near wrecks, for up to 5 seconds per phase (`NO_PHASE_UPKEEP_NEAR_WRECK_RANGE` plus a new time cap); peak performance time paused while phased (`PEAK_TIME_PAUSED_WHILE_PHASED`); leaving phase fires EMP arcs (`PHASE_EXIT_EMP_ARCS_FLAT`).
- Launch Assembly: 10-15% increased fighter, interceptor, bomber and support role damage, one implicit each (`FIGHTER_ROLE_DAMAGE_PERCENT` and its role siblings).
- Armor Plate: 5-8% reduced kinetic, high explosive, energy and fragmentation damage taken by armor and hull, one implicit each (four new, since the existing damage-taken effects also cover shields).

### Excavation Sites (decided in outline)

Today: a planet's items leaning toward one drop type, behind a frigate fight or a raid.

The shafts are the main source of the new inhibitors, which can drop elsewhere too. They are never used on an item themselves: the player activates up to four in the bench's Inhibitors tray (section 2, Crafting Bench rework), and like Path of Exile's fossils they change how the next crafting item used behaves. The set:

- Prefix Inhibitor and Suffix Inhibitor, the existing two, working as today.
- Seven tag inhibitors, each making the next crafting item used that adds modifiers less likely to add modifiers of its kind: beam, non-beam energy, ballistic, missile, shield, armour and phase. They weight by theme tag (`theme_tags.csv`: `beam`, `non_beam_energy`, `ballistic`, `missile`, `shield`, `armour`, `phase`); how much less likely is a placeholder.

### Spatial Breach (decided)

Today: caches with rarity raised by depth.

Warped hull items from the deepest caches, carrying one modifier drawn from a different part's pool, chosen by the breach and shown as warped. Nothing else breaks part pools, so this is a new kind of item, deeper caches having a higher chance. Before building, check which part pools hold effects that only work on their own part (`SlotItemEffect` lines).

### Relay Network (decided in outline)

Today: harder systems down the chain and one unique of any source from the last boss.

After Incursion's room choices: each time the player wakes a relay, its dialog offers a choice of one of three modifiers, and the chosen modifiers change the reward the last stage's boss drops. The user details the modifiers when this step starts.

### Faction Skirmish (decided in outline)

Today: a capture on the attacker's side, and a weaker allied side that drops nothing.

When the run completes, the faction left standing trains one of the player's officers, picked in a dialog, with a new officer skill tied to Exiled Sector's own systems. It is a real Starsector skill (`skill_data.csv` and a `.skill` file), so it shows on the officer and travels with them. The user details the skill when this step starts.

### Remnant Nexus (decided)

Today: chart fleet loot from the station and its launches.

Each hull item the Nexus station itself drops has one modifier rolled 25% past its top roll, marked on the item as overcharged (built). Its launched fleets drop no chart items, so they are untouched. The Domain-era Mothership is unchanged for now; the earlier idea of one item with two biome modifiers is open.

### Stellar Flares (decided)

Every hull item dropped in a run system with Stellar Flares rolls one of its modifiers an additional time and keeps the better value, as lucky does in Path of Exile (`ModifierLine.bestRoll` picks the better end for inverted effects).

### Black Hole (decided)

Each hull item from the black hole's derelicts has a 10% chance to drop with one modifier already immutable.

### Dense Nebula (decided)

Today: +50% star charts.

Settling dust: each battle won in the nebula adds a stack for the rest of the run, and the boss drops more star charts per stack, capped. The nebula rewards clearing the system before the boss rather than rushing it.

### Unchanged

The Coronal Hypershunt is its own reward, as are the Derelict Forge, the Pre-Collapse Facility and the abyssal landmarks (the forge programs, VoK databanks, RAT drop groups and Abyssal Infusion). Magnetic Storm, Asteroid Storm and Neutron Star have no new reward yet; the Asteroid Storm's belt caches could hold the new inhibitors once those exist.

## Open questions

- Supply Convoy: which tag families each infused feedstock covers; the split above is a first guess.
- Supply Convoy: whether the bench should also craft subroutines, whose lines have tags too.
- Supply Convoy: whether infused feedstock should also be bought in the main sector, or stay chart-only.
- Faction Skirmish: whether staying out earns the training (from the attacker, left standing once the station falls) or nothing.
- Faction Skirmish: one skill for every faction, or one per faction; whether it counts against the officer's skill limit; what a run grants an officer who already has it.
- Dense Nebula: whether settling dust replaces today's flat +50% star charts or adds to it, and the per-stack amount and cap.
- Domain-era Mothership: whether it gets a reward of its own.

## Build order

1. [x] Storm Front change: the guaranteed biome grant, removal or move of the storm modifiers, strings (en and zh_CN), `docs/docs.md`, tests.
2. [x] The three infused feedstock items, their `special_items.csv` rows, icons (placeholders), their tier rows, the ±15% roll per feedstock and the convoy cargo split. Built: `items.InfusedFeedstock`, `anomaly.convoy.ConvoyFeedstock` with `convoy_feedstock.csv`, rolled in `ConvoyFleets.spawn` and shared into the pods and cache by `ConvoyCargo`; the icons reuse Nanite Feedstock's.
3. [x] The Crafting Bench rework: the right-side tray framework and content hook, the bench moving left while a tray is open, the Inhibitors tray with the Prefix and Suffix Inhibitors moved into it and active inhibitors orbiting the socket, inhibitors removed from the Star Charts mode, the two inhibitors' drop rates lowered to the Immutable Nanoswarm's, then the Engrams tray (engrams moved out of the crafting items), with tests. Built: `BenchTray`, `BenchTrayEntry` and `BenchTrayContext` in core `ui.socket`, offered through `ExiledSectorDomain.hullCraftingBenchTrays()` and `BenchModeSupport.trays()`; `InhibitorTray`, `ActiveInhibitors` and `InhibitorOrbit` in core; `EngramTray` in Star Charts' `ui.socket`; the layout in `BenchSpread.trayOpen`.
4. [x] The Infused Crafting tray and its crafts as core `ForgeOperation`s, with tests per craft. Built: the crafts are core `InfusedCraft` (logic in `InfusedCrafting`, costs and feedstock per tag in `infused_crafts.csv` read by `InfusedCraftCosts`), tested in hull-upgrades `InfusedCraftTest` and core `InfusedCraftCostsTest`. The tray is a filtered list (core `BenchTrayList` and `BenchTrayRow`, split into columns by `BenchTray.columns`), filled by Star Charts' `InfusedCraftingTray` with an `InfusedCraftEntry` per tag for the tagged reforge (100) and add-and-remove (1000); click confirms, ctrl-click crafts at once. The alike reforges, the tag value reroll and the damage type swap were built, never given costs, and deleted. The Adaptive and Grafting Nanoswarms became rows of the same tray, Adaptive Reforge and Biome Graft at 100 and 1000 of each feedstock (Star Charts `items.BiomeCraft` and `ui.socket.BiomeCraftEntry`, tested in `BiomeCraftTest`); their items and chart drops are gone. A row can show several icons, one cost beside each (`BenchTrayEntry.rowIcons`). The tagged reforge and Adaptive Reforge also take one Reshaping Nanoswarm, add-and-remove and Biome Graft two (`InfusedCraft.reshapingCost`, `BiomeCraft.reshapingCost`), each shown as its own cost row. Rows sort by feedstock cost, then by action (`InfusedCraftingTray.ROW_ORDER`). The Engrams tray shows its icons in a prefix and a suffix column (`EngramTray.sided`), the Inhibitors tray in two unsorted columns, both sized to their icons (`BenchTray.iconColumns`, `BenchTrayList.tiles`); each tray opens leftwards from the tray icons inside the stat panel border (`BorderedPanel`, `BenchSpread.trayOpen`).
5. [x] Depot prototypes: `prototype_implicits.csv`, the vault's pick of three, strings, tests. Built: core `items.PrototypeImplicits` reads `prototype_implicits.csv` (a pool per base, ids in `item_modifiers.csv` ending `_PROTOTYPE_IMPLICIT`) and adds one rolled implicit to a normal roll of the base; `ItemImplicits.withCurrentIds` keeps it through implicit changes. Star Charts `anomaly.listeningpost.VaultPrototypes` rolls three distinct bases from the vault seed, offered by `PostDialog` before the alarm and one left after it. `ItemDataIntegrity.checkPrototypeImplicits`, run from hull-upgrades `HullItemDataRules`, checks the file. The new effects (shield armor share and phase-through, damage type percents, armor and hull damage taken per type, EMP-dealt hard flux venting, capped wreck shelter, hull and crafting item quantity, Pirate Frenzy and Heartless dissipation per stack) live in their parts' packages. Tested in mod `PrototypeImplicitsTest`, `VaultPrototypesTest` and `ItemDataIntegrityTest`, and core `ShieldArmorShareListenerTest`, `ShieldPhaseThroughListenerTest`, `DamageTypeListenerTest`, `EmpDealtHardFluxVentListenerTest`, `FoundItemQuantityTest` and `WreckShelterListenerTest`.
6. [ ] Stellar Flares lucky rolls, Black Hole immutable drops and Dense Nebula settling dust.
7. [x] Remnant Nexus overcharged drops. Built: the Nexus had no reward beyond chart fleet loot, so nothing was removed. Core `items.ItemOvercharge` rolls one plain-range modifier 25% past its better end (`ModifierLine.overchargedRoll`) and marks it on the `RolledModifier` (`overcharged`, kept in saves and as an `o` after the slot letter in `ItemCodec`, which also writes `i` for immutable); the tooltip colours it and the F1 view heads it Overcharged. Rerolling the modifier's values (Tuning, corrupted value rerolls) clears the flag, Perfect leaves it, and `GrantedModifiers.add` never removes it. Star Charts `RemnantNexusStation` is a `LootShaper` whose `overchargesHullItems` picks the station fleet only; `StarChartLootSource.fleetLootRules` passes it on as `FleetLootRules.overchargesHullItems`, which `BattleItemDrops` plans as its own share, and `ChartFleetLoot.roll` overcharges the station's own hull items. Tested in hull-upgrades `ItemOverchargeTest` and `BattleItemDropsTest`, Star Charts `LaunchingStationTest` and `GrantedModifiersTest`.
8. [ ] Spatial Breach warped items, after the part pool check.
9. [x] The seven tag inhibitors, their `special_items.csv` rows, icons (placeholders), strings (en and zh_CN), the weighting in the modifier roll, and excavation shaft drops, with tests. Built: the inhibitors are core `CraftingInhibitor` values with no slot, an inhibited tag and battle drop weight 0; each kind can be active once (decided: no duplicates, so weights never stack), and a modifier matching several active ones gets one 0.2x weight (`CraftingInhibitor.tagWeights`). `CraftingContext` carries the weights to Augmentation and Reshaping (`ItemCrafting`), the infused crafts (`InfusedCraft`, `InfusedCrafting`), Star Charts `Engram` and Adaptive Reforge (`BiomeCraft`); crafts that add modifiers spend them (`CraftingMaterialFamily.addsModifiers`), Biome Graft stays unweighted. Shafts drop one per full raid and guardian fleets half that, scaled by the site quantity (`ExcavationLoot`, `ExcavationRules.INHIBITORS_PER_RAID`), with chart rows in `chart_materials.csv`. The icons are recolours of the Prefix and Suffix Inhibitors' icons (`graphics/icons/cargo/<kind>_inhibitor.png`). Tested in core `TagInhibitorWeightsTest`, mod `CraftingInhibitorTest`, hull-upgrades `InfusedCraftTest`, Star Charts `BiomeCraftTest` and `ExcavationRulesTest`.
10. [ ] Relay Network choices, once the user details them.
11. [ ] Faction Skirmish officer training: the skill, the end-of-run officer pick, strings (en and zh_CN), tests, once the user details the skill.
