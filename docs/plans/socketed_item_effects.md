# Socketed item effects as skill effects

Hull item effects become something skill effects can act on. Today the Extended Warranty Contract's doubling is a special case inside `HullUpgradeEffectSource`; nothing else can reach it. This plan turns "change the effect of socketed items" into ordinary skill effects that any source can carry: tree nodes, hull items, subroutines or another mod.

## Design

Exiled Lib gets a marker for socketed items and one resolution step. An `EffectContribution` whose origin implements `SocketedItem` is a socketed item. The interface reports the slot the item sits in and, for items that target another slot, that slot. Lib knows nothing else about hull items, so the item framework stays in the mod, and another mod can mark its own contributions the same way.

`SocketedItemScaling.withScaledItems` runs first in `EffectTotals.of`, before the affinity step, so items that grant affinity are scaled before affinity is counted. It gathers every item-scaling effect from all contributions, then multiplies each socketed item's magnitudes by 1 + the summed percent that applies to it. Scaling effects are never scaled themselves, so two scaling items can't feed each other.

The effects live in `special`, backed by a `SocketedItemScaling` record with a scope, the way `AffinityScaling` backs the affinity effects:

| Effect | Scales | Example |
|---|---|---|
| `SOCKETED_HULL_ITEM_EFFECT_PERCENT` | every socketed hull item | a notable granting increased effect of hull items |
| `TARGETED_HULL_ITEM_EFFECT_PERCENT` | the hull item in the source item's target slot; inert on nodes | the Extended Warranty Contract at 100 |
| `SOCKETED_HULL_ITEM_DRAWBACK_EFFECT_PERCENT` | only the drawback lines of socketed hull items | a node with reduced effect of hull item drawbacks |

A line is a drawback when its magnitude works against the player: negative on a normal effect, positive on a `lowerIsBetter` one.

On the hull item side, `HullUpgradeEffectSource` gives each section's contribution a `HullItemOrigin` that implements `SocketedItem`, and the hard-coded doubling goes. `HullItem` rolls a target section whenever one of its modifiers carries an effect that targets a socketed item (`SkillEffect.targetsSocketedItem()`), instead of checking the `doubler` mechanic. The stored target, the Tuning Nanoswarm reroll and the target tooltip line stay. The Extended Warranty Contract carries `TARGETED_HULL_ITEM_EFFECT_PERCENT` at 100 as a normal modifier, so it shows in the editor like any other effect, and the `doubler` mechanic is removed.

## Decisions

- Percents from every source add up before they are applied. Two Warranties on one target give 200% increased effect, so triple strength.
- The general and targeted effects scale drawbacks too; the drawback effect is the way to treat them apart.
- With Hull Upgrades switched off there are no socketed hull items, so the effects do nothing. Nodes that grant them still show their line.
- Only permanent contributions grant scaling; temporary and gated ones are ignored, and scaling never takes an item below zero or flips a line's sign.
- The ship summary totals the tree and section contributions through `EffectTotals.of`, so it shows every scaling source.

## Stages

1. [x] Framework and Warranty migration: `SocketedItem`, `SocketedItemScaling`, the three effects with en and zh lines, the `EffectTotals.of` step, `HullItemOrigin`, target rolling from effects, the Warranty's modifier, removing the `doubler` mechanic from code, data, editor vocabulary and editor tests, and the `docs/docs.md` section. Built: `exiledlib.skills.SocketedItem` and `SocketedItemScaling` (backing and resolution step), the effects in `SpecialSkillEffect`, `SkillEffect.socketedItemScaling()` and `targetsSocketedItem()`, `HullItemOrigin` from `HullUpgradeEffectSource`, `HullItem.targetsAnotherItem()`, the Warranty's `TARGETED_HULL_ITEM_EFFECT_PERCENT_EXTENDED_WARRANTY` modifier. Tests in `SocketedItemScalingTest`, `ShipSectionsTest` and `HullUpgradeTest` (same expected values as the old doubler), and the editor's `models.test.ts`.
2. [x] Per-section effects such as increased effect of Shield Generator items. Sections are mod data and the effect registry is a fixed list, so the mod passes its section ids to Exiled Lib at start-up and the scoped effects are generated the way `ScopedWeaponEffect` generates weapon scopes. Built: `SlotItemEffect` in `special` (named `<SECTION>_ITEM_EFFECT_PERCENT`, backed by the `SLOT` scope of `SocketedItemScaling`, sharing the `effect.SLOT_ITEM_EFFECT_PERCENT` lines), registered by the `HullUpgradesDomain` constructor; `SkillEffectRegistry` falls back to it for lookups and name lists. Tests in `SocketedItemScalingTest` and `ShipSectionsTest`.
3. [ ] Subroutines as socketed items. Most subroutine lines grant or gate nodes rather than carry magnitudes, so this needs its own design pass first.
4. [ ] Content: nodes and uniques that use the effects.
