# Chart expansion: Supply Convoy

An enemy convoy runs a fixed route through the system on a schedule you can see, carrying a known cargo. Where you catch it is the gameplay: strike where the escorts are spread out and you only fight part of them, or follow it dark to its drop point and take the cargo without a fight.

Status: planned, nothing built. See `overview.md` for the shared framework.

## How it plays

The convoy is a freighter fleet with escort fleets travelling with it: a screen ahead and a close escort. It enters at the system edge, visits two to four waypoints (planets, stations) and reaches a drop point, where it unloads part of its cargo into a cache and leaves the system.

Its route, its timing and its manifest (for example "1-2 subroutines, currency items") show in intel from the start of the run, so you can plan the strike.

The escorts keep formation in open space and fall out of it in terrain: in asteroid fields and nebulae the fleets slow by different amounts and drift apart. Hit the freighters while the escorts are too far away to join the battle and you only fight the freighters and whatever's close. Interdiction Pulse holds fleets in place to open a gap. The freighters flee once attacked and drop cargo pods as they take losses.

The other way: shadow the convoy dark to its drop point and loot the cache once it's gone. If an escort spots you while you're shadowing, the convoy skips its drop and runs for the system edge.

## Rewards

The cargo is chart loot budget split between the freighters (dropped as pods, scaled by how many die) and the drop cache. Catching it before the drop gets more of the cargo, and waiting gets the cache without a fight.

## Chart modifiers

- `CHART_CONVOY_CHANCE`: Area contains a Supply Convoy.
- `CHART_CONVOY_CARGO_PERCENT`: increased convoy cargo.
- `CHART_CONVOY_ESCORT_PERCENT`: increased or reduced convoy escort strength.
- `CHART_EXTRA_CONVOYS`: +N to convoys.

## Build order

1. [ ] Convoy fleets, route planning through waypoints, intel with route and manifest.
2. [ ] Escort formation and terrain separation.
3. [ ] Freighter flight and cargo pod drops; split loot budget.
4. [ ] Drop point cache, shadowing detection, the convoy fleeing.
5. [ ] Chart modifier lines, strings.

## Open questions

- Vanilla pulls nearby fleets into a battle by distance; confirm that range so escort separation can be tuned against it.
