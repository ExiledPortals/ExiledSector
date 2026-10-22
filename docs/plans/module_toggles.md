# Module toggles: plan

Exiled Sector has three modules: the skill tree, hull upgrades and star charts. Players asked to pick which ones a run uses, and to get hull upgrade loot before player level 15. This plan adds an unlock level setting for hull upgrades and on/off switches for star charts and the skill tree.

The level-15 gate is deliberate, so new players aren't hit with all three modules at once. 15 stays the default.

Status: steps 1 to 4 built on `main` (cherry-picked from `claude/loving-hofstadter-a8f91e`, which branched from `parallel-work`). The skill tree off switch is built, uncommitted on `parallel-work`, as step 8 of `domain_split.md`.

## Decided

- Unique Subroutines get their own unlock level setting, separate from hull upgrades.
- When the skill tree is off, NPC trees switch off with it.
- The switches are global LunaLib settings. No per-save module choice.
- Star charts off: an open run plays out, a built gateway goes inert, held charts stay in storage.

## What exists

- Hull upgrades already have a switch, `exiledSector_hullUpgradesEnabled` (`items/HullUpgradesSetting`). When it is off, Hull Upgrades and hull items stop dropping and the Hull Items refit button is hidden. Since step 8 of `domain_split.md`, sections already unlocked also stop applying, but nothing is removed and switching back on restores them.
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

`docs/docs.md` covers mechanics not evident from tooltips, so these settings need nothing there. README and `docs/forum_post.bbcode` say after the three-module intro that hull upgrades and star charts can be switched off, and the "From level 15" line says the level can be lowered. The skill tree joined that sentence with its switch.

## Skill tree off switch (done)

A LunaLib boolean, `exiledSector_skillTreeEnabled` (`skills/SkillTreeSetting`), default on, under a Skill Tree header at the top of the tree's settings. It is `SkillTreeDomain.enabled()`, so the domain framework drops the tree's effect source (node, Subroutine, Doctrine and granted node effects, fit effects, the station module share) and loot source (faction fleet drops, warden Doctrine Archives, showcase items, Anchor market stock). When it is off:

- `ShipTreeSync` strips phantom and OP reserve hull mods from player ships at the next fleet pass, as if no node were allocated. Nodes, level and XP stay in each record, and switching back marks every player ship for a stat rebuild, which puts them back.
- No combat XP, no level floor, no Hyperspace Navigation Beacon grant.
- Subroutines leave the generic item pools and Modspec Cache salvage.
- The Skill Tree button and its K key are hidden; the column re-anchors the buttons below it and parks the hidden one off screen. The LunaLib entry, the tree chip and the panel open call refuse too.
- No new NPC trees. Fleets already levelled keep theirs, including their phantom hull mods, until they despawn.
- The codex adds no tree entry to ship pages, and the NPC inspect key does nothing.
- Battle chart drops take their tier from defeated deployment points (`strength_per_tier` in the battle row of `chart_drops.csv`), and battle hull item power falls back to plain DP. Chart fleets keep their size, quality and officer scaling but get no tree levels.

Known gaps: Anchor listings already on a market stay until the tree is back on, and the core hull mod tooltip loses its Hull Upgrades lines, since they are drawn by the tree's summary.
