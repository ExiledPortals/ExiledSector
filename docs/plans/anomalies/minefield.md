# Anomaly: Minefield

An old minefield sits somewhere in the system. Battles fought inside it start with Doom-style mines scattered across the arena, live for the whole fight and dangerous to both sides.

Status: proposed 2026-11-02 (community suggestion, from the Minelaying chart drawback discussion). Not built. See `overview.md` for the shared framework.

## How it plays

Mines are missiles, so ship AI on both sides steers around them; what they catch is anything too fast to brake or turn: fighters, burning frigates, reckless charges. Spread over the whole arena that makes positioning matter without being unavoidable damage. Neutral mines hit both sides, so the player can also bait enemies into them.

The minefield is a custom campaign terrain on `BaseTerrain`, a ring or patch (placeholder 2500 su radius) around a guarded spot: the boss, a station or a cluster of POIs, so the best loot sits inside it. It is drawn on the map and named in chart intel. A battle is in the minefield when the player fleet is inside the terrain as the engagement starts, recorded as Storm Front records its flag (`StormBattles.playerFront`, `StarChartLootSource.recordEngagement`).

The anomaly adds no fleets.

## Battles in the minefield

At battle start a combat plugin places mines across the arena (placeholder 40, scaled with battle size), clear of both deployment zones and at least vanilla's `MIN_SPAWN_DIST` from every ship and each other. Mines are spawned as `MineStrikeStats.spawnMine` does (`minelayer2` via `engine.spawnProjectile`), with no source ship, belonging to neither side. Vanilla mines time out after a few seconds, so the plugin keeps them alive by resetting their flight time on a one-second timer, and lays a replacement elsewhere a while after one goes off (placeholder 20 seconds), so the field thins slowly instead of staying full.

## Rewards

Battles in the minefield drop increased item quantity (placeholder +30%), added in `ChartFleetLoot` the way Storm Front's bonus is. Optional: minefield-only hull item modifiers with a mine and proximity theme, reserved at item load like Storm Front's.

## With other anomalies

Excludes Storm Front for the first version, since two battle-field anomalies around one spot would crowd it; revisit once both are tested. The Supply Convoy's route avoids the minefield.

## Chart modifiers

Tuning lines are chart effects carried by an atlas source, per `overview.md`:

- `CHART_MINEFIELD_MINE_COUNT_PERCENT`: increased number of mines in minefield battles.
- `CHART_MINEFIELD_LOOT_QUANTITY_PERCENT`: increased quantity of items dropped by battles in the minefield.

## Checks before building

1. Neutral ownership: whether a mine spawned with a null source can be given owner 100 (neutral) and still arm and hit both sides, or whether it needs a source ship and per-side mines instead.
2. Keeping mines alive: whether resetting flight time on `minelayer2` keeps it armed and fuse-ready, or whether a longer-lived custom projectile in `data/weapons/proj/` (copying `minelayer2`) is cleaner.
3. Foiler mods: no combat nebula tiles, terrain on `BaseTerrain`, textures preloaded (`../atlas.md`).

## Build order

1. [ ] Settle the checks above.
2. [ ] The anomaly class (`exiledsector.charts.anomaly.minefield`), its insertable `chart_anomalies.csv` row, the terrain plugin placed around the guarded spot, intel text, atlas marker icon (placeholder), save alias.
3. [ ] The in-minefield battle flag and `MinefieldBattlePlugin` (placement, keep-alive, replacement), placement in a static helper for tests.
4. [ ] The loot bonus in `ChartFleetLoot`; the chart modifier lines; strings (en and zh_CN draft); the `overview.md` table row; the `docs/docs.md` section; tests.
