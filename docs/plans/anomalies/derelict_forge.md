# Anomaly: Derelict Forge

A Domain nanoforge the lockdown missed is still running on a derelict station in the system. It can do precise work the crew's nanoswarms can't: add a modifier of a chosen kind, remove a chosen modifier, perfect one. Each forge offers a few programs, and running one costs metals and rare metals, which the system itself helps you gather. There are no fights.

Status: planned, nothing built. Proposed 2026-10-28. See `overview.md` for the shared framework.

## Lore

The crews reprogram nanoswarms because the Domain's licensed nanoforges are locked down. This one never got the lockdown signal. It still takes raw stock and runs its programs, and a crew that feeds it can direct a single, exact change to a hull item or subroutine.

## How it plays

The chart places the forge station on a clear orbit (`AnomalyContentSite.clearOrbit`). Interacting with it opens a dialog of ours listing the forge's programs, each with its cost. Choosing one opens a Forge mode on the Modification Bench to pick the item, and the modifier where the program needs one. The forge runs one program per run (placeholder), after which it goes dark.

While the forge is in the system, the chart's sites lean toward resources (`DropType.RESOURCE`, placeholder +100%), so a player who sweeps the system can usually fuel it from what they find.

## Programs

Each forge rolls three programs (placeholder) from `data/config/exiledSector/chart_forge_programs.csv` (`id,weight,min_tier,item_types,metals,rare_metals,note`) on the chart seed. Starting rows, all placeholders:

| Program | What it does |
|---|---|
| Graft | Adds a modifier whose effect belongs to a chosen ship part (the subpackage of `exiledlib.skills.skilleffect` the line's effect lives in: shield, flux, weapon and so on) to an item with room for one. |
| Excise | Removes a chosen modifier. |
| Perfect | Sets a chosen modifier's values to the top of its range. |
| Anchor reroll | Rerolls every modifier but a chosen one. |
| Retemper | Rerolls a hull item's implicit values, keeping the better roll of the old and the new for each line. |

Costs scale with tier (placeholder). Items the currency can't touch are refused in the same way (`ItemCrafting.isLockedFromCurrency`, corrupted and mirrored items), and an immutable modifier can't be excised, rerolled or replaced. Prefix and Suffix Inhibitors don't apply, because every program picks exactly what it touches.

## Rewards

The program is the reward. The forge drops no loot of its own.

## Chart modifiers

Tuning lines are chart effects carried by an atlas source, per `overview.md`:

- `CHART_FORGE_PROGRAMS`: +N programs offered by the Derelict Forge.
- `CHART_FORGE_RUNS`: +N programs the Derelict Forge can run.
- `CHART_FORGE_COST_PERCENT`: reduced Derelict Forge program costs.

## Build order

1. [ ] The anomaly class (`exiledsector.charts.anomaly.forge`) and its insertable `chart_anomalies.csv` row; the forge station on a clear orbit, its rolled programs and runs left in `ChartRunState`; the resource leaning on the system's sites.
2. [ ] `chart_forge_programs.csv` and its loader; each program as an operation on `Item` next to `ItemCrafting`, sharing its refusal rules, with tests per program.
3. [ ] The forge dialog through `ChartCampaignPlugin.pickInteractionDialogPlugin`, and the Forge bench mode registered through `StarChartsDomain.benchModes` (item picker, modifier picker, cost and a confirmation).
4. [ ] Chart modifier lines, strings (en and zh_CN), the atlas marker icon, the `docs/docs.md` section and tests.
5. [ ] The Forge Survey catalyst (`atlas_catalysts.md`): the `atlas_catalysts.csv` row (survey family), its `special_items.csv` row and name strings (en and zh_CN); it loads once the anomaly has its icon.

## Open questions

- How Graft groups modifiers by part: through the effect's registry entry in `SkillEffectRegistry`, or a `part` column on `item_modifiers.csv`. Lines whose effects span parts need a rule.
- Whether Star Charts can be forged too, and which programs make sense for them.
- Whether programs that pick a modifier are too strong against Reshaping and Tuning Nanoswarms, and need a higher cost or fewer runs at low tiers.
- Whether some programs should cost Nanite Feedstock as well as metals.
