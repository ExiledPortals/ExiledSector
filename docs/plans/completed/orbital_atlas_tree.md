# Orbital Atlas Tree

The Atlas Tree is one large star system whose nodes orbit. Built 2026-11-17.

## Layout (done: `tools/atlas/build-orbital-atlas.mjs`)

- The root is the central star. Five highway rings of small nodes with no anomaly orbit it: rings 1-2 Boss Star Charts,
  rings 3-4 Catalysts, ring 5 Star Chart Modifier Effect. Each ring turns at its own speed, slower further out. The
  rings hold 8, 12, 18, 20 and 24 nodes; three rays leave the star, and 3, 4, 5 and 6 tethers reach rings 2 to 5.
  Rings sit half the natural spacing apart, but never so close that a wheel reaches the next belt. Wheels from both rings
  share the space between them, so each takes the spoke nearest its slot that clears every placed wheel, and ring links
  pick anchors clear of the wheels they cross (the generator checks both).
- Every notable and keystone is its own wheel: a body (planet, fleet, derelicts, station, relay, breach) that turns with
  its ring, on its inside or outside, with two small nodes orbiting it on two tracks. The path is ring node, outer small, inner
  small, notable, so each notable has exactly two lead-ups of its anomaly. Convoy fleets are the exception: their two
  escort fleets do not orbit but hold station beside and a little behind the convoy.
- Excavation notables are planets of differing non-gas-giant types with the blinking site light; Debris notables are
  dormant hulls of their sleeper faction in a debris field; Double Down is a binary of two relays; Fortified Depot is a
  low-tech battlestation; Incursion a research station; Open Season a fleet of combat freighters.
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

- The art is the node: dull when unallocated, brighter with a breathing ring when allocatable, full colour when
  allocated. Bodies have no glow; excavation planets carry the chart screen's blinking site dot. Hover shows the node
  tooltip and slows orbits to a quarter speed.
- Highway rings are asteroid belts (the outermost ice) turning with their ring; highway nodes keep a white node ring.
  `charts.tree.AtlasOrbitLayout` sorts bodies into highway nodes, wheels, star rays and belts.
- Star rays, ring arcs and ring-to-ring tethers use the skill tree's connector beams.
- Each wheel's chain (`ui.atlas.AtlasWheelRoute`) is drawn as vanilla's lay-in-course arrows moving toward the notable
  (`ui.atlas.AtlasCourseArrows`, the `ship_arrow` sprite in the player colour, fading in and out at the ends): the
  highway node wraps around the notable to the outer small (switching sides over the planet once the other way is
  clearly shorter), a spiral carries on the same way to the inner small, and a slight curve joins the notable.
- Fleets, derelicts and the battlestation are drawn from vanilla hulls (`ui.decoration.AtlasShipGroup`); fleets drift
  in formation with engine flames, derelicts tumble without drives.

## Atlas Tree Effects (done: `ui.atlas.AtlasTreeEffectsPanel`, `charts.tree.AtlasTreeEffectGroups`)

- A Show Effects button beside Back toggles a panel on the right listing every granted effect, summed per anomaly. Nodes
  without an anomaly tag form the Every Star Chart group first, then single anomalies, then hybrids under both names.
- The panel scrolls with the mouse wheel, blocks clicks and hovers beneath it, closes on Esc before the screen does,
  and rebuilds its text after each allocation or refund.

## Open

- Wheel exceptions the user will describe later.
