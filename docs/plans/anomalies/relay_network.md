# Anomaly: Relay Network

Dormant Domain-era relays sit around the system: a nav buoy, a comm relay and a sensor array. Waking all three recovers a data trace someone left behind, and finishing the system sends the network on, further out along the trail. Each step of the chain makes the whole next system harder and richer, for three or four steps, and the last boss guarantees a unique. Missing a step loses the trace for good.

Status: being built 2026-10-27; see the build order. Redesigned 2026-10-27 as a chain across atlas systems, replacing the pre-captured battle objectives. See `overview.md` for the shared framework.

## In a system

The chart places one of each relay type at spread-out orbits, each dormant. Each one wakes in its own way, using vanilla abilities:

- The nav buoy needs your fleet to hold station beside it for a few hours, moving slowly enough to sync.
- The comm relay needs your transponder on in its range, broadcasting the handshake. That raises your sensor profile while you do it.
- The sensor array needs an Active Sensor Burst fired in its range.

Each also costs supplies and heavy machinery through a short dialog. A relay near a guarded POI shares that POI's guard, so waking it may mean the fight you'd have had there anyway. Enemy patrols never retake a woken relay.

A woken nav buoy and sensor array behave like vanilla campaign objectives owned by the player for the rest of the run, giving their usual in-system bonus to the player's fleet. A woken comm relay shows every enemy fleet in the system on the map and minimap for the rest of the run.

Relays never touch battle objectives.

## The chain

The anomaly rolls on the atlas like any other and starts the chain at stage 1. Completing the system with all three relays woken moves the network on as an inserted anomaly record carrying the next stage (`AtlasAnomalyRecord`, `AtlasAnomalies.move`). The target is two or three lanes away, reached only through uncompleted systems, further from the Gateway than the system the network leaves, and accepted by `AtlasAnomalies.canInsert`; it is picked on the cell's hash. If no system qualifies, the chain ends there.

The atlas always shows where the chain is: the network's icon on its current system, a trace line drawn from the system it left, and the stage in the node tooltip. The network belongs to its system, so it waits there while the player runs other systems.

The trace is lost, and the record removed, when:

- the run in the network's system fails;
- the run closes with any relay still dormant.

A chain has three or four stages, rolled when it starts. Completing the last stage's system with all three relays woken ends the chain, and that system's boss always drops a random unique hull item or unique subroutine, picked from every unique whatever its source.

## Escalation

Stage 1 adds nothing. From stage 2 on, each stage adds lines to the whole system, every fleet and the boss alike: increased fleet strength and increased loot quantity, rising per stage (placeholders). They reuse the existing chart lines (Strength %, loot quantity) and reach the run through `ChartGenContext` like the `effects` column, so no new effect types are needed. The node tooltip lists the current stage's lines.

## Chart modifiers

Tuning lines are chart effects carried by an atlas source, per `overview.md`:

- `CHART_RELAY_COST_PERCENT`: reduced or increased supply and machinery cost to wake relays.
- `CHART_RELAY_CHAIN_STAGES`: +N to relay chain stages.

## Build order

1. [x] Relay entities and their dormant/woken state in the run, using vanilla objective entity types; the nav buoy and sensor array bonuses for the player, and the comm relay's fleet reveal. Built: `charts.anomaly.relay` (`RelayNetwork` places vanilla `nav_buoy`, `comm_relay` and `sensor_array` entities in three orbit bands, each orbiting a guarded POI half the time through `AnomalyContentSite.guardedPois` and `addEntity`; dormant relays are neutral and `$objectiveNonFunctional`), relay flags kept as text on the run record (`ChartRun.anomalyState`, `RelayStates`), and `ChartRelays.wake`: the nav buoy and sensor array switch to the player faction, so vanilla's `NavBuoyEntityPlugin` and `SensorArrayEntityPlugin` give the player's fleet their usual bonus, and the comm relay multiplies every other fleet's `detected_by_player_range_mult` and raises its extended detection range, so only the player sees them at any range (the relay itself stays non-functional, so it never counts as a comm relay for intel). Marker icon: `graphics/icons/anomalies/relay_network.png`, a circular copy of vanilla `comm_sniffer`. Not checked in game: the bonuses and the map and minimap reveal.
2. [x] Wake conditions per type (station keeping, transponder, sensor burst) and the cost dialog. Built: `ChartRelays.check` on the run script's one-second objective timer, so nothing runs per frame: the nav buoy syncs once the fleet holds within 300 su of it below burn 2 for 0.25 days, the comm relay once the transponder stays on within 2000 su of it for 0.25 days, and the sensor array once an Active Sensor Burst is active or in progress within 2000 su of it (placeholders in `RelayRules`; the hold start is kept in the relay's memory). `RelayDialog`, picked over vanilla's objective dialog by `ChartCampaignPlugin`, shows the wake condition and a vanilla cost panel, and Restore power takes supplies and heavy machinery (10 + 4 per tier and 5 + 2 per tier, placeholders). Syncing and restoring power can happen in either order, and a relay wakes once both are done. Not checked in game: the dialog, the conditions and their timing.
3. [ ] The stage on the anomaly record, moving it on completion with all three woken, removing it on failure or a dormant relay at close, and the outward target pick.
4. [ ] Per-stage escalation lines in the run's context, and the last boss's guaranteed unique.
5. [ ] Atlas trace line and the stage in the node tooltip.
6. [ ] Chart modifier lines, strings, `docs/docs.md` section and tests.

## Open questions

- Decompile how to force map and minimap icons for fleets the player has no sensor contact on, and keep the reveal off the per-frame path.
