# Orbital Atlas Tree

The Atlas Tree is one large star system whose nodes orbit. Built 2026-11-17.

## Layout (done: `tools/atlas/build-orbital-atlas.mjs`)

- The root is the central star. Five highway rings of small nodes with no anomaly orbit it: rings 1-2 Boss Star Charts,
  rings 3-4 Catalysts, ring 5 Star Chart Modifier Effect. Each ring turns at its own speed, slower further out. The
  rings hold 8, 12, 18, 20 and 24 nodes; three rays leave the star, and 3, 4, 5 and 6 tethers reach rings 2 to 5.
- Every notable and keystone is its own wheel: a body (planet, fleet, station, relay, breach) that turns with its ring,
  on its inside or outside, with two small nodes orbiting it on two tracks. The path is ring node, outer small, inner
  small, notable, so each notable has exactly two lead-ups of its anomaly.
- Hybrids are planets whose two smalls come one from each parent anomaly (Event Horizon takes two breach smalls).
- Chance nodes (+8%) fill lead-up slots first on hybrids, then from the inner ring outward, at most 12 per anomaly.
- Harder notables sit on outer rings. An anomaly's notables are spread around and across rings, never grouped.
- The generator writes `data/skilltrees/atlas_tree.json` (nodes as the editor knows them, positions at time 0) and
  `data/skilltrees/atlas_orbits.json` (per node: orbit centre, speed, colour, art). Rerun it with
  `node tools/atlas/build-orbital-atlas.mjs`; it is seeded, so the same table gives the same layout.

## Orbits and tethers (done: `charts.tree.AtlasOrbitSystem`)

- A body's frame is its centre plus its speed; satellites turn relative to their centre's frame.
- Links in one frame are rigid (an arc when both sit on the same track). A link to the body's own centre is straight.
- Any other link is a tether: a radial line from the anchor that lands on the other node's whole track, so it never
  stretches. The anchor is the outer body for two orbits round one centre, otherwise the body whose frame holds the
  other track's centre. `AtlasTreeData` adds every node on the landing track as a neighbour of the anchor.

## Drawing (done: `ui.atlas.AtlasOrbitView`, `ui.atlas.AtlasBodyPainter`)

- The art is the node: dull when unallocated, brighter with a pulsing ring when allocatable, full colour with a glow
  when allocated. Hover shows the node tooltip. Orbits slow to a stop while a node is hovered.
- Tracks that links use are drawn as faint circles, lit in the node's colour once a node on them is allocated.

## Open

- Wheel exceptions the user will describe later.
