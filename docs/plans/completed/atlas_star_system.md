# Atlas Star System

The Atlas Tree is one large, still star system. Built 2026-11-17 as an orbiting system; the orbits, belts and
lay-in-course arrows were removed on 2026-11-18 and every body now stays where the generator puts it.

## Layout (done: `tools/atlas/build-atlas-tree.mjs`)

- The root is the central star. Five highway rings of small nodes with no anomaly circle it: rings 1-2 Boss Star Charts,
  rings 3-4 Catalysts, ring 5 Star Chart Modifier Effect. The rings hold 8, 12, 18, 20 and 24 nodes; three rays leave
  the star, and 3, 4, 5 and 6 links reach rings 2 to 5. Each ring link joins one node to one node, picked as close to
  radial as it can be while clearing every wheel.
- Every notable and keystone is its own wheel: a body (planet, derelicts, station, relay, breach) on the inside or
  outside of its ring, with two small nodes beside it. The path is ring node, outer small, inner small, notable, so each
  notable has exactly two lead-ups of its anomaly. The smalls sit well clear of the notable, inside the wheel circle on
  its spoke, and every link of the path spirals in towards the notable like a ship falling down a gravity well; each
  notable turns one way, so its spirals never cross.
  Supply Convoy notables are populated worlds (jungle, terran, ocean, or a city-covered world) with vanilla city lights,
  and their lead-ups are orbital stations at most a fifth the planet's size. Three convoys of seven to ten ships per station
  follow the wheel's connectors to its centre and back on right-hand lanes, slipping behind the planet and fading out at the station
  (hybrids and Mixed Manifest keep their own centre, and their convoy stations run to it).
- Excavation notables are planets of differing non-gas-giant types with the blinking site light; Debris notables are
  dormant hulls of their sleeper faction in a debris field (Veiled Sleepers are shrouded dwellers swimming instead, and
  Silent Choir is a derelict fleet); Double Down is a binary of two relays; Incursion is a research station.
  Depot Vault notables are small stations (a quarter of the body, Fortified Depot a low-tech battlestation) with four
  little frigates swimming round them; their lead-ups are one or two little frigates flying about inside the node ring
  under engine power, each sending out a small sensor-burst ping every few seconds.
- Hybrids are planets whose two smalls come one from each parent anomaly (Event Horizon takes two breach smalls).
- Chance nodes (+8%) fill lead-up slots first on hybrids, then from the inner ring outward, at most 12 per anomaly.
- Harder notables sit on outer rings. An anomaly's notables are spread around and across rings, never grouped.
- The generator checks that no wheels overlap, no link passes over a body and no two links cross, and writes
  `data/skilltrees/atlas_tree.json` (nodes as the editor knows them) and `data/skilltrees/atlas_bodies.json` (per node:
  colour, art, heading, whether it is a highway node, the wheel a small belongs to and which way a notable spirals). Rerun it with `node tools/atlas/build-atlas-tree.mjs`; it is
  seeded, so the same table gives the same layout.

## Drawing (done: `ui.atlas.AtlasTreeView`, `ui.atlas.AtlasBodyPainter`, `charts.tree.AtlasTreeBodies`)

- The art is the node: dull when unallocated, brighter with a breathing ring when allocatable, full colour when
  allocated. Excavation planets carry a bright blinking site light fixed to the surface, turning with the planet and
  hidden while it is on the far side. Breach bodies sit on a pitch-black disc that hides the starscape, with their
  corona peeking out round it. Every body but the star wears a node ring drawn with the connectors' beam, in its anomaly colour
  (white on highways), and connectors stop at the ring's edge as they do in the ship tree. Hover shows the node tooltip.
- A wheel's three rings (its notable and both smalls) are always in their anomaly colour: faint while the wheel is
  untouched and brighter for each of its nodes allocated, breathing while allocatable. There is no corona glow.
- Links along a ring are connector beams following the ring's circle, wheel links follow their spiral
  (`charts.tree.AtlasLinkPath`), and the rays between the star and the rings are straight.
- Fleets, derelicts and the battlestation are drawn from vanilla hulls (`ui.decoration.AtlasShipGroup`); fleets drift
  in formation with engine flames, derelict fleets of 5-10 small, unpowered hulls weave slowly back and forth in formation, without engine flames, in a
  vanilla-style debris field (`ui.decoration.AtlasDebrisField`:
  drifting, spinning sheet pieces, some glowing, some bracketed), and the shrouded dwellers swim loops trailing their
  shroud (`ui.decoration.AtlasShroudedPod`). Stations, relays and other non-planet bodies turn at a fifth of a planet's
  speed.
- Event Horizon is a miniature black hole: black disc, glowing rim, faint horizon aurora, turning mauve accretion
  bands and the vanilla star halo.

## Screen switches (done: `CampaignOverlay`, `OverlayFade`)

- Going between the Atlas and the Atlas Tree (and the officer reward picker) hands the screen over: the old canvas
  stays up beneath the new one until the new one has faded fully in, so a slow load never shows the campaign.

## Atlas Tree Effects (done: `ui.atlas.AtlasTreeEffectsPanel`, `charts.tree.AtlasTreeEffectGroups`)

- A Show Effects button beside Back toggles a panel on the right listing every granted effect, summed per anomaly, with
  space between effects. Nodes
  without an anomaly tag form the Every Star Chart group first, then single anomalies, then hybrids under both names.
- The panel scrolls with the mouse wheel, blocks clicks and hovers beneath it, closes on Esc before the screen does,
  and rebuilds its text after each allocation or refund.

## Open

- Wheel exceptions the user will describe later.
