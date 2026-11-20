# Atlas Tree nodes

Node types for the Atlas Tree. Every type listed under Built is in `data/skilltrees/atlas_tree_types.json`, ready to place on the tree in the editor's Atlas Tree page; none is placed yet. Everything else is an idea to argue over, not a commitment. The tree's shape and rules are in `completed/atlas_skill_tree.md`. Magnitudes and icons are placeholders, and a line marked new needs an effect that doesn't exist yet.

Building since 2026-11-16: the six anomalies with seven or more notables (Spatial Breach, Relay Network, Supply Convoy, Anomalous Debris, Excavation Sites and Depot), each on its own branch, with their chance nodes and the tree's own line text on a shared one. The other anomalies' nodes stay off for now.

This plan took in `atlas_tree_anomalies.md` on 2026-11-15: its tooltip rules, its Spatial Breach ideas and its section per anomaly now live here.

## Tooltip rules

Atlas Tree lines follow `docs/tooltip_wording.md`, and take their shape from Path of Exile's atlas passive tree (3.29). Where the two disagree, `tooltip_wording.md` wins.

Scope:

- Every line names where it applies, as Path of Exile's "in your Maps" does. A chart line says "your Star Charts": "Spatial Breaches in your Star Charts have 20% increased maximum radius". An atlas line names the atlas: "+1 lane of visibility around completed atlas systems".
- The subject comes first, then the change: "Breach caches in your Star Charts contain...", "Fleets charging Spatial Breaches in your Star Charts have...". A line about the chart as a whole opens with "Your Star Charts".
- Tree lines are their own text, not the chart's "in the system" wording, since a tree line describes every chart opened rather than the one in hand.
- The Atlas Tree tooltip shows `atlasTree.line.<EFFECT_ID>` (starCharts catalogue, with its plural forms and negative-value variant like the chart line's) when that key exists and the chart line `chart.modifier.<EFFECT_ID>` otherwise, built with the chart line's own arguments (`AtlasTreeNodeText`).

Shapes:

- Chance to hold an anomaly, added as percentage points: "Your Star Charts have +4% chance to contain a Spatial Breach".
- Change to an existing chance: "50% increased chance for Spatial Breaches in your Star Charts to...".
- Guaranteed count: "Your Star Charts contain an additional Supply Convoy". Conditional count: "Your Star Charts that contain a Supply Convoy have 10% chance to contain an additional Supply Convoy". Fixed count: "Your Star Charts with Relay Networks always have four relays".
- Quantity: "N% increased quantity of items found in breach caches in your Star Charts", "N% increased quantity of hull items dropped by Supply Convoys in your Star Charts".
- Property of an anomaly's part: "Spatial Breaches in your Star Charts have 20% increased maximum radius", "Spatial Breaches in your Star Charts stay open 30 additional seconds before collapsing".
- Time: "N% faster" and "N% slower" only when the value divides a time, and "N additional seconds" for flat time.
- Turning content off: "Your Star Charts have no chance to contain Spatial Breaches".
- Duplication and rerolls: "N% chance to be duplicated". A double roll is lucky, with its reminder line: "Hull items found in your Star Charts with Stellar Flares have 1 additional lucky modifier" over "(Lucky modifiers are rolled twice, keeping the best)".
- Guards: "X in your Star Charts are guarded by an additional fleet".
- Tier gates trail in brackets: "(tier 10+)".

Nodes:

- A small node is one line, or two short lines on the same anomaly. Its name is plain and says what it changes, such as "Breach Cache Quantity" or "Spatial Breach Chance".
- A notable is two or three lines and changes how the anomaly plays. Its name is evocative, two or three words, with no number. A drawback is stated flatly as one of its lines, never labelled as a cost.
- A keystone changes a rule outright, often with a matching drawback in the same tooltip: "Spatial Breaches in your Star Charts never collapse" beside a drain line.
- A term the player may not know gets a short bracketed reminder line under the stat, as Path of Exile's reminder text does.

## Travel nodes

Small nodes that make up the highways between volumes. They change with distance from the root.

### Built

| Distance | Type id | Name | Effect |
|---|---|---|---|
| Near | `travel_boss_star_charts` | Boss Star Charts | 2% increased quantity of Star Charts dropped by Star Chart Bosses (`CHART_BOSS_STAR_CHART_PERCENT`) |
| Further | `travel_catalysts` | Atlas Catalysts | 2% increased quantity of Atlas Catalysts found in the system (`CHART_CATALYST_PERCENT`) |
| Very far | `travel_chart_modifier_effect` | Star Chart Modifier Effect | 2% increased effect of Star Chart modifiers (`CHART_MODIFIER_EFFECT_PERCENT`) |

Increased effect of Star Chart modifiers scales every implicit and explicit modifier on a chart once it is opened, so 10% increased shield damage taken becomes 15% with 50% increased effect. Each prefix's hidden reward (set per prefix in `star_chart_prefix_rewards.csv`) is scaled too, so a prefix is a downside plus a reward that grow together. The bonus still shows only in the totals at the top of the chart tooltip.

### Ideas

## Anomaly nodes

The Atlas Tree makes every anomaly more rewarding or more likely to appear, one way or another. Each anomaly's nodes focus on the reward only it grants (`completed/anomalies/anomaly_rewards.md` and the anomaly's own plan): Excavation Sites on inhibitors, Supply Convoys on infused feedstock, and so on. They give more of the anomaly, more of its reward, or a change to how it grants it. An anomaly with no reward type of its own gets other effects instead. Each section names the reward, then lists small nodes, notables and, where one fits, a keystone. Nodes for an anomaly that needs a mod are hidden without it, as gated volumes are.

An anomaly marker on an atlas system adds one more of that anomaly to the system where that makes sense, such as one more anomalous fleet. Anomalies where it doesn't keep today's rules, and their sections say so.

### Chance nodes

Every anomaly's small nodes include one chance node: "Your Star Charts have +8% chance to contain a Spatial Breach". It adds 8 percentage points to the anomaly's `opening_chance` (0.08 for every common anomaly today), so one node doubles the chance. Type id `<anomaly id>_chance`, name "<Anomaly> Chance", one new chart line per anomaly read by `OpeningAnomalies` on top of the `chart_anomalies.csv` chance. The roll keeps the system's cap, conflicts, biome weights and `insertVerdict`, so the node never puts an anomaly where it can't go. The sections below list it as the chance node.

Some anomalies have no opening chance. The star anomalies (Black Hole, Neutron Star, Dense Nebula) and the Coronal Hypershunt replace a system's star, which is fixed to its atlas cell and shown on the atlas, so their chance node raises how often the atlas rolls them instead: "8% increased chance for Black Holes to appear on the atlas". It is an increase to the anomaly's base atlas chance (`atlas_anomaly_chances.csv`: 5% at the Gateway, rising 0.2% for each cell of distance to 15%, times the anomaly's weight), not 8 percentage points on top of it. Type id `<anomaly id>_atlas_chance`, name "<Anomaly> Atlas Chance", one new atlas line (`AtlasModifier`) per anomaly, read where `AtlasData.rules` builds each candidate's chance. These are the one exception to the tree plan's rule that anomaly chances are never atlas lines (`completed/atlas_skill_tree.md`, Atlas lines): like every atlas line, the map shows the change at once. The sections below list it as the atlas chance node.

The biome landmarks sit in exactly one system of every pocket of their biome, so their chance node raises how often the biome appears instead: "8% increased chance for Remnant Expanses to appear on the atlas". It is an increase to the biome's weight in `atlas_regions.csv` (with its weight per step), read where `AtlasData` registers the regions. Type id `<biome id>_biome_chance`, name "<Biome> Chance", one new atlas line per biome. The Remnant Nexus takes the Remnant Expanse's, the Domain-era Mothership the Derelict Graveyard's, and the Abyssal Shipyard and Abyssal Research Complex share the Abyssal Rift's, one node type listed in both sections. The sections below list it as the biome chance node.

Faction Skirmish has no chance node: the officer turns place it, and its nodes are bound to Atlas officers.

### Black Hole (`black_hole`)

Status: off for now (2026-11-16).

Reward: 6 to 9 derelicts around the hole, each with a 10% chance that its hull items drop with four modifiers, one of them immutable.

Small nodes:

- The atlas chance node.
- Black Hole Derelicts: "Black Holes in your Star Charts are ringed by 2 additional derelicts" (new, `StarAnomalies.scatterDerelicts`).
- Immutable Derelicts: "50% increased chance for derelicts around Black Holes in your Star Charts to hold immutable hull items" (new, scales `DERELICT_IMMUTABLE_CHANCE`).

Notables:

- Locked Wreckage: "Immutable hull items from derelicts around Black Holes in your Star Charts have 2 immutable modifiers".
- Deep Wreckage: "Derelicts around Black Holes in your Star Charts are guarded by an additional fleet" and "100% increased chance for them to hold immutable hull items".
- Lost Observatory: "One of the derelicts around Black Holes in your Star Charts is always a research station" (new). `StarAnomalies.scatterDerelicts` places the Neutron Star's research station (`RESEARCH_STATION`) as one of the derelicts, so it counts toward their number, gets their chance to hold immutable hull items and sits inside Event Horizon's breach.

### Neutron Star (`neutron_star`)

Status: off for now (2026-11-16).

Reward: a research station in orbit, in a system with at least 7 planets for cover. It has no reward of its own yet (`anomaly_rewards.md`, Unchanged), so its nodes stay on the station until it gets one.

Small nodes:

- The atlas chance node.
- Pulsar Station Quantity: "20% increased quantity of items found in research stations around Neutron Stars in your Star Charts" (new).

Notables:

- Twin Stations: "Neutron Stars in your Star Charts have an additional research station".

### Dense Nebula (`dense_nebula`)

Status: built on branch `atlas-nodes/dense-nebula` 2026-11-19, wiring only (not placed on the tree).

Reward: Settling Dust, a stack for each battle won in the system, each giving 10% increased boss star charts, up to 10 stacks.

The stacks are one run-level count in `SettlingDust` (`anomaly`), stored in the run's anomaly state under `settling_dust.stacks`. `ChartAnomalies.battleWon` adds a stack after the anomalies' own battle hooks, and the boss's Star Charts, Dust Trails, the chart intel's dust line and Settled Shafts all read that count. The Settling Dust lines are `ChartModifier` anomaly tuning lines read through `SettlingDustTuning`.

Small nodes:

- The atlas chance node. Built: type `dense_nebula_atlas_chance` (`ATLAS_DENSE_NEBULA_CHANCE_PERCENT`), an `AtlasModifier` carrying its anomaly id; `AtlasData.rules(AtlasEffects)` multiplies that anomaly's chance rule (its own row, or the shared anomaly row) by the increase, base chance, growth and cap alike. Black Hole and Neutron Star need only their own `AtlasModifier` constant.
- Settling Dust Effect: "Settling Dust in your Star Charts has 20% increased effect" (new, scales `STAR_CHART_PERCENT_PER_DUST` and Dust Trails' per-stack quantity). Built: type `settling_dust_effect` (`CHART_SETTLING_DUST_EFFECT_PERCENT`), read in `SettlingDust.bossStarChartPercent` and `quantityPercent`. It doesn't scale Settled Shafts, which counts stacks.
- Settling Dust Stacks: "+1 to maximum Settling Dust stacks in your Star Charts" (new, adds to `MAX_DUST`). Built: type `settling_dust_stacks` (`CHART_SETTLING_DUST_MAX_STACKS`), adding to `SettlingDust.BASE_MAX_STACKS`.

Notables:

- Dust Trails: "Settling Dust in your Star Charts also grants 5% increased quantity of items found for each stack" (new). Settling Dust Effect scales the 5% as it scales the boss's Star Charts. Built: type `dust_trails` (`CHART_SETTLING_DUST_QUANTITY_PERCENT`), added as item quantity to the run context in `ChartRunEffects.contextOf`, which refreshes when the stack count changes, so every loot roll, salvage and the chart's totals see it.
- Long Settling: "+5 to maximum Settling Dust stacks in your Star Charts" and "Enemy fleets in Dense Nebulae in your Star Charts have 3% more size for each Settling Dust stack". Built: type `long_settling` (`CHART_SETTLING_DUST_MAX_STACKS`, `CHART_SETTLING_DUST_FLEET_SIZE_MULT`). The tree line reads "Enemy fleets in your Star Charts have 3% more fleet size for each Settling Dust stack" (more, because it multiplies a starting size that already has the chart's Fleet Size %), since with Ionized Dust the storm charts gather stacks too. Fleets are generated before any stack exists, so `SettlingDustFleets` grows the live ones instead: each time a stack is added, every hostile, unlooted chart fleet in the system that drops items and isn't the boss or a station boss's defenders gains copies of its own non-flagship ships until it is the stack total's share larger than it started, and its ledger's starting fleet and deployment points rise with it.
- Ionized Dust (Magnetic Storm, Storm Front): "Battles won in your Star Charts with a Magnetic Storm or Storm Front grant Settling Dust stacks" (new). Those charts gather Settling Dust as a Dense Nebula does, and every Settling Dust line applies there. A chart with a Dense Nebula too still has one count of stacks, so a battle adds one stack, not two. The stacks live on the Dense Nebula's run state today (`DenseNebula.dust`), read only when the nebula is in the run, so they move to a run count that any of the three anomalies can add to and that chart intel shows in all three. Built: type `ionized_dust` (`CHART_SETTLING_DUST_IONIZED`), read in `SettlingDust.gathersIn`; the dust code moved out of `DenseNebula` into `SettlingDust`, and the Magnetic Storm and Storm Front classes are unchanged.

### Asteroid Storm (`asteroid_storm`)

Status: retired 2026-11-18 (`retire-asteroid-storm`, merged into `parallel-work`). It wasn't interesting enough to keep, so it gets no nodes and has left the game.

Retiring it:

1. [x] Remove the anomaly: its `chart_anomalies.csv` row, `AsteroidStorm` and its entry in `ChartAnomalies`, its case in `AtlasSystemArt`, its name and description strings (en and zh_CN), its atlas marker icon (`graphics/exiledSector/anomalies/asteroid_storm.png`), its entry in `tools/editor_vocabulary.json`, and the mentions in `anomalies/overview.md` and `atlas.md`. Tests that use it as a sample anomaly switch to another one. Built: also `AnomalyLayoutSite.outerRadius` and `angle()`, which only it used; tests now sample Magnetic Storm or Anomalous Debris, and `AtlasFingerprintTest` has the new atlas hash, so atlas systems that would have rolled it roll differently, as when Powerful Boss went.
2. [x] Retire its catalyst, the Shattered Terminus: its `atlas_catalysts.csv` row goes, while its `special_items.csv` row stays as a stub so a held one still loads, and a load sweep removes it from cargo and storage (save compatibility rule). Built: the row is a retired stub (`exiledSector_retired_id` tag, `BaseSpecialItemPlugin`), swept by `RetiredItems.removeFrom` from `ExiledSectorModPlugin.onGameLoad`; its catalyst name strings went, since the stub names itself. A Shattered Terminus left switched on is skipped and switched off by `ActiveCatalysts`. Tested in `AtlasCatalystsTest`.
3. [x] Sweep old atlases on load: an Asteroid Storm marker on an atlas system is dropped, and the system keeps its other anomalies. A run open in such a system needs nothing, since run state isn't save compatible. Built: `asteroid_storm` joins `AtlasState.REMOVED_ANOMALY_IDS`, dropped by `AtlasState.repaired()`; tested in `AtlasAnomaliesTest.loadingDropsRecordsOfRemovedAnomalies`.

### Magnetic Storm (`magnetic_storm`)

Status: built on branch `atlas-nodes/magnetic-storm` 2026-11-19, wiring only (not placed on the tree). The types are in `atlas_tree_types.json`; Ion Front and Geomagnetic Surge read their lines once per run through `MagneticStormTuning` (`anomaly.magneticstorm`). `MagneticStorm` itself stays in the `anomaly` root so the other anomalies' branches keep compiling; it can move into `anomaly.magneticstorm` once they are merged.

Reward: 20% increased quantity of crafting items found in the system, behind magnetic fields that blind sensors. No reward of its own yet.

Change to the anomaly: its crafting item line drops from 50% to 20% (`CHART_CRAFTING_ITEM_PERCENT` in its `effects` in `chart_anomalies.csv`), and Charged Fields brings it back to 50%. Built.

Its nodes are passive like the anomaly itself: none asks the player to fly into a field or time an aurora. It gets a handful of notables rather than a full set.

Small nodes:

- The chance node: built as `magnetic_storm_chance` (`CHART_MAGNETIC_STORM_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- Storm Crafting Items: "Your Star Charts with a Magnetic Storm have 10% increased quantity of crafting items found" (new, joins its `effects` line). Built: type `storm_crafting_items` (`CHART_MAGNETIC_STORM_CRAFTING_ITEM_PERCENT`). The line is a conditional drop line: `ChartGenContext.withAnomalyEffects` adds it as `CHART_CRAFTING_ITEM_PERCENT` only when the run has a Magnetic Storm (`ChartModifier.dropLineWith`), so it never stands in for the plain line in `ChartModifier.forDropType`.

Notables:

- Charged Fields: "Your Star Charts with a Magnetic Storm have 30% increased quantity of crafting items found" (new, joins its `effects` line). Built: type `charged_fields`, the same line as Storm Crafting Items.
- Static Harvest: "Your Star Charts with a Magnetic Storm have 30% increased quantity of feedstock found" (new, `CHART_FEEDSTOCK_PERCENT` read only with a Magnetic Storm). Built: type `static_harvest` (`CHART_MAGNETIC_STORM_FEEDSTOCK_PERCENT`), a conditional drop line as above that joins `CHART_FEEDSTOCK_PERCENT`.
- Ion Front (Storm Front): "Storm Fronts in your Star Charts with a Magnetic Storm set off auroras in the magnetic fields they cross" and "Battles in Storm Fronts in your Star Charts with a Magnetic Storm drop 50% increased quantity of crafting items" (new). Its auroras are vanilla magnetic storms, blinding sensors as usual. Built: type `ion_front` (`CHART_MAGNETIC_STORM_ION_FRONT`, `CHART_MAGNETIC_STORM_ION_FRONT_CRAFTING_ITEM_PERCENT`), `IonFront`. Once a second the storm's `checkRun` samples each magnetic field's ring and sets off an aurora facing where a live Storm Front crosses it (towards the front when it covers the whole ring). The battle line goes through a new `LootShaper.battleCraftingItemPercent`, summed in `ChartFleetLoot.fleetLootContext`; the storm holds a battle when the player fights inside a Storm Front of a run with a Magnetic Storm (`StormBattles.playerFront`). No Storm Front code changed.
- Geomagnetic Surge (Stellar Flares): "Corona flares in your Star Charts with a Magnetic Storm set off auroras" and "Hull items found in your Star Charts with a Magnetic Storm and Stellar Flares have 1 additional lucky modifier" (new). Its auroras are vanilla magnetic storms, blinding sensors as usual. It adds to Stellar Flares' own, so 2 modifiers are lucky. Built: type `geomagnetic_surge` (`CHART_MAGNETIC_STORM_FLARE_AURORAS`, `CHART_MAGNETIC_STORM_LUCKY_MODIFIERS`), `GeomagneticSurge`. While a star's corona flares, the star's own field gets an aurora in the flare's direction and a world's field gets one facing the star when the world lies within the flare's arc. The lucky line (`AnomalyLine.LUCKY_COUNT`) joins the lucky modifier count in `FlareLuck.dropLuck`, read through `MagneticStormTuning` in charts with a Magnetic Storm, so `StellarFlares.shapeDrop` rolls it with Golden Hour's roll count, and the tree adds the shared lucky reminder under the line.

Built readings where the plan left room:

- An aurora is a vanilla flare built from the field's own flare ranges (arc, fade, length) and colours, put ahead of the field's natural flares so it plays at once; a field already showing an aurora that faces the trigger is left alone, so a front or flare keeps one aurora going rather than restarting it every second.
- The Geomagnetic Surge aurora line needs only a Magnetic Storm, as its wording says; corona flares exist wherever a star has a corona. The lucky line needs both anomalies. A world's field responds to a flare at any distance, as long as it lies in the flare's arc.
- Ion Front's battle line applies to every battle the player fights inside a Storm Front in such a run, the same battles Storm Front's own battle lines take.

Idea for a fifth, not decided:

- Lodestone Seams (Excavation Sites): "Shafts at Excavation Sites in your Star Charts with a Magnetic Storm drop 1 additional inhibitor".

### Stellar Flares (`stellar_flares`)

Status: built on branch `atlas-nodes/stellar-flares` 2026-11-19, wiring only (not placed on the tree).

Reward: every hull item found in the system has 1 lucky modifier.

Lucky is the word for Stellar Flares' double roll in every tooltip: a lucky modifier is rolled twice, keeping the best. Every line that makes something lucky carries the bracketed reminder "(Lucky modifiers are rolled twice, keeping the best)" in muted text on the line below. Built for the anomaly's reward line on the atlas (`chart.atlas.anomaly.stellar_flares.reward`, en and zh_CN, 幸运 in the glossary); the node lines below use it when built. Built for the node lines too: a `ChartModifier` line of kind `AnomalyLine.LUCKY_COUNT` or `LUCKY_FLAG` (`makesLucky`) gets the reminder from `AtlasTreeNodeText.addChartLine`, in node tooltips and the effects panel, saying "rolled N times" with Golden Hour allocated (`FlareLuck.treeRolls`). The atlas reward line keeps "twice", since it describes the anomaly itself.

Every drop's luck goes through one shared path: `FlareLuck.dropLuck` (`anomaly` root) sums the run's lucky lines into a `Luck` (lucky modifier count, every modifier, lucky implicits, roll count), and `LuckyRolls.withLuck` rolls it, read by `StellarFlares.shapeDrop` for every item found in the system. Another anomaly's lucky line adds to `FlareLuck.dropLuck`, as Geomagnetic Surge does, or, for luck on chosen modifiers only, calls `FlareLuck.withLuckyModifiers(item, run, predicate)`, as Solar Front does, which takes Golden Hour's roll count from `FlareLuck.rolls`.

Small nodes:

- The chance node: built as `stellar_flares_chance` (`CHART_STELLAR_FLARES_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- Flare Hull Items: "Your Star Charts with Stellar Flares have 10% increased quantity of hull items found" (new). Built: type `flare_hull_items` (`CHART_STELLAR_FLARES_HULL_ITEM_PERCENT`), an anomaly drop line like Magnetic Storm's crafting item lines, which `ChartGenContext.withAnomalyEffects` turns into the run's hull item quantity only in runs with the flares (`ChartModifier.dropLineWith`).

Notables:

- Solar Maximum: "Hull items found in your Star Charts with Stellar Flares have 1 additional lucky modifier" and "Coronas in your Star Charts with Stellar Flares throw flares 50% more often". Built: type `solar_maximum` (`CHART_STELLAR_FLARES_LUCKY_MODIFIERS`, read by `FlareLuck.dropLuck`; `CHART_STELLAR_FLARES_FLARE_FREQUENCY_MULT`, read by `FlareTempo` in `anomaly.stellarflares`). `FlareTempo` is added by `StellarFlares.generate` only with the line allocated, and advances each star corona's vanilla `FlareManager` by an extra 50% of every frame, so flares come and run their course 50% faster.
- Tempered Light: "Implicit values of hull items found in your Star Charts with Stellar Flares are lucky". Built: type `tempered_light` (`CHART_STELLAR_FLARES_LUCKY_IMPLICITS`, read by `FlareLuck.dropLuck`); `LuckyRolls` rerolls each varying implicit and keeps the best.
- Golden Hour: "Lucky modifiers in your Star Charts with Stellar Flares are rolled 4 times instead of twice, keeping the best" (new, `LuckyRolls`). It covers every lucky roll in those charts, Geomagnetic Surge's and Solar Front's too, and the reminder lines there say "rolled 4 times" while it is allocated. Built: type `golden_hour` (`CHART_STELLAR_FLARES_LUCKY_ROLLS`, read by `FlareLuck.rolls`). Its magnitude is the total roll count (4), never below the base 2, so the line shows its own number. It applies only in charts with Stellar Flares.
- Sunstruck Convoy (Supply Convoy): "Supply Convoys in your Star Charts with Stellar Flares route through the star's corona" and "Supply Convoys in your Star Charts with Stellar Flares start battles with -20% to combat readiness" (new). The corona's flares wear the convoy down on its way, so it arrives at the fight weakened. Built: type `sunstruck_convoy` (`CHART_CONVOY_SUNSTRUCK`, `CHART_CONVOY_SUNSTRUCK_COMBAT_READINESS_PERCENT`), read by `ConvoySunstruck` in `anomaly.convoy`. On the convoy script's first frame, convoys still waiting get the system's star as their first waypoint, so the vanilla corona wears them down on the way. When the first battle with a convoy starts (`SunstruckBattleCreationPlugin`, offered by `SupplyConvoy.battleFor` for every convoy on the opposing side), its ships lose 20% combat readiness once, so it starts that fight weakened even before it reaches the corona.

Keystone:

- Perfect Light: "Every modifier on hull items found in your Star Charts with Stellar Flares is lucky" and "Your Star Charts with Stellar Flares have 40% reduced quantity of hull items found". Built: type `perfect_light` (`CHART_STELLAR_FLARES_ALL_LUCKY`, read by `FlareLuck.dropLuck`; `CHART_STELLAR_FLARES_HULL_ITEM_PERCENT` -40, as Flare Hull Items). Every modifier means every explicit modifier; implicits stay Tempered Light's.

Follow-up: move `StellarFlares` into `anomaly.stellarflares` once the parallel anomaly branches have merged (it stays in the `anomaly` root for now so their imports keep working).

### Spatial Breach (`spatial_breach`)

Status: built on branch `atlas-nodes/spatial-breach` 2026-11-16.

Reward: breach caches, and a 30% chance for every hull item dropped inside the open breach to be warped, carrying one modifier from another part's pool.

A chart holds at most one Spatial Breach, and a Spatial Breach marker on an atlas system doesn't add another. Its drains are the same strength everywhere inside it.

Small nodes:

- The chance node: built as `spatial_breach_chance` (`CHART_SPATIAL_BREACH_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- Breach Cache Quantity: increased breach cache quantity (`CHART_BREACH_CACHE_PERCENT`). Built: `breach_cache_quantity`.
- Breach Cache Items: increased quantity of items found in breach caches (`CHART_BREACH_CACHE_ITEM_PERCENT`). Built: `breach_cache_items`.
- Warped Chance: "50% increased chance for hull items dropped in Spatial Breaches in your Star Charts to be warped" (new, scales `BreachRules.WARPED_CHANCE`). Built: `warped_chance`, `CHART_BREACH_WARPED_CHANCE_PERCENT` read by `SpatialBreach.shapeDrop` and `shapeBattleDrop`.

Notables:

- Heavy Breach: "Spatial Breaches in your Star Charts hold at full size for 30 seconds before collapsing" and "Spatial Breaches in your Star Charts drain 3% increased fuel and supplies for each second they've held at full size" (a hold phase in `BreachRules.Growth`, and a drain multiplier for `BreachSupplyBuff` and the fuel drain). You get more time at the outermost caches, at a rising price: the drain builds through the hold, reaching 90% increased after 30 seconds, and stays there through the collapse. Built: `heavy_breach`, `CHART_BREACH_HOLD_SECONDS` (`BreachRules.Growth.holdSeconds`) and `CHART_BREACH_HELD_DRAIN_PERCENT` (`BreachRules.drainMult`, applied in `BreachTerrainPlugin.tick`).
- Rift Magnetism: "Open Spatial Breaches in your Star Charts drift toward your fleet" and "Breach caches in your Star Charts hold 40% increased quantity of items" (new). You can't park at the edge, because the drain follows you. Built: `rift_magnetism`, `CHART_BREACH_DRIFT` (`BreachTerrainPlugin.drift` moves the field, its core and its unformed caches at `BreachRules.DRIFT_SPEED`) and `CHART_BREACH_CACHE_ITEM_PERCENT`.
- Hyperspace Bleed: "Ships fighting inside Spatial Breaches in your Star Charts are struck by hyperspace lightning" and "Battles inside Spatial Breaches in your Star Charts drop 50% increased quantity of hull items" (new). The lightning hits both sides, like Pitch Black's vision penalty. Built: `hyperspace_bleed`, `CHART_BREACH_LIGHTNING` (`BreachLightningPlugin`) and `CHART_BREACH_BATTLE_HULL_ITEM_PERCENT` (`LootShaper.battleHullItemPercent`, added in `StarChartLootSource.fleetLootRules`).
- Rally Point: "50% increased chance for hull items dropped by fleets you defeat inside Spatial Breaches in your Star Charts to be warped" (new). The fleets charging the breach become the reward. Built: `rally_point`, `CHART_BREACH_FLEET_WARPED_CHANCE_PERCENT` read by `SpatialBreach.shapeBattleDrop`.
- Incursion: "Spatial Breaches in your Star Charts reveal a research station guarded by a large fleet" (new). The guard is always an Explorarium drone fleet (`derelict`), 110% of the Star Chart Boss's fleet size, so its losses count toward the Explorarium Wormhole Anchor's chance like any drone fleet's. The station appears when the breach opens, at a random point inside its maximum radius rather than at the core. If the collapsing edge passes the guard fleet before you defeat it, the station and its guard disappear. The station drops what a vanilla research station would drop in the chart, with every chart line applied, and its drops can be warped like anything else inside the breach. Built: `incursion`, `CHART_BREACH_INCURSION` (`BreachIncursion`, its point picked by `BreachRules.incursionSite`).
- Event Horizon (Black Hole): "Black Holes in your Star Charts sit inside a permanent Spatial Breach" and "Hull items found in derelicts around Black Holes in your Star Charts are always warped" (new). A separate effect, not a Spatial Breach: a static, fully open breach field drawn behind the hole, centred on it and wide enough to take in every derelict around it. It never opens or collapses, has no core, caches, drains or fleets charging it, and doesn't count toward the chart's one Spatial Breach, which still rolls as usual. It only makes the derelicts' hull items warped, on top of their chance to hold immutables. Built: `event_horizon`, `CHART_BLACK_HOLE_EVENT_HORIZON` and `CHART_BLACK_HOLE_WARPED_DERELICTS` (`EventHorizon`, called from `StarAnomalies.scatterDerelicts`, with `EventHorizonTerrainPlugin`).
- Rude Awakening (Anomalous Debris): "Anomalous fleets in your Star Charts wake and charge Spatial Breaches as they open" and "Hull items dropped by anomalous fleets defeated inside Spatial Breaches in your Star Charts are always warped" (new). Corrupting one first still gives its engram. Built: `rude_awakening`, `CHART_BREACH_WAKES_ANOMALOUS_FLEETS` (`BreachCharge.rally`) and `CHART_BREACH_ANOMALOUS_FLEETS_WARPED` (`BreachBattles`; battle drops are pooled, so the anomalous fleets' share of the defeated deployment points is always warped).
- Rift Duel (Atlas officers): "Atlas officers you defeat inside Spatial Breaches in your Star Charts duplicate 2 of their rewards at random" (new). Officer fleets already charge the breach like every fleet in the system. A defeated officer drops every reward on their card; two of those are picked at random and dropped a second time. Built: `rift_duel`, `CHART_BREACH_OFFICER_DUPLICATED_REWARDS` (`RiftDuel`, called from `OfficerDefeat`). Its second line, an extra reward to pick from, went with the officer reward choice (removed 2026-11-20).

### Relay Network (`relay_network`)

Status: built on branch `atlas-nodes/relay-network` 2026-11-16.

Reward: each woken relay offers three pairs of a reward and a scar, both added to the stage's boss; the last stage's boss drops a unique.

Change to the anomaly: a woken comm relay no longer shows every enemy fleet in the system; that reveal moves to the Full Array notable below. The comm relay keeps its wake condition and choice. Built: the comm relay no longer reveals on wake (`ChartRelays.applyWoken`), and its strings and the Relay Network section of `docs/docs.md` say so.

Small nodes:

- The chance node: built as `relay_network_chance` (`CHART_RELAY_NETWORK_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- Relay Reward Rarity: "10% increased rarity of items from relay rewards in your Star Charts" (new). Item rarity applied to the items the boss drops for relay choices (`OfficerRewardDrop.roll`), on top of the chart's own rarity. Built: `relay_reward_rarity`, `CHART_RELAY_REWARD_RARITY_PERCENT`, read in `RelayBossReward.withRewardRarity`.

Notables:

- Forked Trace: "Completed Relay Network stages in your Star Charts move on to 2 systems" (new). The chain splits: each branch runs its own remaining stages and ends in its own unique. The network is a single chained record today (`AtlasAnomalyRecord`), so a fork needs a second record carrying the same stage. Built: `forked_trace`, `CHART_RELAY_ONWARD_SYSTEMS` (the total count of systems), in `RelayStages.settle` and `fork`; every completed stage forks again, so three stages end in up to four last-stage systems.
- Crowded Channel: "Relays in your Star Charts offer 1 additional choice". Built: `crowded_channel`, `CHART_RELAY_EXTRA_CHOICES`, in `RelayChoices.Offering` and `OfficerRewardPool.offeredGroups`.
- Double Down: "Each relay choice in your Star Charts adds its reward twice" and "Each relay choice in your Star Charts adds an additional scar" (new). The additional scar is a different one, rolled like the offered scars (`Scars.withEffectsOn`), not a copy of the choice's own. Built: `double_down`, `CHART_RELAY_EXTRA_REWARD_COPIES` and `CHART_RELAY_EXTRA_SCARS`, in `RelayBossReward.rewardGroups` and `RelayChoices.extraScarsFor`; the extra scars are stored with the choice (`RelayChoice`).
- Extra Relay: "Relay Networks in your Star Charts have 1 additional relay" (new). The fourth relay is a second of a random type, woken the same way and offering its own choice, and all four must be woken for the network to move on. Never more than four relays. Built: `extra_relay`, `CHART_RELAY_EXTRA_RELAYS`, as `RelaySlot` (the fourth relay's slot is `<kind>_extra`), placed in `RelayNetwork.slots` and recorded in the run's relay state.
- Scar Collector: "Star Chart Bosses in your Star Charts drop 1 additional reward for every 2 relay scars they carry" (new). Built: `scar_collector`, `CHART_RELAY_SCAR_REWARDS`, in `RelayBossReward.rewardGroups`; each extra reward is drawn from the group of a random pick, and Double Down's extra scars count.
- Early Echo: "Relay Network bosses in your Star Charts have 15% chance per stage to drop a unique" (new). Every stage's boss gets the chance, not only the last. Built: `early_echo`, `CHART_RELAY_STAGE_UNIQUE_CHANCE`, in `RelayBossReward.echoUnique`; like the last stage's unique, it needs every relay awake, and the last stage keeps its guaranteed one instead.
- Shared Broadcast (Atlas officers): "Relay choices in your Star Charts also add their rewards and scars to a random Atlas officer" (new). The boss keeps both; the officer carries the reward like any carried item and takes the scar on their flagship, so beating them later drops it. Built: `shared_broadcast`, `CHART_RELAY_SHARED_BROADCAST`, in `RelayBroadcast`; the officer is a random one in command, gets as many copies as the boss and every scar of the choice, and a campaign message names them.
- Full Array: "Waking every relay in a system in your Star Charts reveals all enemy fleets in it on the map and minimap" (new). The reveal the comm relay gave on its own, using the same detection stats (`Stats.DETECTED_BY_PLAYER_RANGE_MULT` and the extended detected range), applied once the last relay wakes. With Extra Relay that means all four. Built: `full_array`, `CHART_RELAY_FULL_ARRAY`, in `ChartRelays.fullArrayRevealing`, revealing on the last wake and on every check after it.

### Supply Convoy (`supply_convoy`)

Status: built on branch `atlas-nodes/supply-convoy` 2026-11-16.

Reward: infused feedstock in the cargo pods and the delivered cache.

Change to the anomaly: a convoy carries one kind of infused feedstock instead of all three. The kind is rolled when the convoy is generated, Threat-, Shroud- and Phase-infused alike likely, and shown in its manifest and intel. It carries the total the three shared before (three times the shared amount, rolled up or down by as much as 15%), so a convoy is worth the same and the kind is the gamble. Built: `ConvoyFeedstock.roll` with `FeedstockOdds`, the kind's name in the manifest through `ConvoyCargo.feedstockKey` (`chart.anomaly.supply_convoy.cargo.feedstock.<kind>`), `anomaly_rewards.md` updated; `docs/docs.md` has no Supply Convoy section to change.

Small nodes (types in `atlas_tree_types.json`, tested in `SupplyConvoyAtlasTypesTest`):

- The chance node: built as `supply_convoy_chance` (`CHART_SUPPLY_CONVOY_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- [x] Threat Feedstock Chance: "10% reduced chance for Supply Convoys in your Star Charts to carry Threat-infused Feedstock" (new). Lead-in to Threat Embargo. Built: `threat_feedstock_chance`, `CHART_CONVOY_THREAT_FEEDSTOCK_CHANCE_PERCENT`, read by `FeedstockOdds`.
- [x] Shroud Feedstock Chance: the same for Shroud-infused Feedstock. Lead-in to Shroud Embargo. Built: `shroud_feedstock_chance`, `CHART_CONVOY_SHROUD_FEEDSTOCK_CHANCE_PERCENT`.
- [x] Phase Feedstock Chance: the same for Phase-infused Feedstock. Lead-in to Phase Embargo. Built: `phase_feedstock_chance`, `CHART_CONVOY_PHASE_FEEDSTOCK_CHANCE_PERCENT`.
- [x] Feedstock Duplication: "Supply Convoys in your Star Charts have 25% chance to carry double feedstock" (new, rolled with the kind). Built: `feedstock_duplication`, `CHART_CONVOY_DOUBLE_FEEDSTOCK_CHANCE_PERCENT`, rolled in `ConvoyFeedstock.roll`.
- [x] Additional Convoy Chance: "Your Star Charts that contain a Supply Convoy have +30% chance to contain an additional Supply Convoy" (new: `CHART_EXTRA_CONVOYS` rounds to whole convoys, so this needs a chance line, still capped by `ConvoyRules.MAX_CONVOYS`). Built: `additional_convoy_chance`, `CHART_EXTRA_CONVOY_CHANCE_PERCENT`, rolled in `ConvoyRules.convoyCount` from `SupplyConvoy.generate`.

The feedstock chance lines scale that kind's weight in the roll, so reducing one makes the other two likelier. A player who wants one kind reduces the other two; reducing all three equally changes nothing. Built: a kind's weight is 100% less its reduced chance, at least 0; with every weight at 0 the kinds are picked evenly.

Notables (tested in `ConvoyAtlasNodesTest` and `ExcavationDeliveriesTest`):

- [x] Threat Embargo: "25% reduced chance for Supply Convoys in your Star Charts to carry Threat-infused Feedstock". Built: `threat_embargo`, the small node's line at 25.
- [x] Shroud Embargo: the same for Shroud-infused Feedstock. Built: `shroud_embargo`.
- [x] Phase Embargo: the same for Phase-infused Feedstock. Built: `phase_embargo`.
- [x] Torn Holds: "Supply Convoys defeated inside Spatial Breaches in your Star Charts drop 50% increased quantity of feedstock" (new, read while the player fleet is inside the open breach, as warped items are). Built: `torn_holds`, `CHART_CONVOY_BREACH_FEEDSTOCK_PERCENT`; `ConvoyScript.recordLosses` scales the pods' feedstock by `FeedstockBonus.breachShare` while `SpatialBreach.breachIn` holds the player fleet.
- [x] Supply Lines: "Supply Convoys you defeat in your Star Charts give a random Atlas officer carried feedstock equal to the convoy's" (new). The officer carries it like any carried item, so beating them drops it (`completed/atlas_officers.md`). Built: `supply_lines`, `CHART_CONVOY_OFFICER_FEEDSTOCK`; `ConvoySupplyLines.handOver` gives a random officer in command the convoy's rolled feedstock as carried items when its freighters are destroyed.
- [x] Bolt-Hole Drop: "Supply Convoys in your Star Charts deliver to an Excavation Site where one exists, adding their frigates and destroyers to its guardian fleet", "Raids on Excavation Sites a Supply Convoy delivered to in your Star Charts have 100% increased difficulty" and "30% increased quantity of feedstock and inhibitors found at Excavation Sites a Supply Convoy delivered to in your Star Charts" (new). The convoy picks a site's planet as its destination (`ConvoyRoute`), and the delivered cache joins that site's reward instead of a raid of its own: won with the guardian fleet at an open shaft, or with the raid at a collapsed one (`ExcavationLoot`). Only the frigates and destroyers still in the convoy when it unloads join the guardian fleet, since nothing bigger fits down the shaft. With no Excavation Site in the chart the convoy delivers as usual. Built: `bolt_hole_drop`, `CHART_CONVOY_EXCAVATION_DELIVERY`, `CHART_CONVOY_EXCAVATION_RAID_DIFFICULTY_PERCENT`, `CHART_CONVOY_EXCAVATION_LOOT_PERCENT`. `ConvoyBoltHole.reroute` sends waiting convoys to the standing sites' planets on the script's first frame (the sites are only known once every anomaly has generated), and `ConvoyBoltHole.deliver` hands the ships and cache to `excavation.ExcavationDeliveries`: an open shaft's guardian fleet takes the cache as extra salvage, a collapsed shaft keeps it on the planet for the raid (`ExcavationSiteObjective`), which `ExcavationSiteDialog` launches harder through `ChartRaid.Session.launch`. The site's inhibitors get the loot line in `ExcavationLoot`.
- [x] Open Season: "Supply Convoys in your Star Charts are hostile to you" and "Supply Convoys in your Star Charts drop 20% increased quantity of feedstock" (new). The convoy fights from the start, so there's no free approach to pick the strike. Built: `open_season`, `CHART_HOSTILE_CONVOYS` (`ConvoyFleets.huntsFromTheStart`) and `CHART_CONVOY_FEEDSTOCK_PERCENT` (`FeedstockBonus.rollMult`, added to the crafting item quantity).

Keystone:

- [x] Mixed Manifest: "Supply Convoys in your Star Charts carry an equal amount of all three kinds of infused feedstock" and "Supply Convoys in your Star Charts have 20% increased fleet size" (new). The total stays the same, split three ways; with it allocated the feedstock chance lines do nothing. Built: `mixed_manifest`, `CHART_CONVOY_MIXED_MANIFEST` (`FeedstockOdds.mixed`) and `CHART_CONVOY_FLEET_SIZE_PERCENT` (added to the escort size in `ConvoyRules.escortMult`).

### Anomalous Debris (`anomalous_debris`)

Status: built on branch `atlas-nodes/anomalous-debris` 2026-11-16, chance node on `atlas-nodes/shared`.

Reward: an engram from each corrupted anomalous fleet beaten.

Change to the anomaly: an anomalous fleet is 50% of the Star Chart Boss's fleet size before corruption, down from 110% (`DebrisRules.BOSS_SHARE`). Corruption and the nodes below scale from that. The debris fleets are called anomalous fleets everywhere, in these nodes and in game: the fleet's name (`chart.anomaly.anomalous_debris.fleet.name`, now "Anomalous Fleet" and "Corrupted Anomalous Fleet") and the Engram Quantity line (`chart.modifier.CHART_ANOMALOUS_DEBRIS_CRAFTING_ITEM_QUANTITY_PERCENT`), in en and zh_CN. Built. The faction keeps its name, Dormant Cores, and the code keeps its `DormantFleet*` class names.

Every node's lines are read through `DebrisTuning` (one record per chart) in `charts/anomaly/debris`.

Small nodes:

- The chance node: built as `anomalous_debris_chance` (`CHART_ANOMALOUS_DEBRIS_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- Additional Debris (`additional_debris`): "Your Star Charts that contain Anomalous Debris have 1 additional debris field" (`CHART_EXTRA_ANOMALOUS_DEBRIS`). Built.
- Engram Quantity (`engram_quantity`): "20% increased quantity of crafting items dropped by corrupted anomalous fleets in your Star Charts" (`CHART_ANOMALOUS_DEBRIS_CRAFTING_ITEM_QUANTITY_PERCENT`). Built.

Notables:

- Deep Corruption (`deep_corruption`): "Corrupted anomalous fleets in your Star Charts have 30% increased fleet size" (`CHART_ANOMALOUS_DEBRIS_CORRUPTED_FLEET_SIZE_PERCENT`) and "Corrupted anomalous fleets in your Star Charts drop 1 additional engram" (`CHART_ANOMALOUS_DEBRIS_CORRUPTED_ENGRAMS`). Built: `DebrisRules.fleetSize` and `DormantFleetLoot`.
- Double Dose (`double_dose`): "Anomalous fleets in your Star Charts can be corrupted a second time for 1 additional Corrupted Nanoswarm" (`CHART_ANOMALOUS_DEBRIS_SECOND_CORRUPTION`) and "Anomalous fleets corrupted twice in your Star Charts drop 2 additional engrams" (`CHART_ANOMALOUS_DEBRIS_TWICE_CORRUPTED_ENGRAMS`). The second corruption strengthens the fleet again by the same step. Built: the dialog offers "Corrupt twice and engage" for two nanoswarms (`DormantFleetDialog`), and `DormantFleets.corrupt` rebuilds the fleet with the count stored on it.
- Lone Sleeper (`lone_sleeper`): "Your Star Charts with Anomalous Debris have only one debris field" (`CHART_ANOMALOUS_DEBRIS_SINGLE_FIELD`), "Anomalous fleets in your Star Charts have 100% increased fleet size" (`CHART_ANOMALOUS_DEBRIS_FLEET_SIZE_PERCENT`) and "Corrupted anomalous fleets in your Star Charts drop 3 additional engrams" (`CHART_ANOMALOUS_DEBRIS_CORRUPTED_ENGRAMS`). Several small fights become one big one. Built: `DebrisRules.fieldCount`.
- Breached Graveyard (`breached_graveyard`, Spatial Breach): "Spatial Breaches in your Star Charts with Anomalous Debris open in a debris field" (`CHART_ANOMALOUS_DEBRIS_BREACH_FIELD`). The breach's core sits in the field, so the anomalous fleet's fight is always inside the open breach and its hull item drops can be warped. Built: `BreachedGraveyard`, whichever anomaly generates second; a breach placed after the debris centres on the field farthest from the entry, and debris placed after the breach puts its first field on the breach core.
- Restless Sleepers (`restless_sleepers`): "Anomalous fleets in your Star Charts are always corrupted and awake" (`CHART_ANOMALOUS_DEBRIS_AWAKE`) and "Anomalous fleets in your Star Charts roam their system and are hostile to you" (`CHART_ANOMALOUS_DEBRIS_ROAMING`). They're hostile only to you, not to the chart's other fleets, and are revealed like any other fleet once awake. Built: fleets wake when placed (`AnomalousDebris.placeField`) and a woken fleet patrols its system (`DormantFleets.wake`).
- [REDACTED] Sleepers (`redacted_sleepers`): "Anomalous fleets in your Star Charts have 30% of their ships replaced by what was left behind" (`CHART_ANOMALOUS_DEBRIS_REMNANT_SHIP_SHARE`). Remnant hulls. Built.
- Threatening Sleepers (`threatening_sleepers`): "Anomalous fleets in your Star Charts have 30% of their ships replaced by something that crept in from the deep dark" (`CHART_ANOMALOUS_DEBRIS_THREAT_SHIP_SHARE`). Threat hulls. Built.
- Veiled Sleepers (`veiled_sleepers`): "Anomalous fleets in your Star Charts have 30% of their ships replaced by something that dwells behind the veil" (`CHART_ANOMALOUS_DEBRIS_DWELLER_SHIP_SHARE`). Shrouded hulls. Built.

The tooltips never name the faction; the player learns it from the ships they meet. The three Sleepers notables share one roll per ship, so they never claim the same ship: with all three, 90% of the fleet is a mix of the three and 10% keeps its random factions' hulls. A replaced ship becomes an equivalent hull from that faction's own hulls (`remnant`, `threat`, `dweller`): the same hull size where the faction has one, else the nearest size, closest in deployment points. It keeps its AI core captain, and corruption strengthens it like the rest of the fleet. Built: `SleeperShips`, drawing from the faction's combat roles.

Each replaced ship counts as its faction's for that faction's wormhole anchor, so its deployment points add to the anchor's chance per 100 deployment points lost (`FactionFleetDrops.chance`, `drop_chance` in `wormhole_anchors.csv`), alongside the fleet's usual loot and engram. The anchor's Janus Device and volume gates still apply. Replaced Shrouded hulls use the same per-deployment-point chance as the other two: the Shrouded anchor's every-battle guarantee (`guaranteed_drop`) stays with real Dweller fleets. Built: the fleet's memory maps each replaced member's id to its faction (`FleetFactions.recordMemberFactions`), so a recovered ship carries no trace; `BattleEngagement.deploymentPointsLostByFaction` reads it before the fleet's faction, `deploymentPointsLostByFleetFaction` keeps the old tally, and `FactionFleetDrops.chance` fires a guarantee only on the fleet tally (`SkillTreeLootSource` passes both).

Keystone:

- Silent Choir (`silent_choir`): "Anomalous fleets in your Star Charts are always corrupted twice" (`CHART_ANOMALOUS_DEBRIS_ALWAYS_TWICE_CORRUPTED`) and "Anomalous fleets in your Star Charts have 50% increased fleet size" (`CHART_ANOMALOUS_DEBRIS_FLEET_SIZE_PERCENT`). Built: fleets are generated corrupted twice and still dormant, so their dialog offers only Engage; with Restless Sleepers they are also awake.

### Excavation Sites (`excavation_sites`)

Status: built on branch `atlas-nodes/excavation-sites` 2026-11-16, chance node on `atlas-nodes/shared`. Every line below is a `ChartModifier` anomaly tuning line read once per run through `ExcavationTuning` (`anomaly.excavation`); the types are in `atlas_tree_types.json` but not yet placed in the tree.

Reward: inhibitors, two from each full raid of a collapsed shaft and one from each open shaft's guardian fleet.

Additional inhibitors raise the base count, before any quantity. Each fight or raid adds up the base and every "N additional inhibitors" line that applies to it, scales the total by every quantity the run has for it (the run's quantity of items found, its crafting item quantity and Inhibitor Quantity below), and rolls each inhibitor of the result as a random kind, so a large drop is a spread of kinds rather than many of one. A guardian fleet with 2 additional inhibitors drops a base of 3, which 100% increased quantity of items found or crafting item quantity doubles to 6. Change: today `ExcavationLoot.expectedInhibitors` reads only the site's own quantity line; it also reads the run's item and crafting item quantity (`ChartGenContext.dropMult(DropType.CRAFTING_ITEM)`, as convoy feedstock does), and the additional lines join `perShaft`. The random kind is `ExcavationLoot.tagInhibitors` as built. Built: `ExcavationRules.guardianInhibitors` and `raidInhibitors` add the lines to the base, and `ExcavationLoot.quantityMult` sums the site's quantity percents before multiplying by the run's crafting item drop multiplier.

Small nodes:

- The chance node: built as `excavation_sites_chance` (`CHART_EXCAVATION_SITES_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- Additional Site: "Your Star Charts that contain Excavation Sites have 1 additional excavation site" (`CHART_EXTRA_EXCAVATION_SITES`). Built: type `additional_site`.
- Inhibitor Quantity: "20% increased quantity of items found at excavation sites in your Star Charts" (`CHART_EXCAVATION_LOOT_QUANTITY_PERCENT`). Built: type `inhibitor_quantity`.

Notables, a matching pair so the player picks fleet or marine play:

- Open Bores: "Excavation Sites in your Star Charts always have open shafts" and "Fleets guarding open shafts in your Star Charts drop 1 additional inhibitor". Built: type `open_bores` (`CHART_EXCAVATION_OPEN_SHAFTS`, `CHART_EXCAVATION_GUARDIAN_INHIBITORS`), shaft choice in `ExcavationRules.access`.
- Sapper Teams: "Excavation Sites in your Star Charts always have collapsed shafts" and "Raids on collapsed shafts in your Star Charts take 1 additional inhibitor". Built: type `sapper_teams` (`CHART_EXCAVATION_COLLAPSED_SHAFTS`, `CHART_EXCAVATION_RAID_INHIBITORS`). With both and no Twin Shafts, each site keeps its 50/50 roll and both inhibitor lines apply.

More notables:

- Twin Shafts: "Excavation Sites in your Star Charts have both an open and a collapsed shaft" (new, `ExcavationRules.access`). Each shaft drops its own inhibitors. With Open Bores or Sapper Teams allocated too, every site still has both shafts and their additional inhibitor lines still apply. Built: type `twin_shafts` (`CHART_EXCAVATION_TWIN_SHAFTS`), access `ExcavationSite.Access.BOTH`; each shaft closes on its own through `ExcavationStates.clearShaft`.
- Deeper Bores: "Open shafts in your Star Charts have a second cavern below the first" and "Fleets guarding second caverns in your Star Charts are 50% larger and drop 2 additional inhibitors" (new). After beating the guardian fleet the site dialog offers going deeper or leaving with what was won; the same frigates and destroyers go down with the damage and CR the first fight left them, so it is a gamble on the fleet's condition. Lines that apply to fleets guarding open shafts apply to the second cavern's fleet too. Built: type `deeper_bores`, split into `CHART_EXCAVATION_SECOND_CAVERN`, `CHART_EXCAVATION_SECOND_CAVERN_FLEET_SIZE_MULT` ("50% more fleet size", since the wording rules ban "larger") and `CHART_EXCAVATION_SECOND_CAVERN_INHIBITORS`; the offer is in `ExcavationSiteDialog`, and leaving the second cavern's fight by any outcome closes the open shaft.
- Needle Bores: "Only frigates can enter open shafts in your Star Charts" and "Fleets guarding open shafts in your Star Charts drop 2 additional inhibitors" (new, `ExcavationRules.MAX_SHIP_SIZE` 1 for both sides). Built: type `needle_bores` (`CHART_EXCAVATION_FRIGATES_ONLY`), size from `ExcavationRules.maxShipSize`, read by the fleet builder, the site and encounter dialogs and both battle plugins.
- Widened Bores: "Cruisers can enter open shafts in your Star Charts" and "Fleets guarding open shafts in your Star Charts drop 1 additional inhibitor" (new, `ExcavationRules.MAX_SHIP_SIZE` 3 for both sides). With Needle Bores allocated too, only frigates go down, and both nodes' additional inhibitors apply. Built: type `widened_bores` (`CHART_EXCAVATION_CRUISERS_ALLOWED`), as Needle Bores.
- Pitch Black: "Ships fighting in open shafts in your Star Charts have 70% reduced vision range" and "50% increased quantity of inhibitors from open shafts in your Star Charts" (new, the sight radius of every ship on both sides, set by the shaft's battle plugin, `ExcavationBattlePlugin`). Built: type `pitch_black` (`CHART_EXCAVATION_SIGHT_RADIUS_PERCENT`, `CHART_EXCAVATION_OPEN_SHAFT_INHIBITOR_PERCENT`), `ExcavationBattlePlugin.darken`.
- Infested Bolt-Holes: "Fleets guarding open shafts in your Star Charts with Anomalous Debris can be corrupted with a Corrupted Nanoswarm" and "Corrupted guardian fleets in your Star Charts drop an engram and 1 additional inhibitor" (new). The site dialog offers the corruption before the fight, as the anomalous fleets' dialog does (`anomaly.debris`). Built: type `infested_bolt_holes` (`CHART_EXCAVATION_CORRUPTIBLE_GUARDIANS`, `CHART_EXCAVATION_CORRUPTED_GUARDIAN_INHIBITORS`), `ExcavationCorruption`. The debris fleets' corruption only works on dormant fleets, so the guardian fleet is built corrupted instead (`ExcavationRules.CORRUPTED_SIZE_MULT` and `CORRUPTED_STRENGTH_BONUS_PERCENT`, matching the debris placeholders), offered for either cavern before its fleet is first met, and its engram drops once (`ExcavationLoot.engramOnce`).
- Settled Shafts: "Each Settling Dust stack in your Star Charts grants 30% increased quantity of inhibitors found at Excavation Sites" (new, read through the Dense Nebula's stacks, so the nebula's battles come before the dig). Built: type `settled_shafts` (`CHART_EXCAVATION_INHIBITOR_PERCENT_PER_DUST`), `SettlingDust.stacks` read in `ExcavationLoot` (`DenseNebula.dust` until the Dense Nebula nodes moved the count to the run, so Ionized Dust's stacks count too).

### Storm Front (`storm_front`)

Status: built on branch `atlas-nodes/storm-front` 2026-11-19, wiring only (not placed on the tree). Every line below is a `ChartModifier` anomaly tuning line; the new ones are read through `StormTuning` (`anomaly.stormfront`), once at generation and from the run afterwards. Ion Front is not built here.

Reward: every non-unique hull item from a battle in the storm carries a biome modifier, and those battles drop 50% increased quantity of items.

Small nodes:

- The chance node: built as `storm_front_chance` (`CHART_STORM_FRONT_CHANCE_PERCENT`, read by `OpeningAnomalies.addedChances`).
- Storm Item Quantity: "15% increased quantity of items dropped by battles in Storm Fronts in your Star Charts" (`CHART_STORM_FRONT_LOOT_QUANTITY_PERCENT`). Built: type `storm_item_quantity`.
- Storm Front Size: "20% increased Storm Front size in your Star Charts" (`CHART_STORM_FRONT_SIZE_PERCENT`). Built: type `storm_front_size`.

Its nodes are about fighting in the storm. Ion Front, its pairing with the Magnetic Storm, is listed there.

Notables:

- Lightning Rod: "50% increased frequency of storm strikes in your Star Charts" (`CHART_STORM_STRIKE_FREQUENCY_PERCENT`) and "30% increased quantity of items dropped by battles in Storm Fronts in your Star Charts" (`CHART_STORM_FRONT_LOOT_QUANTITY_PERCENT`). Built: type `lightning_rod`.
- Supercell: "100% increased Storm Front size in your Star Charts" (`CHART_STORM_FRONT_SIZE_PERCENT`). Built: type `supercell`.
- Eye of the Storm: "Storm Fronts in your Star Charts have 80% reduced speed while your fleet is inside them" (new, slows the band's movement in `StormFrontTerrainPlugin`; the crossing still ends at the far edge, later by however long the player stayed inside, and chart intel's days follow). Built: type `eye_of_the_storm` (`CHART_STORM_FRONT_HELD_SPEED_PERCENT`, share from `StormRules.heldSlowShare`, at most a full hold). While the player fleet is inside, `StormFrontTerrainPlugin.applyEffect` adds that share of each day to `heldDays`, and the band, its end and the intel follow `travelledDays`; a negative value speeds the front up.
- [REDACTED] Front: "Enemy fleets you fight in Storm Fronts in your Star Charts are joined by 5 to 10 frigates of what was left behind" (new). Remnant frigates join the enemy side when the battle in the storm starts. As with the [REDACTED] Sleepers, the tooltip never names the faction, and each joining ship counts as Remnant for the Remnant wormhole anchor's chance per 100 deployment points lost (`FleetFactions.recordMemberFactions`). The ships keep their AI core captains, so they can drop AI cores as vanilla Remnant ships do. Built: type `redacted_front` (`CHART_STORM_FRONT_REDACTED_FRIGATES`, the line's value is the most, the fewest is half of it rounded up, `StormRules.redactedFrigatesFewest`), in `RedactedFront`. They join when the player opens a dialog with a hostile fleet while inside a front of the run (`StormFront.dialogFor`), once per fleet, and only the engaged fleet is reinforced, not fleets pulled into the battle; an earlier anomaly's own dialog can take the interaction first. Each is a Remnant combat-small frigate with a Remnant AI core captain at the anomalous debris' core level for the tier (`DebrisRules.coreLevel`), at full CR.
- Captured Front (Black Hole): "Storm Fronts in your Star Charts with a Black Hole circle the hole instead of crossing the system" and "Hull items dropped by battles in Storm Fronts in your Star Charts with a Black Hole have 10% chance to have an immutable modifier" (new). The front orbits the hole through its derelicts for the whole run and never leaves (`StormPath`), so chart intel says where it is next rather than when it leaves. Built: type `captured_front` (`CHART_STORM_FRONT_CAPTURED`, `CHART_STORM_FRONT_CAPTURED_IMMUTABLE_CHANCE_PERCENT`). The path is now a `StormCourse`, either `StormPath` or `StormOrbit`: a spoke of the band's width reaching from just outside the hole (`StormRules.CAPTURED_INNER_GAP`) past the derelicts (`StarAnomalies.DERELICT_REACH` plus `CAPTURED_OUTER_MARGIN`), turning once every `CAPTURED_ORBIT_DAYS`. It only circles when the chart's planned anomalies hold the Black Hole, otherwise it crosses as usual. Intel says the front circles the hole and when it next reaches each marked world. The immutable chance goes through `ImmutableDrops.withChance` in `StormLoot.shapedDrop`.
- Solar Front (Stellar Flares): "Biome modifiers on hull items dropped by battles in Storm Fronts in your Star Charts with Stellar Flares are lucky" (new, in `StormLoot`). The front's clouds and flashes turn red and orange, on the map and in battle (`StormFrontTerrainPlugin`, `StormOverlay`). Built: type `solar_front` (`CHART_STORM_FRONT_LUCKY_BIOME_MODIFIERS`), set at generation when the planned anomalies hold Stellar Flares. `StormLoot.shapedByFront` makes every biome modifier on the drop lucky through `FlareLuck.withLuckyModifiers`, the shared lucky path, so Golden Hour's roll count covers it; the line is `AnomalyLine.LUCKY_FLAG`, so the tree adds the shared lucky reminder under it. The colours are `StormPalette.SOLAR` for the clouds, flashes and the battle's arcs (`StormBattlePlugin`).
- Thunder Duel (Atlas officers): "Atlas officers you defeat in Storm Fronts in your Star Charts drop 1 additional item from their reward pool" (new). The officers don't seek out the storm: fighting them in it is up to the player. Built: type `thunder_duel` (`CHART_STORM_FRONT_OFFICER_EXTRA_REWARDS`), in `ThunderDuel`, added to the carried loot in `OfficerDefeat` while the player fleet is inside a front of the run; the items are drawn from the officer's reward pool (`OfficerRewardPool.offers`) and rolled through `OfficerRewardDrop.roll`.

### Derelict Forge (`derelict_forge`)

Status: off for now (2026-11-16).

Reward: the forge's programs, run on a hull item of the player's choosing.

Change to the anomaly (overhaul, 2026-11-18, built on `derelict-forge-overhaul`, merged into `parallel-work`):

- [x] The forge no longer grants commodities: its `CHART_RESOURCE_PERCENT:100` effect leaves `chart_anomalies.csv`, since its programs no longer cost metals.
- [x] Its guards' drone escort is 80% of the Star Chart Boss's fleet size (`max(bossSize, fleetSize)`, as the other anomalies' boss-sized fleets are), up from 50% of a chart fleet's. Built as `ForgeRules.ESCORT_BOSS_SHARE` and `ForgeRules.escortSize`. The Guardian stays.
- [x] Anchor Reroll and Retemper are deleted (`chart_forge_programs.csv`, `ForgeOperation`, their strings and route texts).
- [x] Every forge offers all of its programs instead of rolling 3: Graft, Excise and Perfect. The program roll, its weights, tiers and `ForgeRules.programCount` go, and so does `CHART_FORGE_PROGRAMS`. Built as `DerelictForge.programs`.
- [x] A forge in a biome system offers Biome Infusion instead of Graft: "Adds the system's biome modifier for the item's part to an item with room for it." It is the Abyssal Research Complex's Abyssal Infusion generalised to the system's biome (`ForgeInfusions`, which now takes the biome per call, with the modifier from `BiomeModifiers.modifierFor` for the system's region), refused on the same terms. A system whose region has no biome modifiers keeps Graft. It is named Biome Infusion (program id `biome_infuse`) because the Infused Crafting tray already has a Biome Graft, which trades a random modifier for a biome modifier. Biome Infusion and Abyssal Infusion both refuse an item that already has 2 biome modifiers, as the Infused Crafting tray's biome crafts do (`ForgeInfusions.Block.LIMIT`, with the limit from `ChartForge.BiomeInfusion`).
- [x] Every program costs a fixed amount, with no metals, rare metals or tier scaling: Graft 300 Nanite Feedstock, Excise 500 Nanite Feedstock, Perfect 1000 Nanite Feedstock, and Biome Infusion 500 of each infused feedstock (Threat-, Shroud- and Phase-infused). The cost columns in `chart_forge_programs.csv` are now `feedstock` and `infused_feedstock_each` (`ForgeRules.Cost`); Abyssal Infusion keeps its 500 Nanite Feedstock at the Research Complex. The cost line reads "feedstock cost of Derelict Forge programs".

Small nodes:

- The chance node.

Notables:

- Recycled Stock: "20% reduced feedstock cost of Derelict Forge programs in your Star Charts" (`CHART_FORGE_COST_PERCENT`, applied to both Nanite and infused feedstock).
- Overclocked Forge: "Derelict Forges in your Star Charts can run 1 additional program" (`CHART_FORGE_RUNS`).

### Depot (`listening_post`)

Status: built on branch `atlas-nodes/depot` 2026-11-16, chance node on `atlas-nodes/shared`. The types are in `atlas_tree_types.json` without icons or tree placement; their lines' tuning is read through `DepotTuning`.

Reward: prototype hull items, each with its implicits replaced by one from its base's pool. Reaching the vault unseen picks one of three; after the alarm one is left at random.

Small nodes:

- The chance node: built as `listening_post_chance` (`CHART_LISTENING_POST_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- Vault Quantity: "20% increased quantity of items in Depot vaults in your Star Charts" (`CHART_LISTENING_POST_LOOT_QUANTITY_PERCENT`). Built: `vault_quantity`.
- Slow Purge: "25% reduced Depot vault purge rate in your Star Charts" (`CHART_LISTENING_POST_PURGE_PERCENT`). Built: `slow_purge`.

Notables:

- Garrison Pickets: "Depot pickets in your Star Charts are much larger" (new). Each picket becomes half the size of the Star Chart Boss's fleet, up from 30% of a chart fleet today (`HeistRules`). The size is the reward: pickets drop ordinary chart fleet loot, so larger pickets are more fleet to fight and more to win. Built: `garrison_pickets` (`CHART_LISTENING_POST_GARRISON_PICKETS`), sized in `ListeningPostRules.picketSize` and spawned as large patrols with the boss's overflow strength in `Pickets.spawn`.
- Hair Trigger: "Depot pickets in your Star Charts that identify your fleet raise the alarm at once" and "Depot vaults reached unseen in your Star Charts offer 2 additional prototypes to pick from" (new). Attacking a picket always identifies the fleet first (Detection in `completed/anomalies/listening_post.md`), so this rules out picking pickets off: the heist is pure stealth. Built: `hair_trigger` (`CHART_LISTENING_POST_INSTANT_ALARM`, `CHART_LISTENING_POST_UNSEEN_PROTOTYPES`); the alarm in `PostScript.detect`, the picks in `DepotTuning.prototypePicks`.
- Smash and Grab: "Depot vaults in your Star Charts can only be opened by raiding the post", "30% increased quantity of items in Depot vaults in your Star Charts" and "Depot vaults in your Star Charts offer 1 additional prototype to pick from" (new). The post dialog offers a raid instead of opening the vault, using vanilla's raid mechanics through the shared `ChartRaid`, as the Excavation Sites' collapsed shafts do. The raid is needed with the alarm down or up; after the alarm the guard must be beaten first, as today. Built: `smash_and_grab` (`CHART_LISTENING_POST_RAID_ONLY`, `CHART_LISTENING_POST_LOOT_QUANTITY_PERCENT`, `CHART_LISTENING_POST_PROTOTYPES`); `PostDialog.launchRaid` raids the post's hidden market (`PostMarket`) for one `VaultRaidObjective` that empties the vault. A raid on a quiet post counts as reaching the vault unseen.
- Hunting Party: "When the alarm is raised at a Depot in your Star Charts, its pickets hunt your fleet instead of guarding the post" and "Depot pickets in your Star Charts can drop prototypes" (new). The chance scales with the picket's strength like any battle drop: a prototype row rolled per 100 deployment points the pickets lose, at 0.12 (about 60% of the hull item's 0.192 in `battle_loot.csv`), with the hull item's diminishing curve and difficulty scaling. Small pickets rarely drop one, and larger ones (Garrison Pickets) drop more. The post is left unguarded, so the vault's remains are free to take once the hunt is survived. Built: `hunting_party` (`CHART_LISTENING_POST_HUNTING_PICKETS`, `CHART_LISTENING_POST_PICKET_PROTOTYPES`); pickets intercept the player, or patrol the system while the player is away (`Pickets.hunt`, `PostScript.keepHunting`), and the `prototype` row in `battle_loot.csv` is rolled by `PicketPrototypes` through the new `LootShaper.battleItems` hook in `ChartFleetLoot`.
- Fortified Depot: "Depot posts in your Star Charts are armed stations that fight beside their guard" and "Depot vaults in your Star Charts don't purge while their post stands" (new). The post becomes a station fleet joining the guard's fight (`PostScript.gatherDefenders`), so the alarm path is a station assault for the full vault. Built: `fortified_depot` (`CHART_LISTENING_POST_ARMED_STATION`, `CHART_LISTENING_POST_PURGE_HELD`); `ArmedPost` orbits a boss station (midline fallback) around the post, and engaging it moves the guard beside it so the guard joins the fight.
- Officer's Ledger: "Depot vaults in an Atlas officer's territory in your Star Charts duplicate 1 of that officer's rewards at random" (new). The reward is picked at random from the officer's card; the officer keeps it, and the copy is taken with the vault, alarm or not. A system outside every officer's territory gains nothing. Built: `officers_ledger` (`CHART_LISTENING_POST_OFFICER_LEDGER`) in `VaultExtras`.
- Stolen Charts: "Depot vaults reached unseen in your Star Charts hold 1 to 3 corrupted Star Charts with 8 modifiers, up to 2 tiers below or above the current tier" (new). Each chart's tier is rolled within two of the run's tier, never below 1 or above 15 (`ChartTiers.HIGHEST_DROP_TIER`). Each is corrupted with the expanded-modifiers outcome (`StarChartCorruption.expandModifiers`, 8 modifiers, 4 a side), so it can't be corrupted again: tier 16 stays reachable only by using a Corrupted Nanoswarm on an uncorrupted tier 15 chart. Built: `stolen_charts` (`CHART_LISTENING_POST_STOLEN_CHARTS`) in `VaultExtras`, with `StarChartCorruption.withExpandedModifiers`.

### Pre-Collapse Facility (`pre_collapse_facility`)

Status: off for now (2026-11-16).

Reward: Vaults of Knowledge databanks. Needs Ashes of the Domain: Vaults of Knowledge, and its nodes are hidden without it.

Small nodes:

- The chance node.
- Facility Databanks: "20% increased quantity of databanks found in Pre-Collapse Facilities in your Star Charts" (new, scales `FacilityRules.databankRolls`).

### Coronal Hypershunt (`coronal_hypershunt`)

Status: off for now (2026-11-16).

Reward: the Omega fleet guarding it. Rare, never insertable by players and never within 15 cells of the Gateway. With no reward type of its own, it gets other effects later.

Small nodes:

- The atlas chance node.

### Remnant Nexus (`remnant_nexus_station`)

Status: off for now (2026-11-16).

Reward: each hull item the station itself drops has one modifier overcharged, rolled 25% past its top value.

Small nodes:

- The biome chance node (Remnant Expanse).
- Nexus Hull Items: "Remnant Nexus stations in your Star Charts drop 20% increased quantity of hull items" (new).

Notables:

- Surge Cores: "Hull items dropped by Remnant Nexus stations in your Star Charts have 1 additional overcharged modifier" and "Remnant Nexus stations in your Star Charts rebuild their fleets 50% faster".

### Domain-era Mothership (`domain_mothership`)

Status: off for now (2026-11-16).

Reward: none of its own (one was won't do, 2026-11-14), so it gets other effects later.

Small nodes:

- The biome chance node (Derelict Graveyard).

### Abyssal Shipyard (`abyssal_shipyard`)

Status: off for now (2026-11-16).

Reward: the shipyard's salvage from RAT's drop groups, behind its heavy abyssal guard. Needs Random Assortment of Things, as do the two below, and the nodes are hidden without it.

Small nodes:

- The biome chance node (Abyssal Rift).

### Abyssal Storm (`abyssal_storm`)

Status: off for now (2026-11-16).

Reward: 30% increased quantity of items found in the system, while storms light up every fleet outside the fog.

Small nodes:

- The chance node.
- Abyssal Storm Items: "Your Star Charts with an Abyssal Storm have 10% increased quantity of items found" (new, joins its `effects` line).

### Abyssal Research Complex (`abyssal_research_complex`)

Status: off for now (2026-11-16).

Reward: Abyssal Infusion, which adds the Abyssal Rift modifier for a hull item's part once per run for 500 Nanite Feedstock.

Small nodes:

- The biome chance node (Abyssal Rift).
- Infusion Cost: "20% reduced Nanite Feedstock cost of Abyssal Infusion in your Star Charts" (new).

Notables:

- Second Infusion: "Abyssal Research Complexes in your Star Charts can run Abyssal Infusion 1 additional time".

## Atlas officers

Reward: beating an officer drops everything they carry. There is no reward choice any more (removed 2026-11-20, with its picker, pending choice and console command): a beaten officer is dealt a new starting item on the next turn (`OfficerRewardRepair`), and officers gather more by scavenging and by taking a beaten rival's items in challenges. Carried items stack. Faction Skirmish is the battlefield of the officers' own pushes and challenges and has no reward of its own (`completed/anomalies/faction_skirmish_rework.md`), so its nodes are here.

Lines here act on the atlas and its officers more than on one chart, so most are atlas lines (`AtlasModifier`) read by the reward pool and the turn engine, not chart lines.

Small nodes:

- Trophy Duplication: "Items Atlas officers take from officers they beat have 5% chance to be duplicated" (new). Rolled for each item taken where a challenge winner joins the loser's carried items into their own (`OfficerTurn.resolveChallenge`, `CarriedItem.joined`), whether the challenge resolves on its roll or in a Faction Skirmish. A stack rolls each of its items, so a carried 4 Augmentation Nanoswarms can come out as 5 to 8. Pushes take no items, so they don't roll.
- Scavenge Duplication: "Items Atlas officers scavenge have 5% chance to be duplicated" (new). Rolled on the scavenged item in `OfficerTurn.scavenge`, from the same seeded random so a turn stays deterministic.

Both are atlas lines read by the turn engine, so they go into the turn's rules (`TurnRules`) rather than a chart's lines, and the turn stays a pure function of the saved state and the allocated tree.

Notables:

- Spoils of War: "Atlas officers you defeat in Faction Skirmishes in your Star Charts drop 1 additional item from their reward pool" and "Officer fleets in Faction Skirmishes in your Star Charts have 30% increased size".
- Hardened Command: "Atlas officers' flagship fleets in your Star Charts have 25% increased size" and "+1 to Atlas officers' level" (new). The size joins the rank's boss size wherever an officer's fleet is built (`OfficerFleets.dress`), at their command centre and in Faction Skirmishes alike; the level joins the scars' extra levels (`OfficerFleets.flagshipExtraLevels`).
- Trophy Refit: "When an Atlas officer wins a challenge, the items they carry are rerolled with 25% to 65% increased item rarity, rising with their rank" (new). The tooltip doesn't mention the crafting item rule below. The reroll comes after the winner takes the loser's items, so it covers the whole joined stack, and uses the winner's rank after the challenge: 25% for a Lieutenant, 35% Commander, 45% Captain, 55% Commodore, 65% Admiral. Each item is rerolled from the reward pool at the officer's tier (as `OfficerScavenging` rolls one), keeping its kind: a hull item stays that base type and can become a unique of it, a biome hull item keeps its biome modifier, a basic crafting item stays a basic crafting item (`DropType.BASIC_CRAFTING_ITEM`) at its `MaterialTier` or higher, an inhibitor stays an inhibitor, and a unique stays as it is. Each item of a stack rerolls on its own, so a stack can split into several items. A push won, a Defend held or a win against the player rerolls nothing.
- Open Warfare: "100% increased frequency of fights between Atlas officers" (new). Doubles the push and challenge weights in every rank's intent roll (`IntentWeights`) and the board's cap on declared pushes and challenges (`max_conflicts_per_turn`, 6 to 12), so twice as many Faction Skirmishes appear on the atlas each turn and items change hands twice as often.
- Scrapper's Fortune: "100% increased number of Atlas officers who scavenge each turn" and "Items Atlas officers scavenge are lucky" (new), over the reminder "(Lucky items are rolled twice, keeping the best)". The count doubles `scavengers_min` and `scavengers_max` (2 to 3 becomes 4 to 6), still drawn only from officers who declared Hold and aren't targeted. Lucky here rolls the scavenged item twice and keeps the rarer of the two (the higher `MaterialTier` for crafting items, a unique over a non-unique hull item), and a hull item it keeps has every modifier lucky, through the shared lucky path (`FlareLuck.withLuckyModifiers`).
- Iron Grip: "Atlas officers you defeat drop every reward type they carry, but lose only 1 of them", "Atlas officers' flagship fleets in your Star Charts have 50% increased fleet quality" and "+2 to the level of other officers in Atlas officers' flagship fleets" (new). A reward type is a group on the officer's card (`RewardGroup`: a crafting item tier, inhibitors, Engrams, hull items and so on). A defeat still drops everything the officer carries (`OfficerRewardDrop.roll` over the whole card, with Rift Duel and Thunder Duel adding as usual), but `OfficerDefeat` then removes only one group, picked at random from the defeat's seeded random, and stores the officer with the rest still carried. Their card stays worth hunting: beating them again drops the remaining groups again, until the last one goes and `OfficerRewardRepair` deals them a new starting item. The quality joins the rank's fleet quality and the level joins the other officers' levels wherever an officer's fleet is built (`OfficerFleets.dress`); the flagship's own officer is unchanged.

## Open questions

1. Which anomalies get a volume of their own and which share one: the tree plan's Volumes table groups the storm anomalies and gives Breach, Relay and Convoy their own, and the rest aren't placed yet.
