# Module toggles: plan

Exiled Sector has three modules: the skill tree, hull upgrades and star charts. Players asked to pick which ones a run uses, and to get hull upgrade loot before player level 15. This plan adds an unlock level setting for hull upgrades and an on/off switch for star charts, and plans an off switch for the skill tree that is not built yet.

The level-15 gate is deliberate, so new players aren't hit with all three modules at once. 15 stays the default.

Status: steps 1 to 4 built on `main` (cherry-picked from `claude/loving-hofstadter-a8f91e`, which branched from `parallel-work`). The skill tree off switch is planned, not built.

## Decided

- Unique Subroutines get their own unlock level setting, separate from hull upgrades.
- When the skill tree is off, NPC trees switch off with it.
- The switches are global LunaLib settings. No per-save module choice.
- Star charts off: an open run plays out, a built gateway goes inert, held charts stay in storage.

## What exists

- Hull upgrades already have a switch, `exiledSector_hullUpgradesEnabled` (`items/HullUpgradesSetting`). When it is off, Hull Upgrades and hull items stop dropping and the Hull Items refit button is hidden. Sections already unlocked keep working and nothing is removed.
- The level gate is hardcoded in `items/ItemUnlock`: `SECTION_ITEM_MIN_PLAYER_LEVEL = 15` for Hull Upgrades and hull items (`sectionsOpen`), and `UNIQUE_MIN_PLAYER_LEVEL = 15` for every unique item in `canDrop`. Unique hull items only come from pools that `sectionsOpen` already gates, so in practice the unique gate only decides when unique Subroutines (skill tree loot) can drop.
- NPC trees have their own switch, `exiledSector_npcTreesEnabled`. It only stops new trees; fleets that already have one keep it.
- Star charts have no switch. The Wormhole Gateway is a colony structure (`data/campaign/industries.csv`, `charts/WormholeGateway`), gated on the Janus flag or on the Vaults of Knowledge row when AoTD is installed.

## Settings are global

LunaLib settings apply to every save, not to one run. That is fine for hull upgrades and star charts, because switching them never deletes anything and switching back restores the module. A per-save choice made at new game would need its own UI and save field and isn't part of this plan.

## Step 1: hull upgrade unlock level (done)

A LunaLib int, `exiledSector_hullUpgradesUnlockLevel`, from 1 to 15, default 15, next to the Hull Upgrades switch on the main tab. It lives in `HullUpgradesSetting` with the same cached value and settings listener as the switch.

- `ItemUnlock.sectionsOpen(int)` compares against the setting instead of the constant. Every drop source already goes through it: battle loot, salvage, tech mining, market stock, chart rewards and chart fleet loot.
- `ItemUnlock.canDrop` uses the hull upgrade level for unique hull items.
- `framework.panel.upgradeNone` takes the level as a placeholder. The Hull Item Drops From Battles tooltip refers to the Hull Upgrades level instead of 15.
- Tests: `ItemUnlockTest`, `HullUpgradesSettingTest`, `ExiledSectorSettingsTest`.

## Step 2: unique Subroutine unlock level (done)

`items/UniqueSubroutinesSetting`, a LunaLib int `exiledSector_uniqueSubroutinesUnlockLevel`, 1 to 15, default 15, because unique Subroutines are skill tree loot and shouldn't move with the hull upgrade level. `ItemUnlock.uniqueMinPlayerLevel` picks the hull upgrade level for hull items and this one for everything else.

## Step 3: star charts switch (done)

A LunaLib boolean, `exiledSector_starChartsEnabled`, default on, at the top of the Star Charts header. `StarChartSettings` caches it with a settings listener, like `HullUpgradesSetting`.

When it is off:

- The Wormhole Gateway can't be built and is hidden from the build menu (`WormholeGateway.isAvailableToBuild`, `showWhenUnavailable`). A gateway that is already built stays but goes inert: its colony option is gone (`WormholeGatewayOption`) and `WormholeOpenCheck` refuses first with a `DISABLED` blocker, which covers the bench's open button.
- No chart or chart currency drops from anywhere. `ChartDrops.roll` is the single choke point for every row of `chart_drops.csv` (battles, salvage, tech mining, run rewards, chart fleets and wardens). Generic item pools can't return charts, so nothing else needs gating.
- The bench offers no mode switch (`ModificationBay.offersModeSwitch`), so it stays on hull items, and `ModificationBenchCargoLauncher.openStarCharts` does nothing. The chart cargo tooltip gets a "switched off" line like the Hull Upgrade one.
- Charts the player holds stay in storage. Nothing is deleted, and switching back on restores everything.
- A run that is already open is left alone and finishes normally, so the fleet is never stranded in a chart system. The run scripts, chart battle plugin and run closer stay ungated. Its run reward and chart fleet drops roll nothing, since they go through `ChartDrops.roll`.
- Console commands stay ungated.
- With AoTD, the Vaults of Knowledge row is CSV data and stays; researching it just unlocks a gateway that can't be built.

Tests: `ChartDropsTest`, `WormholeGatewayTest`, `WormholeGatewayOptionTest`, `WormholeOpenCheckTest`, `StarChartSettingsTest`, `ExiledSectorSettingsTest`.

## Step 4: docs (done)

`docs/docs.md` covers mechanics not evident from tooltips, so these settings need nothing there. README and `docs/forum_post.bbcode` say after the three-module intro that hull upgrades and star charts can be switched off, and the "From level 15" line says the level can be lowered. The skill tree joins that sentence once its switch is built.

## Skill tree off switch (planned, not built)

### Why it is harder

The tree, hull upgrades and chart effects all run through one hull mod, `exiledSector_core` (`SkillTreeHullMod`), which every player ship carries. Its resolved tree includes the hull item section effects, and it also applies the player fleet's chart conditions. Hull upgrade state (`frameworkPoints`, unlocked sections, section items) lives in the same `ShipSkillData` record as the nodes, level and XP. So the tree can't be switched off by removing the hull mod or the record.

### Proposed behaviour

A LunaLib boolean, `exiledSector_skillTreeEnabled`, default on. When it is off:

- The core hull mod stays. Its resolve skips node effects (allocated nodes, socketed Subroutines, Doctrine Archives, granted nodes) and keeps section effects and chart conditions.
- Phantom hull mods are removed by `PhantomInstallSync`, as if no node were allocated, and the OP reserve hull mods are removed the same way.
- Nodes, level and XP stay in each ship's record, untouched and unrefunded. Combat XP and the level floor are skipped while it is off. Switching back on restores every tree at the next sync.
- The Skill Tree refit button and its K keybind are hidden, not removed: the Hull Items and bench buttons anchor below it, and MagicLib's paintjob button anchors on our last refit component. Both need re-anchoring when it is hidden.
- Tree loot stops dropping: Subroutines (including uniques), Wormhole Anchors, Doctrine Archives and Contraband Modspec Caches, from battles, salvage, showcases, markets and faction fleet drops.
- NPC trees switch off with it, whatever the NPC tree setting says. Existing NPC trees on fleets already levelled stay until those fleets despawn, as the NPC switch does today.

### Knock-on effects to solve before building

- Chart fleets get their difficulty from NPC trees (`NpcFleetLeveller`, `ChartTiers.fleetStrengthRangeFor`). Without trees they keep their size, quality and officer scaling but lose the tree levels, so higher tiers need another source of difficulty.
- Battle chart drops pick their tier from the highest defeated NPC tree level (`ItemLootListener`, `ChartDrops.battleTier`). Without trees that collapses to tier 1 and needs a DP-based fallback.
- Battle hull item drops weigh ship power as DP times (1 + nodes / 30) (`BattleItemDrops.shipPower`). That falls back to plain DP, which is acceptable.
- The Hull Upgrades screen reuses the tree's allocation session (`HullUpgradesScreen`, `NodeAllocator`). It must keep working with the tree off.
- The codex and NPC inspect screen show tree pages that should be hidden.

### Build order (later)

1. Setting and cached flag, resolve that skips node effects, phantom and OP reserve removal. Tests that a ship with nodes and sections keeps only section effects, and gets everything back when switched on.
2. Skip XP and level floor; hide the refit button and keybind, re-anchor the buttons below it.
3. Gate tree loot.
4. NPC trees and the chart difficulty and drop-tier fallbacks.
5. Codex and inspect screens, README and forum post.
