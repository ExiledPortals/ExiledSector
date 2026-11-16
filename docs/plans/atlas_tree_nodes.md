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
- Duplication and rerolls: "N% chance to be duplicated", "rolls N twice, keeping the highest".
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

Increased effect of Star Chart modifiers scales every implicit and explicit modifier on a chart once it is opened, so 10% increased shield damage taken becomes 15% with 50% increased effect. Each prefix's hidden item quantity bonus is scaled too, so a prefix is a downside plus a loot bonus that grow together. The bonus still shows only in the totals at the top of the chart tooltip.

### Ideas

## Anomaly nodes

The Atlas Tree makes every anomaly more rewarding or more likely to appear, one way or another. Each anomaly's nodes focus on the reward only it grants (`completed/anomalies/anomaly_rewards.md` and the anomaly's own plan): Excavation Sites on inhibitors, Supply Convoys on infused feedstock, and so on. They give more of the anomaly, more of its reward, or a change to how it grants it. An anomaly with no reward type of its own gets other effects instead. Each section names the reward, then lists small nodes, notables and, where one fits, a keystone. Nodes for an anomaly that needs a mod are hidden without it, as gated volumes are.

An anomaly marker on an atlas system adds one more of that anomaly to the system where that makes sense, such as one more anomalous fleet. Anomalies where it doesn't keep today's rules, and their sections say so.

### Chance nodes

Every anomaly's small nodes include one chance node: "Your Star Charts have +8% chance to contain a Spatial Breach". It adds 8 percentage points to the anomaly's `opening_chance` (0.08 for every common anomaly today), so one node doubles the chance. Type id `<anomaly id>_chance`, name "<Anomaly> Chance", one new chart line per anomaly read by `OpeningAnomalies` on top of the `chart_anomalies.csv` chance (it doesn't read tree lines yet). The roll keeps the system's cap, conflicts, biome weights and `insertVerdict`, so the node never puts an anomaly where it can't go. The sections below list it as the chance node.

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

### Neutron Star (`neutron_star`)

Status: off for now (2026-11-16).

Reward: a research station in orbit, in a system with at least 7 planets for cover. It has no reward of its own yet (`anomaly_rewards.md`, Unchanged), so its nodes stay on the station until it gets one.

Small nodes:

- The atlas chance node.
- Pulsar Station Quantity: "20% increased quantity of items found in research stations around Neutron Stars in your Star Charts" (new).

Notables:

- Twin Stations: "Neutron Stars in your Star Charts have an additional research station".

### Dense Nebula (`dense_nebula`)

Status: off for now (2026-11-16).

Reward: Settling Dust, a stack for each battle won in the system, each giving 10% increased boss star charts, up to 10 stacks.

Small nodes:

- The atlas chance node.
- Settling Dust Effect: "Settling Dust in your Star Charts has 20% increased effect" (new, scales `STAR_CHART_PERCENT_PER_DUST`).
- Settling Dust Stacks: "+1 to maximum Settling Dust stacks in your Star Charts" (new, adds to `MAX_DUST`).

Notables:

- Dust Trails: "Settling Dust in your Star Charts also grants 3% increased quantity of items found for each stack".
- Long Settling: "+5 to maximum Settling Dust stacks in your Star Charts" and "Enemy fleets in Dense Nebulae in your Star Charts have 3% increased size for each Settling Dust stack".

### Asteroid Storm (`asteroid_storm`)

Status: off for now (2026-11-16).

Reward: caches hidden in its belts. It has no reward of its own yet; `anomaly_rewards.md` suggests the belt caches hold inhibitors, which its notable takes up.

Small nodes:

- The chance node.
- Belt Cache Quantity: "Asteroid Storms in your Star Charts hide 2 additional belt caches" (new).

Notables:

- Prospector's Find: "Belt caches in your Star Charts contain an inhibitor".
- Rockfall: "50% increased frequency of asteroid impacts in your Star Charts" and "40% increased quantity of items found in belt caches in your Star Charts".

### Magnetic Storm (`magnetic_storm`)

Status: off for now (2026-11-16).

Reward: 50% increased quantity of crafting items found in the system, behind magnetic fields that blind sensors. No reward of its own yet.

Small nodes:

- The chance node.
- Storm Crafting Items: "Your Star Charts with a Magnetic Storm have 10% increased quantity of crafting items found" (new, joins its `effects` line).

Notables:

- Charged Fields: "Magnetic Storms in your Star Charts have 2 additional magnetic fields" and "Your Star Charts with a Magnetic Storm have 30% increased quantity of crafting items found".
- Shielded Arrays: "Your fleet's sensors are unaffected by magnetic fields in your Star Charts" and "Enemy fleets' sensors are unaffected by magnetic fields in your Star Charts".

### Stellar Flares (`stellar_flares`)

Status: off for now (2026-11-16).

Reward: every hull item dropped in the system rolls one of its modifiers again and keeps the better value.

Small nodes:

- The chance node.
- Flare Hull Items: "Your Star Charts with Stellar Flares have 10% increased quantity of hull items found" (new).

Notables:

- Solar Maximum: "Hull items dropped in your Star Charts with Stellar Flares roll 2 of their modifiers twice, keeping the better values" and "Coronas in your Star Charts with Stellar Flares throw flares 50% more often".
- Tempered Light: "Hull items dropped in your Star Charts with Stellar Flares also roll their implicit values twice, keeping the better".

Keystone:

- Perfect Light: "Hull items dropped in your Star Charts with Stellar Flares roll every modifier twice, keeping the better values" and "Your Star Charts with Stellar Flares have 40% reduced quantity of hull items found".

### Spatial Breach (`spatial_breach`)

Status: building on branch `atlas-nodes/spatial-breach` since 2026-11-16, chance node on `atlas-nodes/shared`.

Reward: breach caches, and a 30% chance for every hull item dropped inside the open breach to be warped, carrying one modifier from another part's pool.

A chart holds at most one Spatial Breach, and a Spatial Breach marker on an atlas system doesn't add another. Its drains are the same strength everywhere inside it.

Small nodes:

- The chance node: built as `spatial_breach_chance` (`CHART_SPATIAL_BREACH_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- Breach Cache Quantity: increased breach cache quantity (`CHART_BREACH_CACHE_PERCENT`).
- Breach Cache Items: increased quantity of items found in breach caches (`CHART_BREACH_CACHE_ITEM_PERCENT`).
- Warped Chance: "50% increased chance for hull items dropped in Spatial Breaches in your Star Charts to be warped" (new, scales `BreachRules.WARPED_CHANCE`).

Notables:

- Heavy Breach: "Spatial Breaches in your Star Charts hold at full size for 30 seconds before collapsing" and "Spatial Breaches in your Star Charts drain 3% increased fuel and supplies for each second they've held at full size" (a hold phase in `BreachRules.Growth`, and a drain multiplier for `BreachSupplyBuff` and the fuel drain). You get more time at the outermost caches, at a rising price: the drain builds through the hold, reaching 90% increased after 30 seconds, and stays there through the collapse.
- Rift Magnetism: "Open Spatial Breaches in your Star Charts drift toward your fleet" and "Breach caches in your Star Charts hold 40% increased quantity of items" (new). You can't park at the edge, because the drain follows you.
- Hyperspace Bleed: "Ships fighting inside Spatial Breaches in your Star Charts are struck by hyperspace lightning" and "Battles inside Spatial Breaches in your Star Charts drop 50% increased quantity of hull items" (new). The lightning hits both sides, like Pitch Black's vision penalty.
- Rally Point: "50% increased chance for hull items dropped by fleets you defeat inside Spatial Breaches in your Star Charts to be warped" (new). The fleets charging the breach become the reward.
- Incursion: "Spatial Breaches in your Star Charts reveal a research station guarded by a large fleet" (new). The guard is always an Explorarium drone fleet (`derelict`), 110% of the Star Chart Boss's fleet size, so its losses count toward the Explorarium Wormhole Anchor's chance like any drone fleet's. The station appears when the breach opens, at a random point inside its maximum radius rather than at the core. If the collapsing edge passes the guard fleet before you defeat it, the station and its guard disappear. The station drops what a vanilla research station would drop in the chart, with every chart line applied, and its drops can be warped like anything else inside the breach.
- Event Horizon (Black Hole): "Black Holes in your Star Charts sit inside a permanent Spatial Breach" and "Hull items found in derelicts around Black Holes in your Star Charts are always warped" (new). A separate effect, not a Spatial Breach: a static, fully open breach field drawn behind the hole, centred on it and wide enough to take in every derelict around it. It never opens or collapses, has no core, caches, drains or fleets charging it, and doesn't count toward the chart's one Spatial Breach, which still rolls as usual. It only makes the derelicts' hull items warped, on top of their chance to hold immutables.
- Rude Awakening (Anomalous Debris): "Anomalous fleets in your Star Charts wake and charge Spatial Breaches as they open" and "Hull items dropped by anomalous fleets defeated inside Spatial Breaches in your Star Charts are always warped" (new). Corrupting one first still gives its engram.
- Rift Duel (Atlas officers): "Atlas officers you defeat inside Spatial Breaches in your Star Charts duplicate 2 of their rewards at random" and "Atlas officers you defeat inside Spatial Breaches in your Star Charts offer 1 additional reward to pick from, granted the next time you defeat them" (new). Officer fleets already charge the breach like every fleet in the system. A defeated officer drops every reward on their card; two of those are picked at random and dropped a second time. The pick-one offer is separate and isn't duplicated.

### Relay Network (`relay_network`)

Status: building on branch `atlas-nodes/relay-network` since 2026-11-16, chance node on `atlas-nodes/shared`.

Reward: each woken relay offers three pairs of a reward and a scar, both added to the stage's boss; the last stage's boss drops a unique.

Change to the anomaly: a woken comm relay no longer shows every enemy fleet in the system; that reveal moves to the Full Array notable below. The comm relay keeps its wake condition and choice. Not built yet: it goes in `ChartRelays` (the reveal on wake), with the comm relay strings and the Relay Network section of `docs/docs.md` updated to match.

Small nodes:

- The chance node: built as `relay_network_chance` (`CHART_RELAY_NETWORK_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- Relay Reward Rarity: "10% increased rarity of items from relay rewards in your Star Charts" (new). Item rarity applied to the items the boss drops for relay choices (`OfficerRewardDrop.roll`), on top of the chart's own rarity.

Notables:

- Forked Trace: "Completed Relay Network stages in your Star Charts move on to 2 systems" (new). The chain splits: each branch runs its own remaining stages and ends in its own unique. The network is a single chained record today (`AtlasAnomalyRecord`), so a fork needs a second record carrying the same stage.
- Crowded Channel: "Relays in your Star Charts offer 1 additional choice".
- Double Down: "Each relay choice in your Star Charts adds its reward twice" and "Each relay choice in your Star Charts adds an additional scar" (new). The additional scar is a different one, rolled like the offered scars (`Scars.withEffectsOn`), not a copy of the choice's own.
- Extra Relay: "Relay Networks in your Star Charts have 1 additional relay" (new). The fourth relay is a second of a random type, woken the same way and offering its own choice, and all four must be woken for the network to move on. Never more than four relays.
- Scar Collector: "Star Chart Bosses in your Star Charts drop 1 additional reward for every 2 relay scars they carry" (new).
- Early Echo: "Relay Network bosses in your Star Charts have 15% chance per stage to drop a unique" (new). Every stage's boss gets the chance, not only the last.
- Shared Broadcast (Atlas officers): "Relay choices in your Star Charts also add their rewards and scars to a random Atlas officer" (new). The boss keeps both; the officer carries the reward like any carried item and takes the scar on their flagship, so beating them later drops it.
- Full Array: "Waking every relay in a system in your Star Charts reveals all enemy fleets in it on the map and minimap" (new). The reveal the comm relay gave on its own, using the same detection stats (`Stats.DETECTED_BY_PLAYER_RANGE_MULT` and the extended detected range), applied once the last relay wakes. With Extra Relay that means all four.

### Supply Convoy (`supply_convoy`)

Status: building on branch `atlas-nodes/supply-convoy` since 2026-11-16, chance node on `atlas-nodes/shared`.

Reward: infused feedstock in the cargo pods and the delivered cache.

Change to the anomaly: a convoy carries one kind of infused feedstock instead of all three. The kind is rolled when the convoy is generated, Threat-, Shroud- and Phase-infused alike likely, and shown in its manifest and intel. It carries the total the three shared before (three times the shared amount, rolled up or down by as much as 15%), so a convoy is worth the same and the kind is the gamble. Not built yet: it goes in `ConvoyFeedstock.roll`, with `anomaly_rewards.md` and the Supply Convoy section of `docs/docs.md` updated to match.

Small nodes:

- The chance node: built as `supply_convoy_chance` (`CHART_SUPPLY_CONVOY_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- Threat Feedstock Chance: "10% reduced chance for Supply Convoys in your Star Charts to carry Threat-infused Feedstock" (new). Lead-in to Threat Embargo.
- Shroud Feedstock Chance: the same for Shroud-infused Feedstock. Lead-in to Shroud Embargo.
- Phase Feedstock Chance: the same for Phase-infused Feedstock. Lead-in to Phase Embargo.
- Feedstock Duplication: "Supply Convoys in your Star Charts have 25% chance to carry double feedstock" (new, rolled with the kind).
- Additional Convoy Chance: "Your Star Charts that contain a Supply Convoy have +30% chance to contain an additional Supply Convoy" (new: `CHART_EXTRA_CONVOYS` rounds to whole convoys, so this needs a chance line, still capped by `ConvoyRules.MAX_CONVOYS`).

The feedstock chance lines scale that kind's weight in the roll, so reducing one makes the other two likelier. A player who wants one kind reduces the other two; reducing all three equally changes nothing.

Notables:

- Threat Embargo: "25% reduced chance for Supply Convoys in your Star Charts to carry Threat-infused Feedstock".
- Shroud Embargo: the same for Shroud-infused Feedstock.
- Phase Embargo: the same for Phase-infused Feedstock.
- Torn Holds: "Supply Convoys defeated inside Spatial Breaches in your Star Charts drop 50% increased quantity of feedstock" (new, read while the player fleet is inside the open breach, as warped items are).
- Supply Lines: "Supply Convoys you defeat in your Star Charts give a random Atlas officer carried feedstock equal to the convoy's" (new). The officer carries it like any carried item, so beating them drops it (`completed/atlas_officers.md`).
- Bolt-Hole Drop: "Supply Convoys in your Star Charts deliver to an Excavation Site where one exists, adding their frigates and destroyers to its guardian fleet", "Raids on Excavation Sites a Supply Convoy delivered to in your Star Charts have 100% increased difficulty" and "30% increased quantity of feedstock and inhibitors found at Excavation Sites a Supply Convoy delivered to in your Star Charts" (new). The convoy picks a site's planet as its destination (`ConvoyRoute`), and the delivered cache joins that site's reward instead of a raid of its own: won with the guardian fleet at an open shaft, or with the raid at a collapsed one (`ExcavationLoot`). Only the frigates and destroyers still in the convoy when it unloads join the guardian fleet, since nothing bigger fits down the shaft. With no Excavation Site in the chart the convoy delivers as usual.
- Open Season: "Supply Convoys in your Star Charts are hostile to you" and "Supply Convoys in your Star Charts drop 20% increased quantity of feedstock" (new). The convoy fights from the start, so there's no free approach to pick the strike.

Keystone:

- Mixed Manifest: "Supply Convoys in your Star Charts carry an equal amount of all three kinds of infused feedstock" and "Supply Convoys in your Star Charts have 20% increased fleet size" (new). The total stays the same, split three ways; with it allocated the feedstock chance lines do nothing.

### Anomalous Debris (`anomalous_debris`)

Status: building on branch `atlas-nodes/anomalous-debris` since 2026-11-16, chance node on `atlas-nodes/shared`.

Reward: an engram from each corrupted anomalous fleet beaten.

Change to the anomaly: an anomalous fleet is 50% of the Star Chart Boss's fleet size before corruption, down from 110% (`DebrisRules.BOSS_SHARE`). Corruption and the nodes below scale from that. The debris fleets are called anomalous fleets everywhere, in these nodes and in game: the fleet's name ("Dormant Fleet" today, `chart.anomaly.anomalous_debris.fleet.name`) and the Engram Quantity line (`chart.modifier.CHART_ANOMALOUS_DEBRIS_CRAFTING_ITEM_QUANTITY_PERCENT`), in en and zh_CN. Not built yet.

Small nodes:

- The chance node: built as `anomalous_debris_chance` (`CHART_ANOMALOUS_DEBRIS_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- Additional Debris: "Your Star Charts that contain Anomalous Debris have 1 additional debris field" (`CHART_EXTRA_ANOMALOUS_DEBRIS`).
- Engram Quantity: "20% increased quantity of crafting items dropped by corrupted anomalous fleets in your Star Charts" (`CHART_ANOMALOUS_DEBRIS_CRAFTING_ITEM_QUANTITY_PERCENT`).

Notables:

- Deep Corruption: "Corrupted anomalous fleets in your Star Charts have 30% increased fleet size" and "Corrupted anomalous fleets in your Star Charts drop 1 additional engram".
- Double Dose: "Anomalous fleets in your Star Charts can be corrupted a second time for 1 more Corrupted Nanoswarm" and "Anomalous fleets corrupted twice in your Star Charts drop 2 additional engrams" (new). The second corruption strengthens the fleet again by the same step.
- Lone Sleeper: "Your Star Charts with Anomalous Debris have only one debris field" and "Anomalous fleets in your Star Charts have 100% increased fleet size and drop 3 additional engrams when corrupted" (new). Several small fights become one big one.
- Breached Graveyard (Spatial Breach): "Spatial Breaches in your Star Charts with Anomalous Debris open in a debris field" (new). The breach's core sits in the field, so the anomalous fleet's fight is always inside the open breach and its hull item drops can be warped.
- Restless Sleepers: "Anomalous fleets in your Star Charts are always corrupted and awake" and "Anomalous fleets in your Star Charts roam their system and are hostile to you" (new). They're hostile only to you, not to the chart's other fleets, and are revealed like any other fleet once awake.
- [REDACTED] Sleepers: "Anomalous fleets in your Star Charts have 30% of their ships replaced by what was left behind" (new). Remnant hulls.
- Threatening Sleepers: "Anomalous fleets in your Star Charts have 30% of their ships replaced by something that crept in from the deep dark" (new). Threat hulls.
- Veiled Sleepers: "Anomalous fleets in your Star Charts have 30% of their ships replaced by something that dwells behind the veil" (new). Shrouded hulls.

The tooltips never name the faction; the player learns it from the ships they meet. The three Sleepers notables share one roll per ship, so they never claim the same ship: with all three, 90% of the fleet is a mix of the three and 10% keeps its random factions' hulls. A replaced ship becomes an equivalent hull from that faction's own hulls (`remnant`, `threat`, `dweller`): the same hull size where the faction has one, else the nearest size, closest in deployment points. It keeps its AI core captain, and corruption strengthens it like the rest of the fleet.

Each replaced ship counts as its faction's for that faction's wormhole anchor, so its deployment points add to the anchor's chance per 100 deployment points lost (`FactionFleetDrops.chance`, `drop_chance` in `wormhole_anchors.csv`), alongside the fleet's usual loot and engram. Today `BattleEngagement.deploymentPointsLostByFaction` counts every lost ship as its fleet's faction (`FleetFactions.realFactionIdOf`), which would give the anomalous fleet's faction all of it. The replaced ship carries its source faction on the member instead, and the tally reads that before the fleet's faction. The anchor's Janus Device and volume gates still apply. Replaced Shrouded hulls use the same per-deployment-point chance as the other two: the Shrouded anchor's every-battle guarantee (`guaranteed_drop`) stays with real Dweller fleets.

Keystone:

- Silent Choir: "Anomalous fleets in your Star Charts are always corrupted twice" and "Anomalous fleets in your Star Charts have 50% increased fleet size".

### Excavation Sites (`excavation_sites`)

Status: building on branch `atlas-nodes/excavation-sites` since 2026-11-16, chance node on `atlas-nodes/shared`.

Reward: inhibitors, two from each full raid of a collapsed shaft and one from each open shaft's guardian fleet.

Additional inhibitors raise the base count, before any quantity. Each fight or raid adds up the base and every "N additional inhibitors" line that applies to it, scales the total by every quantity the run has for it (the run's quantity of items found, its crafting item quantity and Inhibitor Quantity below), and rolls each inhibitor of the result as a random kind, so a large drop is a spread of kinds rather than many of one. A guardian fleet with 2 additional inhibitors drops a base of 3, which 100% increased quantity of items found or crafting item quantity doubles to 6. Change: today `ExcavationLoot.expectedInhibitors` reads only the site's own quantity line; it also reads the run's item and crafting item quantity (`ChartGenContext.dropMult(DropType.CRAFTING_ITEM)`, as convoy feedstock does), and the additional lines join `perShaft`. The random kind is `ExcavationLoot.tagInhibitors` as built.

Small nodes:

- The chance node: built as `excavation_sites_chance` (`CHART_EXCAVATION_SITES_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- Additional Site: "Your Star Charts that contain Excavation Sites have 1 additional excavation site" (`CHART_EXTRA_EXCAVATION_SITES`).
- Inhibitor Quantity: "20% increased quantity of items found at excavation sites in your Star Charts" (`CHART_EXCAVATION_LOOT_QUANTITY_PERCENT`).

Notables, a matching pair so the player picks fleet or marine play:

- Open Bores: "Excavation Sites in your Star Charts always have open shafts" and "Fleets guarding open shafts in your Star Charts drop 1 additional inhibitor".
- Sapper Teams: "Excavation Sites in your Star Charts always have collapsed shafts" and "Raids on collapsed shafts in your Star Charts take 1 additional inhibitor".

More notables:

- Twin Shafts: "Excavation Sites in your Star Charts have both an open and a collapsed shaft" (new, `ExcavationRules.access`). Each shaft drops its own inhibitors. With Open Bores or Sapper Teams allocated too, every site still has both shafts and their additional inhibitor lines still apply.
- Deeper Bores: "Open shafts in your Star Charts have a second cavern below the first" and "Fleets guarding second caverns in your Star Charts are 50% larger and drop 2 additional inhibitors" (new). After beating the guardian fleet the site dialog offers going deeper or leaving with what was won; the same frigates and destroyers go down with the damage and CR the first fight left them, so it is a gamble on the fleet's condition. Lines that apply to fleets guarding open shafts apply to the second cavern's fleet too.
- Needle Bores: "Only frigates can enter open shafts in your Star Charts" and "Fleets guarding open shafts in your Star Charts drop 2 additional inhibitors" (new, `ExcavationRules.MAX_SHIP_SIZE` 1 for both sides).
- Widened Bores: "Cruisers can enter open shafts in your Star Charts" and "Fleets guarding open shafts in your Star Charts drop 1 additional inhibitor" (new, `ExcavationRules.MAX_SHIP_SIZE` 3 for both sides). With Needle Bores allocated too, only frigates go down, and both nodes' additional inhibitors apply.
- Pitch Black: "Ships fighting in open shafts in your Star Charts have 70% reduced vision range" and "50% increased quantity of inhibitors from open shafts in your Star Charts" (new, the sight radius of every ship on both sides, set by the shaft's battle plugin, `ExcavationBattlePlugin`).
- Infested Bolt-Holes: "Fleets guarding open shafts in your Star Charts with Anomalous Debris can be corrupted with a Corrupted Nanoswarm" and "Corrupted guardian fleets in your Star Charts drop an engram and 1 additional inhibitor" (new). The site dialog offers the corruption before the fight, as the anomalous fleets' dialog does (`anomaly.debris`).
- Settled Shafts: "Each Settling Dust stack in your Star Charts grants 30% increased quantity of inhibitors found at Excavation Sites" (new, read through the Dense Nebula's stacks, so the nebula's battles come before the dig).

### Storm Front (`storm_front`)

Status: off for now (2026-11-16).

Reward: every non-unique hull item from a battle in the storm carries a biome modifier, and those battles drop 50% increased quantity of items.

Small nodes:

- The chance node.
- Storm Item Quantity: "15% increased quantity of items dropped by battles in Storm Fronts in your Star Charts" (`CHART_STORM_FRONT_LOOT_QUANTITY_PERCENT`).
- Storm Front Size: "20% increased Storm Front size in your Star Charts" (`CHART_STORM_FRONT_SIZE_PERCENT`).

Notables:

- Lightning Rod: "50% increased frequency of storm strikes in your Star Charts" (`CHART_STORM_STRIKE_FREQUENCY_PERCENT`) and "30% increased quantity of items dropped by battles in Storm Fronts in your Star Charts".
- Stalled Front: "Storm Fronts in your Star Charts take 15 additional days to cross the system".

### Derelict Forge (`derelict_forge`)

Status: off for now (2026-11-16).

Reward: the forge's programs, run on a hull item of the player's choosing.

Small nodes:

- The chance node.
- Forge Cost: "20% reduced Derelict Forge program costs in your Star Charts" (`CHART_FORGE_COST_PERCENT`).

Notables:

- Unlocked Library: "Derelict Forges in your Star Charts offer 2 additional programs" (`CHART_FORGE_PROGRAMS`).
- Overclocked Forge: "Derelict Forges in your Star Charts can run 1 additional program" (`CHART_FORGE_RUNS`) and "Derelict Forge guards in your Star Charts have 50% increased fleet size".

### Depot (`listening_post`)

Status: building on branch `atlas-nodes/depot` since 2026-11-16, chance node on `atlas-nodes/shared`.

Reward: prototype hull items, each with its implicits replaced by one from its base's pool. Reaching the vault unseen picks one of three; after the alarm one is left at random.

Small nodes:

- The chance node: built as `listening_post_chance` (`CHART_LISTENING_POST_CHANCE_PERCENT`, read by `OpeningAnomalies.rolled`).
- Vault Quantity: "20% increased quantity of items in Depot vaults in your Star Charts" (`CHART_LISTENING_POST_LOOT_QUANTITY_PERCENT`).
- Slow Purge: "25% reduced Depot vault purge rate in your Star Charts" (`CHART_LISTENING_POST_PURGE_PERCENT`).

Notables:

- Garrison Pickets: "Depot pickets in your Star Charts are much larger" (new). Each picket becomes half the size of the Star Chart Boss's fleet, up from 30% of a chart fleet today (`HeistRules`). The size is the reward: pickets drop ordinary chart fleet loot, so larger pickets are more fleet to fight and more to win.
- Hair Trigger: "Depot pickets in your Star Charts that identify your fleet raise the alarm at once" and "Depot vaults reached unseen in your Star Charts offer 2 additional prototypes to pick from" (new). Attacking a picket always identifies the fleet first (Detection in `completed/anomalies/listening_post.md`), so this rules out picking pickets off: the heist is pure stealth.
- Smash and Grab: "Depot vaults in your Star Charts can only be opened by raiding the post", "30% increased quantity of items in Depot vaults in your Star Charts" and "Depot vaults in your Star Charts offer 1 additional prototype to pick from" (new). The post dialog offers a raid instead of opening the vault, using vanilla's raid mechanics through the shared `ChartRaid`, as the Excavation Sites' collapsed shafts do. The raid is needed with the alarm down or up; after the alarm the guard must be beaten first, as today.
- Hunting Party: "When the alarm is raised at a Depot in your Star Charts, its pickets hunt your fleet instead of guarding the post" and "Depot pickets in your Star Charts can drop prototypes" (new). The chance scales with the picket's strength like any battle drop: a prototype row rolled per 100 deployment points the pickets lose, at 0.12 (about 70% of the basic hull item's 0.176 in `battle_loot.csv`), with the hull item's diminishing curve and difficulty scaling. Small pickets rarely drop one, and larger ones (Garrison Pickets) drop more. The post is left unguarded, so the vault's remains are free to take once the hunt is survived.
- Fortified Depot: "Depot posts in your Star Charts are armed stations that fight beside their guard" and "Depot vaults in your Star Charts don't purge while their post stands" (new). The post becomes a station fleet joining the guard's fight (`PostScript.gatherDefenders`), so the alarm path is a station assault for the full vault.
- Officer's Ledger: "Depot vaults in an Atlas officer's territory in your Star Charts duplicate 1 of that officer's rewards at random" (new). The reward is picked at random from the officer's card; the officer keeps it, and the copy is taken with the vault, alarm or not. A system outside every officer's territory gains nothing.
- Stolen Charts: "Depot vaults reached unseen in your Star Charts hold 1 to 3 corrupted Star Charts with 8 modifiers, up to 2 tiers below or above the current tier" (new). Each chart's tier is rolled within two of the run's tier, never below 1 or above 15 (`ChartTiers.HIGHEST_DROP_TIER`). Each is corrupted with the expanded-modifiers outcome (`StarChartCorruption.expandModifiers`, 8 modifiers, 4 a side), so it can't be corrupted again: tier 16 stays reachable only by using a Corrupted Nanoswarm on an uncorrupted tier 15 chart.

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

Reward: beating an officer drops everything they carry and offers three items, one of which they carry to their next defeat. Carried items stack and pass to whoever beats them in a challenge. Faction Skirmish is the battlefield of the officers' own pushes and challenges and has no reward of its own (`completed/anomalies/faction_skirmish_rework.md`), so its nodes are here.

Lines here act on the atlas and its officers more than on one chart, so most are atlas lines (`AtlasModifier`) read by the reward pool and the turn engine, not chart lines.

Small nodes:

- Officer Crafting Items: "Crafting items carried by Atlas officers drop with 20% increased quantity" (new, scales the carried base counts).
- Officer Uniques: "25% increased chance for Atlas officers' reward choices to offer a unique" (new, scales the unique share in `atlas_officer_rewards.csv`).
- Officer Boss Items: "20% increased quantity of items dropped by Atlas officers' flagship fleets in your Star Charts" (new).

Notables:

- Fourth Offer: "Atlas officers you defeat offer 1 additional item to pick from".
- Spoils of War: "Atlas officers you defeat in Faction Skirmishes in your Star Charts drop 1 additional item from their reward pool" and "Officer fleets in Faction Skirmishes in your Star Charts have 30% increased size".
- Restless Board: "The atlas takes a turn on every third other Star Chart Boss killed, instead of every fourth" (new, `OfficerTurns`), so carried items gather on winning officers faster.

Keystone:

- Blood Feud: "Atlas officers you defeat carry 2 of the 3 offered items" and "Atlas officers' flagship fleets in your Star Charts have 30% increased size".

## Open questions

1. Which anomalies get a volume of their own and which share one: the tree plan's Volumes table groups the storm anomalies and gives Breach, Relay and Convoy their own, and the rest aren't placed yet.
