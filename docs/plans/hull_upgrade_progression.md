# Hull upgrade progression: plan

Hull upgrade points let a ship unlock its sections (bridge, reactor, flight deck and so on) for hull items. Today every ship starts with none and gains one per Hull Upgrade item, up to four. This plan replaces that with two ways to earn points, picked in LunaLib:

- Refit schematics (the default): every ship starts with one point. Four tiers of a new currency item raise a ship's points one step at a time, up to five, in the style of Path of Exile 2's Jeweller's Orbs.
- Star Chart progress: every ship starts with one point, and clearing the first tier 5, 10 and 15 Star Charts raises every player ship to two, three and four.

Status: planned, not built.

## Today

Paths are relative to `exiledsector-hull-upgrades/src/main/java/exiledsector/` unless they name another module.

- Points are stored per ship as `ShipSkillData.frameworkPoints` (core, `skills/ShipSkillData.java`). `getHullUpgradePoints` returns it, `getUnspentHullUpgradePoints` subtracts the unlocked sections, and `unlockSocketType` checks unspent points itself.
- `items/HullUpgradeRules` holds `MAX_POINTS = 4`, the install rule (`upgradeBlock`, `installUpgrade`) and the section unlock rule (`unlockBlock`, `unlock`).
- The item is `HullUpgradeData` (core, `items/HullUpgradeData.java`): one special item id, `exiledSector_hull_upgrade`, whose data is a hull size. It only fits ships of that size. `ItemStore` (core) keeps counts per hull size (`upgradeCount`, `addUpgrades`, `takeUpgrade`), and `HullUpgradeData.absorbInto` moves cargo stacks into it.
- Sources: battle drops (`items/BattleItemDrops`, drop kind `HULL_UPGRADE`, chance in `data/config/exiledSector/battle_item_drops.csv`), salvage and tech mining (`ItemDrops.SectionItemLoot` and `SiteLoot` in core, through `items/HullUpgradeLootSource`), and the console (`console/GrantHullUpgradesCommand`).
- Spending: `ui/socket/HullUpgradePanel` shows the ship's points, one upgrade tile for its hull size with the storage count, an install confirmation (`framework.confirm.*`) and the section tiles. Strings are in `data/strings/exiledSector/hullUpgrades/`.
- Gate: `items/HullUpgradesSetting` switches the domain on and holds the player level sections open at (default 15). That stays as it is.
- Chart clears: `ItemUnlock.highestClearedChartTier` (core) reads the sector flag `$exiledSector_chartsClearedTier`, which `ChartRewards` raises through `ItemUnlock.recordChartCleared` when a run is cleared. It's sector memory, so it already belongs to the player's game rather than to any ship.

## The setting

A radio setting on the Hull Upgrades part of the settings page, "Hull Upgrade Points": `Refit Schematics` (default) or `Star Chart Progress`. It reads live. Switching never takes away unlocked sections (see "Switching modes").

## Shared points model

New `items/HullUpgradePoints` decides a ship's points:

- Refit schematics: `max(1, frameworkPoints)`. A schematic sets `frameworkPoints` to its tier's value.
- Star Chart progress: 1, plus one for each of tiers 5, 10 and 15 that `highestClearedChartTier` has reached. `frameworkPoints` is ignored but kept, so switching back restores the schematic points.

Everything that reads points goes through it: `HullUpgradeRules`, the panel, the section tooltips and the console. `ShipSkillData.getUnspentHullUpgradePoints` and the check inside `unlockSocketType` can't see the setting from core, so they take the point count as an argument (or the check moves to `HullUpgradeRules.unlock`, which already checks first).

Points apply to the player's ships only. NPC ships have no hull upgrades (see "NPC ships").

## Option 1: refit schematics

Four new currency items, one per tier:

| Tier | Name | Raises a ship | Drop weight |
|---|---|---|---|
| Degraded | Scrapped Refit Schematic | from 1 point to 2 | 60 |
| Common | Standard Refit Schematic | from 2 to 3 | 28 |
| Rare | Military Refit Schematic | from 3 to 4 | 10 |
| Unique | Domain Refit Schematic | from 4 to 5 | 2 |

Drop weights are placeholders pending balance.

`MAX_POINTS` becomes 5 in this mode (there are nine sections).

The tiers are strict, following PoE2's Jeweller's Orbs. In PoE2 a Lesser Jeweller's Orb only works on a skill gem with exactly two support sockets and adds a third, a Greater only on three, and a Perfect only on four. Each orb is consumed on use, and none works out of order. So here a schematic works only on a ship with exactly one point fewer than its tier. A Standard schematic does nothing on a ship with one point, and a Scrapped one does nothing on a ship with two or more. A ship goes 1 to 5 by using one schematic of each tier in order.

The schematics fit every hull size. A destroyer and a capital use the same one, unlike today's per-size upgrades.

### Items

- One special item id per tier, or one id with the tier as data (like `HullUpgradeData` now). One id with data keeps old cargo stacks readable (see "Migration") and matches the existing item.
- Built as currency items, alongside `CraftingCurrency` (core): an enum in this module, `RefitSchematic`, implementing `ItemCurrency` if that interface fits use outside the crafting bench. Check what `ItemCurrency` requires before choosing. Schematics are applied only on the Hull Upgrades panel, never on the bench.
- `ItemStore` keeps counts per tier instead of per hull size. Cargo stacks are absorbed the same way as today.
- Placeholder icons until art is made, in `graphics/icons/hull_upgrades/`, one per tier with the tier's rarity colour.

### Applying a schematic

`HullUpgradePanel`'s single upgrade tile becomes a row of four schematic tiles with their storage counts. The tile that fits the ship's next step is highlighted. The others are dimmed, with a tooltip saying why: "Needs a ship with 2 points." or "This ship already has 3 points." Clicking the usable tile opens the existing confirmation, reworded for schematics. Applying sets the ship's points to the tier's value and consumes the schematic.

`HullUpgradeRules.UpgradeBlock` gets `WRONG_TIER` next to `AT_MAX_POINTS` and `NONE_IN_STORAGE`.

### Sources

Schematics replace Hull Upgrades in every source: battle drops, salvage, tech mining and the console. The per-hull-size roll in `BattleItemDrops` becomes a tier roll using the weights in the table. Every tier can drop from any source.

## NPC ships

NPC ships have no hull upgrades. As far as the code shows, none get them today: nothing gives an NPC build points or unlocked sections, and `NpcTreeTag` encodes only nodes and socketed subroutines, not sections. What's left to remove:

- The NPC branch in `skills/ShipSections.appliedEffects` (`shipData.isNpcBuild()` filtering section effects), which can then apply section items with no NPC check.
- Any section data on an NPC build: `NpcSkillTreeBuilder` and `NpcTreeTag.decode` never set `frameworkPoints` or unlocked sections, and a test pins that.
- A captured NPC ship (`ShipTreeSync.adoptNpcTree`) starts with the player's rules: one point, or whatever Star Chart progress gives.

Hull items still drop from NPC battles. That's loot, not an NPC ship's own upgrades.

## Option 2: Star Chart progress

- Every player ship has one point.
- Once the player has cleared any tier 5 Star Chart, every player ship has two. A tier 10 clear gives three and a tier 15 clear gives four.
- It's read live from `highestClearedChartTier`, so it covers every ship in the fleet, every ship added later, and ships in storage.
- Schematics don't drop in this mode. Held schematics stay in storage but can't be applied, and their tooltip says Star Chart Progress is on.
- The panel shows the next step instead of schematic tiles: "Clear a tier 10 Star Chart to raise every ship to 3 points."
- When a clear raises the point count, a campaign message says so.
- If the Star Charts domain is switched off, ships stay at one point. The setting's tooltip says Star Chart Progress needs Star Charts.

## Switching modes

Switching never locks a section. If a ship has more sections unlocked than its new point count, they stay unlocked and it can't unlock more until its points catch up. Stored schematic points and held schematics are kept for switching back.

## Migration

Saves with today's Hull Upgrades:

- Ships keep their points. A ship with three points from old upgrades has three under option 1 and needs a Military Refit Schematic next. A ship with none has one.
- Old Hull Upgrade items in cargo and storage are converted once on load: each becomes one Standard Refit Schematic. `ItemStore`'s per-hull-size counts are an existing persisted field: read them once in the migration, then clear them.
- `HullUpgradeData` stays readable for old cargo stacks until the migration has run, then goes.

## Strings and docs

- New and reworded keys in `data/strings/exiledSector/hullUpgrades/en.json` and `zh_CN.json`: schematic names and tooltips, the panel row, the `WRONG_TIER` reasons, the Star Chart progress lines, the campaign message, and the setting and its tooltip. All wording follows `docs/tooltip_wording.md`.
- `docs/docs.md`: a short "Hull upgrade points" section on both modes and the schematic tier rule.
- README and `docs/forum_post.bbcode`: one line on each mode.

## Decided

- Names: Scrapped, Standard, Military and Domain Refit Schematic (2026-10-23).
- Strict tiers, as in PoE2 (2026-10-23).
- Each old Hull Upgrade becomes one Standard Refit Schematic (2026-10-23).
- Drop weights 60 / 28 / 10 / 2, from any source (2026-10-23).
- NPC ships have no hull upgrades at all (2026-10-23).

## Build order

1. `HullUpgradePoints`, the setting, and the point reads moved onto it.
2. The schematic items, `ItemStore` per tier, and applying them on the panel.
3. Sources switched to schematics.
4. Star Chart progress mode, its panel text and message.
5. NPC leftovers removed.
6. Migration of old upgrades.
7. Strings and docs.

## Tests

Each test goes in the lowest module that sees every class it uses.

- `HullUpgradePoints`: schematics mode gives `max(1, stored)`. Chart mode gives 1, 2, 3 and 4 at cleared tiers 0, 5, 10 and 15 (and 1 at tier 4, 2 at tier 9), and ignores stored points.
- Schematics: each tier applies only at one point below its value, sets the points and consumes the schematic. A wrong-tier schematic or a ship at 5 is refused with the right block, and the schematic is kept.
- No schematic applies in chart mode, and none drops in it.
- Switching modes never locks a section. An over-budget ship can't unlock more.
- An NPC build has no points and no unlocked sections, before and after a tag round trip. A captured NPC ship starts at the player's point count.
- The migration keeps ship points, turns each old item into one Standard Refit Schematic, and never runs twice.
- The sources roll schematics by tier weight, and old per-size upgrades never appear.
- `TranslationKeyReferencesTest` covers the new keys.

Sources for the Jeweller's Orb rule: [Maxroll PoE2 crafting overview](https://maxroll.gg/poe2/resources/path-of-exile-2-crafting-overview), [Gfinity: how to get more support gem slots](https://www.gfinityesports.com/article/path-of-exile-2-how-to-get-more-support-gem-slots).
