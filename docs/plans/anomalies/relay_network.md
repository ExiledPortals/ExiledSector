# Anomaly: Relay Network

Dormant Domain-era relays sit around the system: a nav buoy, a comm relay and a sensor array. Waking them is campaign-layer work, and every relay you hold starts the chart's battles already captured on your side. The boss holds whatever you leave alone.

Status: planned, nothing built. See `overview.md` for the shared framework.

## How it plays

The chart places one of each relay type at spread-out orbits. Each relay starts dormant and owned by the chart's enemy faction. Each one wakes in its own way, using vanilla abilities:

- The nav buoy needs your fleet to hold station beside it for a few hours, moving slowly enough to sync.
- The comm relay needs your transponder on in its range, broadcasting the handshake. That raises your sensor profile while you do it.
- The sensor array needs an Active Sensor Burst fired in its range.

Each also costs supplies and heavy machinery through a short dialog. A relay near a guarded POI shares that POI's guard, so waking it may mean the fight you'd have had there anyway.

A woken relay behaves like a vanilla campaign objective owned by the player for the rest of the run, giving its usual in-system bonus. Waking all three also uncovers a hidden cache, rolled from `chart_poi.csv` with a bonus budget.

## In battle

`ChartBattlePlugin` already touches every battle objective. In any battle inside the chart, each objective matching a held relay type starts captured by the player, and each matching an enemy-held relay starts captured by the enemy. Where the battle map has no objective of that type, one is added. A Domain-era relay variant (a chart modifier) makes captured objectives give stronger bonuses.

## Chart modifiers

- `CHART_RELAY_NETWORK_CHANCE`: Area contains a Relay Network.
- `CHART_RELAY_COST_PERCENT`: reduced or increased supply and machinery cost to wake relays.
- `CHART_RELAY_DOMAIN_ERA`: Relays are Domain-era.
- `CHART_RELAY_CACHE_BUDGET_PERCENT`: increased budget for the network's hidden cache.

## Build order

1. [ ] Relay entities and their dormant/woken state in the run, using vanilla objective entity types.
2. [ ] Wake conditions per type (station keeping, transponder, sensor burst) and the cost dialog.
3. [ ] Battle hook: pre-captured and added objectives in `ChartBattlePlugin`.
4. [ ] Hidden cache on all three woken.
5. [ ] Chart modifier lines and strings.

## Open questions

- Confirm by decompiling that a battle objective's owner can be set at battle start, and how the battle creation plugin places objectives, before building step 3.
- Should enemy patrols ever retake a relay? Leaning no, since it would add fights.
