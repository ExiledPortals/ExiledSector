# Star Charts: plan for procedurally generated star systems

This plan is based on three read-only investigations: Beyond the Sector (0.95), Exiled Space (0.96), and the 0.98a engine (decompiled) together with our own item code (re-checked after main's hull item refactor). File and line references for every claim are in those reports. This document keeps only the conclusions.

## What the two mods teach us

Exiled Space is PureTilt's "PoE maps in Starsector". Its ideas are the right ones; its build quality isn't.

Worth keeping:
- Chips carry rolled modifiers loaded from CSV. Each modifier is a plugin with before-generation hooks (which change the fleet parameters) and after-generation hooks (which change the finished system).
- A modifier's cost feeds both its drop weight and the reward.
- A loot budget is spent on points of interest, weighted towards ones whose cost is close to the average. (Since 2026-10-19 this is four site budgets, one per site category; see `chart_loot.md` section 2 and the Star Chart loot section of `docs/docs.md`.)
- Above a set FP, a commander skill shrinks deployment cost, so huge fleets can still be fought.
- The chip UI previews a sample fleet.
- Battle drops are seeded per ship, which resists save-scumming.

Its build:
- One permanent hidden system is wiped and refilled for each chip. The wipe leaves stale state behind, which breaks patrols.
- Nothing is seeded, and it writes to the shared list of used names forever.
- Objectives never finish.
- There are several crash paths and an infinite loop.

Beyond the Sector is the save-bloat cautionary tale. Each trip only calls `removeStarSystem`, but the system's hyperspace anchor, its star and gas-giant jump points, its gravity wells, a Remnant warning beacon and a permanent gate intel entry all survive. Because they survive, the whole system stays reachable and gets written to every save. Rejected generation attempts leak a full system each. It also:
- adds a duplicate permanent listener on every load;
- reserves every generated name forever;
- fakes the tutorial flag to block escape, which also switches off salaries and the colony economy.

Vanilla 0.98a does the job properly, in two classes:
- `AbyssalRogueStellarObjectEPEC` builds each temporary "Deep Space" system:
  - a fresh system with a unique optional id;
  - tags `THEME_HIDDEN`, `THEME_SPECIAL` and `TEMPORARY_LOCATION`;
  - a seeded generator;
  - catalogue-style names that don't touch the shared name list;
  - no Economy markets.
- `AbyssalLocationDespawner` later removes the system and then deletes its hyperspace jump points, gravity wells and anchor.

That pattern is what we build on, with three changes:
1. Our system has no hyperspace presence at all.
2. Cleanup is triggered when the run ends, not by a 10-day timer.
3. Cleanup is far more thorough, below.

## Engine facts the design depends on

- `createStarSystem` only adds the system to the sector's system list. Hyperspace presence is opt-in: the 6-argument `initStar`, `generateAnchorIfNeeded` and `autogenerateHyperspaceJumpPoints` are what add it. A system with no anchor and no jump points is still fully visitable through `doHyperspaceTransition`, and the hyperspace map simply doesn't draw it.
- `removeStarSystem` only removes the system from that list. Everything else that points into the system keeps it in the save. That includes:
  - hyperspace entities;
  - the player fleet's interaction target and plotted course;
  - the campaign map's last-viewed location;
  - vanilla's `StrandedGiveTJScript.prev`;
  - intel, listeners, routes, memory keys and other mods' caches.

  Removal must happen on the next frame, never while the player is inside.
- Fleets inside are not despawned by removal. They must be despawned explicitly so despawn listeners fire.
- `initStar` with 4 arguments still reserves the system's name in `ProcgenUsedNames`. If the system's base name is constant (for example "Charted Space") and uniqueness comes from `setOptionalUniqueId`, that list doesn't grow. Alternatively, use `initNonStarCenter` for starless types.
- Transverse Jump works from any system, but its `forceDisable()` blocks it each frame.
- A custom entity is better than a `JumpPointAPI` for the wormhole, because AI travel logic ignores it.
- The isolation tags vanilla and Nexerelin honour:
  - `THEME_HIDDEN`: hub missions, salvage specials, base managers and nearby events;
  - `THEME_SPECIAL`;
  - `SYSTEM_CUT_OFF_FROM_HYPER`: pirate, Luddic Path and merc fleets, academy and pilgrim fleets, and Nexerelin colonies;
  - memory `$nex_do_not_colonize`;
  - `DO_NOT_SHOW_STRANDED_DIALOG`.

  The one unfiltered consumer is Nexerelin's Special Task Group "patrol" target list.

## The Star Chart item

A chart is a third item type in the item system main introduced on 2026-10-18 (hull items and subroutines are the first two). It is `ItemType.STAR_CHART`, with a `StarChart extends Item` subclass. Main's `ItemType` was written with star charts in mind. Note the naming: main's `ItemKind` is the old socket type (subroutine or ship section). What this plan calls the item's type is main's `ItemType`.

Each chart instance holds:
- the shared item fields: definition id, seed, explicit and implicit rolled modifiers, and a frozen name;
- chart-only fields on the subclass: tier (1 to 15), quality, and a corrupted flag.

The seed plus the definition reproduce the same system every time, so a chart is a recipe, not a saved system. That makes it trivial to preview, to tooltip and to regenerate after a crash.

Tier sets:
- fleet size, as a percentage of a standard 50 fleet point fleet (about a vanilla heavy patrol), and the warden fleet's size on the same scale;
- officer levels, fleet quality (d-mods) and fleet strength (the NPC skill-tree size);
- the loot budget;
- which modifiers can roll, through a minimum-tier filter;
- the value band each modifier rolls in;
- the chance that the run drops a higher-tier chart.

Higher tiers add bosses and remove the easy system types.

Modifiers follow Path of Exile, using main's modifier layer of prefixes, suffixes and implicits.

Prefixes are difficulty. They raise reward quantity and rarity, as PoE's item quantity does:
- larger fleets;
- extra fleets;
- higher fleet quality;
- officers with elite skills;
- monofleets;
- an Omega or Remnant presence;
- storms and hazardous terrain;
- reduced sensor range;
- combat readiness loss;
- fuel drain;
- "enemies are hidden";
- a mirror of the player's fleet.

Suffixes are reward and run rules:
- loot quantity and rarity;
- extra caches or derelicts;
- hull item drop chance;
- blueprint chance;
- an extra boss.

Implicits are new since the first draft. Main binds implicits to an item kind, so a chart can carry a fixed base modifier for its system type, as PoE maps do. Examples: a "Nebula" chart always has reduced sensor range; a "Black hole" chart always has stronger loot.

### Building on the item system

Main already did part of what the first draft asked for. What still needs work:

| Need | State after main | Work left |
|---|---|---|
| A shared modifier catalogue | Done. `ItemModifiers` loads id, slot, `item_types` scoping, hybrid lines and names. Pools reference rows as `@ID:weight`. | Chart modifiers go in their own `star_chart_modifiers.csv`, loaded into the same `Modifier` records under `STAR_CHART`. Shipped hull item data and its released-item tests stay untouched. |
| Effects not tied to ship skills | Done in phase 4. `ItemEffects` picks a `ModifierEffects` resolver per item type; skill effects are registered for subroutines and hull items. Loading, validation, descriptions and `Item.modifierEffects()` all go through it. | Register a chart effect resolver for `STAR_CHART`. Its effect names must not overlap skill effect names, because `RolledEffect` renames through every resolver. Chart effects supply `beforeGeneration(ChartGenContext)` and `afterGeneration(ChartRun)` hooks. |
| Values that scale with tier | Done in phase 4, code only. `ModifierLine` has a `ValueScaling` (unscaled, per hull size, per tier band) read through a `ScaleContext`. Pool entries have a `TierRange`, and `ItemRoller.rollAtTier` and augment/substitute filter by tier. | Data syntax: a tier-band line format in `ModifierLine.parse` and min/max tier columns on pool entries, with the editor serialisers, `EditorRoundTripTest` and `ItemDataIntegrity` updated together. |
| Rarities | Done. `ItemRarity` is Common, Rare and Unique, inferred from the explicit modifier count; uniques come from their definition. Since phase 4 the affix limits are a per-type `AffixLayout.Limits` policy. | Nothing for rarity (decided 2026-10-18). Register chart limits only if charts should carry more than 4 explicit modifiers. |
| A chart codec | Partly done. The JSON codec and its modifier and name encoding are reusable, but there is one special-item id and no tier, quality or corrupted keys. | A separate special item `exiledSector_star_chart` with its own plugin and icon. Its data string reuses `ItemCodec`'s JSON, with optional `t`, `q` and `c` keys. `HullUpgradeData` is the precedent for a second id absorbed into storage. |
| Open-ended currencies | Done in phase 4, except the workbench. `ItemCurrency` is an interface; the eight `CraftingCurrency` kernels implement it, and `ItemCurrencies` is the registry used by crafting, materials, drops and cargo icons. Wards stay separate materials. | The workbench (`SocketWorkbenchPanel`) still builds its buttons from `CraftingCurrency.values()`; it was left alone while another session had uncommitted changes there. Then: `ItemCurrency` parameters, `ItemCurrencies.forType(type)` for the buttons, `currency.key()` for text keys, and `createsCopy()` / `mayChangeDefinition()` instead of comparisons against Replication, Transposition and Corruption. |

The base `Item` class carries ship-only methods (`skillEffects`, `canSocketInto`, `effectLines(hullSize)`). They are harmless for charts. A later refactor could move them into a ship-item intermediate class, but charts don't need it.

Existing hull item and subroutine behaviour must not change. `ReleasedItemsTest`, `ItemCodecTest`, `ItemSaveTest` and `ItemDataIntegrityTest` already guard it.

The chart currencies fit the three rarities. Main already has four of them as kernels (since commit e06f5104), and they can be extended to charts through `appliesTo`:
- augment: adds a modifier, up to the cap. A third modifier turns a Common chart Rare (Augmentation).
- recalibrate: rerolls modifier values (Recalibration).
- transpose: turns a unique chart into another unique of the same tier (Transposition).
- corrupt: Corruption already exists for hull items. It picks an outcome (nothing, implicit replaced, values stretched, modifiers rerolled) and makes the item permanent; charts need their own corruption pool.

New chart-only currencies:
- reforge: rerolls every explicit modifier, with rarity following the new count. This is close to Corruption's "modifiers rerolled" outcome.
- annul: removes every explicit modifier, or only the unprotected side while an inhibitor is on.
- tier-up: the "cartographer's" step.

Chart-only currencies must not drop from ship loot: `ItemDrops.pickKernel` picks from every registered currency, so they need a drop-source filter or a weight of 0 there.

Charts drop:
- from chart runs: the sustain loop, weighted towards the same tier or one above;
- rarely from high-strength battles, salvage and tech mining, through the three listeners hull items already use (`ItemLootListener`, `ItemSalvageListener`, `TechMiningItemListener`), with a `pickChart` beside `ItemDrops`' existing pickers;
- from a unique-chart unlock rule, like `ItemUnlock`.

## Storage, the Star Chart UI, and opening a wormhole

Decided (2026-10-18):
- Charts are lootable items. Like hull items, they are scooped from cargo into a persistent storage, with an absorb pass when the UI opens or on load.
  - The storage is main's `ItemStore` with its key parameterised by item type (for example `ItemStore.get(ItemType.STAR_CHART)`), or a `StarChartStore` built on the same class under its own key.
  - Keeping charts out of the hull item store means the hull item screen, which lists everything in its store, needs no change.
  - Absorb dispatches on the special-item id.
- A custom Star Chart UI, built from the same pieces as main's Hull Items outfit screen:
  - a new `StarChartPanel extends HoloPanel` (`ItemStoragePanel` is too hull-specific to reuse: install status, section filters, placement, custody);
  - the existing `ItemCell`, `ItemTooltip`, `ItemHoverTooltip` and workbench tiles;
  - `SocketWorkbenchPanel` as the selected-chart panel, once its recipe list comes from the currency registry for the chart type.

  The UI provides:
  - charts are organised by tier, 1 to 15;
  - selecting a chart opens a detail panel like the modify-hull-item UI;
  - currency is spent on the selected chart to roll or reroll its modifiers;
  - a big "Open Wormhole" button.
- Eventually a colony building enables wormhole opening. Until it exists, a dev or settings switch allows opening anywhere, so the system can be built and tested first. The building can then gate where and when the button works, and whether it works at all.

Opening consumes the chart and creates a Wormhole entity beside the player fleet, in hyperspace or a normal system. It is a custom entity with our own plugin. Since 2026-10-18 its visual is a port of the vanilla wormhole terminus renderer (`JumpPoint` with `$wormhole` set), using the vanilla sprites: masked starfield or hyperspace backdrop, coronas, 100 rings, swirl bands and glow, opening when a fleet heads for it and closing 5 seconds after, except in hyperspace.

Run rules (decided):
- **Entries are a LunaLib setting.** "Star Chart wormhole entries" defaults to 1.
  - Each pass through the outer wormhole uses one entry.
  - Inside, an exit wormhole leads back to the outer wormhole. Leaving keeps the run open while entries remain.
  - When the last entry is used, the outer wormhole collapses behind the player. Leaving through the exit wormhole then ends the run.
  - With the default of 1, this means entering collapses the outer wormhole, and leaving ends the run.
  - The setting is read when a wormhole opens and stored on the run, so changing it mid-run doesn't affect that run.
- **No time limit.** The system exists for as long as the run is open.
- **One open run at a time.** "Open Wormhole" is disabled while a run is open.
- **Collapsing.** While outside, the player can collapse the outer wormhole at any time to end the run. The chart was already consumed when it opened.
- **Anything left inside is deleted when the run closes,** including jettisoned cargo, unrecovered ships, unlooted caches and fleets.

The run also ends if:
- the player's whole fleet is destroyed inside. Normal vanilla respawn moves them out; we detect the location change and close the run.
- the player escapes some other way, such as another mod's ability we didn't block. We detect the location change and close the run.

## The generator

`ChartGenerator.generate(chart)` is pure, deterministic from the seed, and runs in two stages.

Stage 1 is layout:
- A fresh `StarSystemAPI` is created with base name "Charted Space", a unique optional id `exiledSector_chart_<runId>`, and our tag `exiledSector_chart_system`.
- The vanilla isolation tags listed above are added, plus `setDoNotShowIntelFromThisLocationOnMap(true)`.
- Its location is placed far outside the sector, never at the origin or near real systems.
- A seeded `StarSystemGenerator` is used through `init(system, age)` and `GenContext`, following the abyssal code, with the static random swapped in and restored afterwards. It reuses vanilla `addStars` and `addPlanetsAndTerrain`, so planets, moons, rings, belts, magnetic fields, coronas and planet conditions look native.
- No anchor, no autogenerated jump points, no `NameAssigner`, and no Economy markets.
- Names come from catalogue ids, or from a chart-themed name list that is local to the run.
- Planets are condition-only and pre-surveyed to the "seen" level. They stay surveyable for flavour, and colonising is blocked by a rules hook.

Stage 2 is content, spending the tier's loot and fleet budgets:
- Loot points of interest come from `BaseThemeGenerator` locations plus `addSalvageEntity`, using our own `chart_poi.csv`:
  - supply caches;
  - weapon and technology caches;
  - derelict ships;
  - research and mining stations;
  - an orbital habitat;
  - a guarded "vault" at higher tiers (removed 2026-10-19; warden spoils replace it, see `chart_loot.md` section 4).
- Each entity gets a salvage seed derived from the chart seed, and a salvage-spec override, so hull item and chart drop rules apply.
- Fleets come from `FleetFactoryV3`:
  - added directly to the system, with no RouteManager;
  - each guards a point of interest (aggressive orbit) or patrols the system;
  - `NO_JUMP`, never auto-despawn, no going dark, permanently hostile to the player;
  - a boss fleet at higher tiers;
  - every fleet is recorded in the run.
- Every fleet gets `$exiledSector_chartTier` before levelling, so `NpcFleetLeveller` scales NPC trees and carried hull items by tier instead of by player power.
- Factions come from `chart_factions.csv`, but fleets stay on their real faction and hostility is enforced through memory flags. Exiled Space's "unknown faction" trick isn't needed. Since 2026-10-19 the faction is the implicit every chart shares ("The system is inhabited by X"): the chart rolls it by weight when it drops and stores it, so a later change to the list leaves charts already rolled alone. The list holds only vanilla factions for now and has no tier gating. A chart whose faction isn't loaded, or an opening without a chart, picks one by weight when the system generates. Corruption's implicit replacement swaps the faction for another listed one.
- Objectives are "clear the guardians" or "kill the boss" (a third, "loot the vault", was removed on 2026-10-19; warden spoils replace it). Finishing one gives a completion reward and a chance at a chart drop, and the objective's intel ends with the run.

## Isolation

Rules for the system:
- No hyperspace presence at all.
- No Economy markets.
- Every fleet inside is `NO_JUMP` and has local assignments only.
- No jump points; the wormhole entity is the only door.

A transient script runs while the player is inside, including while paused. Each frame it:
- force-disables Transverse Jump;
- force-disables any configured escape abilities from other mods. That list lives in a CSV, so new mods can be added without code.

## Lifecycle and cleanup

This is the most important part. The aim: once a run closes, a save contains nothing of it.

1. **A single owner.** `ChartRun` lives in persistent data and stores only strings and numbers: run id, the system's unique id, the wormhole entity ids, the origin location id, the chart JSON, state and timers. It never stores a `StarSystemAPI`, entity or fleet. Everything the run creates goes through a `ChartRunFactory` that tags it with the run id. That covers entities, fleets, intel, scripts and memory keys. Nothing is created any other way.
2. **States.**
   - `OPEN`: the outer wormhole exists and the player is outside.
   - `INSIDE`: the player is in the system. The outer wormhole remains while entries are left; it collapses on the last entry.
   - `CLOSING`: entered when any of these happens:
     - the player leaves with no entries left;
     - the player collapses the outer wormhole while outside;
     - the player is respawned outside;
     - the player escapes some other way.
   - `CLOSED`.

   Only one run exists at a time. The run stores its entry allowance and the entries used.
3. **Close order.** Closing runs from a script on the next frame, once the player is out and not mid-transition:
   1. Clear the player's interaction target and plotted course.
   2. Reset the campaign map's last-viewed location if it points at the system. This needs a MethodHandle, because there's no API setter.
   3. Re-add `StrandedGiveTJScript`, which drops vanilla's stale reference.
   4. Despawn every fleet in the system with `despawn(...)`, so listeners fire.
   5. End every intel item the run created, and any intel whose map location is in the system.
   6. Remove our scripts and listeners for the run, and clear our memory keys.
   7. Remove every entity in the system while its listener is still attached, which clears the engine's id map.
   8. Remove both wormhole entities and any hyperspace entity that points into the system. There should be none; this is a safety net.
   9. Call `removeStarSystemNextFrame`.
   10. Delete the run record.

   Every step is idempotent and wrapped, so a failure in one step can't stop the rest.
4. **A sweep on load.** Every `onGameLoad` runs a purge of orphans:
   - any system tagged `exiledSector_chart_system` with no live run gets the full close sequence;
   - any hyperspace entity tagged with a dead run id is removed;
   - any intel pointing at a missing system is ended.

   This catches crashes, mod updates, and saves made while a run was half-closed.
5. **Disabling the mod.** We document that runs should be closed before removing the mod. Because no persisted class of ours sits inside the system's graph, besides plugins on our own entities, a closed run leaves nothing to break.
6. **Verification.** There are two layers:
   - Unit tests over the run state machine and the factory's bookkeeping, with Mockito, as we do today.
   - A dev console command `ChartSoak N` that runs N charts back to back: open, enter, clear, exit, close. It reports the system count, hyperspace entity count, intel count and persistent-data size before and after. A PowerShell helper saves the game and greps `campaign.xml` for `exiledSector_chart_` ids and tags. The expected count is 0, and save size should return to its starting value within a few KB.

   This is the acceptance test for every phase that touches generation.

## Editor and data

Main's editor already has an Items page with Definitions, Modifiers, Implicits, Names, Drops and Crafting tabs. Adding `STAR_CHART` to `ItemType` flows into the editor vocabulary automatically, because it is generated from the enum.

New data files:
- `star_charts.csv`: definitions and unique charts;
- `star_chart_modifiers.csv`, loaded into the shared modifier records;
- `star_chart_implicits.csv` for the system-type implicits, which roll after the shared faction implicit (empty since 2026-10-19);
- `star_chart_tiers.csv`;
- `chart_poi.csv`;
- `chart_factions.csv`.

Currency costs go in the existing `item_crafting.csv`.

The editor work:
- a definitions model and serialiser for `star_charts.csv`;
- extending the Modifiers and Implicits tabs to the new files, including their tier columns. `modifiers.ts` currently defaults implicits to the hull item type and needs checking.
- new models for the tier, point-of-interest and faction tables;
- `dataFiles.ts` entries;
- round-trip coverage in both `EditorRoundTripTest` and vitest. Without it, editor saves would drop fields.

All visible text goes through the en and zh_CN catalogues.

## Compatibility

- Nexerelin: tags cover colonies, missions and invasions. Special Task Group patrol orders can still list the system. Accept that, since the order fails harmlessly, or filter it if Nex exposes a hook.
- RAT, Nightcross and the area gates: unaffected, though we could add "clear a tier N chart" as a gate condition later.
- GraphicsLib: optional visuals for the wormhole only, using the existing `CombatGlow` pattern.
- Second-in-Command and other fleet mods: chart fleets are ordinary fleets.
- Mods that loop over every star system see the system only while a run is open. The tags exclude it from the well-behaved ones.

## Build order

Each phase ends with tests passing and, from phase 1 onwards, the soak check.

1. **Skeleton.** A wormhole entity, an empty system (one star), enter and exit, the full close sequence, the load sweep, and the soak command plus save grep. This proves cleanup before any content exists.
2. **Generator layout.** Seeded vanilla stars, planets and terrain, the isolation tags, and blocking escape abilities.
3. **Content.** Loot points of interest with salvage seeds, guard and patrol fleets, the boss, objectives, the NPC-levelling hook, and the drop-listener hooks.
4. **Generalising the item layer (done 2026-10-18, except the workbench buttons).** These are refactors, with existing item behaviour unchanged and the released-item tests green:
   - the per-type effect resolver;
   - the value-scaling axis and the minimum-tier filter;
   - per-type affix layout constants, only if charts need more than 4 explicit modifiers;
   - the currency registry replacing `CraftingCurrency`, with the workbench recipe list built from it.
5. **The Star Chart item and storage.** `ItemType.STAR_CHART` and the `StarChart` subclass, the chart effect registry with its generation hooks, the `exiledSector_star_chart` special item and codec keys, the chart store with cargo absorb, and tooltips. Notes from phase 4:
   - the tier must survive every path that rebuilds an item through `Item.create`: mirroring, copying, transposing, `ItemData.rolled` and re-rolls, and `ItemStore.add`;
   - `Item.create` and `readResolve` need a `STAR_CHART` case;
   - register the chart resolver, limits and currencies before modifiers and definitions load and before saves load (`AffixLayout.register` is package-private, so that code lives in `exiledsector.items`);
   - charts need their own implicit table and corruption pool; `ItemImplicits` currently assumes hull items;
   - pass the tier to `ItemRollOdds` and the codex when a chart is shown, and cache `ScaleContext` if chart tooltips build it every frame.
6. **The Star Chart UI.** Tier columns or tabs, the selected-chart panel, the currency buttons, and the Open Wormhole button with the one-run lock and a dev switch in place of the colony building. Since 2026-10-19 it is the Star Charts mode of the Crafting window (the refit Crafting button): the chart picker beside the bench, the chart's details on the bench plate, and the Open Wormhole button under that plate. The Star Charts tab that sat in the refit Hull Items screen and the skill tree socketing panel is gone, along with its separate detail panel. The button is greyed out, with the reason in its tooltip, until the Gateway check below passes; the LunaLib option "Star Charts: open wormholes anywhere" (off by default) skips the Gateway part. Opening a wormhole closes the panel, the refit screen and any market dialog it sits in, so the player lands on the campaign map beside the wormhole. Opening is refused during a fleet encounter, because that dialog cannot be closed safely from outside.
7. **Crafting currencies and chart drops (done 2026-10-18).** The sustain loop and tier progression, described under "Chart currencies and drops" below.
8. **Editor and localisation (done 2026-10-18).** The Definitions, Modifiers, Implicits and Names tabs have an item-type switch between hull items and subroutines and star charts, covering `star_charts.csv` (system type, minimum tier, cleared-tier unlocks, tier-gated pool entries), `star_chart_modifiers.csv` (tier-band values), `star_chart_implicits.csv` and `star_chart_names.json`. The Crafting tab lists the three ciphers. New Chart tiers, Chart POIs, Chart factions, Chart drops and Chart blocked abilities tabs edit the system tables, refusing what the loaders skip and warning, before saving, about what they clamp or ignore, such as a cipher chance outside chart systems. Every chart file round-trips byte for byte in vitest, including quotes the files had, and `EditorRoundTripTest` pins every chart loader. Every chart key in `en.json` already had zh_CN text, apart from the `.one` plural forms, which Chinese never uses.
9. **The colony building (built 2026-10-18, awaiting in-game testing).** The Wormhole Gateway (`exiledSector_wormhole_gateway`, `exiledsector.charts.WormholeGateway`) enables the Open Wormhole button. Its source art stays at `graphics/unused/Wormhole_gateway.png`; the build menu image (`graphics/icons/industry/`, 190x95) and the research icon (`graphics/icons/tech/`, 320x320) are crops of it. Decided with the user on 2026-10-18:
   - The Wormhole Gateway is a structure, not an industry, because Ashes of the Domain makes industry slots scarce. One is allowed per faction, and it cannot be raided.
   - A wormhole can be opened only while docked at the planet that has the Gateway. Opening a wormhole from the Crafting window needs a working Gateway. The check reads the current interaction dialog: its target must be a player-owned market whose Gateway is functional, so one under construction or disrupted does not count.
   - It has a reasonable monthly upkeep. Whether opening a wormhole should also cost something is to be investigated later.
   - It has no AI core or administrator effects for now; those are a later addition.
   - Its `init` and `apply` must have no side effects and must work with a null market. Vaults of Knowledge builds industries on a fake market for tooltips, and Tides of Tomorrow calls `getNewPluginInstance(null)`.
   - Without Vaults of Knowledge, building it needs the Janus Device, checked exactly like the skill tree's wormhole nodes: a `memoryFlag` unlock condition on `$playerCanUseGates` through `SkillTypeUnlockStatus`, so the "disable memory flag unlocks" setting covers it too (decided 2026-10-19).
   - With Ashes of the Domain Vaults of Knowledge (`aotd_vok`) enabled, the Gateway is also a research node, and the research replaces the Janus Device requirement.
     - The node goes in `data/campaign/aotd_tech_options.csv`, in Vaults of Knowledge's own tab (`modId=aotd_vok`), at tier 5, column 1, row 6, directly below the Bifrost Gate.
     - Like the Bifrost, it requires `aotd_tech_hyperspace_stabilization`. It carries the `$aotd_experimental_tier` requirement like every tier 5 node, so it needs a Hyperdimensional Processor. That is intended, since Star Charts are an endgame mechanic.
     - It costs `research_databank:12`, takes 30 research time, and has an `:industry` reward naming the Gateway.
     - Its icon is registered under `graphics.ui_icons_tech_tree` in `settings.json`.
     - The research check runs at runtime, behind `isModEnabled("aotd_vok")`, through `exiledsector.compat.VaultsOfKnowledgeCompat` to `AoTDMainResearchManager.getInstance().isAvailableForThisMarket(id, market)`, which also covers NPC markets and returns true for a null market. A failed lookup falls back to "researched". There is no pom dependency.
     - The parser splits `otherReq` and reward text on colons, so neither may contain an extra colon. It also does not trim `reqToResearchFirst`, so it must contain no spaces.
   - Every system planet now carries the `not_random_mission_target` tag. Vaults of Secrets' Project Charon otherwise picks unsurveyed planets. Systems must never get ruins, remnant or derelict theme tags, or station-mode Remnant fleets, which Vaults of Secrets' Rogue AI looks for.
   - Farming QoL commission merit in systems is fine.
   - The dev setting stays, renamed as a dev switch and off by default: when on, it skips the Gateway and lets the button work anywhere outside a chart system.
   - No upgrade or downgrade chain for now (decided 2026-10-18). Better Market Overhaul would turn one into an upgrade path.
   - Clicking the Gateway in the colony screen offers a "Star Charts" option (`exiledsector.ui.chart.WormholeGatewayOption`, a vanilla `IndustryOptionProvider`), shown on the player's own colony even while the Gateway is under construction. It opens the Crafting window locked to its Star Charts mode: no mode switch and no hull items, only the chart picker, chart crafting and the Open Wormhole button, which still needs the docked Gateway check above (decided 2026-10-19).
10. **Polish.** Wormhole visuals and sounds, the fleet preview, and balance.

Atlas progression is deferred to a later project.

## Decisions (2026-10-18)

1. Opening is eventually enabled by a colony building. Charts are lootable, are scooped into a Star Chart storage, and are managed in a custom UI: tiers 1 to 15, a selected-chart panel, currency rolling, and a big Open Wormhole button.
2. The number of entries is a LunaLib setting, default 1 (changed from a fixed single entry on 2026-10-18). There is no time limit, and only one run can be open at a time.
3. Anything left inside is deleted when the run closes.
4. Atlas features come later.
5. When the wormhole closes, the system must vanish without a trace: nothing left in the save.
6. Re-evaluated after main's hull item refactor (2026-10-18). Charts become a third item type on the shared item and modifier system, rather than a parallel copy of the old socketables code.
7. Star charts use the same three rarities as hull items: Common, Rare and Unique (2026-10-18).
8. Surveying system planets grants vanilla survey data and XP, as part of a chart's reward (2026-10-18).

## Chart currencies and drops

Phase 7 built the sustain loop. This section records how it works.

Five kernels also work on charts: Augmentation, Recalibration, Transposition, Corruption and, since 2026-10-19, Attunement (the Fundamental Nanoswarm). Attunement treats the faction implicit as the implicit's value: it re-picks the faction by weight from `chart_factions.csv`, retrying until the faction changes, through the `implicitCanVary`/`rerolledImplicit` hooks that `StarChart` overrides. Their code is not changed for it. Instead, `ItemCurrencies` keeps an overlay of extra item types per registered currency (`extend`, `retract` and `appliesTo(currency, type)`), and `StarChartCurrency.registerAll()` extends those four kernels to `STAR_CHART` at application load. Crafting and the currency row both read the overlay, so hull items and subroutines see exactly the same currencies as before. The kernels' own code was already type-aware: augment uses the chart limits (six modifiers, three a side) and the tier filter, recalibrate rerolls roll positions, transpose only picks a unique that rolls at the chart's tier and keeps the tier, and corruption uses the chart implicit pool.

Corruption's "values stretched" outcome now reaches tier-band lines too. Their roll positions are rerolled from -35 to 135 instead of 0 to 100, and `ValueScaling.PerTierBand` reads a position outside 0 to 100 as a value up to 35 percent of the band's bound past that bound, never crossing zero. Positions inside 0 to 100 read exactly as before. Hull items have no tier, so their values never reach this code, and their own stretch is unchanged.

Three chart-only currencies are `StarChartCurrency` entries, each a special item that reuses a kernel icon tinted with its own colour:
- the Reforging Cipher rerolls every explicit modifier at the chart's tier, and the rarity follows the new count;
- the Annulment Cipher removes every explicit modifier, so the chart turns Common;
- the Cartographer's Cipher raises the tier by one, up to 15. Modifiers keep their roll positions and take the new tier's values, and a unique starts from its minimum tier.

Uniques refuse the reforge and the annulment, and corrupted or mirrored charts refuse everything.

The Prefix and Suffix Inhibitors work at the Star Chart bench too, on the two side consoles of the map device art. With one on, the Reforging Cipher keeps every modifier on the protected side and rerolls only the other side, the Annulment Cipher removes only the unprotected side, and Tuning skips the protected side as it does on hull items. An inhibitor is used up only when the craft would have changed a modifier it protected, and a craft that would leave nothing to change is refused. All three can be grown from Nanite Feedstock by right-clicking their tile in the Crafting window's Star Charts mode, at the costs in `item_crafting.csv`. Their `dropsFromShipLoot()` is false, so `ItemDrops.pickKernel` never picks them and ship kernel drops are unchanged.

Chart drops come from `chart_drops.csv`, one row per source, through `ChartDrops`:
- run_reward: completing an objective drops one chart and a 25 percent chance of a second, scaled by the chart's reward multiplier and capped at four. Each chart is 40 percent likely to be one tier higher. A chart-only currency comes with it 75 percent of the time.
- system_salvage: each salvaged point of interest inside a chart system has a 4 percent chance of a chart at the run tier, scaled by the chart's Star Chart bonus (which Quantity and Salvage also raise), and a small chance of a chart-only currency.
- chart_fleet and chart_warden: these replaced system_battle on 2026-10-19. Each defeated fleet of the live run rolls once in total, scaled by the share of its starting deployment points destroyed, and the warden adds a richer roll scaled by the Hoard. The Star Chart loot section of `docs/docs.md` has the details.
- battle: won battles outside chart systems that defeat at least 150 enemy deployment points have a 1 percent chance, at tier 1 plus one per 120 more points, up to tier 6.
- salvage, techmining_first_find and techmining_monthly: rare finds at a tier from the player's level, up to tier 5.

Chart-only currencies only drop from the system sources; the loader zeroes a currency chance on any other row. Dropped charts go into cargo, or colony storage for tech mining, and the chart store absorbs them from the player's cargo on load and when the Crafting window's chart picker opens.

A unique chart's unlock column takes an `ItemUnlock` id or `cleared_chart_tier_N`. Completing an objective on a tier N chart raises the sector memory value `$exiledSector_chartsClearedTier` to N if it was lower, so the unlock stays latched. The Warden's Vigil needs a cleared tier 3 chart. A unique also never drops below its minimum tier, and an unknown unlock id is logged at load and never drops.

Rare charts take codename words from `star_chart_names.json`, which the editor's Names tab edits.

None of this adds anything to a save for a closed run. The only new save value is that cleared-tier number.

Left for later phases: drop balance (phase 10).

## Enemy scaling by tier (planned, agreed 2026-10-19)

Today a chart fleet's ship trees are still sized from the player's fleet. The tier only sets a percentage of a random player ship's tree power (`fleet_strength_min/max`, 50-80% at tier 1 up to 170-240% at tier 15, read in `ChartTiers.fleetStrengthRangeFor` and used in `NpcFleetLeveller.decide`). In the regular sector that is right: enemies are always 80-120% of the player. Inside charts the scaling is inverted. The tier alone decides how strong the enemy is, and nothing inside a chart system reads the player's fleet.

### Tier-based trees

- Tree size is a share of a full tree (`ShipLevelConfig.maxAllocatedNodesBesidesRoot`), not of the player's ships. `fleet_strength_min/max` become `npc_tree_min/max`, a percent of a full tree, reaching 100% at tier 10.
- Everything else in a chart fleet that reads the player reads the tier instead. A new `npc_item_level` column replaces the player level in `NpcItems.rollCount` and `NpcItems.rollSectionItems`.
- The overflow strength bonus (`STRENGTH_BONUS_MEMORY_KEY`, from fleets cut by the fleet limit or sizes past the cap) adds to the tree percent up to a full tree. Anything left over goes to officer levels.

### Tiers 11 to 15: per-ship strength after full trees

Starsector's battle size caps what each side fields at once, so above some size extra fleet points become reinforcement waves rather than a stronger front line. Fleet points keep growing above tier 10, but per-ship strength carries the difficulty there, through new `star_chart_tiers.csv` columns:
- `average_smods` sets `FleetParamsV3.averageSMods`: 0 up to tier 10, rising to about 2 at tier 15.
- `min_ship_size` sets `FleetParamsV3.minShipSize`: 0 up to tier 10, then frigates and later destroyers are excluded, so the same fleet points sit in fewer, stronger hulls.
- `officer_level_limit` sets `FleetParamsV3.officerLevelLimit`, raised only at the top tiers, so elite officers appear.
- `npc_item_level` keeps rising, so ships carry more and better hull items and subroutines.
- Fleet quality keeps rising to 100 at tier 15, as it does now.

Tiers 1 to 10 feel like enemies catching up as their trees fill. Tiers 11 to 15 feel like elite fleets. All values are placeholders.

### Keeping tier 1 from feeling too easy

A player who reaches charting with large trees would find tier 1 enemies much weaker than the sector's. Three parts handle this, following how Path of Exile starts maps right where the campaign ends and lets strong players climb fast:

1. Parity tier. The parity tier is the lowest tier whose average enemy tree is at least as big as what the sector gives the player's enemies now (the player's tree power from `PlayerTreePower`, with the same level floor, times the sector's 80-120% midpoint). Charts found outside chart systems (the `battle`, `salvage` and `techmining_*` rows of `chart_drops.csv`) drop at the parity tier, with their existing tier-up chance, instead of the current deployment-point and player-level formulas. Trees are full at tier 10, so a fully levelled player's parity tier tops out at 10, and tiers 11 to 15 still have to be climbed.
2. The chart tooltip compares the chart with the sector: one line saying whether its enemies are weaker than, about equal to, or stronger than the sector's, from the chart's tier against the current parity tier.
3. Charts dropped inside a run (`run_reward`, and the fleet and site sources from `docs/plans/chart_loot.md`) never come below the parity tier. A late player who runs a low chart is never stuck grinding the bottom of the ladder.

The parity tier is worked out when a chart drops and is frozen into the chart's tier, so nothing new goes into the save. Stripping the fleet to lower it gains nothing: the level floor limits how low it goes, and lower charts pay less.

Low-tier charts stay low on purpose: they're easy, they pay at their own tier, and running one is the player's choice.

## Chart drawbacks (planned)

Today every chart drawback strengthens the enemy: fleet size, rolled and extra fleets, officer levels and count, warden size and fleet quality. The one exception is sensor range (Shrouded), which shrinks the player fleet's sensors through a temporary fleet stat modifier (`ChartRunEffects.applySensorRange`). That is too few drawbacks for six-modifier charts, and they all feel alike: more enemies. This section adds a second family that weakens the player's fleet while it is inside the system, in the spirit of Path of Exile's "Players have ..." map modifiers.

### Player fleet drawbacks

Combat, per ship:
- more shield damage taken, more armour damage taken, more hull damage taken, more EMP damage taken;
- less top speed, less acceleration and turn rate;
- less flux dissipation, less flux capacity;
- less weapon range, less rate of fire;
- shorter peak CR time, faster CR loss, lower maximum CR;
- longer overloads, longer ship system cooldowns;
- slower fighter replacement;
- less missile ammo.

Campaign, while in the system:
- lower maximum burn;
- larger sensor profile, so enemy fleets spot the player sooner;
- higher supply use;
- slower CR recovery and repair between fights;
- higher deployment cost;
- more crew losses.

Each one is a hybrid modifier: the drawback plus Quantity, fixed by how punishing it is (as in section 8 of `docs/plans/chart_loot.md`). These drawbacks are relative to the player's own stats, like Path of Exile's player modifiers. So, as in Path of Exile, their values grow over three tier bands (1-5, 6-10, 11-15), for example 5%, 7% and 10% for the damage-taken lines, while the Quantity they pay stays the same.

### How they apply

- Combat and per-ship stats go through `SkillTreeHullMod`, which `ShipTreeSync` puts on every player fleet member and keeps applying last. In `applyEffectsBeforeShipCreation` it reads a cached snapshot of the active chart drawbacks and applies them only to members of the player fleet. That also covers stats the engine fixes when a ship is built, such as armour, hull and missile ammo, and the refit screen shows the reduced values while the player is inside.
- `ChartRunScript` sets the snapshot when the player fleet enters a live chart system and clears it when the fleet leaves or the run closes. On each change it marks the player fleet's members for a stat update. This is event-driven with no per-frame work, and nothing is saved, so leaving the system or loading a save outside it always restores the fleet.
- Campaign stats use temporary fleet stat modifiers, the same way sensor range already does.
- The drawbacks are applied as multipliers under their own modifier id, so the tree's additive percent pools can't absorb them. Where it matters, the stacking is checked against the decompiled engine first.
- Allied fleets that join a battle are not affected. Only the player's own fleet is.
- When the player enters the system, a campaign message lists the active drawbacks. The tree hullmod's tooltip gets a "Chart conditions" block while they apply.

### Enemy combat buffs (later)

The mirror family, where chart enemies get more damage, armour, speed or faster flux, can go through the same hullmod on NPC ships that carry a tree, reading the fleet's chart tier key. Ships that rolled no tree would miss out, so this needs a decision on whether chart fleets always get trees before it is built.

### Pool structure

- A `group` column in `star_chart_modifiers.csv`, checked by `AffixLayout`, means at most one modifier per group can roll. Groups cover variants of the same stat, and pairs that together would be miserable rather than interesting.
- Player drawbacks are prefixes, like the enemy drawbacks (decided 2026-10-20, replacing the 2026-10-19 plan to make them suffixes). The first batch is in `docs/plans/chart_fleet_drawbacks.md`.
- Some drawbacks sit behind a minimum tier, as in Path of Exile, where the nastier modifiers only enter at higher bands.
