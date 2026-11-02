# Chart drawback: Minelaying (enemy ships seed Doom mines)

Each enemy ship in a chart battle has a chance every second to drop a Doom-style mine near itself. The battlefield around the enemy line fills with mines as the fight goes on.

Status: proposed 2026-11-02 (community suggestion). Not built.

## How mines behave

Vanilla mines (`MineStrikeStats.spawnMine`, `minelayer2`) are missiles, so ship AI on both sides already steers around them and raises shields. They catch what is too fast to brake or turn in time: fighters, frigates under burn drive or plasma jets, and anything charging in. So the line mostly punishes reckless approaches and fighter swarms, which is the intended feel, not unavoidable damage.

## Effect

One red-tier prefix line, `CHART_ENEMY_MINE_CHANCE_PERCENT` (`high=1`, name Minelaying): every second, each enemy ship has a 1% chance to lay a mine. Placeholder; with 30 enemy ships that is about 18 mines a minute.

A mine spawns at a random point 200 to 500 su from its ship (placeholder), rerolled up to a few times if the point is within vanilla's minimum distance (`MIN_SPAWN_DIST`, 110 for frigates) of any ship or another mine, and skipped if no point fits. It copies `spawnMine`: `engine.spawnProjectile(enemyShip, null, "minelayer2", ...)`, `applyDamageModifiersToSpawnedProjectileWithNullWeapon` so the ship's missile damage bonuses count, zero velocity, a short fade-in with the jitter and `mine_teleport` sound so the player sees it arrive, and vanilla's 5 second live time (`setFlightTime(getMaxFlightTime() - liveTime)`). Fighters, modules, hulks, phased ships and ships under 10% CR (placeholder) never lay mines.

## Where it lives

A battle plugin like Ionized: `ChartMinelayerPlugin` in `charts.effect`, registered in `data/config/settings.json` beside `ChartArcStrikePlugin`, on only in campaign battles (not simulator) while `ChartPlayerModifiers.enemyMineChance()` is above zero. To keep per-frame cost low it rolls once per second per eligible ship on a one-second timer, not every frame, and reuses one candidate list rebuilt on that timer (as `ChartMisfirePlugin.refresh` does). Failures switch the plugin off for the battle and log once.

## Checks before building

1. Whether a `minelayer2` explosion hurts ships of its own side. Doom's mines don't harm allies in vanilla play; confirm by decompiling the mine's on-hit and explosion path. If they do, enemy-laid mines catching enemy ships is acceptable and maybe fun, but should be known.
2. Whether AI avoidance holds up with dozens of mines on the field (path planning cost and AI freezing in place). Test a large fleet at 1% and at 3%.
3. Mine sprite and sound load: `minelayer2` belongs to the Doom's system; check it is loaded in every game (it is a vanilla projectile, so it should be).

## Build order

1. [ ] `CHART_ENEMY_MINE_CHANCE_PERCENT` in `ChartModifier`, `star_chart_modifiers.csv` row, the chance through `ChartGenContext`, `ChartPlayerModifiers` (set, clear) and `ChartRunScript`.
2. [ ] `ChartMinelayerPlugin` with the spawn rules above; spawn point picking in a static helper so it can be tested without an engine.
3. [ ] Strings (en and zh_CN draft) following `docs/tooltip_wording.md`, for example "Every second in the system, each enemy ship has a <bad>{value}%</bad> chance to lay a mine nearby".
4. [ ] Tests for the spawn point rules and eligibility; `docs/docs.md` "Chart system rules" sentence; in-game pass on the checks above.
