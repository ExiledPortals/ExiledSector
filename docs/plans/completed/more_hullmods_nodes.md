# More HullMods nodes

PureTilt agreed on 2026-11-22 that Exiled Sector can use More HullMods' (MHM 1.13.0) designs and art for skill tree nodes, with a note that they come from MHM. Each of its 21 hull mods is a notable with its own wheel in the core volume, drawn with MHM's icons.

## Rules

- The nodes are always on the tree, tagged `core`. While MHM is installed, each node needs its MHM hull mod blueprint; without MHM, nothing blocks them.
- Each node adds its MHM hull mod as a phantom, which only exists while MHM is loaded.
- Every node credits More HullMods by PureTilt at the bottom of its F1 tooltip.
- The nodes do what MHM intends, not its bugs: Emergency Venting System's integrated bonus works, Unstable Shields' integrated swing is x0.6 to x1.2, Flux Booster scales with flux level without a stray +1, and Hardlight Focus doesn't log beam hits.
- An integrated node always does more of what the hull mod does and never removes a penalty. MHM s-mod bonuses that already do that are kept; Fuel Additive's (removes the supply penalty) and Missile Prefabricator's (brings reloading back) are replaced.
- Bombardment Package, Exploration Refit and Fuel Additive count toward the Logistics limit, as in MHM.
- Magnitudes start at MHM's numbers and are placeholders.

## Steps

1. Icons and credits. Done: circular copies in `graphics/exiledSector/tree/more_hullmods/mhm_*.png`, the `mhm_` prefix in the editor's `imageFolders.ts`, and a More HullMods section in `CREDITS.md`.
2. Effects. Done, in exiledsector-core `skills/skilleffect/`: `weapon` (`HIT_STRENGTH_PERCENT`, `DAMAGE_TO_SHIELDS_MULT`, `PROJECTILE_SPEED_PERCENT`, `HardlightFocusListener`, `MissilePrefabrication`), `flux` (`FluxOverdriveListener`, `FluxGeneratorListener`, `FluxBoosterListener`, `EmergencyVentingListener`), `shield` (`UnstableShieldsListener`), `defense` (`MIN_ARMOR_FRACTION_MULT`, `HullfoamListener`), `movement` (`HyperEngineListener`) and `logistics` (`FLEET_BOMBARD_COST_REDUCTION_FLAT`), with tests mirroring each package.
3. Gating, credit and compatibility. Done: a blueprint for a hull mod that isn't loaded never locks (`SkillTypeUnlockStatus`), the hull mod credit rule (`ModCredit.MORE_HULLMODS`), `MoreHullModsCompat` registered in `SkillTreeDomain`, and the Ground Support conflict row in `hullmod_conflicts.csv`.
4. Skill types and strings. Done: 21 notables in `data/skilltrees/skill_types.json`, with zh_CN names and flavour in the skill tree catalogue.
5. Layout. Done in `data/skilltrees/ship_skill_tree.json`: 20 wheels outward off `outer_ring`, the `ice_ring` highway at radius 7000 with its `ice_ring_belt` decoration, four spokes and four notable links between the rings, and Exploration Refit hanging inward off the ice ring near the logistics wheels.
6. Generated data and tests. Done: `tools/editor_vocabulary.json` and `descriptions.en.golden` regenerated; `SkillTypesDataConsistencyTest` checks that every blueprint names a known hull mod.
