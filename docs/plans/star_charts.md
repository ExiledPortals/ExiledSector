# Star Charts: plan for procedurally generated pocket systems

This plan is based on three read-only investigations: Beyond the Sector (0.95), Exiled Space (0.96), and the 0.98a engine (decompiled) together with our own item code (re-checked after main's hull item refactor). File and line references for every claim are in those reports. This document keeps only the conclusions.

## What the two mods teach us

Exiled Space is PureTilt's "PoE maps in Starsector". Its ideas are the right ones; its build quality isn't.

Worth keeping:
- Chips carry rolled modifiers loaded from CSV. Each modifier is a plugin with before-generation hooks (which change the fleet parameters) and after-generation hooks (which change the finished system).
- A modifier's cost feeds both its drop weight and the reward.
- A loot budget is spent on points of interest, weighted towards ones whose cost is close to the average.
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
1. Our pocket has no hyperspace presence at all.
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
- the fleet power budget;
- officer levels, quality, average S-mods and NPC skill-tree size;
- the loot budget;
- which modifiers can roll, through a minimum-tier filter;
- the value band each modifier rolls in;
- the chance that the run drops a higher-tier chart.

Higher tiers add bosses and remove the easy system types.

Modifiers follow Path of Exile, using main's modifier layer of prefixes, suffixes and implicits.

Prefixes are difficulty. They raise reward quantity and rarity, as PoE's item quantity does:
- stronger fleets;
- extra fleets;
- officers with elite skills;
- S-mods;
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
| Effects not tied to ship skills | Partly done. Modifier records hold plain strings, but loading, validation and descriptions still go through `SkillEffect`. | An effect-resolver interface chosen per item type. `SkillEffect` becomes the first implementation; a chart effect registry is the second. A chart effect describes itself and supplies `beforeGeneration(ChartGenContext)` and `afterGeneration(ChartRun)` hooks. Every place that currently calls `SkillEffect.byName` switches to the resolver. |
| Values that scale with tier | Not done. Lines scale by hull size only (four fixed values). | A value-scaling axis on `ModifierLine`: hull size for ship items, tier band for charts. Also a minimum-tier filter on pool entries, used by the roller. |
| PoE rarities | Not done. `ItemRarity` is COMMON, RARE and UNIQUE, inferred from the modifier count, and the layout constants are fixed. | A rarity and layout policy per item type. Charts get Normal (0 modifiers), Magic (1-2), Rare (3-6) and Unique, with rarity set by currency, as in PoE. Hull items keep their current policy. Naming needs a Magic branch, which can reuse the Common path. |
| A chart codec | Partly done. The JSON codec and its modifier and name encoding are reusable, but there is one special-item id and no tier, quality or corrupted keys. | A separate special item `exiledSector_star_chart` with its own plugin and icon. Its data string reuses `ItemCodec`'s JSON, with optional `t`, `q` and `c` keys. `HullUpgradeData` is the precedent for a second id absorbed into storage. |
| Open-ended currencies | Not done. `CraftingCurrency` is still a closed enum, hard-wired in five places, including the workbench recipe list. | A currency interface or data-driven registry keyed by commodity id, with `appliesTo(ItemType)`, `canUse(item)`, `use(item, random)`, a parts cost and a drop weight. The workbench builds its buttons from the currencies for the loaded item's type. `CraftingMaterials` and `item_crafting.csv` already work per commodity id, so they carry over unchanged. |

The base `Item` class carries ship-only methods (`skillEffects`, `canSocketInto`, `effectLines(hullSize)`). They are harmless for charts. A later refactor could move them into a ship-item intermediate class, but charts don't need it.

Existing hull item and subroutine behaviour must not change. `ReleasedItemsTest`, `ItemCodecTest`, `ItemSaveTest` and `ItemDataIntegrityTest` already guard it.

The chart currencies, PoE-style:
- transmute (normal to magic);
- alteration (reroll a magic chart);
- augment (add a modifier to a magic chart; main's Augmentation);
- regal (magic to rare);
- alchemy (normal to rare);
- chaos (reroll a rare chart);
- scour (back to normal);
- exalt (add a modifier to a rare chart);
- divine (reroll values; main's Recalibration);
- a corruption step that can't be undone;
- a tier-up "cartographer's" step.

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
- Eventually a colony building enables wormhole opening. Until it exists, a dev or settings switch allows opening anywhere, so the pocket can be built and tested first. The building can then gate where and when the button works, and whether it works at all.

Opening consumes the chart and creates a Wormhole entity beside the player fleet, in hyperspace or a normal system. It is a custom entity with our own plugin and visual (the skill tree's wormhole band renderer is a good look to reuse).

Run rules (decided):
- **Entries are a LunaLib setting.** "Star Chart wormhole entries" defaults to 1.
  - Each pass through the outer wormhole uses one entry.
  - Inside, an exit wormhole leads back to the outer wormhole. Leaving keeps the run open while entries remain.
  - When the last entry is used, the outer wormhole collapses behind the player. Leaving through the exit wormhole then ends the run.
  - With the default of 1, this means entering collapses the outer wormhole, and leaving ends the run.
  - The setting is read when a wormhole opens and stored on the run, so changing it mid-run doesn't affect that run.
- **No time limit.** The pocket exists for as long as the run is open.
- **One open run at a time.** "Open Wormhole" is disabled while a run is open.
- **Collapsing.** While outside, the player can collapse the outer wormhole at any time to end the run. The chart was already consumed when it opened.
- **Anything left inside is deleted when the run closes,** including jettisoned cargo, unrecovered ships, unlooted caches and fleets.

The run also ends if:
- the player's whole fleet is destroyed inside. Normal vanilla respawn moves them out; we detect the location change and close the run.
- the player escapes some other way, such as another mod's ability we didn't block. We detect the location change and close the run.

## The generator

`ChartGenerator.generate(chart)` is pure, deterministic from the seed, and runs in two stages.

Stage 1 is layout:
- A fresh `StarSystemAPI` is created with base name "Charted Space", a unique optional id `exiledSector_chart_<runId>`, and our tag `exiledSector_chart_pocket`.
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
  - a guarded "vault" at higher tiers.
- Each entity gets a salvage seed derived from the chart seed, and a salvage-spec override, so hull item and chart drop rules apply.
- Fleets come from `FleetFactoryV3`:
  - added directly to the system, with no RouteManager;
  - each guards a point of interest (aggressive orbit) or patrols the system;
  - `NO_JUMP`, never auto-despawn, no going dark, permanently hostile to the player;
  - a boss fleet at higher tiers;
  - every fleet is recorded in the run.
- Every fleet gets `$exiledSector_chartTier` before levelling, so `NpcFleetLeveller` scales NPC trees and carried hull items by tier instead of by player power.
- Factions are picked from `chart_factions.csv` with weights, like Exiled Space, but fleets stay on their real faction and hostility is enforced through memory flags. Exiled Space's "unknown faction" trick isn't needed.
- Objectives can be "clear the guardians", "kill the boss" or "loot the vault". Finishing one gives a completion reward and a chance at a chart drop, and the objective's intel ends with the run.

## Isolation

Rules for the pocket:
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
   - `INSIDE`: the player is in the pocket. The outer wormhole remains while entries are left; it collapses on the last entry.
   - `CLOSING`: entered when any of these happens:
     - the player leaves with no entries left;
     - the player collapses the outer wormhole while outside;
     - the player is respawned outside;
     - the player escapes some other way.
   - `CLOSED`.

   Only one run exists at a time. The run stores its entry allowance and the entries used.
3. **Close order.** Closing runs from a script on the next frame, once the player is out and not mid-transition:
   1. Clear the player's interaction target and plotted course.
   2. Reset the campaign map's last-viewed location if it points at the pocket. This needs a MethodHandle, because there's no API setter.
   3. Re-add `StrandedGiveTJScript`, which drops vanilla's stale reference.
   4. Despawn every fleet in the pocket with `despawn(...)`, so listeners fire.
   5. End every intel item the run created, and any intel whose map location is in the pocket.
   6. Remove our scripts and listeners for the run, and clear our memory keys.
   7. Remove every entity in the pocket while its listener is still attached, which clears the engine's id map.
   8. Remove both wormhole entities and any hyperspace entity that points into the pocket. There should be none; this is a safety net.
   9. Call `removeStarSystemNextFrame`.
   10. Delete the run record.

   Every step is idempotent and wrapped, so a failure in one step can't stop the rest.
4. **A sweep on load.** Every `onGameLoad` runs a purge of orphans:
   - any system tagged `exiledSector_chart_pocket` with no live run gets the full close sequence;
   - any hyperspace entity tagged with a dead run id is removed;
   - any intel pointing at a missing pocket is ended.

   This catches crashes, mod updates, and saves made while a run was half-closed.
5. **Disabling the mod.** We document that runs should be closed before removing the mod. Because no persisted class of ours sits inside the pocket's graph, besides plugins on our own entities, a closed run leaves nothing to break.
6. **Verification.** There are two layers:
   - Unit tests over the run state machine and the factory's bookkeeping, with Mockito, as we do today.
   - A dev console command `ChartSoak N` that runs N charts back to back: open, enter, clear, exit, close. It reports the system count, hyperspace entity count, intel count and persistent-data size before and after. A PowerShell helper saves the game and greps `campaign.xml` for `exiledSector_chart_` ids and tags. The expected count is 0, and save size should return to its starting value within a few KB.

   This is the acceptance test for every phase that touches generation.

## Editor and data

Main's editor already has an Items page with Definitions, Modifiers, Implicits, Names, Drops and Crafting tabs. Adding `STAR_CHART` to `ItemType` flows into the editor vocabulary automatically, because it is generated from the enum.

New data files:
- `star_charts.csv`: definitions and unique charts;
- `star_chart_modifiers.csv`, loaded into the shared modifier records;
- `star_chart_implicits.csv` for the system-type implicits;
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

- Nexerelin: tags cover colonies, missions and invasions. Special Task Group patrol orders can still list the pocket. Accept that, since the order fails harmlessly, or filter it if Nex exposes a hook.
- RAT, Nightcross and the area gates: unaffected, though we could add "clear a tier N chart" as a gate condition later.
- GraphicsLib: optional visuals for the wormhole only, using the existing `CombatGlow` pattern.
- Second-in-Command and other fleet mods: chart fleets are ordinary fleets.
- Mods that loop over every star system see the pocket only while a run is open. The tags exclude it from the well-behaved ones.

## Build order

Each phase ends with tests passing and, from phase 1 onwards, the soak check.

1. **Skeleton.** A wormhole entity, an empty pocket (one star), enter and exit, the full close sequence, the load sweep, and the soak command plus save grep. This proves cleanup before any content exists.
2. **Generator layout.** Seeded vanilla stars, planets and terrain, the isolation tags, and blocking escape abilities.
3. **Content.** Loot points of interest with salvage seeds, guard and patrol fleets, the boss, objectives, the NPC-levelling hook, and the drop-listener hooks.
4. **Generalising the item layer.** These are refactors, with existing item behaviour unchanged and the released-item tests green:
   - the per-type effect resolver;
   - the value-scaling axis and the minimum-tier filter;
   - the per-type rarity and layout policy;
   - the currency registry replacing `CraftingCurrency`, with the workbench recipe list built from it.
5. **The Star Chart item and storage.** `ItemType.STAR_CHART` and the `StarChart` subclass, the chart effect registry with its generation hooks, the `exiledSector_star_chart` special item and codec keys, the chart store with cargo absorb, and tooltips.
6. **The Star Chart UI.** Tier columns or tabs, the selected-chart panel, the currency buttons, and the Open Wormhole button with the one-run lock and a dev switch in place of the colony building.
7. **Crafting currencies and chart drops.** The sustain loop and tier progression.
8. **Editor and localisation.** Editor pages, round-trip tests, and the zh_CN text.
9. **The colony building.** It enables the Open Wormhole button and replaces the dev switch.
10. **Polish.** Wormhole visuals and sounds, the fleet preview, and balance.

Atlas progression is deferred to a later project.

## Decisions (2026-10-18)

1. Opening is eventually enabled by a colony building. Charts are lootable, are scooped into a Star Chart storage, and are managed in a custom UI: tiers 1 to 15, a selected-chart panel, currency rolling, and a big Open Wormhole button.
2. The number of entries is a LunaLib setting, default 1 (changed from a fixed single entry on 2026-10-18). There is no time limit, and only one run can be open at a time.
3. Anything left inside is deleted when the run closes.
4. Atlas features come later.
5. When the wormhole closes, the system must vanish without a trace: nothing left in the save.
6. Re-evaluated after main's hull item refactor (2026-10-18). Charts become a third item type on the shared item and modifier system, rather than a parallel copy of the old socketables code.
