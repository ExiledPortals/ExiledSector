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
3. [x] Subroutines as socketed items, as designed below. Built: `SocketedItem.category()` and the category on `SocketedItemScaling`, `SOCKETED_SUBROUTINE_EFFECT_PERCENT` and `SOCKETED_SUBROUTINE_DRAWBACK_EFFECT_PERCENT` in `SpecialSkillEffect`, `SubroutineOrigin` and `RadiusLineOrigin` in `exiledsector.items`, `AllocatedSkillEffects.nodeParts`, the split in `SkillTreeEffectSource.contributions` and the folding in `nodeEffectsOf`, the gating socket on `GatedSubroutines.Gating`, scaled affinity in `AffinityTooltipLines` and `RootAffinityNumber`. Tests in `SocketedItemScalingTest`, `SubroutineScalingTest` and `SkillTreeEffectSourceTest`.
4. [ ] Content: nodes and uniques that use the effects.

## Stage 3: subroutines

### What counts as a subroutine's effect

A subroutine's effect is every magnitude it carries or adds: its own lines (explicits, implicits, the corruption implicit), the Root Conduit's affinity, the Empty Shell's affinity per hollow node, the lines an augmenting Doctrine Archive adds to nodes in its radius, and a gating line's strength. Nodes it grants or swaps in are not part of it: a Modspec Cache's notables, a transforming Doctrine Archive's replacement nodes, and the nodes a gate covers keep their own values. On a Modspec Cache or a transforming archive only the corruption implicit scales.

A gating line's value is the strength its gated nodes run at, so scaling the line scales them: the Phoenix Ash Archive's 3 becomes 4.5 at 50% increased effect. This is the one place the scaling reaches node effects, and only because the line itself is the multiplier.

### Effects

Each `SocketedItem` gets a `category()` ("hull_item" or "subroutine") and `SocketedItemScaling` a category, so hull item effects never scale subroutines and the other way round. Two new effects in `special`:

| Effect | Scales |
|---|---|
| `SOCKETED_SUBROUTINE_EFFECT_PERCENT` | every socketed subroutine's effect, as above |
| `SOCKETED_SUBROUTINE_DRAWBACK_EFFECT_PERCENT` | only its drawback lines (the Cracked Drive Governor's CR loss) |

No targeted subroutine effect: sockets are tree nodes, not fixed slots, so there is nothing stable to target. A catch-all "increased effect of socketed items" (category null) and per-kind effects such as increased effect of Doctrine Archives are cheap to add later and wait for stage 4 content that wants them.

### Wiring

Today a subroutine's lines are merged into its socket node's contribution (`AllocatedSkillEffects.appliedEffects`), and Doctrine lines into each covered node's, so the scaling step can't tell them apart. `SkillTreeEffectSource` splits them out:

- The socket node keeps its own type's effects. The subroutine's lines, conduit affinity and hollow affinity go in a second contribution whose origin is a `SubroutineOrigin(socket node)` implementing `SocketedItem`, with the socket node's scaling flags and a `subroutine_` mod id part.
- An augmenting archive's added lines go in a contribution per covered node with a `RadiusLineOrigin(covered node, archive's SubroutineOrigin)`, so they keep that node's flags (an unscaled logistics node's added lines stay unscaled). A vanilla hull mod node keeps its contribution with no effects.
- A gated node's contribution takes a `RadiusLineOrigin` too, pointing at the gating subroutine, so the scaling step multiplies its gated effects.
- `SkillTreeEffectSource.nodeEffects` folds both origins back into their node, so module sharing, phantom hull mods and the allocated list see the same nodes as before, now with scaled values.
- `AllocatedSkillEffects.treeEffects` and `forData` keep returning the merged, unscaled list for fit checks and eligibility, as hull items do today.

### Decisions

- Scaling never takes a line below zero or flips its sign, and only permanent contributions grant it, as for hull items. A gated contribution is scaled but never grants scaling.
- A subroutine can carry hull item scaling and a hull item subroutine scaling; both are gathered in the same single step, so neither feeds the other.
- Item tooltips keep showing the item's own values; the ship summary and stat panel show the scaled totals.
- NPC trees go through the same step, so NPC subroutines scale too.

### Steps

1. `SocketedItem.category()`, the category on `SocketedItemScaling`, the two effects with en and zh lines (子程序), lib tests.
2. `SubroutineOrigin` and the split in `SkillTreeEffectSource` and `AllocatedSkillEffects`; `nodeEffects` folding; tests that the stats are unchanged without scaling (station module share included) and that 100% doubles a unique's lines, the conduit's affinity and the hollow affinity.
3. `RadiusLineOrigin` for augmenting archives and gated nodes; tests for augment lines, the gating strength, and a Modspec Cache's notables staying unscaled.
4. `docs/docs.md`: a "Socketed item scaling" section covers both kinds; "Hull item mechanics" keeps targeting and the fix-up.
