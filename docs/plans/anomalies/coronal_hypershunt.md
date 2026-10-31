# Coronal Hypershunt

A rare star anomaly found only far out in the Outer Reaches: the system's star is a blue supergiant with a Domain coronal hypershunt in its corona, guarded by an Omega fleet. Beating the fleet earns its loot, but the hypershunt itself can never be restored.

Status: built 2026-10-31 apart from the in-game checks; move this plan to `completed/anomalies/` once those are done. Requested 2026-10-31. See `overview.md` for the shared framework.

## Design

- Star anomaly: `primaryStar` is `star_blue_supergiant` and the system type is single, like the Black Hole and Neutron Star. It never shares a system with another star anomaly.
- Rare and find-only: `weight` 0.1 in `chart_anomalies.csv` (placeholder) and not `insertable`, so the atlas roll is the only way it appears in play. `fitsAt` refuses systems closer than 15 cells to the Gateway, so it never rolls nearer.
- The hypershunt is vanilla's `coronal_tap` entity, placed where vanilla places it (the star's radius plus 250 su, pointing down). It gets no particle script, which vanilla only runs for a working hypershunt.
- The guards are an Omega fleet built like the Derelict Forge's: the tier's fleet size, never fewer than the 80 points of vanilla's hypershunt defenders, with officers from vanilla's Omega officer plugin, chart loot, and vanilla's Omega weapon drop groups and limited ship tooltips. They are fought through vanilla `SalvageDefenderInteraction` from the hypershunt's memory.
- Never restorable: our dialog takes every interaction with the hypershunt during its run, so vanilla's `coronal_tap` rules (the repair offer and the hypershunt intel) never fire. After the fight it only says the structure is beyond repair.
- Console-only catalyst: Hypershunt Survey (Explorarium Survey family) with the new `star` effect in `atlas_catalysts.csv`. A star catalyst may insert a star anomaly despite `insertable`, through `AtlasAnomalies.canInsertStar` and `insertStar`; every other check stays, so it refuses a system that already holds a star anomaly and, through the anomaly's own `insertVerdict`, any system closer than 15 cells. The loader gives a star catalyst weight 0, so it never drops and only `ExiledSectorGrantAtlasCatalysts` hands it out. Inserting it changes the system's star to a blue supergiant, on the atlas preview and in the run.

## Build order

1. [x] The anomaly, its row and placeholder icon (a circular crop of vanilla's `coronal_tap2` illustration). Built: `CoronalHypershunt` (`charts.anomaly.hypershunt`), registered in `ChartAnomalies`. Tests: `CoronalHypershuntTest`.
2. [x] The Omega guards and the dialog. Built: `HypershuntGuards`, `HypershuntDialog`; `ChartAnomalyFleets.Spec.atLeast` now shared with the Pre-Collapse Facility's guards. Tests: `HypershuntGuardsTest`.
3. [x] The star catalyst. Built: `AtlasCatalysts.Effect.STAR`, `AtlasAnomalies.canInsertStar` and `insertStar`, `AnomalyCatalystUse` choosing between them; the `exiledSector_hypershunt_survey` rows in `atlas_catalysts.csv` and `special_items.csv`. Tests: `AtlasCatalystsTest`, `AtlasCatalystUseTest`, refusal key in `StarChartUiStringKeysTest`.
4. [x] Strings (en and zh_CN draft) and the `docs/docs.md` section.
5. [ ] In-game checks: reach a system 15 or more cells out (`ExiledSectorAtlasReach`), grant the survey (`ExiledSectorGrantAtlasCatalysts 1 hypershunt_survey`), insert it, see the blue supergiant on the atlas, open the run, fight the Omega fleet, and confirm the hypershunt never offers repair.

## Open questions

- Whether beating the guards should give a reward of its own beyond the fleet's loot.
- The weight, the distance and the guard size are placeholders.
