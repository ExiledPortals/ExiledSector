# Anomaly: Listening Post

A fortified listening post sits in the system behind a ring of sensor pickets, with a vault full of intercepted data. Reach it unseen and the vault is yours whole. Get spotted and the alarm goes up: the vault starts purging and the pickets close in on the post. It's a heist played with Starsector's own sensor game.

Status: built and tested 2026-10-29 (package `exiledsector.charts.anomaly.heist`). Not yet checked in game. Proposed 2026-10-28. See `docs/plans/anomalies/overview.md` for the shared framework.

## Lore

Domain-era listening posts sat on quiet orbits and recorded everything that passed through a system's comm traffic. Whoever holds this one now has kept it running and its vault full, and set pickets to make sure nobody gets close enough to read it.

## How it plays

The chart places the post on a clear orbit away from the inner wormhole (`AnomalyContentSite.clearOrbit`) and three to five picket fleets (placeholder) in a ring around it. Pickets are small chart-faction fleets with long sensor range (placeholder), holding station on their points; they never patrol or chase while the post is quiet. The post and the pickets show on the map and in chart intel from the start, so you can plan a way in.

Getting in unseen uses only vanilla's tools: Go Dark, moving slowly, nebulae, asteroid fields, and staying outside each picket's sensor range. While any picket has sensor contact on your fleet, suspicion builds; out of contact it drains (placeholder rates). Suspicion shows in the post's map tooltip and chart intel. Full suspicion raises the alarm. A picket that identifies your fleet outright reports in, raising it shortly after unless you destroy that picket first (see Detection).

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

Each picket checks `SectorEntityToken.getVisibilityLevelOfPlayerFleet()` (0.98a API: `NONE`, `SENSOR_CONTACT`, `COMPOSITION_DETAILS`, `COMPOSITION_AND_FACTION_DETAILS`). `SENSOR_CONTACT` builds suspicion. Anything higher means the picket has identified you and starts its report, which raises the alarm after a short delay (placeholder half a day) unless that picket is destroyed with nothing escaping first (decided 2026-10-29: with the transponder off vanilla identifies a fleet inside half its detection range, so attacking a picket always passes through identification, and an instant alarm would rule out picking one off). The check runs from one script on a short game-time interval, never per frame, and stops once the alarm is up or the vault is taken.

## Chart modifiers

Tuning lines are chart effects carried by an atlas source, per `docs/plans/anomalies/overview.md`:

- `CHART_LISTENING_POST_PICKETS`: +N pickets around the listening post.
- `CHART_LISTENING_POST_LOOT_QUANTITY_PERCENT`: increased quantity of items in the listening post's vault.
- `CHART_LISTENING_POST_PURGE_PERCENT`: reduced vault purge rate.

## Build order

1. [x] The anomaly class (`exiledsector.charts.anomaly.heist`) and its insertable `chart_anomalies.csv` row; the post entity on a clear orbit, the picket ring, and state in `ChartRunState`. Built: `ListeningPost` puts the post (custom entity `exiledSector_chart_listening_post`, vanilla `station_side07` sprite, `ListeningPostPlugin` for its tooltips) of the chart faction on a clear orbit 2500 to 8000 su from the centre, preferring one at least 4000 su from the inner wormhole, and three to five pickets in a ring 1800 su (plus or minus 300) around it (`Pickets`, `PicketStation`, numbers in `HeistRules`, placeholders). Pickets are 30% of a chart fleet, non-aggressive and never sidetracked, hold their points (`ORBIT_PASSIVE` on a point `PostScript` keeps beside the post), have 50% more sensor range and always show on the map. Suspicion, the alarm, the vault share and whether it was taken live on the run record (`PostState`, one `listening_post` entry); the layout lives in the system script `PostScript`, as the convoy's does. Like the convoy, it doesn't fit special sites or infested systems. Tests: `ListeningPostTest`.
2. [x] Detection: the suspicion script, the alarm, and the picket-battle rule (alarm unless the picket is wiped out). Built: `PostScript.check` runs every 0.1 days while the post is quiet and you are in the system: sensor contact builds suspicion 25% a day, out of contact it drains 10% a day, and identification starts that picket's half-day report (kept in the picket's memory, so it dies with the picket). Full suspicion or a report running out raises the alarm (`raiseAlarm`); so does `reportBattleOccurred` when a picket still has ships after a battle you were in. Tests: `PostScriptTest`, `HeistRulesTest`.
3. [x] The alarm: the vault purge, the pickets gathering into one guard fleet, intel and tooltip updates. Built: after the alarm the vault share falls 20% a day to 25% (`HeistRules.vaultAfter`, slowed by the purge line); every picket flies to the post, and the first within 300 su becomes the aggressive Post Guard orbiting it while later arrivals merge into it, bringing their ships (none as flagship), officers, vanilla drops and loot ledger, and counting as one more fleet for the guard's item roll (`ChartFleetLoot.LOOT_FLEETS_MEMORY_KEY` multiplies its share) (`Pickets.standGuard`, `merge`). Tests: `PicketsTest`. `PostText` writes the status for the post's map and campaign tooltips and the chart intel (`ChartAnomaly.intelText`): suspicion and picket count, a pending report, the alarm with the vault left, or the vault taken. Tests: `PostScriptTest`.
4. [x] The post dialog through `ChartCampaignPlugin.pickInteractionDialogPlugin`, the vault loot with its leanings, and the lane reveal for a clean heist. Built: `PostDialog` opens the vault while the post is quiet, offers to engage its defenders while the alarm is up and any picket or the guard lives (`PostScript.gatherDefenders` merges every living picket into the guard first, so it is one fight, and the vanilla fleet dialog hands back to the post on leave), and otherwise takes what is left. The vault is the `listening_post` scope of `chart_loot.csv` plus the `exiledSector_chart_listening_post` row of `item_salvage.csv`, 1.5 steps at tier 1 and 0.35 more per tier, +100% to Star Charts, blueprints and subroutines, times the share left (`VaultLoot`, placeholders), on vanilla's loot screen. A clean heist flags the atlas cell `AtlasState.LANES_REVEALED` (`AtlasProgress.revealLanesAround`), and `AtlasReach.visible` reveals 3 lanes around such cells (placeholder) without completing them. Tests: `AtlasReachTest`.
5. [x] Chart modifier lines, strings (en and zh_CN), the atlas marker icon, the `docs/docs.md` section and tests. Built: `ChartEffect.CHART_LISTENING_POST_PICKETS` (up to 5), `CHART_LISTENING_POST_LOOT_QUANTITY_PERCENT` (scales the vault's steps and items) and `CHART_LISTENING_POST_PURGE_PERCENT` (reads "increased" when negative), carried by no source yet, also in `tools/editor_vocabulary.json`; `chart.post.*` and atlas strings in both catalogues (zh a draft); the atlas marker `graphics/icons/anomalies/listening_post.png`, a circular crop of vanilla's `comm_relay` illustration, which is also the dialog image; the `docs/docs.md` section. Tests: `ListeningPostTest`, `PostScriptTest`, `PicketsTest`, `HeistRulesTest`, `AtlasReachTest`.
6. [x] The Picket Orders catalyst (`atlas_catalysts.md`), with the pickets and post flying the system's faction as when the atlas rolls the anomaly: the `atlas_catalysts.csv` row (orders family), its `special_items.csv` row, name strings (en and zh_CN) and tests. Built: `exiledSector_picket_orders` in both tables with the orders flavour, and `catalyst.exiledSector_picket_orders.name`; an inserted post needs no code of its own, since every anomaly is generated with the chart faction, the system's owner. Tests: `AtlasCatalystsTest`, `ListeningPostTest.thePostAndItsPicketsFlyTheSystemsFactionHoweverTheAnomalyArrived`.

## Open questions

- Whether Dense Nebula and Magnetic Storm make the heist too easy, and should go in `excludes` or shrink the pickets' sensor penalty instead.
- Whether suspicion needs its own on-screen indicator in the campaign layer, or the tooltip and intel are enough.
- How the vault's purge reads to the player: a fixed share each day, or visible "data cores" disappearing one by one.
- Whether vanilla's sensor profile gives the player enough tools at every tier, or a late-tier picket's range needs a cap.
- The name is a placeholder.
