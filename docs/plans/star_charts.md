# Star Charts: plan for procedurally generated pocket systems

This plan is based on three read-only investigations: Beyond the Sector (0.95), Exiled Space (0.96), and the 0.98a engine (decompiled) together with our own socketables code. File and line references for every claim are in those reports. This document keeps only the conclusions.

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

A chart is a special item whose state lives entirely in its JSON data string, like a socketable, so it stacks, trades and survives the mod being updated.

Each chart has:
- a definition id;
- a seed;
- a tier;
- a rarity: normal, magic, rare or unique;
- rolled modifiers (prefixes and suffixes);
- quality;
- a corrupted flag;
- a frozen name.

The seed plus the definition reproduce the same system every time, so a chart is a recipe, not a saved system. That makes it trivial to preview, to tooltip and to regenerate after a crash.

Tiers run from 1 to 15. Tier sets:
- the fleet power budget;
- officer levels, quality, average S-mods and NPC skill-tree size;
- the loot budget;
- which modifiers can roll, through a minimum-tier filter;
- the chance that the run drops a higher-tier chart.

Higher tiers add bosses and remove the easy system types.

Modifiers follow Path of Exile.

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
- socketable drop chance;
- blueprint chance;
- an extra boss;
- longer wormhole lifetime.

Each modifier is a row in `star_chart_modifiers.csv`: id, side, family, tier range, weight, value ranges per tier, quantity and rarity contribution, and a plugin class. Plugin classes implement `beforeGeneration(ChartGenContext)` and `afterGeneration(ChartRun)`.

Crafting reuses the socketables framework once it's generalised:
- an `AffixEffect` catalogue interface;
- tier-scaled pool values;
- a per-kind rarity and layout config;
- an item-kind codec;
- a currency interface in place of the closed `SocketCurrency` enum.

The new currencies cover:
- transmute;
- alteration;
- augment;
- regal;
- alchemy;
- chaos;
- scour;
- exalt;
- divine (rerolls values; this is our existing recalibrate);
- a corruption step that can't be undone;
- a tier-up "cartographer's" step.

Existing socketable behaviour must not change. The released-socketable tests already guard it.

Charts drop:
- from chart runs: the sustain loop, weighted towards the same tier or one above;
- rarely from high-strength battles and salvage, through the same three listener hooks socketables already use;
- from a unique-chart unlock rule, like `SocketableUnlock`.

## Storage, the Star Chart UI, and opening a wormhole

Decided (2026-10-18):
- Charts are lootable items. Like socketables and hull items, they are scooped from cargo into a persistent Star Chart storage (`StarChartStore` in sector persistent data). It follows the `SocketableStore` pattern: an absorb pass over player cargo when the UI opens or on load, and the same codec.
- A custom Star Chart UI, built like the hull items UI:
  - charts are organised by tier, 1 to 15;
  - selecting a chart opens a detail panel like the modify-hull-item UI;
  - currency is spent on the selected chart to roll or reroll its modifiers;
  - a big "Open Wormhole" button.
- Eventually a colony building enables wormhole opening. Until it exists, a dev or settings switch allows opening anywhere, so the pocket can be built and tested first. The building can then gate where and when the button works, and whether it works at all.

Opening consumes the chart and creates a Wormhole entity beside the player fleet, in hyperspace or a normal system. It is a custom entity with our own plugin and visual (the skill tree's wormhole band renderer is a good look to reuse).

Run rules (decided):
- **One entry.** The player can enter the wormhole once. Entering collapses the outer wormhole behind them. Inside, an exit wormhole leads back to the origin location. Using it ends the run.
- **No time limit.** The pocket exists for as long as the player stays inside.
- **One open run at a time.** "Open Wormhole" is disabled while a run is open.
- Before entering, the player can collapse the outer wormhole to abandon the run. The chart was already consumed when it opened.
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
- Each entity gets a salvage seed derived from the chart seed, and a salvage-spec override, so socketable and chart drop rules apply.
- Fleets come from `FleetFactoryV3`:
  - added directly to the system, with no RouteManager;
  - each guards a point of interest (aggressive orbit) or patrols the system;
  - `NO_JUMP`, never auto-despawn, no going dark, permanently hostile to the player;
  - a boss fleet at higher tiers;
  - every fleet is recorded in the run.
- Every fleet gets `$exiledSector_chartTier` before levelling, so `NpcFleetLeveller` scales NPC trees and socketables by tier instead of by player power.
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
   - `OPEN`: the outer wormhole exists and the player hasn't entered.
   - `INSIDE`: the outer wormhole has collapsed.
   - `CLOSING`: entered when the player leaves through the exit wormhole, is respawned outside, escapes, or abandons the run before entering.
   - `CLOSED`.

   Only one run exists at a time.
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

New data files:
- `star_charts.csv`: definitions and unique charts;
- `star_chart_modifiers.csv`;
- `star_chart_tiers.csv`;
- `chart_poi.csv`;
- `chart_factions.csv`;
- the currency costs.

Each needs a loader, an editor page or tab, serialisers and round-trip coverage in both `EditorRoundTripTest` and vitest. Without those, editor saves would drop fields. All visible text goes through the en and zh_CN catalogues.

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
4. **The Star Chart item and storage.** Data model, codec, tiers 1 to 15, modifier rolling with plugins, `StarChartStore` with cargo absorb, and the tooltip. This includes generalising the socketable rolling code, with its tests unchanged.
5. **The Star Chart UI.** Tier columns or tabs, the selected-chart panel, the currency buttons, and the Open Wormhole button with the one-run lock and a dev switch in place of the colony building.
6. **Crafting currencies and chart drops.** The sustain loop and tier progression.
7. **Editor and localisation.** Editor pages, round-trip tests, and the zh_CN text.
8. **The colony building.** It enables the Open Wormhole button and replaces the dev switch.
9. **Polish.** Wormhole visuals and sounds, the fleet preview, and balance.

Atlas progression is deferred to a later project.

## Decisions (2026-10-18)

1. Opening is eventually enabled by a colony building. Charts are lootable, are scooped into a Star Chart storage, and are managed in a custom UI: tiers 1 to 15, a selected-chart panel, currency rolling, and a big Open Wormhole button.
2. One entry, no time limit, one open run at a time.
3. Anything left inside is deleted when the run closes.
4. Atlas features come later.
5. When the wormhole closes, the system must vanish without a trace: nothing left in the save.
