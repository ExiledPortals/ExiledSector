# Chart expansion: Spatial Breach

A tear into hyperspace opens somewhere in the system and grows each day. Inside, the fleet burns more fuel, slowly loses CR and sees less, but breach-touched caches form there that exist only while it's open. Reaching its core closes it for a large reward. There are no fights: the pressure is time and logistics.

Status: planned, nothing built. See `overview.md` for the shared framework.

## How it plays

The breach is a custom terrain field that starts small and grows each day to a maximum radius, then stops. The chart itself still has no timer; the breach is the only thing that changes with time, and it never forces you out. Inside it:

- fuel use is increased;
- CR drains while you're inside, the way a star's corona does;
- sensor range and burn level are reduced.

All of these get stronger toward the core. Visuals borrow from vanilla hyperspace storms and abyssal hyperspace.

## Breach caches and the core

Breach caches form inside the field as it grows, more of them deeper in and of higher rarity there. A normal POI the field grows over becomes breach-touched: it keeps its loot and gains extra rarity, but now sits in the harsh zone.

The core is an entity at the centre. Interacting with it closes the breach: the field collapses, its drains end, and the core drops a reward scaled by how large the breach had grown. Caches it hasn't reached yet never form, so closing early is safer but smaller.

## Chart modifiers

- `CHART_BREACH_CHANCE`: Area contains a Spatial Breach.
- `CHART_BREACH_GROWTH_PERCENT`: increased breach growth rate.
- `CHART_BREACH_CACHE_PERCENT`: increased breach cache quantity.
- `CHART_BREACH_CORE_REWARD_PERCENT`: increased breach core reward.

## Build order

1. [ ] Breach terrain plugin with growth and the fuel, CR, sensor and burn effects.
2. [ ] Breach cache spawning by depth and breach-touched POIs.
3. [ ] Core entity, closing, core reward.
4. [ ] Visuals.
5. [ ] Chart modifier lines, strings.

## Open questions

- Growth and drains should update on a slow timer, not per frame.
