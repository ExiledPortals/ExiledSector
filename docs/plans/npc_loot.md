# NPC loot: plan

Today our items reach the player from NPC fleets mostly by being fitted on the fleet flagship and dropping when it dies. That gives very little. The basic subroutine pool is empty, so 98% of NPC socket rolls give nothing. Unique hull items can never be fitted on an NPC. Only flagships carry anything. This plan separates the two jobs. NPC ships only carry what their tree needs to work: the wormhole anchor for their faction volume, and nothing else. Loot becomes its own roll per won battle, sized by the power of the defeated fleet and the difficulty of the fight. That roll becomes a new source in the Star Chart loot framework (`docs/plans/chart_loot.md`), so chart bonuses scale it the same way they scale every other source.

All numbers below are placeholders, like every other magnitude in the mod. `docs/analysis/balance.csv` is left alone.

Status: built, on `parallel-work`. Every open question is decided and built. Also added on 2026-10-20: the Hyper-Nav Beacon is always for sale at Exo-Tech's Daybreak after a restock, and every open, military and black market sells one-modifier, non-unique hull items (`market_hull_items.csv`).

## Today

For reference, this is what the plan replaces.

- `NpcFleetLeveller.decide` gives every officered ship and the flagship a tree, and 30% of other ships. Only the flagship of a non-player-faction fleet rolls items, from its own seed stream (`|socketables`).
- Subroutines: `NpcItems.rollCount` gives 0 to 2 items (first roll 5% + 1% per player level, second roll 5% from level 15). The builder claims that many sockets, pathing to new ones if needed. Each socket then gets a unique subroutine 2% of the time (filtered by `faction:` sources), and otherwise `ItemDrops.pickBasic`, which returns null.
- Hull items: from level 15, a 50% chance of 2 to 4 unlocked sections. Each section holds a basic hull item at the first-roll chance. A ship with sections has a 10% chance to drop a Hull Upgrade when it dies.
- Faction volumes: the builder treats one socket as holding an implicit anchor for the fleet's volume (`ShipSkillData.implicitAnchorExits`, tag field `anchors:`). No item exists. The anchor is dropped when the player recovers the ship.
- Battle drops: `ItemLootListener` adds the dead ships' carried items, the Hull Upgrade drops, battle Feedstock (`DP × rate × difficulty multiplier`) and `FactionFleetDrops`. That last one is the wormhole anchors and the Contraband Modspec Cache, each at a chance per 100 DP defeated, capped at 50% per battle. Anchors use 10% per 100 DP.
- Chart fleets also roll the `exiledSector_chart_fleet` site rule through `ChartFleetLoot`: hull item cascade, targeted section rolls, Hull Upgrade, unique, materials, charts and ciphers, all scaled by the run's `ItemChances`.

## Part 1. Subroutines

### 1.1 No item rolls on NPC ships

NPC ships stop rolling subroutines entirely. What goes:
- In `NpcFleetLeveller`: the `itemRandom` stream, the `NpcItems.rollCount` call, and the socket-filling loop after `generate`.
- In `NpcItems`: `firstChance`, `secondChance`, `rollCount`, `pickDefinition`, `BASE_CHANCE`, `CHANCE_PER_PLAYER_LEVEL`, `SECOND_ROLL_PLAYER_LEVEL`, `SECOND_CHANCE` and `UNIQUE_SHARE`.
- In the builder: `NpcBuildRequest.itemCount`, `Generation.claimSockets`, `reachSocket` (unless the anchor still needs it, see 1.2), `NpcTreeBuild.claimedSockets` and the `HULL_ITEM_DISCARDED` step.
- `NpcUniqueAlerts` and its calls in `NpcFleetLeveller` and `NpcFleetSweepScript`, plus its strings. No NPC can carry a unique any more, so it has nothing to report.

The Contraband Modspec Cache stays a battle drop through `FactionFleetDrops`. It was never fitted on ships.

### 1.2 Real wormhole anchors

Every NPC ship whose tree reaches a node in its faction volume gets that faction's real wormhole anchor socketed. It is an NPC item (`NpcItems.id(ItemData.rolled(anchorDefinition, 0))`) in the socket the implicit anchor uses today. The tree view, the inspector and the socket wormhole art then show it like a player's anchor. Anchors have no modifiers, so a fixed seed of 0 keeps the tag short and the item identical on every ship.

Changes:
- `NpcSkillTreeBuilder` keeps its planning exactly as it is: `canTakeImplicitAnchor`, `jumpTarget` and the search treat one unclaimed socket as able to open the faction exit. Only the commit changes. Where `anchorAllocatedSocket` and `openImplicitAnchor` call `setImplicitAnchor`, they socket the anchor item instead. `undoPath` unsockets it. The anchor definition for the volume comes from `WormholeAnchorDefinitions` (one anchor per volume is already enforced there).
- A ship only keeps the anchor if it actually allocated a notable or keystone in the volume. Today `anchorAllocatedSocket` anchors an empty allocated socket before `tasteFactionVolume` knows whether a goal is affordable, so a ship can end up with the exit open and nothing behind it. After `tasteFactionVolume`, if no volume node was allocated, the anchor is unsocketed and the exit released. That matches "only ships with enough points to reach their faction node".
- `WormholeAnchorLinks.anchor` returns null for NPC item ids today (`WormholeAnchorLinks.java:37`), so an NPC anchor would never link. It needs to resolve NPC anchors through `NpcItems.resolve`. The NPC side already checks locked exit types through `lockedWormhole`.
- `ShipSkillData.implicitAnchorExits`, `setImplicitAnchor`, `dropImplicitAnchors` and the `canOpen` shortcut for them are removed. Starsector saves ignore unknown fields, so removing the persisted field is safe. Player ships never kept one anyway, because adoption drops it.

Old tags in saves: fleets that were already levelled keep their tag in fleet memory (`NpcTreeRecords`). `NpcTreeTag.decode` turns an `anchors:` field into a socketed NPC anchor, and drops every socketed NPC item that isn't an anchor. 1.3 and 2.1 rely on that.

### 1.3 What happens to an NPC anchor

When the ship dies, the anchor never drops. Anchors only come from the per-fight roll (1.4) and from markets (1.5).

When the player recovers the ship, `ShipTreeSync.adoptNpcTree` removes the anchor before `NpcItems.claimForPlayer` and releases the volume nodes, the same as today with the implicit anchor (open question 1). Otherwise every recovered faction ship would hand out a free anchor, and the market price would mean nothing.

`ItemLootListener.hold` stops reading carried items at all (`NpcItems.carriedBy` and `subroutinesCarriedBy` go), because no NPC carries anything that drops.

### 1.4 A 50% anchor chance per fight

Every won battle where the player destroyed or disabled at least one ship of a faction gets one roll for that faction's anchor, at 50%. Each faction in the battle rolls separately. A fleet that runs before losing a ship gives nothing, so engaging and disengaging can't be farmed.

Changes:
- `FactionFleetDrops.Source` gets a per-fight mode next to the per-100-DP one. In per-fight mode the chance is `drop_chance` whenever any qualifying DP was defeated, still capped by `MAX_CHANCE_PER_BATTLE`. The Modspec Cache keeps the per-100-DP mode.
- `wormhole_anchors.csv`: `drop_chance` means per fight for anchors. It becomes 0.5 on every row that should drop. `FactionFleetDrops.available` still hides anchors whose area is off.
- Chart fleets count like any other fleet of their faction. Chart bonuses don't scale anchors, because a per-fight chance with bonuses on top would hit 100% quickly.

### 1.5 Anchors in faction markets

Each market owned by a faction that maps to an anchor's volume (through `npc_faction_volumes.csv` plus the row's `drop_factions`) has a small chance to stock one anchor whenever its stock refreshes. The price is 100,000 credits.

Changes:
- New `market_chance` column in `wormhole_anchors.csv`: the chance per submarket refresh, placeholder 0.1. Leaving it blank means never sold. The Exo-Tech beacon, Remnant and the Abyss stay blank (Remnant and Omega have no markets anyway).
- A `SubmarketUpdateListener` adds the anchor to the open market's cargo after vanilla refreshes it (open question 2 covers the military market). The listener interface is in `starfarer.api.jar`. Decompile it to confirm the method signature and when it fires before building.
- New `price` column in `wormhole_anchors.csv`, default 100000. `ItemPlugin.getPrice` returns it for anchors. Every socketable shares the `exiledSector_socketable` special item at a base price of 15000, so this has to be per item, in the plugin. See open question 3 about the sell price.
- The editor's anchor model (`tools/editor/src/pages/items/model/anchors.ts`) and its round-trip test learn both new columns, or saving from the editor deletes them.

## Part 2. Hull items

### 2.1 NPC ships get no sections

No NPC ship unlocks a section or holds a hull item. Hull items are player only. NPC power is matched to the player elsewhere.

What goes:
- `NpcItems.rollSectionItems`, `carriesSectionItem`, `rollUpgradeDrop`, `SECTION_ITEM_CHANCE` and `UPGRADE_DROP_CHANCE`, plus the call in `NpcFleetLeveller.decide`.
- The section half of `NpcItems.claimForPlayer`.
- `NpcTreeTag` stops writing `frameworkTypes:` and `frameworkSockets:`. On decode it skips them, so old tags lose their sections.
- The Hull Upgrade drop per ship in `ItemLootListener.hold`. It moves into the battle drop table (2.2).

### 2.2 Battle drops: one source sized by fleet power and difficulty

Every won battle rolls hull items, Hull Upgrades and unique hull items from one table. It works the way battle Feedstock and XP already do: defeated size × rate × difficulty multiplier.

Gates: the roll needs hull upgrades enabled (`HullUpgradesSetting`) and player level 15 or higher (`ItemUnlock.SECTION_ITEM_MIN_PLAYER_LEVEL`). Each unique also needs `ItemUnlock.canDrop`: level 15 and its own unlock.

Fleet power: each destroyed or disabled enemy ship counts as

`ship power = DP × (1 + allocated nodes / NODES_PER_DOUBLING)`

The allocated node count comes from the NPC tree tag that `hold` already decodes. A ship with no tree counts 0 nodes. `NODES_PER_DOUBLING` is a placeholder of 30: a ship with 30 nodes counts double, and a full 60-node tree counts triple. NPC tree size already follows player power (`NpcTreeSize`, 80% to 120% of the player's ships), so loot grows as the player and the trees they face grow. Recovered ships still count, because nothing is carried any more.

Difficulty: each engagement's power is multiplied by `ShipLevelSystem.difficultyMultiplier(engagement.difficulty(), xpDifficultyStrength, xpDifficultyMaxMultiplier)`. That is the same multiplier as the XP difficulty bonus and battle Feedstock, so the "difficulty bonus" the player sees in the XP report also describes their loot. Several engagements in one battle add up, the same way `partsEarned` already does.

The table, in a new `data/config/exiledSector/battle_item_drops.csv` (`drop,per_100_power`):

| Drop | Placeholder per 100 power | Rarity |
|---|---|---|
| `mods_1` | 0.40 | common |
| `mods_2` | 0.25 | common |
| `mods_3` | 0.15 | rare |
| `mods_4` | 0.04 | rare |
| `unique` | 0.015 | unique |
| `hull_upgrade` | 0.10 | |

For each row: `expected = Σ (ship power × difficulty multiplier) / 100 × rate × source multiplier` (section 2.3). The count is `ItemDrops.wholeParts(expected)`: the whole part, plus one more at the leftover chance.

How the drops are rolled:
- `mods_N` picks a basic hull item through `ItemDrops.pickSectionItemBasic` (random section kind, then weighted by rarity). Only definitions whose prefix and suffix pools can hold N modifiers count. It is rolled with exactly N modifiers through a new `ItemRoller.rollWithEffectCount`, plus `ItemData.rolledWithEffectCount`. The rest of `rollAffixes` is unchanged. Rolled modifiers are stored in `ItemData`, so the forced count survives saving. Check that nothing re-rolls hull items from their seed alone before relying on that.
- `unique` picks through `ItemDrops.pickUniqueFrom(FACTION, factionId, …)`, filtered to `ItemType.HULL_ITEM` and `ItemUnlock.canDrop`. The faction is the one that lost the most DP in the battle. No unique hull item has sources today, so every faction can drop every one. A `source` column for hull items can make them faction flavoured later.
- `hull_upgrade` rolls a uniformly random hull size through `HullUpgradeRoller.rollHullSize`, the same as salvage and tech mining.

Worked examples (difficulty multiplier as vanilla computes it):

| Fight | Defeated DP | Average nodes | Power | Difficulty | Hull items | Uniques | Upgrades |
|---|---|---|---|---|---|---|---|
| Patrol, about half the ships with trees | 120 | 20 on half | 160 | 1.0 | 1.3 | 0.02 | 0.16 |
| Large faction fleet | 300 | 30 | 600 | 1.5 | 7.6 | 0.14 | 0.9 |
| Doom stack, outmatched | 400 | 35 | 867 | 2.0 | 14.6 | 0.26 | 1.7 |

Code: a pure, testable `items/BattleItemDrops` (table, power, expected counts, rolls) that takes the gathered power and the source multipliers. `ItemLootListener.hold` gathers power per source (2.3), next to `partsEarned`. `reportEncounterLootGenerated` and `reportBattleFinished` add the rolled drops where they add the battle drops now. A battle has no drop cap, so `announceSalvaged` would print a line per drop, so battle drops get one grouped message per rarity instead ("Salvaged 4 common, 2 rare hull items").

Settings: one LunaLib setting `exiledSector_battleItemDropPercent` (default 100) multiplies the whole table, next to the existing battle Feedstock setting. Its name and tooltip go in `en.json` and `zh_CN.json`.

### 2.3 How it fits the Star Chart loot framework

`chart_loot.md` sorts loot into sources (site salvage, chart fleets, warden spoils, completion reward) and drop types (the nine hull sections, subroutines, star charts, currency and so on). Its rule is that a drop bonus raises its drop type at every source. Battle drops become one more source, "fleet battles", that applies everywhere. Outside a chart system its multipliers are 1, so the table above is the whole story. Inside one, the run's bonuses scale it exactly as they scale site salvage.

The multipliers come in as `ItemChances`, the same record the chart sources already use:
- `ItemChances.general` (Quantity + Item %, from `ChartGenContext`) multiplies every row of the table.
- `ItemChances.targeted(kind)` (the per-section bonuses such as "+40% Reactor items") adds targeted drops for that section: `expected hull items / section kinds with basics × bonus`. Those use the same `mods_N` split. This is the same formula as today's `ItemDrops.rollTargeted`, so "+40% Reactor items" still means 40% more Reactor items, without thinning out anything else.
- Power is gathered per source fleet, and each fleet's multipliers come from one resolver (see "Atlas readiness"). A chart fleet gets its run's context, scaled by nothing else, because power already measures how much of the fleet died. Every other fleet gets the global context, which is `ChartGenContext.none()` until an atlas tree exists. So a mixed battle, such as a chart fleet joined by a passing patrol, only scales the chart fleet's share by the chart's bonuses.

The chart difficulty knobs then feed loot without any new code. Fleet Size % adds DP, Fleet Strength % grows the NPC trees (`ChartTiers.fleetStrengthRangeFor`), and bigger fleets raise vanilla's difficulty rating. All three raise power or the difficulty multiplier, the way Path of Exile's pack size and monster level raise its loot. That is intended. The per-battle cap keeps it from running away, and a tier 15 warden fight is the case to tune the cap against.

What changes in the chart sources so nothing is counted twice:
- `ChartFleetLoot` stops rolling hull items, the Hull Upgrade and the unique for chart fleets. `exiledSector_chart_fleet` keeps its materials, star chart and cipher rows. It needs a way to roll only those parts (for example a `rollSite` flag that skips `rollSectionItemLoot` and the unique roll), because `ItemDrops.roll` bundles them today.
- Warden spoils are unchanged. `exiledSector_chart_warden` stays the jackpot on top, and the warden's own ships also count as battle power like any fleet.
- Site salvage and the completion reward are unchanged. Sites aren't fleets.
- `chart_loot.md` gets a note in "Chart fleets" that NPC-carried items are gone and fleet hull items now come from the battle source. Its line saying the `NpcItems` drops "stack on top" is out of date once this lands.

Optional, proposed: a `CHART_ITEM_RARITY_PERCENT` chart effect (a suffix, like Path of Exile's item rarity on maps) that multiplies only the `mods_3`, `mods_4` and `unique` rows. The per-count table makes it possible for battle drops. For site salvage it would need `ItemRoller` to shift its effect count chances (35/20/35/10 today) by the same bonus, so the effect means the same thing at every source. It isn't needed for the rest of the plan (open question 5).

## Atlas readiness

An atlas tree is planned for later: a player-wide passive tree, like Path of Exile's atlas tree, whose nodes raise the items found and change the events that happen in charts, and eventually in the overworld too. This plan doesn't build it, but it shouldn't make it harder.

Most of the work is already there. Every chart loot source reads its multipliers from one `ChartGenContext` (`dropMult(DropType)`, `itemChances()`). It gets that context from `ChartRunEffects.contextOf(run)` inside a chart and from `ChartGenContext.none()` outside one (`ChartDrops.liveContextAt`). The context is built from a list of applied effects (`ChartGenContext.of(List<AppliedEffect>)`), so an atlas tree is one more provider of `ChartEffect` lines in that list. Inside a chart the context would be the chart's modifiers plus the atlas effects. Outside a chart it would be the atlas effects alone, in place of `none()`. Atlas bonuses then stack with chart modifiers under the existing rule (same drop type adds before it multiplies), and every source that reads the context honours them.

Three things in this plan keep that path open:

1. One resolver for every source. Battle drops get each defeated fleet's multipliers from one place, for example `LootContexts.forFleet(fleet)`: the run's context for a chart fleet, and a global context for every other fleet. They don't use a hardcoded `ItemChances.BASE`. The global context is `none()` for now, so behaviour is unchanged. Later the atlas fills it without touching any loot code. Site salvage outside charts should go through the same resolver.
2. Anchors as a drop type, with a ceiling. The per-fight anchor chance (1.4) and the market chance (1.5) stay flat for now. If atlas nodes such as "+X% wormhole anchor chance" are wanted, anchors get their own `DropType` and pass through a soft ceiling like colony items (`ceiling × (1 − e^(−chance / ceiling))`), so no stack of bonuses makes them certain.
3. Separate rarity rows. The battle table keeps `mods_1` to `mods_4` and `unique` as separate rows (2.2), so a Rarity effect, from a chart modifier or an atlas node, can scale the rare rows on their own. That makes the effect a data change.

Not covered by this plan, and needed before an atlas tree:
- Overworld events. Chart events (site budgets, fleet size and strength, the warden) work because a chart has a generation step where effects run (`beforeGeneration`). The overworld has none. Each overworld event needs its own hook, for example fleet strength through `NpcFleetLeveller` (it already reads a chart tree range) or salvage site content through the salvage listeners.
- Effect names. Every effect is `CHART_*`. Star Charts aren't released, so they can be renamed for free to neutral names once they also apply in the overworld.
- The tree. `SkillTree` is one static registry. Reusing the skill tree's UI, editor and allocation for an atlas tree means supporting more than one tree. That is the same refactor the planned per-hull-size trees need.
- Cost and storage. The global context is cached and rebuilt only when the atlas allocation changes, never per frame or per roll. Player-wide allocations live in sector persistent data.

## Part 3. Docs

- `docs/docs.md`: rewrite the NPC tree paragraphs about implicit anchors (real socketed anchor, kept only with a volume node, removed on recovery). Remove NPC items and sections. Add a "Battle item drops" section with the power formula, the table, the gates, the cap and the chart source multipliers. Update "Wormhole anchors" with the per-fight chance and the market stock.
- `docs/plans/chart_loot.md`: the cross-references in 2.3.
- Tick this plan's steps off as they land, with where they live.

## Build order

1. Remove NPC subroutine rolls and `NpcUniqueAlerts` (1.1). Done, on `parallel-work`.
2. Real socketed anchors, the volume-node-only rule, the NPC anchor link lookup, tag compatibility and removal on recovery (1.2, 1.3). Done, on `parallel-work`. The builder takes the anchor id in `NpcBuildRequest.factionAnchorItemId` and reads the faction exit from the anchor's own link.
3. Remove NPC sections and the per-ship Hull Upgrade drop (2.1). Done, on `parallel-work`. `HullUpgradeRoller.rollSectionCount` and `rollSections` went with it.
4. Per-fight anchor chance (1.4). Done, on `parallel-work`, then reverted on `main` (uncommitted, 2026-10-22): flat 50% per fight dropped about 10 anchors in a tutorial start, so anchors scale by DP again at 10% per 100 DP (cap 50%) and sell for 1% of their price. `ChanceMode` is removed.
5. `ItemRoller.rollWithEffectCount` and `BattleItemDrops` with its data file and setting, outside charts first (2.2). Done, on `parallel-work`. Planned drops are cut in the order mods_1, mods_2, hull_upgrade, mods_3, mods_4, unique. Battle drops of one common or rare item, and every unique, are still announced by name.
6. Chart integration: per-fleet `ItemChances`, and the hull item rolls removed from `exiledSector_chart_fleet` (2.3). Done, on `parallel-work`. The resolver is `charts/LootContexts`; `ChartDrops.liveContextAt` falls back to `LootContexts.global()` too. Chart fleets keep their subroutine, Modspec Cache, material, chart and cipher rows.
7. Market stock and anchor price, plus the editor columns (1.5). Done, on `parallel-work`. Open and military markets, plus RAT's Exoship market (open question 2). The listener (`effects/MarketItemStock`) rolls when a submarket's weapon and ship stock has just refreshed (`getSinceSWUpdate() == 0`), replacing its previous listings, and only once per restock (a timestamp in market memory; the Exoship market relies on RAT's counter alone, see docs.md). Selling earns 1% through the listing item (open question 3, lowered from 5% on 2026-10-22).
8. Docs (part 3). Done, on `parallel-work`. `docs/docs.md` has a new "Battle item drops" section; README and the forum post no longer send players to NPC flagships for hull items.

Steps 1 to 3 all edit `NpcFleetLeveller`, `NpcItems`, `NpcTreeTag` and the builder, so they run in sequence. Step 7 only touches the anchor data, `ItemPlugin`, a new listener and the editor, so it can run alongside any of them. Steps 4 to 6 all edit `ItemLootListener` and run in sequence.

## Tests

- Builder: a ship that reaches a volume notable has the real anchor in the socket the old implicit anchor used. A ship that can't afford one has no anchor and no open exit. Undoing a path removes the anchor. The same seed gives the same tree.
- `NpcTreeTag`: round trip with a socketed NPC anchor. An old tag with `anchors:` decodes to a socketed anchor. Old `frameworkTypes:` / `frameworkSockets:` fields and non-anchor socketed items are dropped.
- `WormholeAnchorLinks`: an NPC anchor id links its socket to its exit.
- Adoption: a recovered ship loses its anchor and its volume nodes, and keeps its core tree.
- `ItemLootListener`: dead NPC ships drop nothing they carried. One faction in a won battle rolls its anchor at 50%, two factions roll separately, a lost battle rolls nothing, and a faction with no ship lost rolls nothing.
- `BattleItemDrops`: the power formula, the difficulty multiplier, `wholeParts` per row, cap order (common rows cut first), the level 15 and setting gates, unique unlock gating, and `rollWithEffectCount` giving exactly N modifiers (fewer only where the pools are too small, and such definitions are filtered out).
- Chart integration: general and targeted chances scale only the chart fleet's share of a mixed battle. `exiledSector_chart_fleet` no longer gives hull items.
- Market: the listener stocks only the faction's own markets, honours `market_chance`, and the plugin prices anchors at the `price` column.
- `EditorRoundTripTest` / editor round-trip test with the new anchor columns.
- `TranslationKeyReferencesTest` after the alert strings go and the setting strings arrive.

## Open questions

1. Recovering an NPC ship: plan default is that the anchor is removed (as today). The alternative is that the player keeps it, which makes every recovered faction ship a free anchor. Decided 2026-10-20: the anchor is removed and the nodes behind it are refunded as banked free allocations (already how adoption worked).
2. Which market stocks anchors: plan default is the open market. The military market would gate them behind reputation or a commission, which fits a faction handing out its own wormhole tech. Decided 2026-10-20: open and military markets, 30% per restock each. Built, on `parallel-work`.
3. Selling price: `getPrice` sets the value for buying and selling alike. With a 50% drop chance per fight, every anchor drop could be sold for close to 100,000 credits, minus tariffs. That is a large credit faucet. Options: accept it, find a buy-only price hook (decompile `BaseSubmarketPlugin` / `CargoDataGridView`), or lower `price` and live with a cheaper shop. Decided 2026-10-20: selling earns 5% of the buying price, lowered to 1% on 2026-10-22. Built with a separate listing special item (`exiledSector_socketable_listing`) that converts to a normal item once bought, since `getPrice` can't tell buying from selling.
4. The two faction-sourced unique subroutines (Halcyon Fire-Control Lattice from Tri-Tachyon, Cracked Drive Governor from pirates) lose their only NPC route. Give them a small `FactionFleetDrops` battle chance (not on ships, so still within "no other subroutines on NPC ships"), or leave them to sites and crafting for now? Decided 2026-10-20: unique subroutines are salvage only for now. Halcyon moved to research stations, Cracked Drive Governor to mining stations.
5. Do we want the chart Rarity effect from 2.3 now, later, or never? Decided 2026-10-20: yes. Built as `CHART_ITEM_RARITY_PERCENT` (the Treasure suffix), on `parallel-work`.
6. Should Doctrine Archives get their missing drop source here (a `FactionFleetDrops` per-fight or per-DP roll from their own faction), or stay console only until their content review? Decided 2026-10-20: only tier 14 and 15 chart wardens drop them (nothing dropped them before). Built, on `parallel-work`.
