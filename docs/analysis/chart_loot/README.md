# Chart loot estimates

Average drops per Star Chart run by tier, modelled on 2026-11-03 from the data and loot code as of commit a22099f9.
They are estimates for balancing, not measurements.

- `chart_hull_items.csv`: basic hull items, plus unique hull items with the best modifiers.
- `chart_crafting_items.csv`: nanoswarms, inhibitors, ciphers and feedstock. The per-item columns are for the six random modifiers case.
- `chart_catalysts.csv`: Atlas Catalysts.
- `chart_loot_model.js`: the model that writes all three; run `node docs/analysis/chart_loot/chart_loot_model.js` (about a minute).
  Its tier table, drop rates, site rows and modifier rolls are copied from the data files, so update them there first when those change.

Columns:

- `none`: a chart with no modifiers.
- `six_random`: three prefixes and three suffixes that do nothing for loot, so only each prefix's item quantity counts (5%, 15% or 25% by tier group).
- `best`: the six modifiers that drop the most of that table's items, each at the top of its range; `best_modifiers` names them.

Assumptions:

- Player and enemy fleets are about equal, so battle difficulty is 1x. Every fleet and the boss are destroyed and every site is salvaged.
- Enemy deployment points are 1.2x fleet points for patrols and 1.3x for the boss, from vanilla combat hulls.
- Each fleet is its own battle, the boss is a fleet, there is no anomaly, chart quality is 0 and the fleet limit setting is the default 14.
- Sites are planned by the same budget spending as `ChartContentPlanner`; vanilla salvage defenders' loot is left out.
- Hull items include the boss's guaranteed one and leave out Refit and Master Schematics.
