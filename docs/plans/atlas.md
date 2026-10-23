# Atlas

Status: planned, nothing built.

The atlas replaces "pick a chart, open a wormhole" with a persistent star map that keeps generating as the player pushes outward, modelled on Path of Exile 2's Atlas of Worlds. Its panel works like the skill tree canvas, with star systems in place of skill nodes. Path of Exile gets its variety from a hundred hand-made map layouts. The atlas gets it from combining generated traits on each system (region, layout, faction, boss, expansions, special sites), so a handful of data rows produce many distinct places.

## What Path of Exile 2 does

The Atlas is an endless, procedurally generated graph of map nodes, unique to each character. You start at a fixed origin. A node can be entered once a neighbour is completed, and most of the graph is hidden until you get close to it.

The node decides where you go: its biome (forest, desert, swamp, city), its layout, and its content icons (map boss, Breach, Delirium, Ritual, Expedition, Abyss). The Waystone decides how hard it is. Waystones are single-use consumables with a tier from 1 to 15 and up to six crafted modifiers, and any Waystone can go into any reachable node. Waystones drop at or above the tier of the map you're in, map bosses drop higher ones, three of one tier combine into the next, and tier 1 Waystones can be bought, so the player can't run out.

Completing a node's boss completes the node. Dying too often loses the Waystone and the node's bonus content, so the player routes around it.

Special nodes give the player reasons to travel. Precursor Towers reveal a large radius around them and drop Tablets, which you add to a Waystone to add or stack one kind of league content. Corrupted zones hold a Corrupted Nexus that needs a heavily modified Waystone; beating it cleanses the nodes around it into Cleansed Maps with exclusive loot. Three Citadels sit far out, need a tier 15 Waystone, allow one attempt each, and drop the keys to a pinnacle boss. Unique maps (Untainted Paradise, Vaults of Kamasa, Castaway) bend one reward rule hard.

Atlas passive points first came from completing maps, bosses and Nexus cleanses. Patch 0.5.0 moved them to a Fortress storyline that rises after the first Tower and added fixed landmarks at set bearings from the origin, plus "Masters of the Atlas" with a few swappable choice nodes each. All of it can be respecced for free.

## Charts and systems split

A node says where and a chart says how hard, as with Waystones. The chart item keeps its tier, rarity, prefixes, suffixes and crafting, and loses its system type and its inhabiting-faction implicit; both move to the node. The eight chart bases in `star_charts.csv` collapse into one chart per tier. Unique charts become unique nodes. Stored charts are converted by dropping their system type (only our saves matter before 1.0).

`ChartGenContext` and `ChartLayoutRequest` take the node's traits next to the chart's, so the content planner, loot framework, objectives and run lifecycle are reused unchanged.

## Generation

A seed per save and a generator version. Nodes sit on a jittered hex lattice cut into chunks (8 by 8 cells), and every trait is a pure function of the seed and the cell coordinates. Nothing generated is saved; chunks are built on demand into a transient cache when they come into view. Some cells are voids (empty space). Voids are never adjacent to each other, which keeps the lattice connected without a global check.

Each node gets these traits.

| Trait | How it's chosen | Data |
|---|---|---|
| Region (the biome) | Voronoi cells over sparse region seeds, so regions form contiguous areas. Regions set star and terrain bias, the POI category mix, the faction pool and drop leanings. Examples: Derelict Graveyard, Remnant Expanse, Nebula Reach, Pirate Badlands, Pulsar Fields, Black Hole Deep, Abyssal Rift (RAT only), Threat Expanse (after vanilla's `$encounteredThreat`). | `atlas_regions.csv` |
| Layout (the map type) | Weighted by region. Each row maps to layout parameters: system type, star type and age, terrain set, planet count range. Aim for about 30 rows at launch; this is where map-type count grows cheaply. | `atlas_layouts.csv` |
| Faction | A coarser second Voronoi layer so factions hold territories that span regions. Replaces the faction implicit. | faction pool per region |
| Boss | Every node has one. About 1 in 4 carries a Powerful Boss marker: a bigger boss and a guaranteed tier-up chart. | `atlas_markers.csv` |
| Expansions | 0 to 2 expansion markers from `chart_expansions.csv` weights, more often further out. | `chart_expansions.csv` |
| Special site | Rare per-chunk rolls (Sensor Array, Infestation Nexus, unique systems), plus fixed positions for Bastions. | `atlas_specials.csv` |

Further from the origin, nodes roll more markers and rarer regions. Distance never gates the tier, matching PoE2.

## Movement and visibility

The origin is the player's Wormhole Gateway, shown as a completed node at the centre. A node is reachable when a neighbour is completed. Nodes within 2 steps of a completed node are visible; beyond that only faint stars show. Sensor Arrays reveal 6 steps around them.

## Running a node

Select a reachable node, choose a chart from storage, optionally add intel, and press Open Wormhole. The existing Gateway rules still apply (docked at the Gateway planet, one open run, the entries setting). Killing the boss (the chart objective) completes the node and opens its neighbours.

When a run closes without the objective done, the chart is spent and the node loses its markers but can be attempted again with another chart (the PoE2 0.5 rule).

## Keeping charts flowing

Chart drops in a run never fall below the run tier. Bosses keep their tier-up chance and Powerful Bosses guarantee one. The crafting bench combines three charts of one tier into one of the next tier. The Gateway sells tier 1 charts for credits, so the atlas can't stall. Outside drops (battles, salvage, tech mining) stay as they are.

## Expansions and intel

Expansion markers show which expansion a node rolls. Intel is the Tablet equivalent: a consumable added at opening that adds its expansion to the run, or strengthens it if the node already has one, stacking with intel of the same type. Intel drops from Sensor Arrays and from expansion content. This answers the open point in `chart_expansions/overview.md` about how players favour one expansion.

## Special sites

A Sensor Array is a short run with a boss. Completing it reveals 6 steps around it, drops intel and grants an atlas point.

An infestation is a cluster of infested nodes around an Infestation Nexus. Infested nodes have stronger fleets and a better loot multiplier. The Nexus needs a chart with at least four modifiers. Clearing it turns every infested node within 2 steps into a Cleansed node with exclusive drops, and grants an atlas point. Infestations never spread, because charts have no timers.

Bastions are three nodes at fixed bearings about 25 steps out, so every save has the same long-range goals. Each needs a tier 15 chart and allows one attempt. Each drops a key fragment, and three fragments open a pinnacle encounter at the Gateway. The pinnacle gets its own plan.

Unique systems take over the unique charts' role. Each one bends a single rule hard: all loot and no fleets, a huge boss, or every POI a cache.

## Atlas points and passives

Points come from milestones: the first boss kill at each tier (15), each Sensor Array, each Nexus and each Bastion. Respecs are free. What the passives are, and whether they form a small tree on the atlas canvas or Masters-style choice slots, gets its own plan. They are how players specialise in regions, expansions and boss farming.

## UI

The Wormhole Gateway opens the atlas panel instead of the chart-first window, and chart storage becomes a side panel for the selected node. The canvas reuses the skill tree's: `TreeCamera`, `SmoothZoom`, `PointerGesture`, `CameraPanAnimation`, `ScreenRect`, `ChromeMetrics` and `TreeLayers` move from `exiledsector-skill-tree` to `exiledsector-core`, because the star charts module sees only core. The atlas gets its own node drawer built on the shared ring, icon and connector rendering, the hyperspace backdrop, a search bar for markers, and tooltips through the existing tooltip renderer. Only visible chunks are drawn or generated.

## Save footprint

`AtlasState` stores the seed, the generator version, a map from packed cell coordinates to a state byte (completed, failed, markers spent), completed special ids, atlas points and key fragments: a few hundred entries in a long game. Chart systems keep the zero-trace rule. A generator change reshuffles unvisited nodes only, because state is keyed by coordinates.

## Unchanged

Star Chart Progress for hull upgrade points (highest cleared tier), the entries setting, chart crafting and currency, the loot framework, and the console commands.

## Decisions

Decided 2026-10-23: failed nodes can be attempted again. The infestation design stands as written. Atlas passives get their own plan. There is one atlas per save, and every Gateway the player owns opens it. Distance from the origin never gates the tier.

## Open questions

1. Which faction infests: Remnant, Threat (once encountered), or a new one?
2. Names: Atlas, Sensor Array, Infestation, Nexus, Bastion and Intel are placeholders.

## Build order

1. [ ] Move the generic canvas classes to core, with no behaviour change.
2. [ ] Atlas generator as pure, unit-tested Java: lattice, voids, regions, factions, layouts, markers, special sites. Determinism and connectivity tests. New CSVs added to the editor serialisers.
3. [ ] `AtlasState`, reachability and visibility.
4. [ ] Chart and system split: node traits feed `ChartGenContext` and `ChartLayoutRequest`, stored charts converted, unique charts become unique nodes.
5. [ ] Atlas panel: canvas, node drawer, tooltips, select node and chart, open.
6. [ ] Completion and failure wired through the objectives and `ChartRunCloser`.
7. [ ] Chart supply: tier floor, three-into-one combine, Gateway vendor.
8. [ ] Sensor Arrays and reveal.
9. [ ] Expansion markers and intel, after step 1 of `chart_expansions/overview.md`.
10. [ ] Infestations, Nexus and Cleansed nodes.
11. [ ] Bastions and key fragments (pinnacle in its own plan).
12. [ ] Atlas points and passives (own plan).
