# Anomaly: Supply Convoy

A convoy runs a fixed route through the system on a schedule you can see, carrying a known cargo to a planet. It leaves you alone until you turn on it. Where you strike is the gameplay: hit it where the escorts are spread out and you only fight part of them, or let it deliver and raid the planet for the cache with your marines.

Status: built and tested 2026-10-27 (package `exiledsector.charts.anomaly.convoy`, raid in `exiledsector.charts.raid`). Not yet checked in game. Redesigned 2026-10-27: the convoy is friendly until attacked, it delivers to an existing planet, the cache is taken with a raid, and the shadowing rule is dropped. See `docs/plans/anomalies/overview.md` for the shared framework.

## How it plays

The convoy is a freighter fleet with escort fleets travelling with it: a screen ahead and a close escort. It stays inside the chart's system: it enters at the system edge, visits two to four waypoints (planets, stations), reaches its destination, an existing planet in the system, unloads its cargo there and leaves through the edge.

Its route, its destination, its timing and its manifest (for example "1-2 subroutines, currency items") show in intel from the start of the run, so you can plan the strike.

The convoy is non-hostile to the player, unlike every other chart fleet, so you can fly alongside it freely and it never attacks first. Attacking any one of its fleets turns the freighters, the screen and the close escort hostile together for the rest of the run, and so does hitting one with Interdiction Pulse. Attacking the convoy never changes your standing with any faction.

The escorts keep formation in open space and fall out of it in terrain: in asteroid fields and nebulae the fleets slow by different amounts and drift apart. Hit the freighters while the escorts are too far away to join the battle and you only fight the freighters and whatever's close. Interdiction Pulse holds fleets in place to open a gap, and starts the attack. The freighters flee once attacked and drop cargo pods as they take losses.

## The raid

Once the convoy has unloaded, the destination planet holds the cache, and interacting with it offers a raid. The raid uses Exiled Sector's own raid class (`exiledsector.charts.raid`), copied from vanilla's non-market raid in `MarketCMD` (source in `starsector-core/starfarer.api.zip`) so we can change it freely. It keeps vanilla's framework: raid strength from marines and ground support with the fleet's ground attack bonus, raid effectiveness against an operation difficulty, the ground raid target picker with marine tokens per objective, marine losses, marine XP and the spoils screen. It drops reputation, military response, stability, hostility timeouts, raid cooldowns and `rules.csv`; the chart's interaction dialog opens it directly, and its text goes through i18n.

The operation difficulty is fixed by the chart's tier. The cache is one raid objective of our own, holding its chart loot, and stays available until taken, so a raid you can't yet launch can be retried with more marines.

## Rewards

The cargo is chart loot budget split between the freighters (dropped as pods, scaled by how many die) and the cache. Freighters that survive an attack still deliver their share, so the cache holds whatever reached the planet. Striking before the delivery costs a fight and gets more of the cargo; waiting avoids the fleet battle and costs marines.

## Chart modifiers

Tuning lines are chart effects carried by an atlas source, per `docs/plans/anomalies/overview.md`:

- `CHART_CONVOY_CARGO_PERCENT`: increased convoy cargo.
- `CHART_CONVOY_ESCORT_PERCENT`: increased or reduced convoy escort strength.
- `CHART_EXTRA_CONVOYS`: +N to convoys.

## Build order

1. [x] Convoy fleets, non-hostile to the player, with the route through waypoints to the destination planet; intel with route, destination, timing and manifest. `charts.anomaly.convoy`: `SupplyConvoy` (anomaly, generation), `ConvoyRoute`, `ConvoyFleets` (fleets fly the run-only faction `exiledSector_convoy`, under its own name (`chart.convoy.faction.*`) with the chart faction's crest and ship prefix, and Nexerelin told not to sync or randomise its relations), `ConvoyScript` (system script: departs 5 days after the player arrives), `ConvoyIntel`; anomaly fleets go through `content.ChartAnomalyFleets`.
2. [x] Turning the whole convoy hostile when any of its fleets is attacked or pulsed, with no reputation change. `ConvoyEncounterDialog` (picked by `ChartCampaignPlugin` through `ConvoyDialogs`), `ConvoyScript.turnHostile` and `checkPulse` (reads vanilla's `$justDidInterdictionPulse` on its periodic check, holds the hit fleets for vanilla's interdict seconds), flags in `ConvoyHostility`.
3. [x] Escort formation and terrain separation. The close escort follows the freighters and the screen follows a point 1200 su ahead of them (beyond the 500 su pull-in); in open space the freighters' burn is capped to the slowest escort's, and in asteroid belts, asteroid fields and nebulae each fleet's burn drops by its own share (`ConvoyTerrain`, `ConvoyScript.keepFormation`).
4. [x] Freighter flight and cargo pod drops; split loot budget. Once hostile the freighters skip their remaining stops and run for the destination (`ConvoyScript.flee`). The cargo is the `convoy` scope of `chart_loot.csv` plus the `exiledSector_chart_convoy` item row, sized by the freighters' fleet points; each share of freighter fleet points lost drops that share as cargo pods that last the run, and the rest is delivered (`ConvoyState`, `ConvoyCargo`, `loot.ChartCargoLoot`, `ConvoyScript.recordLosses`).
5. [x] The raid class in `charts.raid`, copied from vanilla's non-market raid, and the cache raid objective. `raid.ChartRaid` (raid menu, objective picker, marine losses, XP, loot screen; text in the `chart.raid.*` keys) and `ConvoyCacheObjective`, which rolls exactly the delivered share of the cargo (vanilla's leftover-effectiveness bonus is a sawtooth, so the cache ignores it and more marines never shrink it). Left out from vanilla: reputation, military response, stability, hostile timeouts, cooldowns, defender preparedness and rules; other mods' raid-objective listeners aren't asked to add objectives, but marine-loss listeners still apply.
6. [x] Delivery to the destination planet, the raid option there, difficulty by tier. The freighters' arrival and unloading run `ConvoyStep` callbacks on their assignments (`ConvoyScript.arrived`, `unloaded`); while a cache is there, `ConvoyCacheDialog` (through `ConvoyDialogs`) offers the raid before the planet's own dialog, at `ConvoyRules.raidDifficulty` and `cacheDanger` for the tier.
7. [x] Chart modifier lines, strings, `docs/docs.md` section and tests. `ChartEffect.CHART_CONVOY_CARGO_PERCENT`, `CHART_CONVOY_ESCORT_PERCENT` (reads "reduced" when negative) and `CHART_EXTRA_CONVOYS` (up to 3 convoys), carried by no source yet; `chart.convoy.*` and `chart.raid.*` strings in both catalogues (zh draft); docs section "Supply Convoy"; tests in `charts.anomaly.convoy` and `charts.raid`. Atlas icon `graphics/icons/anomalies/supply_convoy.png`, a circular crop of vanilla's `freighters` illustration.

## Resolved questions

- Pull-in range: vanilla's `battleJoinRange` is 500 su (`settings.json`, 200 more for AI fleets of the player's faction), measured edge to edge from the attacked fleet (`FleetInteractionDialogPluginImpl.pullInNearbyFleets`). A fleet in range also has to be visible to the player's sensors and its AI has to want to join. Escort separation is tuned against 500 su.
- Attacking a non-hostile fleet: the pull-in runs as soon as the encounter dialog opens, before the player picks Engage, and `TacticalModule.wantsToJoin` refuses any fleet with `$cfai_makeNonHostile` that would join the side against the player, and every fleet with `$isTradeFleet`. So clicking a peaceful convoy fleet opens our own `ConvoyEncounterDialog` first; choosing to attack turns the whole convoy hostile, then hands over to vanilla's fleet dialog, whose pull-in now takes escorts within 500 su. The freighters keep `$isTradeFleet`, so they never join a fight over an escort.
- Faction and reputation: battles skip the reputation change when the enemy side's primary fleet has `$noRepImpact` (`FleetEncounterContext.adjustPlayerReputation`), which every convoy fleet carries. Interdiction Pulse, though, applies `RepActions.INTERDICTED` to the hit fleet's faction with no memory check (`InterdictionPulseAbility.applyEffect`). So the convoy flies the run-only faction `exiledSector_convoy`, under its own name so vanilla's reputation message never names a real faction, set back to neutral towards the player whenever a convoy is generated, and no real faction's standing ever changes.
