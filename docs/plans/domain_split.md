# Domain split: plan

Exiled Sector is one Maven module and one jar, with about 700 classes. It holds three player-facing features and the code they share. This plan splits it into a library mod and four domains:

- Exiled Lib: a separate library mod holding the generic code (the stat effect library, the item framework, helpers and the domain framework), which other mods could build on later.
- Exiled Sector Core: the shared code that only Exiled Sector needs, such as the core hull mod, the ship record and the loot wiring.
- Ship Skill Trees: per-ship trees, nodes, levels, NPC trees and tree loot.
- Hull Upgrades: Hull Upgrades, ship sections and hull items.
- Star Charts: charts, chart runs, the Wormhole Gateway and chart currencies.

Exiled Sector stays one mod with one launcher entry, shipping a core jar plus one jar per feature, and it depends on Exiled Lib. Each feature domain depends only on Exiled Lib and Exiled Sector Core, never on another feature domain. Each one is switched on or off in LunaLib without touching the others.

Status: step 1 built (uncommitted on `parallel-work`), steps 2 to 9 planned. It builds on `module_toggles.md`, whose hull upgrade and star chart switches are built and whose skill tree switch is not. It also covers the station module share (`module_skill_tree.md`), built on branch `module_skill_tree` and merged into `parallel-work` on 2026-10-22, and the skill tree's faction fleets and Nexerelin compat (uncommitted on `parallel-work`).

## Decided

- The library is called Exiled Lib, mod id `exiledLib`. It is its own Starsector mod, with its own `mod_info.json`, version file and jar.
- Exiled Sector stays one Starsector mod. Its feature domains are switched by LunaLib settings, not by installing separate mods.
- Until 1.0, Exiled Lib's API can change in any release. Only Exiled Sector saves have to keep loading. Other mods that might use Exiled Lib aren't considered before 1.0.
- In code, "module" means a station module (`ModuleTreeShare`, `ModuleBoost`, `ModuleShares`, the Module Share setting). The framework that loads and switches the parts is called `Domain`, so the two never share a name.

## Names

| Part | Maven module | Jar | Owns |
|---|---|---|---|
| Exiled Lib | `exiled-lib` | `ExiledLib.jar` (in the `exiledLib` mod) | The stat effect library (`skills/skilleffect`, `modifiers`, `StatBaseline`, `DiminishingReturns`, `EffectTotals`, `DamageTakenCaps`, `ShipHullSizes`, `ShipFacts`), including each effect's station module class (`ModuleShare`, `ModuleShares`) and the module stat rebuild (`ModuleBoost`). The item framework (`Item`, `ItemStore`, `ItemType`, `ItemDefinition`, rolling, modifiers, implicits, corruption, crafting rules). `ModCsv`, `ModSettings`, `ModSettingsListener`, `Seeds`, `PlayerFleet`, `SaveData`, the `i18n` engine, `ui/util`, generic UI (`TooltipTable`, `VanillaText`, `Fade`, `SmoothZoom`, `SkillTreePanelStyle`), `motes` and the combat compat the effects use (GraphicsLib glow, Second-in-Command), the domain framework and its extension points. |
| Exiled Sector Core | `exiledsector-core` | `ExiledSector-Core.jar` | The `exiledSector_core` hull mod and its resolve, `ShipSkillData`, the loot listeners and `PlayerEngagementPipeline`, Nanite Feedstock and nanoswarms, the workbench shell, item storage and cargo icons, the refit button column and its station module tab guard (`RefitModuleTabs`), the settings page, the compat targets two domains use, `console/ConsoleShips`, the shared parts of `ui` and `effects`. |
| Ship Skill Trees | `exiledsector-skill-tree` | `ExiledSector-SkillTree.jar` | `skills` (except the parts in Exiled Lib), `skills/*`, `ui/node`, `ui/refit` (tree parts), `ui/decoration` (including the faction fleets and their battles), `ui/belt`, `ui/exoship`, `ui/hyperspace`, `ui/inspect` (tree parts), the station module share (`ModuleTreeShare`, `ModuleShareSetting`), `TaniaCompat`, `NexerelinCompat`, the `Item` subclasses that socket into the tree (`Subroutine`, `WormholeAnchor`), Doctrine Archives and Modspec Caches. |
| Hull Upgrades | `exiledsector-hull-upgrades` | `ExiledSector-HullUpgrades.jar` | `ui/section`, `ui/socket` (section parts), `HullItem`, the `HullUpgrade*` classes and `ShipSections`. |
| Star Charts | `exiledsector-star-charts` | `ExiledSector-StarCharts.jar` | `charts`, `ui/chart`, `StarChart` and the other `StarChart*` classes in `items`. |

The table is a summary. `domain_split_classes.csv` assigns every class and wins where they differ.

`Item` and the rest of the item framework live in Exiled Lib. Each feature domain owns only its own `Item` subclasses and the code that drops, shows or applies them.

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

Exiled Lib holds generic code that knows nothing about Exiled Sector:

- No `exiledSector` data paths, string keys, setting ids or persistent data keys. A consumer mod registers its own data files, string catalogue and settings with the library, and Exiled Sector does the same from Exiled Sector Core.
- No art, sounds or other assets. The GGG loot sounds and the RAT and Nightcross assets stay in Exiled Sector under their own permissions.
- Its own string catalogue (`data/strings/exiledLib/`) for the text its classes produce, such as effect tooltip lines. The `i18n` engine merges the catalogues of every registered mod.
- The extension point interfaces listed below, so a feature domain implements library types, not Exiled Sector Core types, wherever a library type fits.

Exiled Sector Core holds what at least two feature domains need but that is specific to this mod. The `exiledSector_core` hull mod is one example: its id is in every saved variant, so it keeps that id. The ship record, the loot wiring, the workbench and storage UI, Nanite Feedstock and nanoswarms, the refit button column and the settings page are others. Everything else goes to the feature domain that uses it. NPC trees, phantom hull mods and OP reserve hull mods belong to Ship Skill Trees.

## Extension points

Exiled Lib defines these interfaces in `exiledlib.domain` (`exiledsector.domain` until step 7). Exiled Sector Core wires them up, and the feature domains implement them. Once all of them exist, no feature domain needs to import another.

- `Domain`: id, `enabled()` (its LunaLib switch), `onApplicationLoad`, `onGameLoad`, `configureXStream`, `registerSettings`, `addCodexEntries` and `compatTargets()`, which `CompatChecks` logs at startup instead of keeping its own list. Exiled Sector's mod plugin runs every domain's hooks in a fixed order. Discovery is an explicit list, since the sandbox forbids reflection (`ScriptSandboxRestrictionsTest`).
- `ShipEffectSource`: contributes `SkillEffect` entries and fighter effects to a ship's resolve. The tree contributes allocated nodes, Subroutines, Doctrine and granted nodes. Hull Upgrades contributes section items, and Star Charts contributes fleet conditions. Diminishing returns stay joint across sources, as they are now. The resolve keeps each source's entries after diminishing returns, so the tree works out the station module share from its own nodes only. Section items and fleet conditions aren't shared onto modules, as now.
- `LootSource`: rolls a domain's drops for a battle, salvage site, tech mining result or market stock. It gets one shared context (player level, defeated DP, site type).
- `FleetDifficulty`: reports a defeated fleet's strength. Exiled Sector Core falls back to DP, and the tree reports NPC tree levels. Chart fleets and battle chart tiers use it, which also solves the knock-on effects of turning the tree off (see `module_toggles.md`).
- `RefitButton`: Exiled Sector Core owns the column, its anchoring and the station module tab guard, and domains add buttons. MagicLib's paintjob button anchors on our last refit component, so core keeps that component last whichever buttons are present.
- `BenchMode`: the workbench asks domains for its modes (hull items, star charts) instead of checking `StarChartSettings`.
- `ItemEffects` already works this way, with one resolver per `ItemType`. Exiled Lib keeps it, and each domain registers its resolver.

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

### Step 1: assign every class (done, uncommitted on `parallel-work`)

`docs/plans/domain_split_classes.csv` gives every main class one part: `lib`, `core`, `skill-tree`, `hull-upgrades` or `star-charts`. `DomainBoundaryTest` fails when a class is missing from the CSV, and when a class reaches across a boundary that isn't in `src/test/resources/domain_boundary_allowlist.txt`:

- Exiled Lib using anything from Exiled Sector.
- Exiled Sector Core using a feature domain.
- One feature domain using another.

It reads imports and same-package class names from the source, like `ScriptSandboxRestrictionsTest`, with no build plugins. It also fails on allowlist entries that no longer cross, so the list only shrinks. It starts at 311 crossings, 153 of them core to the tree. The mod plugin has 56, and `ItemDataIntegrity`, `CargoIconLooks`, `SkillTreeHullMod`, the settings page and `ShipSkillData` have 13 to 17 each. A new class needs a CSV row. The save fixture test is described under Save compatibility.

### Step 2: domain framework and bootstrap

Add `Domain`. Split `ExiledSectorModPlugin`'s loading sequence into core plus three domain classes, keeping today's order wherever one domain's load reads another's data. One example is `NodeGrantLinks.install`, which runs after the item definitions load. Split `ExiledSectorSettings` the same way. Each domain registers its own section, in today's page order (the Module Share setting goes in the tree's), so the LunaLib page looks the same. Move each compat target into its domain's `compatTargets()`, keeping today's log order (`CompatChecksTest` pins it).

### Step 3: ship effects

Add `ShipEffectSource` and rebuild `ResolvedTree` on top of it. Move `ShipSections` and section effects to Hull Upgrades, `ChartFleetConditions` to Star Charts, and node and Doctrine resolution and `ModuleTreeShare` to the tree. The resolved effect totals and the station module shares (`ModuleTreeShareTest`) must match the current resolver exactly. Test this on fixed ships before deleting the old path.

### Step 4: loot and difficulty

Add `LootSource` and `FleetDifficulty`, and split the four loot listeners into one core listener per event, plus a loot source per domain. `ChartDrops.battleTier` and `ChartTiers` read `FleetDifficulty`.

### Step 5: untangle the rest

Move the remaining classes to match the CSV, clearing each allowlist entry as you go:

- Split `ItemStore`'s chart-specific parts behind the store, without renaming the class.
- Move the star chart item classes to Star Charts, and the tree loot items to the tree.
- Add the refit button and bench mode registries.
- Make the library classes take their data paths, string catalogue and setting ids from a registering mod instead of reading Exiled Sector's directly.

Split the strings into one catalogue per part and language, with `data/strings/exiledLib/` for the library and `data/strings/exiledSector/<domain>/` for the rest. `i18n` merges them. `TranslationKeyReferencesTest` and the editor round-trip tests must still pass. Data files keep their paths, and each file's part is recorded in the CSV.

### Step 6: Maven modules

Turn `pom.xml` into a parent pom with five child modules. The game jar dependencies stay in the parent, and the surefire `--add-opens` args move with the tests. Tests move with their classes, and shared test fixtures come from a test jar. The allowlist must be empty by now, and Maven then enforces the boundaries at compile time. `DomainBoundaryTest` stays as a cheap guard.

Update `build-common.ps1`, `deploy.ps1` and `release.ps1` for the new jars and `mod_info.json`'s `jars` list. The release zip's required file check lists every jar. Until step 7, `ExiledLib.jar` ships inside the Exiled Sector mod folder.

### Step 7: Exiled Lib as its own mod

Create the `exiledLib` mod folder with its own `mod_info.json` (LazyLib, MagicLib and LunaLib as needed), version file, jar and string catalogue. Add `exiledLib` to Exiled Sector's dependencies. Move the library classes to `exiledlib.*` packages, registering a legacy-name alias for any persisted class saved by full name, updating the class names in data files, and run the save fixture test. Deploy writes both mod folders, and the release zip holds both, so players still download one file.

### Step 8: switches

Every domain's `enabled()` gates its effect sources, loot sources, refit buttons, bench mode and settings section. Hull Upgrades and Star Charts reuse their existing switches. The skill tree switch planned in `module_toggles.md` becomes one more `enabled()`. Its effect source, loot source and station module share stop, and `FleetDifficulty` falls back to DP.

### Step 9: docs

Update `CLAUDE.md`'s layout and build sections and the `docs/docs.md` sections whose code moved. Add Exiled Lib to the README and forum post install instructions and requirements.

## Not covered

The tools/editor serialisers read the same data paths, so they don't change. Per-hull-size trees are their own change, and this plan doesn't block them. A stable public API for other mods waits for 1.0.
