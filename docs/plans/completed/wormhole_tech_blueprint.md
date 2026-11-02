# Wormhole Tech Blueprint: plan

Today the Wormhole Gateway structure unlocks with the Janus Device (`$playerCanUseGates`), or with the `exiledSector_tech_wormhole_gateway` research when Ashes of the Domain: Vaults of Knowledge (`aotd_vok`) is on. That research sits at tier 5 behind Hyperspace Stabilization and the Hyperdimensional Processor.

This plan replaces the Janus Device with a new item, the Wormhole Tech Blueprint, found where vanilla hides the first wormhole anchor: the Mothballed Equipment Cache in Limbo. Without VoK, right-clicking the blueprint unlocks the gateway for good. With VoK, the blueprint becomes the cost of a gateway research at the start of the tree. The gateway can then open charts up to tier 5 only, and two upgrade items, earned by finishing the first tier 5 and tier 10 charts, raise the limit through two more researches.

## Facts checked

- Vanilla `Limbo.generate` (`starfarer.api.zip`, `impl/campaign/world/Limbo.java`) adds a `LARGE_CACHE` named "Mothballed Equipment Cache" with memory `$limboWormholeCache` and salvage spec `limbo_wormhole_cache`. Its anchors and fuel are put in with `BaseSalvageSpecial.addExtraSalvage(entity, cargo)`, so a special item added to that same extra salvage comes out with them. Opening the cache sets `$player.gotWormholeCalibrationData` (rules.csv `limboWormholeCacheBegin2`).
- VoK reads `itemReqToResearch` as `id:count` pairs split by commas (`ResearchOptionSpec`), so `"research_databank:5,exiledSector_wormhole_blueprint:1"` works. `AoTDFactionResearchManager.retrieveAmountOfItems` counts commodities and special items, but special items only as `new SpecialItemData(id, null)` in the Research Facility submarket. The new items must therefore carry null data, and the player has to store them in a Research Facility like databanks.
- VoK's requirement panel (`ResearchInfoUI.populateReqItems`) draws `getSpecialItemSpec(id).getIconName()`, so the new items need a real icon path in `special_items.csv`, not the `graphics/fx/empty.png` our socketables use.
- VoK's tier 0 holds only Industrial Base Template (column 0, row 1) and Rudimentary Working Equipment (0, 4), both with `reqToResearchFirst` = `none`. Row 6 is free in tier 0 and tier 1.
- `AoTDMainResearchManager.isResearchedForPlayer(String)` exists alongside the `isAvailableForThisMarket` we already call.
- Star Chart icons already band by tier: white for 1 to 5, the default (yellow) for 6 to 10, red for 11 and up (`StarChart.LAST_LOW_TIER`, `FIRST_HIGH_TIER`). The upgrade tiers line up with those bands.
- `ChartObjectives.progressed` is where a run's objective completes (it calls `AtlasProgress.completed` and `ChartRewards.grant`). `AtlasState` keeps a `clearedTiers` set. Chart opening goes through `AtlasOpening.check`, used by `ChartRunActions`, `StarChartBench` and `AtlasNodeText`.

## Items

Three special items in `data/campaign/special_items.csv`, star-charts domain, null data, stack size 1, real icon paths:

- `exiledSector_wormhole_blueprint`, "Wormhole Tech Blueprint". Icon: the wormhole anchor art, core white.
- `exiledSector_gateway_upgrade_advanced`, "Gateway Array Upgrade". Core yellow (the tier 6 to 10 chart colour).
- `exiledSector_gateway_upgrade_perfected`, "Gateway Nexus Upgrade". Core red (the tier 11+ chart colour).

Names and descriptions go through the star-charts string catalogue like the other chart items, with zh_CN drafts. Descriptions are flavour only, no numbers.

Blueprint description draft: "Calibration routines, field geometry and fuel-feed schematics lifted from the Explorarium anchor cache in Limbo. Your science teams have spent weeks tracing how the Domain held a wormhole's far end steady, and believe they can build a lattice that does the same for a single set of coordinates. Not a gate. Nothing like one. But it opens."

Upgrade description drafts: Advanced, "Field readings taken on the far side of a deep chart. The lattice can be retuned to hold coordinates that drift further from known space." Perfected, "A full map of how the deepest charts fold. With it, the lattice can hold anything the charts can reach."

The tooltip line (not the description) says what each item does in the current setup: without VoK, "Right-click to unlock the Wormhole Gateway structure."; with VoK, "Store in a Research Facility to research the Wormhole Gateway." (and the upgrade equivalents). Wording follows `docs/tooltip_wording.md`.

### Icons

Done (2026-10-30): `tools/WormholeTechIcons.java` reads the user-supplied wormhole anchor art (`graphics/unused/wormhole_tech_item_source.png`, 80x80), turns its white and pale metal black and its purple core white (blueprint), yellow (Advanced) or red (Perfected), and writes the three cargo icons to `graphics/icons/cargo/`.

### Research tree icon

The current tree icon (`graphics/icons/tech/exiledSector_wormhole_gateway.png`) is the painted scene, which doesn't fit VoK's tree. VoK's icons are 320x320 flat glyphs on transparency: a thick white ring open at the lower left, a white symbol inside, one accent colour (purple for gate tech), and sometimes a small badge at the top left. The new icon follows that: two white pillar silhouettes with a purple vortex between them. The Advanced and Perfected researches get the same glyph with a larger vortex. Option sheet first, as with the item art. The painted file stays in `graphics/unused/`, since the item icons are made from it.

### Structure images

Done (2026-10-30): `graphics/icons/industry/exiledSector_wormhole_gateway_advanced.png` (small wormhole) and `_perfected.png` (large wormhole). Each has a vortex between the two towers, in the colours of vanilla's wormhole: black centre, deep blue rim, white, pink and lavender spiral arms, and a blue-violet haze. Each was painted on the 1920x1080 source and shrunk with the same crop as the base icon (0, 60, 1920, 960). Only the difference was added to the base icon, so everything outside the wormhole matches it pixel for pixel. The full-size paintings are in `graphics/unused/Wormhole_gateway_advanced.png` and `_perfected.png`.

The upgrades are separate structures, built with vanilla's Upgrade button like Heavy Industry to Orbital Works: `exiledSector_wormhole_gateway` upgrades to `_advanced`, which upgrades to `_perfected` (`industries.csv` upgrade and downgrade columns), each with its own image, cost and build time (placeholders: cost mult 150 and 200, 90 and 120 days). All three use the `WormholeGateway` plugin. An upgrade is available only with VoK, once its research is done, from the structure before it; the industry picker never offers them as new builds, since it skips industries with a downgrade. The research rows reward the upgraded industry. One gateway per faction, the docked check, the atlas option and the gateway system lookup count all three ids.

## Step 1: the Limbo drop

- New `LimboBlueprintCache` in the star-charts domain (package `exiledsector.charts.wormhole`). On new game and on load, it finds the entity with `$limboWormholeCache` and, if our memory flag `$exiledSector_blueprintPlaced` isn't set, adds the blueprint to its extra salvage (`BaseSalvageSpecial.getExtraSalvage`, add, `setExtraSalvage`) and sets the flag.
- Saves where the cache is already gone or looted: if `$player.gotWormholeCalibrationData` is set and the blueprint was never placed or granted, the blueprint goes straight into the player's cargo with a campaign message.
- No Limbo cache in the sector (a Nexerelin random sector, or a mod that removes Limbo): the blueprint goes into the player's cargo, with a campaign message, once `JanusDevice.obtained` is true. The same placed-or-granted flag stops it granting twice.
- Hooked in through the star-charts domain's lifecycle hook, skipped while the domain is switched off.

## Step 2: the unlock without VoK

- `WormholeGateway.isUnlockedFor` without VoK reads a new persistent sector flag `$exiledSector_gatewayUnlocked` instead of `JanusDevice.obtained`. The "Disable Game State Unlock Conditions" bypass (`ignoresMemoryFlagUnlocks`) still opens it.
- The blueprint's plugin implements `hasRightClickAction`, `performRightClickAction` (sets the flag, shows a campaign message) and `shouldRemoveOnRightClickAction`. With VoK enabled, `hasRightClickAction` is false.
- `chart.gateway.unavailable.janus` becomes `chart.gateway.unavailable.blueprint`: "Requires the Wormhole Tech Blueprint." (en and zh_CN).
- Without VoK there is no tier limit and the upgrade items never drop.
- Gateways already built in a save keep working; only the build check changes. `JanusDevice` stays, since the anchor gate still uses it.

## Step 3: the VoK research tree

`data/campaign/aotd_tech_options.csv`:

| id | tier | column, row | requires | items | reward line |
|---|---|---|---|---|---|
| `exiledSector_tech_wormhole_gateway` (existing id, moved) | 0 | 0, 9 | `none` | `research_databank:5,exiledSector_wormhole_blueprint:1` | `exiledSector_wormhole_gateway:industry` |
| `exiledSector_tech_wormhole_gateway_advanced` | 3 | 1, 9 | `aotd_tech_janus_device_analysis` (Janus Device Analysis, tier 3) and the base research | `research_databank:10,exiledSector_gateway_upgrade_advanced:1` | modifier text: charts up to tier 10 |
| `exiledSector_tech_wormhole_gateway_perfected` | 5 | 1, 9 | Advanced | `research_databank:15,exiledSector_gateway_upgrade_perfected:1` | modifier text: every chart tier |

- The base research drops the `$aotd_experimental_tier` other requirement. Keeping the id means saves that already researched it stay researched.
- All three sit on their own row, 9, below VoK 5.0.7's lowest row (Aquatic Biosphere Harvest, row 8): the gateway in tier 0 column 0, Advanced under Megastructure Analysis (tier 3 column 1), Perfected under Bifrost Gate (tier 5 column 1). Re-check the row after a VoK update. Research times are placeholders.
- The upgrades also list the previous gateway research, so the tier limit always rises 5, 10, then every tier.
- The three researches use the VoK-style tree icons described under "Research tree icon".

## Step 4: the tier limit with VoK

- `VaultsOfKnowledgeCompat` gains `isResearchedForPlayer(String id)` through a new `ApiMember`, failing open (treated as researched) like the existing lookup, and `missingFeatures` lists it.
- New `GatewayTierLimit` (`charts.wormhole`): `maxTier(SectorAPI)` returns `ChartTiers.CORRUPTED_TIER` without VoK, otherwise reads the player's built gateway: 5 for the base, 10 for Advanced, every tier for Perfected. Researching an upgrade does nothing until the structure is upgraded.
- `AtlasOpening` gets a new blocker `GATEWAY_TIER_LIMIT`, checked when a chart is given and `chart.tier() > maxTier`. Its reason (`chart.atlas.blocked.gateway_tier_limit`): "Your Wormhole Gateway can only hold charts up to tier {tier}." The bench, the atlas node text and `ChartRunActions` already show and enforce blockers, so they pick it up. Corruption raising a chart above the limit is allowed; the chart just can't be opened until the upgrade.
- Console commands stay unlimited.
- Charts open only on an atlas system. `StarChartBench` refuses an open with no atlas cell or skirmish entry ("Pick a system in the Outer Reach Atlas."), and the old open-from-store path (`StarChartOpening.FROM_STORE`) is gone. `ChartRunActions.openFromStore` stays only for the `ChartOpenStored` console command.

## Step 5: the upgrade drops

- New `GatewayUpgradeRewards` (`charts.objective`), called from `ChartObjectives.progressed` after `ChartRewards.grant`, only with VoK enabled. A completed run of tier 5 or more grants Advanced once (flag `$exiledSector_gatewayAdvancedGranted`); tier 10 or more grants Perfected once. Both can fire from the same run.
- The upgrades never drop as loot. A boss or salvage drop can be missed, left behind or lost in a full hold, and then the gateway could never be upgraded. Each grant instead adds the item straight to the player's cargo and queues a popup: a small `InteractionDialogPlugin` shown by an `EveryFrameScript` once no dialog, menu or combat is open. It shows the item icon, a short text ("Your science teams have pieced together field readings from the chart. Store the Gateway Array Upgrade in a Research Facility to research it.") and one confirm option. The queue is transient-safe: the pending grant lives in a memory flag, so a save made before the popup shows still shows it on load.
- Load migration with VoK: if `AtlasState`'s cleared tiers already include 5+ or 10+ and the matching item was never granted, grant it (with the popup). Saves that already run high-tier charts aren't stuck.

## Step 6: text and docs

- Strings for items, tooltip lines, blocker, popups, and the two research reward lines, in `data/strings/exiledSector/starCharts/en.json` and `zh_CN.json`.
- `docs/docs.md`: the Wormhole Gateway paragraph says it unlocks with the Limbo blueprint, or through VoK research with the tier limit and upgrades, naming `WormholeGateway`, `GatewayTierLimit` and `GatewayUpgradeRewards`.
- `README.md` and `docs/forum_post.bbcode` (kept in sync): replace the Janus Device lines for the gateway (README line 286, forum post lines 18 and 211). The anchor line (README 273, forum 200) stays, since anchors still wait for Janus.

## Tests

- `WormholeGatewayTest`: closed without the flag, open with it, open with the bypass; with VoK, research still decides. The Janus test changes to the blueprint.
- Blueprint plugin: right-click sets the flag and consumes the item without VoK; no right-click action with VoK.
- `LimboBlueprintCache`: adds the blueprint to the cache's extra salvage once; grants to cargo when the cache was already opened; does nothing twice.
- `GatewayTierLimit`: 16 without VoK, 5 / 10 / 16 by research, fails open on a lookup failure.
- `AtlasOpening`: tier 6 chart blocked at limit 5, tier 5 allowed, null chart never blocked by the limit.
- `GatewayUpgradeRewards`: tier 4 grants nothing, tier 5 grants Advanced once, tier 10 grants both on a fresh save, nothing without VoK, migration from cleared tiers.
- `VaultsOfKnowledgeCompatTest`: the new member resolves and is listed in `missingFeatures` when absent.
- A CSV check that the three tech rows parse with VoK's `id:count` format and reference existing item ids.

## Decisions

- Research only unlocks each gateway upgrade; the structure has to be upgraded, using the wormhole structure art (2026-10-30).
- The upgrades go straight into cargo with a confirm popup, never as loot, so they can't be missed or lost before the player has them (2026-10-30).
- Databank costs: 5 for the gateway research, 10 for Advanced, 15 for Perfected (2026-10-30).
- VoK tree: the three researches sit on their own bottom row. The gateway needs nothing; Advanced needs Janus Device Analysis (which itself needs Interstellar Logistics) and the gateway; Perfected needs Advanced (2026-10-30).
- The atlas is the only way to open a chart; the bench's open-from-store path is removed (2026-10-30).
- Item icons are the wormhole anchor art with white parts black and the purple core white, yellow or red (2026-10-30).
- Sectors without the Limbo cache grant the blueprint with the Janus Device (2026-10-30).
- The Neutron Star anomaly stays as it is. A player who opens a chart before getting Transverse Jump learns that the hard way (2026-10-30).

## Progress

1. Done: items in `special_items.csv` (`exiledsector.items.WormholeTechItems`, `WormholeTechItemPlugin`). `tools/WormholeTechIcons.java` writes the sheets (`docs/plans/art_options/wormhole_tech_item_icons.png`, `wormhole_tech_tree_icons.png`) and installs with `--install --tree N`. Item icons are final, recoloured from the wormhole anchor art; tree variant 1 is installed as a placeholder until the user picks. The painted tree icon is kept as `graphics/unused/Wormhole_gateway_tech_icon.png`. The tree glyph's ring opens at the upper left, as VoK's own icons do. Structure images done, picked by `WormholeGateway.getCurrentImage`.
2. Done: Limbo drop and migration in `charts.wormhole.LimboBlueprintCache`, called from `StarChartsDomain.afterSocketCustody`; a transient daily watch covers the Janus Device fallback.
3. Done: `WormholeGateway.isUnlockedFor` reads `$exiledSector_gatewayUnlocked`, set by the blueprint's right-click.
4. Done: three rows in `data/campaign/aotd_tech_options.csv`, tree icons registered in `data/config/settings.json`.
5. Done: `charts.wormhole.GatewayTierLimit`, `VaultsOfKnowledgeCompat.isResearchedForPlayer`, `AtlasOpening.Blocker.GATEWAY_TIER_LIMIT`. Blocker reasons are plain keys, so the reason has one key per limit (`.advanced` for tier 5, `.perfected` for tier 10).
6. Done: `charts.objective.GatewayUpgradeRewards` (grants, pending queue, load migration), `GatewayUpgradeNotice` (transient script) and `GatewayUpgradeDialog`.
7. Done: star-charts catalogue strings (en, zh_CN draft), `docs/docs.md` Wormhole Gateway section, README and forum post. The VoK research names and reward lines stay English in the CSV, since VoK has no way to translate them.
