# Integrated nodes

Integration replaces the node upgrade tiers and blueprints (removed; the old work is on the `node-tiers-backup` branch). The player spends story points to integrate a node on one ship, the way vanilla builds in an s-mod. An integrated node uses its integrated effects instead of its normal ones.

Status: built 2026-11-08. All magnitudes are placeholders.

## Rules

- Only node types with integrated effects can be integrated: every core notable and keystone that had a tier 3. Their normal effects are the old tier 2 and their integrated effects the old tier 3. Tier 1 is dropped, and blueprint-locked nodes go back to their vanilla blueprint lock.
- Integrating needs the node allocated. Clicking and holding it for 3 seconds integrates it, with the cost shown in the tooltip while held and the miniature circuit growing in. Releasing early cancels; a short click still deallocates as before.
- Integration is per ship and per node. A ship's first integration costs 1 story point, its second 2, its third 3 and so on. Unlike building in an s-mod, it grants no bonus XP, so a refund can't be farmed for XP.
- Deallocating or respeccing an integrated node keeps it integrated, so allocating it again restores the integrated effects for free. Integrated nodes still count toward the next cost.
- An integrated node draws the root node's circuit animation in miniature, in story point green, dimmed while unallocated. It grows in while the click is held.
- The tooltip shows the node's current effects. Integrated effects that differ from the normal ones are story point green, and so is a "No longer:" line for each normal line they drop. A node that isn't integrated shows its integrated effects and cost only in the F1 details, in their own section with dividers between the current effects and the other details.
- The Deintegration Schematic resets a ship's integrated nodes and refunds all their story points. It is used from a skill tree button beside the Logistics Schematic's and drops the same way, at the Tuning Nanoswarm's battle rate.

## NPCs

An NPC ship gets as many integrated nodes as it has s-mods, picked at random from its allocated integrable nodes, so fleet quality drives both the same way. A fleet not yet inflated rolls the count with vanilla's `DefaultFleetInflater.getMaxSMods` from its average s-mods. The integrated nodes go in the NPC tree tag. A recovered NPC hull loses them along with the rest of its NPC build.

## Data

A type lists its integrated effects as a full replacement of its normal ones:

```json
{ "id": "heavy_armor", "effects": [ { "effect": "MANEUVERABILITY_MULT", "magnitude": -20 } ],
  "hullSizeEffects": [ { "effect": "ARMOR_FLAT", "frigate": 150, "destroyer": 300, "cruiser": 400, "capitalShip": 500 } ],
  "integrated": { "hullSizeEffects": [ { "effect": "ARMOR_FLAT", "frigate": 150, "destroyer": 300, "cruiser": 400, "capitalShip": 500 } ] } }
```

`ShipSkillData.integratedNodeIds` holds a ship's integrated node ids; node id renames apply to it.

## Steps

1. Done. Restore the content of the tier work: the new effects, the Ballistic Rangefinder and Missile Autoloader replicas, Flux Coil Adjunct and Flux Distributor, the Shield Arc Multiplier removal and passthrough nodes applying their own effects.
2. Done. Data and model: the `integrated` block in `skill_types.json` (`IntegratedEffects`, `SkillType`, `SkillTypeLoader`), module sharing, and the editor's serialiser, validation and type form.
3. Done. Ship state and effects: `ShipSkillData.integratedNodeIds`, `IntegratedNodes`, `AllocatedSkillEffects`, `ShipTrees.renameNode` and `ShipTreeRepairs.forgetUnknownNodes`. Caches follow `ShipSkillData.revision`.
4. Done. Integrating: a held click (`IntegrationHold`, driven by `TreeAllocationSession` from `SkillTreeCanvasPlugin`), story points in `NodeIntegration`.
5. Done. Display: `IntegrationLines` in the node tooltip, the miniature circuit in `SkillTreeNodeDrawer` and story point green names in `ShipTreeSummaryRenderer`.
6. Done. NPCs: `NpcIntegratedNodes` from `NpcFleetLeveller`, the `integrated:` field of `NpcTreeTag`, cleared in `ShipTreeSync.adoptNpcTree`.
7. Done. The reset item: `DeintegrationSchematic`, its button `IntegrationResetButton` (sharing `SchematicButton` and `SchematicDialog` with `LogisticsSlots`), drops in `SkillTreeSchematicDrops` and `SkillTreeSchematicLootSource`, and the `ExiledSectorGrantDeintegrationSchematics` console command.
