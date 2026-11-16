# Chart loot estimates

Average drops per Star Chart run by tier, modelled on 2026-11-16 from the data and loot code after the completion reward, the boss's
guaranteed hull items and the boss's chest were removed.
They are estimates for balancing, not measurements.

- `chart_hull_items.csv`: basic hull items, plus unique hull items with the best modifiers.
- `chart_crafting_items.csv`: nanoswarms, inhibitors, ciphers and feedstock. The per-item columns are for the six random modifiers case.
- `chart_catalysts.csv`: Atlas Catalysts.
- `chart_star_charts.csv`: Star Charts from fleets, the boss and salvage.
- `chart_loot_model.js`: the model that writes all four; run `node docs/analysis/chart_loot/chart_loot_model.js` (about a minute).
  Its tier table, drop rates, site rows and modifier rolls are copied from the data files, so update them there first when those change.
  Crafting items are read straight from `crafting_drops.csv`, the `crafting_item` row of `battle_loot.csv` and the `band_shift` column of
  `star_chart_tiers.csv`.
- `rarity_band_model.js`: crafting drops under `docs/plans/completed/rarity_drop_bands.md` against the drops before it, with the old
  `chart_materials.csv` amounts and `battle_loot.csv` crafting rows kept in the script and the chart tiers read from the data files; run
  `node docs/analysis/chart_loot/rarity_band_model.js`. It writes `rarity_band_tiers.csv` (totals at the planned rate, Augmentation, Corrupted, the unique band and
  Immutable, common share), `rarity_band_items.csv` (each item at tiers 1, 8 and 15) and `rarity_band_outside.csv` (a 300 DP fight at player level 1 and the level cap
  outside charts).

Columns:

- `none`: a chart with no modifiers.
- `six_random`: three prefixes and three suffixes that do nothing for loot, so only each prefix's item quantity counts (5%, 15% or 25% by tier group).
- `best`: the six modifiers that drop the most of that table's items, each at the top of its range; `best_modifiers` names them.

Assumptions:

- Player and enemy fleets are about equal, so battle difficulty is 1x. Every fleet and the boss are destroyed and every site is salvaged.
- Enemy deployment points are 1.2x fleet points for patrols and 1.3x for the boss, from vanilla combat hulls.
- Each fleet is its own battle, the boss is a fleet, there is no anomaly, chart quality is 0 and the fleet limit setting is the default 14.
- Sites are planned by the same budget spending as `ChartContentPlanner`; vanilla salvage defenders' loot is left out.
- Hull items leave out Refit and Master Schematics. The boss guarantees no hull item; its uniques drop by chance like any other.
- The boss drops from the shared pool by its deployment points like any fleet; the Hoard adds to the quantity of its own loot only.
- Star Charts count every chart dropped, whatever its tier, including the boss's one guaranteed chart.
