# Domain split: plan

Exiled Sector is one Maven module and one jar, with about 700 classes. It holds three player-facing features and the code they share. This plan splits it into a library mod and four domains:

- Exiled Lib: a separate library mod holding the generic code (the stat effect library, the item framework, helpers and the domain framework), which other mods could build on later.
- Exiled Sector Core: the shared code that only Exiled Sector needs, such as the core hull mod, the ship record and the loot wiring.
- Ship Skill Trees: per-ship trees, nodes, levels, NPC trees and tree loot.
- Hull Upgrades: Hull Upgrades, ship sections and hull items.
- Star Charts: charts, chart runs, the Wormhole Gateway and chart currencies.

Exiled Sector stays one mod with one launcher entry, shipping a core jar plus one jar per feature, and it depends on Exiled Lib. Each feature domain depends only on Exiled Lib and Exiled Sector Core, never on another feature domain. Each one is switched on or off in LunaLib without touching the others.

Status: step 1 built (committed on `parallel-work`), step 2 built (committed on `parallel-work`), step 3 built (committed on `parallel-work`), step 4 built (committed on `parallel-work`), step 5 built (committed on `parallel-work`), step 6 built (committed on `parallel-work`), step 7 built (committed on `parallel-work`), step 8 built (committed on `parallel-work`), step 9 built (committed on `parallel-work`), step 10 built (committed on `parallel-work`). It builds on `module_toggles.md`, whose three switches are all built. It also covers the station module share (`module_skill_tree.md`), built on branch `module_skill_tree` and merged into `parallel-work` on 2026-10-22, and the skill tree's faction fleets and Nexerelin compat (committed on `parallel-work`).

## Decided

- The library is called Exiled Lib, mod id `exiledLib`. It is its own Starsector mod, with its own `mod_info.json`, version file and jar.
- Exiled Sector stays one Starsector mod. Its feature domains are switched by LunaLib settings, not by installing separate mods.
- Until 1.0, Exiled Lib's API can change in any release. Only Exiled Sector saves have to keep loading. Other mods that might use Exiled Lib aren't considered before 1.0.
- In code, "module" means a station module (`ModuleTreeShare`, `ModuleBoost`, `ModuleShares`, the Module Share setting). The framework that loads and switches the parts is called `Domain`, so the two never share a name.

## Names

| Part | Maven module | Jar | Owns |
|---|---|---|---|
| Exiled Lib | `exiled-lib` | `ExiledLib.jar` (in the `exiledLib` mod) | The stat effect library (`skills/skilleffect`, `ModifierEffect`, `StatBaseline`, `DiminishingReturns`, `EffectTotals`, `DamageTakenCaps`, `ShipHullSizes`, `ShipFacts`), including each effect's station module class (`ModuleShare`, `ModuleShares`), the module stat rebuild (`ModuleBoost`) and the drone drawing helpers (`Fade`, `SpriteCache`, `FallbackSupport`). `ModCsv`, `ModSettings`, `ModSettingsListener`, `Seeds`, `PlayerFleet`, `FleetCrewLedger`, the `i18n` engine, `motes` and the combat compat the effects use (GraphicsLib glow, Second-in-Command). |
| Exiled Sector Core | `exiledsector-core` | `ExiledSector-Core.jar` | `ExiledSectorDomain`, the item framework (`Item`, `ItemStore`, `ItemType`, `ItemDefinition`, rolling, `modifiers`, implicits, corruption, crafting rules, `SaveData`), the `exiledSector_core` hull mod and its resolve, `ShipSkillData`, the loot listeners, `PlayerEngagementPipeline` and `FleetCrewLedgerSettlement`, Nanite Feedstock and nanoswarms, the workbench shell, item storage and cargo icons, the refit button column and its station module tab guard (`RefitModuleTabs`), the settings page, the compat targets two domains use, `console/ConsoleShips`, the shared parts of `ui`, `ui/util` and `effects`. |
| Ship Skill Trees | `exiledsector-skill-tree` | `ExiledSector-SkillTree.jar` | `skills` (except the parts in Exiled Lib), `skills/*`, `ui/node`, `ui/refit` (tree parts), `ui/decoration` (including the faction fleets and their battles), `ui/belt`, `ui/exoship`, `ui/hyperspace`, `ui/inspect` (tree parts), the station module share (`ModuleTreeShare`, `ModuleShareSetting`), `TaniaCompat`, `NexerelinCompat`, the `Item` subclasses that socket into the tree (`Subroutine`, `WormholeAnchor`), Doctrine Archives and Modspec Caches. |
| Hull Upgrades | `exiledsector-hull-upgrades` | `ExiledSector-HullUpgrades.jar` | `ui/section`, `ui/socket` (section parts), `HullItem`, the `HullUpgrade*` classes and `ShipSections`. |
| Star Charts | `exiledsector-star-charts` | `ExiledSector-StarCharts.jar` | `charts`, `ui/chart`, `StarChart` and the other `StarChart*` classes in `items`. |

The table is a summary. `domain_split_classes.csv` assigns every class and wins where they differ.

`Item` and the rest of the item framework live in Exiled Sector Core. Each feature domain owns only its own `Item` subclasses and the code that drops, shows or applies them.

Java packages don't change while the code is untangled (steps 1 to 6). Classes move between Maven modules without changing their fully qualified names, so saves keep loading. Exiled Lib's classes move to `exiledlib.*` packages only in step 7, with legacy names registered for every persisted class (see Save compatibility).

## What exists

The import map (October 2026) has imports between the domains in every direction:

- Shared code to the tree: about 100 class imports.
- Tree to hull items: 50.
- Hull items to tree: 34.
- Charts to hull items: 32.
- Tree to charts: 9.
- Hull items to charts: 5.
- Charts to tree: 3.

The tangles that matter:

- One hull mod applies everything. `effects/SkillTreeHullMod` (`exiledSector_core`) builds `effects/ResolvedTree` from tree nodes, ship section items (`skills/ShipSections`) and Doctrine overrides, then applies `charts/ChartFleetConditions` on top. It also attaches `effects/ModuleTreeShare`, which pushes a share of the parent's node effects (after diminishing returns pooled with section items) and the Tania module node bonuses onto station modules through `ModuleBoost`. `ModuleTreeShare` reads package-private parts of `ResolvedTree`, so it sits in `effects`.
- Hull items and Subroutines reuse the skill effect classes directly. `items/ItemEffects` resolves both through `SkillEffect.byName`. Charts have their own `ChartEffect` and register a resolver in `ItemEffects`.
- `items/` holds the item framework, but also `StarChart`, `Subroutine`, `WormholeAnchor`, Doctrine Archives and Modspec Caches. Charts depend on `items` because a star chart is an `Item`.
- `effects/ItemLootListener`, `ItemSalvageListener`, `TechMiningItemListener` and `MarketItemStock` each roll tree loot, hull items and charts in one class. Battle chart tiers come from NPC tree levels (`ChartDrops.battleTier`).
- Chart fleets get their difficulty from NPC trees (`NpcFleetLeveller`, `ChartTiers.fleetStrengthRangeFor`).
- `ExiledSectorModPlugin` loads all four domains in one interleaved sequence, and `ui/ExiledSectorSettings` registers every LunaLib setting.
- `compat/CompatChecks` lists every compat target, so core imports compat classes that belong to the domains: `TaniaCompat`, `NexerelinCompat` and `LostSectorCompat` (tree) and `VaultsOfKnowledgeCompat` (charts).
- The faction fleets on the tree canvas (`FactionFleetRenderer`, `FleetBattle`) read vanilla faction relations, `NpcFactionVolumes` and Nexerelin alliances. All of that stays inside the tree.
- `skills/ShipSkillData` is the per-ship record. It holds tree fields (nodes, level, XP), socket fields (`socketedItems`, `chargedItemIds`) and section fields (`frameworkPoints`, `unlockedSocketTypes`, `frameworkItemsBySocketType`). Every field is a string or number collection.
- `items/ItemStore` is one class for both hull item storage and chart storage (`exiledSector_socketables`, `exiledSector_starCharts`).
- `en.json` and `zh_CN.json` hold every domain's strings in one file per language.
- The item framework and skill effects read `data/config/exiledSector/` paths, Exiled Sector string keys and `exiledSector_*` LunaLib setting ids directly.

## What Exiled Lib holds

Exiled Lib holds generic code that knows nothing about Exiled Sector. It exists so other mods can reuse the skill effects in their own implementations, so it holds only the effects and what they need to run. Code that only Exiled Sector uses goes to core, even when several of its parts use it.

- No `exiledSector` data paths, string keys, setting ids or persistent data keys. A consumer mod registers its own data files, string catalogue and settings with the library, and Exiled Sector does the same from Exiled Sector Core.
- No art, sounds or other assets. The GGG loot sounds and the RAT and Nightcross assets stay in Exiled Sector under their own permissions.
- Its own string catalogue (`data/strings/exiledLib/`) for the text its classes produce, such as effect tooltip lines. The `i18n` engine merges the catalogues of every registered mod.
- No extension points. The domain hooks and the item framework are Exiled Sector's own, so they live in core.

Exiled Sector Core holds what at least two feature domains need but that is specific to this mod. The `exiledSector_core` hull mod is one example: its id is in every saved variant, so it keeps that id. The ship record, the loot wiring, the workbench and storage UI, Nanite Feedstock and nanoswarms, the refit button column and the settings page are others. Everything else goes to the feature domain that uses it. NPC trees, phantom hull mods and OP reserve hull mods belong to Ship Skill Trees.

## Extension points

Exiled Sector Core defines every extension point, and a domain offers them through `ExiledSectorDomain`, which holds the `Domain` hooks below since step 10 merged that interface into it. Exiled Sector Core wires them up, and the feature domains implement them. Once all of them exist, no feature domain needs to import another.

- `Domain` (built in step 2): load, settings, codex, XStream and game load hooks, plus `compatTargets()`. Step 8 adds `enabled()`. Exiled Sector's mod plugin runs every domain's hooks in a fixed order. Discovery is an explicit list, since the sandbox forbids reflection (`ScriptSandboxRestrictionsTest`).
- `ShipEffectSource` (built in step 3): contributes `SkillEffect` entries and fighter effects to a ship's resolve. The tree contributes allocated nodes, Subroutines, Doctrine and granted nodes. Hull Upgrades contributes section items, and Star Charts contributes fleet conditions. Diminishing returns stay joint across sources, as they are now. The resolve keeps each source's entries after diminishing returns, so the tree works out the station module share from its own nodes only. Section items and fleet conditions aren't shared onto modules, as now.
- `LootSource` (built in step 4): records each player engagement and rolls a domain's drops for a battle, salvage site, tech mining result or market stock. Core keeps the per-battle state and the shared random, so draws stay in today's order. A source can also report the item chances and drop multipliers at a fleet or location.
- `FleetDifficulty` (built in step 4): the tree reports a defeated ship's NPC tree level (0 without a tree), which battle hull items and battle chart tiers use. Star Charts reports a chart fleet's raw level range and seed, and the tree clamps the range to its own level caps. This also solves the knock-on effects of turning the tree off (see `module_toggles.md`).
- `RefitButton`: Exiled Sector Core owns the column, its anchoring and the station module tab guard, and domains add buttons. MagicLib's paintjob button anchors on our last refit component, so core keeps that component last whichever buttons are present.
- `BenchMode`: the workbench asks domains for its modes (hull items, star charts) instead of checking `StarChartSettings`.
- `ItemEffects` already works this way, with one resolver per `ItemType`, and each domain registers its resolver. It is core, with the item framework.

## Save compatibility

Only Exiled Sector saves have to keep loading. A save loads as long as every persisted class still resolves from the name the save holds: its XStream alias, or its fully qualified name when it has none.

- Steps 1 to 6 keep every package, so moving a class to another Maven module changes nothing in saves.
- Aliased classes (`ItemSaveAliases`, `ChartSaveAliases`, `ShipSkillData`) can move freely, as long as their alias registration moves with them.
- No class is saved by its full name today. Every persisted root and every polymorphic element (ship records, item stores, items, rolled modifiers, the chart run, the objective intel, the wormhole plugin, the Gateway) has an alias. Enum and record fields such as `CorruptionOutcome` and `ModifierSlot` are written under their field names. The template store and relocked gates hold only strings, and `ChartCampaignPlugin` is transient. A class that later gets saved by full name needs an alias before it changes package.
- `SaveFixtureTest` loads `src/test/resources/save_fixture.xml`, written by the build before step 2, and fails if the fixture or its resave names any class by its Java name. It must pass after every step.
- Data files name classes by full name: the hull mod scripts, the special item plugins (`ItemPlugin`, `MaterialItemPlugin`, `HullUpgradeItemPlugin`, `StarChartItemPlugin`), the chart wormhole entity plugin, `ChartBattlePlugin` in `settings.json`, the Gateway industry, `MoteImpactSound` as a projectile's `onHitEffect`, and the console commands. A package rename has to update these strings too.
- `ShipSkillData` stays one class in Exiled Sector Core. Its domain fields are only string and number collections, so it carries no domain types.
- Persistent data keys don't change.
- Package renames change code only, never string literals (`TranslationKeyReferencesTest` guards the translation keys).

## Build order

Steps 1 to 5 happen inside today's single Maven module, so the game keeps running after every step, and each step can be merged and tested on its own.

### Step 1: assign every class (done, committed on `parallel-work`)

`docs/plans/domain_split_classes.csv` gives every main class one part: `lib`, `core`, `skill-tree`, `hull-upgrades` or `star-charts`. `DomainBoundaryTest` fails when a class is missing from the CSV, and when a class reaches across a boundary that isn't in `src/test/resources/domain_boundary_allowlist.txt`:

- Exiled Lib using anything from Exiled Sector.
- Exiled Sector Core using a feature domain.
- One feature domain using another.

It reads imports and same-package class names from the source, like `ScriptSandboxRestrictionsTest`, with no build plugins. It also fails on allowlist entries that no longer cross, so the list only shrinks. It starts at 311 crossings, 153 of them core to the tree. The mod plugin has 56, and `ItemDataIntegrity`, `CargoIconLooks`, `SkillTreeHullMod`, the settings page and `ShipSkillData` have 13 to 17 each. A new class needs a CSV row. The save fixture test is described under Save compatibility.

### Step 2: domain framework and bootstrap (done, committed on `parallel-work`)

`exiledsector.domain.Domain` is implemented by `SkillTreeDomain`, `HullUpgradesDomain` and `StarChartsDomain`, which `ExiledSectorModPlugin.DOMAINS` lists in that order. The plugin keeps the core steps and calls each hook between them, so load steps that read another domain's data keep their order. For example, the item framework loads between `loadData` and `loadItems`. Game load has four hooks because core's save repair, loot listeners and socket custody sit between tree steps. The tree's battle listeners stay after the engagement pipeline (`ExiledSectorModPluginTest` pins it). The plugin is the composition root, the one class `DomainBoundaryTest` lets use every domain.

Each domain registers its own settings, so the page is grouped by domain: core, then the tree, a new Hull Upgrades header, then Star Charts. Unique Subroutines level moved under Ship Leveling. Setting ids and defaults are unchanged. Compat targets come from `CompatChecks.coreTargets()` plus each domain's, and the log is grouped the same way. The allowlist went from 311 to 234 crossings, and the plugin has none left.

### Step 3: ship effects (done, committed on `parallel-work`)

Each source returns `EffectContribution` records: the effects plus their mod id prefix, duration, vanilla hull mod and NPC scaling flags. `EffectTotals` scales, diminishes and totals them without knowing where they came from, and `ResolvedTree` turns them into entries. Its cache key is built from each source's `cacheKey`. `SkillTreeEffectSource` owns node and Doctrine resolution, the NPC bonus scale, doctrine fact binding, tree sync, the station module share and the phantom conflict checks. `HullUpgradeEffectSource` owns section items, and `ChartFleetConditionSource` applies the fleet conditions. `SkillTreeHullMod` resolves and applies the entries, then calls each source's hooks.

Before the old path was deleted, a test compared the old and new resolvers on 24 fixed ships built from the real tree and items. It covered every entry, mod id, magnitude, temporary node, shown total, diminished stat, phantom id and station module share, including temporary nodes, damage-taken caps, pooled multipliers, hull items, diminishing returns and NPC scaling. They matched exactly. The allowlist went from 234 to 214 crossings.

### Step 4: loot and difficulty (done, committed on `parallel-work`)

The four loot listeners stay in core and call `SkillTreeLootSource` (faction fleet drops, market wormhole anchors), `HullUpgradeLootSource` (battle hull items, tech mining section loot, market hull items) and `StarChartLootSource` (battle, fleet, salvage and tech mining charts, and loot modifiers). `ChartLevelRange` moved into the tree as `NpcNodeRange`. The one visible change is that battle Nanite Feedstock is now added and announced after the star chart loot instead of before it. The allowlist went from 214 to 198 crossings.

### Step 5: untangle the rest (done, committed on `parallel-work`)

Move the remaining classes to match the CSV, clearing each allowlist entry as you go:

- Split `ItemStore`'s chart-specific parts behind the store, without renaming the class.
- Move the star chart item classes to Star Charts, and the tree loot items to the tree.
- Add the refit button and bench mode registries.
- Make the library classes take their data paths, string catalogue and setting ids from a registering mod instead of reading Exiled Sector's directly.

Split the strings into one catalogue per part and language, with `data/strings/exiledLib/` for the library and `data/strings/exiledSector/<domain>/` for the rest. `i18n` merges them. `TranslationKeyReferencesTest` and the editor round-trip tests must still pass. Data files keep their paths, and each file's part is recorded in the CSV.

Step 5 starts at 198 crossings and is built in this order, with the tests passing after each:

1. Shared pieces (done, committed on `parallel-work`): the Lost Sector compat, Nightcross gravity drag reading and tooltip table the library uses moved to the library, `FighterBayOverflow` moved to core, the RAT Singularity ids moved into `RandomAssortmentCompat`, `ShipProfile` builds its own `ShipFacts`, and `SkillTreeSounds` takes a node size instead of a tier.
2. Registering mod (done, committed on `parallel-work`): the library reads the mod id, config directory, strings directory and key prefix (for setting ids and persistent data keys) from `HostMod`, which the mod plugin registers when it loads and `HostModExtension` registers for tests. Stat modifier and combat data ids keep their `exiledSector_` prefix. The allowlist went from 198 to 184 crossings.
3. Item types (done, committed on `parallel-work`): `Domain.itemTypes()` gives an `ItemTypeSupport` per `ItemType` (factory, save alias, definitions file and parser, tiers, rarity, availability, unique drop level, salvage extras, cargo price), which the mod plugin registers in `ItemTypes` when it loads. `ItemDefinition` holds a subroutine's kind as a plain `subtype`, read through `SubroutineKind.of`. `ItemDataIntegrity` moved to core with per-type `ItemDataRules` from each domain, cargo icon looks come from each domain's `ItemIconLooks`, and the tree registers its codex with `ItemCodexLink`. Crafting, the currencies, `ItemPlugin`, `HullUpgradeData` and `HullUpgradeRoller` moved to core, and `BattleItemDrops` moved to Hull Upgrades, whose settings now hold the battle hull item setting. `SocketCustody` waits for 6 and `ItemStore` for 4. The allowlist went from 184 to 85 crossings.
4. `ItemStore` (done, committed on `parallel-work`): NPC item ids resolve through their item type's support, hull upgrade stacks through a stack absorber core registers, and the store's save keys come from `HostMod.dataKey`, still `exiledSector_socketables` and `exiledSector_starCharts`. The chart store stays one `ItemStore` with its saved `holdsStarCharts` flag. The allowlist went from 85 to 83 crossings.
5. Workbench modes and refit buttons (done, committed on `parallel-work`): Star Charts gives the workbench its mode through `BenchModeSupport` (currencies, refusals, crafting, detail lines, wormhole opening and the chart picker), registered in `BenchModes`. Core's `RefitButtonColumn` lays out the buttons under the hull mods from each domain's `ButtonSpec` and keeps the Modification Bench button last; the tree's refit hook still triggers it. `ExoticaCompat`, which only makes room under Build In, moved to core with its compat target. The allowlist went from 83 to 73 crossings.
6. Ship record and hull mod (done, committed on `parallel-work`): `ExiledSectorDomain` hooks give core the tree's engagement steps (`engagementSteps`), fleet and ship change notices, NPC ship records (`shipRecord`, read through core's `ShipRecords.resolve`), the core hull mod's description, refit refresh and player fleet sync. `FleetDifficulty` gives the XP difficulty multiplier the loot uses. `SocketCustody` and `SocketTypeMigration` (now core) work through each domain's `ShipCustody` (tree sockets, sections), refunds are core `CargoRefund`s, and `ShipSkillDataManager`'s node repairs moved to the tree's `ShipTreeRepairs`. `BattleDifficulty`, `ShipCombatPlan` and a new `WrappingHullModEffect` are core, and `FleetCrewLedgerSettlement` is library. `ShipSkillData` stays core and holds only the saved fields and their plain reads and writes; its tree logic (allocation, grants, respecs, roots, anchor links, wakes) moved to the tree's static `ShipTrees`. With 8, the allowlist went from 73 to 28 crossings.
7. Hull Upgrades and the tree screen (done, committed on `parallel-work`): the ship's fit (`ShipProfile`, `WeaponKind`, `NodeRequirements`, `FittedShipProfile`) is core, and the tree adds its shield and fighter bay effects through the `fitEffects` hook. Core's `ShipEditSession` applies a ship change and refreshes the ship. The tree's `NodeAllocator` and Hull Upgrades' new `SectionEditor` both use it, so the Hull Upgrades screen no longer opens a tree session. The tree's summary and codex views get Hull Upgrades' summary line, contributions and installed items through `ShipSummaryPart`. The refit portrait and its drawing classes (`FleetHullVisuals`, `HullEngines`, `VanillaShipView`, `FlameBatch`, `HullFrame`, and the new `HullPoint` and `InspectedShip`) are core, and Hull Upgrades builds its own `ShipAnchors` from the portrait. The allowlist went from 28 to 0 crossings.
8. Star Charts' uses of the tree (done, committed on `parallel-work`): chart wardens roll Doctrine Archives through the `wardenDrops` loot source hook, and the Wormhole Gateway reads the Janus flag itself, honouring the tree's memory flag override through `ignoresMemoryFlagUnlocks`.
9. Strings split (done, committed on `parallel-work`): each part has its own `en.json` and `zh_CN.json`, in `data/strings/exiledLib/` and in `core/`, `skillTree/`, `hullUpgrades/` and `starCharts/` under `data/strings/exiledSector/`. `I18n` merges the library folder with the folders `HostMod` lists. A key sits with the part of the classes that look it up, in the lowest part they share, and a data translation sits with its data file's part. `TranslationKeyReferencesTest` checks that every literal key is in its class's part, core or lib. The editor's text page loads and saves every catalogue. Data files' parts are in `domain_split_files.csv`, which `DomainBoundaryTest` checks.

### Step 6: Maven modules (done, committed on `parallel-work`)

Turn `pom.xml` into a parent pom with a child module per part. The game jar dependencies stay in the parent, and the surefire `--add-opens` args move with the tests. Tests move with their classes, and shared test fixtures come from a test jar. The allowlist must be empty by now, and Maven then enforces the boundaries at compile time. The mod plugin lists every domain, so it moves to one more small module that depends on the other five. `DomainBoundaryTest` stays as a cheap guard.

Update `build-common.ps1`, `deploy.ps1` and `release.ps1` for the new jars and `mod_info.json`'s `jars` list. The release zip's required file check lists every jar. Until step 7, `ExiledLib.jar` ships inside the Exiled Sector mod folder.

Built: the root pom is the parent of `exiled-lib`, `exiledsector-core`, `exiledsector-skill-tree`, `exiledsector-hull-upgrades`, `exiledsector-star-charts` and `exiledsector-mod`, each in a folder of that name at the repo root. Each writes its jar (`ExiledLib.jar`, `ExiledSector-Core.jar`, `ExiledSector-SkillTree.jar`, `ExiledSector-HullUpgrades.jar`, `ExiledSector-StarCharts.jar`, and `ExiledSector.jar` for the plugin) into `jars/`, and `mod_info.json` lists all six; `release.ps1` takes its required jars from that list. Core's `ExiledSector` holds the mod id, log tag and host mod, and the plugin installs the domains through it (`ExiledSector.install`, which also fills the item type, data rule, icon look, bench mode and refit button registries), so core and the domains read `ExiledSector.domains()` instead of the plugin. `DomainContributions` caches each domain list. Tests run from the repo root and sit in the lowest module that sees every class they use; tests that need several domains are in `exiledsector-mod`. `ProjectFiles` finds the repo root and every module's sources, `GameStaticsExtension` loads `Misc` and `LunaSettings` under a mocked `Global` once per test run, and core's `DomainsExtension` installs the domains a module's pom names in `exiledsector.test.domains`. `DomainBoundaryTest` also checks that each class sits in its part's module. The editor's in-game descriptions run from `exiledsector-mod`'s classpath.

### Step 7: Exiled Lib as its own mod (done, committed on `parallel-work`)

Create the `exiledLib` mod folder with its own `mod_info.json` (LazyLib, MagicLib and LunaLib as needed), version file, jar and string catalogue. Add `exiledLib` to Exiled Sector's dependencies. Move the library classes to `exiledlib.*` packages, registering a legacy-name alias for any persisted class saved by full name, updating the class names in data files, and run the save fixture test. Deploy writes both mod folders, and the release zip holds both, so players still download one file.

Built: the `exiledLib` mod files (`mod_info.json` depending on LazyLib, MagicLib and LunaLib, `ExiledLib.version`, its version file list and the `data/strings/exiledLib/` catalogues) live in `exiled-lib/mod/`, and the lib module writes `ExiledLib.jar` into `exiled-lib/mod/jars/`. Exiled Lib has its own version, so Exiled Sector can release without bumping it. Maven follows the mods' versions: the parent pom and the Exiled Sector modules carry Exiled Sector's version, `exiled-lib` carries Exiled Lib's, and the parent's `dependencyManagement` pins Exiled Lib through its `exiledlib.version` property so the other modules name no version. `PomVersionsTest` and `VersionFileTest` keep the poms, `mod_info.json` files and version files in step. `release.ps1` refuses a mismatch between its `mod_info.json` and `ExiledLib.version`, and warns when its main code or mod files changed since the last release tag while its version didn't. Exiled Sector depends on `exiledLib` and no longer lists its jar. Every lib class and lib test moved from `exiledsector.*` to the same path under `exiledlib.*`, with the lib members other parts used made public. No lib class needed a legacy alias: every persisted lib class already has an XStream alias, and lib only adds transient scripts and combat listeners. The one data file naming a lib class (the stardust mote projectile's `onHitEffect`) now names `exiledlib.motes.MoteImpactSound`. `deploy.ps1` mirrors both folders into the game's mods folder, and `release.ps1` zips `ExiledSector/` and `ExiledLib/` and checks both. Tests find game files in either folder through `ProjectFiles.gameFile`.

### Step 8: switches (done, committed on `parallel-work`)

Every domain's `enabled()` gates its effect sources, loot sources, refit buttons, bench mode and settings section. Hull Upgrades and Star Charts reuse their existing switches. The skill tree switch planned in `module_toggles.md` becomes one more `enabled()`. Its effect source, loot source and station module share stop, and `FleetDifficulty` falls back to DP.

Built: `Domain.enabled()` defaults to on. `DomainContributions` leaves out a switched-off domain's effect sources, loot sources and summary parts, and puts them back when it is switched on; fleet difficulties come from every domain, so battle difficulty still scales loot. Refit buttons and bench modes are registered with their domain's `enabled()`, and the refit column re-anchors the shown buttons under Build In. Settings sections stay listed, so a switch can always be turned back on. Hull Upgrades off now also stops installed sections from applying. Star Charts stays enabled while a run is open, so the run plays out. The skill tree switch is `SkillTreeSetting`; what it turns off is listed in `module_toggles.md`. `FleetDifficulty.ratesShipLevels` tells Star Charts whether ship levels exist, and battle chart tiers fall back to defeated deployment points when they don't.

### Step 9: docs (done, committed on `parallel-work`)

Update `CLAUDE.md`'s layout and build sections and the `docs/docs.md` sections whose code moved. Add Exiled Lib to the README and forum post install instructions and requirements.

Built: `CLAUDE.md`'s build, deploy, layout and i18n lines were updated in steps 6 and 7, and its layout section now says how the domain switches gate core's hooks. `docs/docs.md` names classes, not packages or modules, and every class it names still exists, so it needed nothing beyond step 8's battle chart tier line. The README and forum post list Exiled Lib under Requirements and say the download holds both mod folders. The TriOS install link still lists only LazyLib, MagicLib and LunaLib, since Exiled Lib's version file only reaches `main` with this branch.

### Step 10: move Exiled Sector's own code Out of Exiled Lib (done, committed on `parallel-work`)

Step 1 put each class in the lowest part that sees every class using it, so Exiled Lib also took classes only because a lib class used them, and the plan sent it the item framework and `Domain`. Those are Exiled Sector's own. `ItemKind` and `ItemType` list Subroutines, the hull sections, Star Charts and Wormhole Anchors, `ItemData` names the `exiledSector_` cargo items, and only core runs `Domain`'s hooks. Before 1.0 nothing is designed for other mods, so they move to core instead of being made generic.

Move these out, from `exiledlib.*` to the same path under `exiledsector.*`, each into the lowest Exiled Sector part that sees every class using it (core when several parts use it, the tree when only the tree does):

- `items`, and `persistence/SaveData`, whose only user is `ItemStore`.
- `modifiers`, except `ModifierEffect`, which `SkillEffect` extends.
- `domain/Domain`, merged into `ExiledSectorDomain`.
- All of `ui` and `ui/util`. A mod using lib's skill effects draws its own tooltips and panels, so lib gives effect text as `StyledText` with `Style` marks and the drawing mod picks the colours. The combat drawing helpers the drone effects use (`Fade`, `SpriteCache`, `FallbackSupport`) stay in lib, moved into `skills/skilleffect` next to `RepairDroneRenderer` and `WeaponDroneShell`.
- `effects/FleetCrewLedgerSettlement`, which core's engagement pipeline runs and which prints its result through `VanillaText`. Lib keeps `FleetCrewLedger`, and a mod using it settles the crew change from `FleetCrewLedger.drain()` itself.

Also in lib, `DiminishedStat` reads its setting ids through `HostMod.settingId`, like the other lib settings. The ids stay the same.

Message keys that only moved classes look up move from the lib catalogue to core's. Update `domain_split_classes.csv`. Tests move with their classes. Every persisted item class has an XStream alias, so saves are unaffected, and `SaveFixtureTest` must pass. Exiled Lib's version bumps at its next release.

Built: everything listed moved to core, except `ItemRollOdds`, `SmoothZoom`, `ColorUtil`, `LineBatch` and `WobblyOrbit`, which only the tree uses and which moved to the tree. `TexturePreloader`, which only the mod plugin uses, went to core, since the plugin module holds nothing else. `ExiledSectorDomain` now declares the `Domain` hooks itself. The lib keys only core and the domains look up (`combat.crewLedger`, `combat.liveMunitions`, `socket.type`, `socketable`, `codex.socketables`, `hullItem.tooltip`) moved to core's `en.json` and `zh_CN.json`. `FontCoverageTest` stays in lib, and its check of the tooltip body font size against the Chinese atlas moved to core's `SkillTreePanelStyleFontTest`. `EditorRoundTripTest` reads only Exiled Sector loaders, so it moved to `exiledsector-mod` next to the other editor tests.

## Not covered

The tools/editor serialisers read the same data paths, so they don't change. Per-hull-size trees are their own change, and this plan doesn't block them. A stable public API for other mods waits for 1.0.
