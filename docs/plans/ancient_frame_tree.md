# Ancient Frame tree: plan

Status: planned 2026-10-22 from Spectros's feedback on the Navarchy of Tania volume. Nothing is built. The owner is building a demo of the zoom first.

Ancient Frame stays a single node on the main tree. Allocating it zooms the view into the node, the way boarding the exoship does, and reveals a small tree of its own. That tree splits Ancient Frame's hull mod bonuses into three branches, each with a notable per hull mod and themed small nodes leading up to it. The idea comes from Borderlands 2's three-column skill trees.

## Decided

- Every other Tania node stays where it is. The Rise From the Ashes, Solaris Overdrive and Thermo-Accelerator wheels and their lead-ins don't change. Only Ancient Frame changes.
- Ancient Frame keeps its spot on the main tree (`tania_ancient_frame_1`, at the end of the fire rate lead-in).
- Its hull mod lines move out of the Ancient Frame node and into the zoomed tree, sorted into three branches: mobility, defence and firepower.
- Each moved line becomes its own notable, with a few thematic small nodes leading up to it.
- Each notable still applies only while its hull mod is installed, or while it's an s-mod for the built-in lines, exactly as `HullModInteractionEffect` does now.
- The branches don't connect to each other (Spectros's preference).

## Branches

There's one notable per hull mod, so the two Dedicated Targeting Core lines share a notable, and so do the two Insulated Engine lines. That makes 15 notables, five per branch. Magnitudes are today's Ancient Frame values, which are half of Tania's.

Mobility (green). Spectros put peak performance time here.

| Notable for | Lines | Now |
|---|---|---|
| Nav Relay | `NAV_RELAY_TOP_SPEED_PERCENT` | 5 |
| Insulated Engine Assembly (built-in) | `BUILT_IN_INSULATED_ENGINE_ACCELERATION_PERCENT`, `BUILT_IN_INSULATED_ENGINE_TURN_RATE_PERCENT` | 25, 15 |
| Auxiliary Fuel Tanks (built-in) | `BUILT_IN_FUEL_TANKS_TOP_SPEED_PERCENT` | 10 |
| Auxiliary Thrusters (built-in) | `BUILT_IN_AUX_THRUSTERS_ZERO_FLUX_TOP_SPEED_PERCENT` | 10 |
| Additional Berthing (built-in) | `BUILT_IN_BERTHING_PEAK_CR_PERCENT` | 15 |

Defence (blue).

| Notable for | Lines | Now |
|---|---|---|
| Reinforced Bulkheads | `BULKHEADS_EMP_DAMAGE_TAKEN_MULT` | -15 |
| Blast Doors (built-in) | `BUILT_IN_BLAST_DOORS_HULL_PERCENT` | 15 |
| Integrated Point Defense AI | `POINT_DEFENSE_AI_DAMAGE_TO_FIGHTERS_MULT` | 25 |
| Distributed Fire Control | `FIRE_CONTROL_FLUX_DISSIPATION_MULT` | 10 |
| Efficiency Overhaul (built-in) | `BUILT_IN_EFFICIENCY_OVERHAUL_CR_LOSS_MULT` | -10 |

Firepower (red).

| Notable for | Lines | Now |
|---|---|---|
| Ballistic Rangefinder | `RANGEFINDER_BALLISTIC_WEAPON_DAMAGE_PERCENT` | 5 |
| Dedicated Targeting Core | `TARGETING_CORE_BALLISTIC_WEAPON_DAMAGE_PERCENT`, `TARGETING_CORE_BALLISTIC_PROJECTILE_SPEED_PERCENT` | 5, 15 |
| ECM Package | `ECM_BALLISTIC_WEAPON_RANGE_PERCENT` | 5 |
| Armored Weapon Mounts (built-in) | `BUILT_IN_ARMORED_MOUNTS_RECOIL_MULT` | -15 |
| High Resolution Sensors (built-in) | `BUILT_IN_SENSORS_DAMAGE_TO_HULL_PERCENT` | 5 |

Distributed Fire Control and Point Defense AI sit in defence to keep the branches at five each. Flux dissipation backs shields, and point defence protects the ship.

The lead-in small nodes use existing small types that match the branch: top speed and maneuverability for mobility, hull, armor and flux for defence, and ballistic damage and range for firepower.

## Layout

- Ancient Frame is the centre. The three branches leave it in different directions.
- Inside a branch, a spine runs out from the centre and each notable sits at the end of a short spur of one or two small nodes. Exact positions get set in the editor.
- The zoomed nodes are placed around Ancient Frame's own coordinates. They're hidden on the main tree, so overlapping the Tania wheels there doesn't matter.
- Point budget: ships have 60 points by default, and reaching Ancient Frame already costs the path in plus the Navarchy unlock item. Five notables and about ten small nodes per branch is about 15 points, so one full branch plus part of another fits.

## How the zoom works today

The exoship (`ExoshipNodes`, `ui/ExoshipView`) is the only zoomed tree. Nodes tagged with the `exotech` region are "carried":

- `TreeLayers` draws carried nodes in a second viewport and cross-fades it in as the view zooms (`ExoshipView.layers`, `viewLevel`). Only one layer takes clicks at a time.
- `SkillTreeNodeConnectorRenderer` draws only connectors whose two ends are in the same layer.
- `NodeRadiusCache.isExcluded` and `SkillTreeWormholeGhostFlights` leave carried nodes out.
- Boarding (`SkillTreeCanvasPlugin.boardExoship`) happens when you click the ship or make a wormhole jump whose target is the exoship (`commitNodeClick`). The camera then follows the ship at a zoom fitted to the farthest carried node (`fitZoom`, `carriedReach`). `stopFollowing` and Escape leave the view.
- `CanvasMode.EXOSHIP_VIEW` hides search and the template bar while you're aboard.
- The exoship roams the main tree (`ExoshipVoyage`), so the carried viewport shifts by how far the ship is from its home coordinates.

Carried nodes are ordinary graph nodes. They connect to the exoship node in `ship_skill_tree.json`, and allocation pathing goes through them as normal.

## What changes

- A carrier abstraction replaces the single exoship lookup. A carrier is a node plus the region tag its carried nodes use: the exoship carries `exotech`, and Ancient Frame carries a new region, e.g. `ancient_frame`. `ExoshipNodes.isCarried` becomes "carried by which carrier".
- `TreeLayers` shows the carried layer of the carrier being viewed. Nodes carried by any other carrier stay hidden in both layers.
- Ancient Frame is a static carrier: no voyage, no shift, focus on its own coordinates, and no ship sprite. Its centre is the Ancient Frame node itself (a Tania hull could replace it later).
- `CanvasMode.EXOSHIP_VIEW` becomes a carried view mode for any carrier.
- Allocating Ancient Frame zooms in. Escape leaves, as on the exoship.
- The new region needs the same Navarchy gating as `tania`. It already sits behind Ancient Frame, but `SkillTags` region lists and any region checks need it added.
- NPCs don't enter the Tania volume, so the NPC tree builder needs no change. Only the region exclusion lists need the new tag.
- Saves: ships with Ancient Frame allocated keep it, but it loses its lines until branch nodes are taken. Only our own saves matter before 1.0.

## Build order

1. Generalise the exoship carried layer into carriers. Code only: the exoship must look and behave the same. Tests for `TreeLayers` and `CanvasMode`.
2. Make Ancient Frame a static carrier with the new region: zoom on allocation, Escape out.
3. Add the 15 notable node types to `skill_types.json`, each with one or two existing `HullModInteractionEffect` lines, plus en and zh_CN names and flavour descriptions (crew learning vectors per node), and circular icons.
4. Lay out the three branches and their lead-ins in the editor.
5. Take the lines off Ancient Frame and settle what it keeps.
6. Regenerate the tooltip golden, shorten the "Hull mod interactions" section of `docs/docs.md`, and add any new data field to the editor serialisers.

## Spectros's other ideas, not in this plan

These came up in the same thread and stay out of scope unless picked up later: branch capstones (all ballistic slots become hybrid; more module OP at 8/15/30/40 by hull size; more speed the smaller the hull), and module upgrades in the defence branch (missile ammo, smaller fighter bay penalties, more mass for ramming).

## Open questions

- Does Ancient Frame grant anything itself once its lines move out, or is it only the entrance?
- One notable per hull mod (15), or one per line (17)?
- Can one ship enter more than one branch? Spectros wants no links between branches, which doesn't settle it.
- Magnitudes: keep half Tania's values, or go to full now that each line costs its own nodes?
- How do you zoom back in after leaving? Clicking an allocated node refunds it (`TreeAllocationSession.clickNode`), so re-entry needs a different action: a modifier click, an entry in the node's tooltip, or a wormhole-style jump like the exoship's.
- When Ancient Frame is refunded, do the branch nodes get released, or do the normal refund rules already block it?
- Centre art: the Ancient Frame icon or a Tania hull sprite, as Spectros suggested with the Black Hawk.
