# Anomaly: Listening Post

A fortified listening post sits in the system behind a ring of sensor pickets, with a vault full of intercepted data. Reach it unseen and the vault is yours whole. Get spotted and the alarm goes up: the vault starts purging and the pickets close in on the post. It's a heist played with Starsector's own sensor game.

Status: planned, nothing built. Proposed 2026-10-28. See `overview.md` for the shared framework.

## Lore

Domain-era listening posts sat on quiet orbits and recorded everything that passed through a system's comm traffic. Whoever holds this one now has kept it running and its vault full, and set pickets to make sure nobody gets close enough to read it.

## How it plays

The chart places the post on a clear orbit away from the inner wormhole (`AnomalyContentSite.clearOrbit`) and three to five picket fleets (placeholder) in a ring around it. Pickets are small chart-faction fleets with long sensor range (placeholder), holding station on their points; they never patrol or chase while the post is quiet. The post and the pickets show on the map and in chart intel from the start, so you can plan a way in.

Getting in unseen uses only vanilla's tools: Go Dark, moving slowly, nebulae, asteroid fields, and staying outside each picket's sensor range. While any picket has sensor contact on your fleet, suspicion builds; out of contact it drains (placeholder rates). Suspicion shows in the post's map tooltip and chart intel. Full suspicion raises the alarm. A picket that identifies your fleet outright raises it at once.

Fighting a picket is allowed. A picket destroyed in the engagement with nothing escaping raises no alarm; one that survives, escapes or leaves the battle raises it. That makes picking off one picket to open a gap a real choice.

Interacting with the post while the alarm is down opens a short dialog of ours and gives the whole vault.

## The alarm

Once raised, the alarm stays up for the rest of the run:

- The vault purges: its loot budget falls by a share each day down to a floor (placeholder: 20% a day, down to 25%).
- The pickets abandon their points, gather at the post and guard it as one fleet. Reaching the vault means beating them, and only one fight is ever needed.
- The post's tooltip and intel switch to the alarm and show what's left of the vault.

You can also walk away. Nothing else in the system reacts to the alarm.

## Rewards

The vault is chart loot budget leaning toward data: Star Charts, blueprints and subroutines (placeholder leanings, through `ChartLoot` like the Excavation Sites' leanings). A clean heist, with the alarm never raised, also reveals the atlas lanes around the system as a Sensor Array does (placeholder radius).

## Detection

Each picket checks `SectorEntityToken.getVisibilityLevelOfPlayerFleet()` (0.98a API: `NONE`, `SENSOR_CONTACT`, `COMPOSITION_DETAILS`, `COMPOSITION_AND_FACTION_DETAILS`). `SENSOR_CONTACT` builds suspicion and anything higher raises the alarm (placeholder thresholds). The check runs from one script on a short game-time interval, never per frame, and stops once the alarm is up or the vault is taken.

## Chart modifiers

Tuning lines are chart effects carried by an atlas source, per `overview.md`:

- `CHART_LISTENING_POST_PICKETS`: +N pickets around the listening post.
- `CHART_LISTENING_POST_LOOT_QUANTITY_PERCENT`: increased quantity of items in the listening post's vault.
- `CHART_LISTENING_POST_PURGE_PERCENT`: reduced vault purge rate.

## Build order

1. [ ] The anomaly class (`exiledsector.charts.anomaly.heist`) and its insertable `chart_anomalies.csv` row; the post entity on a clear orbit, the picket ring, and state in `ChartRunState`.
2. [ ] Detection: the suspicion script, the alarm, and the picket-battle rule (alarm unless the picket is wiped out).
3. [ ] The alarm: the vault purge, the pickets gathering into one guard fleet, intel and tooltip updates.
4. [ ] The post dialog through `ChartCampaignPlugin.pickInteractionDialogPlugin`, the vault loot with its leanings, and the lane reveal for a clean heist.
5. [ ] Chart modifier lines, strings (en and zh_CN), the atlas marker icon, the `docs/docs.md` section and tests.
6. [ ] The Picket Orders catalyst (`atlas_catalysts.md`), after that plan's step 7 puts the faction on Transit Orders: the pickets and post fly the order's faction when one is recorded; the `atlas_catalysts.csv` row (orders family), its `special_items.csv` row, name strings (en and zh_CN) and tests.

## Open questions

- Whether Dense Nebula and Magnetic Storm make the heist too easy, and should go in `excludes` or shrink the pickets' sensor penalty instead.
- Whether suspicion needs its own on-screen indicator in the campaign layer, or the tooltip and intel are enough.
- How the vault's purge reads to the player: a fixed share each day, or visible "data cores" disappearing one by one.
- Whether vanilla's sensor profile gives the player enough tools at every tier, or a late-tier picket's range needs a cap.
- The name is a placeholder.
