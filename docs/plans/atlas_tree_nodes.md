# Atlas Tree nodes

Ideas for Atlas Tree node types. Every type listed under Built is in `data/skilltrees/atlas_tree_types.json`, ready to place on the tree in the editor's Atlas Tree page; none is placed yet. The tree's shape and rules are in `atlas_skill_tree.md`. Magnitudes and icons are placeholders.

## Travel nodes

Small nodes that make up the highways between volumes. They change with distance from the root.

### Built

| Distance | Type id | Name | Effect |
|---|---|---|---|
| Near | `travel_boss_star_charts` | Boss Star Charts | 2% increased quantity of Star Charts dropped by Star Chart Bosses (`CHART_BOSS_STAR_CHART_PERCENT`) |
| Further | `travel_catalysts` | Atlas Catalysts | 2% increased quantity of Atlas Catalysts found in the system (`CHART_CATALYST_PERCENT`) |
| Very far | `travel_chart_modifier_effect` | Star Chart Modifier Effect | 2% increased effect of Star Chart modifiers (`CHART_MODIFIER_EFFECT_PERCENT`) |

Increased effect of Star Chart modifiers scales every implicit and explicit modifier on a chart once it is opened, so 10% increased shield damage taken becomes 15% with 50% increased effect. Each prefix's hidden item quantity bonus is scaled too, so a prefix is a downside plus a loot bonus that grow together. The bonus still shows only in the totals at the top of the chart tooltip.

### Ideas
