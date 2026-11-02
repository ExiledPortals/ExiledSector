# Chart drawback: Faulty (ship systems can fail on activation)

Ship systems on the player's side have a chance to fail each time they are activated. A failed activation still costs whatever activating costs (flux, a charge, the cooldown), and nothing happens.

Status: proposed 2026-11-02 (community suggestion). Not built.

## Versus Inert

Inert (`CHART_NO_SHIP_SYSTEMS`) switches player-side systems off for the whole battle, so it is strictly worse for the player than Faulty, and both only touch the player's side. Faulty is the softer, more random version: a 33% failure leaves systems usable but unreliable, and charge-based systems hedge by spending two or three charges (all three of three failing is a 1 in 27 chance). It hits ships built around one critical system hardest (Hyperion teleports, Doom mines, Odyssey, Paragon), while skimmer frigates shrug it off.

## Effect

One prefix line, `CHART_SHIP_SYSTEM_FAILURE_CHANCE_PERCENT` (name Faulty), placeholders `mid=15-20/high=30-35`. It shares an exclusive group (`ship_systems`) with Inert, so the two never roll on one chart. Each activation of a player-side ship's system (including allies, not fighters) rolls the chance; on a failure the system is stopped before its effect lands, its normal cooldown starts, and a short floating "System failure" text and a fizzle sound show it (`engine.addFloatingText`, a vanilla sound such as `system_ammo_feeder_deactivate` or similar, to pick).

## Where it lives

`ChartSystemFailurePlugin` in `charts.effect`, registered beside `ChartMisfirePlugin`, on only in campaign battles while `ChartPlayerModifiers.systemFailureChance()` is above zero. The API has no system activation event, so it watches each player-side ship's `getSystem().getState()` once per frame and acts on the step out of `IDLE` (into `IN`, or straight into `ACTIVE` for systems without charge-up). On a failure it calls `getSystem().deactivate()` and, if the cooldown didn't start, `setCooldownRemaining(getCooldown())`. Ships are kept in a small map of last-seen states, rebuilt on a one-second timer like `ChartMisfirePlugin`'s weapon list, so the per-frame work is one state read per ship.

## Fragility

The roll has to land before the system's script applies anything. Most scripts act as the effect level ramps up through `IN`, so stopping on the first `IN` frame is clean; instant systems that act in their first frame may already have fired by the time the plugin sees them, depending on plugin and ship advance order. Before building, test at least: Burn Drive, Plasma Jets, Phase Skimmer, Phase Teleporter (Hyperion), Mine Strike (Doom), Temporal Shell, Fortress Shield, Damper Field, Recall Device, Entropy Amplifier, Lidar Array, Reality Disruptor and Targeting Feed, plus a toggled system. Any system the deactivate path can't stop cleanly is excluded from the roll (listed by id in the plugin), rather than half-failing.

## Open questions

1. Whether the AI should know. Proposed no: it retries after the cooldown like a player would.
2. Whether a failure also costs the ship flux or CR on top of the activation cost. Proposed no; the lost system use is the drawback.

## Build order

1. [ ] Spike: the activation watcher and deactivate path tested against the system list above in the simulator; settle the exclusion list.
2. [ ] `CHART_SHIP_SYSTEM_FAILURE_CHANCE_PERCENT` in `ChartModifier`, `star_chart_modifiers.csv` row with the `ship_systems` group (also added to Inert's row), the chance through `ChartGenContext`, `ChartPlayerModifiers` (set, clear) and `ChartRunScript`.
3. [ ] `ChartSystemFailurePlugin`, with the state-step detection in a static helper for tests.
4. [ ] Strings (en and zh_CN draft) following `docs/tooltip_wording.md`, for example "Ship systems on your side have a <bad>{value}%</bad> chance to fail when activated in the system", and the floating text key.
5. [ ] Tests for the step detection and the roll; `docs/docs.md` "Chart system rules" sentence.
