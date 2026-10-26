# Anomaly: Neutron Star fixes

The Neutron Star anomaly (`StarAnomalies.NEUTRON_STAR`) forces a single `star_neutron` primary and places a research station. Its pulsar beam and corona push fleets outward and drain CR, which causes three problems:

1. The player has too little to hide behind. Every Neutron Star chart needs at least 7 planets.
2. Enemy fleets get blown to the edge of the map and drained of CR, so the player can't find them or finish the chart. Every non-player fleet in a chart system must ignore the push and the CR loss.
3. Getting back to the inner wormhole against the beam is slow. The player should be able to Transverse Jump out of a chart, with the same result as choosing to leave at the inner wormhole.

Status: planned, nothing built. See `overview.md` for the shared framework.

## Engine facts

These were read from the 0.98a API source (`starfarer.api.zip`).

- Vanilla `StarSystemGenerator.setPulsarIfNeutron` replaces a neutron star's corona with `addCorona(star, 300, 3 wind, 0 flares, 3 CR loss)` and adds a `Terrain.PULSAR_BEAM` terrain (`PulsarBeamTerrainPlugin`). The method is `protected`, so `ChartStarSystemGenerator` can override it.
- In `PulsarBeamTerrainPlugin.applyEffect`, CR loss scales with `Stats.CORONA_EFFECT_MULT`, but no stat resists the push: the wind changes the fleet's velocity directly. `StarCoronaTerrainPlugin` works the same way. A stat can't give immunity, so we need our own terrain plugin.
- Planets do block the beam. The plugin's `RangeBlockerUtil` shortens it behind bodies within `bandWidthInEngine + 1000` of the star, so a planet only gives cover if it orbits inside that radius.
- `Misc.getPulsarFor` and `Misc.getCoronaFor` find terrain with `instanceof`, so subclasses still count as the system's pulsar and corona (which `hasPulsar`, nascent gravity well sizing and other mods rely on).
- Terrain ids map to plugin classes in `data/campaign/terrain.json`. A new id in our own `terrain.json` adds a terrain type without replacing vanilla's `pulsar_beam`.
- Transverse Jump is the ability `fracture_jump`, plugin `FractureJumpAbility`. When `level == 1` in a star system, it charges CR and fuel and then calls `doHyperspaceTransition` to a hyperspace token near `system.getLocation()`. Its cost helpers (`getCRCostMult`, `getNonReadyShips`, `computeFuelCost`) are `protected`. Charts currently force-disable it through `ChartBlockedAbilities.ALWAYS_BLOCKED`.
- The player gets Transverse Jump from the Galatia Academy questline, so some players won't have it. They still have the inner wormhole.
- Of the installed mods, only Knights of Ludd touches the jump abilities. It adds its own ids (`fracture_jump_elysia`, `fracture_jump_luna_sea`, `fracture_jump_pullsar`) and doesn't replace the `fracture_jump` row.

## 1. At least 7 planets

Add `minPlanets()` to `ChartAnomaly` (default 0). `NEUTRON_STAR` returns 7, held in a constant in `StarAnomalies` (a placeholder like other magnitudes).

`ChartStarSystemGenerator.addPlanetsAndTerrain` takes both paths: vanilla orbits and shape-set orbits. After either one, count the non-star `PlanetAPI` bodies. While there are fewer than `minPlanets()`, add more orbits with a planet-only pick (`pickTopCategory` or a context with terrain categories weighted 0), until the count is reached or the orbit limit is hit. If the limit is hit first, start again from the star's corona edge, in gaps between existing orbits, so the minimum always holds.

Most of those planets should orbit inside the pulsar's blocking radius (`bandWidthInEngine + 1000`), or they don't give cover. Check this against real `star_neutron` gen data, and lower the orbit spacing for this anomaly if the band is too narrow for 7 planets.

The research station is placed after the layout, so `clearOrbit` already avoids the new planets.

## 2. Non-player fleets ignore the star

Add `ChartPulsarBeamTerrainPlugin extends PulsarBeamTerrainPlugin` and `ChartCoronaTerrainPlugin extends StarCoronaTerrainPlugin` in the star charts module. Both override `applyEffect` and return straight away unless the entity is the player fleet (`fleet.isPlayerFleet()`). This covers everything else in the system: chart fleets, bosses, the Hunter, convoys and skirmish factions, and allied escorts too. The player still feels the full effect, and the beam still looks and sounds the same.

Register both under new ids in `data/campaign/terrain.json` (for example `exiledSector_chart_pulsar_beam` and `exiledSector_chart_corona`), copying vanilla's entries except for the plugin class. Then override `setPulsarIfNeutron` in `ChartStarSystemGenerator` to add those ids with the same parameters vanilla uses. This covers every neutron star a chart system could get, not only the forced primary.

The terrain lives in the chart system and goes with it when `ChartRunCloser` removes the system, so nothing stays in the save. Saves made mid-run before this change keep the vanilla plugin until that run closes, which is fine.

Open question: should the non-player fleets also ignore the ordinary corona of other stars in charts (black hole charts, normal stars)? Exempting them everywhere is simple. Only neutron stars are in scope for now.

## 3. Transverse Jump out of a chart

### Options

A. Subclass the vanilla ability (recommended). Add `ChartTransverseJumpAbility extends FractureJumpAbility` and point the `fracture_jump` row at it in our `data/campaign/abilities.csv` (rows merge by id, so the row is copied from vanilla with only the plugin class changed). Outside chart systems, or with the star charts domain switched off, it does exactly what vanilla does. In a chart system with an open run, it charges vanilla's CR and fuel costs when `level == 1` and then calls a `ChartRunActions` exit that does what the inner wormhole's Return option does (`run.requestReturn()` and a transition to `returnPoint`), with the player fleet as the jump source instead of the wormhole. The tooltip gets one line in chart systems saying where the jump leads.
   - For: it reuses the button players know, needs no ability-bar handling or new art, and keeps vanilla's costs and charge-up.
   - Against: it replaces a vanilla ability row for the whole game, so another mod replacing the same row would clash, and load order would decide which one wins. Nothing installed does that today. Add a compat self-check that warns if the `fracture_jump` plugin at runtime isn't ours.

B. Watch the vanilla ability from `ChartRunScript`. Stop blocking it, and when it's in progress in a chart system, cancel it just before it fires and do our own exit. This doesn't touch the vanilla row, but the jump fires inside the ability's own `advance`, so catching it in time depends on frame order. We'd also have to copy the cost logic. Too fragile.

C. Let vanilla jump to hyperspace and redirect from `reportFleetJumped`. That means two transitions with a hyperspace flash in between, and the jump reaches hyperspace at whatever `system.getLocation()` a chart system has, with run closing logic seeing an exit nobody requested. Rejected.

D. A new chart-only ability (for example "Wormhole Recall"), added to the player fleet on entry and removed on exit. We'd control it fully, and it would work for players who never unlocked Transverse Jump. But it needs ability-bar slot handling, icon art and new strings, it must be removed reliably to keep the save clean, and it's a second button for what players see as the same action. Worth doing only if the exit should work without Transverse Jump.

### Recommendation

Option A. If the exit should also work for players who don't have Transverse Jump, the same subclass makes it easy to grant `fracture_jump` while inside a chart and take it away on exit, which is cheaper than option D.

Keep the other `chart_blocked_abilities.csv` entries blocked. Remove `Abilities.TRANSVERSE_JUMP` from `ALWAYS_BLOCKED` only once option A is in, so a domain switch-off or a clashing mod falls back to blocking it again. In other words, block it whenever the runtime plugin isn't ours.

Scope: this exit works in every chart system, not only Neutron Star ones, since leaving at the inner wormhole is allowed in all of them. If it should only work in Neutron Star charts, the subclass checks the run's anomaly.

## Build order

1. [ ] `ChartAnomaly.minPlanets()` and the planet top-up in `ChartStarSystemGenerator`, with a `ChartStarSystemGeneratorTest` case that checks every Neutron Star seed in a sample gets 7 or more planets, and that most of them sit inside the pulsar's blocking radius.
2. [ ] `ChartPulsarBeamTerrainPlugin` and `ChartCoronaTerrainPlugin`, their `terrain.json` entries, and the `setPulsarIfNeutron` override. Tests check that a non-player fleet keeps its velocity and CR and the player fleet doesn't.
3. [ ] `ChartTransverseJumpAbility`, the `abilities.csv` row, a shared exit in `ChartRunActions` that both it and the inner wormhole's Return option call, and the blocking fallback in `ChartBlockedAbilities` / `ChartRunScript`. Tests cover the chart exit path, vanilla behaviour outside charts, and the fallback.
4. [ ] Tooltip line through `exiledlib.i18n` (en and zh_CN), following `docs/tooltip_wording.md`.
5. [ ] Compat self-check for the `fracture_jump` plugin class.
6. [ ] Update the Neutron Star text in `docs/docs.md` and `../atlas.md`, then move this plan to `../completed/`.
