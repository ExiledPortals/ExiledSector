# Master Schematics: plan

An optional mode, off by default, in which a ship's skill tree stays shut until a Master Schematic is used on it. A Master Schematic is consumed on the Hull Upgrades panel of the Hull Items screen and opens the tree for that one ship for good. It's a rare currency, so the player picks which ships get trees.

Status: built, uncommitted on `parallel-work` (2026-10-29). Not yet tried in game.

## Name

Master Schematic (decided 2026-10-29, after "Refit License" and "Overhaul Refit License" were dropped: hull mods are already the refit, and "Overhaul" clashed with Efficiency Overhaul).

It's the full design file for one hull. Hull mods are generic; this gives the refit crew a far more detailed understanding of their own ship, and with it much finer control. It's written for one hull, which is why it opens one ship and can't be moved. It sits with the Refit Schematics as the second ship currency, and it reads the same on crewed and automated hulls.

Flavour (written by the user, 2026-10-29; keep as written):

> Every hull mod is written for a generic hull. This file is written for exactly one, by the people who built it. A refit crew working from it can adjust the ship's hull in much finer detail.

## The setting

A checkbox on the Hull Upgrades part of the settings page, "Require Master Schematics", off by default. It reads live. It sits with Hull Upgrades rather than the Skill Tree because the item is only used there, and the Hull Upgrades module can see both its own switch and core, while the skill tree module can't see Hull Upgrades.

While Hull Upgrades is switched off, every tree is open whatever this setting says (`HullUpgradesSetting.masterSchematicsRequired`), and the setting's tooltip says it needs Hull Upgrades.

## A locked ship

- The Skill Tree refit button is disabled with the tooltip "Needs a Master Schematic. Use one on the Hull Upgrades panel." The button under the hull mods plays the disabled sound and doesn't open.
- The ship switcher on the tree's ship card shows locked ships dimmed and won't open them.
- No tree effects, phantom hull mods, OP reserve or fit effects apply. The skill tree hull mod stays installed but contributes nothing.
- The ship keeps earning XP and keeps the level floor, so a veteran opens with its levels ready to spend.

## Where it lives

- Core `items/MasterSchematic`: the item (`exiledSector_master_schematic`), `treeOpen`, the `required` supplier the Hull Upgrades domain binds at load, change listeners, and the salvage roll. `ShipCurrency` is the shared type of Refit and Master Schematics in loot records.
- `ShipSkillData.skillTreeUnlocked` (core), which also keeps the record from being dropped as blank.
- `ItemStore` keeps a count; cargo stacks are absorbed like Refit Schematics.
- Hull Upgrades: `HullUpgradeRules.masterSchematicBlock` and `applyMasterSchematic`, `MasterSchematicActivation` (switch-on handling), `MasterSchematicItemPlugin`, the Skill Tree row on `HullUpgradePanel`, and `GrantMasterSchematicsCommand`.
- Skill tree: `SkillTreeEffectSource`, `ShipTreeSync.syncVariant`, `OpReserveHullMods`, `SkillTreeDomain.fitEffects`, `SkillTreeRefitButton`, `FleetShipList`, `PlayerTreePower`, `NpcFleetLeveller`.

## Sources

- Battle drops: a `master_schematic` row in `battle_item_drops.csv`, placeholder 0.05 per 100 power (Refit Schematics are 0.10). Enemy ships with bigger trees count for more power, so treed enemies drop more. They drop before the Hull Upgrades level gate opens; the other battle items still wait for it.
- Salvage, Tech Mining and chart clears: a roll at half the site's first-item chance (`MasterSchematic.SALVAGE_CHANCE_SHARE`), also ungated by level.
- The console: `ExiledSectorGrantMasterSchematics [copies]`.
- Nothing drops while the mode is off. Held ones stay in storage.

Drop rates are placeholders pending balance.

## NPC difficulty scaling

Open-world NPC trees mirror the player: each NPC ship rolls 80% to 120% of a random player combat ship's tree power (`PlayerTreePower`, `NpcTreeSize.roll`), then the category cap applies (`NpcLevelCaps`). The mode changes only what goes into that list.

A locked ship counts as 0 power, whatever its level, and a fleet with no combat ships counts as 0 instead of the level floor. An open-world NPC ship that rolls 0 nodes records `NOT_LEVELLED` and carries no tree at all.

So the share of NPC ships with trees follows the share of the player's combat ships that are unlocked, and their tree sizes follow the unlocked ships' power. With six combat ships, two of them unlocked at 20 and 30 power, two in three NPC ships get no tree, and the rest roll 16 to 24 or 24 to 36 nodes before the cap.

Two alternatives were weighed and rejected:

- Counting locked ships at their level would give every NPC a full tree while the player has a few, which is the opposite of the mode.
- Mirroring only unlocked ships would give every NPC ship the power of the player's best one or two. A thirty-ship pirate fleet, every ship treed, against a fleet with two trees.

Category caps, the bonus scale (`NpcBonusScale`) and the NPC ship selection chance are unchanged. A bounty flagship's +3 levels still apply on a 0 roll, so it always has a small tree.

### Star Chart fleets

Chart fleets roll fixed tier levels from 26 up. In this mode each chart ship is levelled only at its tier's `tree_share` in `star_chart_tiers.csv` (placeholder 50% at tier 1, rising by 5% a tier to 100% at tier 10), carried on `FleetLevelRange.treeShare`. Boss fleets always have trees. Levelled ships keep their tier's level range.

### Captured ships

A recovered NPC ship keeps its adopted tree (`ShipTreeSync.adoptNpcTree`, trimmed to its level) but arrives locked. A Master Schematic opens it with the nodes already in place.

## Turning the mode on and off

- On (on load or when the setting changes in a campaign): every ship with a node allocated besides its root is unlocked, so no existing work is locked away. A sector memory flag marks the mode active, so ships captured later aren't unlocked by the next load.
- On, in a save with no unlocked ship: the player gets one Master Schematic, once per save, to pick a ship with.
- Off: every tree opens and the active flag is cleared. Unlock flags and held Master Schematics are kept for switching back.

## Decided

- Icon: vanilla `blueprint_hightech` datapad with the Refit Schematic renderer's wireframe line-up (Wolf, Hammerhead, Dominator, Onslaught) screen-blended onto its screen in white at the line-up's full size, cropped by the screen edges (option D5, 2026-10-29).

- Name: Master Schematic (2026-10-29).
- Locked ships keep earning XP; charts get `tree_share`; captured ships arrive locked; markets don't sell them for now (2026-10-29, the plan's recommended defaults, built when work started).

## Open questions

- Whether markets should sell them (military and black markets, rarely).

## Build order

1. Done: `skillTreeUnlocked` on `ShipSkillData`, `MasterSchematic` in core, the setting and its binding.
2. Done: the gate on effects, phantoms, OP reserve, fit effects, the refit button and the ship switcher.
3. Done: the item, `ItemStore` count, the panel row and `MasterSchematicBlock`.
4. Done: `PlayerTreePower` at 0 for locked ships and the empty-fleet fallback; 0-node rolls leave NPC ships treeless.
5. Done: sources and the console command.
6. Done: chart `tree_share`.
7. Done: switching on mid-save (`MasterSchematicActivation`).
8. Done: strings (en and zh_CN draft), `docs/docs.md`, README and forum post.

## Tests

- `MasterSchematicTest` (core): open and shut rules, the flag keeps the record, storage, absorbing stacks, the salvage roll.
- `MasterSchematicRulesTest` (Hull Upgrades): requires Hull Upgrades, applying and refusing, switch-on unlocking, the starter schematic once, captured ships stay locked.
- `BattleItemDropsTest`: Master Schematics drop only while required, and alone before sections open.
- `PlayerTreePowerTest`: locked ships and empty fleets count 0.
- `NpcFleetLevellerTest` (mod): an NPC mirroring only locked ships gets no tree.
- `ChartTierLevelsTest` (mod): the tier share and the boss exception.
