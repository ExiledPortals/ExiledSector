# Chart expansion: Asteroid Stream

An Industrial Evolution meteor swarm sweeps through the chart's system along a path marked in advance. Some asteroids in it are laden with chart loot, and you break them apart in the campaign layer with Industrial Evolution's missiles before the swarm carries them away. There's no battle.

Only rolls while Industrial Evolution (`IndEvo`) is installed and its meteor swarms are switched on (its `IndEvo_meteor_enabled` LunaLib setting), through `requires_mod` in `chart_expansions.csv` like the Arsenal Station POI.

Status: planned, nothing built. See `overview.md` for the shared framework.

## How it plays

Some days after the chart opens, a swarm arrives on a path that crosses at least one orbit with POIs. Its path shows on the map from the start of the run as Industrial Evolution's warning trajectory, so you can be waiting in the right place. Impacts, visuals and despawning are Industrial Evolution's own.

The swarm's treasure asteroids hold chart loot instead of their usual contents: ore, rare ore, volatiles, or a buried cache (chart loot budget), with bigger ones holding more. You break them with Industrial Evolution's campaign missiles as usual, and the contents drop as cargo.

Independent mining fleets may follow the swarm too. They're neutral, crack the laden asteroids they reach first, and leave with the contents, so the swarm is a race, not a fight.

## Chart modifiers

- `CHART_ASTEROID_STREAM_CHANCE`: Area contains an Asteroid Stream.
- `CHART_LADEN_ASTEROIDS_PERCENT`: increased laden asteroid quantity.
- `CHART_STREAM_MINERS`: Rival miners follow the stream.

## Build order

1. [ ] Read Industrial Evolution's meteor code (`indevo.exploration.meteor`: `MeteorSwarmManager`, the spawners, the treasure asteroid entities) and decide how to spawn a swarm in a chart system and swap its contents.
2. [ ] Spawn the swarm when the run starts, timed and aimed across the chart's POIs.
3. [ ] Chart loot in laden asteroids.
4. [ ] Rival miner fleets.
5. [ ] Chart modifier lines, strings, and an Industrial Evolution entry in the compat self-checks.

## Open questions

- Whether to call Industrial Evolution's classes directly or through reflection. A direct call needs its jar on the build path and a version check, like the other compat targets.
- Industrial Evolution's swarm cooldowns and location timeouts must not block chart swarms, and chart swarms must not reset its cooldowns for the real sector.
- Every swarm entity has to go when the chart closes, so the chart leaves no save trace.
