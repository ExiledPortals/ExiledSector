# The Outer Reaches (atlas)

Status: planned, nothing built.

In game the atlas is called The Outer Reaches: the uncharted space beyond the Persean Sector that the Wormhole Gateway reaches. Its button is "Outer Reach Atlas", and the Tablet equivalent is "Outer Reach Intel", which keeps clear of vanilla's intel screen. Code and this plan keep the short words atlas and intel.

The atlas replaces "pick a chart, open a wormhole" with a persistent star map that keeps generating as the player pushes outward, modelled on Path of Exile 2's Atlas of Worlds. Its panel works like the skill tree canvas, with star systems in place of skill nodes. Path of Exile gets its variety from a hundred hand-made map layouts. The atlas gets it from combining generated traits on each system (region, layout, faction, boss, expansions, special sites), so a handful of data rows produce many distinct places.

## What Path of Exile 2 does

The Atlas is an endless, procedurally generated graph of map nodes, unique to each character. You start at a fixed origin. A node can be entered once a neighbour is completed, and most of the graph is hidden until you get close to it.

The node decides where you go: its biome (forest, desert, swamp, city), its layout, and its content icons (map boss, Breach, Delirium, Ritual, Expedition, Abyss). The Waystone decides how hard it is. Waystones are single-use consumables with a tier from 1 to 15 and up to six crafted modifiers, and any Waystone can go into any reachable node. Waystones drop at or above the tier of the map you're in, map bosses drop higher ones, three of one tier combine into the next, and tier 1 Waystones can be bought, so the player can't run out.

Completing a node's boss completes the node. Dying too often loses the Waystone and the node's bonus content, so the player routes around it.

Special nodes give the player reasons to travel. Precursor Towers reveal a large radius around them and drop Tablets, which you add to a Waystone to add or stack one kind of league content. Corrupted zones hold a Corrupted Nexus that needs a heavily modified Waystone; beating it cleanses the nodes around it into Cleansed Maps with exclusive loot. Three Citadels sit far out, need a tier 15 Waystone, allow one attempt each, and drop the keys to a pinnacle boss. Unique maps (Untainted Paradise, Vaults of Kamasa, Castaway) bend one reward rule hard.

Atlas passive points first came from completing maps, bosses and Nexus cleanses. Patch 0.5.0 moved them to a Fortress storyline that rises after the first Tower and added fixed landmarks at set bearings from the origin, plus "Masters of the Atlas" with a few swappable choice nodes each. All of it can be respecced for free.

## Charts and systems split

A node says where and a chart says how hard, as with Waystones. The chart item keeps its tier, rarity, prefixes, suffixes and crafting, and loses its system type and its inhabiting-faction implicit; both move to the node. The six chart bases in `star_charts.csv` collapse into one chart per tier. Stored charts are converted by dropping their system type (only our saves matter before 1.0).

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
| Expansion | At most one expansion marker, rolled as described under Expansions, more often further out. | `chart_expansions.csv` |
| Special site | Rare per-chunk rolls (Sensor Array, Infestation Nexus, unique systems), plus fixed positions for Bastions. | `atlas_specials.csv` |

Further from the origin, nodes roll more markers and rarer regions. Distance never gates the tier, matching PoE2.

## Movement and visibility

The origin is the player's Wormhole Gateway, shown as a completed node at the centre. A node is reachable when a neighbour is completed. Nodes within 2 steps of a completed node are visible; beyond that only faint stars show. Sensor Arrays reveal 6 steps around them.

## Running a node

Select a chart in the Star Charts window, press Outer Reach Atlas, pick a reachable node, optionally add Outer Reach Intel, and confirm the existing Open Wormhole dialog. The existing Gateway rules still apply (docked at the Gateway planet, one open run, the entries setting). Killing the boss (the chart objective) completes the node and opens its neighbours.

When a run closes without the objective done, the chart is spent and the node loses its markers but can be attempted again with another chart (the PoE2 0.5 rule).

## Keeping charts flowing

Chart drops in a run never fall below the run tier. Bosses keep their tier-up chance and Powerful Bosses guarantee one. The crafting bench combines three charts of one tier into one of the next tier. The Gateway sells tier 1 charts for credits, so the atlas can't stall. Outside drops (battles, salvage, tech mining) stay as they are.

## Expansions

The atlas is built before any expansion in `chart_expansions/`. It ships with the expansion slot in place and no expansions in the data, so each expansion later adds a data row and a class without touching atlas code.

One place decides a run's expansions. When a run opens, `ChartContentPlan` collects expansion requests from every source: the node's marker, the chart's `CHART_<EXPANSION>_CHANCE` lines, intel, and later atlas passives. The overview's fight budget still holds: a node rolls at most one marker, and more expansions come only from what the player adds.

Node rolls survive new expansions and mods. Each expansion rolls on its own hash of the seed, the cell and the expansion id, so adding an expansion in an update, or installing Industrial Evolution for the Asteroid Stream, moves no other node's rolls. When more than one passes, the lowest hash wins. Every other trait rolls on its own salted hash for the same reason.

`ChartExpansion` gets optional atlas hooks with defaults, so an expansion that lives only inside its system ignores them. They give a weight for a node from its traits (Faction Skirmish prefers territory borders next to a hostile faction, Spatial Breach prefers nebula regions) and a small per-expansion entry in `AtlasState` for anything that spans runs. Marker icons, tooltip lines and intel come from `chart_expansions.csv` columns, so the node drawer and the intel item need nothing per expansion.

Outer Reach Intel is the Tablet equivalent: one item type keyed by expansion id, added at opening, which adds its expansion to the run or strengthens one the node already has, stacking with intel of the same type. Intel drops only for expansions that exist, so it stays out of loot until the first one ships. This answers the open point in `chart_expansions/overview.md` about how players favour one expansion.

Expansion tuning, region effects and later atlas passives all reach a run as `ChartEffect` lines through `ChartGenContext`, next to the chart's own lines, so none of them needs a new pipeline.

## Special sites

A Sensor Array is a short run with a boss. Completing it reveals 6 steps around it, drops intel and grants an atlas point.

An infestation is a cluster of infested nodes around an Infestation Nexus. There are two kinds: Remnant infestations, and Threat infestations, which appear only once the player has met the Threat (vanilla's `$encounteredThreat`). Infested nodes have stronger fleets and a better loot multiplier. The Nexus needs a chart with at least four modifiers. Clearing it turns every infested node within 2 steps into a Cleansed node with exclusive drops, and grants an atlas point. Infestations never spread, because charts have no timers.

Bastions are three nodes at fixed bearings about 25 steps out, so every save has the same long-range goals. Each needs a tier 15 chart and allows one attempt. Each drops a key fragment, and three fragments open a pinnacle encounter at the Gateway. The pinnacle gets its own plan.

Unique systems are new content (the old unique charts were removed 2026-10-23). Each one bends a single rule hard: all loot and no fleets, a huge boss, or every POI a cache.

## Atlas points and passives

Points come from milestones: the first boss kill at each tier (15), each Sensor Array, each Nexus and each Bastion. Respecs are free. What the passives are, and whether they form a small tree on the atlas canvas or Masters-style choice slots, gets its own plan. They are how players specialise in regions, expansions and boss farming.

## UI

The Star Charts window keeps its storage and crafting, and its Open Wormhole button becomes Outer Reach Atlas (`chart.ui.open.button`), which shows the atlas panel with the selected chart; the player picks a node there, and the Open Wormhole confirmation moves to that step. The Open Anywhere dev switch applies to that confirmation. The canvas reuses the skill tree's: `TreeCamera`, `SmoothZoom`, `PointerGesture`, `CameraPanAnimation`, `ScreenRect`, `ChromeMetrics` and `TreeLayers` move from `exiledsector-skill-tree` to `exiledsector-core`, because the star charts module sees only core. The atlas gets its own node drawer built on the shared ring, icon and connector rendering, the hyperspace backdrop, a search bar for markers, and tooltips through the existing tooltip renderer. Only visible chunks are drawn or generated.

## Save footprint

`AtlasState` stores the seed, the generator version, a map from packed cell coordinates to a state byte (completed, failed, markers spent), completed special ids, atlas points and key fragments: a few hundred entries in a long game. Chart systems keep the zero-trace rule. A generator change reshuffles unvisited nodes only, because state is keyed by coordinates.

## Foiler mods compatibility

Everything in the atlas and the chart expansions must work with Foiler's mods (`foilerHydro`, `foilerTerrain`, `foilerGunFX`, `foilerHyperspaceH`, kept at `E:\Dev\other mods\foilers`). Terrain FX rebuilds each battle's background from the campaign location. It sorts terrain by vanilla classes, reads star and planet specs, never uses system ids or hyperspace links, and holds campaign objects only until shortly after combat, so chart systems need no cleanup hook for it. Audited 2026-10-23 against Hydrofoil 1.11.2 and Terrain FX 1.07.

Rules for what we add:

- Never build a chart mechanic on combat nebula tiles (`CombatNebulaAPI`): Terrain FX clears every tile and draws its own clouds. Combat effects of expansion terrain live in our own plugin logic.
- Custom terrain (Spatial Breach, infestation visuals) extends `BaseTerrain` or `BaseTiledTerrain`, never `HyperspaceTerrainPlugin`, which would switch the battle to hyperspace mode. Extending `NebulaTerrainPlugin` is allowed and gets nebula weather tinted from our texture.
- Preload every texture that chart or combat visuals use at load time. Hydrofoil unloads textures it loaded itself after combat, so a texture first loaded lazily during a battle can be deleted under us.
- Chart systems get no vanilla jump points and no `system_abyssal` tag.
- Layouts use only vanilla star, planet and terrain types. Black hole, neutron star and nebula layouts are tested in game with Terrain FX on, since those are its fragile paths.
- Atlas UI tints don't assume vanilla's blue hyperspace textures, which Hot Hyperspace recolours.
- Chart entities that orbit planets can get proper backgrounds through a shipped `data/config/foilerterrainfx.json` (`customEntities` keyed by texture or one unique tag, complete entries only, since one malformed entry drops the rest of the file).
- `foilerTerrain` and `foilerHydro` get CompatTarget self-checks.

## Unchanged

Star Chart Progress for hull upgrade points (highest cleared tier), the entries setting, chart crafting and currency, the loot framework, and the console commands.

## Decisions

Decided 2026-10-23: failed nodes can be attempted again. The infestation design stands as written, with both Remnant and Threat infestations. Atlas passives get their own plan. The in-game name is The Outer Reaches, with the Outer Reach Atlas button and Outer Reach Intel. There is one atlas per save, and every Gateway the player owns opens it. Distance from the origin never gates the tier.

## Open questions

1. Names: Sensor Array, Infestation, Nexus and Bastion are placeholders.

## Build order

1. [ ] Move the generic canvas classes to core, with no behaviour change.
2. [~] Expansion framework (step 1 of `chart_expansions/overview.md`; built without the atlas hooks and per-node roll, see there) with the atlas hooks: `ChartExpansion`, `chart_expansions.csv` with icon and intel columns, the per-expansion hash roll, expansion requests in `ChartContentPlan`, run state and cleanup, and a test-only expansion that places nothing.
3. [ ] Atlas generator as pure, unit-tested Java (layouts limited to vanilla star, planet and terrain types, see Foiler mods compatibility): lattice, voids, regions, factions, layouts, boss and expansion markers, special sites. Determinism and connectivity tests, plus a test that adding an expansion moves no other roll. New CSVs added to the editor serialisers.
4. [ ] `AtlasState`, reachability and visibility.
5. [ ] Chart and system split: node traits feed `ChartGenContext` and `ChartLayoutRequest`, stored charts converted.
6. [ ] Atlas panel: canvas, node drawer, tooltips, the Outer Reach Atlas button, node selection and the moved Open Wormhole confirmation. Markers drawn from the expansion data, checked with the test expansion.
7. [ ] Completion and failure wired through the objectives and `ChartRunCloser`. CompatTarget self-checks for `foilerTerrain` and `foilerHydro`, and an in-game pass of black hole, neutron star and nebula layouts with Terrain FX on.
8. [ ] Chart supply: tier floor, three-into-one combine, Gateway vendor.
9. [ ] Sensor Arrays and reveal.
10. [ ] Infestations, Nexus and Cleansed nodes.
11. [ ] Bastions and key fragments (pinnacle in its own plan).
12. [ ] First expansion (Relay Network, per the overview's order) together with the intel item.
13. [ ] The other expansions, one at a time, each to its own plan.
14. [ ] Atlas points and passives (own plan).
