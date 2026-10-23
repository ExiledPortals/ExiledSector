# Chart expansion: Excavation Sites

Buried sites on the chart's planets offer a visible trade before you dig: a reward, and a way digging makes the boss stronger. You pick what to dig, so your greed decides how hard the one big fight is. Modelled on Path of Exile's Expedition.

Status: planned, nothing built. See `overview.md` for the shared framework.

## How it plays

Only rolls on tiers that have a boss (`boss` column in `star_chart_tiers.csv`, every tier by default). The chart marks two to five planets with anomalous readings. A site's trade stays hidden until the planet is surveyed, either a full survey or Remote Survey, and is then shown in the planet's dialog and the chart's intel, for example:

- "1 additional rare hull item; Star Chart Boss has 15% increased fleet strength"
- "2 additional currency items; Star Chart Boss starts battles with every objective captured"
- "Contains a Wormhole Anchor; Star Chart Boss officers are 2 levels higher"

Digging costs supplies, heavy machinery, crew and days, scaled by tier. The reward joins the boss's spoils, since the dig wakes the boss and it takes the find on board, so it's claimed only by winning that fight. Each dig's drawback is applied to the boss as you dig, so the boss's tooltip always shows what you've built.

A story point rerolls one site's trade before you dig.

## Trades

Rewards and drawbacks are separate lists, so they combine freely. Drawbacks reuse `ChartEffect` lines (fleet strength, quality, officer level, damage taken, deployment cost, objective capture) scoped to the boss fleet only. Rewards are loot budget for a `DropType`, extra rarity, or a fixed item. Both lists live in `data/config/exiledSector/chart_excavation.csv` (`id,kind,lines,weight,min_tier,note`), with drawback strength rising with the reward's size.

## Chart modifiers

- `CHART_EXCAVATION_CHANCE`: Area contains Excavation Sites.
- `CHART_EXTRA_EXCAVATION_SITES`: +N to excavation sites.
- `CHART_EXCAVATION_REWARD_PERCENT`: increased excavation rewards.
- `CHART_EXCAVATION_COST_PERCENT`: reduced dig costs.

## Build order

1. [ ] `chart_excavation.csv` and the trade roller.
2. [ ] Site markers on planets, hidden until survey; dig dialog and costs.
3. [ ] Boss-scoped `ChartEffect` lines so drawbacks touch only the boss.
4. [ ] Dig rewards added to boss spoils.
5. [ ] Story point reroll, chart modifier lines, strings.

## Open questions

- If the player digs and then leaves the chart without fighting, the rewards are lost. Is that clear enough from the dig dialog alone?
