# Anomaly: Storm Front

An ion storm front drifts slowly across the system on a path you can see in advance. Any battle fought inside it is fought in the storm: lightning arcs strike ships on both sides every few seconds. Battles inside drop more loot, so where you fight becomes the decision.

Status: planned, nothing built. Proposed 2026-10-28. See `overview.md` for the shared framework.

## How it plays

The storm is a custom campaign terrain on `BaseTerrain`, a wide band that enters at the system edge, crosses at least two orbits with POIs or planets, and leaves at the far edge over a fixed number of days (placeholder 30), then dissipates. Its path and timing show in chart intel from the start of the run, as the Supply Convoy's route does. Inside it, every fleet's sensor range is reduced (placeholder), so the storm is also cover.

The storm adds no fleets. Chart fleets, bosses, convoys and anomaly fleets can all end up fighting in it, depending on where you catch them. A fleet the storm passes over isn't pushed or damaged in the campaign layer.

## Battles in the storm

A battle is in the storm when the player fleet is inside the terrain as the engagement starts. The flag is recorded then, the way `ShaftEncounters.battleContextCreated` records the cavern battle, and a combat plugin of ours reads it.

In the battle, an arc strikes every few seconds (placeholder 6), hitting a random ship on the field weighted by hull size, from either side. Strikes are EMP arcs (`CombatEngineAPI.spawnEmpArc`) that shields can block and phased ships can't be hit by, so raising shields and phasing are how you weather them. A brief flash and drifting cloud overlay are drawn by a `CombatLayeredRenderingPlugin` of ours, GL11 only, with no `glGet` calls while drawing (Fast Rendering rules). Combat nebula tiles are never used, per the Foiler mods rules in `../atlas.md`.

## Rewards

Battles in the storm drop increased item quantity (placeholder +50%), added in `ChartFleetLoot` from the flag recorded at the start. Hull items from those battles can also roll storm-only modifiers (`item_modifiers.csv` rows with the `_STORM_FRONT` suffix, as Anomalous Debris' engram modifiers carry `_ANOMALOUS_DEBRIS`), placeholder lines with an energy and EMP theme.

## With other anomalies

The storm counts as terrain for the Supply Convoy's escorts (`ConvoyTerrain`), so they drift apart inside it like they do in nebulae and asteroid fields.

## Chart modifiers

Tuning lines are chart effects carried by an atlas source, per `overview.md`:

- `CHART_STORM_FRONT_LOOT_QUANTITY_PERCENT`: increased quantity of items dropped by battles in the storm.
- `CHART_STORM_STRIKE_FREQUENCY_PERCENT`: increased frequency of storm strikes.
- `CHART_STORM_FRONT_SIZE_PERCENT`: increased Storm Front size.

## Build order

1. [ ] The anomaly class (`exiledsector.charts.anomaly.storm`) and its insertable `chart_anomalies.csv` row; the storm terrain plugin with its path, movement, sensor penalty and renderer; route and timing in chart intel.
2. [ ] The in-storm battle flag, the strike plugin and its overlay. In-game pass with Terrain FX on.
3. [ ] The loot bonus in `ChartFleetLoot` and the storm-only hull item modifiers.
4. [ ] `ConvoyTerrain` counting the storm; chart modifier lines, strings (en and zh_CN), the atlas marker icon, the `docs/docs.md` section and tests.
5. [ ] The Storm Terminus catalyst (`atlas_catalysts.md`): the `atlas_catalysts.csv` row (terminus family), its `special_items.csv` row and name strings (en and zh_CN); it loads once the anomaly has its icon.

## Open questions

- Whether `spawnEmpArc` accepts a null source ship, or the strikes need an invisible source entity.
- Strike damage and EMP by tier, and whether strikes should favour the side with more ships on the field.
- Whether ships near a strike should get a short boost (energy weapon damage, flux dissipation) so the storm isn't only a hazard.
- Whether Terrain FX should draw anything for our terrain in the battle background, or leave it to our overlay.
- Whether a second front should follow once the first has crossed, so a long run still has a storm.
