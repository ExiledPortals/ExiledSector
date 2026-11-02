# Naming consistency plan

Goal: what a player sees names the code behind it. Searching the code for a name on screen ("Prefix Inhibitor", "Engram", "Refracting Projectiles", "volume") should land on the class, id, key and data row for it. Where one word means two things, one of them gets a new name.

This plan collects a read-only audit of every domain done on 2026-11-02. Skill type and node ids have their own plan with save migration: [node_id_renames.md](node_id_renames.md).

## How each kind of rename is done

- Code-only names (classes, methods, packages, fields that aren't saved) use a rename that skips string literals. Then run `TranslationKeyReferencesTest` and diff every changed literal. Moved saved classes keep their XStream alias strings.
- Translation keys are renamed in `en.json` and `zh_CN.json` together with the code lookups. Keys built in code (`AtlasNames.name(category, id)`, `skillType.<id>.*`) need the builder changed too.
- Saved names (XStream aliases, memory keys, persisted fields, special and commodity item ids, LunaLib setting ids) need an alias or a load-time migration, like `item_id_aliases.csv`, `effect_aliases.csv`, `MaterialMigration` and `NodeReplacements`. Pre-1.0 saves don't block a change, but a renamed LunaLib id silently resets that player setting.
- Data ids (CSV columns and ids shared across files, the editor, or other mods) are renamed in every file in one pass, and the editor serialisers learn the new column name.

Rare item names don't need protecting. They can change every release, so `FrozenName` needs no aliasing. That also removes a latent hazard: `FrozenName` currently runs stored modifier ids through the effect-name alias lookup (`RolledEffect.currentName`).

## Decisions needed

Each of these picks the canonical word; the work items below follow from it.

1. The crafting screen. Decided 2026-11-02: "Crafting Bench" everywhere. Today players see "Crafting" (buttons, keybind), "Hull Item Modification" / "Star Chart Modification" (titles), "Modify", "workbench" and "map device". The code says `ModificationBench*`, `SocketWorkbenchPanel`, `HullBench`, `StarChartBench`, `ForgeBench`, `ModificationBay` and `ui.workbench.*`. All of these become "Crafting Bench" on screen, `CraftingBench*` in code (bench modes as `HullCraftingBench`, `StarChartCraftingBench`, `ForgeCraftingBench`) and `ui.craftingBench.*` in keys. The keybind setting id `exiledSector_modificationBenchButtonKey` is saved, so moving it resets the keybind once.
2. Tree regions. Decided 2026-11-02: "volume" everywhere. Today players see "volume" (Hegemony volume, wormhole tooltips) and also "area" (settings "Optional Areas", console Lock/UnlockAreas). The code splits about evenly between `region`, `volume` and `area`. Settings text, tree tags (`region` tag group), the CSV `area`/`region` columns, `ItemAreas`, `AreaToggles`, `TreeRegionFilter` and the `hyperspace.region.*` keys all become volume. Console Lock/UnlockAreas toggles unlock gates (it includes Pascal, which isn't a volume), so it becomes …Gates. The per-mod LunaLib toggles (`exiledSector_lostSectorArea` and the rest) are saved setting ids.
3. Chart ciphers. Decided 2026-11-02: the "Annulment Cipher" (removes every modifier from a Star Chart) becomes the "Erasure Cipher". The "Annulment Nanoswarm" (removes one random modifier) keeps its name. The cipher's enum becomes `StarChartCipher.ERASURE`, and its key and id use `erasure` (`chart.currency.erasure.*`, `material.exiledSector_erasure_cipher.*`, item id `exiledSector_erasure_cipher` through `item_id_aliases.csv`), along with the `special_items.csv` and `item_crafting.csv` rows and `zh_CN.json`.
4. "Nexus". Decided 2026-11-02: only the atlas infestation specials change. "Remnant Nexus" and "Threat Nexus" (`atlas_specials.csv` kind `infestation`) become "Remnant Infestation" and "Threat Infestation", and the generic "Infestation Nexus" becomes "Infestation". That covers ids `remnant_infestation` and `threat_infestation`, keys `chart.atlas.special.*_infestation`, `nexus_needs_modifiers`, and `AtlasOpening.NEXUS` / `NEXUS_MODIFIERS`. It also covers the `nexus` row of `atlas_node_effects.csv`, which is saved if atlas state stores special or effect ids, so check `AtlasState` first. The Remnant Nexus station anomaly and the Gateway Nexus tier stay as they are.

## Crafting materials and items (core)

| Player sees | Code says | Rename to | Kind |
|---|---|---|---|
| Prefix / Suffix Inhibitor | `CraftingWard` (128 refs), `respectsWards`, `wardsSpentBy`, `activeWards`, `guardedSlot(s)`, keys `ui.workbench.ward.*` / `notice.ward*` (15 core, 15 starCharts), ids `exiledSector_prefix_ward` / `suffix_ward` | `CraftingInhibitor`, `inhibitor` in keys and ids | code, keys, saved |
| Augmentation, Tuning, Fundamental, Reshaping, Metamorph, Immutable Nanoswarm | enums AUGMENTATION, RECALIBRATION, ATTUNEMENT, SUBSTITUTION, TRANSPOSITION, FIXATION; ids `*_kernel`; methods `retuneTrait`, `attune`. The icons already use the display names (`nanoswarm_tuning.png`) | enum `Nanoswarm` with TUNING, FUNDAMENTAL, RESHAPING, METAMORPH, IMMUTABLE; ids `*_nanoswarm` | code, keys, saved |
| Nanite Feedstock | "parts": `PARTS_ITEM_ID`, `partsCost`, `{parts}`, `notEnoughParts`, `battleParts`, id `exiledSector_socketable_parts`, LunaLib `exiledSector_socketPartsPer100DeploymentPoints`; but `item_salvage.csv` already says `feedstockMin/Max` | `feedstock` | code, keys, saved, data |
| Grow | `synthesise`, `synthesisable`, keys `tile.synthesise`, `notSynthesisable`, `synthesisedCurrency` | `grow` | code, keys |
| Immutable | `FIXATION`, `RolledModifier.locked` (saved field), `asLocked`, `hasLockedModifier`, key `alreadyFixed`. "Locked" also means "can't take currency" (`isLockedFromCurrency`) | `immutable` for the modifier, keep "locked" for the item state | code, saved field |
| Currencies / Inhibitors / Crafting Materials | "currency" covers three groups: `CraftingCurrency` (nanoswarms), `ItemCurrency` (adds ciphers), `ShipCurrency` (Refit and Master Schematics). Inhibitors and feedstock are "materials". `ItemDrops.kernelDrops()` returns `CurrencyDrop` including inhibitors | one `CraftingMaterial` family; `ShipCurrency` becomes a schematic name | code |
| Metamorph Nanoswarm text says "reshapes" | while "Reshaping" is a different swarm | reword the Metamorph text | text |
| Hull item section ("{type} section") | `ItemKind` held in variables named `socketType`; keys `socket.type.*`; `SocketTypeMigration`; editor label "Kind (socket type)"; console `[socket type]` | `section` throughout, keys `section.type.*` | code, keys |
| Command Suite, Launch Assembly, Specialist Crew, Armor Plate | ids `bridge_officer`, `flight_deck_crew`, `crew_quarters_specialists`, `armor_plating_plate` | ids from the names, through `ItemIdAliases` | saved, data |
| "Socket Items" panel (subroutines and anchors) | `ui.subroutines.*`, `openSubroutines`, also "Subroutines" and "Hull Item Storage" in other strings | one name; the panel holds tree items | keys |
| Explicit / Implicit tooltip headers | `socketable.tooltip.primaryData` vs `hullItem.tooltip.implicit` | `item.tooltip.explicit` / `.implicit` | keys |

Smaller ones:
- `AffixLayout.Limits` and `ItemRoller …WithEffectCount` count modifiers but say "effects". The player text has the same drift ("rolled effects will be lost").
- `ModifierLine` (template) vs `RolledEffect` (rolled line): rename the latter `RolledLine`.
- `item_modifiers.csv` `item_types` holds `ModifierScope` ids: rename it to `scopes`. Its `slot` column means affix position, so rename that to `affix`.
- `LootModifiers` are multipliers, not item modifiers.
- Stardust mote vs `CORROSION_MOTE_*` / `CorrosionMoteListener`.

The Socketable leftovers (`socketable.*` keys, `ItemStore` `socketables`, special item `exiledSector_socketable`, icon folder `socketables/`, memory keys) and the framework leftovers (`framework.*` keys, `exiledSector_framework_`, `frameworkPoints`) belong to the same cleanup. Do them as one saved-data pass with aliases.

## Hull upgrades

| Player sees | Code says | Rename to | Kind |
|---|---|---|---|
| Refit Schematic (the item) | id `exiledSector_hull_upgrade`, `HullUpgradeItemPlugin`, `HullUpgradeTooltip`, `upgrade.tooltip.*`, sound `exiledSector_loot_alert_hull_upgrade`, `HullUpgradeTest` | `RefitSchematic*`; keep `HullUpgrade*` for the points and sections system | code, keys, saved |
| "Hull Items" button and screen | `HullUpgradesRefitButton`, `HullUpgradesScreen`, `HullUpgradesLayout`, tab key `ui.socketPanel.tab.socketables`; package `ui.socket` beside `ui.section` | `HullItemsScreen`, `HullItemsRefitButton`; merge into `ui.section` | code, keys |
| Sections | console `ExiledSectorRemoveHullSockets` ("Locks every hull socket"), text "hull upgrade sockets", keys `framework.socket*`, `HullItem.canSocketInto`, `slotId` | section wording (a section *has* a socket) | text, keys, code |
| "Skill Tree Button Under Hull Mods" | moves all three refit buttons | "Refit Buttons Under Hull Mods" | text, saved setting id |
| Hull Upgrade Points (radio) | LunaLib saves the English option text, so it can't be reworded or translated | option ids, with labels from the catalogue | saved setting |
| "Hull Upgrades From Player Level" | tooltip says drops start; code `sectionsOpen()` | `hullItemDropLevel` | code |

Settings keys `settings.socketStorage.*` cover every item setting. LunaLib ids `socketableOtherModDrops`, `storeSocketMaterials` and `socketStorageHeader` are saved setting ids; rename them only with a migration.

## Skill tree

Type and node ids are covered by [node_id_renames.md](node_id_renames.md). The rest:

| Player sees | Code says | Rename to | Kind |
|---|---|---|---|
| Choose an option | "optional": `_optional` type suffix, `optionalOptions`, `isOptional`, `optionalSelections` (saved), "dropdown" renderer | "choice" node with "options" | code, data field, saved field |
| Remove / unallocate | `respec`, `release*` / `ReleasedNodes` (~76), `deallocate` (~22). `released_skill_nodes.json` means "shipped in a game release" | `deallocate` in code; ledger becomes `shipped_node_ids.json` | code |
| Unidentified / hidden / locked | `NodeGates`, `gate_*` tags, `HiddenNodeDisplayConfig`, `lockedFor`, ghost renderer. "Gate" also means `AllocationGate`, `AnchorJanusGate` and `summary.gated.*` | "gate" only for unlock conditions; `AllocationGate` becomes `AllocationCheck` | code |
| Starting location / starting point / start / root | `startingRootId`, `StartingRootChoice`, item "Root Conduit" | one player word ("root", since the item already shows it) | text |
| Item cost | `itemCost` vs `ItemChargeLedger`, `chargedItemIds` (saved); "charges" also means ship-system charges | "item cost" in code | code, saved fields |
| Free allocation | also "credit" (`addFreeAllocationCredit`, `pendingTemplateCredits`); three kinds of "free" node set | "free allocation" for the level token, "granted" for subroutine and pair nodes | code |
| Node-granting subroutine lines | keys `socketable.cache.*` used for every granting kind; code says `NodeGrant*` | keys `subroutine.grant.*` | keys |
| "Rewritten by" (doctrine) | `DoctrineBehaviour.TRANSFORM` (csv value) | `REWRITE` | data, keys |
| Mutually exclusive / can't be combined | `phantomConflict.*` keys show an internal term | `node.conflict.*` | keys |

Also:
- `gate_pascal_visited` now opens on the Nightcross anchor (`node_gates.json`), and `commands.csv` still describes a "Pascal" area.
- `sindrian_dictat` (region) vs the vanilla faction id `sindrian_diktat`.
- "ExoTech" vs "Exo-Tech" in player text.
- The tag `armour` vs "Armor".
- `fighter_*` type ids for strikecraft nodes, while `docs/tooltip_wording.md` keeps "fighter" for the wing role.

## Skill effects (core)

| Player sees | Code says | Rename to | Kind |
|---|---|---|---|
| Refracting Projectiles, "Max Projectile Refractions" | `NON_BEAM_ENERGY_WEAPON_CHAIN_*`, `EnergyChainListener`, `ChainLink`, `MaxChainCountConfig`, setting `exiledSector_maxChainCount`, `energy_chain_blocklist.csv`; the same package also has `Refraction*` | refraction | code, effect names (via `effect_aliases.csv`), saved setting |
| Borrowed Grace / Seraph Hunting Routines | `SERAPHS_GRACE_*` / `SeraphsGraceListener` vs `SERAPHIM_*` / `SeraphimListener` | `BORROWED_GRACE_*`, `SERAPH_HUNTING_*` | effect names, keys |
| Phase-Entangled Wings | `WingSharedFateListener`, docs "Wing Shared Fate"; "Shared Fate" is the shield skill | `PhaseEntangledWings*` | code |
| Empty Shell | `EVERY_AFFINITY_FLAT_PER_HOLLOW_NODE`, `hollow*` | empty shell | effect name, code |
| Volatile flux | `FLUX_SCALED_*`, `FluxScaledVolatilityListener`, key `combat.fluxScaled.title` | volatile flux | effect names, keys |
| Hardlight Marking | `FIGHTER_FOCUS_FIRE_*`, `FighterFocusFireMarks` | hardlight marking | effect names, code |
| Lion's Gaze | `BeamSplitListener` vs `SplitBeamDrones`, `split_beam_effect_blocklist.csv` | lions gaze, or at least one word order | code, data file |
| rate of fire | `*_FIRE_RATE_*` (10 names) vs `*_RATE_OF_FIRE_*` (14), node names "Weapon Fire Rate" | `RATE_OF_FIRE` | effect names |
| combat D-mods | `NON_LOGISTICS_DMOD_EFFECT_MULT_PER_DMOD` | `COMBAT_DMOD_*` | effect name |

Also:
- About 15 `stat.*` keys are camel-cased from old sentences (`stat.minimumCrewRequiredPerFighterBayCappedAt80Total`). Shorten them to the stat name.
- Package `skilleffect.hullmodule` holds `StationModuleSkillEffect`, which breaks the `<Part>SkillEffect` rule.
- `ARMOUR` / `diminishingArmour` (setting id) vs `ARMOR_*` (237 uses).

## Star Charts: runs, modifiers and loot

| Player sees | Code says | Rename to | Kind |
|---|---|---|---|
| Modifiers | `ChartEffect` enum, `chart.effect.*` keys (~130), `ChartFleetConditions`, "battle drawbacks", docs "player-side lines / enemy rules", tests "Drawbacks / Penalty" | `ChartModifier`, `chart.modifier.*`; `ChartFleetConditions` becomes `ChartPlayerModifiers` | code, keys |
| Quality (more items) | `StarChart.quality` (saved), `ItemTraits.quality`; "quality" also means fleet quality | "Quantity" on screen and in code | text, saved field |
| Engrams | `AnomalousDebrisCurrency(ies)`, `anomalous_debris_currencies.csv`, `chart.debris.currency.*`, `ui.workbench.notice.used.anomalousDebris.*` | `Engram*`, `engrams.csv` | code, keys, data |
| Gateway Array / Gateway Nexus | ids `_advanced` / `_perfected`; `ADVANCED_TIER = 5` (earned) vs `ADVANCED_MAX_TIER = 10` (allowed); `gatewayItem.*` keys for `WormholeTechItems` | constants `ARRAY` / `NEXUS`; keep the industry and research ids | code, keys |
| Station Guard | `FleetRole.DEFENDER`; "defender" also means salvage defenders, the skirmish side, and `alliedDefenders` ("the allied station") | `STATION_GUARD`, `alliedStationFleetId` | code, one saved field |
| Faction (the chart implicit) | `StarChartImplicits` still keyed by system type; `star_chart_implicits.csv` is header-only; `CHART_BASE_FLEETS` is dead | treat the faction as the implicit in code; retire the old path | code, data |

Also:
- "Strength" has three meanings: the level-scaling % (`STRENGTH_BONUS_MEMORY_KEY`, saved), DP defeated (`chart_drops.csv` `min_strength`, `strength_per_tier`), and combat points (`ALLY_STRENGTH_DIVISOR`).
- Settings keys `settings.starCharts.*` vs `chart.ui.settings.*` in one tab. "No Colony Requirement" and "Open Wormholes Anywhere" do the same visible thing.
- POI vocabulary: `*_BUDGET_PERCENT`, `ChartPoi.Kind`, `chart.siteCategory.*`.
- "Outer Reach Atlas" (15) vs "Outer Reaches" (14).
- Player words reused for something else: "Warded" (boss size), "Anchoring" (phase cost), "Anchor Reroll", "burnt-out wormhole anchor", "Explorarium anchor cache" (docs call it the Mothballed Equipment Cache), "Aegis" (suffix and Engram), and the two "Sensor Array"s.

## Star Charts: atlas and anomalies

| Player sees | Code says | Rename to | Kind |
|---|---|---|---|
| Biomes, "{region}, held by…" | `AtlasRegion` (row) and `AtlasBiome` (instance), `AtlasState.siteRegions` (saved), `atlas_regions.csv`, `chart_anomalies.csv` column `biomes` holding region ids; docs and tests say "pocket" | "biome" for the type, "biome pocket" for one placed | code, data, saved field |
| Anomalies ("Markers spent") | `AtlasMarkerRule`, `atlas_markers.csv`, `markers` column, `markerWeights`, `MARKERS_SPENT` (109 refs) | "anomaly chance", "anomalies spent" | data, code, keys |
| Listening Post | package `anomaly.heist`, `HeistRules`; `custom_entities.json` plugin path | `anomaly.listeningpost`, `ListeningPostRules` | code, data |
| Excavation Site | also "bolt-hole", "shaft", "cavern"; `Shaft*` classes (120 refs), `$exiledSector_shaftOpponent` | `Excavation*`; "bolt-hole" stays flavour | code, saved memory keys |
| Remnant Nexus / Domain-era Mothership | package `anomaly.station`, `LaunchingStation`; the plan calls them "biome landmarks" | `anomaly.landmark` | code |
| Relay Network stage | `RelayChain`, `AtlasAnomalyRecord.Chain`, "lines" in notes | "stage" | code |
| Storm Front | package `anomaly.storm` holds only Storm Front; `AsteroidStorm` and `MagneticStorm` are in the root; `storms` column = hyperspace storms | package `stormfront`; column `hyperspace_storms` | code, data |
| "hands the system over" | `AtlasState.capturedSystems` (saved), `AtlasCapture`, `REDEPLOY` | one verb | code |

Per-anomaly translation key prefixes don't match ids or packages: `chart.debris`, `chart.post`, `chart.nexus`, `chart.mothership`, `chart.storm`, `chart.breach`, and the intel keys are split two ways. Move them to `chart.anomaly.<id>.*` (about 150 keys) and the wormhole dialog to `chart.wormhole.dialog.*`.

## Editor and shared words

- The editor labels `SkillTier` as "Size", stores the hull item section as `kind`, still says "Modular Hull Socket" (`types.ts:122`), and names new nodes from the type id. The node rename plan covers the last one.
- "Anchor" has four meanings: wormhole anchors, tree orbit anchors (`orbitAnchors`, `orbitAnchorId`), `ShipAnchors` (section tile points) and `HyperspaceAnchor`. Keep it for wormhole anchors; the others become orbit centres and section points.
- "Part" has four meanings: skill-effect part packages, the i18n catalogue part, feedstock, and forge graft parts. "Slot", "site" and "rule" are overloaded the same way.
- `domain` (feature switch) vs the lore "Domain" (`RefitSchematic.DOMAIN`, `ChartPoi.Kind.DOMAIN`).
- British vs US spelling mixes in code (`Colour`/`Color`, `Centre`/`Center`, `Behaviour`/`Behavior`). Pick one for new code.

## Docs and text that are simply stale

- `docs/i18n/glossary-zh_CN.md` still lists Modular Hull Socket, Socketable Parts, Socket Modification, the Kernels and Synthesise, which contradicts `zh_CN.json`. Translators follow it, so fix it first.
- `special_items.csv` fallback text: "Modular Hull Socket Item", "Wards can only be found", "Replication Kernels can't be synthesised", "Fixed modifiers". `commodities.csv` and `descriptions.csv` have more of the same.
- `chart.ui.settings.debugTools.tooltip` puts the Complete button on the bench; it is on the atlas.
- `completed/charts_packages.md` lists `BuiltInTable` in the root and a `ChartObjective` that doesn't exist. `atlas.md` mentions `ChartExpansion` and `ChartPhenomenon`, and `atlas_catalysts.md` mentions `CatalystTile`.
- `tools/HullUpgradeIcons.java` generates 12 icons, of which only `hull_upgrade_cruiser_unique.png` is used. The old `*_kernel.png` cargo icons are used only by legacy `commodities.csv`.

## Order of work

1. Stale text and docs (the last section). No risk, and it fixes what translators and players read today.
2. Decisions 1-4, then the display-name fixes in [node_id_renames.md](node_id_renames.md) decision 3.
3. Code-only renames, one domain per change: classes, packages, enums that aren't saved. Each one is a rename plus a test run.
4. Translation key families, each with `en.json`, `zh_CN.json` and `TranslationKeyReferencesTest`.
5. Saved ids in one release, together with the node and type renames: material and item ids through `ItemIdAliases` / `MaterialMigration`, saved fields with readResolve or aliases, memory keys with a load-time copy. Leave LunaLib setting ids alone unless the reset is acceptable.
