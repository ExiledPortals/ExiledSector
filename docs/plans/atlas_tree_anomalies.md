# Atlas Tree anomaly mechanics

Status: planning. Atlas Tree mechanics for each anomaly, one anomaly at a time.

## General

The framework is built (`completed/atlas_skill_tree.md`). An Atlas Tree node can carry chart lines, which join a chart when it opens from the atlas, and atlas lines (`AtlasModifier`), which change the atlas itself. Every common anomaly rolls an 8% `opening_chance` when a chart opens (`OpeningAnomalies`). Anomaly plans are in `anomalies/`.

An anomaly marker on an atlas system adds one more of that anomaly to the system where that makes sense: one more Spatial Breach, one more Anomalous Debris fleet. Anomalies where it doesn't keep today's rules, and their sections say so.

## Tooltip rules

Atlas Tree lines follow `docs/tooltip_wording.md`, and take their shape from Path of Exile's atlas passive tree (3.29). Where the two disagree, `tooltip_wording.md` wins.

Scope:

- Every line names where it applies, as Path of Exile's "in your Maps" does. A chart line says "your Star Charts": "Spatial Breaches in your Star Charts have 20% increased maximum radius". An atlas line names the atlas: "+1 lane of visibility around completed atlas systems".
- The subject comes first, then the change: "Breach caches in your Star Charts contain...", "Fleets charging Spatial Breaches in your Star Charts have...". A line about the chart as a whole opens with "Your Star Charts".
- Tree lines are their own text, not the chart's "in the system" wording, since a tree line describes every chart opened rather than the one in hand.

Shapes:

- Chance to hold an anomaly, added as percentage points: "Your Star Charts have +4% chance to contain a Spatial Breach".
- Change to an existing chance: "50% increased chance for Spatial Breaches in your Star Charts to...".
- Guaranteed count: "Your Star Charts contain an additional Supply Convoy". Conditional count: "Your Star Charts that contain a Spatial Breach have 10% chance to contain an additional Spatial Breach". Fixed count: "Your Star Charts with Relay Networks always have four relays".
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

## Black Hole (`black_hole`)

## Neutron Star (`neutron_star`)

## Dense Nebula (`dense_nebula`)

## Asteroid Storm (`asteroid_storm`)

## Magnetic Storm (`magnetic_storm`)

## Stellar Flares (`stellar_flares`)

## Spatial Breach (`spatial_breach`)

A system can hold more than one breach. A Spatial Breach marker on an atlas system adds one guaranteed breach to it. Today a system holds at most one: several need indexed cores, caches owned by their breach, spacing between breaches, and a `BreachTerrainPlugin.collapse` that removes only its own caches rather than every `CACHE_TAG` entity in the system (`completed/anomalies/spatial_breach_changes.md`).

Small nodes:

- Increased chance of a Spatial Breach when a chart opens, on top of its `opening_chance` (`OpeningAnomalies`, which doesn't read tree lines yet).
- Increased breach cache quantity (`CHART_BREACH_CACHE_PERCENT`).
- Increased quantity of items found in breach caches (`CHART_BREACH_CACHE_ITEM_PERCENT`).

Notables:

- Heavy Breach: the breach stays at full size for a while before it collapses (a hold phase in `BreachRules.Growth`).
- Wide Tear: the breach opens to a larger maximum radius (a new radius line in `BreachRules`).
- Hardened Hulls: the breach drains double the fuel and supplies, and 20% increased quantity of hull items found in breach caches (a drain line for `BreachSupplyBuff` and the fuel drain, and a hull item line scoped to breach caches).
- Incursion: the breach spawns a guaranteed research station guarded by a fleet 110% of the boss fleet's size. The station drops what a vanilla research station would drop in the chart, with every chart line applied.

Open question: when the Incursion station appears, and whether it goes with the collapse.

## Faction Skirmish (`faction_skirmish`)

## Relay Network (`relay_network`)

## Supply Convoy (`supply_convoy`)

## Anomalous Debris (`anomalous_debris`)

## Excavation Sites (`excavation_sites`)

## Storm Front (`storm_front`)

## Derelict Forge (`derelict_forge`)

## Depot (`listening_post`)

## Pre-Collapse Facility (`pre_collapse_facility`)

## Coronal Hypershunt (`coronal_hypershunt`)

## Remnant Nexus (`remnant_nexus_station`)

## Domain-era Mothership (`domain_mothership`)

## Abyssal Shipyard (`abyssal_shipyard`)

## Abyssal Storm (`abyssal_storm`)

## Abyssal Research Complex (`abyssal_research_complex`)
