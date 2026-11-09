# Atlas Tree

Status: in progress. Steps 1 to 4 and 6 are built (the shared abstractions, the data, the editor page, allocation and points, the screen); node effects do not reach charts or the atlas yet. This is the atlas points and passives plan that `atlas.md` defers to.

The Atlas Tree is a passive tree for the whole Outer Reaches, one per save rather than per ship, spent with Atlas Skill Points. Its nodes change what star charts hold and drop, change how anomalies behave, and change the atlas itself. It plays the part of Path of Exile 2's Atlas passive tree (see What Path of Exile 2 does in `atlas.md`), drawn the way our ship skill tree is drawn.

## Shape

The tree has two planes, as the ship skill tree does.

The starscape plane is the canvas players allocate on. Nodes gather around star systems (volumes), each a cluster of wheels around its star, scattered across a wide starfield. There is no core volume. The root node sits at the centre of the canvas in open space, allocated for free, and highways of small nodes run out from it to the nearest volumes and from those to the further ones. A node can be allocated when it touches an allocated node, so every allocated node keeps a path back to the root, and a node can be refunded only when nothing would lose that path.

Wormholes join distant volumes, as in the ship tree: a wormhole node is paired with one in another volume, and once one end is allocated its partner is reachable, so a far volume can be reached without crossing everything in between. Atlas wormholes cost a point like any node and need no anchor item.

The hyperspace plane is the zoomed-out map. Each volume shows as its star with its name and how many of its nodes are allocated, and clicking one flies the camera there, as the ship tree's hyperspace mode does today (`HyperspaceMode`, `HyperspaceAnchor`, `HyperspaceTransition`). It moves the camera only, not the allocation path.

## Distance

Nodes grow stronger and riskier the further they sit from the root. Near the root are plain rewards, such as more crafting items; further out are effects that make charts both harder and richer, such as increased effect of chart modifiers. This is a design leaning for laying out the tree, not a rule the code enforces.

## Volumes

Each volume has one loot reward theme, carried by its notables and its small nodes. Some volumes are tied to one anomaly and change how that anomaly behaves. The list below is a first pass to argue over, not a commitment; magnitudes are placeholders until balance.

| Volume (placeholder) | Theme | Example nodes |
|---|---|---|
| Salvors | Hull items | Hull item quantity, one part's item percent per wheel (`CHART_REACTOR_ITEM_PERCENT` and its siblings). |
| Archivists | Subroutines | Subroutine quantity, POI budget. |
| Swarm | Crafting items | Crafting item and Nanite Feedstock quantity. |
| Cartographers | Star Charts | Star Chart quantity, boss tier-up chance, cheaper three-into-one combine. |
| Catalysts | Atlas Catalysts | Catalyst quantity, a chance a used catalyst is not spent. |
| Wardens | Bosses | Boss size with boss spoils, Powerful Boss chance, twinned bosses that drop twice. |
| Breach | Spatial Breach | More breaches on the map, more breach fleets with better loot. |
| Relay | Relay Network | Longer chains, stronger later stages, stage rewards. |
| Convoy | Supply Convoy | Convoys more often, richer cargo, more escorts. |
| Storms | Storm Front, Magnetic Storm, Stellar Flares | Storm anomalies more often, storm loot. |
| Infestation | Infestations | Wider infested radius, stronger infested fleets with more loot. |
| Scouts | Revealing the map | Lanes of visibility, Sensor Array reveal radius, Sensor Arrays more often. |

A volume tied to a mod-only anomaly or biome (the Abyssal Rift with Random Assortment of Things, the Threat after `$encounteredThreat`) is hidden while its content can't appear, the way the ship tree hides gated volumes. The volumes take their look from the region they echo, using the ship tree's decorations (stars, orbits, belts, nebula images).

Notables follow the ship tree's rules: one notable per wheel, small nodes leading into it, wheels hanging off highways. Keystones are notables that change a rule outright rather than adding a percent.

## What nodes do

Node effects are lines, as the atlas's own node effects are. Every allocated node's lines are applied to a star chart when it is opened, and stay applied to that chart whatever happens to the tree afterwards; tree changes take effect from the next chart opened. They reach the game in three places.

1. Chart lines. Lines that change a chart (loot percents, fleet and boss size, enemy effects, chart modifier effect) join the chart's `ChartGenContext` at opening, next to its own lines and `atlas_node_effects.csv`.
2. Anomaly tuning. Lines an anomaly reads to change its own behaviour (more breach fleets, longer relay chains), doing nothing in a chart without that anomaly. Each anomaly that gets a volume names its tuning lines in its own plan, as `anomalies/overview.md` already expects tuning to arrive as `ChartModifier` lines.
3. Atlas lines. Lines that change the atlas itself: anomaly chances, Sensor Array frequency, visibility range, faction fleets, infestations. The generator stays a pure function of the seed and its inputs, with the allocated atlas lines added to the inputs (`AtlasRuntime` already caches one generator per save and unlock set; the lines join that key). A higher anomaly weight only lets more hashes pass, so allocating a chance node adds anomalies to the map and refunding it takes them back without reshuffling anything else, except where a new anomaly wins a conflict or the per-system cap. Biome weights affect only sites not yet revealed, since revealed sites are saved. The map shows the current tree at once; a chart takes what its system holds when it opens.

## Points and refunds

The tree grants at most 100 Atlas Skill Points, one for each chart completed (its boss killed). This replaces today's sources, a point for the first clear of each tier and one per Sensor Array (`AtlasProgress`, `AtlasState.atlasPoints`). Every node costs one point.

Refunds are free and work at any time, as in Path of Exile 2, since an open chart keeps the lines it opened with.

## Reuse

The Atlas Tree shares the ship skill tree's code, never its content: every node, type and volume on it is new. The tree belongs to the Star Charts domain and lives in `exiledsector-star-charts` (`charts.tree`, its screen in `ui.atlas`), which sees only core, so the shared code moved from `exiledsector-skill-tree` into core with no change to the ship tree. Classes kept their packages, so most moves were file moves.

The seam is the core `TreeNode` (id, position, connections, tags and volume, tier, wormhole partner, ring and wormhole looks). `SkillNode` adds its skill type and ship rules on top of it, and `AtlasTreeNode` adds its atlas type. A node's volume is resolved by the loader of its tree (`TreeTags`), from `SkillTags` for the ship tree and from the `volumes` list in `atlas_tree.json` for the Atlas Tree.

| Area | Shared in core | Atlas only |
|---|---|---|
| Data and loading | `TreeLayoutReader` (nodes, curves, hidden connectors, volumes, decorations through `DecorationLoader`), `TreeJson`, `WormholePairValidator`, the decoration model | `AtlasTreeData` reading types with chart effects |
| Graph | `TreeTopology` (connectors, neighbours, roots, wormhole pairs; `SkillTreeTopology` extends it), `TreeSearch` | `AtlasTreeAllocation`: free root, allocate beside an allocated node, paths, refunds that strand nothing |
| Drawing | `SkillTreeNodeRingRenderer`, `SkillTreeNodeGeometry`, `WormholeOpenness`, `ConnectorPainter` (beam lanes, curves, fades, helix bubbles), `ConnectorKind`, `AllocationAnimation`, `TreeDecorations` and every decoration renderer, `TreeNodeLayer` (nodes and connectors for trees without ship mechanics) | Node states from the allocation |
| Navigation | `CanvasCamera` (now with the hyperspace camera jump), `HyperspaceMode` with its labels, ghost flights, anchors and routes, `PointerGesture`, `StarfieldRenderer` | Volume labels from `atlasTree.volume.<id>` |
| Screens | `CampaignOverlay` (now able to switch one screen for another) | `AtlasTreeScreen`, the Atlas Tree button on `AtlasScreen` |

The ship canvas (`SkillTreeCanvasPlugin`) was not split into a shared canvas with plug-ins: almost all of it is ship-only (sockets, templates, exoships, the root choice, the stat panel), so the Atlas Tree screen composes the shared parts directly instead. The ship's node drawer and connector renderer keep their ship state logic and hand the drawing to the shared ring renderer and `ConnectorPainter`.

## Data

The layout is `data/skilltrees/atlas_tree.json`, the same shape as `ship_skill_tree.json` (nodes, connections, wormhole pairs, volume tags, decorations) plus a `volumes` list declaring the tree's volume ids, since the ship tree's volumes are a fixed Java list. Volume names are `atlasTree.volume.<id>` strings. Node types are `data/skilltrees/atlas_tree_types.json`, the same shape as `skill_types.json` (id, name, icon, tier, `effects` as effect id and magnitude, flavour, tags), with chart modifier ids as effects. Text goes in `data/strings/exiledSector/starCharts/`.

## Editor

The Atlas Tree gets its own page in `tools/editor`, beside the Tree page, which places nodes and decorations the same way: drag nodes, draw connections, pair wormholes, orbit nodes around a star, and add, move and edit stars, orbits, belts and nebula images per volume. It is the Tree page pointed at the other two files, not a copy of it. The canvas (`TreeCanvas`, `DecorationLayers`, `CanvasOverlays`), the decoration model and inspector (`decorations.ts`, `DecorationInspector`), connectors, geometry, validation, serialisation, the node inspector, the volume field, the add-node and type-picker dialogs, the type form and the undo store take the tree they edit as a parameter, so both pages share them and a fix to one fixes both. Since atlas types share the skill type shape, the type form only swaps its effect picker (chart modifier ids in place of skill effects) and hides the ship-only sections (OP cost, affinity, integration, phantom hull mods).

The server gets the two new files in `dataFiles.ts` with their shape checks, persistence keeps the Atlas Tree's unsaved changes apart from the ship tree's, and a round-trip test like the Tree page's (`roundTrip.test.ts`) covers every field the Atlas Tree adds, so saving never drops one. Rendering is covered by jsdom tests as the Tree page's are.

## Save footprint

`AtlasState` adds the allocated node ids, and its atlas points become the points earned (capped at 100) with the spent count derived from the allocation. A node id the data no longer has is dropped on load and its point refunded. Open charts already store their node (`AtlasRunNode`); they also store the tree lines they opened with, so mid-run effects that rebuild the context (`ChartRunEffects.contextOf`) keep them. Chart systems keep the zero-trace rule.

## Decisions

Decided 2026-11-09: the tree is the Atlas Tree and its points are Atlas Skill Points. It has wormholes. It grants at most 100 points, one per chart completed. Every node's effects apply to a chart when it opens and stay with that chart; tree changes take effect from the next chart opened. The tree may change the atlas itself, not only charts. Nodes grow stronger and riskier with distance from the root as a layout leaning, not an enforced rule.

## Open questions

1. Volume names and the first layout: which themes to keep, and which sit near the root.

## Build order

1. [x] Core tree node and data: `TreeNode`, `TreeTags`, `TreeTopology`, `TreeLayoutReader`, `TreeJson`, `WormholePairValidator`, the decoration model and `DecorationLoader` in core; `SkillNode`, `SkillTreeTopology` and `SkillTreeLoader` built on them with no ship tree change (tests moved with the classes).
1b. [x] Graph search: `TreeSearch` generic over `TreeTopology` in core. The ship's `AllocationPath`, `NodeAllocator` and `TreeAllocationSession` keep their ship rules and stay in the skill tree; the Atlas Tree has its own small rules (`AtlasTreeAllocation`).
1c. [x] Drawing and navigation in core: decoration renderers and `TreeDecorations` (ship factory `ShipTreeDecorations`), the hyperspace plane (`HyperspaceMode`, labels, ghost flights; ship factory `ShipHyperspace`), `SkillTreeNodeRingRenderer`, `SkillTreeNodeGeometry`, `WormholeOpenness`, `ConnectorPainter` split out of the ship connector renderer, `ConnectorKind`, `AllocationAnimation`, and `TreeNodeLayer` for plain trees. The ship canvas was not split (see Reuse).
2. [x] Tree data: `AtlasTreeData` reads both files through the shared reader, with chart modifier ids as effects and volumes from the `volumes` list; seed files hold only the root. Tests: `charts.tree.AtlasTreeDataTest`.
3. [x] Editor: every Tree page part takes the tree it edits (`treeKinds.ts`, `state/treeEditor.ts`, `state/treeEditorContext.ts`, `TreeEditorPage.tsx`), and the Atlas Tree page at `/atlas-tree` (`pages/atlasTree/`) edits the two files with chart effects, atlas tiers, a volumes dialog and no ship-only type sections. Tests: `atlasRoundTrip.test.ts`, `AtlasTreePage.test.tsx`, and the editor pins in `EditorRoundTripTest`. Not built: the hyperspace preview.
4. [x] Allocation and points: `AtlasState` keeps the allocated ids, each completed chart grants an Atlas Skill Point up to 100 (`AtlasProgress`; Sensor Arrays and first tier clears no longer grant extra), `AtlasTree` allocates single nodes and whole paths and refunds for free. Tests: `charts.tree.AtlasTreeAllocationTest`, `AtlasTreeTest`. Not built: dev console commands to allocate.
5. [ ] Chart lines: allocated lines join `ChartGenContext` at opening and stay on the open chart.
6. [x] Screen: `ui.atlas.AtlasTreeScreen`, opened by the Atlas Tree button on the atlas screen and handing back to it on close (`CampaignOverlay.Host.switchTo`): starfield, decorations, nodes and connectors, allocation by click (Ctrl+Shift for a path), refunds, wormhole travel on right-click, the hyperspace plane when scrolling out at minimum zoom, the points line and node tooltips (`AtlasTreeNodeText`). Not built: search, camera memory.
7. [ ] Atlas lines: anomaly and special-site weights, visibility, faction fleets and infestations into the generator inputs.
8. [ ] A first layout of three volumes near the root (crafting items, hull items, Star Charts) to test the loop.
9. [ ] Anomaly tuning lines, one anomaly volume at a time, each in that anomaly's plan.
10. [ ] The rest of the volumes, then keystones.
