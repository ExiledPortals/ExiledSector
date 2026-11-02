# Skill type and node id renames

Goal: someone reading a node's name in game can search the code for it. "Heavy Armor" should find `heavy_armor`, not `heavyarmor`; "Lion's Gaze" should find `lions_gaze`, not `beam_split`. Node ids are `<typeId>_<n>`, so the type ids are renamed first and the node ids follow.

Two proposed mappings, generated on 2026-11-02 from `skill_types.json`, `ship_skill_tree.json`, `released_skill_nodes.json` and `node_replacements.csv`:

- [type_id_renames.csv](type_id_renames.csv): 110 of 224 skill types.
- [node_id_renames.csv](node_id_renames.csv): 283 of 516 nodes, 277 of them in released saves (`in_released_saves = yes`). The other 6 never shipped and need no row.

If display names change before this lands, regenerate both files rather than hand-editing them.

## Rules used

- The new id is the English display name, lower-cased, with apostrophes dropped and other punctuation turned into `_`. For example `fuel_core_overload` becomes `ludds_light`, and `small_flux_optional` becomes `affinity`.
- Doctrine-only types are the 21 types that only appear in archive pools and are never placed in the tree. They become `doctrine_` plus the display name, with the faction segment dropped (`doctrine_tt_flux_arbitrage` becomes `doctrine_flux_arbitrage`, `doctrine_remnant_automated_command` becomes `doctrine_tireless_cadence`). Pool types that are also tree nodes (`tactical_superiority`, `violence_of_action`, `machine_precision`) follow the normal rule.
- A node keeps its number (`missile_damage_3` becomes `missile_weapon_damage_3`). Nodes whose id stem no longer matches their type take the next free number: `acceleration_percent_1/2` and `turn_acceleration_percent_1/2` become `maneuverability_7` to `_10`, and `phase_sensor_upgrades_1` becomes `hybrid_p_space_shrouding_1`.
- The following stay exactly as they are:
  - Mod-prefixed ids (`nskr_`, `rat_`, `tania_`), including Tania nodes whose type is renamed (`tania_small_flux_optional_1`).
  - The root types and nodes.
  - Wormholes and wormhole exits, and `exoship`.
  - `hull_multi` (see decision 2).
- No new id reuses an id that is in the tree, in `released_skill_nodes.json` or in `node_replacements.csv`. A reused id would make an old save's node look already current.
- Hull mod ids are not type ids, even where they look the same. `hullmod_conflicts.csv`, `LostSectorCompat` and the `phantomHullMods` / `exclusiveHullMods` lists keep `heavyarmor`, `adaptive_coils` and the rest. No code assumes a type id equals a hull mod id.

## Open decisions

1. Doctrine archives reshuffle. Decided 2026-11-02: accept the one-time reshuffle (option a) and note it in the changelog. Background: archives shipped in 0.9.12. The item saves only its definition and seed; `DoctrineOverrides` works out the rewrites again whenever the ship changes. Transform picks (`candidateOrder`) hash the seed, node id and candidate type id, and must also be valid for that ship. Augment lines (`augmentLines`) hash the seed and node id only. Renaming ids therefore reshuffles almost every owned archive. So do edits to the archive pools or to the tree around a socket, so archives are not frozen today. The options are:
   - (a) accept a one-time reshuffle, noted in the changelog;
   - (b) hash a permanent pre-rename id, which means a lookup that never retires;
   - (c) store augment lines on the item.

   Transform picks can't be frozen on the item, because they depend on the ship.
2. `hull` and `hull_multi` are both called "Hull" but give different amounts (2% vs 5%). One of them needs another display name before `hull_multi` can be renamed.
3. Display names that aren't title case become ids as they are, so fix them first: "Crew capacity", "Survey cost reduction", "Survey supplies cost reduction", "Shield Raise rate or Shield Turn rate", "Shield flux/damage reduction", "Less EMP damage taken", "Increased vent rate", "Vent rate and EMP damage".

## What saves hold

Node ids sit in `ShipSkillData` (renamed fields keep their old saved names through `ShipSkillData.SAVED_FIELD_NAMES`): `allocatedNodeIds`, `dormantNodeIds`, `freeNodeIds`, `pairGrantedNodeIds`, `subroutineGrantedNodeIds`, `replacementAllocatedNodeIds`, `startingRootId`, and the keys of `choiceSelections`, `socketedItems`, `paidItemCostIds` and `paidItemCostQuantities`. They also sit in saved templates, subroutine rolls and NPC anchor tags. `NodeReplacements` (`data/config/exiledSector/node_replacements.csv`, `old,new`) already translates all of those on load. `ShipTreeRepairs.replaceRemovedNodes` runs before unknown nodes are forgotten.

Type ids are saved only as chosen options: the values of `choiceSelections`, and the option part of template steps (`NodeEntry`). Five option ids change: `affinity_low_tech`, `affinity_high_tech`, `affinity_midline`, `ballistic_damage` and `non_beam_energy_damage`. `shield_raise_rate` and `shield_turn_rate` already match their names. No translation key contains a node id; `zh_CN.json` holds `skillType.<id>.*` keys for every renamed type.

The gap: `ShipTrees.replaceNode` was built for removed nodes. It drops whatever is socketed in the old node (`unsocketItem` just removes the map entry), and it skips nodes that aren't allocated. A rename has to keep the 6 Subroutine Sockets' subroutines.

## Steps

1. Add `data/config/exiledSector/node_id_renames.csv` and `type_id_renames.csv` (`old,new`; only released rows for nodes). Keep them separate from `node_replacements.csv` so they can be deleted on their own later. `NodeReplacements.load` merges the node rows into its lookup and remembers which are renames. A small type-alias lookup maps old option ids in `choiceSelections`, `NodeEntry` and `ShipTrees.carryOptionOver` / `hasInvalidOption`.
2. Add a rename path in `ShipTrees` that moves every node-keyed field listed above without dropping sockets, charges or options. `ShipTreeRepairs` uses it for rename rows and keeps `replaceNode` for replacement rows.
3. Point the existing `node_replacements.csv` targets straight at the new ids (`small_flux_optional_15` becomes `affinity_15`, `non_beam_energy_damage_6` becomes `non_beam_energy_weapon_damage_6`).
4. Rewrite the data:
   - `skill_types.json`: `id`, `options` and any type references.
   - `ship_skill_tree.json`: node `id`, `type`, `connectedTo` and `pairedWith`.
   - `doctrine_archives.csv`: the 12 type ids in the notable/keystone/small columns.
   - `zh_CN.json`: the `skillType.<id>.*` keys.
   - `AffinityOptions.java`: the three option constants.
   - `ChartEffect.java`: the four node literals.
   - Tests: about 50 Java test files and a few editor tests name these ids.

   Rename identifiers outside string literals only, then diff every changed string literal. The run must not touch hull mod ids.
5. Update `released_skill_nodes.json`: switch `nodes` to the new ids and add a `retired` list with the old ones. That list keeps the old ids reserved after the CSVs go. `tools/editor/server/reservedNodeIds.ts` should read it too.
6. Change the editor to build new node ids from the type's display name instead of its id (`tools/editor/src/pages/tree/state/treeActions.ts:42`). Also add a data test that every type id equals its display-name slug (except the kept ids listed above), so the ids don't drift again.
7. Tests:
   - A data test that every `old` is gone from the tree, every `new` is present, and no `new` equals a released or retired id.
   - A `ShipTrees` test showing a rename keeps sockets, charges, options, free and dormant state.
   - A template and option round trip using old option ids.
8. In the changelog, note the rename and that owned Doctrine Archives roll new rewrites once ("Save compatible.").

## Retiring the mapping

Ship both CSVs in the next release and keep them for at least one more. Then delete them and the rename branch in `NodeReplacements`; the `retired` list stays. A save first loaded after that treats renamed nodes as removed: OP and free allocations are refunded, item costs are not, and lost options fall back to the first option.
