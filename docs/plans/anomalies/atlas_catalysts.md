# Atlas Catalysts

Atlas Catalysts are one category of consumable item: each adds one anomaly to a system on the Outer Reach Atlas. You pick a catalyst on the atlas, click a system, and the anomaly is there until a run spends it. Catalysts come in themed families with their own names and art, but every one works and drops the same way.

Status: built 2026-10-28 apart from the in-game checks of steps 3 to 8; move this plan to `completed/` once those are done. Proposed 2026-10-28. This is the player's way of inserting anomalies that `../atlas.md` (Anomalies, step 17) leaves for later. See `overview.md` for the shared framework.

## Lore

The factions are pushing into the Outer Reaches through wormholes, and the Gateway is how you follow them: it holds a system's coordinates steady long enough to open a way there. A catalyst is something the Gateway can lock onto alongside those coordinates. Feeding one into the projectors changes what is waiting at the far end. Each family explains what that something is:

- Unstable Termini are burnt-out wormhole anchors, salvaged from the factions' careless early pushes. Locked onto a system, the terminus opens and fails, and the collapse scars the system.
- Explorarium Surveys are Domain survey records recovered in the Outer Reaches. They create nothing. They show where something old lies, and the Gateway plots the way to it.
- Transit Orders are a faction's intercepted wormhole transit orders. Rewritten with a system's coordinates, they send that faction's next push to the system you chose.

Star anomalies have no catalyst: not even a failing terminus can turn a star into a black hole.

## The items

Each catalyst is one special item (a `special_items.csv` row, like the nanoswarms, ciphers and engrams) and one row in `data/config/exiledSector/atlas_catalysts.csv` (`id,anomaly,family,weight,min_tier,note`). The family sets the item's effects and flavour; the anomaly sets what it inserts and its icon. Starting rows, all names placeholders:

| Family | Item | Anomaly |
|---|---|---|
| Unstable Terminus | Breach Terminus | Spatial Breach |
| Unstable Terminus | Shattered Terminus | Asteroid Storm |
| Unstable Terminus | Magnetised Terminus | Magnetic Storm |
| Unstable Terminus | Flare Terminus | Stellar Flares |
| Unstable Terminus | Wreck Terminus | Anomalous Debris |
| Unstable Terminus | Storm Terminus | Storm Front |
| Explorarium Survey | Bolt-hole Survey | Excavation Sites |
| Explorarium Survey | Relay Survey | Relay Network |
| Explorarium Survey | Forge Survey | Derelict Forge |
| Transit Orders | Convoy Orders | Supply Convoy |
| Transit Orders | Assault Orders | Faction Skirmish |
| Transit Orders | Picket Orders | Listening Post |
| Transit Orders | Flagship Orders | Powerful Boss |
| Transit Orders | Redeployment Orders | none: changes the system's faction (see Redeployment Orders) |

A row whose anomaly isn't insertable, is unknown or needs a missing mod is skipped with a load problem. Asteroid Storm, Magnetic Storm and Stellar Flares become `insertable` in `chart_anomalies.csv`, as `../atlas.md` already allows. The Storm, Forge, Picket and Assault rows were built as a step of each anomaly's own plan (`storm_front.md`, `derelict_forge.md`, `../completed/anomalies/listening_post.md`, `faction_skirmish.md`), and Flagship Orders with Powerful Boss (`../atlas.md` step 19).

The tooltip names the category ("Atlas Catalyst"), says what it adds ("Adds a Spatial Breach to a system on the Outer Reach Atlas"), and carries the family's flavour, per `docs/tooltip_wording.md`.

## Icons and effects

A catalyst's icon is its anomaly's atlas marker icon, read from the `icon` column of `chart_anomalies.csv` (the circular art in `graphics/icons/anomalies/`), never a copy, so a catalyst always looks like the marker it puts on the map. A catalyst whose anomaly has no icon yet (Storm Front, Derelict Forge and Listening Post until their plans add theirs) is skipped with a load problem until the icon exists.

Catalysts are animated like the doctrines (`DoctrineIconFx`), the Modspec Cache (`CacheIconFx`) and the currency items (`CargoIconFx` with its `SwarmMotion`s), but must look like none of them. They may reuse the same texture files and the shared drawing code, but never with the colours and patterns another item type uses: no orbiting swarm specks, rotating rings and ticks or sparkle bursts as the currency items draw them, no screen or stamp sequence like the doctrines, no cracking rock like the Modspec Cache. The effect is an `IconArt` returned by a catalyst item plugin over `AnimatedCargoItemPlugin` (as `MaterialItemPlugin` is, overriding `cargoLook` and `cargoIconPath`), so the cargo grid, tooltips and the Catalysts window all draw it the same way. Every catalyst shares one category element, so the three families read as one kind of item, and each family adds its own motion that says what it does:

- Unstable Termini: a terminus straining to open around the icon and failing, the collapse leaving the icon scarred for a moment.
- Explorarium Surveys: a survey pass resolving the icon out of the dark, as if a sensor sweep is finding what was always there.
- Transit Orders: wormhole traffic, fleets or transit streaks passing through the icon.

Before building, the agent opens the vanilla game's `graphics/fx/` directory (`E:\Dev\Starsector\starsector-core\graphics\fx\`) and looks through the textures there, as `CacheIconFx` already uses vanilla's `glow64`, `emp_arcs`, `starburst_glow1` and `smoke32`. It picks textures whose look matches each family's theme and the shared category element (a wormhole or warp texture for termini, a scan or sensor texture for surveys, a streak or jump flash for orders), and gives the user a short list of each choice and how it would move, for the user to approve before it's built. Textures are preloaded like every other chart texture. The effects follow the Fast Rendering rules (GL11 only, render thread only, no `glGet` while drawing), set their own blend state per sprite, and honour the Item Animation setting (Full, Calm, Off) like every animated icon.

## Using one

The atlas screen has nothing for this yet (it has only Close, Faction Territories and Biomes), so it gets a new window. A Catalysts button beside Faction Territories and Biomes, showing a count of the catalysts in the player fleet's cargo, opens and closes a Catalysts window over the atlas, built like the skill tree's windows: a `HoloPanel` placed through `SocketPanelPlacement`, as the subroutine storage window (`ItemStoragePanel`) and the Star Charts storage window (`StarChartStoragePanel`) are. `HoloPanel` is package-private, so the window lives in `exiledsector.ui.socket` in the star charts module, beside `StarChartStoragePanel`. The atlas stays live behind it, so you can pan and zoom while it's open.

The window shows every catalyst in cargo as an icon with its stack count and the item tooltip, grouped by family. Clicking one selects it and puts the atlas into targeting mode:

- Every revealed system is marked usable or refused. Hovering a refused one says why.
- Clicking a usable system asks for confirmation, naming the anomaly and the system. Confirming spends the item and calls `AtlasAnomalies.insert` with `Source.PLAYER`, and the anomaly's marker appears on the system.
- Esc or right-click leaves targeting mode without spending anything, and a second Esc closes the window. Clicking another catalyst in the window switches to it.
- After a catalyst is spent, the window stays open with the counts updated, and the same catalyst stays selected while you have more of it.

The check runs before the item is spent, so a refused insertion never costs a catalyst.

## Where one can go

Any revealed system, reachable or not, so you can seed systems ahead of your route; a fogged system has no coordinates to lock onto. Everything else is `AtlasAnomalies.canInsert`, with each refusal shown in plain words:

| Check | Refusal |
|---|---|
| `NO_SYSTEM` | There's no system here to lock onto. |
| `COMPLETED` | This system's coordinates are spent. |
| `OPEN_RUN` | The Gateway is already holding this system's coordinates. |
| `ALREADY_PRESENT` | This system already holds that anomaly. |
| `CONFLICT` | Names the anomaly already there that it can't share the system with. |
| `DOES_NOT_FIT` | The anomaly's own reason, from its `insertVerdict` (Assault Orders: no neighbouring faction can attack this system). |

A catalyst never goes into a system that already holds its anomaly, whether rolled or inserted, and never strengthens one. Strengthening an anomaly is left to other sources.

An inserted anomaly then behaves like any other: the next run there takes it, and closing that run spends it.

## Shared use code

Using a catalyst on a system is the same act as using a currency on an item: a consumable in cargo, a target, a check that either allows it or gives a reason, and an apply step that spends the consumable only when it succeeds. Today each target type does this its own way:

- Hull items and subroutines: `ItemCurrency.canUse` and `use`, run through `ItemCrafting.canUse` and `use` (which counts the item in `CraftingMaterials`, spends it and any Inhibitors only on success), with refusal notices worked out inside `SocketWorkbenchPanel`, and bench modes reporting through `BenchModeSupport.Refusal` and `Use`.
- Star Charts: the same `ItemCrafting` path, wrapped by `StarChartCrafting.refusal` (its own `Refusal` enum) and `use` (its own `Used`).
- Atlas systems: `AtlasAnomalies.canInsert` (`InsertCheck`) and `insert` (`Insertion`).
- Socketing: `AllocationGate` verdicts for subroutines in the tree, `HullUpgradeRules.canSocket` for hull items in sections. Socketing moves an item rather than spending one.

Catalysts don't add a fifth way. The first build step extracts one abstraction in core (`exiledsector.items`, since it is item framework, not something a reusing mod would call) and moves the existing paths onto it before any catalyst exists:

- A consumable applied to a target of type `T`: its item id, a check returning a verdict (allowed, or a refusal with its translation key), and an apply step.
- One use routine that checks the count in `CraftingMaterials`, checks the verdict, applies, and takes the consumable (and any Inhibitors it spent) only on success. It replaces the count-check-apply-take logic in `ItemCrafting.use` and `StarChartCrafting.use`.
- `ItemCurrency` becomes the `Item` case, `StarChartCrafting`'s refusals map onto the shared verdict instead of their own enum, and `BenchModeSupport.Refusal` is built from it. A catalyst is the `AtlasCell` case, wrapping `canInsert` and `insert`, with the `InsertCheck` refusal texts as its verdict keys.
- Socketing adopts the shared verdict for its refusals and tooltips, so every "why can't I put this here" in the mod reads the same way. It keeps its own apply path unless mapping the code shows the overlap goes further, because nothing is spent.

The agent first maps all four paths and the UI that drives them, then settles the exact shape, keeping whatever reads most simply over a forced fit. Persisted classes and enums keep their names (`CraftingCurrency`, `StarChartCurrency` and anything `ItemCodec` reads), and existing crafting, Star Chart and socketing tests must pass unchanged apart from moved call sites.

## Factions

Transit Orders carry no faction of their own. An anomaly an Order inserts takes the system's faction, as the anomaly does when the atlas rolls it: the Supply Convoy and the Listening Post fly the system's owner, and the Faction Skirmish defends with the owner and picks its attacker from the neighbours as usual.

## Redeployment Orders

Redeployment Orders (placeholder name, Transit Orders family) insert no anomaly. They hand the system to a random other faction that could hold a system in its biome: the region's faction weights from `atlas_regions.csv`, or the territory pool for a region that keeps the territories, without the current owner, weighted as the atlas weights them. The change is a capture record (`AtlasState.capture`), so the owner shown on the map, the chart's faction, borders and fleet traffic all follow it, and a later Faction Skirmish capture replaces it like any other. The new faction is seeded per system (the atlas seed, the cell and the current owner, raced like the atlas's own faction picks), so reloading never rerolls it and no new state is saved, and a second Order on the same system moves it on from the new owner. The window names the new faction after use.

It uses the same window, targeting and spend as the anomaly catalysts, with its own consumable beside `AtlasCatalystUse` and its own refusals: a fogged, completed or open-run system as for anomalies, no system, the Gateway and special sites (which hold no faction), an infested system (the infestation holds it), and a biome with no other faction to take it. Its row in `atlas_catalysts.csv` names an effect in place of an anomaly, and its icon is its own, picked by the user from an option sheet of vanilla art before it's built. Its tooltip says what it does ("Hands a system on the Outer Reach Atlas to another faction of its biome").

## Drops

Catalysts drop like crafting materials: every chart source that rolls crafting materials (salvage in chart systems, defeated chart fleets, chart cargo and objective rewards) also has a chance of one catalyst, picked among all loaded catalysts by `weight` from those whose `min_tier` the run's tier reaches. Families make no difference to where a catalyst drops. The chances are placeholders, set beside the nanoswarm kernel chance.

A new `CATALYST` drop type gets its own chart modifier, Catalyst % (`CHART_CATALYST_PERCENT`), which scales catalyst drops the way Currency % scales nanoswarms; the general chart quantity lines apply as well. Catalysts never drop outside the Outer Reaches.

## Build order

1. [x] Shared use code: map the hull item, Star Chart, atlas and socketing paths, extract the consumable, verdict and use routine in core, and move the existing currencies and `StarChartCrafting` onto them and socketing refusals onto the verdict, with existing tests passing. Built: `UseVerdict` (allowed, or a refusal key plus args, rendered with `{name}` for the thing used and `{target}`), `Consumable` and `Consumables.use` in core `exiledsector.items`; `ItemCrafting.verdict` holds the workbench reason chain and `ItemCrafting.use` runs through a private `CurrencyUse`; `StarChartCrafting` returns verdict constants in place of its enum; `BenchModeSupport.Refusal` wraps a verdict; `SubroutineSocketLimits`, `WormholeAnchorSocketLimits` and `WormholeAnchor.socketRefusal` return verdicts that `TreeAllocationSession` renders. Left as they were: `AllocationGate` (node allocation, not putting an item somewhere) and `HullUpgradeRules.canSocket` (no hull item refusal is shown anywhere). Tests: `ConsumablesTest`.
2. [x] `atlas_catalysts.csv` and its loader (`exiledsector.charts.atlas`), with load problems for unknown, non-insertable, mod-missing and iconless anomalies; the special item rows, the catalyst item plugin with the anomaly's icon, and tooltips; `insertable` set for Asteroid Storm, Magnetic Storm and Stellar Flares. Built: `AtlasCatalysts` (loaded after `ChartAnomalies`), twelve special item rows, `AtlasCatalystItemPlugin` drawing the anomaly icon in a plain family-coloured frame until step 3, `AtlasCatalystItems` (family colours, tooltip line, name localisation) and `catalyst.*` strings. Tests: `AtlasCatalystsTest`. Convoy Orders loads since the Supply Convoy got its icon (a circular crop of vanilla's `freighters` illustration); the Storm, Forge, Picket and Assault rows moved to their anomalies' plans.
3. [x] Effects: survey vanilla's `graphics/fx/`, get the user's approval of the textures and motions for the shared element and each family, then build the catalyst `IconArt`, with the Item Animation setting and Fast Rendering rules. Built: `CatalystLook` and `CatalystIconFx` (`exiledsector.ui.cargo`, star charts module), textures preloaded from `StarChartsDomain.onApplicationLoad`. Shared: corner brackets closing in over a tinted `wormhole_corona`. Termini: `shields128ringc` strains, snaps into an `explosion_ring0` collapse, then a `dust_clouds_colorless` scar. Surveys: a `scanline_large` sweep brightens the dimmed icon from the top, under a faint `shimmer`. Orders: `contrail64b` streaks with `hit_glow` jump flashes. Calm halves the speed, holds the brackets and drops the collapse, scar, shimmer and flashes. Tests: `CatalystIconFxTest`.
4. [x] The Catalysts window: the button with its count on `AtlasScreen`, the window opening and closing as a `HoloPanel` through `SocketPanelPlacement`, catalysts grouped by family with counts and tooltips, and selection. Built: `AtlasCatalystPanel` and `CatalystTile` (`exiledsector.ui.socket`, star charts module), the Catalysts toggle on `AtlasScreen` (opening a system's bench closes the window; Esc drops the pick, then closes the window), and `AtlasCatalystItems.carried`. The selection is held on `AtlasScreen` for step 5. Tests: `AtlasCatalystItemsTest`.
5. [x] Targeting mode in `ui.atlas`: usable and refused markers on `AtlasMapCanvas`, refusal tooltips, confirmation, spending the catalyst through the shared use routine, and the window refreshing after. Built: `AtlasCatalystUse` (`charts.atlas`, the `AtlasCell` consumable with `catalyst.refused.*` verdicts; fogged systems refuse as well), `AtlasMapCanvas.target` with green and red rings, the verdict line in system tooltips, and the confirm and spend in `AtlasCatalystPanel`. `DOES_NOT_FIT` shows one generic line until an anomaly with its own fit rule (the Faction Skirmish's border check) gives its own reason. Tests: `AtlasCatalystUseTest`, refusal keys in `StarChartUiStringKeysTest`.
6. [x] Drops: the catalyst roll on every chart source that rolls crafting materials, the `CATALYST` drop type and the Catalyst % chart modifier (row in `star_chart_modifiers.csv`, strings). Built: a `catalyst_chance` column in `chart_drops.csv` (run_reward 0.25, system_salvage 0.03, chart_fleet 0.04, chart_boss 0.25, placeholders; a load problem zeroes it outside chart systems), rolled in `ChartDrops.roll` after the chart currency through `AtlasCatalysts.pickDrop` (by weight, `min_tier` against the run tier); `ChartDrops.Mults` carries the chart, currency and catalyst multipliers, and `Loot.materials()` adds catalysts to cargo wherever chart currencies go. `DropType.CATALYST` and `ChartEffect.CHART_CATALYST_PERCENT` ("Catalysis" suffix, weight 4 in the chart suffix pool, placeholder) scale it alongside the quantity lines; at run rewards Catalyst % multiplies the reward multiplier. Tests: `ChartDropsTest`.
7. [x] Redeployment Orders: the user picks the icon from an option sheet; the effect column and icon override in `atlas_catalysts.csv`, the faction-change consumable with its refusals, the capture on use, the window, confirm and tooltip texts for it, the special item row, strings (en and zh_CN) and tests. Built: icon `graphics/icons/catalysts/redeployment_orders.png`, a circular crop of vanilla's `entering_hyperspace` illustration; `effect` (`anomaly` by default, or `redeploy`) and `icon` columns read by `AtlasCatalysts` (a redeploy row needs an icon); `AtlasCatalystUse` is now a sealed interface returning an `Outcome` (the window's notice), with `AnomalyCatalystUse` (the step 5 logic) and `RedeploymentCatalystUse`, which takes the pool from `AtlasGenerator.factionPool` (`AtlasTerritories.factionPool`), drops the current territory owner, races the rest on the atlas seed, the cell and the owner, and writes `AtlasState.capture`; the window, confirm and system tooltip ask the use for their texts. Tests: `RedeploymentCatalystUseTest`, `AtlasCatalystsTest`, `AtlasCatalystUseTest`, keys in `StarChartUiStringKeysTest`.
8. [x] A console command to grant catalysts, strings (en and zh_CN), the `docs/docs.md` section and tests. Built: `ExiledSectorGrantAtlasCatalysts [copies] [catalyst id]` (`GrantAtlasCatalystsCommand`, registered in `data/console/commands.csv`), granting 1 to 100 of every loaded catalyst or of one named with or without the `exiledSector_` prefix; the "Atlas Catalysts" section of `docs/docs.md`; strings were added with each step. Tests: `GrantAtlasCatalystsCommandTest`, and `AtlasCatalystsTest` now checks every catalyst has a `special_items.csv` row.
9. [x] Flagship Orders (Transit Orders), inserting Powerful Boss once it became an anomaly (`../atlas.md` step 19). Built: the `exiledSector_flagship_orders` rows in `atlas_catalysts.csv` and `special_items.csv` (order 937) and name strings (en and zh_CN); it shows Powerful Boss's placeholder icon. Tests: `AtlasCatalystsTest`, `PowerfulBossTest`.

## Open questions

- The category name "Atlas Catalyst" and every item name are placeholders.
- Whether seeding unreachable systems is too strong, letting players pile anomalies on a far system before reaching it.
- Whether the Catalysts window needs filters once there are many rows, or the atlas's search bar should find systems a picked catalyst can go into.
- Whether catalysts should also be sold anywhere, or stay drops only.
