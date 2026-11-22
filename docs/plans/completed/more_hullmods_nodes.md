# More HullMods nodes

Status: completed 2026-11-23.

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
5. Layout. Done in `data/skilltrees/ship_skill_tree.json`. Every wheel has the Stabilized Shields shape: a ring node, a lead-in, then an arc round a small orbit anchor through a second lead-in to the notable. Twelve wheels hang outward off `outer_ring`. The `ice_ring` highway at radius 6000, as far out from `outer_ring` as that is from `middle_ring`, has its `ice_ring_belt` decoration and eight direct spokes to the outer ring at 0, 54, 90, 144, 180, 234, 270 and 324 degrees. The other nine wheels hang inward off it: Integrated Armor and Hullfoam Dispenser at the bottom between the Low Tech and Midline roots, Flux Overdrive at the top beside the outer flux wheels near High Tech.
7. Affinity wheels. Done: `low_tech_mastery`, `midline_mastery` and `high_tech_mastery` (+20 and 8% increased affinity, using the new `HIGH_TECH_AFFINITY_PERCENT` and `MIDLINE_AFFINITY_PERCENT` beside `LOW_TECH_AFFINITY_PERCENT` in `AffinitySteps`) hang inward off the ice ring at their roots' angles, 30, 150 and 270 degrees, each led in by two greater affinity nodes (`greater_low_tech_affinity` and `greater_midline_affinity` are new, +10 like `greater_high_tech_affinity`). The ice ring gains nodes at 30 and 150 degrees for them.
6. Generated data and tests. Done: `tools/editor_vocabulary.json` and `descriptions.en.golden` regenerated; `SkillTypesDataConsistencyTest` checks that every blueprint names a known hull mod.
