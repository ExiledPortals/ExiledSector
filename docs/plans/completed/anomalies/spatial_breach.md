# Anomaly: Spatial Breach

A tear into hyperspace opens somewhere in the system and grows each day. Inside, the fleet burns fuel, uses more supplies and sees less, but breach-touched caches form there that exist only while it's open. Reaching its core closes it for a large reward. There are no fights: the pressure is time and logistics.

Status: built and tested 2026-10-23, commit bd01ea16 on parallel-work, ahead of the atlas (see `docs/plans/anomalies/overview.md` for what the framework still lacks). Not yet checked in game; the visuals especially need a look.

## How it plays

The breach is a custom terrain field that starts small and grows each day to a maximum radius, then stops. The chart itself still has no timer; the breach is the only thing that changes with time, and it never forces you out. Inside it:

- fuel use is increased;
- each ship in your fleet uses more supplies per day (changed from a CR drain on 2026-10-24: draining CR wasn't fun);
- sensor range and burn level are reduced.

All of these get stronger toward the core. Visuals borrow from vanilla hyperspace storms and abyssal hyperspace.

## Breach caches and the core

Breach caches form inside the field as it grows, more of them deeper in and of higher rarity there. A normal POI the field grows over becomes breach-touched: it keeps its loot and gains extra rarity, but now sits in the harsh zone.

The core is an entity at the centre. Interacting with it closes the breach: the field collapses, its drains end, and the core drops a reward scaled by how large the breach had grown. Caches it hasn't reached yet never form, so closing early is safer but smaller.

## Chart modifiers

- `CHART_BREACH_CHANCE`: Area contains a Spatial Breach.
- `CHART_BREACH_GROWTH_PERCENT`: increased breach growth rate.
- `CHART_BREACH_CACHE_PERCENT`: increased breach cache quantity.
- `CHART_BREACH_CORE_ITEM_PERCENT`: increased quantity of items found in the breach core (renamed from `CORE_REWARD`, since tooltips never say "reward" for found items).

The single line is `CHART_BREACH_CHANCE` at 100. The three tuning lines only roll as hybrids that also carry `CHART_BREACH_CHANCE` (`HYBRID_CHART_BREACH_GROWTH` prefix, `HYBRID_CHART_BREACH_CACHES` and `HYBRID_CHART_BREACH_CORE` suffixes), so a tuning line never sits on a chart without a breach.

## Build order

1. [x] Breach terrain plugin with growth and the fuel, CR, sensor and burn effects (`BreachTerrainPlugin`, `BreachRules`). Sensor and burn apply to every fleet; the supply increase (a `BreachSupplyBuff` on each ship, refreshed each tick) and the fuel drain apply only to the player fleet.
2. [x] Breach cache spawning by depth and breach-touched POIs (`ChartSites.addCache`, rarity through `LootSource.modifiersFor`).
3. [x] Core entity, closing, core reward (`BreachCoreDialog`, salvage from the station loot tables plus items from the `exiledSector_chart_breach_core` salvage rule).
4. [x] Visuals (reworked 2026-10-24 on parallel-work, after RAT's Primordial Sea): the field is a pitch-black tear (`BreachFieldRenderer`, TERRAIN_1, under planets and fleets) with dark cracks from vanilla `emp_arcs` fading outward and a near-black aurora rim (the doctrine socket belt, `AuroraBeltRenderer.renderShaded`). The core is vanilla's black hole look: a black centre, a pink `star_halo` corona and a spinning `wormhole_ring`. The map still draws a tinted `fx_radial` glow. Later the black backdrop becomes the atlas hyperspace view, clipped to the tear. Not yet checked in game.
5. [x] Chart modifier lines, strings (en and zh_CN).
6. [x] Timed lifecycle (2026-10-25, after playing it): the breach starts closed at its fixed spot, opens once the player fleet flies over the core, eases out to full size over 60 seconds, then collapses over 60 seconds, dropping caches as the edge passes them and removing its core at the end (`BreachRules.Growth`, `BreachTerrainPlugin.elapse`). The core reward uses the largest size reached. This replaces the daily growth above.

## Open questions

- Growth and drains should update on a slow timer, not per frame. Done: every 0.1 days.
