# Chart drawback: Improvised (Converted Hangar penalties)

Every ship in the player fleet suffers vanilla Converted Hangar's penalties while in the chart system, whether or not it has the hull mod. No chart line targets carrier fleets yet; this one does, and it hits a carrier harder the more fighter OP it fields.

Status: proposed 2026-11-02 (community suggestion). Not built.

## Effect

One red-tier prefix line, `CHART_IMPROVISED_HANGARS` (`high=1`, name Improvised), a flag like Inert. While the system's lines are active, each player fleet ship gets vanilla `ConvertedHangar`'s penalties (`starfarer.api.jar`, 0.98a), counted over all of its fitted wings rather than only the converted bay's:

- Fighter refit time 50% longer (`getFighterRefitTimeMult` x1.5), and the replacement rate falls 1.5 times faster and recovers 1.5 times slower (`replacement_rate_decrease_mult` and `replacement_rate_increase_mult` x1/1.5).
- Rearm time 40% of base refit time longer (`fighter_rearm_time_extra_fraction_of_base_refit_time_mod` +0.4).
- 20 more minimum crew (`getMinCrewMod` +20).
- Deployment points and supplies to recover up by 1 per 5 OP of fitted wings, rounded up (`deployment_points_mod`, `getSuppliesToRecover`), with CR per deployment raised in proportion (`getCRPerDeploymentPercent` x(1 + DP added / supplies to recover)).

Placeholder magnitudes copy vanilla's; they go to `docs/analysis/balance.csv` review like every other line.

## Where it lives

`ChartGenContext.improviseHangars()` sets the flag, `ChartFleetConditions` carries it beside `shipSystemsDisabled`, and `ChartFleetConditions.applyToShip` (reached through `ChartFleetConditionSource`, so before ship creation) adds the penalties under the chart's `MOD_ID`. A small `ChartHangarPenalties` class in `charts.effect` holds the numbers and the wing OP sum (`FighterWingSpecAPI.getOpCost(stats)` over `getVariant().getFittedWings()`). The DP and supply penalties are deployment-time stats, so they must be in place when the refit and deployment screens read them; `PlayerFleetStats.markForStatUpdate` on entering and leaving already covers that for `DEPLOYMENT_COST`.

## Open questions

1. Bayless ships. Vanilla's minimum of 1 DP and the 20 crew land on every ship. Proposed: apply the DP and crew penalties only to ships with at least one fitted wing, so the line stays about carriers; the refit penalties already do nothing without bays.
2. Waivers. Our tree has nodes that waive Converted Hangar penalties through `Stats.CONVERTED_HANGAR_NO_*` (`FighterSkillEffect.WaivedPenalty`). Proposed: the chart penalties ignore those waivers, since they belong to the hull mod and not the chart; a ship with real Converted Hangar takes both.
3. Built-in carriers with `converted_hangar` already fitted: proposed to stack (the chart is a drawback on top).

## Build order

1. [ ] `CHART_IMPROVISED_HANGARS` in `ChartEffect`, `star_chart_modifiers.csv` row, the flag through `ChartGenContext`, `ChartFleetConditions` (set, clear) and `ChartRunScript`.
2. [ ] `ChartHangarPenalties` applied in `ChartFleetConditions.applyToShip`; fighters need nothing extra since the stats live on the carrier.
3. [ ] Strings (en and zh_CN draft) following `docs/tooltip_wording.md`, for example "Ships in your fleet suffer <bad>Converted Hangar</bad>'s penalties in the system"; `tools/editor_vocabulary.json` if the editor lists chart lines.
4. [ ] Tests in `ChartFleetConditionsTest` (penalties on a player ship with wings, none on another fleet's, DP from wing OP, the bayless rule); `docs/docs.md` "Chart system rules" sentence.
