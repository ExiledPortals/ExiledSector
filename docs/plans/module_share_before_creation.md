# Module share before creation: plan

A modular ship's tree reaches its modules only on the parent's first combat frame (`ModuleTreeShare`), so the vanilla refit module tabs, and any other view of a module's own stats, never show the share. A player reported that Blast Doors, Reinforced Bulkheads and the caps and vents trios "give modules nothing" for this reason. Applying the share after creation also forces `ModuleBoost` to rescale hull, armor cells, shield arc and ammo by hand, and to clamp carried armor so it isn't raised twice.

This plan applies the share the way the parent gets its own tree: through a hull mod on the module variant, before the module is created. The engine then builds hull, armor, arc and ammo from the boosted stats, and every view of the module, refit tabs included, shows them. It was the intended design for modules from the start.

Status: not started.

## Engine facts

Decompiled from 0.98a (see the memory note on module creation):

- Combat modules are built by `loading.specs._.o00000(Ship parent, Ship)` after the parent ship exists. Each gets a new `FleetMember(owner, parentMember.getVariant().getModuleVariant(slotId), SHIP)` named "temp", with a fresh id and no fleet data. Its hull mods run during creation; `setParentStation` and `setStationSlot` come only afterwards. Inside a module's `applyEffectsBeforeShipCreation`, only the module variant (and its tags) links to the parent.
- The module variant comes from the parent member's variant, not the ship's, so links must sit on `member.getVariant()`'s module variants.
- The refit simulator copies the parent member (same id, same fleet data, the variant being edited), enters combat and builds modules the same way.
- Refit module tabs use a stand-in member that has the parent fleet's fleet data but isn't in its member list (`RefitModuleTabs.isModuleTab`). The refit UI keeps the parent internally, with no API access.
- `getModuleVariant(slot)` returns the shared global stock variant when the parent variant has no module variant of its own for that slot. `setModuleVariant` silently ignores a stock-source variant, so a module variant must be cloned and set to REFIT source before it's stored.
- `ShipVariantAPI.clone()` deep-copies module variants and their tags.
- Hull mods on a module variant run both in refit and in combat (ARMAA docked ships already rely on this with `exiledSector_core`).

## Design

### Module link

Every module variant of a parent that carries `exiledSector_core` gets:

- the perma hull mod `exiledSector_module` (hidden in `hull_mods.csv`, core, `ModuleHullMod`), and
- the variant tag `exiledSector_moduleOf_<parentMemberId>`.

A core helper `ModuleLinks.sync(member, variant)` adds both. It clones any stock module variant to REFIT source first and stores it with `setModuleVariant`, and fixes a stale parent id. It's called from `SkillTreeHullMod.applyResolved` outside the OP cost pass, for `member.getVariant()` and, when different, the variant being edited. That one call site covers player ships, NPC trees, stations and chart enemies, because they all go through the core hull mod already, and it runs before modules are built in combat. It never edits a stock-source parent variant; those have no tree to share.

A variant with its own tree (`ShipRecords.taggedTree`, an ARMAA docked ship) gets no link. `ModuleLinks` also removes a link tag from a variant that is a fleet member's own variant, so a docked ship that was ejected doesn't keep borrowing its old parent.

### Finding the parent

`ModuleParents.find(moduleStats)` reads the link tag, then:

- in combat (a current `CombatEngineAPI`): reads a registry in the engine's custom data, keyed by parent member id. `SkillTreeHullMod.applyResolved` fills it with the parent's `ShipEffectContext` and `ResolvedTree` whenever it runs in combat. Combat parents are always created before their modules, and the simulator copy shares the real member's id.
- outside combat (refit module tabs): looks the id up in the stand-in member's fleet data and resolves that member's tree with `SkillTreeHullMod.resolvedTree(member, member.getVariant())`.
- otherwise (codex, a module with no parent around): nothing, so no share.

### Applying the share

`ShipEffectSource` gets a module hook, `moduleContributions(ShipEffectContext parentContext, ResolvedTree parentTree, ShipVariantAPI moduleVariant)`, returning permanent effect totals. Domains provide the module content, so core doesn't know about trees or charts:

- `SkillTreeEffectSource` returns today's `ModuleTreeShare.sharedTotals` times the module share setting (station or not), plus the parent's `MODULE_HULL_PERCENT` and `MODULE_ARMOR_PERCENT` at full strength, read from the parent tree's totals rather than the parent ship's dynamic stats.
- `ChartEnemyShipEffects` returns its granted stat multipliers and nodes granted without a tree, which `ChartEnemyModuleGrant` applies today, and adds its damage listener after creation.

`ModuleHullMod.applyEffectsBeforeShipCreation` applies the totals in the two passes the parent uses (effects that don't apply after other effects first, then those that do). `applyEffectsAfterShipCreation` calls `applyAfterShipCreation` for each shared effect and the domains' module after-creation hooks. Mod ids keep the `exiledSector_moduleShare_<parentId>_` prefix so existing tests and readers still match.

### What goes away

- `ModuleTreeShare` as a combat listener (`attach`, `advance`, the `shared` flag) and its call in `SkillTreeEffectSource.afterShipCreation`. Its static `sharedTotals` and `affinityBonusRatios` stay, moved next to the skill tree module hook.
- `ModuleBoost.boost` with its hull, armor, arc and ammo rescaling and the carried-armor clamp. The engine now builds those from the stats, and carries module hull and armor fractions between engagements on its own.
- `ModuleShare.SHARE_REBUILD`, which only existed for the after-creation rebuild; those effects become `SHARE`.
- `ChartEnemyModuleGrant` and `ChartEnemyShipEffects.applyToModule`.
- The `MODULE_*` dynamic stats on the parent, if nothing else reads them after the change.

## Steps

1. Verify the engine order before building: decompile the deploy path (`CombatFleetManager`) and the simulator path (`coreui.refit.Q`) to confirm the parent's `applyEffectsBeforeShipCreation` runs before `_.o00000(Ship, Ship)` builds its modules, and confirm the refit module tab builds its ship from a clone of the parent member's module variant (so the link tag is there). Record the answer in the engine facts above.
2. Add `exiledSector_module` (`ModuleHullMod`, hidden) and `ModuleLinks`, called from `SkillTreeHullMod.applyResolved`. Check the chart-enemy and NPC install paths (`ChartEnemyShipEffects` around line 76, `NpcFleetLeveller.ownedVariant`) hand `applyResolved` an owned, non-stock parent variant.
3. Add `ModuleParents` with the combat registry and the refit fleet lookup.
4. Add the `moduleContributions` hook and its after-creation counterpart to `ShipEffectSource`, and implement them in `SkillTreeEffectSource` (share plus `MODULE_*`) and `ChartEnemyShipEffects` (grants and damage listener).
5. Remove the combat listener path: `ModuleTreeShare` listener, `ModuleBoost.boost` and rescaling, `ChartEnemyModuleGrant`, `SHARE_REBUILD`. Check each former `SHARE_REBUILD` effect's `applyAfterShipCreation` behaves on a module built with the bonus already in its stats (ship mass, small PD reclassification, low-base armor).
6. Domain switches: with the skill tree domain off, `moduleContributions` returns nothing from it and modules build at their plain stats. Links stay on the variants, which is harmless (the hull mod does nothing without a parent tree).
7. Docs: rewrite "Module effects" in `docs/docs.md` (applied before creation through `exiledSector_module`, found through the link tag; at most two short paragraphs). No README or forum post change, since the share itself is unchanged.

## Decided

- Modules get the share before creation through a hull mod on their variant, replacing the first-frame listener everywhere: player, NPC, station and chart-enemy parents, in refit and in combat (2026-11-06).

## Open

- Whether the stat page should list the modules' resulting numbers now that their stats carry the share. Not needed for this plan.

## Tests

Each test goes in the lowest module that sees every class it uses.

- `ModuleLinks.sync` adds the hull mod and tag to every module slot, clones a stock module variant to REFIT before storing it, fixes a stale parent id, skips a variant with its own tree, and leaves a stock-source parent alone.
- `ModuleParents.find` returns the registered parent in combat, the fleet data member outside combat, and nothing with no tag or no parent.
- `ModuleHullMod` applies the shared totals at the module share (50%, 100% for a station parent) and the `MODULE_*` bonus at full, in both passes, and nothing for a docked ship with its own tree. Port the existing `ModuleTreeShareTest` cases on affinity ratios, own hull mod copies and damage-taken caps.
- Charts: a chart enemy's module gets the granted multipliers, the nodes granted without a tree and the damage listener.
- `ModuleSharesTest` passes with `SHARE_REBUILD` folded into `SHARE`.
- Remove `ModuleBoostTest`'s rescaling cases with the code they cover.
