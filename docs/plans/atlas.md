# The Outer Reaches (atlas)

Status: in progress. Steps 1, 3, 3b, 4, 5, 8 and 15 are built; 2, 3c, 6, 7 and 9 to 11 are partly built and not yet tried in game; 12 to 14 are not started (see the build order).

In game the atlas is called The Outer Reaches: the uncharted space beyond the Persean Sector that the Wormhole Gateway reaches. Its button is "Outer Reach Atlas", and the Tablet equivalent is "Outer Reach Intel", which keeps clear of vanilla's intel screen. Code and this plan keep the short words atlas and intel.

The atlas replaces "pick a chart, open a wormhole" with a persistent star map that keeps generating as the player pushes outward, modelled on Path of Exile 2's Atlas of Worlds. Its panel works like the skill tree canvas, with star systems in place of skill nodes. Path of Exile gets its variety from a hundred hand-made map layouts. The atlas gets it from combining generated traits on each system (region, layout, phenomenon, faction, boss, expansions, special sites), so a handful of data rows produce many distinct places.

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

A seed per save and a generator version. Space is a square grid cut into chunks (8 by 8 cells), and every trait is a pure function of the seed and the cell coordinates. Nothing generated is saved except the biome decisions described below; chunks are built on demand into a transient cache when they come into view.

Systems are sparse and irregular. Each cell holds at most one system, placed anywhere inside it, so neighbouring systems sit at varying distances and no grid shows. Some cells are empty space, but a cell whose coordinates are both even never is, so every 2 by 2 block keeps a system. Lanes join systems that see each other with nothing in between (a Gabriel graph): every system is reachable, and lanes stay shorter than about four cells, so a chunk only needs its neighbours to work them out. Distances in this plan (min_steps, Bastions, radii) are in cells; "steps" for visibility and reveals are lanes.

Most of the atlas is normal space: plain hyperspace with a starscape, whose systems use ordinary generation. Unique biomes are patches in it. Each biome site gathers 4 to 7 systems from a sparser sub-grid and clears every other system inside its patch, so a biome is visually large but holds few systems. Patches never overlap and keep at least a cell of normal space between them. On the canvas a biome is drawn as a soft patch around its systems that fades to black. A site can also roll normal, which leaves gaps. A biome behind a mod or an unlock is decided when its site first comes out of the fog, with the unlocks the player has then, and that decision is saved, so revealed biomes never change (unless their required mod is removed, which turns them back into normal space).

Each node gets these traits.

| Trait | How it's chosen | Data |
|---|---|---|
| Region (the biome) | Normal space, or the biome whose site claims the system. Regions set the look, the layout weights, the POI category mix, the faction pool and phenomenon and expansion leanings (see Regions, layouts and phenomena). Examples: Derelict Graveyard, Remnant Expanse, Frozen Fringe, Furnace, Pirate Badlands, Collapsed Reach, Abyssal Rift (RAT only), Threat Expanse (after vanilla's `$encounteredThreat`). | `atlas_regions.csv` |
| Layout (the map type) | Weighted by region. Each row is a recipe that constrains vanilla's generator: system type, planet count range, orbit spread, cosmetic terrain and one signature feature. Aim for about 30 rows at launch; this is where map-type count grows cheaply. | `atlas_layouts.csv`, `atlas_features.csv` |
| Phenomenon | At most one, rolled like an expansion and leaned on by the region, more often further out. | `chart_phenomena.csv` |
| Faction | A Voronoi layer of large territories that span biomes and normal space. A region with a faction pool that lacks the territory faction swaps it for one pool faction per territory. Replaces the faction implicit. | faction pool per region |
| Boss | Every node has one. About 1 in 4 carries a Powerful Boss marker: a bigger boss and a guaranteed tier-up chart. | `atlas_markers.csv` |
| Expansion | At most one expansion marker, rolled as described under Expansions, more often further out. | `chart_expansions.csv` |
| Special site | Rare per-chunk rolls (Sensor Array, Infestation Nexus, unique systems), plus fixed positions for Bastions. | `atlas_specials.csv` |

Further from the origin, nodes roll more markers and rarer biomes. Distance never gates the tier, matching PoE2.

A node's star, planets and terrain come from its cell seed, so a node looks the same every time it is opened, including after a failed run. Fleets, POIs and expansions come from the chart seed.

## Regions, layouts and phenomena

Today every chart is vanilla's generator with every planet category allowed, so systems blur together. Variety comes from constraint: a region or layout is defined as much by what it leaves out as by what it adds.

A region is a look and a shape palette, with no implicit effect lines. The `normal` row is normal space; every other row is a biome. It sets the background pool (separate pools for nebula and clear systems), a light tint, weights for ordinary star types and planet categories, an optional music set (check vanilla's per-system music memory key first), the layout weights, the POI mix, the faction pool, and weight multipliers for phenomena and expansions. A region's theme is a lean, never a guarantee: the Collapsed Reach leans hard towards black holes and neutron stars, but most of its systems have an ordinary star.

A layout constrains vanilla's generator (system type, planet count, how far out the orbits reach, cosmetic terrain such as rings, dust and debris fields) and names one signature feature from `atlas_features.csv`: a small class building a set-piece such as a ringed giant with a court of moons, a debris ring around a dead star, twin worlds or a rogue planet. The boss fleet sits at the feature and the wormhole opens on the far side, so the shape a player remembers is also where the fight is. Planets keep their ruins in regions that want them, and `chart_poi.csv` gets a `planet_categories` column so sites such as cryo vaults only appear on matching worlds and a planet's look hints at its loot.

Phenomena are the gameplay changers: Black Hole (event horizon, accretion disk), Neutron Star (pulsar beam), Dense Nebula (starless nebula system), Asteroid Storm (dense belts and fields with impacts), Magnetic Storm (magnetic fields that blind sensors) and Stellar Flares (corona flares). They are kept out of ordinary generation: layouts never pick black holes or neutron stars, never build nebula systems, and strip vanilla's random magnetic fields and corona flares, so these only come through a phenomenon. A phenomenon is one class behind `ChartPhenomenon` with one row in `chart_phenomena.csv` (`id,weight,min_tier,max_tier,requires_mod,icon,kind,note`). It shares the expansion roll (its own hash per id, weight from the node's traits, `CHART_<PHENOMENON>_CHANCE` lines, marker icon and tooltip line), but runs before and during layout generation instead of after content. Phenomena of kind `star` replace the star or the system type, so they come only from the node roll, which keeps the node's layout fixed. Phenomena of kind `overlay` add terrain on top of the layout, so chart lines and intel can add them as well. A node rolls at most one phenomenon, in its own slot next to the expansion marker.

## Unknown Skies compatibility

Unknown Skies (`US`) adds planet types to vanilla's `planet_gen_data.csv`, so they already appear in charts when it is installed. Its texture swaps for planets and stars and its background pool only run at new game, so chart systems miss them. When it is loaded, the generator reads its background map (`unknownSkies.backgrounds` in its `modSettings.json`, keyed by star age, nebula backgrounds ending in `n.jpg`) and its star sprite variants from the installed mod, never copied into ours, and applies them with our seeded randoms instead of its `new Random()`. Its swaps keep the planet type, so Terrain FX and conditions are unaffected. It gets a CompatTarget self-check.

## Movement and visibility

The origin is the player's Wormhole Gateway, shown as a completed node at the centre. A node is reachable when a neighbour is completed. Nodes within 2 lanes of a completed node are visible; beyond that only faint stars show. Sensor Arrays reveal 6 lanes around them.

## Running a node

Select a chart in the Star Charts window, press Outer Reach Atlas, pick a reachable node, optionally add Outer Reach Intel, and confirm the existing Open Wormhole dialog. The existing Gateway rules still apply (docked at the Gateway planet, one open run, the entries setting). Killing the boss (the chart objective) completes the node and opens its neighbours.

When a run closes without the objective done, the chart is spent and the node loses its markers but can be attempted again with another chart (the PoE2 0.5 rule).

## Keeping charts flowing

Chart drops in a run never fall below the run tier. Bosses keep their tier-up chance and Powerful Bosses guarantee one. The crafting bench combines three charts of one tier into one of the next tier. The Gateway sells tier 1 charts for credits, so the atlas can't stall. Outside drops (battles, salvage, tech mining) stay as they are.

## Expansions

The atlas is built before any expansion in `chart_expansions/`. It ships with the expansion slot in place and no expansions in the data, so each expansion later adds a data row and a class without touching atlas code.

One place decides a run's expansions. When a run opens, `ChartContentPlan` collects expansion requests from every source: the node's marker, the chart's `CHART_<EXPANSION>_CHANCE` lines, intel, and later atlas passives. The overview's fight budget still holds: a node rolls at most one expansion marker (plus at most one phenomenon, which adds no fights), and more expansions come only from what the player adds.

Node rolls survive new expansions and mods. Each expansion rolls on its own hash of the seed, the cell and the expansion id, so adding an expansion in an update, or installing Industrial Evolution for the Asteroid Stream, moves no other node's rolls. When more than one passes, the lowest hash wins. Every other trait rolls on its own salted hash for the same reason.

`ChartExpansion` gets optional atlas hooks with defaults, so an expansion that lives only inside its system ignores them. They give a weight for a node from its traits (Faction Skirmish prefers territory borders next to a hostile faction, Spatial Breach prefers nebula regions) and a small per-expansion entry in `AtlasState` for anything that spans runs. Marker icons, tooltip lines and intel come from `chart_expansions.csv` columns, so the node drawer and the intel item need nothing per expansion.

Outer Reach Intel is the Tablet equivalent: one item type keyed by expansion id, added at opening, which adds its expansion to the run or strengthens one the node already has, stacking with intel of the same type. Intel drops only for expansions that exist, so it stays out of loot until the first one ships. This answers the open point in `chart_expansions/overview.md` about how players favour one expansion.

Expansion tuning, region effects and later atlas passives all reach a run as `ChartEffect` lines through `ChartGenContext`, next to the chart's own lines, so none of them needs a new pipeline.

## Special sites

A Sensor Array is a short run with a boss. Completing it reveals 6 lanes around it, drops intel and grants an atlas point.

An infestation is a cluster of infested nodes around an Infestation Nexus. There are two kinds: Remnant infestations, and Threat infestations, which appear only once the player has met the Threat (vanilla's `$encounteredThreat`). Infested nodes have stronger fleets and a better loot multiplier. The Nexus needs a chart with at least four modifiers. Infested nodes are those within a radius of the Nexus (2.5 cells to start). Clearing it turns every infested node into a Cleansed node with exclusive drops, and grants an atlas point. Infestations never spread, because charts have no timers. Like biomes, a Threat infestation is decided when its chunk is revealed, so one never appears in space the player has already seen.

Bastions are three nodes at fixed bearings 25 cells out, so every save has the same long-range goals. Each needs a tier 15 chart and allows one attempt. Each drops a key fragment, and three fragments open a pinnacle encounter at the Gateway. The pinnacle gets its own plan.

Unique systems are new content (the old unique charts were removed 2026-10-23). Each one bends a single rule hard: all loot and no fleets, a huge boss, or every POI a cache.

## Atlas points and passives

Points come from milestones: the first boss kill at each tier (15), each Sensor Array, each Nexus and each Bastion. Respecs are free. What the passives are, and whether they form a small tree on the atlas canvas or Masters-style choice slots, gets its own plan. They are how players specialise in regions, expansions and boss farming.

## UI

The Star Charts window keeps its storage and crafting, and its Open Wormhole button becomes Outer Reach Atlas (`chart.ui.open.button`), which shows the atlas panel with the selected chart; the player picks a node there, and the Open Wormhole confirmation moves to that step. The Open Anywhere dev switch applies to that confirmation. The canvas reuses the skill tree's camera and input classes, which live in `exiledsector-core` (`exiledsector.ui`) because the star charts module sees only core: `CanvasCamera` (pan, zoom, drag, follow, viewport), `SmoothZoom`, `PointerGesture` (generic over the canvas's mode type), `CameraPanAnimation`, `ScreenRect` and `ChromeMetrics`. The skill tree's `TreeCamera` extends `CanvasCamera` with the starting-root follow and hyperspace camera, and `TreeLayers` stays in the skill tree since it only splits exoship nodes. The atlas gets its own node drawer built on the shared ring, icon and connector rendering, the hyperspace backdrop, a search bar for markers, and tooltips through the existing tooltip renderer. Only visible chunks are drawn or generated.

## Save footprint

`AtlasState` stores the seed, the generator version, a map from packed cell coordinates to a state byte (completed, failed, markers spent), completed special ids, atlas points, key fragments, the region decided for each revealed biome site and the Threat infestation decision for each revealed chunk: a few hundred entries in a long game. Chart systems keep the zero-trace rule. A generator change reshuffles unvisited nodes only, because state is keyed by coordinates.

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

Star Chart Progress for hull upgrade points (highest cleared tier), the entries setting, chart crafting and currency, the loot framework, and the existing console commands.

## Decisions

Decided 2026-10-23: failed nodes can be attempted again. The infestation design stands as written, with both Remnant and Threat infestations. Atlas passives get their own plan. The in-game name is The Outer Reaches, with the Outer Reach Atlas button and Outer Reach Intel. There is one atlas per save, and every Gateway the player owns opens it. Distance from the origin never gates the tier.

Decided 2026-10-24: gameplay-changing terrain (black holes, neutron stars and pulsar beams, dense nebulae, asteroid impacts, magnetic fields, stellar flares) is a phenomenon marker rolled like an expansion, not bound to a region; regions only lean on it. A node's star, planets and terrain are fixed per cell. Regions carry no implicit effect lines or loot leanings. Unknown Skies gets optional support. Biomes are patches of 4 to 7 systems in normal space, which is the default, rather than regions tiling the map; locked biomes are decided when first revealed.

## Open questions

1. Names: Sensor Array, Infestation, Nexus and Bastion are placeholders, as are the region and phenomenon names.

## Build order

1. [x] Move the generic canvas classes to core, with no behaviour change (see UI; tests in core's `exiledsector.ui`).
2. [~] Expansion framework (step 1 of `chart_expansions/overview.md`; the weight hook `ChartExpansion.atlasWeight` and the per-node roll are built with step 3, the `AtlasState` entry, node marker requests and icon/intel use wait for steps 4 to 6) with the atlas hooks: `ChartExpansion`, `chart_expansions.csv` with icon and intel columns, the per-expansion hash roll, expansion requests in `ChartContentPlan`, run state and cleanup, and a test-only expansion that places nothing.
3. [x] Atlas generator as pure, unit-tested Java (layouts limited to vanilla star, planet and terrain types, see Foiler mods compatibility): sparse systems and lanes, normal space and biome patches, factions, layouts, boss, phenomenon and expansion markers, special sites. Determinism and connectivity tests, plus a test that adding a region, expansion or phenomenon moves no other roll. New CSVs added to the editor serialisers. Built: package `exiledsector.charts.atlas` (`AtlasGenerator` with `AtlasBiomes`, `AtlasTerritories` and `AtlasSpecialPlacer`, `AtlasCell`, `AtlasBiome`, `AtlasData` loading `atlas_regions.csv` with its `normal` row, `atlas_layouts.csv` with placeholder ids until 3b, `atlas_markers.csv` and `atlas_specials.csv`), tests in the matching test package, and four Atlas tabs in the editor's Items page. Biome systems come from the every-other-cell sub-grid, whose cells are never empty, and the patch clears every other system inside it; a site survives unless a surviving higher-priority patch comes within a cell of it. Patch shape never depends on the region, so unlocks and recorded biomes only change which region a patch is. Every weighted pick is a per-id hash race, so adding a row only moves what it wins. With the shipped weights biomes cover about a fifth of space and hold about a tenth of systems. Phenomenon candidates stay empty until 3c.
3b. [x] Layout recipes: region palettes (backgrounds for clear and nebula systems, nebula chance, light tint, star and planet category weights, keeping ruins) and layout recipes (system type weights, orbit count, how far out orbits reach, terrain weights, feature) in `atlas_regions.csv` and `atlas_layouts.csv`, turned into a `ChartLayoutRecipe` by `AtlasData.recipeFor` and applied by `ChartStarSystemGenerator` (overrides of the star, category, background and orbit picks). Black holes, neutron stars and magnetic fields never come from ordinary picks, and corona flares are switched off. `atlas_features.csv` with four set-pieces in `charts.feature` (ringed giant, debris ring, twin worlds, rogue planet), built past the last orbit; the boss holds at the feature and the inner wormhole opens on the far side. `chart_poi.csv` has `planet_categories` (stations only so far). Vanilla's orbit spacing is a constant, so compact and sprawling come from orbit count and reach. Region music is a `music` column (vanilla's `$musicSetId` system memory key), blank in the data for now. Until step 5 chart runs use the plain recipe; `ExiledSectorChartOpen region=ID layout=ID` builds a system from the tables for testing.
3c. [~] Phenomenon framework: `ChartPhenomenon`, `chart_phenomena.csv`, the shared roll, the layout-stage hook and cleanup, then Black Hole, Neutron Star, Dense Nebula, Asteroid Storm, Magnetic Storm and Stellar Flares. In-game pass with Terrain FX on. Built: `charts.phenomenon` (star kind forces the primary star or the system type before layout; black holes and neutron stars force a single-star system so vanilla cannot swap them out; Dense Nebula hides the centre star as vanilla does; overlays add terrain after layout), `ChartPhenomena` loading `chart_phenomena.csv`, the atlas phenomenon roll, `phenomenon=` on `ExiledSectorChartOpen`, and the editor table. `CHART_ASTEROID_STORM_CHANCE`, `CHART_MAGNETIC_STORM_CHANCE` and `CHART_STELLAR_FLARES_CHANCE` chart suffixes add overlay phenomena when the node has none (`ChartPhenomena.rolledOverlay`). Not built: the marker icons (all use a placeholder) and the in-game pass.
4. [x] `AtlasState`, reachability and visibility, and recording the biome region of each site and the Threat infestation of each chunk when first revealed. Built: `AtlasState` (persistent data, implements `AtlasMemory`), `AtlasReach` (reachable, visible within 2 lanes or 6 around a completed Sensor Array, spent Bastions), `AtlasRuntime` (one cached generator per save and unlock set). Dev console commands `ExiledSectorAtlasStatus`, `Reach`, `Complete`, `Fail`, `Grant` and `Reset` (`AtlasDevTools`).
5. [x] Chart and system split: node traits feed `ChartGenContext` and `ChartLayoutRequest`, stored charts converted. Built: `AtlasOpening` (checks and builds the request: layout seed from the cell, content seed from the chart, node faction, region and layout recipe, phenomenon, forced expansion, `atlas_node_effects.csv` lines for Powerful Boss, infested, cleansed and special sites), `AtlasRunNode` stored on the run so run-time effects keep the node, guaranteed tier-up boss chart (once per run, on the first boss loot roll). The six chart bases are one `star_chart` (old ids aliased), charts no longer roll a faction implicit (old stored ones drop), and Attunement no longer applies to charts.
6. [~] Atlas panel: canvas, node drawer, tooltips, the Outer Reach Atlas button, node selection and the moved Open Wormhole confirmation. Markers drawn from the expansion data, checked with the test expansion. Built: `ui.atlas.AtlasPanel` (full-bench canvas on `CanvasCamera`, biome glows, lanes, systems coloured by state, marker dots, hover tooltips from `AtlasNodeText`, click a reachable system to confirm) and a target-picking step in the bench (`BenchModeSupport.TargetPicker`, `openAt`). Not built: marker icons from the data (dots for now), a search bar, the hyperspace backdrop (a seeded starfield instead). Not yet run in game.
7. [~] Completion and failure wired through the objectives and `ChartRunCloser`. CompatTarget self-checks for `foilerTerrain` and `foilerHydro`, and an in-game pass of black hole, neutron star and nebula layouts with Terrain FX on. Built: `AtlasProgress`, `FoilerCompat`. Not done: the in-game pass.
8. [x] Chart supply: tier floor, three-into-one combine, Gateway vendor. Built: inside-system drops never fall below the run tier (`ChartDrops`), Combine three in Star Chart storage (unique charts are never spent) and the Gateway's Buy option (`ChartSupply`, placeholder price 15000 credits).
9. [~] Sensor Arrays and reveal. Built: the 6-lane reveal, atlas point and short-run node effects. Not built: the intel drop (waits for the intel item).
10. [~] Infestations, Nexus and Cleansed nodes. Built: infested systems take the infestation faction and stronger lines, a Nexus needs four modifiers and cleanses on completion, cleansed systems get better loot lines. Not built: exclusive Cleansed drops (better rarity and reward stand in).
11. [~] Bastions and key fragments (pinnacle in its own plan). Built: tier 15 requirement, one attempt, key fragments. Not built: the pinnacle encounter.
12. [ ] First expansion (Relay Network, per the overview's order) together with the intel item.
13. [ ] The other expansions, one at a time, each to its own plan.
14. [ ] Atlas points and passives (own plan).
15. [x] Unknown Skies support: backgrounds and star variants read from the installed mod, CompatTarget self-check. Built: `UnknownSkiesCompat` (its background map joins every region's pool by star age; its star variants are applied with the chart's seeded random and follow its Star Variants setting).
